package Project._6.demo.service;

import Project._6.demo.dto.ConcernSubmissionDTO;
import Project._6.demo.entity.Concern;
import Project._6.demo.entity.Student;
import Project._6.demo.entity.User;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConcernService {

    private final ConcernRepository concernRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    private static final String UPLOAD_DIR = "uploads/";

    public ConcernService(ConcernRepository concernRepository,
                          StudentRepository studentRepository,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          NotificationService notificationService) {
        this.concernRepository = concernRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
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
        concern.setStudent(student);

        Concern saved = concernRepository.save(concern);

        // Trigger notification: Step 1 - Concern Submitted
        notificationService.notifyConcernSubmitted(saved);

        return saved;
    }

    /**
     * Get all concerns submitted by a specific student.
     */
    public List<Concern> getConcernsByStudentUserId(Integer userId) {
        Student student = studentRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return concernRepository.findByStudent_StudentId(student.getStudentId());
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
}
