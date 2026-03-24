package Project._6.demo.controller;

import Project._6.demo.dto.AnalyticsReportDTO;
import Project._6.demo.entity.Admin;
import Project._6.demo.entity.AdminReply;
import Project._6.demo.entity.AnalyticsReport;
import Project._6.demo.entity.Concern;
import Project._6.demo.entity.Feedback;
import Project._6.demo.entity.Notification;
import Project._6.demo.repository.AnalyticsReportRepository;
import Project._6.demo.repository.AdminReplyRepository;
import Project._6.demo.repository.AdminRepository;
import Project._6.demo.repository.ConcernRepository;
import Project._6.demo.repository.FeedbackRepository;
import Project._6.demo.service.AnalyticsReportService;
import Project._6.demo.service.NotificationService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/owner")
public class OwnerController {

    private final AnalyticsReportService analyticsReportService;
    private final AnalyticsReportRepository analyticsReportRepository;
    private final ConcernRepository concernRepository;
    private final AdminRepository adminRepository;
    private final FeedbackRepository feedbackRepository;
    private final AdminReplyRepository adminReplyRepository;
    private final NotificationService notificationService;

    public OwnerController(AnalyticsReportService analyticsReportService,
                           AnalyticsReportRepository analyticsReportRepository,
                           ConcernRepository concernRepository,
                           AdminRepository adminRepository,
                           FeedbackRepository feedbackRepository,
                           AdminReplyRepository adminReplyRepository,
                           NotificationService notificationService) {
        this.analyticsReportService = analyticsReportService;
        this.analyticsReportRepository = analyticsReportRepository;
        this.concernRepository = concernRepository;
        this.adminRepository = adminRepository;
        this.feedbackRepository = feedbackRepository;
        this.adminReplyRepository = adminReplyRepository;
        this.notificationService = notificationService;
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

        List<Feedback> feedbacks = feedbackRepository.findByConcern_Admin_UserId(adminId);
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
            List<Feedback> feedbacks = feedbackRepository.findByConcern_Admin_UserId(report.getAdminIdFk());
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
}
