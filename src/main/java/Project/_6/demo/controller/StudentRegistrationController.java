package Project._6.demo.controller;

import Project._6.demo.dto.LoginDTO;
import Project._6.demo.dto.StudentRegistrationDTO;
import Project._6.demo.entity.Student;
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

@Controller
public class StudentRegistrationController {

    private final StudentRegistrationService registrationService;

    public StudentRegistrationController(StudentRegistrationService registrationService) {
        this.registrationService = registrationService;
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

    /**
     * Handle login form submission - checks admin first, then student
     */
    @PostMapping("/login")
    public String loginUser(@ModelAttribute LoginDTO loginDTO,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
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
        return "student-dashboard";
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
