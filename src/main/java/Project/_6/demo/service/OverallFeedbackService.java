package Project._6.demo.service;

import Project._6.demo.entity.OverallFeedback;
import Project._6.demo.repository.OverallFeedbackRepository;
import Project._6.demo.repository.StudentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class OverallFeedbackService {

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;
    private static final int MIN_COMMENT_LENGTH = 10;
    private static final int MAX_COMMENT_LENGTH = 500;

    private final OverallFeedbackRepository overallFeedbackRepository;
    private final StudentRepository studentRepository;
    private final FeedbackModerationService feedbackModerationService;

    public OverallFeedbackService(OverallFeedbackRepository overallFeedbackRepository,
                                  StudentRepository studentRepository,
                                  FeedbackModerationService feedbackModerationService) {
        this.overallFeedbackRepository = overallFeedbackRepository;
        this.studentRepository = studentRepository;
        this.feedbackModerationService = feedbackModerationService;
    }

    @Transactional
    public OverallFeedback submitFeedback(Integer studentUserId, int rating, String comment) {
        if (overallFeedbackRepository.existsByStudentUserId(studentUserId)) {
            throw new RuntimeException("You have already submitted your overall feedback. Only one feedback per student is allowed.");
        }

        validateInput(rating, comment);

        String studentName = resolveStudentName(studentUserId);

        OverallFeedback feedback = new OverallFeedback();
        feedback.setStudentUserId(studentUserId);
        feedback.setStudentName(studentName);
        feedback.setRating(rating);
        feedback.setComment(normalizeComment(comment));

        return overallFeedbackRepository.save(feedback);
    }

    public Optional<OverallFeedback> getStudentFeedback(Integer studentUserId) {
        return overallFeedbackRepository.findByStudentUserId(studentUserId);
    }

    public boolean hasStudentSubmitted(Integer studentUserId) {
        return overallFeedbackRepository.existsByStudentUserId(studentUserId);
    }

    @Transactional
    public OverallFeedback updateFeedback(Integer studentUserId, int rating, String comment) {
        OverallFeedback feedback = overallFeedbackRepository.findByStudentUserId(studentUserId)
                .orElseThrow(() -> new RuntimeException("Feedback not found. You need to submit feedback first."));

        validateInput(rating, comment);

        feedback.setRating(rating);
        feedback.setComment(normalizeComment(comment));
        return overallFeedbackRepository.save(feedback);
    }

    @Transactional
    public void deleteFeedback(Integer studentUserId) {
        OverallFeedback feedback = overallFeedbackRepository.findByStudentUserId(studentUserId)
                .orElseThrow(() -> new RuntimeException("Feedback not found."));
        
        overallFeedbackRepository.delete(feedback);
    }

    public List<OverallFeedback> getAllFeedback(String sortBy) {
        if (sortBy == null) {
            return overallFeedbackRepository.findAllByOrderBySubmittedAtDesc();
        }

        return switch (sortBy.toLowerCase()) {
            case "oldest" -> overallFeedbackRepository.findAllByOrderBySubmittedAtAsc();
            case "rating-high" -> overallFeedbackRepository.findAllByOrderByRatingDesc();
            case "rating-low" -> overallFeedbackRepository.findAllByOrderByRatingAsc();
            default -> overallFeedbackRepository.findAllByOrderBySubmittedAtDesc();
        };
    }

    /**
     * Get top N feedback for the home page (highest-rated first, then newest).
     */
    public List<OverallFeedback> getTopFeedbackForHome(int limit) {
        int safeLimit = Math.max(1, limit);
        return overallFeedbackRepository.findAllByOrderByRatingDesc().stream()
                .limit(safeLimit)
                .toList();
    }

    private void validateInput(int rating, String comment) {
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new RuntimeException("Rating must be between " + MIN_RATING + " and " + MAX_RATING + ".");
        }

        String normalized = normalizeComment(comment);

        if (normalized == null || normalized.isBlank()) {
            throw new RuntimeException("Please provide a comment with your feedback.");
        }

        int length = normalized.trim().length();
        if (length < MIN_COMMENT_LENGTH) {
            throw new RuntimeException("Comment must be at least " + MIN_COMMENT_LENGTH + " characters.");
        }
        if (length > MAX_COMMENT_LENGTH) {
            throw new RuntimeException("Comment must not exceed " + MAX_COMMENT_LENGTH + " characters.");
        }

        var moderationResult = feedbackModerationService.moderateFeedbackText(normalized);
        if ("BLOCK".equalsIgnoreCase(moderationResult.getDecision())) {
            throw new RuntimeException(moderationResult.getReason());
        }
    }

    private String normalizeComment(String comment) {
        if (comment == null) {
            return null;
        }
        String normalized = comment.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    private String resolveStudentName(Integer studentUserId) {
        return studentRepository.findById(studentUserId)
                .map(student -> {
                    if (student.getUser() != null) {
                        String first = student.getUser().getFirstName();
                        String last = student.getUser().getLastName();
                        String full = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
                        if (!full.isEmpty()) {
                            return full;
                        }
                    }
                    return "Student " + studentUserId;
                })
                .orElse("Student " + studentUserId);
    }
}
