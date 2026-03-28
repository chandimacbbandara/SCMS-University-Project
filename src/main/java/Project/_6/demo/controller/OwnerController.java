package Project._6.demo.controller;

import Project._6.demo.dto.AnalyticsReportDTO;
import Project._6.demo.entity.Admin;
import Project._6.demo.entity.AdminReply;
import Project._6.demo.entity.AnalyticsReport;
import Project._6.demo.entity.Concern;
import Project._6.demo.entity.Feedback;
import Project._6.demo.entity.Notification;
import Project._6.demo.entity.User;
import Project._6.demo.repository.AnalyticsReportRepository;
import Project._6.demo.repository.AdminReplyRepository;
import Project._6.demo.repository.AdminRepository;
import Project._6.demo.repository.ConcernRepository;
import Project._6.demo.repository.FeedbackRepository;
import Project._6.demo.repository.UserRepository;
import Project._6.demo.service.AnalyticsReportService;
import Project._6.demo.service.EmailVerificationService;
import Project._6.demo.service.NotificationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/owner")
public class OwnerController {

    private final AnalyticsReportService analyticsReportService;
    private final AnalyticsReportRepository analyticsReportRepository;
    private final ConcernRepository concernRepository;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final FeedbackRepository feedbackRepository;
    private final AdminReplyRepository adminReplyRepository;
    private final NotificationService notificationService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9])\\S{12,}$");

    private static final String OWNER_ADMIN_VERIFY_EMAIL = "ownerAdminVerifyEmail";
    private static final String OWNER_ADMIN_VERIFY_CODE = "ownerAdminVerifyCode";
    private static final String OWNER_ADMIN_VERIFY_EXPIRY = "ownerAdminVerifyExpiry";
    private static final String OWNER_ADMIN_VERIFY_CONFIRMED = "ownerAdminVerifyConfirmed";
    private static final String OWNER_ADMIN_UPDATE_VERIFY_USER_ID = "ownerAdminUpdateVerifyUserId";
    private static final String OWNER_ADMIN_UPDATE_VERIFY_EMAIL = "ownerAdminUpdateVerifyEmail";
    private static final String OWNER_ADMIN_UPDATE_VERIFY_CODE = "ownerAdminUpdateVerifyCode";
    private static final String OWNER_ADMIN_UPDATE_VERIFY_EXPIRY = "ownerAdminUpdateVerifyExpiry";
    private static final String OWNER_ADMIN_UPDATE_VERIFY_CONFIRMED = "ownerAdminUpdateVerifyConfirmed";

    public OwnerController(AnalyticsReportService analyticsReportService,
                           AnalyticsReportRepository analyticsReportRepository,
                           ConcernRepository concernRepository,
                           AdminRepository adminRepository,
                           UserRepository userRepository,
                           FeedbackRepository feedbackRepository,
                           AdminReplyRepository adminReplyRepository,
                           NotificationService notificationService,
                           EmailVerificationService emailVerificationService,
                           PasswordEncoder passwordEncoder) {
        this.analyticsReportService = analyticsReportService;
        this.analyticsReportRepository = analyticsReportRepository;
        this.concernRepository = concernRepository;
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.feedbackRepository = feedbackRepository;
        this.adminReplyRepository = adminReplyRepository;
        this.notificationService = notificationService;
        this.emailVerificationService = emailVerificationService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        if (!isOwnerLoggedIn(session)) {
            return "redirect:/login";
        }

        List<AnalyticsReport> reports = analyticsReportService.getAllReports();
        model.addAttribute("reports", reports);
        model.addAttribute("reportDTO", new AnalyticsReportDTO());

        return "owner-dashboard";
    }

    @GetMapping("/admin/create-page")
    public String showCreateAdminPage(HttpSession session) {
        if (!isOwnerLoggedIn(session)) {
            return "redirect:/login";
        }
        return "owner-create-admin";
    }

    @GetMapping("/admin/manage")
    public String showManageAdminsPage(HttpSession session, Model model) {
        if (!isOwnerLoggedIn(session)) {
            return "redirect:/login";
        }

        List<Admin> admins = adminRepository.findAll().stream()
                .sorted(Comparator.comparing(Admin::getUserId))
                .collect(Collectors.toList());
        model.addAttribute("admins", admins);
        return "owner-manage-admins";
    }

    @PostMapping("/admin/{userId}/update")
    public String updateAdminAccount(@PathVariable("userId") Integer userId,
                                     @RequestParam("email") String email,
                                     @RequestParam("username") String username,
                                     @RequestParam(value = "newPassword", required = false) String newPassword,
                                     @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        if (!isOwnerLoggedIn(session)) {
            return "redirect:/login";
        }

        try {
            Admin admin = adminRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Admin account not found."));

            String normalizedEmail = normalizeEmail(email);
            String normalizedUsername = normalize(username);

            if (normalizedEmail == null || !EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
                throw new RuntimeException("Please enter a valid email address.");
            }
            if (normalizedUsername == null) {
                throw new RuntimeException("Username is required.");
            }

            Optional<User> userWithEmail = userRepository.findByEmailIgnoreCase(normalizedEmail);
            if (userWithEmail.isPresent() && !userWithEmail.get().getUserId().equals(userId)) {
                throw new RuntimeException("Another account already uses this email.");
            }

            Optional<Admin> adminWithStaffId = adminRepository.findByStaffIdIgnoreCase(normalizedUsername);
            if (adminWithStaffId.isPresent() && !adminWithStaffId.get().getUserId().equals(userId)) {
                throw new RuntimeException("Another admin already uses this username.");
            }

            User user = admin.getUser();

            String currentEmail = normalizeEmail(user.getEmail());
            boolean emailChanged = currentEmail == null
                    ? normalizedEmail != null
                    : !currentEmail.equalsIgnoreCase(normalizedEmail);

            if (emailChanged) {
                Integer verifiedUserId = (Integer) session.getAttribute(OWNER_ADMIN_UPDATE_VERIFY_USER_ID);
                String verifiedEmail = (String) session.getAttribute(OWNER_ADMIN_UPDATE_VERIFY_EMAIL);
                Boolean verified = (Boolean) session.getAttribute(OWNER_ADMIN_UPDATE_VERIFY_CONFIRMED);

                if (!Boolean.TRUE.equals(verified)
                        || verifiedUserId == null
                        || !verifiedUserId.equals(userId)
                        || verifiedEmail == null
                        || !verifiedEmail.equalsIgnoreCase(normalizedEmail)) {
                    throw new RuntimeException("Please verify the new email with the code before updating this admin account.");
                }
            }

            user.setEmail(normalizedEmail);
            user.setFirstName(normalizedUsername);
            user.setLastName("Admin");

            String normalizedNewPassword = normalize(newPassword);
            String normalizedConfirmPassword = normalize(confirmPassword);
            if (normalizedNewPassword != null || normalizedConfirmPassword != null) {
                if (normalizedNewPassword == null || normalizedConfirmPassword == null
                        || !normalizedNewPassword.equals(normalizedConfirmPassword)) {
                    throw new RuntimeException("New password and confirm password do not match.");
                }
                if (!STRONG_PASSWORD_PATTERN.matcher(normalizedNewPassword).matches()) {
                    throw new RuntimeException("Password must be at least 12 characters and include uppercase, lowercase, number, and special character.");
                }
                user.setPassword(passwordEncoder.encode(normalizedNewPassword));
            }

            admin.setStaffId(normalizedUsername);
            userRepository.save(user);
            adminRepository.save(admin);

            session.removeAttribute(OWNER_ADMIN_UPDATE_VERIFY_USER_ID);
            session.removeAttribute(OWNER_ADMIN_UPDATE_VERIFY_EMAIL);
            session.removeAttribute(OWNER_ADMIN_UPDATE_VERIFY_CODE);
            session.removeAttribute(OWNER_ADMIN_UPDATE_VERIFY_EXPIRY);
            session.removeAttribute(OWNER_ADMIN_UPDATE_VERIFY_CONFIRMED);

            redirectAttributes.addFlashAttribute("successMessage", "Admin account updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update admin: " + e.getMessage());
        }

        return "redirect:/owner/admin/manage";
    }

    @PostMapping("/admin/{userId}/delete")
    public String deleteAdminAccount(@PathVariable("userId") Integer userId,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        if (!isOwnerLoggedIn(session)) {
            return "redirect:/login";
        }

        try {
            Admin admin = adminRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Admin account not found."));

            List<Concern> assignedConcerns = concernRepository.findByAdmin_UserId(userId);
            if (!assignedConcerns.isEmpty()) {
                for (Concern concern : assignedConcerns) {
                    concern.setAdmin(null);
                }
                concernRepository.saveAll(assignedConcerns);
            }

            adminRepository.delete(admin);
            userRepository.deleteById(userId);

            redirectAttributes.addFlashAttribute("successMessage", "Admin account deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete admin: " + e.getMessage());
        }

        return "redirect:/owner/admin/manage";
    }

    @PostMapping("/admin/{userId}/email/send-code")
    @ResponseBody
    public ResponseEntity<Map<String, String>> sendAdminUpdateEmailCode(@PathVariable("userId") Integer userId,
                                                                        @RequestParam("email") String email,
                                                                        HttpSession session) {
        Map<String, String> result = new HashMap<>();
        if (!isOwnerLoggedIn(session)) {
            result.put("status", "error");
            result.put("message", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }

        Admin admin = adminRepository.findById(userId)
                .orElse(null);
        if (admin == null || admin.getUser() == null) {
            result.put("status", "error");
            result.put("message", "Admin account not found.");
            return ResponseEntity.badRequest().body(result);
        }

        String normalizedEmail = normalizeEmail(email);
        String currentEmail = normalizeEmail(admin.getUser().getEmail());

        if (normalizedEmail == null || !EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            result.put("status", "error");
            result.put("message", "Please enter a valid email address.");
            return ResponseEntity.badRequest().body(result);
        }

        if (currentEmail != null && currentEmail.equalsIgnoreCase(normalizedEmail)) {
            result.put("status", "error");
            result.put("message", "Email is unchanged.");
            return ResponseEntity.badRequest().body(result);
        }

        Optional<User> userWithEmail = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (userWithEmail.isPresent() && !userWithEmail.get().getUserId().equals(userId)) {
            result.put("status", "error");
            result.put("message", "Another account already uses this email.");
            return ResponseEntity.badRequest().body(result);
        }

        String code = String.valueOf(100000 + new Random().nextInt(900000));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(10);

        try {
            emailVerificationService.sendAdminCreationVerificationCode(normalizedEmail, code);
        } catch (Exception ex) {
            result.put("status", "error");
            result.put("message", "Could not send verification code. Check mail settings and try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }

        session.setAttribute(OWNER_ADMIN_UPDATE_VERIFY_USER_ID, userId);
        session.setAttribute(OWNER_ADMIN_UPDATE_VERIFY_EMAIL, normalizedEmail);
        session.setAttribute(OWNER_ADMIN_UPDATE_VERIFY_CODE, code);
        session.setAttribute(OWNER_ADMIN_UPDATE_VERIFY_EXPIRY, expiry);
        session.setAttribute(OWNER_ADMIN_UPDATE_VERIFY_CONFIRMED, false);

        result.put("status", "ok");
        result.put("message", "Verification code sent to the new email.");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/admin/{userId}/email/verify-code")
    @ResponseBody
    public ResponseEntity<Map<String, String>> verifyAdminUpdateEmailCode(@PathVariable("userId") Integer userId,
                                                                          @RequestParam("email") String email,
                                                                          @RequestParam("code") String code,
                                                                          HttpSession session) {
        Map<String, String> result = new HashMap<>();
        if (!isOwnerLoggedIn(session)) {
            result.put("status", "error");
            result.put("message", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }

        String normalizedEmail = normalizeEmail(email);
        String normalizedCode = code == null ? "" : code.trim();

        Integer savedUserId = (Integer) session.getAttribute(OWNER_ADMIN_UPDATE_VERIFY_USER_ID);
        String savedEmail = (String) session.getAttribute(OWNER_ADMIN_UPDATE_VERIFY_EMAIL);
        String savedCode = (String) session.getAttribute(OWNER_ADMIN_UPDATE_VERIFY_CODE);
        LocalDateTime expiry = (LocalDateTime) session.getAttribute(OWNER_ADMIN_UPDATE_VERIFY_EXPIRY);

        if (savedUserId == null || savedEmail == null || savedCode == null || expiry == null) {
            result.put("status", "error");
            result.put("message", "Please request a verification code first.");
            return ResponseEntity.badRequest().body(result);
        }

        if (!savedUserId.equals(userId)) {
            result.put("status", "error");
            result.put("message", "Verification request does not match this admin account.");
            return ResponseEntity.badRequest().body(result);
        }

        if (normalizedEmail == null || !savedEmail.equalsIgnoreCase(normalizedEmail)) {
            result.put("status", "error");
            result.put("message", "Email does not match verification request.");
            return ResponseEntity.badRequest().body(result);
        }

        if (LocalDateTime.now().isAfter(expiry)) {
            result.put("status", "error");
            result.put("message", "Verification code expired. Request a new code.");
            return ResponseEntity.badRequest().body(result);
        }

        if (!savedCode.equals(normalizedCode)) {
            result.put("status", "error");
            result.put("message", "Invalid verification code.");
            return ResponseEntity.badRequest().body(result);
        }

        session.setAttribute(OWNER_ADMIN_UPDATE_VERIFY_CONFIRMED, true);
        result.put("status", "ok");
        result.put("message", "Email verified successfully.");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/report/create")
    public String createReport(@ModelAttribute AnalyticsReportDTO reportDTO,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        if (!isOwnerLoggedIn(session)) {
            return "redirect:/login";
        }

        try {
            AnalyticsReport report = analyticsReportService.createReport(reportDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Analytics Report #" + report.getReportId() + " created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to create report: " + e.getMessage());
        }

        return "redirect:/owner/dashboard";
    }

    @PostMapping("/admin/send-code")
    @ResponseBody
    public ResponseEntity<Map<String, String>> sendAdminCreateCode(@RequestParam("email") String email,
                                                                   HttpSession session) {
        Map<String, String> result = new HashMap<>();
        if (!isOwnerLoggedIn(session)) {
            result.put("status", "error");
            result.put("message", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }

        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null || !EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            result.put("status", "error");
            result.put("message", "Please enter a valid email address.");
            return ResponseEntity.badRequest().body(result);
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            result.put("status", "error");
            result.put("message", "An account with this email already exists.");
            return ResponseEntity.badRequest().body(result);
        }

        String code = String.valueOf(100000 + new Random().nextInt(900000));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(10);

        try {
            emailVerificationService.sendAdminCreationVerificationCode(normalizedEmail, code);
        } catch (Exception ex) {
            result.put("status", "error");
            result.put("message", "Could not send verification code. Check mail settings and try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }

        session.setAttribute(OWNER_ADMIN_VERIFY_EMAIL, normalizedEmail);
        session.setAttribute(OWNER_ADMIN_VERIFY_CODE, code);
        session.setAttribute(OWNER_ADMIN_VERIFY_EXPIRY, expiry);
        session.setAttribute(OWNER_ADMIN_VERIFY_CONFIRMED, false);

        result.put("status", "ok");
        result.put("message", "Verification code sent to admin email.");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/admin/verify-code")
    @ResponseBody
    public ResponseEntity<Map<String, String>> verifyAdminCreateCode(@RequestParam("email") String email,
                                                                     @RequestParam("code") String code,
                                                                     HttpSession session) {
        Map<String, String> result = new HashMap<>();
        if (!isOwnerLoggedIn(session)) {
            result.put("status", "error");
            result.put("message", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }

        String normalizedEmail = normalizeEmail(email);
        String normalizedCode = code == null ? "" : code.trim();

        String savedEmail = (String) session.getAttribute(OWNER_ADMIN_VERIFY_EMAIL);
        String savedCode = (String) session.getAttribute(OWNER_ADMIN_VERIFY_CODE);
        LocalDateTime expiry = (LocalDateTime) session.getAttribute(OWNER_ADMIN_VERIFY_EXPIRY);

        if (savedEmail == null || savedCode == null || expiry == null) {
            result.put("status", "error");
            result.put("message", "Please send verification code first.");
            return ResponseEntity.badRequest().body(result);
        }

        if (normalizedEmail == null || !savedEmail.equalsIgnoreCase(normalizedEmail)) {
            result.put("status", "error");
            result.put("message", "Email does not match verification request.");
            return ResponseEntity.badRequest().body(result);
        }

        if (LocalDateTime.now().isAfter(expiry)) {
            result.put("status", "error");
            result.put("message", "Verification code expired. Request a new code.");
            return ResponseEntity.badRequest().body(result);
        }

        if (!savedCode.equals(normalizedCode)) {
            result.put("status", "error");
            result.put("message", "Invalid verification code.");
            return ResponseEntity.badRequest().body(result);
        }

        session.setAttribute(OWNER_ADMIN_VERIFY_CONFIRMED, true);
        result.put("status", "ok");
        result.put("message", "Admin email verified successfully.");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/admin/create")
    @ResponseBody
    public ResponseEntity<Map<String, String>> createAdminAccount(@RequestParam("email") String email,
                                                                  @RequestParam("username") String username,
                                                                  @RequestParam("password") String password,
                                                                  @RequestParam("confirmPassword") String confirmPassword,
                                                                  HttpSession session) {
        Map<String, String> result = new HashMap<>();
        if (!isOwnerLoggedIn(session)) {
            result.put("status", "error");
            result.put("message", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }

        String normalizedEmail = normalizeEmail(email);
        String normalizedUsername = normalize(username);
        Boolean emailVerified = (Boolean) session.getAttribute(OWNER_ADMIN_VERIFY_CONFIRMED);
        String verifiedEmail = (String) session.getAttribute(OWNER_ADMIN_VERIFY_EMAIL);

        if (!Boolean.TRUE.equals(emailVerified) || verifiedEmail == null || !verifiedEmail.equalsIgnoreCase(normalizedEmail)) {
            result.put("status", "error");
            result.put("message", "Please verify admin email before creating the account.");
            return ResponseEntity.badRequest().body(result);
        }

        if (normalizedEmail == null || !EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            result.put("status", "error");
            result.put("message", "Please enter a valid email address.");
            return ResponseEntity.badRequest().body(result);
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            result.put("status", "error");
            result.put("message", "An account with this email already exists.");
            return ResponseEntity.badRequest().body(result);
        }

        if (normalizedUsername == null) {
            result.put("status", "error");
            result.put("message", "Username is required.");
            return ResponseEntity.badRequest().body(result);
        }

        if (adminRepository.existsByStaffIdIgnoreCase(normalizedUsername)) {
            result.put("status", "error");
            result.put("message", "Username already in use.");
            return ResponseEntity.badRequest().body(result);
        }

        if (password == null || !password.equals(confirmPassword)) {
            result.put("status", "error");
            result.put("message", "Password and confirm password do not match.");
            return ResponseEntity.badRequest().body(result);
        }

        if (!STRONG_PASSWORD_PATTERN.matcher(password).matches()) {
            result.put("status", "error");
            result.put("message", "Password must be at least 12 characters and include uppercase, lowercase, number, and special character.");
            return ResponseEntity.badRequest().body(result);
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setFirstName(normalizedUsername);
        user.setLastName("Admin");
        user.setPassword(passwordEncoder.encode(password));
        user.setRegistrationStatus("APPROVED");
        user = userRepository.save(user);

        Admin admin = new Admin();
        admin.setUser(user);
        admin.setStaffId(normalizedUsername);
        adminRepository.save(admin);

        session.removeAttribute(OWNER_ADMIN_VERIFY_EMAIL);
        session.removeAttribute(OWNER_ADMIN_VERIFY_CODE);
        session.removeAttribute(OWNER_ADMIN_VERIFY_EXPIRY);
        session.removeAttribute(OWNER_ADMIN_VERIFY_CONFIRMED);

        result.put("status", "ok");
        result.put("message", "Admin account created successfully.");
        return ResponseEntity.ok(result);
    }

    // ========================
    // REST API Endpoints
    // ========================

    /**
     * Get total concern count for a given category
     */
    @GetMapping("/api/concerns/count")
    @ResponseBody
    public Map<String, Object> getConcernCountByCategory(@RequestParam("category") String category,
                                                          HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (!isOwnerLoggedIn(session)) {
            result.put("error", "Unauthorized");
            return result;
        }
        long count = concernRepository.countByCategory(category);
        result.put("count", count);
        result.put("category", category);
        return result;
    }

    /**
     * Get average resolution time for a given category (in hours)
     * Resolution time = AdminReply.replyTime - Concern.createdTime
     */
    @GetMapping("/api/resolution-time")
    @ResponseBody
    public Map<String, Object> getAvgResolutionTime(@RequestParam("category") String category,
                                                     HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (!isOwnerLoggedIn(session)) {
            result.put("error", "Unauthorized");
            return result;
        }

        List<AdminReply> replies = adminReplyRepository.findByConcern_Category(category);

        if (replies.isEmpty()) {
            result.put("avgHours", 0);
            result.put("count", 0);
            result.put("message", "No resolved concerns in this category");
        } else {
            // Group replies by concern, take the earliest reply per concern
            Map<Integer, AdminReply> earliestReplyPerConcern = new LinkedHashMap<>();
            for (AdminReply reply : replies) {
                int cid = reply.getConcern().getConcernId();
                if (!earliestReplyPerConcern.containsKey(cid) ||
                    reply.getReplyTime().isBefore(earliestReplyPerConcern.get(cid).getReplyTime())) {
                    earliestReplyPerConcern.put(cid, reply);
                }
            }

            double totalHours = 0;
            int validCount = 0;
            for (AdminReply reply : earliestReplyPerConcern.values()) {
                if (reply.getConcern().getCreatedTime() != null && reply.getReplyTime() != null) {
                    long minutes = java.time.Duration.between(
                            reply.getConcern().getCreatedTime(), reply.getReplyTime()).toMinutes();
                    totalHours += minutes / 60.0;
                    validCount++;
                }
            }

            if (validCount > 0) {
                double avg = totalHours / validCount;
                BigDecimal avgRounded = BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
                result.put("avgHours", avgRounded);
                result.put("count", validCount);
            } else {
                result.put("avgHours", 0);
                result.put("count", 0);
                result.put("message", "No timing data available");
            }
        }

        result.put("category", category);
        return result;
    }

    /**
     * Get all admins with their names
     */
    @GetMapping("/api/admins")
    @ResponseBody
    public List<Map<String, Object>> getAllAdmins(HttpSession session) {
        if (!isOwnerLoggedIn(session)) {
            return Collections.emptyList();
        }
        List<Admin> admins = adminRepository.findAll();
        return admins.stream().map(admin -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("userId", admin.getUserId());
            map.put("staffId", admin.getStaffId());
            map.put("firstName", admin.getUser().getFirstName());
            map.put("lastName", admin.getUser().getLastName());
            map.put("fullName", admin.getUser().getFirstName() + " " + admin.getUser().getLastName());
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * Get sentiment score (average feedback rating) for a given admin
     */
    @GetMapping("/api/sentiment")
    @ResponseBody
    public Map<String, Object> getSentimentForAdmin(@RequestParam("adminId") Integer adminId,
                                                     HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (!isOwnerLoggedIn(session)) {
            result.put("error", "Unauthorized");
            return result;
        }

        List<Feedback> feedbacks = feedbackRepository.findByRatedAdminUserId(adminId);
        if (feedbacks.isEmpty()) {
            result.put("score", 0);
            result.put("count", 0);
            result.put("message", "No feedback received for this admin");
        } else {
            double avg = feedbacks.stream()
                    .mapToInt(Feedback::getRating)
                    .average()
                    .orElse(0.0);
            BigDecimal score = BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
            result.put("score", score);
            result.put("count", feedbacks.size());
        }
        result.put("adminId", adminId);
        return result;
    }

    /**
     * Refresh a single report — recalculate its stats from live data
     */
    @PostMapping("/api/report/refresh/{id}")
    @ResponseBody
    public Map<String, Object> refreshReport(@PathVariable("id") Integer reportId,
                                              HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (!isOwnerLoggedIn(session)) {
            result.put("error", "Unauthorized");
            return result;
        }

        Optional<AnalyticsReport> optReport = analyticsReportRepository.findById(reportId);
        if (optReport.isEmpty()) {
            result.put("error", "Report not found");
            return result;
        }

        AnalyticsReport report = optReport.get();
        recalculateReport(report);
        analyticsReportRepository.save(report);

        result.put("success", true);
        result.put("reportId", report.getReportId());
        result.put("totalConcerns", report.getTotalConcerns());
        result.put("avgResolutionTime", report.getAvgResolutionTime());
        result.put("sentimentTrendScore", report.getSentimentTrendScore());
        result.put("mostFrequentCategory", report.getMostFrequentCategory());
        return result;
    }

    /**
     * Refresh ALL reports — recalculate stats for every saved report
     */
    @PostMapping("/api/reports/refresh-all")
    @ResponseBody
    public Map<String, Object> refreshAllReports(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (!isOwnerLoggedIn(session)) {
            result.put("error", "Unauthorized");
            return result;
        }

        List<AnalyticsReport> allReports = analyticsReportRepository.findAll();
        for (AnalyticsReport report : allReports) {
            recalculateReport(report);
        }
        analyticsReportRepository.saveAll(allReports);

        result.put("success", true);
        result.put("updatedCount", allReports.size());
        return result;
    }

    /**
     * Recalculate a report's totalConcerns, avgResolutionTime, and sentimentTrendScore
     * from live data based on its category and adminIdFk.
     */
    private void recalculateReport(AnalyticsReport report) {
        String category = report.getMostFrequentCategory();

        // 1. Total Concerns
        if (category != null) {
            long count = concernRepository.countByCategory(category);
            report.setTotalConcerns((int) count);
        }

        // 2. Avg Resolution Time
        if (category != null) {
            List<AdminReply> replies = adminReplyRepository.findByConcern_Category(category);
            Map<Integer, AdminReply> earliest = new LinkedHashMap<>();
            for (AdminReply reply : replies) {
                int cid = reply.getConcern().getConcernId();
                if (!earliest.containsKey(cid) ||
                    reply.getReplyTime().isBefore(earliest.get(cid).getReplyTime())) {
                    earliest.put(cid, reply);
                }
            }
            double totalHours = 0;
            int validCount = 0;
            for (AdminReply reply : earliest.values()) {
                if (reply.getConcern().getCreatedTime() != null && reply.getReplyTime() != null) {
                    long minutes = java.time.Duration.between(
                            reply.getConcern().getCreatedTime(), reply.getReplyTime()).toMinutes();
                    totalHours += minutes / 60.0;
                    validCount++;
                }
            }
            if (validCount > 0) {
                report.setAvgResolutionTime(
                        BigDecimal.valueOf(totalHours / validCount).setScale(2, RoundingMode.HALF_UP));
            }
        }

        // 3. Sentiment Score
        if (report.getAdminIdFk() != null) {
            List<Feedback> feedbacks = feedbackRepository.findByRatedAdminUserId(report.getAdminIdFk());
            if (!feedbacks.isEmpty()) {
                double avg = feedbacks.stream().mapToInt(Feedback::getRating).average().orElse(0.0);
                report.setSentimentTrendScore(
                        BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            }
        }
    }

    // ========================
    // Broadcast Notifications
    // ========================

    @GetMapping("/notifications")
    public String showNotificationsPage(HttpSession session, Model model) {
        if (!isOwnerLoggedIn(session)) {
            return "redirect:/login";
        }
        List<Notification> broadcastNotifications = notificationService.getAllBroadcastNotifications();
        model.addAttribute("notifications", broadcastNotifications);
        return "owner-notifications";
    }

    @PostMapping("/notifications/send")
    public String sendBroadcastNotification(@RequestParam("title") String title,
                                            @RequestParam("message") String message,
                                            @RequestParam("targetAudience") String targetAudience,
                                            HttpSession session,
                                            RedirectAttributes redirectAttributes) {
        if (!isOwnerLoggedIn(session)) {
            return "redirect:/login";
        }
        try {
            notificationService.createBroadcastNotification(title, message, targetAudience, null);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Notification sent successfully to " + targetAudience + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to send notification: " + e.getMessage());
        }
        return "redirect:/owner/notifications";
    }

    private boolean isOwnerLoggedIn(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("ownerLoggedIn"));
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
