package Project._6.demo.service;

import Project._6.demo.dto.LoginDTO;
import Project._6.demo.dto.ChangePasswordDTO;
import Project._6.demo.dto.StudentProfileUpdateDTO;
import Project._6.demo.dto.StudentRegistrationDTO;
import Project._6.demo.entity.Admin;
import Project._6.demo.entity.Concern;
import Project._6.demo.entity.StudentCommunityPost;
import Project._6.demo.entity.Student;
import Project._6.demo.entity.User;
import Project._6.demo.repository.AdminRepository;
import Project._6.demo.repository.AdminReplyRepository;
import Project._6.demo.repository.ConcernRepository;
import Project._6.demo.repository.FeedbackRepository;
import Project._6.demo.repository.NotificationRepository;
import Project._6.demo.repository.StudentCommunityModerationLogRepository;
import Project._6.demo.repository.StudentCommunityPostRepository;
import Project._6.demo.repository.StudentCommunityReplyRepository;
import Project._6.demo.repository.StudentCommunityRulesAcceptanceRepository;
import Project._6.demo.repository.StudentRepository;
import Project._6.demo.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Service
public class StudentRegistrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudentRegistrationService.class);

    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9])\\S{12,}$"
    );
        private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{10,15}$");
        private static final Pattern ADDRESS_PATTERN = Pattern.compile("^[A-Za-z0-9\\s,./#-]{3,255}$");

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final StudentRepository studentRepository;
    private final ConcernRepository concernRepository;
    private final AdminReplyRepository adminReplyRepository;
    private final FeedbackRepository feedbackRepository;
    private final NotificationRepository notificationRepository;
    private final StudentCommunityPostRepository studentCommunityPostRepository;
    private final StudentCommunityReplyRepository studentCommunityReplyRepository;
    private final StudentCommunityRulesAcceptanceRepository studentCommunityRulesAcceptanceRepository;
    private final StudentCommunityModerationLogRepository studentCommunityModerationLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public StudentRegistrationService(UserRepository userRepository,
                                      AdminRepository adminRepository,
                                      StudentRepository studentRepository,
                                      ConcernRepository concernRepository,
                                      AdminReplyRepository adminReplyRepository,
                                      FeedbackRepository feedbackRepository,
                                      NotificationRepository notificationRepository,
                                      StudentCommunityPostRepository studentCommunityPostRepository,
                                      StudentCommunityReplyRepository studentCommunityReplyRepository,
                                      StudentCommunityRulesAcceptanceRepository studentCommunityRulesAcceptanceRepository,
                                      StudentCommunityModerationLogRepository studentCommunityModerationLogRepository,
                                      PasswordEncoder passwordEncoder,
                                      EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.studentRepository = studentRepository;
        this.concernRepository = concernRepository;
        this.adminReplyRepository = adminReplyRepository;
        this.feedbackRepository = feedbackRepository;
        this.notificationRepository = notificationRepository;
        this.studentCommunityPostRepository = studentCommunityPostRepository;
        this.studentCommunityReplyRepository = studentCommunityReplyRepository;
        this.studentCommunityRulesAcceptanceRepository = studentCommunityRulesAcceptanceRepository;
        this.studentCommunityModerationLogRepository = studentCommunityModerationLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
    }

    /**
     * Register a new student with PENDING status.
     * The student must wait for admin approval.
     */
    @Transactional
    public Student registerStudent(StudentRegistrationDTO dto, MultipartFile studentIdPhoto) throws IOException {

        // Check if passwords match
        if (dto.getPassword() == null || !dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match.");
        }

        String normalizedEmail = normalizeEmail(dto.getEmail());
        if (normalizedEmail == null) {
            throw new RuntimeException("Email is required.");
        }

        Set<Integer> removedRejectedUserIds = new HashSet<>();

        Optional<User> existingUserOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (!"REJECTED".equalsIgnoreCase(existingUser.getRegistrationStatus())) {
                throw new RuntimeException("An account with this email already exists.");
            }
            removeRejectedAccount(existingUser.getUserId());
            removedRejectedUserIds.add(existingUser.getUserId());
        }

        Optional<Student> existingStudentByIdOpt = studentRepository.findByStudentId(dto.getStudentId());
        if (existingStudentByIdOpt.isPresent()) {
            Student existingStudent = existingStudentByIdOpt.get();
            User existingUser = existingStudent.getUser();
            Integer existingUserId = existingStudent.getUserId();

            if (existingUser == null || !"REJECTED".equalsIgnoreCase(existingUser.getRegistrationStatus())) {
                throw new RuntimeException("A student with this Student ID already exists.");
            }

            if (!removedRejectedUserIds.contains(existingUserId)) {
                removeRejectedAccount(existingUserId);
            }
        }

        // Create User with PENDING status
        User user = new User();
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRegistrationStatus("PENDING");
        user = userRepository.save(user);

        // Create Student linked to the User
        Student student = new Student();
        student.setUser(user);
        student.setStudentId(dto.getStudentId());

        // Save student ID photo
        if (studentIdPhoto != null && !studentIdPhoto.isEmpty()) {
            student.setStudentPhoto(studentIdPhoto.getBytes());
        }

        Student savedStudent = studentRepository.save(student);

        try {
            emailVerificationService.sendPendingReviewEmail(
                    user.getEmail(),
                    user.getFirstName(),
                    savedStudent.getStudentId()
            );
        } catch (Exception ex) {
            LOGGER.warn("Failed to send pending review email for userId={}", user.getUserId(), ex);
        }

        return savedStudent;
    }

    /**
     * Get all students with PENDING registration status
     */
    public List<Student> getPendingStudents() {
        return studentRepository.findByUser_RegistrationStatus("PENDING");
    }

    /**
     * Get students by a specific registration status
     */
    public List<Student> getStudentsByStatus(String status) {
        return studentRepository.findByUser_RegistrationStatus(status);
    }

    /**
     * Get all students (for admin overview)
     */
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    /**
     * Approve a student registration
     */
    @Transactional
    public Student approveStudent(Integer userId) {
        Student student = studentRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Student not found with UserID: " + userId));
        student.getUser().setRegistrationStatus("APPROVED");
        userRepository.save(student.getUser());

        try {
            emailVerificationService.sendApprovalEmail(
                    student.getUser().getEmail(),
                    student.getUser().getFirstName(),
                    student.getStudentId()
            );
        } catch (Exception ex) {
            LOGGER.warn("Failed to send approval email for userId={}", userId, ex);
        }

        return student;
    }

    /**
     * Reject a student registration
     */
    @Transactional
    public Student rejectStudent(Integer userId) {
        Student student = studentRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Student not found with UserID: " + userId));
        student.getUser().setRegistrationStatus("REJECTED");
        userRepository.save(student.getUser());

        try {
            emailVerificationService.sendRejectionEmail(
                    student.getUser().getEmail(),
                    student.getUser().getFirstName(),
                    student.getStudentId()
            );
        } catch (Exception ex) {
            LOGGER.warn("Failed to send rejection email for userId={}", userId, ex);
        }

        return student;
    }

    /**
     * Permanently delete a student account (Student + User records).
     */
    @Transactional
    public Student deleteStudentAccount(Integer userId) {
        Student student = studentRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Student not found with UserID: " + userId));

        deleteStudentDependencies(userId);

        studentRepository.delete(student);
        userRepository.deleteById(userId);

        return student;
    }

    private void deleteStudentDependencies(Integer userId) {
        // Remove direct student-linked records first.
        notificationRepository.deleteByStudent_UserId(userId);
        studentCommunityRulesAcceptanceRepository.deleteByStudent_UserId(userId);
        studentCommunityModerationLogRepository.deleteByStudent_UserId(userId);

        // Remove community replies authored by the student.
        studentCommunityReplyRepository.deleteByStudent_UserId(userId);

        // Remove replies on posts created by the student before deleting posts.
        List<Integer> studentPostIds = studentCommunityPostRepository.findByStudent_UserId(userId).stream()
                .map(StudentCommunityPost::getPostId)
                .collect(Collectors.toList());
        if (!studentPostIds.isEmpty()) {
            studentCommunityReplyRepository.deleteByPost_PostIdIn(studentPostIds);
        }
        studentCommunityPostRepository.deleteByStudent_UserId(userId);

        // Remove concern graph dependencies, then concerns.
        List<Concern> concerns = concernRepository.findByStudent_UserId(userId);
        for (Concern concern : concerns) {
            Integer concernId = concern.getConcernId();
            feedbackRepository.deleteByConcern_ConcernId(concernId);
            adminReplyRepository.deleteByConcern_ConcernId(concernId);
            notificationRepository.deleteByConcern_ConcernId(concernId);
        }
        if (!concerns.isEmpty()) {
            concernRepository.deleteAll(concerns);
        }
    }

    /**
     * Get a student by UserID
     */
    public Student getStudentByUserId(Integer userId) {
        return studentRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Student not found with UserID: " + userId));
    }

    /**
     * Get student ID photo as byte array
     */
    public byte[] getStudentPhoto(Integer userId) {
        Student student = getStudentByUserId(userId);
        return student.getStudentPhoto();
    }

    /**
     * Count pending registrations
     */
    public long getPendingCount() {
        return studentRepository.findByUser_RegistrationStatus("PENDING").size();
    }

    /**
     * Count approved registrations
     */
    public long getApprovedCount() {
        return studentRepository.findByUser_RegistrationStatus("APPROVED").size();
    }

    /**
     * Count rejected registrations
     */
    public long getRejectedCount() {
        return studentRepository.findByUser_RegistrationStatus("REJECTED").size();
    }

    /**
     * Authenticate a student login.
     * Only APPROVED students can log in.
     */
    public Student loginStudent(LoginDTO dto) {
        String normalizedEmail = normalizeEmail(dto.getEmail());
        if (normalizedEmail == null) {
            throw new RuntimeException("Email is required.");
        }

        // Find user by email
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("No account found with this email address.");
        }

        User user = userOpt.get();

        // Check password (BCrypt comparison)
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password.");
        }

        // Check registration status
        if ("PENDING".equals(user.getRegistrationStatus())) {
            throw new RuntimeException("Your registration is still pending admin approval. Please wait for approval before logging in.");
        }

        if ("REJECTED".equals(user.getRegistrationStatus())) {
            throw new RuntimeException("Your registration has been rejected. Please contact the administration for more information.");
        }

        if (!"APPROVED".equals(user.getRegistrationStatus())) {
            throw new RuntimeException("Your account is not active. Please contact the administration.");
        }

        // Find the student record
        Optional<Student> studentOpt = studentRepository.findById(user.getUserId());
        if (studentOpt.isEmpty()) {
            throw new RuntimeException("Student record not found. Please contact the administration.");
        }

        return studentOpt.get();
    }

    public Admin loginAdmin(LoginDTO dto) {
        String normalizedEmail = normalizeEmail(dto.getEmail());
        if (normalizedEmail == null) {
            throw new RuntimeException("Email is required.");
        }

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("No account found with this email address."));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password.");
        }

        if (!adminRepository.existsByUser_UserId(user.getUserId())) {
            throw new RuntimeException("No admin account found with this email address.");
        }

        return adminRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Admin account not found."));
    }

    public boolean emailExists(String email) {
        String normalizedEmail = normalizeEmail(email);
        return normalizedEmail != null && userRepository.existsByEmailIgnoreCase(normalizedEmail);
    }

    public boolean canRegisterWithEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return false;
        }

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (userOpt.isEmpty()) {
            return true;
        }

        return "REJECTED".equalsIgnoreCase(userOpt.get().getRegistrationStatus());
    }

    @Transactional
    public void resetPasswordByEmail(String email, String newPassword, String confirmPassword) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            throw new RuntimeException("Email is required.");
        }

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("No account found with this email address."));

        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            throw new RuntimeException("New password and confirm password do not match.");
        }

        if (!isStrongPassword(newPassword)) {
            throw new RuntimeException("Password must be at least 12 characters and include uppercase, lowercase, number, and special character.");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new RuntimeException("New password must be different from your current password.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase();
    }

    private void removeRejectedAccount(Integer userId) {
        if (userId == null) {
            return;
        }

        studentRepository.findById(userId).ifPresent(studentRepository::delete);
        userRepository.deleteById(userId);
    }

    /**
     * Update editable student profile information.
     */
    @Transactional
    public Student updateStudentProfile(Integer userId, StudentProfileUpdateDTO dto) {
        Student student = getStudentByUserId(userId);
        User user = student.getUser();

        String gender = normalize(dto.getGender());
        if (gender != null && !"Male".equalsIgnoreCase(gender) && !"Female".equalsIgnoreCase(gender) && !"Other".equalsIgnoreCase(gender)) {
            throw new RuntimeException("Gender must be Male, Female, or Other.");
        }

        String phoneNumber = normalize(dto.getPhoneNumber());
        if (phoneNumber != null && !PHONE_PATTERN.matcher(phoneNumber).matches()) {
            throw new RuntimeException("Phone number must contain 10 to 15 digits and may start with +.");
        }

        LocalDate dob = dto.getDob();
        if (dob != null) {
            LocalDate today = LocalDate.now();
            if (dob.isAfter(today)) {
                throw new RuntimeException("Date of birth cannot be in the future.");
            }
            if (dob.isBefore(today.minusYears(100))) {
                throw new RuntimeException("Date of birth is not valid.");
            }
        }

        String category = normalize(dto.getCategory());
        if (category == null) {
            throw new RuntimeException("Category is required.");
        }
        if (!"Online".equalsIgnoreCase(category) && !"Physical".equalsIgnoreCase(category)) {
            throw new RuntimeException("Category must be either Online or Physical.");
        }
        category = "Online".equalsIgnoreCase(category) ? "Online" : "Physical";

        String address1stLane = normalize(dto.getAddress1stLane());
        String address2ndLane = normalize(dto.getAddress2ndLane());
        String address3rdLane = normalize(dto.getAddress3rdLane());

        validateAddress(address1stLane, "Address 1st Lane");
        validateAddress(address2ndLane, "Address 2nd Lane");
        validateAddress(address3rdLane, "Address 3rd Lane");

        user.setGender(gender);
        user.setPhoneNumber(phoneNumber);
        user.setAddress1stLane(address1stLane);
        user.setAddress2ndLane(address2ndLane);
        user.setAddress3rdLane(address3rdLane);

        student.setDob(dob);
        student.setCategory(category);

        userRepository.save(user);
        return studentRepository.save(student);
    }

    /**
     * Change student password with current password check and strong password policy.
     */
    @Transactional
    public void changeStudentPassword(Integer userId, ChangePasswordDTO dto) {
        Student student = getStudentByUserId(userId);
        User user = student.getUser();

        if (dto.getCurrentPassword() == null || !passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect.");
        }

        if (dto.getNewPassword() == null || !dto.getNewPassword().equals(dto.getConfirmNewPassword())) {
            throw new RuntimeException("New password and confirm password do not match.");
        }

        if (!isStrongPassword(dto.getNewPassword())) {
            throw new RuntimeException("Password must be at least 12 characters and include uppercase, lowercase, number, and special character.");
        }

        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("New password must be different from your current password.");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    public boolean isStrongPassword(String password) {
        return password != null && STRONG_PASSWORD_PATTERN.matcher(password).matches() && !password.contains(" ");
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateAddress(String address, String label) {
        if (address == null) {
            return;
        }
        if (!ADDRESS_PATTERN.matcher(address).matches()) {
            throw new RuntimeException(label + " must be 3-255 characters and can include letters, numbers, spaces, comma, dot, slash, # and -.");
        }
    }

}
