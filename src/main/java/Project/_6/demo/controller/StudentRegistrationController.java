package Project._6.demo.controller;

import Project._6.demo.dto.LoginDTO;
import Project._6.demo.dto.ChangePasswordDTO;
import Project._6.demo.dto.StudentProfileUpdateDTO;
import Project._6.demo.dto.StudentRegistrationDTO;
import Project._6.demo.entity.Admin;
import Project._6.demo.entity.AdminReply;
import Project._6.demo.entity.Concern;
import Project._6.demo.entity.Notification;
import Project._6.demo.entity.Student;
import Project._6.demo.service.ConcernService;
import Project._6.demo.service.EmailVerificationService;
import Project._6.demo.service.NotificationService;
import Project._6.demo.service.StudentRegistrationService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

@Controller
public class StudentRegistrationController {

    private final StudentRegistrationService registrationService;
    private final EmailVerificationService emailVerificationService;
    private final NotificationService notificationService;
    private final ConcernService concernService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final String REG_VERIFY_EMAIL = "regVerifyEmail";
    private static final String REG_VERIFY_CODE = "regVerifyCode";
    private static final String REG_VERIFY_EXPIRY = "regVerifyExpiry";
    private static final String REG_VERIFY_CONFIRMED = "regVerifyConfirmed";
    private static final String FORGOT_VERIFY_EMAIL = "forgotVerifyEmail";
    private static final String FORGOT_VERIFY_CODE = "forgotVerifyCode";
    private static final String FORGOT_VERIFY_EXPIRY = "forgotVerifyExpiry";
    private static final String FORGOT_VERIFY_CONFIRMED = "forgotVerifyConfirmed";

    public StudentRegistrationController(StudentRegistrationService registrationService,
                                         EmailVerificationService emailVerificationService,
                                         NotificationService notificationService,
                                         ConcernService concernService) {
        this.registrationService = registrationService;
        this.emailVerificationService = emailVerificationService;
        this.notificationService = notificationService;
        this.concernService = concernService;
    }

    /**
     * Home Page - maps to /
     */
    @GetMapping("/")
    public String showIndex() {
        return "index";
    }

    // ========================
    // STUDENT LOGIN
    // ========================

    /**
     * Show the login form
     */
    @GetMapping("/login")
    public String showLoginForm(Model model) {
        if (!model.containsAttribute("loginDTO")) {
            model.addAttribute("loginDTO", new LoginDTO());
        }
        return "student-login";
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password/send-code")
    @ResponseBody
    public ResponseEntity<Map<String, String>> sendForgotPasswordCode(@RequestParam("email") String email,
                                                                      HttpSession session) {
        Map<String, String> response = new HashMap<>();
        String normalizedEmail = email == null ? "" : email.trim();

        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            response.put("status", "error");
            response.put("message", "Please enter a valid email address.");
            return ResponseEntity.badRequest().body(response);
        }

        if (!registrationService.emailExists(normalizedEmail)) {
            response.put("status", "error");
            response.put("message", "No account found with this email address.");
            return ResponseEntity.badRequest().body(response);
        }

        String code = String.valueOf(100000 + new Random().nextInt(900000));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(10);

        try {
            emailVerificationService.sendPasswordResetCode(normalizedEmail, null, code);
        } catch (Exception ex) {
            response.put("status", "error");
            response.put("message", "Could not send reset code. Check your email settings and try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        session.setAttribute(FORGOT_VERIFY_EMAIL, normalizedEmail);
        session.setAttribute(FORGOT_VERIFY_CODE, code);
        session.setAttribute(FORGOT_VERIFY_EXPIRY, expiry);
        session.setAttribute(FORGOT_VERIFY_CONFIRMED, false);

        response.put("status", "ok");
        response.put("message", "Reset code sent. Please check your email.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password/verify-code")
    @ResponseBody
    public ResponseEntity<Map<String, String>> verifyForgotPasswordCode(@RequestParam("email") String email,
                                                                         @RequestParam("code") String code,
                                                                         HttpSession session) {
        Map<String, String> response = new HashMap<>();
        String normalizedEmail = email == null ? "" : email.trim();
        String normalizedCode = code == null ? "" : code.trim();

        String savedEmail = (String) session.getAttribute(FORGOT_VERIFY_EMAIL);
        String savedCode = (String) session.getAttribute(FORGOT_VERIFY_CODE);
        LocalDateTime expiry = (LocalDateTime) session.getAttribute(FORGOT_VERIFY_EXPIRY);

        if (savedEmail == null || savedCode == null || expiry == null) {
            response.put("status", "error");
            response.put("message", "Please request a reset code first.");
            return ResponseEntity.badRequest().body(response);
        }

        if (!savedEmail.equalsIgnoreCase(normalizedEmail)) {
            response.put("status", "error");
            response.put("message", "The email does not match the one used to request the code.");
            return ResponseEntity.badRequest().body(response);
        }

        if (LocalDateTime.now().isAfter(expiry)) {
            response.put("status", "error");
            response.put("message", "Reset code expired. Please request a new code.");
            return ResponseEntity.badRequest().body(response);
        }

        if (!savedCode.equals(normalizedCode)) {
            response.put("status", "error");
            response.put("message", "Invalid reset code.");
            return ResponseEntity.badRequest().body(response);
        }

        session.setAttribute(FORGOT_VERIFY_CONFIRMED, true);
        response.put("status", "ok");
        response.put("message", "Email verified. You can now set a new password.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password/reset")
    @ResponseBody
    public ResponseEntity<Map<String, String>> resetForgotPassword(@RequestParam("email") String email,
                                                                    @RequestParam("newPassword") String newPassword,
                                                                    @RequestParam("confirmPassword") String confirmPassword,
                                                                    HttpSession session) {
        Map<String, String> response = new HashMap<>();
        String normalizedEmail = email == null ? "" : email.trim();

        String savedEmail = (String) session.getAttribute(FORGOT_VERIFY_EMAIL);
        Boolean verified = (Boolean) session.getAttribute(FORGOT_VERIFY_CONFIRMED);

        if (!Boolean.TRUE.equals(verified) || savedEmail == null || !savedEmail.equalsIgnoreCase(normalizedEmail)) {
            response.put("status", "error");
            response.put("message", "Please verify your email with the reset code first.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            registrationService.resetPasswordByEmail(normalizedEmail, newPassword, confirmPassword);

            session.removeAttribute(FORGOT_VERIFY_EMAIL);
            session.removeAttribute(FORGOT_VERIFY_CODE);
            session.removeAttribute(FORGOT_VERIFY_EXPIRY);
            session.removeAttribute(FORGOT_VERIFY_CONFIRMED);

            response.put("status", "ok");
            response.put("message", "Password reset successful. You can now log in.");
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            response.put("status", "error");
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Predefined admin credentials
    // Predefined owner credentials
    private static final String OWNER_EMAIL = "owner@gmail.com";
    private static final String OWNER_PASSWORD = "123456";

    /**
     * Handle login form submission - checks owner first, then admin, then student
     */
    @PostMapping("/login")
    public String loginUser(@ModelAttribute LoginDTO loginDTO,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        String email = loginDTO.getEmail() == null ? "" : loginDTO.getEmail().trim();

        // Check if owner credentials
        if (OWNER_EMAIL.equalsIgnoreCase(email)) {
            if (OWNER_PASSWORD.equals(loginDTO.getPassword())) {
                session.setAttribute("ownerLoggedIn", true);
                session.setAttribute("ownerEmail", email);
                redirectAttributes.addFlashAttribute("successMessage", "Welcome, Owner!");
                return "redirect:/owner/dashboard";
            }
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid password.");
            redirectAttributes.addFlashAttribute("loginDTO", createLoginDTOWithEmail(email));
            return "redirect:/login";
        }

        // Check admin account from database
        try {
            loginDTO.setEmail(email);
            Admin admin = registrationService.loginAdmin(loginDTO);
            session.setAttribute("adminLoggedIn", true);
            session.setAttribute("adminEmail", admin.getUser().getEmail());
            redirectAttributes.addFlashAttribute("successMessage", "Welcome, Admin!");
            return "redirect:/admin/dashboard";
        } catch (Exception ignored) {
            // Fall through to student login
        }

        // Otherwise try student login
        try {
            loginDTO.setEmail(email);
            Student student = registrationService.loginStudent(loginDTO);

            // Store student info in session
            session.setAttribute("loggedInStudent", student);
            session.setAttribute("studentName", student.getUser().getFirstName() + " " + student.getUser().getLastName());
            session.setAttribute("studentId", student.getStudentId());
            session.setAttribute("studentUserId", student.getUserId());

            redirectAttributes.addFlashAttribute("successMessage",
                    "Welcome back, " + student.getUser().getFirstName() + "!");
            return "redirect:/student/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("loginDTO", createLoginDTOWithEmail(email));
            return "redirect:/login";
        }
    }

    private LoginDTO createLoginDTOWithEmail(String email) {
        LoginDTO dto = new LoginDTO();
        dto.setEmail(email);
        return dto;
    }

    /**
     * Logout - invalidate session
     */
    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("successMessage", "You have been logged out successfully.");
        return "redirect:/login";
    }

    /**
     * Student Dashboard - only accessible after login
     * Re-fetches student from DB to ensure data is always available
     */
    @GetMapping("/student/dashboard")
    public String showStudentDashboard(HttpSession session, Model model) {
        Integer userId = (Integer) session.getAttribute("studentUserId");
        if (userId == null) {
            return "redirect:/login";
        }

        // Re-fetch from DB to get fresh, attached entity
        Student student = registrationService.getStudentByUserId(userId);
        if (student == null) {
            session.invalidate();
            return "redirect:/login";
        }

        model.addAttribute("student", student);
        model.addAttribute("studentName", student.getUser().getFirstName() + " " + student.getUser().getLastName());

        // Add notifications for the student (personal + broadcast)
        List<Notification> personalNotifications = notificationService.getNotificationsForStudent(userId);
        List<Notification> broadcastNotifications = notificationService.getAllBroadcastNotifications();
        List<Notification> allNotifications = new java.util.ArrayList<>(personalNotifications);
        allNotifications.addAll(broadcastNotifications);
        allNotifications.sort((a, b) -> b.getSentTime().compareTo(a.getSentTime()));
        long unreadCount = notificationService.getUnreadCount(userId) + broadcastNotifications.stream().filter(n -> !Boolean.TRUE.equals(n.getIsRead())).count();
        model.addAttribute("notifications", allNotifications);
        model.addAttribute("unreadCount", unreadCount);

        // Add student's concerns for tracking
        List<Concern> concerns = concernService.getConcernsByStudentUserId(userId);
        Map<Integer, List<AdminReply>> repliesMap = concernService.getRepliesMap(concerns);
        model.addAttribute("concerns", concerns);
        model.addAttribute("repliesMap", repliesMap);

        return "student-dashboard";
    }

    @GetMapping("/student/profile")
    public String showStudentProfile(HttpSession session, Model model) {
        Integer userId = (Integer) session.getAttribute("studentUserId");
        if (userId == null) {
            return "redirect:/login";
        }

        Student student = registrationService.getStudentByUserId(userId);
        if (student == null) {
            session.invalidate();
            return "redirect:/login";
        }

        StudentProfileUpdateDTO profileDTO = new StudentProfileUpdateDTO();
        profileDTO.setGender(student.getUser().getGender());
        profileDTO.setPhoneNumber(student.getUser().getPhoneNumber());
        profileDTO.setAddress1stLane(student.getUser().getAddress1stLane());
        profileDTO.setAddress2ndLane(student.getUser().getAddress2ndLane());
        profileDTO.setAddress3rdLane(student.getUser().getAddress3rdLane());
        profileDTO.setDob(student.getDob());
        profileDTO.setCategory(student.getCategory());

        model.addAttribute("student", student);
        model.addAttribute("profileDTO", profileDTO);
        model.addAttribute("changePasswordDTO", new ChangePasswordDTO());
        model.addAttribute("passwordRule", "Use at least 12 characters with uppercase, lowercase, number, and special character.");

        // Keep notification drawer data available on profile page
        List<Notification> personalNotifications = notificationService.getNotificationsForStudent(userId);
        List<Notification> broadcastNotifications = notificationService.getAllBroadcastNotifications();
        List<Notification> allNotifications = new java.util.ArrayList<>(personalNotifications);
        allNotifications.addAll(broadcastNotifications);
        allNotifications.sort((a, b) -> b.getSentTime().compareTo(a.getSentTime()));
        long unreadCount = notificationService.getUnreadCount(userId)
            + broadcastNotifications.stream().filter(n -> !Boolean.TRUE.equals(n.getIsRead())).count();
        model.addAttribute("notifications", allNotifications);
        model.addAttribute("unreadCount", unreadCount);

        return "student-profile";
    }

    @PostMapping("/student/profile/update")
    public String updateStudentProfile(@ModelAttribute("profileDTO") StudentProfileUpdateDTO profileDTO,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        Integer userId = (Integer) session.getAttribute("studentUserId");
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            Student updatedStudent = registrationService.updateStudentProfile(userId, profileDTO);
            session.setAttribute("studentName", updatedStudent.getUser().getFirstName() + " " + updatedStudent.getUser().getLastName());
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update profile: " + e.getMessage());
        }

        return "redirect:/student/profile";
    }

    @PostMapping("/student/profile/change-password")
    public String changeStudentPassword(@ModelAttribute("changePasswordDTO") ChangePasswordDTO changePasswordDTO,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        Integer userId = (Integer) session.getAttribute("studentUserId");
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            registrationService.changeStudentPassword(userId, changePasswordDTO);
            redirectAttributes.addFlashAttribute("passwordSuccessMessage", "Password changed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("passwordErrorMessage", e.getMessage());
        }

        return "redirect:/student/profile";
    }

    @GetMapping("/student/profile/photo")
    @ResponseBody
    public ResponseEntity<byte[]> getOwnStudentPhoto(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("studentUserId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        byte[] photo = registrationService.getStudentPhoto(userId);
        if (photo == null || photo.length == 0) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        return new ResponseEntity<>(photo, headers, HttpStatus.OK);
    }

    @GetMapping("/student/photo/{userId}")
    @ResponseBody
    public ResponseEntity<byte[]> getStudentPhotoForCommunity(@PathVariable("userId") Integer userId,
                                                              HttpSession session) {
        if (session.getAttribute("studentUserId") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        byte[] photo = registrationService.getStudentPhoto(userId);
        if (photo == null || photo.length == 0) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        return new ResponseEntity<>(photo, headers, HttpStatus.OK);
    }

    // ========================
    // STUDENT REGISTRATION
    // ========================

    /**
     * Show the student registration form
     */
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrationDTO", new StudentRegistrationDTO());
        return "student-register";
    }

    /**
     * Handle student registration form submission
     */
    @PostMapping("/register")
    public String registerStudent(
            @ModelAttribute StudentRegistrationDTO registrationDTO,
            @RequestParam(value = "studentIdPhoto", required = false) MultipartFile studentIdPhoto,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            String email = registrationDTO.getEmail() == null ? "" : registrationDTO.getEmail().trim();
            Boolean verified = (Boolean) session.getAttribute(REG_VERIFY_CONFIRMED);
            String verifiedEmail = (String) session.getAttribute(REG_VERIFY_EMAIL);

            if (!Boolean.TRUE.equals(verified) || verifiedEmail == null || !verifiedEmail.equalsIgnoreCase(email)) {
                throw new RuntimeException("Please verify your email with the code before creating the account.");
            }

            Student student = registrationService.registerStudent(registrationDTO, studentIdPhoto);

            session.removeAttribute(REG_VERIFY_EMAIL);
            session.removeAttribute(REG_VERIFY_CODE);
            session.removeAttribute(REG_VERIFY_EXPIRY);
            session.removeAttribute(REG_VERIFY_CONFIRMED);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Registration submitted successfully. Your email is verified and your account is pending admin approval. " +
                    "Student ID: " + student.getStudentId());
            return "redirect:/register";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Registration failed: " + e.getMessage());
        }
        return "redirect:/register";
    }

    @PostMapping("/register/send-code")
    @ResponseBody
    public ResponseEntity<Map<String, String>> sendRegistrationVerificationCode(@RequestParam("email") String email,
                                                                                 @RequestParam(value = "firstName", required = false) String firstName,
                                                                                 HttpSession session) {
        Map<String, String> response = new HashMap<>();
        String normalizedEmail = email == null ? "" : email.trim();

        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            response.put("status", "error");
            response.put("message", "Please enter a valid email address.");
            return ResponseEntity.badRequest().body(response);
        }

        if (!registrationService.canRegisterWithEmail(normalizedEmail)) {
            response.put("status", "error");
            response.put("message", "An account with this email already exists.");
            return ResponseEntity.badRequest().body(response);
        }

        String code = String.valueOf(100000 + new Random().nextInt(900000));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(10);

        try {
            emailVerificationService.sendVerificationCode(normalizedEmail, firstName, code);
        } catch (Exception ex) {
            response.put("status", "error");
            response.put("message", "Could not send verification code. Check your email settings and try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        session.setAttribute(REG_VERIFY_EMAIL, normalizedEmail);
        session.setAttribute(REG_VERIFY_CODE, code);
        session.setAttribute(REG_VERIFY_EXPIRY, expiry);
        session.setAttribute(REG_VERIFY_CONFIRMED, false);

        response.put("status", "ok");
        response.put("message", "Verification code sent. Please check your email.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/verify-code")
    @ResponseBody
    public ResponseEntity<Map<String, String>> verifyRegistrationEmailCode(@RequestParam("email") String email,
                                                                            @RequestParam("code") String code,
                                                                            HttpSession session) {
        Map<String, String> response = new HashMap<>();
        String normalizedEmail = email == null ? "" : email.trim();
        String normalizedCode = code == null ? "" : code.trim();

        String savedEmail = (String) session.getAttribute(REG_VERIFY_EMAIL);
        String savedCode = (String) session.getAttribute(REG_VERIFY_CODE);
        LocalDateTime expiry = (LocalDateTime) session.getAttribute(REG_VERIFY_EXPIRY);

        if (savedEmail == null || savedCode == null || expiry == null) {
            response.put("status", "error");
            response.put("message", "Please request a verification code first.");
            return ResponseEntity.badRequest().body(response);
        }

        if (!savedEmail.equalsIgnoreCase(normalizedEmail)) {
            response.put("status", "error");
            response.put("message", "The email does not match the one used to request the code.");
            return ResponseEntity.badRequest().body(response);
        }

        if (LocalDateTime.now().isAfter(expiry)) {
            response.put("status", "error");
            response.put("message", "Verification code expired. Please request a new code.");
            return ResponseEntity.badRequest().body(response);
        }

        if (!savedCode.equals(normalizedCode)) {
            response.put("status", "error");
            response.put("message", "Invalid verification code.");
            return ResponseEntity.badRequest().body(response);
        }

        session.setAttribute(REG_VERIFY_CONFIRMED, true);

        response.put("status", "ok");
        response.put("message", "Email verified successfully. You can submit the registration form now.");
        return ResponseEntity.ok(response);
    }

    // ========================
    // ADMIN - STUDENT REVIEW
    // ========================

    /**
     * Show admin student review page with all pending registrations
     */
    @GetMapping("/admin/student-review")
    public String showStudentReview(
            @RequestParam(value = "filter", required = false, defaultValue = "PENDING") String filter,
            HttpSession session,
            Model model) {

        if (!Boolean.TRUE.equals(session.getAttribute("adminLoggedIn"))) {
            return "redirect:/login";
        }

        List<Student> students;
        if ("ALL".equalsIgnoreCase(filter)) {
            students = registrationService.getAllStudents();
        } else if ("APPROVED".equalsIgnoreCase(filter)) {
            students = registrationService.getStudentsByStatus("APPROVED");
        } else if ("REJECTED".equalsIgnoreCase(filter)) {
            students = registrationService.getStudentsByStatus("REJECTED");
        } else {
            students = registrationService.getPendingStudents();
        }

        model.addAttribute("students", students);
        model.addAttribute("selectedFilter", filter);
        model.addAttribute("pendingCount", registrationService.getPendingCount());
        model.addAttribute("approvedCount", registrationService.getApprovedCount());
        model.addAttribute("rejectedCount", registrationService.getRejectedCount());

        return "admin-student-review";
    }

    /**
     * Approve a student registration
     */
    @PostMapping("/admin/student-review/{userId}/approve")
    public String approveStudent(@PathVariable("userId") Integer userId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (!Boolean.TRUE.equals(session.getAttribute("adminLoggedIn"))) {
            return "redirect:/login";
        }
        try {
            Student student = registrationService.approveStudent(userId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Student " + student.getStudentId() + " has been approved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to approve student: " + e.getMessage());
        }
        return "redirect:/admin/student-review";
    }

    /**
     * Reject a student registration
     */
    @PostMapping("/admin/student-review/{userId}/reject")
    public String rejectStudent(@PathVariable("userId") Integer userId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (!Boolean.TRUE.equals(session.getAttribute("adminLoggedIn"))) {
            return "redirect:/login";
        }
        try {
            Student student = registrationService.rejectStudent(userId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Student " + student.getStudentId() + " has been rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to reject student: " + e.getMessage());
        }
        return "redirect:/admin/student-review";
    }

    /**
     * Delete a student account
     */
    @PostMapping("/admin/student-review/{userId}/delete")
    public String deleteStudent(@PathVariable("userId") Integer userId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (!Boolean.TRUE.equals(session.getAttribute("adminLoggedIn"))) {
            return "redirect:/login";
        }
        try {
            Student student = registrationService.deleteStudentAccount(userId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Student " + student.getStudentId() + " account has been deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to delete student account: " + e.getMessage());
        }
        return "redirect:/admin/student-review";
    }

    /**
     * Serve student ID photo for admin review
     */
    @GetMapping("/admin/student-review/{userId}/photo")
    @ResponseBody
    public ResponseEntity<byte[]> getStudentPhoto(@PathVariable("userId") Integer userId) {
        byte[] photo = registrationService.getStudentPhoto(userId);
        if (photo == null || photo.length == 0) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        return new ResponseEntity<>(photo, headers, HttpStatus.OK);
    }
}
