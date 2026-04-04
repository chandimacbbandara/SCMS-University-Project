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
	public CommandLineRunner ensureAdminReplySenderRoleColumn(JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				if (tableExists(jdbcTemplate, "Admin_reply")) {
					ensureColumn(jdbcTemplate, "Admin_reply", "Sender_Role", "VARCHAR(20) NULL");
				}
			} catch (Exception e) {
				System.out.println("Warning: Could not ensure Admin_reply sender role column: " + e.getMessage());
			}
		};
	}

	@Bean
	public CommandLineRunner ensureFeedbackReplyColumn(JdbcTemplate jdbcTemplate) {
		return args -> {
			// First, ensure the table actually exists before trying to alter it
			Integer tableCount = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'SCMS_Feedback'",
					Integer.class);

			if (tableCount != null && tableCount > 0) {
				Integer columnCount = jdbcTemplate.queryForObject(
						"SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'SCMS_Feedback' AND COLUMN_NAME = 'ReplyID_FK'",
						Integer.class);

				if (columnCount == null || columnCount == 0) {
					try {
						jdbcTemplate.execute("ALTER TABLE SCMS_Feedback ADD ReplyID_FK INT NULL");
					} catch (Exception e) {
						System.out.println("Warning: Could not alter SCMS_Feedback table: " + e.getMessage());
					}
				}
			}
		};
	}

	@Bean
	public CommandLineRunner ensureLegacyFeedbackCompatibility(JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				if (!tableExists(jdbcTemplate, "feedback")) {
					return;
				}

				if (!columnExists(jdbcTemplate, "feedback", "ConcernID_FK")) {
					jdbcTemplate.execute("ALTER TABLE feedback ADD ConcernID_FK INT NULL");
				}

				if (tableExists(jdbcTemplate, "SCMS_Feedback") && columnExists(jdbcTemplate, "feedback", "ConcernID_FK")) {
					if (tableExists(jdbcTemplate, "customers")) {
						jdbcTemplate.update("""
								INSERT INTO customers (user_id)
								SELECT DISTINCT c.StudentID_FK
								FROM SCMS_Feedback sf
								INNER JOIN Concern c ON c.ConcernID = sf.ConcernID_FK
								WHERE c.StudentID_FK IS NOT NULL
								  AND NOT EXISTS (
								      SELECT 1 FROM customers cs WHERE cs.user_id = c.StudentID_FK
								  )
								""");
					}

					jdbcTemplate.update("""
							INSERT INTO feedback (
							    rating,
							    comments,
							    feedback_date,
							    customer_id,
							    customer_name,
							    is_deleted,
							    is_resolved,
							    reply,
							    reply_date,
							    admin_id,
							    ReplyID_FK,
							    created_at,
							    ConcernID_FK
							)
							SELECT
							    sf.Rating,
							    sf.Comments,
							    COALESCE(sf.submission_time, CURRENT_TIMESTAMP),
							    COALESCE(c.StudentID_FK, 0),
							    COALESCE(
							        NULLIF(TRIM(CONCAT(COALESCE(u.First_Name, ''), ' ', COALESCE(u.Last_Name, ''))), ''),
							        CONCAT('Student ', COALESCE(CAST(c.StudentID_FK AS CHAR(20)), '0'))
							    ),
							    0,
							    CASE WHEN c.Status = 'Complete' THEN 1 ELSE 0 END,
							    ar.Reply_Message,
							    ar.Reply_Time,
							    CASE WHEN ar.AdminID_FK IS NULL THEN NULL ELSE CAST(ar.AdminID_FK AS BIGINT) END,
							    sf.ReplyID_FK,
							    COALESCE(sf.submission_time, CURRENT_TIMESTAMP),
							    sf.ConcernID_FK
							FROM SCMS_Feedback sf
							LEFT JOIN Concern c ON c.ConcernID = sf.ConcernID_FK
							LEFT JOIN Student st ON st.UserID = c.StudentID_FK
							LEFT JOIN Users u ON u.UserID = st.UserID
							LEFT JOIN Admin_reply ar ON ar.ReplyID = sf.ReplyID_FK
							WHERE sf.ConcernID_FK IS NOT NULL
							  AND NOT EXISTS (
							      SELECT 1
							      FROM feedback f
							      WHERE f.ConcernID_FK = sf.ConcernID_FK
							        AND IFNULL(f.is_deleted, 0) = 0
							  )
							""");
				}
			} catch (Exception e) {
				System.out.println("Warning: Could not align legacy feedback compatibility: " + e.getMessage());
			}
		};
	}

	@Bean
	public CommandLineRunner ensureAnalyticsReportImageColumn(JdbcTemplate jdbcTemplate) {
		return args -> {
			Integer tableCount = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'Analytics_Report'",
					Integer.class);

			if (tableCount != null && tableCount > 0) {
				Integer columnCount = jdbcTemplate.queryForObject(
						"SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'Analytics_Report' AND COLUMN_NAME = 'EvidenceImageCount'",
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
	public CommandLineRunner ensureStudentPhotoColumnCompatibility(JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				if (tableExists(jdbcTemplate, "Student") && columnExists(jdbcTemplate, "Student", "StudentDPhoto")) {
					jdbcTemplate.execute("ALTER TABLE Student MODIFY StudentDPhoto LONGBLOB NULL");
				}
			} catch (Exception e) {
				System.out.println("Warning: Could not align Student.StudentDPhoto column type: " + e.getMessage());
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
							    ProposalID INT AUTO_INCREMENT PRIMARY KEY,
							    ConcernID_FK INT NOT NULL,
							    AdminID_FK INT NOT NULL,
							    Proposal_Status VARCHAR(60) NOT NULL DEFAULT 'PENDING_STUDENT_SELECTION',
							    Admin_Note TEXT NULL,
							    Student_Response_Note TEXT NULL,
							    Created_Time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
							    Responded_Time DATETIME NULL
							)
							""");
				}

				if (tableExists(jdbcTemplate, "Concern_Meeting_Proposal")) {
					ensureColumn(jdbcTemplate, "Concern_Meeting_Proposal", "ConcernID_FK", "INT NULL");
					ensureColumn(jdbcTemplate, "Concern_Meeting_Proposal", "AdminID_FK", "INT NULL");
					ensureColumn(jdbcTemplate, "Concern_Meeting_Proposal", "Proposal_Status", "VARCHAR(60) NOT NULL DEFAULT 'PENDING_STUDENT_SELECTION'");
					ensureColumn(jdbcTemplate, "Concern_Meeting_Proposal", "Admin_Note", "TEXT NULL");
					ensureColumn(jdbcTemplate, "Concern_Meeting_Proposal", "Student_Response_Note", "TEXT NULL");
					ensureColumn(jdbcTemplate, "Concern_Meeting_Proposal", "Created_Time", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
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
							    SlotID INT AUTO_INCREMENT PRIMARY KEY,
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
	public CommandLineRunner ensureConcernLinkedReferenceSchema(JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				if (!tableExists(jdbcTemplate, "Concern")) {
					return;
				}

				ensureColumn(jdbcTemplate, "Concern", "Linked_ConcernID_FK", "INT NULL");

				if (!indexExists(jdbcTemplate, "Concern", "IDX_Concern_LinkedConcern")) {
					jdbcTemplate.execute("CREATE INDEX IDX_Concern_LinkedConcern ON Concern(Linked_ConcernID_FK)");
				}

				if (!constraintExists(jdbcTemplate, "FK_Concern_LinkedConcern")) {
					jdbcTemplate.execute("""
							ALTER TABLE Concern
							ADD CONSTRAINT FK_Concern_LinkedConcern
							FOREIGN KEY (Linked_ConcernID_FK) REFERENCES Concern(ConcernID)
							ON DELETE SET NULL
							""");
				}
			} catch (Exception e) {
				System.out.println("Warning: Could not ensure linked concern schema compatibility: " + e.getMessage());
			}
		};
	}

	@Bean
	public CommandLineRunner ensureFaqTipCompatibility(JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				if (tableExists(jdbcTemplate, "tips")) {
					makeColumnNullable(jdbcTemplate, "tips", "createdAt", "DATETIME");
					makeColumnNullable(jdbcTemplate, "tips", "iconClass", "VARCHAR(255)");
				}

				if (tableExists(jdbcTemplate, "faqs")) {
					makeColumnNullable(jdbcTemplate, "faqs", "createdAt", "DATETIME");
				}
			} catch (Exception e) {
				System.out.println("Warning: Could not align FAQ/Tips compatibility columns: " + e.getMessage());
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
				"SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
				Integer.class,
				tableName);
		return count != null && count > 0;
	}

	private boolean columnExists(JdbcTemplate jdbcTemplate, String tableName, String columnName) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
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

	private void makeColumnNullable(JdbcTemplate jdbcTemplate, String tableName, String columnName, String dataTypeDefinition) {
		if (columnExists(jdbcTemplate, tableName, columnName)) {
			jdbcTemplate.execute("ALTER TABLE " + tableName + " MODIFY " + columnName + " " + dataTypeDefinition + " NULL");
		}
	}

	private boolean constraintExists(JdbcTemplate jdbcTemplate, String constraintName) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND constraint_type = 'FOREIGN KEY' AND constraint_name = ?",
				Integer.class,
				constraintName);
		return count != null && count > 0;
	}

	private boolean indexExists(JdbcTemplate jdbcTemplate, String tableName, String indexName) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
				Integer.class,
				tableName,
				indexName);
		return count != null && count > 0;
	}
}
