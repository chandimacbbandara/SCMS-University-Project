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
	public CommandLineRunner ensureConcernMeetingSchema(JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				if (!tableExists(jdbcTemplate, "Concern")) {
					return;
				}

				ensureColumn(jdbcTemplate, "Concern", "Meeting_Status", "VARCHAR(60) NULL");
				ensureColumn(jdbcTemplate, "Concern", "Meeting_Booked_Start_Time", "DATETIME NULL");
				ensureColumn(jdbcTemplate, "Concern", "Meeting_Booked_End_Time", "DATETIME NULL");
				ensureColumn(jdbcTemplate, "Concern", "Meeting_Booked_At", "DATETIME NULL");

				if (!tableExists(jdbcTemplate, "Concern_Meeting_Proposal")) {
					jdbcTemplate.execute("""
							CREATE TABLE Concern_Meeting_Proposal (
							    ProposalID INT IDENTITY(1,1) PRIMARY KEY,
							    ConcernID_FK INT NOT NULL,
							    AdminID_FK INT NOT NULL,
							    Proposal_Status VARCHAR(60) NOT NULL DEFAULT 'PENDING_STUDENT_SELECTION',
							    Admin_Note VARCHAR(MAX) NULL,
							    Student_Response_Note VARCHAR(MAX) NULL,
							    Created_Time DATETIME NOT NULL DEFAULT GETDATE(),
							    Responded_Time DATETIME NULL
							)
							""");
				}

				if (tableExists(jdbcTemplate, "Concern_Meeting_Proposal")) {
					ensureColumn(jdbcTemplate, "Concern_Meeting_Proposal", "ConcernID_FK", "INT NULL");
					ensureColumn(jdbcTemplate, "Concern_Meeting_Proposal", "AdminID_FK", "INT NULL");
					ensureColumn(jdbcTemplate, "Concern_Meeting_Proposal", "Proposal_Status", "VARCHAR(60) NOT NULL DEFAULT 'PENDING_STUDENT_SELECTION'");
					ensureColumn(jdbcTemplate, "Concern_Meeting_Proposal", "Admin_Note", "VARCHAR(MAX) NULL");
					ensureColumn(jdbcTemplate, "Concern_Meeting_Proposal", "Student_Response_Note", "VARCHAR(MAX) NULL");
					ensureColumn(jdbcTemplate, "Concern_Meeting_Proposal", "Created_Time", "DATETIME NOT NULL DEFAULT GETDATE()");
					ensureColumn(jdbcTemplate, "Concern_Meeting_Proposal", "Responded_Time", "DATETIME NULL");

					if (!constraintExists(jdbcTemplate, "FK_ConcernMeetingProposal_Concern")) {
						jdbcTemplate.execute("""
								ALTER TABLE Concern_Meeting_Proposal
								ADD CONSTRAINT FK_ConcernMeetingProposal_Concern
								FOREIGN KEY (ConcernID_FK) REFERENCES Concern(ConcernID)
								""");
					}

					if (!constraintExists(jdbcTemplate, "FK_ConcernMeetingProposal_Admin")) {
						jdbcTemplate.execute("""
								ALTER TABLE Concern_Meeting_Proposal
								ADD CONSTRAINT FK_ConcernMeetingProposal_Admin
								FOREIGN KEY (AdminID_FK) REFERENCES Admin(UserID)
								""");
					}
				}

				if (!tableExists(jdbcTemplate, "Concern_Meeting_Slot")) {
					jdbcTemplate.execute("""
							CREATE TABLE Concern_Meeting_Slot (
							    SlotID INT IDENTITY(1,1) PRIMARY KEY,
							    ProposalID_FK INT NOT NULL,
							    Start_Time DATETIME NOT NULL,
							    End_Time DATETIME NOT NULL,
							    Slot_Status VARCHAR(40) NOT NULL DEFAULT 'AVAILABLE'
							)
							""");
				}

				if (tableExists(jdbcTemplate, "Concern_Meeting_Slot")) {
					ensureColumn(jdbcTemplate, "Concern_Meeting_Slot", "ProposalID_FK", "INT NULL");
					ensureColumn(jdbcTemplate, "Concern_Meeting_Slot", "Start_Time", "DATETIME NULL");
					ensureColumn(jdbcTemplate, "Concern_Meeting_Slot", "End_Time", "DATETIME NULL");
					ensureColumn(jdbcTemplate, "Concern_Meeting_Slot", "Slot_Status", "VARCHAR(40) NOT NULL DEFAULT 'AVAILABLE'");

					if (!constraintExists(jdbcTemplate, "FK_ConcernMeetingSlot_Proposal")) {
						jdbcTemplate.execute("""
								ALTER TABLE Concern_Meeting_Slot
								ADD CONSTRAINT FK_ConcernMeetingSlot_Proposal
								FOREIGN KEY (ProposalID_FK) REFERENCES Concern_Meeting_Proposal(ProposalID)
								""");
					}
				}

				if (!indexExists(jdbcTemplate, "Concern_Meeting_Proposal", "IDX_MeetingProposal_Concern")) {
					jdbcTemplate.execute("CREATE INDEX IDX_MeetingProposal_Concern ON Concern_Meeting_Proposal(ConcernID_FK, Created_Time DESC)");
				}

				if (!indexExists(jdbcTemplate, "Concern_Meeting_Slot", "IDX_MeetingSlot_Proposal")) {
					jdbcTemplate.execute("CREATE INDEX IDX_MeetingSlot_Proposal ON Concern_Meeting_Slot(ProposalID_FK, Start_Time ASC)");
				}
			} catch (Exception e) {
				System.out.println("Warning: Could not ensure concern meeting schema: " + e.getMessage());
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

	private boolean tableExists(JdbcTemplate jdbcTemplate, String tableName) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?",
				Integer.class,
				tableName);
		return count != null && count > 0;
	}

	private boolean columnExists(JdbcTemplate jdbcTemplate, String tableName, String columnName) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?",
				Integer.class,
				tableName,
				columnName);
		return count != null && count > 0;
	}

	private void ensureColumn(JdbcTemplate jdbcTemplate, String tableName, String columnName, String definition) {
		if (!columnExists(jdbcTemplate, tableName, columnName)) {
			jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD " + columnName + " " + definition);
		}
	}

	private boolean constraintExists(JdbcTemplate jdbcTemplate, String constraintName) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM sys.objects WHERE type = 'F' AND name = ?",
				Integer.class,
				constraintName);
		return count != null && count > 0;
	}

	private boolean indexExists(JdbcTemplate jdbcTemplate, String tableName, String indexName) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM sys.indexes WHERE name = ? AND object_id = OBJECT_ID(?)",
				Integer.class,
				indexName,
				tableName);
		return count != null && count > 0;
	}
}
