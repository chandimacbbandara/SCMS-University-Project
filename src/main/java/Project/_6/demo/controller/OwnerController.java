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
import Project._6.demo.service.AdminService;
import Project._6.demo.service.EmailVerificationService;
import Project._6.demo.service.FaqManagementService;
import Project._6.demo.entity.Tip;
import Project._6.demo.entity.Faq;
import Project._6.demo.service.NotificationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/owner")
public class OwnerController {

    private final AnalyticsReportService analyticsReportService;
    private final AdminService adminService;
    private final AnalyticsReportRepository analyticsReportRepository;
    private final ConcernRepository concernRepository;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final FeedbackRepository feedbackRepository;
    private final AdminReplyRepository adminReplyRepository;
    private final NotificationService notificationService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final FaqManagementService faqManagementService;

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
    private static final String ALL_CATEGORIES = "All Categories";
    private static final String ALL_PRIORITIES = "All Priorities";
    private static final String ALL_TIME = "All Time";
    private static final String STATUS_DRAFT = "Draft";

    public OwnerController(AnalyticsReportService analyticsReportService,
                           AdminService adminService,
                           AnalyticsReportRepository analyticsReportRepository,
                           ConcernRepository concernRepository,
                           AdminRepository adminRepository,
                           UserRepository userRepository,
                           FeedbackRepository feedbackRepository,
                           AdminReplyRepository adminReplyRepository,
                           NotificationService notificationService,
                           EmailVerificationService emailVerificationService,
                           PasswordEncoder passwordEncoder,
                           FaqManagementService faqManagementService) {
        this.analyticsReportService = analyticsReportService;
                this.adminService = adminService;
        this.analyticsReportRepository = analyticsReportRepository;
        this.concernRepository = concernRepository;
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.feedbackRepository = feedbackRepository;
        this.adminReplyRepository = adminReplyRepository;
        this.notificationService = notificationService;
        this.emailVerificationService = emailVerificationService;
        this.passwordEncoder = passwordEncoder;
        this.faqManagementService = faqManagementService;
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
            if (reportDTO.getAdminIdFk() == null) {
                throw new RuntimeException("Please select an admin before creating the report.");
            }

            String selectedCategory = normalizeReportCategory(reportDTO.getMostFrequentCategory());
            ReportMetrics metrics = calculateReportMetrics(reportDTO.getAdminIdFk(), selectedCategory);

            reportDTO.setTimePeriod(ALL_TIME);
            reportDTO.setMostFrequentCategory(selectedCategory);
            reportDTO.setTotalConcerns(metrics.totalConcerns());
            reportDTO.setAvgResolutionTime(metrics.avgResolutionHours());
            reportDTO.setSentimentTrendScore(metrics.sentimentScore());
            reportDTO.setEvidenceImageCount(metrics.evidenceImageCount());

            AnalyticsReport report = analyticsReportService.createReport(reportDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Analytics Report #" + report.getReportId() + " created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to create report: " + e.getMessage());
        }

        return "redirect:/owner/dashboard";
    }

    @PostMapping("/report/delete/{id}")
    public String deleteReport(@PathVariable("id") Integer reportId,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        if (!isOwnerLoggedIn(session)) {
            return "redirect:/login";
        }

        try {
            if (!analyticsReportRepository.existsById(reportId)) {
                throw new RuntimeException("Report not found.");
            }
            analyticsReportRepository.deleteById(reportId);
            redirectAttributes.addFlashAttribute("successMessage", "Analytics Report #" + reportId + " deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete report: " + e.getMessage());
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
        assignUserIdIfRequired(user);
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

    @GetMapping("/api/report/metrics")
    @ResponseBody
    public Map<String, Object> getReportMetrics(@RequestParam("adminId") Integer adminId,
                                                @RequestParam(value = "category", required = false) String category,
                                                HttpSession session) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!isOwnerLoggedIn(session)) {
            result.put("error", "Unauthorized");
            return result;
        }
        if (adminId == null || !adminRepository.existsById(adminId)) {
            result.put("error", "Please select a valid admin.");
            return result;
        }

        ReportMetrics metrics = calculateReportMetrics(adminId, category);
        result.put("adminId", adminId);
        result.put("selectedCategory", metrics.selectedCategory());
        result.put("topCategory", metrics.topCategory());
        result.put("totalConcerns", metrics.totalConcerns());
        result.put("avgHours", metrics.avgResolutionHours());
        result.put("resolvedCount", metrics.resolvedConcerns());
        result.put("sentimentScore", metrics.sentimentScore());
        result.put("sentimentCount", metrics.sentimentCount());
        result.put("evidenceImageCount", metrics.evidenceImageCount());
        if (metrics.totalConcerns() == 0) {
            result.put("message", "No concerns found for the selected admin and category.");
        }
        return result;
    }

    @GetMapping("/api/analytics/charts")
    @ResponseBody
    public Map<String, Object> getOwnerAnalyticsCharts(HttpSession session) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!isOwnerLoggedIn(session)) {
            result.put("error", "Unauthorized");
            return result;
        }

        List<Concern> concerns = concernRepository.findAllByOrderByCreatedTimeDesc().stream()
                .filter(concern -> !isDraftStatus(concern.getStatus()))
                .collect(Collectors.toList());

        Map<String, Long> categoryDistribution = concerns.stream()
                .map(Concern::getCategory)
                .filter(StringUtils::hasText)
                .collect(Collectors.groupingBy(cat -> cat, Collectors.counting()));

        List<Map.Entry<String, Long>> sortedCategoryEntries = categoryDistribution.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toList());

        List<String> categoryLabels = sortedCategoryEntries.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        List<Long> categoryCounts = sortedCategoryEntries.stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        long resolvedCount = concerns.stream().filter(c -> isResolvedStatus(c.getStatus())).count();
        long pendingCount = concerns.stream().filter(c -> isPendingStatus(c.getStatus())).count();
        long rejectedCount = concerns.stream().filter(c -> isRejectedStatus(c.getStatus())).count();

        result.put("categoryLabels", categoryLabels);
        result.put("categoryCounts", categoryCounts);

        Map<String, Long> statusDistribution = new LinkedHashMap<>();
        statusDistribution.put("resolved", resolvedCount);
        statusDistribution.put("pending", pendingCount);
        statusDistribution.put("rejected", rejectedCount);
        result.put("statusDistribution", statusDistribution);
        result.put("totalConcerns", concerns.size());

        return result;
    }

    @PostMapping("/api/concerns/rejected/delete-all")
    @ResponseBody
    public Map<String, Object> deleteAllRejectedConcerns(HttpSession session) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!isOwnerLoggedIn(session)) {
            result.put("error", "Unauthorized");
            return result;
        }

        long deletedCount = adminService.deleteAllRejectedConcernsPermanently();
        result.put("success", true);
        result.put("deletedCount", deletedCount);
        return result;
    }

    @GetMapping("/api/reports/monthly")
    @ResponseBody
    public Map<String, Object> getMonthlyReportData(@RequestParam(value = "month", required = false) String month,
                                                    @RequestParam(value = "category", required = false) String category,
                                                    @RequestParam(value = "priority", required = false) String priority,
                                                    HttpSession session) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!isOwnerLoggedIn(session)) {
            result.put("error", "Unauthorized");
            return result;
        }

        YearMonth parsedMonth = YearMonth.now();
        if (StringUtils.hasText(month)) {
            try {
                parsedMonth = YearMonth.parse(month.trim());
            } catch (DateTimeParseException ignored) {
                parsedMonth = YearMonth.now();
            }
        }
        final YearMonth selectedMonth = parsedMonth;

        String selectedCategory = normalizeReportCategory(category);
        String selectedPriority = normalizePriorityFilter(priority);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<Concern> filteredConcerns = concernRepository.findAllByOrderByCreatedTimeDesc().stream()
            .filter(concern -> !isDraftStatus(concern.getStatus()))
                .filter(concern -> concern.getCreatedTime() != null)
                .filter(concern -> YearMonth.from(concern.getCreatedTime()).equals(selectedMonth))
                .filter(concern -> isAllCategories(selectedCategory)
                        || selectedCategory.equalsIgnoreCase(defaultText(concern.getCategory(), ALL_CATEGORIES)))
                .filter(concern -> isAllPriorities(selectedPriority)
                        || selectedPriority.equalsIgnoreCase(defaultText(concern.getAiPriorityLevel(), "—")))
                .sorted(Comparator.comparing(Concern::getCreatedTime).reversed())
                .collect(Collectors.toList());

        List<Integer> concernIds = filteredConcerns.stream()
            .map(Concern::getConcernId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        Map<Integer, AdminReply> latestReplyByConcernId = new HashMap<>();
        if (!concernIds.isEmpty()) {
            List<AdminReply> replies = adminReplyRepository.findByConcern_ConcernIdIn(concernIds);
            latestReplyByConcernId = replies.stream()
                .filter(reply -> reply.getConcern() != null
                    && reply.getConcern().getConcernId() != null
                    && reply.getReplyTime() != null)
                .collect(Collectors.toMap(
                    reply -> reply.getConcern().getConcernId(),
                    reply -> reply,
                    (existing, candidate) -> candidate.getReplyTime().isAfter(existing.getReplyTime()) ? candidate : existing
                ));
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        int rowNo = 1;
        for (Concern concern : filteredConcerns) {
            AdminReply latestReply = latestReplyByConcernId.get(concern.getConcernId());

            String studentId = "N/A";
            String studentName = "Unknown Student";
            if (concern.getStudent() != null) {
            if (StringUtils.hasText(concern.getStudent().getStudentId())) {
                studentId = concern.getStudent().getStudentId().trim();
            }
            if (concern.getStudent().getUser() != null) {
                String firstName = defaultText(concern.getStudent().getUser().getFirstName(), "");
                String lastName = defaultText(concern.getStudent().getUser().getLastName(), "");
                String fullName = (firstName + " " + lastName).trim();
                if (StringUtils.hasText(fullName)) {
                studentName = fullName;
                }
            }
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rowNo", rowNo++);
            row.put("concernId", concern.getConcernId());
            row.put("studentId", studentId);
            row.put("studentName", studentName);
            row.put("category", defaultText(concern.getCategory(), "N/A"));
            row.put("priority", defaultText(concern.getAiPriorityLevel(), "—"));
            row.put("status", normalizeStatusLabel(concern.getStatus()));
            row.put("date", concern.getCreatedTime().format(dateFormatter));
            row.put("concernMessage", defaultText(concern.getMessage(), "No message"));
            row.put("adminReply", latestReply != null
                ? defaultText(latestReply.getReplyMessage(), "No reply yet")
                : "No reply yet");
            rows.add(row);
        }

        result.put("month", selectedMonth.toString());
        result.put("selectedCategory", selectedCategory);
        result.put("selectedPriority", selectedPriority);
        result.put("count", rows.size());
        result.put("rows", rows);
        return result;
    }

    /**
     * Backward-compatible count endpoint.
     */
    @GetMapping("/api/concerns/count")
    @ResponseBody
    public Map<String, Object> getConcernCountByCategory(@RequestParam(value = "category", required = false) String category,
                                                          @RequestParam(value = "adminId", required = false) Integer adminId,
                                                          HttpSession session) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!isOwnerLoggedIn(session)) {
            result.put("error", "Unauthorized");
            return result;
        }

        String normalizedCategory = normalizeReportCategory(category);
        if (adminId != null) {
            ReportMetrics metrics = calculateReportMetrics(adminId, normalizedCategory);
            result.put("count", metrics.totalConcerns());
            result.put("category", metrics.selectedCategory());
            result.put("adminId", adminId);
            return result;
        }

        long count = isAllCategories(normalizedCategory)
                ? concernRepository.findAllByOrderByCreatedTimeDesc().stream()
                    .filter(concern -> !isDraftStatus(concern.getStatus()))
                    .count()
                : concernRepository.findAllByOrderByCreatedTimeDesc().stream()
                    .filter(concern -> !isDraftStatus(concern.getStatus()))
                    .filter(concern -> normalizedCategory.equalsIgnoreCase(defaultText(concern.getCategory(), ALL_CATEGORIES)))
                    .count();
        result.put("count", count);
        result.put("category", normalizedCategory);
        return result;
    }

    /**
     * Backward-compatible avg-resolution endpoint.
     */
    @GetMapping("/api/resolution-time")
    @ResponseBody
    public Map<String, Object> getAvgResolutionTime(@RequestParam(value = "category", required = false) String category,
                                                    @RequestParam(value = "adminId", required = false) Integer adminId,
                                                    HttpSession session) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!isOwnerLoggedIn(session)) {
            result.put("error", "Unauthorized");
            return result;
        }

        if (adminId != null) {
            ReportMetrics metrics = calculateReportMetrics(adminId, category);
            result.put("avgHours", metrics.avgResolutionHours());
            result.put("count", metrics.resolvedConcerns());
            result.put("category", metrics.selectedCategory());
            result.put("adminId", adminId);
            if (metrics.resolvedConcerns() == 0) {
                result.put("message", "No resolved concerns found.");
            }
            return result;
        }

        String normalizedCategory = normalizeReportCategory(category);
        List<AdminReply> replies = isAllCategories(normalizedCategory)
                ? adminReplyRepository.findAll()
                : adminReplyRepository.findByConcern_Category(normalizedCategory);

        Map<Integer, AdminReply> earliestReplyPerConcern = new LinkedHashMap<>();
        for (AdminReply reply : replies) {
            if (reply.getConcern() == null || reply.getConcern().getConcernId() == null || reply.getReplyTime() == null) {
                continue;
            }
            if (isDraftStatus(reply.getConcern().getStatus())) {
                continue;
            }
            int concernId = reply.getConcern().getConcernId();
            if (!earliestReplyPerConcern.containsKey(concernId)
                    || reply.getReplyTime().isBefore(earliestReplyPerConcern.get(concernId).getReplyTime())) {
                earliestReplyPerConcern.put(concernId, reply);
            }
        }

        double totalHours = 0;
        int validCount = 0;
        for (AdminReply reply : earliestReplyPerConcern.values()) {
            if (reply.getConcern().getCreatedTime() == null) {
                continue;
            }
            long minutes = Duration.between(reply.getConcern().getCreatedTime(), reply.getReplyTime()).toMinutes();
            if (minutes < 0) {
                continue;
            }
            totalHours += minutes / 60.0;
            validCount++;
        }

        BigDecimal avg = validCount > 0
                ? BigDecimal.valueOf(totalHours / validCount).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        result.put("avgHours", avg);
        result.put("count", validCount);
        result.put("category", normalizedCategory);
        if (validCount == 0) {
            result.put("message", "No resolved concerns in this selection");
        }
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
        List<Admin> admins = adminRepository.findAll().stream()
                .sorted(Comparator.comparing(Admin::getUserId))
                .collect(Collectors.toList());
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
        Map<String, Object> result = new LinkedHashMap<>();
        if (!isOwnerLoggedIn(session)) {
            result.put("error", "Unauthorized");
            return result;
        }

        ReportMetrics metrics = calculateReportMetrics(adminId, ALL_CATEGORIES);
        result.put("score", metrics.sentimentScore());
        result.put("count", metrics.sentimentCount());
        if (metrics.sentimentCount() == 0) {
            result.put("message", "No feedback received for this admin");
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
        result.put("evidenceImageCount", report.getEvidenceImageCount());
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
     * Recalculate a report's metrics from live data based on its category selection and admin.
     */
    private void recalculateReport(AnalyticsReport report) {
        if (report.getAdminIdFk() == null) {
            report.setTotalConcerns(0);
            report.setAvgResolutionTime(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            report.setSentimentTrendScore(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            report.setEvidenceImageCount(0);
            report.setTimePeriod(ALL_TIME);
            if (!StringUtils.hasText(report.getMostFrequentCategory())) {
                report.setMostFrequentCategory(ALL_CATEGORIES);
            }
            return;
        }

        String selectedCategory = normalizeReportCategory(report.getMostFrequentCategory());
        ReportMetrics metrics = calculateReportMetrics(report.getAdminIdFk(), selectedCategory);

        report.setTimePeriod(ALL_TIME);
        report.setMostFrequentCategory(selectedCategory);
        report.setTotalConcerns(metrics.totalConcerns());
        report.setAvgResolutionTime(metrics.avgResolutionHours());
        report.setSentimentTrendScore(metrics.sentimentScore());
        report.setEvidenceImageCount(metrics.evidenceImageCount());
    }

    private ReportMetrics calculateReportMetrics(Integer adminId, String category) {
        String selectedCategory = normalizeReportCategory(category);
        boolean allCategoriesSelected = isAllCategories(selectedCategory);

        List<Concern> concerns = allCategoriesSelected
                ? concernRepository.findByAdmin_UserId(adminId)
                : concernRepository.findByAdmin_UserIdAndCategory(adminId, selectedCategory);

        concerns = concerns.stream()
            .filter(concern -> !isDraftStatus(concern.getStatus()))
            .collect(Collectors.toList());

        int totalConcerns = concerns.size();
        int evidenceImageCount = (int) concerns.stream()
                .filter(concern -> StringUtils.hasText(concern.getEvidencePath()))
                .count();

        String topCategory = concerns.stream()
                .map(Concern::getCategory)
                .filter(StringUtils::hasText)
                .collect(Collectors.groupingBy(cat -> cat, Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .orElse("N/A");

        List<AdminReply> replies = allCategoriesSelected
                ? adminReplyRepository.findByAdmin_UserId(adminId)
                : adminReplyRepository.findByAdmin_UserIdAndConcern_Category(adminId, selectedCategory);

        Map<Integer, AdminReply> earliestReplyPerConcern = new LinkedHashMap<>();
        for (AdminReply reply : replies) {
            if (reply.getConcern() == null || reply.getConcern().getConcernId() == null || reply.getReplyTime() == null) {
                continue;
            }
            if (isDraftStatus(reply.getConcern().getStatus())) {
                continue;
            }
            Integer concernId = reply.getConcern().getConcernId();
            if (!earliestReplyPerConcern.containsKey(concernId)
                    || reply.getReplyTime().isBefore(earliestReplyPerConcern.get(concernId).getReplyTime())) {
                earliestReplyPerConcern.put(concernId, reply);
            }
        }

        double totalResolutionHours = 0;
        int resolvedConcerns = 0;
        for (AdminReply earliestReply : earliestReplyPerConcern.values()) {
            LocalDateTime createdTime = earliestReply.getConcern().getCreatedTime();
            LocalDateTime replyTime = earliestReply.getReplyTime();
            if (createdTime == null || replyTime == null || replyTime.isBefore(createdTime)) {
                continue;
            }
            long minutes = Duration.between(createdTime, replyTime).toMinutes();
            totalResolutionHours += minutes / 60.0;
            resolvedConcerns++;
        }

        BigDecimal avgResolutionHours = resolvedConcerns > 0
                ? BigDecimal.valueOf(totalResolutionHours / resolvedConcerns).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        List<Feedback> feedbacks = feedbackRepository.findByRatedAdminUserId(adminId);
        int sentimentCount = feedbacks.size();
        BigDecimal sentimentScore = sentimentCount > 0
                ? BigDecimal.valueOf(feedbacks.stream().mapToInt(Feedback::getRating).average().orElse(0.0))
                        .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return new ReportMetrics(
                totalConcerns,
                avgResolutionHours,
                resolvedConcerns,
                sentimentScore,
                sentimentCount,
                evidenceImageCount,
                selectedCategory,
                topCategory
        );
    }

    private String normalizeReportCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return ALL_CATEGORIES;
        }
        return category.trim();
    }

    private String normalizePriorityFilter(String priority) {
        if (!StringUtils.hasText(priority)) {
            return ALL_PRIORITIES;
        }
        return priority.trim();
    }

    private boolean isAllCategories(String category) {
        return !StringUtils.hasText(category) || ALL_CATEGORIES.equalsIgnoreCase(category.trim());
    }

    private boolean isAllPriorities(String priority) {
        return !StringUtils.hasText(priority) || ALL_PRIORITIES.equalsIgnoreCase(priority.trim());
    }

    private boolean isResolvedStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        String normalized = status.trim();
        return "Complete".equalsIgnoreCase(normalized) || "Resolved".equalsIgnoreCase(normalized);
    }

    private boolean isPendingStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return true;
        }
        String normalized = status.trim();
        return "Pending".equalsIgnoreCase(normalized)
                || "In Progress".equalsIgnoreCase(normalized)
                || "Meeting Scheduled".equalsIgnoreCase(normalized);
    }

    private boolean isRejectedStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        String normalized = status.trim();
        return "Rejected".equalsIgnoreCase(normalized) || "Deleted".equalsIgnoreCase(normalized);
    }

    private boolean isDraftStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        return STATUS_DRAFT.equalsIgnoreCase(status.trim());
    }

    private String normalizeStatusLabel(String status) {
        if (isResolvedStatus(status)) {
            return "Resolved";
        }
        if (isRejectedStatus(status)) {
            return "Rejected";
        }
        if (isPendingStatus(status)) {
            return "Pending";
        }
        return defaultText(status, "Pending");
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private record ReportMetrics(
            int totalConcerns,
            BigDecimal avgResolutionHours,
            int resolvedConcerns,
            BigDecimal sentimentScore,
            int sentimentCount,
            int evidenceImageCount,
            String selectedCategory,
            String topCategory
    ) {}

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

    @GetMapping("/notifications/delete/{id}")
    public String deleteNotification(@PathVariable("id") Integer id,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        if (!isOwnerLoggedIn(session)) {
            return "redirect:/login";
        }
        try {
            notificationService.deleteNotification(id);
            redirectAttributes.addFlashAttribute("successMessage", "Notification deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete notification: " + e.getMessage());
        }
        return "redirect:/owner/notifications";
    }

    @GetMapping("/notifications/update/{id}")
    public String showUpdateNotificationPage(@PathVariable("id") Integer id,
                                             HttpSession session,
                                             Model model,
                                             RedirectAttributes redirectAttributes) {
        if (!isOwnerLoggedIn(session)) {
            return "redirect:/login";
        }
        try {
            Notification notification = notificationService.getNotificationById(id);
            model.addAttribute("notification", notification);
            return "owner-notification-update";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
            return "redirect:/owner/notifications";
        }
    }

    @PostMapping("/notifications/update/{id}")
    public String updateBroadcastNotification(@PathVariable("id") Integer id,
                                              @RequestParam("title") String title,
                                              @RequestParam("message") String message,
                                              @RequestParam("targetAudience") String targetAudience,
                                              HttpSession session,
                                              RedirectAttributes redirectAttributes) {
        if (!isOwnerLoggedIn(session)) {
            return "redirect:/login";
        }
        try {
            notificationService.updateBroadcastNotification(id, title, message, targetAudience);
            redirectAttributes.addFlashAttribute("successMessage", "Notification updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/owner/notifications/update/" + id;
        }
        return "redirect:/owner/notifications";
    }

    // =====================================
    // FAQ & TIPS MANAGEMENT
    // =====================================

    @GetMapping("/faq")
    public String showFaqManagement(HttpSession session, Model model) {
        if (!isOwnerLoggedIn(session)) {
            return "redirect:/login";
        }
        model.addAttribute("tips", faqManagementService.getAllTips());
        model.addAttribute("faqs", faqManagementService.getAllFaqs());
        return "owner-faq";
    }

    @PostMapping("/faq/tip/create")
    public String createTip(@RequestParam("title") String title,
                            @RequestParam("description") String description,
                            @RequestParam("iconClass") String iconClass,
                            HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isOwnerLoggedIn(session)) return "redirect:/login";
        
        try {
            Tip tip = new Tip(title.trim(), description.trim(), iconClass.trim());
            faqManagementService.saveTip(tip);
            redirectAttributes.addFlashAttribute("successMessage", "Tip created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to create Tip: " + e.getMessage());
        }
        return "redirect:/owner/faq";
    }

    @PostMapping("/faq/tip/delete/{id}")
    public String deleteTip(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isOwnerLoggedIn(session)) return "redirect:/login";
        try {
            faqManagementService.deleteTip(id);
            redirectAttributes.addFlashAttribute("successMessage", "Tip deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete Tip.");
        }
        return "redirect:/owner/faq";
    }

    @PostMapping("/faq/faq/create")
    public String createFaq(@RequestParam("question") String question,
                            @RequestParam("answer") String answer,
                            HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isOwnerLoggedIn(session)) return "redirect:/login";

        try {
            Faq faq = new Faq(question.trim(), answer.trim());
            faqManagementService.saveFaq(faq);
            redirectAttributes.addFlashAttribute("successMessage", "FAQ created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to create FAQ: " + e.getMessage());
        }
        return "redirect:/owner/faq";
    }

    @PostMapping("/faq/faq/delete/{id}")
    public String deleteFaq(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isOwnerLoggedIn(session)) return "redirect:/login";
        try {
            faqManagementService.deleteFaq(id);
            redirectAttributes.addFlashAttribute("successMessage", "FAQ deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete FAQ.");
        }
        return "redirect:/owner/faq";
    }

    @GetMapping("/faq/tip/update/{id}")
    public String showUpdateTip(@PathVariable("id") Long id, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!isOwnerLoggedIn(session)) return "redirect:/login";
        try {
            Tip tip = faqManagementService.getTipById(id);
            if(tip == null) throw new Exception("Tip not found");
            model.addAttribute("tip", tip);
            return "owner-faq-tip-update";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/owner/faq";
        }
    }

    @PostMapping("/faq/tip/update/{id}")
    public String updateTip(@PathVariable("id") Long id, 
                            @RequestParam("title") String title,
                            @RequestParam("description") String description,
                            @RequestParam("iconClass") String iconClass,
                            HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isOwnerLoggedIn(session)) return "redirect:/login";
        try {
            faqManagementService.updateTip(id, title.trim(), description.trim(), iconClass.trim());
            redirectAttributes.addFlashAttribute("successMessage", "Tip updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/owner/faq";
    }

    @GetMapping("/faq/faq/update/{id}")
    public String showUpdateFaq(@PathVariable("id") Long id, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!isOwnerLoggedIn(session)) return "redirect:/login";
        try {
            Faq faq = faqManagementService.getFaqById(id);
            if(faq == null) throw new Exception("FAQ not found");
            model.addAttribute("faq", faq);
            return "owner-faq-faq-update";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/owner/faq";
        }
    }

    @PostMapping("/faq/faq/update/{id}")
    public String updateFaq(@PathVariable("id") Long id, 
                            @RequestParam("question") String question,
                            @RequestParam("answer") String answer,
                            HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isOwnerLoggedIn(session)) return "redirect:/login";
        try {
            faqManagementService.updateFaq(id, question.trim(), answer.trim());
            redirectAttributes.addFlashAttribute("successMessage", "FAQ updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/owner/faq";
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

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
