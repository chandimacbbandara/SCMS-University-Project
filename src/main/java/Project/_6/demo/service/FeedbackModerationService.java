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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FeedbackModerationService {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private static final List<String> BAD_WORDS = List.of(
            "fuck", "fucking", "shit", "bitch", "asshole", "bastard", "slut", "idiot", "stupid", "dumb"
    );

    public CommunityModerationResultDTO moderateFeedbackText(String comments) {
        if (comments == null || comments.isBlank()) {
            return new CommunityModerationResultDTO("ALLOW", "No feedback comment provided.", 0);
        }

        String lowered = comments.toLowerCase(Locale.ROOT);
        if (containsBadWords(lowered)) {
            return new CommunityModerationResultDTO(
                    "BLOCK",
                    "Inappropriate language detected. Please remove offensive words and try again.",
                    95
            );
        }

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return new CommunityModerationResultDTO("ALLOW", "Passed local moderation checks.", 0);
        }

        try {
            CommunityModerationResultDTO aiResult = runGeminiModeration(comments);
            if (aiResult != null) {
                return aiResult;
            }
        } catch (Exception ignored) {
            // Allow submission when Gemini is temporarily unavailable.
        }

        return new CommunityModerationResultDTO("ALLOW", "Passed moderation checks.", 0);
    }

    private boolean containsBadWords(String loweredMessage) {
        for (String badWord : BAD_WORDS) {
            if (loweredMessage.contains(badWord)) {
                return true;
            }
        }
        return false;
    }

    private CommunityModerationResultDTO runGeminiModeration(String comments)
            throws IOException, InterruptedException {

        String moderationPrompt = "You are a strict moderation engine for student feedback text. "
                + "Detect profanity, abusive language, hate speech, harassment, sexual insults, or degrading words. "
                + "If such language exists, return BLOCK. Otherwise return ALLOW. "
                + "Return JSON only with keys decision, reason, riskScore where riskScore is 0-100. "
                + "text=" + comments;

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

        if (!("ALLOW".equals(decision) || "BLOCK".equals(decision))) {
            decision = "ALLOW";
        }

        if (reason == null || reason.isBlank()) {
            reason = "Passed moderation checks.";
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