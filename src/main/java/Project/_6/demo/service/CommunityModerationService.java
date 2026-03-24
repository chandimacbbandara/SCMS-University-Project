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
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
public class CommunityModerationService {

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
            return new CommunityModerationResultDTO("BLOCK", "Message cannot be empty.", 100);
        }

        CommunityModerationResultDTO localResult = runLocalChecks(message);
        if (!"ALLOW".equals(localResult.getDecision())) {
            return localResult;
        }

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return new CommunityModerationResultDTO("ALLOW", "Passed local moderation checks.", 0);
        }

        try {
            CommunityModerationResultDTO aiResult = runGeminiModeration(message, contentType);
            if (aiResult != null) {
                return aiResult;
            }
        } catch (Exception ignored) {
            // If Gemini is unavailable, keep app usable while local filters are still enforced.
        }

        return new CommunityModerationResultDTO("ALLOW", "Passed moderation checks.", 0);
    }

    private CommunityModerationResultDTO runLocalChecks(String message) {
        String lower = message.toLowerCase(Locale.ROOT);

        if (!isEnglishOnly(message)) {
            return new CommunityModerationResultDTO("BLOCK", "Only English language is allowed in community chat.", 98);
        }

        if (PHONE_PATTERN.matcher(message).find()) {
            return new CommunityModerationResultDTO("BLOCK", "Phone numbers are not allowed in public posts.", 95);
        }

        if (EMAIL_PATTERN.matcher(message).find()) {
            return new CommunityModerationResultDTO("BLOCK", "Email addresses are not allowed in public posts.", 95);
        }

        if (containsAddressLikePii(lower)) {
            return new CommunityModerationResultDTO("BLOCK", "Address or personal location details are not allowed.", 90);
        }

        if (containsBadWords(lower)) {
            return new CommunityModerationResultDTO("BLOCK", "Inappropriate language detected. Please use respectful words.", 90);
        }

        return new CommunityModerationResultDTO("ALLOW", "Passed local moderation checks.", 0);
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
                + "BLOCK if it contains personal contact details, bullying, hate, sexual abuse language, threats, "
                + "or dangerous advice. WARN if tone is borderline rude or misleading but can be corrected. "
                + "Return JSON only with keys decision, reason, riskScore. riskScore must be 0-100. "
                + "contentType=" + contentType + ". text=" + message;

        String escapedPrompt = moderationPrompt
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", " ");

        String requestBody = "{"
            + "\"contents\":[{\"parts\":[{\"text\":\"" + escapedPrompt + "\"}]}],"
            + "\"generationConfig\":{\"temperature\":0.1,\"maxOutputTokens\":180}"
            + "}";

        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                + geminiModel + ":generateContent?key=" + geminiApiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(8))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            return null;
        }

        String text = extractQuotedJsonValue(response.body(), "text");
        if (text == null || text.isBlank()) {
            return null;
        }

        text = text.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }

        String json = text.substring(start, end + 1);
        String decision = extractQuotedJsonValue(json, "decision");
        String reason = extractQuotedJsonValue(json, "reason");
        int risk = extractIntJsonValue(json, "riskScore", 0);

        if (decision == null || decision.isBlank()) {
            decision = "ALLOW";
        } else {
            decision = decision.toUpperCase(Locale.ROOT);
        }
        if (reason == null || reason.isBlank()) {
            reason = "Passed moderation checks.";
        }

        if (!("ALLOW".equals(decision) || "WARN".equals(decision) || "BLOCK".equals(decision))) {
            decision = "ALLOW";
        }

        if (risk < 0) {
            risk = 0;
        }
        if (risk > 100) {
            risk = 100;
        }

        return new CommunityModerationResultDTO(decision, reason, risk);
    }

    private String extractQuotedJsonValue(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"")
                .matcher(json);
        if (matcher.find()) {
            return matcher.group(1)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
        return null;
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
