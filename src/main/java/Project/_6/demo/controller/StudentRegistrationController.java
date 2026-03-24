package Project._6.demo.controller;

import Project._6.demo.dto.LoginDTO;
import Project._6.demo.dto.ChangePasswordDTO;
import Project._6.demo.dto.StudentProfileUpdateDTO;
import Project._6.demo.dto.StudentRegistrationDTO;
import Project._6.demo.entity.AdminReply;
import Project._6.demo.entity.Concern;
import Project._6.demo.entity.Notification;
import Project._6.demo.entity.Student;
import Project._6.demo.service.ConcernService;
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

import java.util.List;
import java.util.Map;

@Controller
public class StudentRegistrationController {

    private final StudentRegistrationService registrationService;
    private final NotificationService notificationService;
    private final ConcernService concernService;

    public StudentRegistrationController(StudentRegistrationService registrationService,
                                         NotificationService notificationService,
                                         ConcernService concernService) {
        this.registrationService = registrationService;
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
        model.addAttribute("loginDTO", new LoginDTO());
        return "student-login";
    }

    // Predefined admin credentials
    private static final String ADMIN_EMAIL = "admin@gmail.com";
    private static final String ADMIN_PASSWORD = "123456";

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
        // Check if owner credentials
        if (OWNER_EMAIL.equals(loginDTO.getEmail()) && OWNER_PASSWORD.equals(loginDTO.getPassword())) {
            session.setAttribute("ownerLoggedIn", true);
            session.setAttribute("ownerEmail", loginDTO.getEmail());
            redirectAttributes.addFlashAttribute("successMessage", "Welcome, Owner!");
            return "redirect:/owner/dashboard";
        }

        // Check if admin credentials
        if (ADMIN_EMAIL.equals(loginDTO.getEmail()) && ADMIN_PASSWORD.equals(loginDTO.getPassword())) {
            session.setAttribute("adminLoggedIn", true);
            session.setAttribute("adminEmail", loginDTO.getEmail());
            redirectAttributes.addFlashAttribute("successMessage", "Welcome, Admin!");
            return "redirect:/admin/dashboard";
        }

        // Otherwise try student login
        try {
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
            return "redirect:/login";
        }
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
            RedirectAttributes redirectAttributes) {

        try {
            Student student = registrationService.registerStudent(registrationDTO, studentIdPhoto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Registration submitted successfully! Your account is pending admin approval. " +
                    "Student ID: " + student.getStudentId());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Registration failed: " + e.getMessage());
        }
        return "redirect:/register";
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
