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
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;
import java.sql.Timestamp;

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
    private final JdbcTemplate jdbcTemplate;

    public FeedbackService(FeedbackRepository feedbackRepository,
                           ConcernRepository concernRepository,
                           AdminReplyRepository adminReplyRepository,
                           FeedbackModerationService feedbackModerationService,
                           JdbcTemplate jdbcTemplate) {
        this.feedbackRepository = feedbackRepository;
        this.concernRepository = concernRepository;
        this.adminReplyRepository = adminReplyRepository;
        this.feedbackModerationService = feedbackModerationService;
        this.jdbcTemplate = jdbcTemplate;
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
        Feedback saved = feedbackRepository.save(feedback);

        // Keep legacy feedback table in sync for environments that still read from `feedback`.
        syncLegacyFeedbackRow(concern, latestReply, rating, comments, saved.getSubmissionTime(), studentUserId);
        return saved;
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
        Feedback saved = feedbackRepository.save(feedback);

        syncLegacyFeedbackRow(feedback.getConcern(), latestReply, rating, comments, saved.getSubmissionTime(), studentUserId);
        return saved;
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
        markLegacyFeedbackDeleted(concernId, studentUserId);
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

    private void syncLegacyFeedbackRow(Concern concern,
                                       AdminReply latestReply,
                                       Integer rating,
                                       String comments,
                                       LocalDateTime submissionTime,
                                       Integer studentUserId) {
        try {
            if (!legacyFeedbackTableExists()) {
                return;
            }

            Integer concernId = concern != null ? concern.getConcernId() : null;
            Integer customerId = resolveStudentUserId(concern, studentUserId);
            if (customerId == null) {
                return;
            }

            ensureLegacyCustomerRow(customerId);

            String customerName = resolveStudentDisplayName(concern, customerId);
            Integer replyId = latestReply != null ? latestReply.getReplyId() : null;
            String replyMessage = latestReply != null ? latestReply.getReplyMessage() : null;
            Long adminId = (latestReply != null && latestReply.getAdmin() != null && latestReply.getAdmin().getUserId() != null)
                    ? latestReply.getAdmin().getUserId().longValue()
                    : null;
            LocalDateTime replyTime = latestReply != null ? latestReply.getReplyTime() : null;
            Timestamp nowTs = Timestamp.valueOf(LocalDateTime.now());
            Timestamp feedbackTs = Timestamp.valueOf(submissionTime != null ? submissionTime : LocalDateTime.now());
            Timestamp replyTs = replyTime != null ? Timestamp.valueOf(replyTime) : null;

            if (legacyFeedbackHasConcernColumn() && concernId != null) {
                Integer existingLegacyId = jdbcTemplate.query(
                    "SELECT feedback_id FROM feedback WHERE ConcernID_FK = ? AND customer_id = ? ORDER BY feedback_id DESC LIMIT 1",
                        rs -> rs.next() ? rs.getInt(1) : null,
                        concernId,
                        customerId);

                if (existingLegacyId != null) {
                    jdbcTemplate.update(
                            "UPDATE feedback SET rating = ?, comments = ?, feedback_date = ?, customer_id = ?, customer_name = ?, " +
                                    "is_deleted = 0, is_resolved = 1, reply = ?, reply_date = ?, admin_id = ?, ReplyID_FK = ?, " +
                                    "created_at = COALESCE(created_at, ?), ConcernID_FK = ? WHERE feedback_id = ?",
                            rating,
                            comments,
                            feedbackTs,
                            customerId,
                            customerName,
                            replyMessage,
                            replyTs,
                            adminId,
                            replyId,
                            nowTs,
                            concernId,
                            existingLegacyId);
                    return;
                }

                jdbcTemplate.update(
                        "INSERT INTO feedback (rating, comments, feedback_date, customer_id, customer_name, is_deleted, is_resolved, reply, reply_date, admin_id, ReplyID_FK, created_at, ConcernID_FK) " +
                                "VALUES (?, ?, ?, ?, ?, 0, 1, ?, ?, ?, ?, ?, ?)",
                        rating,
                        comments,
                        feedbackTs,
                        customerId,
                        customerName,
                        replyMessage,
                        replyTs,
                        adminId,
                        replyId,
                        nowTs,
                        concernId);
                return;
            }

            jdbcTemplate.update(
                    "INSERT INTO feedback (rating, comments, feedback_date, customer_id, customer_name, is_deleted, is_resolved, reply, reply_date, admin_id, ReplyID_FK, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, 0, 1, ?, ?, ?, ?, ?)",
                    rating,
                    comments,
                    feedbackTs,
                    customerId,
                    customerName,
                    replyMessage,
                    replyTs,
                    adminId,
                    replyId,
                    nowTs);
        } catch (Exception e) {
            System.out.println("Warning: Could not sync legacy feedback table: " + e.getMessage());
        }
    }

    private void markLegacyFeedbackDeleted(Integer concernId, Integer studentUserId) {
        try {
            if (!legacyFeedbackTableExists() || !legacyFeedbackHasConcernColumn() || concernId == null || studentUserId == null) {
                return;
            }

            jdbcTemplate.update(
                    "UPDATE feedback f JOIN (" +
                        "SELECT feedback_id FROM feedback WHERE ConcernID_FK = ? AND customer_id = ? ORDER BY feedback_id DESC LIMIT 1" +
                        ") latest ON f.feedback_id = latest.feedback_id " +
                        "SET f.is_deleted = 1",
                    concernId,
                    studentUserId);
        } catch (Exception e) {
            System.out.println("Warning: Could not mark legacy feedback as deleted: " + e.getMessage());
        }
    }

    private boolean legacyFeedbackTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'feedback'",
                Integer.class);
        return count != null && count > 0;
    }

    private boolean legacyFeedbackHasConcernColumn() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'feedback' AND COLUMN_NAME = 'ConcernID_FK'",
                Integer.class);
        return count != null && count > 0;
    }

    private boolean legacyCustomersTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'customers'",
                Integer.class);
        return count != null && count > 0;
    }

    private void ensureLegacyCustomerRow(Integer customerId) {
        if (customerId == null || !legacyCustomersTableExists()) {
            return;
        }

        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customers WHERE user_id = ?",
                Integer.class,
                customerId);

        if (existing == null || existing == 0) {
            jdbcTemplate.update("INSERT INTO customers (user_id) VALUES (?)", customerId);
        }
    }

    private Integer resolveStudentUserId(Concern concern, Integer fallbackStudentUserId) {
        if (fallbackStudentUserId != null) {
            return fallbackStudentUserId;
        }

        if (concern != null && concern.getStudent() != null && concern.getStudent().getUserId() != null) {
            return concern.getStudent().getUserId();
        }

        if (concern != null && concern.getStudent() != null && concern.getStudent().getUser() != null) {
            return concern.getStudent().getUser().getUserId();
        }

        return null;
    }

    private String resolveStudentDisplayName(Concern concern, Integer customerId) {
        if (concern != null && concern.getStudent() != null && concern.getStudent().getUser() != null) {
            String first = concern.getStudent().getUser().getFirstName();
            String last = concern.getStudent().getUser().getLastName();
            String full = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
            if (!full.isEmpty()) {
                return full;
            }
        }
        return "Student " + customerId;
    }
}
