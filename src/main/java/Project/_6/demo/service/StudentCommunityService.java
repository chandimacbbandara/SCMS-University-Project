package Project._6.demo.service;

import Project._6.demo.dto.CommunityModerationResultDTO;
import Project._6.demo.dto.CommunityPostDTO;
import Project._6.demo.dto.CommunityReplyDTO;
import Project._6.demo.entity.*;
import Project._6.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class StudentCommunityService {

    public static final String RULES_VERSION = "1.0";

    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "Academic", "Registration", "Finance", "Hostel", "IT Services", "Other"
    );

    private final StudentRepository studentRepository;
    private final StudentCommunityPostRepository postRepository;
    private final StudentCommunityReplyRepository replyRepository;
    private final StudentCommunityRulesAcceptanceRepository rulesAcceptanceRepository;
    private final StudentCommunityModerationLogRepository moderationLogRepository;
    private final CommunityModerationService moderationService;

    public StudentCommunityService(StudentRepository studentRepository,
                                   StudentCommunityPostRepository postRepository,
                                   StudentCommunityReplyRepository replyRepository,
                                   StudentCommunityRulesAcceptanceRepository rulesAcceptanceRepository,
                                   StudentCommunityModerationLogRepository moderationLogRepository,
                                   CommunityModerationService moderationService) {
        this.studentRepository = studentRepository;
        this.postRepository = postRepository;
        this.replyRepository = replyRepository;
        this.rulesAcceptanceRepository = rulesAcceptanceRepository;
        this.moderationLogRepository = moderationLogRepository;
        this.moderationService = moderationService;
    }

    public boolean hasAcceptedRules(Integer userId) {
        return rulesAcceptanceRepository.findTopByStudent_UserIdOrderByAcceptedAtDesc(userId)
                .map(acceptance -> RULES_VERSION.equals(acceptance.getRulesVersion()))
                .orElse(false);
    }

    @Transactional
    public void acceptRules(Integer userId) {
        Student student = getStudent(userId);

        Optional<StudentCommunityRulesAcceptance> existing =
                rulesAcceptanceRepository.findTopByStudent_UserIdOrderByAcceptedAtDesc(userId);

        StudentCommunityRulesAcceptance acceptance = existing.orElseGet(StudentCommunityRulesAcceptance::new);
        acceptance.setStudent(student);
        acceptance.setRulesVersion(RULES_VERSION);
        acceptance.setAcceptedAt(LocalDateTime.now());
        rulesAcceptanceRepository.save(acceptance);
    }

    public List<StudentCommunityPost> getActivePosts() {
        return postRepository.findByStatusOrderByCreatedTimeDesc("ACTIVE");
    }

    public Map<Integer, List<StudentCommunityReply>> getRepliesMap(List<StudentCommunityPost> posts) {
        Map<Integer, List<StudentCommunityReply>> map = new HashMap<>();
        for (StudentCommunityPost post : posts) {
            List<StudentCommunityReply> replies =
                    replyRepository.findByPost_PostIdAndStatusOrderByCreatedTimeAsc(post.getPostId(), "ACTIVE");
            map.put(post.getPostId(), replies);
        }
        return map;
    }

    public Set<String> getAllowedCategories() {
        return ALLOWED_CATEGORIES;
    }

    @Transactional
    public void createPost(Integer userId, CommunityPostDTO dto) {
        validatePost(dto);

        Student student = getStudent(userId);
        CommunityModerationResultDTO moderation = moderationService.moderateText(buildPostModerationPayload(dto), "post");
        saveModerationLog(student, "POST", moderation);

        if (!"ALLOW".equals(moderation.getDecision())) {
            throw new IllegalArgumentException(moderation.getReason());
        }

        StudentCommunityPost post = new StudentCommunityPost();
        post.setStudent(student);
        post.setTitle(dto.getTitle().trim());
        post.setMessage(dto.getMessage().trim());
        post.setCategory(dto.getCategory().trim());
        post.setStatus("ACTIVE");
        postRepository.save(post);
    }

    @Transactional
    public void updatePost(Integer postId, Integer userId, CommunityPostDTO dto) {
        validatePost(dto);

        StudentCommunityPost post = postRepository.findByPostIdAndStudent_UserId(postId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found or access denied."));

        CommunityModerationResultDTO moderation = moderationService.moderateText(buildPostModerationPayload(dto), "post");
        saveModerationLog(post.getStudent(), "POST", moderation);

        if (!"ALLOW".equals(moderation.getDecision())) {
            throw new IllegalArgumentException(moderation.getReason());
        }

        post.setTitle(dto.getTitle().trim());
        post.setMessage(dto.getMessage().trim());
        post.setCategory(dto.getCategory().trim());
        postRepository.save(post);
    }

    @Transactional
    public void deletePost(Integer postId, Integer userId) {
        StudentCommunityPost post = postRepository.findByPostIdAndStudent_UserId(postId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found or access denied."));
        post.setStatus("DELETED");
        postRepository.save(post);
    }

    @Transactional
    public void createReply(Integer postId, Integer userId, CommunityReplyDTO dto) {
        validateReply(dto);

        Student student = getStudent(userId);
        StudentCommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found."));

        if (!"ACTIVE".equals(post.getStatus())) {
            throw new IllegalArgumentException("This post is no longer available.");
        }

        CommunityModerationResultDTO moderation = moderationService.moderateText(dto.getMessage(), "reply");
        saveModerationLog(student, "REPLY", moderation);

        if (!"ALLOW".equals(moderation.getDecision())) {
            throw new IllegalArgumentException(moderation.getReason());
        }

        StudentCommunityReply reply = new StudentCommunityReply();
        reply.setPost(post);
        reply.setStudent(student);
        reply.setMessage(dto.getMessage().trim());
        reply.setStatus("ACTIVE");
        replyRepository.save(reply);
    }

    @Transactional
    public void updateReply(Integer replyId, Integer userId, CommunityReplyDTO dto) {
        validateReply(dto);

        StudentCommunityReply reply = replyRepository.findByReplyIdAndStudent_UserId(replyId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Reply not found or access denied."));

        CommunityModerationResultDTO moderation = moderationService.moderateText(dto.getMessage(), "reply");
        saveModerationLog(reply.getStudent(), "REPLY", moderation);

        if (!"ALLOW".equals(moderation.getDecision())) {
            throw new IllegalArgumentException(moderation.getReason());
        }

        reply.setMessage(dto.getMessage().trim());
        replyRepository.save(reply);
    }

    @Transactional
    public void deleteReply(Integer replyId, Integer userId) {
        StudentCommunityReply reply = replyRepository.findByReplyIdAndStudent_UserId(replyId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Reply not found or access denied."));
        reply.setStatus("DELETED");
        replyRepository.save(reply);
    }

    @Transactional
    public CommunityModerationResultDTO runLiveModeration(Integer userId, String message, String contentType) {
        Student student = getStudent(userId);
        CommunityModerationResultDTO result = moderationService.moderateLiveText(message, contentType);
        saveModerationLog(student, "LIVE", result);
        return result;
    }

    private void validatePost(CommunityPostDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Post details are required.");
        }

        String title = dto.getTitle() == null ? "" : dto.getTitle().trim();
        String message = dto.getMessage() == null ? "" : dto.getMessage().trim();
        String category = dto.getCategory() == null ? "" : dto.getCategory().trim();

        if (title.isBlank()) {
            throw new IllegalArgumentException("Title is required.");
        }
        if (title.length() > 160) {
            throw new IllegalArgumentException("Title must be 160 characters or less.");
        }

        if (message.isBlank()) {
            throw new IllegalArgumentException("Message is required.");
        }
        if (message.length() > 2000) {
            throw new IllegalArgumentException("Message must be 2000 characters or less.");
        }

        if (!ALLOWED_CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("Please select a valid category.");
        }
    }

    private void validateReply(CommunityReplyDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Reply message is required.");
        }

        String message = dto.getMessage() == null ? "" : dto.getMessage().trim();
        if (message.isBlank()) {
            throw new IllegalArgumentException("Reply message is required.");
        }
        if (message.length() > 1200) {
            throw new IllegalArgumentException("Reply must be 1200 characters or less.");
        }
    }

    private String buildPostModerationPayload(CommunityPostDTO dto) {
        String title = dto.getTitle() == null ? "" : dto.getTitle().trim();
        String message = dto.getMessage() == null ? "" : dto.getMessage().trim();
        return "Title: " + title + "\nMessage: " + message;
    }

    private Student getStudent(Integer userId) {
        return studentRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Student session is invalid. Please log in again."));
    }

    private void saveModerationLog(Student student, String contentType, CommunityModerationResultDTO result) {
        StudentCommunityModerationLog log = new StudentCommunityModerationLog();
        log.setStudent(student);
        log.setContentType(contentType);
        log.setDecision(result.getDecision());
        log.setReasons(result.getReason());
        log.setRiskScore(result.getRiskScore());
        moderationLogRepository.save(log);
    }
}
