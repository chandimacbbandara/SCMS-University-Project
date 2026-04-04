package Project._6.demo.service;

import Project._6.demo.dto.ConcernSubmissionDTO;
import Project._6.demo.entity.AdminReply;
import Project._6.demo.entity.Concern;
import Project._6.demo.entity.Student;
import Project._6.demo.entity.User;
import Project._6.demo.repository.AdminReplyRepository;
import Project._6.demo.repository.ConcernRepository;
import Project._6.demo.repository.StudentRepository;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConcernService {

    private final ConcernRepository concernRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final AdminReplyRepository adminReplyRepository;
    private final ConcernPriorityService concernPriorityService;

    private static final String UPLOAD_DIR = "uploads/";
    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_DRAFT = "Draft";
    private static final String STATUS_REJECTED = "Rejected";
    private static final String STATUS_DELETED = "Deleted";
    private static final String STATUS_IN_PROGRESS = "In Progress";
    private static final String STATUS_MEETING_SCHEDULED = "Meeting Scheduled";
    private static final String STATUS_COMPLETE = "Complete";

    public ConcernService(ConcernRepository concernRepository,
                          StudentRepository studentRepository,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          NotificationService notificationService,
                          AdminReplyRepository adminReplyRepository,
                          ConcernPriorityService concernPriorityService) {
        this.concernRepository = concernRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.adminReplyRepository = adminReplyRepository;
        this.concernPriorityService = concernPriorityService;
    }

    @Transactional
    public Concern submitConcern(ConcernSubmissionDTO dto, MultipartFile evidence) throws IOException {
        Concern saved = createConcern(dto, evidence, false);
        notificationService.notifyConcernSubmitted(saved);
        return saved;
    }

    @Transactional
    public Concern saveConcernDraft(ConcernSubmissionDTO dto, MultipartFile evidence) throws IOException {
        return createConcern(dto, evidence, true);
    }

    private Concern createConcern(ConcernSubmissionDTO dto,
                                  MultipartFile evidence,
                                  boolean saveAsDraft) throws IOException {
        validateConcernFields(dto,
                saveAsDraft
                        ? "Please fill in Subject, Category, and Message before saving a draft."
                        : "Please fill in Subject, Category, and Message.");

        Student student = findOrCreateStudent(dto);
        Concern linkedConcern = resolveLinkedConcern(dto.getLinkedConcernId(), student.getUserId(), null);

        String evidencePath = null;
        if (evidence != null && !evidence.isEmpty()) {
            evidencePath = saveEvidence(evidence);
        }

        Concern concern = new Concern();
        concern.setSubject(dto.getSubject().trim());
        concern.setMessage(dto.getMessage().trim());
        concern.setEvidencePath(evidencePath);
        concern.setStatus(saveAsDraft ? STATUS_DRAFT : STATUS_PENDING);
        concern.setCategory(dto.getCategory().trim());
        if (saveAsDraft) {
            concern.setAiPriorityLevel(null);
        } else {
            concern.setAiPriorityLevel(concernPriorityService.predictPriority(
                    concern.getCategory(),
                    concern.getSubject(),
                    concern.getMessage()));
        }
        concern.setStudent(student);
        concern.setLinkedConcern(linkedConcern);
        assignConcernIdIfRequired(concern);

        return concernRepository.save(concern);
    }

    /**
     * Get a Student entity by userId.
     */
    public Student getStudentByUserId(Integer userId) {
        return studentRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
    }

    /**
     * Get all submitted concerns visible to students (exclude draft/rejected/deleted).
     */
    public List<Concern> getConcernsByStudentUserId(Integer userId) {
        Student student = studentRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return concernRepository.findByStudent_StudentId(student.getStudentId()).stream()
            .filter(concern -> concern != null
                && (concern.getStatus() == null
                || !isHiddenFromStudentViews(concern.getStatus().trim())))
            .sorted(Comparator.comparing(Concern::getCreatedTime,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    }

    /**
     * Get all draft concerns by a specific student.
     */
    public List<Concern> getDraftConcernsByStudentUserId(Integer userId) {
        Student student = studentRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return concernRepository.findByStudent_StudentId(student.getStudentId()).stream()
            .filter(concern -> concern != null
                && concern.getStatus() != null
                && STATUS_DRAFT.equalsIgnoreCase(concern.getStatus().trim()))
            .sorted(Comparator.comparing(Concern::getCreatedTime,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    }

    public long countDraftConcernsByStudentUserId(Integer userId) {
        Student student = studentRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return concernRepository.findByStudent_StudentId(student.getStudentId()).stream()
            .filter(concern -> concern != null
                && concern.getStatus() != null
                && STATUS_DRAFT.equalsIgnoreCase(concern.getStatus().trim()))
            .count();
    }

    /**
     * Get previous concerns that can be linked to a newly submitted concern.
     */
    @Transactional(readOnly = true)
    public List<Concern> getLinkableConcernsForStudent(Integer userId, Integer excludeConcernId) {
        Student student = studentRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        return concernRepository.findByStudent_StudentId(student.getStudentId()).stream()
                .filter(concern -> concern != null && concern.getConcernId() != null)
                .filter(concern -> excludeConcernId == null || !excludeConcernId.equals(concern.getConcernId()))
                .filter(concern -> concern.getStatus() == null
                        || !isStatusUnavailableForLinking(concern.getStatus().trim()))
                .sorted(Comparator.comparing(Concern::getCreatedTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /**
     * Build a map of concernId -> List<AdminReply> for a list of concerns.
     */
    public Map<Integer, List<AdminReply>> getRepliesMap(List<Concern> concerns) {
        Map<Integer, List<AdminReply>> map = new HashMap<>();
        for (Concern c : concerns) {
            map.put(c.getConcernId(),
                    adminReplyRepository.findByConcern_ConcernIdOrderByReplyTimeAsc(c.getConcernId()));
        }
        return map;
    }

    @Transactional
    public AdminReply addStudentChatMessage(Integer concernId, Integer studentUserId, String message) {
        Concern concern = concernRepository.findByConcernIdAndStudent_UserId(concernId, studentUserId)
                .orElseThrow(() -> new RuntimeException("Concern not found or access denied."));

        String status = concern.getStatus() == null ? "" : concern.getStatus().trim();
        if (STATUS_DRAFT.equalsIgnoreCase(status)
                || STATUS_REJECTED.equalsIgnoreCase(status)
                || STATUS_DELETED.equalsIgnoreCase(status)
                || STATUS_COMPLETE.equalsIgnoreCase(status)) {
            throw new RuntimeException("This concern is closed for new chat messages.");
        }

        String normalizedMessage = message == null ? "" : message.trim().replaceAll("\\s+", " ");
        if (normalizedMessage.isEmpty()) {
            throw new RuntimeException("Message cannot be empty.");
        }
        if (normalizedMessage.length() > 1200) {
            throw new RuntimeException("Message is too long. Maximum 1200 characters.");
        }

        AdminReply reply = new AdminReply();
        reply.setConcern(concern);
        reply.setAdmin(null);
        reply.setSenderRole(AdminReply.ROLE_STUDENT);
        reply.setReplyMessage(normalizedMessage);
        assignReplyIdIfRequired(reply);
        return adminReplyRepository.save(reply);
    }

    @Transactional
    public Concern markConcernCompleteByStudent(Integer concernId, Integer studentUserId) {
        Concern concern = concernRepository.findByConcernIdAndStudent_UserId(concernId, studentUserId)
                .orElseThrow(() -> new RuntimeException("Concern not found or access denied."));

        String status = concern.getStatus() == null ? "" : concern.getStatus().trim();
        String meetingStatus = concern.getMeetingStatus() == null ? "" : concern.getMeetingStatus().trim();
        boolean canComplete = STATUS_PENDING.equalsIgnoreCase(status)
                || STATUS_IN_PROGRESS.equalsIgnoreCase(status)
                || STATUS_MEETING_SCHEDULED.equalsIgnoreCase(status);

        if (!canComplete) {
            throw new RuntimeException("This concern cannot be marked complete right now.");
        }

        if ("BOOKED".equalsIgnoreCase(meetingStatus)) {
            throw new RuntimeException("You cannot mark this concern as complete while a meeting is booked.");
        }

        concern.setStatus(STATUS_COMPLETE);

        return concernRepository.save(concern);
    }

    /**
     * Get a pending concern for editing by its owner student.
     */
    @Transactional(readOnly = true)
    public Concern getPendingConcernForStudent(Integer concernId, Integer studentUserId) {
        Concern concern = getEditableConcernForStudent(concernId, studentUserId);
        String status = concern.getStatus() == null ? "" : concern.getStatus().trim();
        if (!STATUS_PENDING.equalsIgnoreCase(status)) {
            throw new RuntimeException("You can only update concerns before admin review begins.");
        }
        return concern;
    }

    @Transactional(readOnly = true)
    public Concern getEditableConcernForStudent(Integer concernId, Integer studentUserId) {
        Concern concern = concernRepository.findByConcernIdAndStudent_UserId(concernId, studentUserId)
                .orElseThrow(() -> new RuntimeException("Concern not found or access denied."));

        String status = concern.getStatus() == null ? "" : concern.getStatus().trim();
        if (!STATUS_PENDING.equalsIgnoreCase(status) && !STATUS_DRAFT.equalsIgnoreCase(status)) {
            throw new RuntimeException("This concern cannot be edited now.");
        }

        return concern;
    }

    @Transactional(readOnly = true)
    public Concern getDraftConcernForStudent(Integer concernId, Integer studentUserId) {
        Concern concern = concernRepository.findByConcernIdAndStudent_UserId(concernId, studentUserId)
                .orElseThrow(() -> new RuntimeException("Draft concern not found or access denied."));

        String status = concern.getStatus() == null ? "" : concern.getStatus().trim();
        if (!STATUS_DRAFT.equalsIgnoreCase(status)) {
            throw new RuntimeException("Only draft concerns are allowed for this action.");
        }
        return concern;
    }

    /**
     * Allow students to update only their own pending concerns.
     */
    @Transactional
    public Concern updateConcernByStudentIfPending(Integer concernId,
                                                   Integer studentUserId,
                                                   ConcernSubmissionDTO dto,
                                                   MultipartFile evidence) throws IOException {
        Concern concern = getPendingConcernForStudent(concernId, studentUserId);

        if (dto == null
                || dto.getSubject() == null || dto.getSubject().trim().isEmpty()
                || dto.getMessage() == null || dto.getMessage().trim().isEmpty()
                || dto.getCategory() == null || dto.getCategory().trim().isEmpty()) {
            throw new RuntimeException("Subject, Category, and Message are required.");
        }

        concern.setSubject(dto.getSubject().trim());
        concern.setMessage(dto.getMessage().trim());
        concern.setCategory(dto.getCategory().trim());
        concern.setAiPriorityLevel(concernPriorityService.predictPriority(
            concern.getCategory(),
            concern.getSubject(),
            concern.getMessage()));

        if (evidence != null && !evidence.isEmpty()) {
            concern.setEvidencePath(saveEvidence(evidence));
        }

        return concernRepository.save(concern);
    }

    /**
     * Update an editable concern (Pending or Draft).
     * If an existing draft is updated with saveAsDraft=false, it gets submitted as Pending.
     */
    @Transactional
    public Concern updateConcernByStudent(Integer concernId,
                                          Integer studentUserId,
                                          ConcernSubmissionDTO dto,
                                          MultipartFile evidence,
                                          boolean saveAsDraft) throws IOException {
        Concern concern = getEditableConcernForStudent(concernId, studentUserId);
        boolean isDraftConcern = STATUS_DRAFT.equalsIgnoreCase(concern.getStatus());

        if (saveAsDraft && !isDraftConcern) {
            throw new RuntimeException("Only draft concerns can be saved as drafts.");
        }

        validateConcernFields(dto,
                saveAsDraft
                        ? "Please fill in Subject, Category, and Message before saving a draft."
                        : "Please fill in Subject, Category, and Message.");

        concern.setSubject(dto.getSubject().trim());
        concern.setMessage(dto.getMessage().trim());
        concern.setCategory(dto.getCategory().trim());

        if (evidence != null && !evidence.isEmpty()) {
            concern.setEvidencePath(saveEvidence(evidence));
        }

        if (isDraftConcern && !saveAsDraft) {
            concern.setStatus(STATUS_PENDING);
        }

        if (saveAsDraft) {
            concern.setAiPriorityLevel(null);
        } else {
            concern.setAiPriorityLevel(concernPriorityService.predictPriority(
                    concern.getCategory(),
                    concern.getSubject(),
                    concern.getMessage()));
        }

        Concern saved = concernRepository.save(concern);
        if (isDraftConcern && !saveAsDraft) {
            notificationService.notifyConcernSubmitted(saved);
        }

        return saved;
    }

    /**
     * Submit an existing draft concern without changing its content.
     */
    @Transactional
    public Concern submitDraftByStudent(Integer concernId, Integer studentUserId) {
        Concern concern = getDraftConcernForStudent(concernId, studentUserId);

        validateConcernFields(concern.getSubject(), concern.getCategory(), concern.getMessage(),
                "Draft is incomplete. Please update Subject, Category, and Message before submitting.");

        concern.setStatus(STATUS_PENDING);
        concern.setAiPriorityLevel(concernPriorityService.predictPriority(
            concern.getCategory(),
            concern.getSubject(),
            concern.getMessage()));
        Concern saved = concernRepository.save(concern);
        notificationService.notifyConcernSubmitted(saved);
        return saved;
    }

    /**
     * Allow students to delete only their own editable concerns (Pending or Draft).
     */
    @Transactional
    public void deleteConcernByStudentIfPending(Integer concernId, Integer studentUserId) {
        if (concernId == null) {
            throw new RuntimeException("Invalid concern request.");
        }

        Concern concern = concernRepository.findByConcernIdAndStudent_UserId(concernId, studentUserId)
                .orElseThrow(() -> new RuntimeException("Concern not found or access denied."));

        String status = concern.getStatus() == null ? "" : concern.getStatus().trim();
        if (!STATUS_PENDING.equalsIgnoreCase(status) && !STATUS_DRAFT.equalsIgnoreCase(status)) {
            throw new RuntimeException("You can only delete draft or pending concerns before admin review begins.");
        }

        notificationService.deleteByConcernId(concernId);
        concernRepository.delete(concern);
    }

    private void validateConcernFields(ConcernSubmissionDTO dto, String message) {
        if (dto == null) {
            throw new RuntimeException(message);
        }
        validateConcernFields(dto.getSubject(), dto.getCategory(), dto.getMessage(), message);
    }

    private void validateConcernFields(String subject,
                                       String category,
                                       String message,
                                       String errorMessage) {
        if (subject == null || subject.trim().isEmpty()
                || message == null || message.trim().isEmpty()
                || category == null || category.trim().isEmpty()) {
            throw new RuntimeException(errorMessage);
        }
    }

    private boolean isHiddenFromStudentViews(String status) {
        return STATUS_DRAFT.equalsIgnoreCase(status)
                || STATUS_REJECTED.equalsIgnoreCase(status)
                || STATUS_DELETED.equalsIgnoreCase(status);
    }

    private Concern resolveLinkedConcern(Integer linkedConcernId,
                                         Integer studentUserId,
                                         Integer currentConcernId) {
        if (linkedConcernId == null) {
            return null;
        }

        if (currentConcernId != null && linkedConcernId.equals(currentConcernId)) {
            throw new RuntimeException("A concern cannot be linked to itself.");
        }

        Concern linkedConcern = concernRepository.findByConcernIdAndStudent_UserId(linkedConcernId, studentUserId)
                .orElseThrow(() -> new RuntimeException("Selected linked concern was not found for this student."));

        String linkedStatus = linkedConcern.getStatus() == null ? "" : linkedConcern.getStatus().trim();
        if (isStatusUnavailableForLinking(linkedStatus)) {
            throw new RuntimeException("Selected linked concern is not eligible for linking.");
        }

        return linkedConcern;
    }

    private boolean isStatusUnavailableForLinking(String status) {
        return STATUS_DRAFT.equalsIgnoreCase(status)
                || STATUS_REJECTED.equalsIgnoreCase(status)
                || STATUS_DELETED.equalsIgnoreCase(status);
    }

    private Student findOrCreateStudent(ConcernSubmissionDTO dto) {
        Optional<Student> existingStudent = studentRepository.findByStudentId(dto.getStudentId());

        if (existingStudent.isPresent()) {
            return existingStudent.get();
        }

        // Create a new User first
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode("temp_" + UUID.randomUUID().toString().substring(0, 8))); // hashed temporary password
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        assignUserIdIfRequired(user);
        user = userRepository.save(user);

        // Create the Student linked to the User
        Student student = new Student();
        student.setUser(user);
        student.setStudentId(dto.getStudentId());
        student.setCategory(dto.getCategory());
        return studentRepository.save(student);
    }

    private String saveEvidence(MultipartFile file) throws IOException {
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

    private void assignUserIdIfRequired(User user) {
        if (user == null || user.getUserId() != null) {
            return;
        }

        Integer identityFlag = userRepository.isUserIdIdentity();
        boolean isIdentity = identityFlag != null && identityFlag == 1;
        if (!isIdentity) {
            user.setUserId(userRepository.getNextUserId());
        }
    }

    private void assignConcernIdIfRequired(Concern concern) {
        if (concern == null || concern.getConcernId() != null) {
            return;
        }

        Integer identityFlag = concernRepository.isConcernIdIdentity();
        boolean isIdentity = identityFlag != null && identityFlag == 1;
        if (!isIdentity) {
            concern.setConcernId(concernRepository.getNextConcernId());
        }
    }

    private void assignReplyIdIfRequired(AdminReply reply) {
        if (reply == null || reply.getReplyId() != null) {
            return;
        }

        Integer identityFlag = adminReplyRepository.isReplyIdIdentity();
        boolean isIdentity = identityFlag != null && identityFlag == 1;
        if (!isIdentity) {
            reply.setReplyId(adminReplyRepository.getNextReplyId());
        }
    }
}
