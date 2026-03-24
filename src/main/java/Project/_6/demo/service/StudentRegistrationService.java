package Project._6.demo.service;

import Project._6.demo.dto.LoginDTO;
import Project._6.demo.dto.ChangePasswordDTO;
import Project._6.demo.dto.StudentProfileUpdateDTO;
import Project._6.demo.dto.StudentRegistrationDTO;
import Project._6.demo.entity.Student;
import Project._6.demo.entity.User;
import Project._6.demo.repository.StudentRepository;
import Project._6.demo.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class StudentRegistrationService {

    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9])\\S{12,}$"
    );
        private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{10,15}$");
        private static final Pattern ADDRESS_PATTERN = Pattern.compile("^[A-Za-z0-9\\s,./#-]{3,255}$");

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentRegistrationService(UserRepository userRepository,
                                      StudentRepository studentRepository,
                                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
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

        // Check if email already exists
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("An account with this email already exists.");
        }

        // Check if student ID already exists
        if (studentRepository.existsByStudentId(dto.getStudentId())) {
            throw new RuntimeException("A student with this Student ID already exists.");
        }

        // Create User with PENDING status
        User user = new User();
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
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

        return studentRepository.save(student);
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
        return student;
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
        // Find user by email
        Optional<User> userOpt = userRepository.findByEmail(dto.getEmail());
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
