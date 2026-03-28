package Project._6.demo.service;

import Project._6.demo.dto.CommunityModerationResultDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
public class CommunityModerationService {

    private static final List<String> API_VERSIONS = List.of("v1beta", "v1");
    private static final List<String> MODEL_FALLBACKS = List.of(
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite",
            "gemini-1.5-flash"
    );

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

        private static final List<String> BAD_WORDS = List.of(
            "fuck", "shit", "bitch", "asshole", "bastard", "slut", "idiot", "stupid", "dumb"
        );

        private static final Pattern ENGLISH_ONLY_PATTERN = Pattern.compile("^[\\x09\\x0A\\x0D\\x20-\\x7E]+$");
        private static final Pattern PHONE_PATTERN = Pattern.compile("(?:\\+?\\d{1,3}[\\s-]?)?(?:\\d[\\s-]?){9,12}");
        private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    public CommunityModerationResultDTO moderateText(String rawMessage, String rawContentType) {
        String message = rawMessage == null ? "" : rawMessage.trim();
        String contentType = rawContentType == null ? "post" : rawContentType.trim().toLowerCase(Locale.ROOT);

        if (message.isBlank()) {
            return new CommunityModerationResultDTO("BLOCK", "Message cannot be empty.", 100, "");
        }

        CommunityModerationResultDTO localResult = runLocalChecks(message);
        if ("BLOCK".equals(localResult.getDecision())) {
            return localResult;
        }

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return new CommunityModerationResultDTO("ALLOW", "Passed local moderation checks.", 20, "");
        }

        try {
            CommunityModerationResultDTO aiResult = runGeminiModeration(message, contentType);
            if (aiResult != null) {
                if ("WARN".equals(aiResult.getDecision())
                        && aiResult.getReason() != null
                        && aiResult.getReason().contains("HTTP 429")) {
                    return new CommunityModerationResultDTO("ALLOW", "Passed local moderation checks.", 25, "");
                }
                return aiResult;
            }
        } catch (Exception ignored) {
            // Fall back to local checks when AI moderation is unavailable.
        }

        return new CommunityModerationResultDTO("ALLOW", "Passed local moderation checks.", 25, "");
    }

    public CommunityModerationResultDTO moderateLiveText(String rawMessage, String rawContentType) {
        String message = rawMessage == null ? "" : rawMessage.trim();
        if (message.isBlank()) {
            return new CommunityModerationResultDTO("ALLOW", "", 0, "");
        }
        
        CommunityModerationResultDTO localResult = runLocalChecks(message);
        if ("BLOCK".equals(localResult.getDecision())) {
            return localResult;
        }

        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            try {
                return runGeminiLiveModeration(message);
            } catch (Exception ignored) {
                ignored.printStackTrace();
                System.err.println("AI live moderation failed: " + ignored.getMessage());
                // fallback to local checks
            }
        }
        
        return localResult;
    }

    private CommunityModerationResultDTO runLocalChecks(String message) {
        String lower = message.toLowerCase(Locale.ROOT);

        if (PHONE_PATTERN.matcher(message).find()) {
            return new CommunityModerationResultDTO("BLOCK", "Phone numbers are not allowed in public posts.", 95, "");
        }

        if (EMAIL_PATTERN.matcher(message).find()) {
            return new CommunityModerationResultDTO("BLOCK", "Email addresses are not allowed in public posts.", 95, "");
        }

        if (containsAddressLikePii(lower)) {
            return new CommunityModerationResultDTO("BLOCK", "Address or personal location details are not allowed.", 90, "");
        }

        if (containsBadWords(lower)) {
            return new CommunityModerationResultDTO("BLOCK", "Inappropriate language detected. Please use respectful words.", 90, "");
        }

        if (!isEnglishOnly(message)) {
            return new CommunityModerationResultDTO("BLOCK", "Only English language is allowed in community chat.", 98, "");
        }

        return new CommunityModerationResultDTO("ALLOW", "Passed local moderation checks.", 20, "");
    }

    private boolean isEnglishOnly(String message) {
        if (!ENGLISH_ONLY_PATTERN.matcher(message).matches()) {
            return false;
        }
        return Pattern.compile("[A-Za-z]").matcher(message).find();
    }

    private boolean containsBadWords(String lowerMessage) {
        for (String badWord : BAD_WORDS) {
            if (lowerMessage.contains(badWord)) {
                return true;
            }
        }
        return false;
    }

    private CommunityModerationResultDTO runGeminiLiveModeration(String message) throws IOException, InterruptedException {
        String moderationPrompt = "You are a strict language gate and spell checker for a university student chat. "
                + "Rules: BLOCK if text is Sinhala script or transliterated Sinhala (Singlish/romanized Sinhala). "
                + "If text is English, ALLOW it and return corrected English in correctedText. "
                + "If English is already correct, keep correctedText exactly equal to input. "
                + "Return JSON only with keys: decision, reason, riskScore, correctedText. "
                + "decision must be ALLOW or BLOCK. riskScore must be an integer 0-100. "
                + "Input text: " + message;

        String rawModelOutput = invokeGeminiAndExtractText(moderationPrompt, 220, 10);
        String json = extractAiJsonText(rawModelOutput);

        String decision = safeUpper(extractQuotedJsonValue(json, "decision"));
        if (!("ALLOW".equals(decision) || "BLOCK".equals(decision))) {
            decision = "ALLOW";
        }

        String reason = stringOrEmpty(extractQuotedJsonValue(json, "reason")).trim();
        if (reason.isBlank() && "ALLOW".equals(decision)) {
            reason = "Passed moderation checks.";
        }

        int risk = clampRisk(extractIntJsonValue(json, "riskScore", 0));
        String correctedText = stringOrEmpty(extractQuotedJsonValue(json, "correctedText")).trim();
        if (correctedText.isBlank()) {
            correctedText = message;
        }

        return new CommunityModerationResultDTO(decision, reason, risk, correctedText);
    }

    private boolean containsAddressLikePii(String lowerMessage) {
        return lowerMessage.contains("my address")
                || lowerMessage.contains("home address")
                || lowerMessage.contains("house no")
                || lowerMessage.contains("street")
                || lowerMessage.contains("road")
                || lowerMessage.contains("lane")
                || lowerMessage.contains("postal code")
                || lowerMessage.contains("zip code");
    }

    private CommunityModerationResultDTO runGeminiModeration(String message, String contentType)
            throws IOException, InterruptedException {

        String moderationPrompt = "You are a strict moderation engine for a university student forum. "
                + "Classify content as ALLOW, WARN, or BLOCK. "
                + "BLOCK if it contains personal contact details (phone number, email, address), bullying, hate, sexual abuse language, threats, "
                + "or dangerous advice. "
                + "BLOCK if text is not proper English, including Sinhala script and transliterated Sinhala (Singlish / romanized Sinhala). "
                + "Examples that MUST be BLOCKED: 'mn enawa', 'oya kohomada', 'meka hari', 'heta oyala enwada'. "
                + "WARN if tone is borderline rude or misleading but can be corrected. "
                + "Return strict JSON only with keys decision, reason, riskScore. riskScore must be 0-100. "
                + "If uncertain, prefer BLOCK. "
                + "contentType=" + contentType + ". text=" + message;

        String rawModelOutput = invokeGeminiAndExtractText(moderationPrompt, 180, 10);
            String json = extractAiJsonText(rawModelOutput);

            String decision = safeUpper(extractQuotedJsonValue(json, "decision"));
            String reason = stringOrEmpty(extractQuotedJsonValue(json, "reason")).trim();
            int risk = clampRisk(extractIntJsonValue(json, "riskScore", 0));

        if (decision == null || decision.isBlank()) {
            decision = "ALLOW";
        }
        if (reason == null || reason.isBlank()) {
            reason = "Passed moderation checks.";
        }

        if (!("ALLOW".equals(decision) || "WARN".equals(decision) || "BLOCK".equals(decision))) {
            decision = "ALLOW";
        }

        return new CommunityModerationResultDTO(decision, reason, risk, "");
    }

    private String invokeGeminiAndExtractText(String prompt, int maxOutputTokens, int timeoutSeconds)
            throws IOException, InterruptedException {
        String escapedPrompt = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ");

        String requestBody = "{"
                + "\"contents\":[{\"parts\":[{\"text\":\"" + escapedPrompt + "\"}]}],"
                + "\"generationConfig\":{\"temperature\":0.1,\"maxOutputTokens\":" + maxOutputTokens + ",\"responseMimeType\":\"application/json\"}"
                + "}";

        Set<String> modelCandidates = new LinkedHashSet<>();
        modelCandidates.add(normalizeModelName(geminiModel));
        modelCandidates.addAll(MODEL_FALLBACKS);

        HttpResponse<String> response = null;
        int lastStatus = 0;

        for (String modelName : modelCandidates) {
            for (String apiVersion : API_VERSIONS) {
                String endpoint = "https://generativelanguage.googleapis.com/"
                        + apiVersion + "/models/" + modelName + ":generateContent?key=" + geminiApiKey;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                lastStatus = response.statusCode();

                if (lastStatus == 404) {
                    continue;
                }

                if (lastStatus >= 300) {
                    throw new IOException("AI moderation service error (HTTP " + lastStatus + ")");
                }

                break;
            }

            if (response != null && response.statusCode() < 300) {
                break;
            }
        }

        if (response == null || response.statusCode() >= 300) {
            throw new IOException("AI moderation model was not found (HTTP " + (lastStatus == 0 ? 404 : lastStatus) + ")");
        }

        String text = extractModelTextFromResponseBody(response.body());
        if (text.isBlank()) {
            throw new IOException("AI moderation response was empty");
        }
        return text;
    }

    private String extractAiJsonText(String modelText) throws IOException {
        String cleaned = modelText == null ? "" : modelText.trim();
        cleaned = cleaned.replace("```json", "").replace("```", "").trim();

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IOException("AI response format was invalid");
        }

        return cleaned.substring(start, end + 1);
    }

    private String extractModelTextFromResponseBody(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }

        Pattern candidateTextPattern = Pattern.compile("\\\"parts\\\"\\s*:\\s*\\[\\s*\\{\\s*\\\"text\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"");
        Matcher matcher = candidateTextPattern.matcher(responseBody);
        if (matcher.find()) {
            return unescapeJsonString(matcher.group(1));
        }

        return "";
    }

    private int clampRisk(int value) {
        if (value < 0) {
            return 0;
        }
        if (value > 100) {
            return 100;
        }
        return value;
    }

    private String safeUpper(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String stringOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String normalizeModelName(String configuredModel) {
        String model = configuredModel == null ? "" : configuredModel.trim();
        if (model.startsWith("models/")) {
            model = model.substring("models/".length());
        }
        if (model.isBlank()) {
            return MODEL_FALLBACKS.get(0);
        }
        return model;
    }

    private String extractQuotedJsonValue(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\\\"\\\\])*)\"")
                .matcher(json);
        if (matcher.find()) {
            return unescapeJsonString(matcher.group(1));
        }
        return null;
    }

    private String unescapeJsonString(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char n = value.charAt(++i);
                switch (n) {
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'u':
                        if (i + 4 < value.length()) {
                            String hex = value.substring(i + 1, i + 5);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                                i += 4;
                            } catch (NumberFormatException ex) {
                                sb.append("\\u").append(hex);
                                i += 4;
                            }
                        } else {
                            sb.append('u');
                        }
                        break;
                    default:
                        sb.append(n);
                        break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private int extractIntJsonValue(String json, String key, int fallback) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)")
                .matcher(json);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}
