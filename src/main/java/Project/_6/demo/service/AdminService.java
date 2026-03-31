package Project._6.demo.service;

import Project._6.demo.dto.AdminReplyDTO;
import Project._6.demo.entity.Admin;
import Project._6.demo.entity.AdminReply;
import Project._6.demo.entity.Concern;
import Project._6.demo.entity.Feedback;
import Project._6.demo.repository.AdminRepository;
import Project._6.demo.repository.AdminReplyRepository;
import Project._6.demo.repository.ConcernRepository;
import Project._6.demo.repository.FeedbackRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private static final String UPLOAD_DIR = "uploads/";

    private final ConcernRepository concernRepository;
    private final AdminRepository adminRepository;
    private final AdminReplyRepository adminReplyRepository;
    private final FeedbackRepository feedbackRepository;
    private final NotificationService notificationService;
    private final ConcernMeetingService concernMeetingService;

    public AdminService(ConcernRepository concernRepository,
                        AdminRepository adminRepository,
                        AdminReplyRepository adminReplyRepository,
                        FeedbackRepository feedbackRepository,
                        NotificationService notificationService,
                        ConcernMeetingService concernMeetingService) {
        this.concernRepository = concernRepository;
        this.adminRepository = adminRepository;
        this.adminReplyRepository = adminReplyRepository;
        this.feedbackRepository = feedbackRepository;
        this.notificationService = notificationService;
        this.concernMeetingService = concernMeetingService;
    }

    /**
     * Get all concerns ordered by newest first, with Complete at the bottom
     */
    public List<Concern> getAllConcerns() {
        return sortCompleteLast(concernRepository.findAllByOrderByCreatedTimeDesc());
    }

    /**
     * Get concerns by status
     */
    public List<Concern> getConcernsByStatus(String status) {
        if (isInProgressBucket(status)) {
            List<Concern> active = concernRepository.findByStatusOrderByCreatedTimeDesc("In Progress");
            active.addAll(concernRepository.findByStatusOrderByCreatedTimeDesc("Meeting Scheduled"));
            return sortCompleteLast(active);
        }
        return sortCompleteLast(concernRepository.findByStatusOrderByCreatedTimeDesc(status));
    }

    /**
     * Get concerns filtered by status and/or time range
     */
    public List<Concern> getFilteredConcerns(String status, String category, LocalDateTime from, LocalDateTime to) {
        boolean hasStatus = status != null && !status.isEmpty() && !status.equals("All");
        boolean hasCategory = category != null && !category.isEmpty() && !category.equals("All");
        boolean hasTime = from != null && to != null;

        List<Concern> concerns;
        if (hasStatus && hasTime) {
            if (isInProgressBucket(status)) {
                concerns = concernRepository.findByStatusAndCreatedTimeBetweenOrderByCreatedTimeDesc("In Progress", from, to);
                concerns.addAll(concernRepository.findByStatusAndCreatedTimeBetweenOrderByCreatedTimeDesc("Meeting Scheduled", from, to));
            } else {
                concerns = concernRepository.findByStatusAndCreatedTimeBetweenOrderByCreatedTimeDesc(status, from, to);
            }
        } else if (hasStatus) {
            if (isInProgressBucket(status)) {
                concerns = concernRepository.findByStatusOrderByCreatedTimeDesc("In Progress");
                concerns.addAll(concernRepository.findByStatusOrderByCreatedTimeDesc("Meeting Scheduled"));
            } else {
                concerns = concernRepository.findByStatusOrderByCreatedTimeDesc(status);
            }
        } else if (hasTime) {
            concerns = concernRepository.findByCreatedTimeBetweenOrderByCreatedTimeDesc(from, to);
        } else {
            concerns = concernRepository.findAllByOrderByCreatedTimeDesc();
        }

        // --- FIXED: Migration of data if category is missing ---
        boolean needsSave = false;
        for (Concern c : concerns) {
            if ((c.getCategory() == null || c.getCategory().isEmpty()) && c.getStudent() != null) {
                c.setCategory(c.getStudent().getCategory());
                needsSave = true;
            }
        }
        if (needsSave) {
            concernRepository.saveAll(concerns);
        }
        // --------------------------------------------------------

        if (hasCategory) {
            concerns = concerns.stream()
                    .filter(c -> Objects.equals(category, c.getCategory()))
                    .collect(Collectors.toList());
        }

        return sortCompleteLast(concerns);
    }

    /**
     * Get a single concern by ID
     */
    public Concern getConcernById(Integer concernId) {
        return concernRepository.findById(concernId)
                .orElseThrow(() -> new RuntimeException("Concern not found with ID: " + concernId));
    }

    /**
     * Get replies for a specific concern
     */
    public List<AdminReply> getRepliesForConcern(Integer concernId) {
        return adminReplyRepository.findByConcern_ConcernIdOrderByReplyTimeDesc(concernId);
    }

    /**
     * Delete a reply for a specific concern.
     */
    @Transactional
    public void deleteReply(Integer concernId, Integer replyId) {
        AdminReply reply = adminReplyRepository.findById(replyId)
                .orElseThrow(() -> new RuntimeException("Reply not found with ID: " + replyId));

        if (reply.getConcern() == null || !concernId.equals(reply.getConcern().getConcernId())) {
            throw new RuntimeException("Reply does not belong to concern ID: " + concernId);
        }

        adminReplyRepository.delete(reply);
    }

    /**
     * Delete a concern and related data (admin replies and feedback).
     */
    @Transactional
    public void deleteConcern(Integer concernId) {
        Concern concern = getConcernById(concernId);

        // Explicitly remove dependent records first to satisfy FK constraints.
        feedbackRepository.deleteByConcern_ConcernId(concernId);

        List<AdminReply> replies = adminReplyRepository.findByConcern_ConcernIdOrderByReplyTimeDesc(concernId);
        if (!replies.isEmpty()) {
            adminReplyRepository.deleteAll(replies);
        }

        concernMeetingService.deleteByConcernId(concernId);

        notificationService.deleteByConcernId(concernId);

        concernRepository.deleteById(concern.getConcernId());
        concernRepository.flush();

        notificationService.sendConcernDeletedEmail(concern);
    }

    /**
     * Update only the latest reply for a specific concern.
     */
    @Transactional
    public void updateLatestReply(Integer concernId, Integer replyId, String replyMessage) {
        if (replyMessage == null || replyMessage.trim().isEmpty()) {
            throw new RuntimeException("Reply message cannot be empty.");
        }

        AdminReply latestReply = adminReplyRepository.findFirstByConcern_ConcernIdOrderByReplyTimeDesc(concernId)
                .orElseThrow(() -> new RuntimeException("No replies found for concern ID: " + concernId));

        if (!replyId.equals(latestReply.getReplyId())) {
            throw new RuntimeException("Only the latest reply can be updated.");
        }

        latestReply.setReplyMessage(replyMessage.trim());
        adminReplyRepository.save(latestReply);
    }

    /**
     * Submit a reply as the currently logged-in admin and update concern status.
     */
    @Transactional
    public AdminReply submitReply(AdminReplyDTO dto, MultipartFile resolutionFile, Integer adminUserId) {
        Concern concern = getConcernById(dto.getConcernId());

        if (adminUserId == null) {
            throw new RuntimeException("Admin identity is missing. Please log in again.");
        }

        Admin admin = adminRepository.findById(adminUserId)
                .orElseThrow(() -> new RuntimeException("Admin account not found. Please log in again."));

        // Create reply
        AdminReply reply = new AdminReply();
        reply.setReplyMessage(dto.getReplyMessage());
        reply.setConcern(concern);
        reply.setAdmin(admin);

        if (resolutionFile != null && !resolutionFile.isEmpty()) {
            try {
                reply.setResolutionScreenshotPath(saveReplyAttachment(resolutionFile));
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload reply attachment.");
            }
        }

        // Update concern status
        if (dto.getNewStatus() != null && !dto.getNewStatus().isEmpty()) {
            concern.setStatus(dto.getNewStatus());
        } else {
            concern.setStatus("In Progress");
        }

        // Assign admin to concern if not already assigned
        if (concern.getAdmin() == null) {
            concern.setAdmin(admin);
        }

        concernRepository.save(concern);
        AdminReply savedReply = adminReplyRepository.save(reply);

        // Trigger notification: Step 3 - Concern Complete (when reply is submitted)
        if ("Complete".equals(concern.getStatus())) {
            notificationService.notifyConcernComplete(concern);
        }

        return savedReply;
    }

    private String saveReplyAttachment(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String storedFilename = UUID.randomUUID().toString() + extension;

        Path filePath = uploadPath.resolve(storedFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return storedFilename;
    }

    /**
     * Update concern status
     */
    @Transactional
    public Concern updateConcernStatus(Integer concernId, String status) {
        Concern concern = getConcernById(concernId);
        concern.setStatus(status);

        if ("Complete".equalsIgnoreCase(status)) {
            if ("BOOKED".equalsIgnoreCase(concern.getMeetingStatus())) {
                concern.setMeetingStatus("MEETING_COMPLETED");
            }
        }

        Concern saved = concernRepository.save(concern);

        // Trigger notification: Step 2 - Concern In Progress (Mark as Read)
        if ("In Progress".equals(status)) {
            notificationService.notifyConcernInProgress(saved);
        } else if ("Complete".equals(status)) {
            notificationService.notifyConcernComplete(saved);
        }

        return saved;
    }

    /**
     * Update concern category/department.
     */
    @Transactional
    public Concern updateConcernCategory(Integer concernId, String category) {
        List<String> allowedCategories = Arrays.asList(
                "Institute Problem",
                "Registration",
                "Administrative",
                "Financial",
                "Other",
                "Education (Creative and IT)"
        );

        if (category == null || category.trim().isEmpty()) {
            throw new RuntimeException("Category is required.");
        }

        String normalizedCategory = category.trim();
        if (!allowedCategories.contains(normalizedCategory)) {
            throw new RuntimeException("Invalid category selected.");
        }

        Concern concern = getConcernById(concernId);
        String previousCategory = concern.getCategory();
        concern.setCategory(normalizedCategory);
        Concern savedConcern = concernRepository.save(concern);

        String oldCategoryNormalized = previousCategory == null ? "" : previousCategory.trim();
        String newCategoryNormalized = normalizedCategory.trim();
        if (!oldCategoryNormalized.equalsIgnoreCase(newCategoryNormalized)) {
            notificationService.sendConcernDepartmentChangedEmail(savedConcern, previousCategory, normalizedCategory);
        }

        return savedConcern;
    }

    /**
     * Get dashboard statistics
     */
    public long getTotalConcerns() {
        return concernRepository.count();
    }

    public long getPendingCount() {
        return concernRepository.countByStatus("Pending");
    }

    public long getInProgressCount() {
        return concernRepository.countByStatus("In Progress") + concernRepository.countByStatus("Meeting Scheduled");
    }

    public long getCompleteCount() {
        return concernRepository.countByStatus("Complete");
    }

    public Optional<Admin> getAdminByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return adminRepository.findByUser_EmailIgnoreCase(email.trim());
    }

    public List<Feedback> getFeedbackHistoryByAdminUserId(Integer adminUserId) {
        if (adminUserId == null) {
            return List.of();
        }
        return feedbackRepository.findByRatedAdminUserId(adminUserId);
    }

    public List<Feedback> getFeedbackHistory() {
        return feedbackRepository.findAllByOrderBySubmissionTimeDesc();
    }

    public Map<String, Double> getDepartmentAverageRatings(List<Feedback> feedbackHistory) {
        return feedbackHistory.stream()
                .filter(feedback -> feedback.getConcern() != null)
                .collect(Collectors.groupingBy(
                        feedback -> normalizeDepartment(feedback.getConcern().getCategory()),
                        LinkedHashMap::new,
                        Collectors.averagingDouble(Feedback::getRating)
                ));
    }

    public Map<String, Integer[]> getDepartmentStarCounts(List<Feedback> feedbackHistory) {
        Map<String, Integer[]> starCounts = new LinkedHashMap<>();

        for (Feedback feedback : feedbackHistory) {
            if (feedback == null || feedback.getConcern() == null) {
                continue;
            }

            int rating = feedback.getRating() == null ? 0 : feedback.getRating();
            if (rating < 1 || rating > 5) {
                continue;
            }

            String department = normalizeDepartment(feedback.getConcern().getCategory());
            Integer[] counts = starCounts.computeIfAbsent(department, key -> new Integer[]{0, 0, 0, 0, 0, 0});
            counts[rating] = counts[rating] + 1;
        }

        return starCounts;
    }

    private String normalizeDepartment(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "Other";
        }
        return category.trim();
    }

    /**
     * Sort concerns so that Complete ones appear at the bottom of the list
     */
    private List<Concern> sortCompleteLast(List<Concern> concerns) {
        return concerns.stream()
                .sorted(Comparator.comparing((Concern c) -> "Complete".equals(c.getStatus()) ? 1 : 0)
                        .thenComparing(Comparator.comparing(Concern::getCreatedTime).reversed()))
                .collect(Collectors.toList());
    }

    private boolean isInProgressBucket(String status) {
        return "In Progress".equalsIgnoreCase(status);
    }
}
