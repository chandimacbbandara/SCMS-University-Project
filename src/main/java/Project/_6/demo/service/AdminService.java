package Project._6.demo.service;

import Project._6.demo.dto.AdminReplyDTO;
import Project._6.demo.entity.Admin;
import Project._6.demo.entity.AdminReply;
import Project._6.demo.entity.Concern;
import Project._6.demo.entity.User;
import Project._6.demo.repository.AdminRepository;
import Project._6.demo.repository.AdminReplyRepository;
import Project._6.demo.repository.ConcernRepository;
import Project._6.demo.repository.FeedbackRepository;
import Project._6.demo.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private static final String UPLOAD_DIR = "uploads/";

    private final ConcernRepository concernRepository;
    private final AdminRepository adminRepository;
    private final AdminReplyRepository adminReplyRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public AdminService(ConcernRepository concernRepository,
                        AdminRepository adminRepository,
                        AdminReplyRepository adminReplyRepository,
                        FeedbackRepository feedbackRepository,
                        UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        NotificationService notificationService) {
        this.concernRepository = concernRepository;
        this.adminRepository = adminRepository;
        this.adminReplyRepository = adminReplyRepository;
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
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
            concerns = concernRepository.findByStatusAndCreatedTimeBetweenOrderByCreatedTimeDesc(status, from, to);
        } else if (hasStatus) {
            concerns = concernRepository.findByStatusOrderByCreatedTimeDesc(status);
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
                    .filter(c -> category.equals(c.getCategory()))
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
        feedbackRepository.findByConcern_ConcernId(concernId)
                .ifPresent(feedbackRepository::delete);

        List<AdminReply> replies = adminReplyRepository.findByConcern_ConcernIdOrderByReplyTimeDesc(concernId);
        if (!replies.isEmpty()) {
            adminReplyRepository.deleteAll(replies);
        }

        notificationService.deleteByConcernId(concernId);

        concernRepository.deleteById(concern.getConcernId());
        concernRepository.flush();
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
     * Submit a reply to a concern and update its status
     */
    @Transactional
    public AdminReply submitReply(AdminReplyDTO dto, MultipartFile resolutionFile) {
        Concern concern = getConcernById(dto.getConcernId());

        // Get or create a default admin for now
        Admin admin = getOrCreateDefaultAdmin();

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
        Concern saved = concernRepository.save(concern);

        // Trigger notification: Step 2 - Concern In Progress (Mark as Read)
        if ("In Progress".equals(status)) {
            notificationService.notifyConcernInProgress(saved);
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
        concern.setCategory(normalizedCategory);
        return concernRepository.save(concern);
    }

    /**
     * Get or create a default admin account for development
     */
    private Admin getOrCreateDefaultAdmin() {
        Optional<Admin> existingAdmin = adminRepository.findByStaffId("ADMIN001");
        if (existingAdmin.isPresent()) {
            return existingAdmin.get();
        }

        // Create a default admin user
        User adminUser = new User();
        adminUser.setUserId(userRepository.getNextUserId());
        adminUser.setEmail("admin@akb.edu");
        adminUser.setPassword(passwordEncoder.encode("admin_" + UUID.randomUUID().toString().substring(0, 8)));
        adminUser.setFirstName("System");
        adminUser.setLastName("Admin");
        adminUser.setRegistrationStatus("APPROVED");
        adminUser = userRepository.save(adminUser);

        Admin admin = new Admin();
        admin.setUser(adminUser);
        admin.setStaffId("ADMIN001");
        return adminRepository.save(admin);
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
        return concernRepository.countByStatus("In Progress");
    }

    public long getCompleteCount() {
        return concernRepository.countByStatus("Complete");
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
}
