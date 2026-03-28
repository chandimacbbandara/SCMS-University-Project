package Project._6.demo.service;

import Project._6.demo.dto.FeedbackDTO;
import Project._6.demo.entity.AdminReply;
import Project._6.demo.entity.Concern;
import Project._6.demo.entity.Feedback;
import Project._6.demo.repository.AdminReplyRepository;
import Project._6.demo.repository.ConcernRepository;
import Project._6.demo.repository.FeedbackRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;

@Service
public class FeedbackService {

    private static final long FEEDBACK_EDIT_WINDOW_HOURS = 24;
    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;
    private static final int LOW_RATING_THRESHOLD = 3;
    private static final int MIN_CHARACTERS = 10;
    private static final int MAX_CHARACTERS = 100;

    private final FeedbackRepository feedbackRepository;
    private final ConcernRepository concernRepository;
    private final AdminReplyRepository adminReplyRepository;
    private final FeedbackModerationService feedbackModerationService;

    public FeedbackService(FeedbackRepository feedbackRepository,
                           ConcernRepository concernRepository,
                           AdminReplyRepository adminReplyRepository,
                           FeedbackModerationService feedbackModerationService) {
        this.feedbackRepository = feedbackRepository;
        this.concernRepository = concernRepository;
        this.adminReplyRepository = adminReplyRepository;
        this.feedbackModerationService = feedbackModerationService;
    }

    @Transactional
    public Feedback submitFeedback(FeedbackDTO dto) {
        return submitFeedback(dto, null);
    }

    @Transactional
    public Feedback submitFeedback(FeedbackDTO dto, Integer studentUserId) {
        if (feedbackRepository.existsByConcern_ConcernId(dto.getConcernId())) {
            throw new RuntimeException("Feedback already submitted for this concern.");
        }

        Integer rating = dto.getRating();
        String comments = normalizeComments(dto.getComments());
        validateFeedbackInput(rating, comments);

        Concern concern;
        if (studentUserId != null) {
            concern = validateConcernOwnership(dto.getConcernId(), studentUserId);
        } else {
            concern = concernRepository.findById(dto.getConcernId())
                    .orElseThrow(() -> new RuntimeException("Concern not found."));
        }

        AdminReply latestReply = adminReplyRepository.findFirstByConcern_ConcernIdOrderByReplyTimeDesc(concern.getConcernId())
            .orElseThrow(() -> new RuntimeException("Feedback can only be submitted after an admin reply is posted."));

        Feedback feedback = new Feedback();
        feedback.setConcern(concern);
        feedback.setAdminReply(latestReply);
        feedback.setRating(rating);
        feedback.setComments(comments);
        return feedbackRepository.save(feedback);
    }

    @Transactional
    public Feedback updateFeedback(Integer concernId, Integer studentUserId, FeedbackDTO dto) {
        validateConcernOwnership(concernId, studentUserId);

        Integer rating = dto.getRating();
        String comments = normalizeComments(dto.getComments());
        validateFeedbackInput(rating, comments);

        Feedback feedback = feedbackRepository.findTopByConcern_ConcernIdOrderBySubmissionTimeDesc(concernId)
                .orElseThrow(() -> new RuntimeException("No feedback found to update."));

        if (!canStudentUpdateFeedback(feedback, concernId)) {
            throw new RuntimeException("Feedback can only be updated after a new admin reply is posted.");
        }

        AdminReply latestReply = adminReplyRepository.findFirstByConcern_ConcernIdOrderByReplyTimeDesc(concernId)
                .orElseThrow(() -> new RuntimeException("No admin reply found for this concern."));

        feedback.setRating(rating);
        feedback.setComments(comments);
        feedback.setAdminReply(latestReply);
        feedback.setSubmissionTime(LocalDateTime.now());
        return feedbackRepository.save(feedback);
    }

    @Transactional
    public void deleteFeedback(Integer concernId, Integer studentUserId) {
        validateConcernOwnership(concernId, studentUserId);

        Feedback feedback = feedbackRepository.findTopByConcern_ConcernIdOrderBySubmissionTimeDesc(concernId)
                .orElseThrow(() -> new RuntimeException("No feedback found to delete."));

        if (!canStudentModifyFeedback(feedback, concernId)) {
            throw new RuntimeException("Feedback can only be deleted within 24 hours unless a new admin reply is posted.");
        }

        feedbackRepository.delete(feedback);
    }

    public Optional<Feedback> getFeedbackByConcernId(Integer concernId) {
        return feedbackRepository.findTopByConcern_ConcernIdOrderBySubmissionTimeDesc(concernId);
    }

    /**
     * Build a map of concernId -> Feedback for displaying on the history page.
     */
    public Map<Integer, Feedback> getFeedbackMap(List<Concern> concerns) {
        Map<Integer, Feedback> map = new HashMap<>();
        for (Concern c : concerns) {
            Optional<Feedback> fb = feedbackRepository.findTopByConcern_ConcernIdOrderBySubmissionTimeDesc(c.getConcernId());
            fb.ifPresent(feedback -> map.put(c.getConcernId(), feedback));
        }
        return map;
    }

    public Map<Integer, Boolean> getFeedbackActionAllowedMap(List<Concern> concerns) {
        Map<Integer, Boolean> map = new HashMap<>();

        for (Concern concern : concerns) {
            Optional<Feedback> feedbackOpt = feedbackRepository.findTopByConcern_ConcernIdOrderBySubmissionTimeDesc(concern.getConcernId());
            map.put(concern.getConcernId(), feedbackOpt
                    .map(feedback -> canStudentModifyFeedback(feedback, concern.getConcernId()))
                    .orElse(false));
        }

        return map;
    }

    public Map<Integer, Boolean> getFeedbackUpdateAllowedMap(List<Concern> concerns) {
        Map<Integer, Boolean> map = new HashMap<>();

        for (Concern concern : concerns) {
            Optional<Feedback> feedbackOpt = feedbackRepository.findTopByConcern_ConcernIdOrderBySubmissionTimeDesc(concern.getConcernId());
            map.put(concern.getConcernId(), feedbackOpt
                    .map(feedback -> canStudentUpdateFeedback(feedback, concern.getConcernId()))
                    .orElse(false));
        }

        return map;
    }

    public List<Feedback> getTopPositiveFeedbackForHome(int limit) {
        int safeLimit = Math.max(1, limit);
        return feedbackRepository.findAllByOrderBySubmissionTimeDesc().stream()
                .filter(feedback -> feedback != null && feedback.getRating() != null && feedback.getRating() >= 4)
                .filter(feedback -> feedback.getComments() != null && !feedback.getComments().isBlank())
            .sorted(Comparator
                .comparing(Feedback::getRating, Comparator.reverseOrder())
                .thenComparing(Feedback::getSubmissionTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(safeLimit)
                .toList();
    }

    private Concern validateConcernOwnership(Integer concernId, Integer studentUserId) {
        Concern concern = concernRepository.findById(concernId)
                .orElseThrow(() -> new RuntimeException("Concern not found."));

        boolean isOwner = concern.getStudent() != null
                && concern.getStudent().getUser() != null
                && concern.getStudent().getUser().getUserId() != null
                && concern.getStudent().getUser().getUserId().equals(studentUserId);

        if (!isOwner) {
            throw new RuntimeException("You are not allowed to modify feedback for this concern.");
        }

        return concern;
    }

    private void validateFeedbackInput(Integer rating, String comments) {
        validateRating(rating);

        if (rating <= LOW_RATING_THRESHOLD && (comments == null || comments.isBlank())) {
            throw new RuntimeException("Feedback comment is required when rating is 3 stars or below.");
        }

        if (comments != null && !comments.isBlank()) {
            int characters = countCharacters(comments);
            if (characters < MIN_CHARACTERS || characters > MAX_CHARACTERS) {
                throw new RuntimeException("Feedback comment must be between 10 and 100 characters.");
            }

            var moderationResult = feedbackModerationService.moderateFeedbackText(comments);
            String decision = moderationResult.getDecision();
            String reason = moderationResult.getReason();
            if ("BLOCK".equalsIgnoreCase(decision)) {
                throw new RuntimeException(reason);
            }
        }
    }

    private void validateRating(Integer rating) {
        if (rating == null || rating < MIN_RATING || rating > MAX_RATING) {
            throw new RuntimeException("Rating must be between 1 and 5.");
        }
    }

    private String normalizeComments(String comments) {
        if (comments == null) {
            return null;
        }
        String normalized = comments.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    private int countCharacters(String comments) {
        if (comments == null || comments.isBlank()) {
            return 0;
        }
        return comments.trim().length();
    }

    private boolean canStudentModifyFeedback(Feedback feedback, Integer concernId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime feedbackTime = feedback.getSubmissionTime();

        boolean within24Hours = feedbackTime != null
                && !feedbackTime.isBefore(now.minusHours(FEEDBACK_EDIT_WINDOW_HOURS));

        boolean hasNewAdminReply = hasNewAdminReplyAfterFeedback(feedbackTime, concernId);

        return within24Hours || hasNewAdminReply;
    }

    private boolean canStudentUpdateFeedback(Feedback feedback, Integer concernId) {
        return hasNewAdminReplyAfterFeedback(feedback.getSubmissionTime(), concernId);
    }

    private boolean hasNewAdminReplyAfterFeedback(LocalDateTime feedbackTime, Integer concernId) {
        Optional<AdminReply> latestReply = adminReplyRepository.findFirstByConcern_ConcernIdOrderByReplyTimeDesc(concernId);
        return latestReply.isPresent()
                && latestReply.get().getReplyTime() != null
                && feedbackTime != null
                && latestReply.get().getReplyTime().isAfter(feedbackTime);
    }
}
