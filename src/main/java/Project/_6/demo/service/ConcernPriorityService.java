package Project._6.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class ConcernPriorityService {

    private static final Logger logger = LoggerFactory.getLogger(ConcernPriorityService.class);

    private static final String PRIORITY_HIGH = "High";
    private static final String PRIORITY_MEDIUM = "Medium";
    private static final String PRIORITY_LOW = "Low";

    @Value("${concern.priority.ai.enabled:true}")
    private boolean aiEnabled;

    @Value("${concern.priority.ai.python-command:python3}")
    private String pythonCommand;

    @Value("${concern.priority.ai.script-path:ai-model/concern-priority/predict_priority.py}")
    private String scriptPath;

    @Value("${concern.priority.ai.model-dir:ai-model/concern-priority}")
    private String modelDir;

    @Value("${concern.priority.ai.default-priority:Medium}")
    private String defaultPriority;

    @Value("${concern.priority.ai.timeout-seconds:12}")
    private long timeoutSeconds;

    public String predictPriority(String category, String subject, String message) {
        String safeCategory = normalizeInput(category);
        String safeSubject = normalizeInput(subject);
        String safeMessage = normalizeInput(message);

        if (aiEnabled) {
            String predicted = runPythonPrediction(safeCategory, safeSubject, safeMessage);
            if (predicted != null) {
                return normalizePriority(predicted);
            }
        }

        return normalizePriority(defaultPriority);
    }

    private String runPythonPrediction(String category, String subject, String message) {
        Path script = resolveWorkspacePath(scriptPath);
        if (!Files.exists(script)) {
            logger.warn("Concern priority script not found: {}", script);
            return null;
        }

        Path modelDirectory = resolveWorkspacePath(modelDir);

        List<String> command = new ArrayList<>();
        command.add(pythonCommand);
        command.add(script.toString());
        command.add("--model-dir");
        command.add(modelDirectory.toString());
        command.add("--category");
        command.add(category);
        command.add("--subject");
        command.add(subject);
        command.add("--message");
        command.add(message);
        command.add("--fallback-priority");
        command.add(normalizePriority(defaultPriority));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder out = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (out.length() > 0) {
                        out.append('\n');
                    }
                    out.append(line);
                }
                output = out.toString();
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                logger.warn("Concern priority model timed out after {} seconds", timeoutSeconds);
                return null;
            }

            if (process.exitValue() != 0) {
                logger.warn("Concern priority model failed with code {}. Output: {}", process.exitValue(), output);
                return null;
            }

            return extractPriorityFromOutput(output);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.warn("Concern priority model call failed: {}", e.getMessage());
            return null;
        }
    }

    private String normalizeInput(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value.replaceAll("\\s+", " ").trim();
        int maxLength = 1500;
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) : cleaned;
    }

    private String normalizePriority(String value) {
        if (value == null) {
            return PRIORITY_MEDIUM;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("high")) {
            return PRIORITY_HIGH;
        }
        if (normalized.startsWith("low")) {
            return PRIORITY_LOW;
        }
        if (normalized.startsWith("med")) {
            return PRIORITY_MEDIUM;
        }

        return PRIORITY_MEDIUM;
    }

    private String extractPriorityFromOutput(String output) {
        if (output == null || output.isBlank()) {
            return null;
        }

        String detected = null;
        for (String line : output.lines().toList()) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String normalized = normalizePriority(trimmed);
            if (!PRIORITY_MEDIUM.equals(normalized)
                    || trimmed.equalsIgnoreCase(PRIORITY_MEDIUM)
                    || trimmed.equalsIgnoreCase("MEDIUM")) {
                detected = normalized;
            }
        }

        return detected;
    }

    private Path resolveWorkspacePath(String configuredPath) {
        Path rawPath = Paths.get(configuredPath == null ? "" : configuredPath.trim());
        if (rawPath.isAbsolute()) {
            return rawPath.normalize();
        }

        Path workspacePath = Paths.get(System.getProperty("user.dir"));
        return workspacePath.resolve(rawPath).normalize();
    }
}
