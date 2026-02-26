package Project._6.demo.service;

import Project._6.demo.dto.AdminReplyDTO;
import Project._6.demo.entity.Admin;
import Project._6.demo.entity.AdminReply;
import Project._6.demo.entity.Concern;
import Project._6.demo.entity.User;
import Project._6.demo.repository.AdminRepository;
import Project._6.demo.repository.AdminReplyRepository;
import Project._6.demo.repository.ConcernRepository;
import Project._6.demo.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdminService {

    private final ConcernRepository concernRepository;
    private final AdminRepository adminRepository;
    private final AdminReplyRepository adminReplyRepository;
    private final UserRepository userRepository;

    public AdminService(ConcernRepository concernRepository,
                        AdminRepository adminRepository,
                        AdminReplyRepository adminReplyRepository,
                        UserRepository userRepository) {
        this.concernRepository = concernRepository;
        this.adminRepository = adminRepository;
        this.adminReplyRepository = adminReplyRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get all concerns ordered by newest first
     */
    public List<Concern> getAllConcerns() {
        return concernRepository.findAllByOrderByCreatedTimeDesc();
    }

    /**
     * Get concerns by status
     */
    public List<Concern> getConcernsByStatus(String status) {
        return concernRepository.findByStatusOrderByCreatedTimeDesc(status);
    }

    /**
     * Get concerns filtered by status and/or time range
     */
    public List<Concern> getFilteredConcerns(String status, LocalDateTime from, LocalDateTime to) {
        boolean hasStatus = status != null && !status.isEmpty() && !status.equals("All");
        boolean hasTime = from != null && to != null;

        if (hasStatus && hasTime) {
            return concernRepository.findByStatusAndCreatedTimeBetweenOrderByCreatedTimeDesc(status, from, to);
        } else if (hasStatus) {
            return concernRepository.findByStatusOrderByCreatedTimeDesc(status);
        } else if (hasTime) {
            return concernRepository.findByCreatedTimeBetweenOrderByCreatedTimeDesc(from, to);
        } else {
            return concernRepository.findAllByOrderByCreatedTimeDesc();
        }
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
     * Submit a reply to a concern and update its status
     */
    @Transactional
    public AdminReply submitReply(AdminReplyDTO dto) {
        Concern concern = getConcernById(dto.getConcernId());

        // Get or create a default admin for now
        Admin admin = getOrCreateDefaultAdmin();

        // Create reply
        AdminReply reply = new AdminReply();
        reply.setReplyMessage(dto.getReplyMessage());
        reply.setConcern(concern);
        reply.setAdmin(admin);

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
        return adminReplyRepository.save(reply);
    }

    /**
     * Update concern status
     */
    @Transactional
    public Concern updateConcernStatus(Integer concernId, String status) {
        Concern concern = getConcernById(concernId);
        concern.setStatus(status);
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
        adminUser.setEmail("admin@akb.edu");
        adminUser.setPassword("admin_" + UUID.randomUUID().toString().substring(0, 8));
        adminUser.setFirstName("System");
        adminUser.setLastName("Admin");
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

    public long getResolvedCount() {
        return concernRepository.countByStatus("Resolved");
    }
}
