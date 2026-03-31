package Project._6.demo;

import Project._6.demo.entity.Concern;
import Project._6.demo.entity.AdminReply;
import Project._6.demo.entity.Admin;
import Project._6.demo.repository.AdminReplyRepository;
import Project._6.demo.entity.User;
import Project._6.demo.repository.AdminRepository;
import Project._6.demo.repository.ConcernRepository;
import Project._6.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

@SpringBootApplication
public class StudentConcernManagementSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(StudentConcernManagementSystemApplication.class, args);
	}

	@Bean
	public CommandLineRunner ensureFeedbackReplyColumn(JdbcTemplate jdbcTemplate) {
		return args -> {
			// First, ensure the table actually exists before trying to alter it
			Integer tableCount = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Feedback'",
					Integer.class);

			if (tableCount != null && tableCount > 0) {
				Integer columnCount = jdbcTemplate.queryForObject(
						"SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Feedback' AND COLUMN_NAME = 'ReplyID_FK'",
						Integer.class);

				if (columnCount == null || columnCount == 0) {
					try {
						jdbcTemplate.execute("ALTER TABLE Feedback ADD ReplyID_FK INT NULL");
					} catch (Exception e) {
						System.out.println("Warning: Could not alter Feedback table: " + e.getMessage());
					}
				}
			}
		};
	}

	@Bean
	public CommandLineRunner ensureAnalyticsReportImageColumn(JdbcTemplate jdbcTemplate) {
		return args -> {
			Integer tableCount = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Analytics_Report'",
					Integer.class);

			if (tableCount != null && tableCount > 0) {
				Integer columnCount = jdbcTemplate.queryForObject(
						"SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Analytics_Report' AND COLUMN_NAME = 'EvidenceImageCount'",
						Integer.class);

				if (columnCount == null || columnCount == 0) {
					try {
						jdbcTemplate.execute("ALTER TABLE Analytics_Report ADD EvidenceImageCount INT NULL");
					} catch (Exception e) {
						System.out.println("Warning: Could not alter Analytics_Report table: " + e.getMessage());
					}
				}
			}
		};
	}

	@Bean
	public CommandLineRunner removePredefinedAdmin(UserRepository userRepository,
			AdminRepository adminRepository,
			AdminReplyRepository adminReplyRepository,
			ConcernRepository concernRepository) {
		return args -> {
			try {
				Optional<Admin> legacyAdmin = adminRepository.findByStaffId("ADMIN001");
				if (legacyAdmin.isEmpty() || legacyAdmin.get().getUser() == null) {
					return;
				}

				User user = legacyAdmin.get().getUser();
				if (user.getEmail() == null || !"admin@akb.edu".equalsIgnoreCase(user.getEmail())) {
					return;
				}

				Integer userId = user.getUserId();

				List<Concern> assignedConcerns = concernRepository.findByAdmin_UserId(userId);
				if (!assignedConcerns.isEmpty()) {
					for (Concern concern : assignedConcerns) {
						concern.setAdmin(null);
					}
					concernRepository.saveAll(assignedConcerns);
				}

				List<AdminReply> adminReplies = adminReplyRepository.findByAdmin_UserId(userId);
				if (!adminReplies.isEmpty()) {
					for (AdminReply reply : adminReplies) {
						reply.setAdmin(null);
					}
					adminReplyRepository.saveAll(adminReplies);
				}

				if (adminRepository.existsByUser_UserId(userId)) {
					adminRepository.deleteById(userId);
				}
				userRepository.deleteById(userId);
			} catch (Exception e) {
				System.out.println("Warning: Skipped removing predefined admin. Tables may not exist yet: " + e.getMessage());
			}
		};
	}
}
