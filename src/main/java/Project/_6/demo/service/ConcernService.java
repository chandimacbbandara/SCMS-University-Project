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

    private static final String UPLOAD_DIR = "uploads/";

    public ConcernService(ConcernRepository concernRepository,
                          StudentRepository studentRepository,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          NotificationService notificationService,
                          AdminReplyRepository adminReplyRepository) {
        this.concernRepository = concernRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.adminReplyRepository = adminReplyRepository;
    }

    @Transactional
    public Concern submitConcern(ConcernSubmissionDTO dto, MultipartFile evidence) throws IOException {

        // Find or create the student
        Student student = findOrCreateStudent(dto);

        // Handle evidence file upload
        String evidencePath = null;
        if (evidence != null && !evidence.isEmpty()) {
            evidencePath = saveEvidence(evidence);
        }

        // Create the concern
        Concern concern = new Concern();
        concern.setSubject(dto.getSubject());
        concern.setMessage(dto.getMessage());
        concern.setEvidencePath(evidencePath);
        concern.setStatus("Pending");
        concern.setCategory(dto.getCategory());
        concern.setStudent(student);
        assignConcernIdIfRequired(concern);

        Concern saved = concernRepository.save(concern);

        // Trigger notification: Step 1 - Concern Submitted
        notificationService.notifyConcernSubmitted(saved);

        return saved;
    }

    /**
     * Get a Student entity by userId.
     */
    public Student getStudentByUserId(Integer userId) {
        return studentRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
    }

    /**
     * Get all concerns submitted by a specific student.
     */
    public List<Concern> getConcernsByStudentUserId(Integer userId) {
        Student student = studentRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return concernRepository.findByStudent_StudentId(student.getStudentId());
    }

    /**
     * Build a map of concernId -> List<AdminReply> for a list of concerns.
     */
    public Map<Integer, List<AdminReply>> getRepliesMap(List<Concern> concerns) {
        Map<Integer, List<AdminReply>> map = new HashMap<>();
        for (Concern c : concerns) {
            map.put(c.getConcernId(),
                    adminReplyRepository.findByConcern_ConcernIdOrderByReplyTimeDesc(c.getConcernId()));
        }
        return map;
    }

    /**
     * Get a pending concern for editing by its owner student.
     */
    @Transactional(readOnly = true)
    public Concern getPendingConcernForStudent(Integer concernId, Integer studentUserId) {
        Concern concern = concernRepository.findByConcernIdAndStudent_UserId(concernId, studentUserId)
                .orElseThrow(() -> new RuntimeException("Concern not found or access denied."));

        String status = concern.getStatus() == null ? "" : concern.getStatus().trim();
        if (!"Pending".equalsIgnoreCase(status)) {
            throw new RuntimeException("You can only update concerns before admin review begins.");
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

        if (evidence != null && !evidence.isEmpty()) {
            concern.setEvidencePath(saveEvidence(evidence));
        }

        return concernRepository.save(concern);
    }

    /**
     * Allow students to delete only their own pending concerns.
     */
    @Transactional
    public void deleteConcernByStudentIfPending(Integer concernId, Integer studentUserId) {
        if (concernId == null) {
            throw new RuntimeException("Invalid concern request.");
        }

        Concern concern = concernRepository.findByConcernIdAndStudent_UserId(concernId, studentUserId)
                .orElseThrow(() -> new RuntimeException("Concern not found or access denied."));

        String status = concern.getStatus() == null ? "" : concern.getStatus().trim();
        if (!"Pending".equalsIgnoreCase(status)) {
            throw new RuntimeException("You can only delete concerns before admin review begins.");
        }

        notificationService.deleteByConcernId(concernId);
        concernRepository.delete(concern);
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
}
