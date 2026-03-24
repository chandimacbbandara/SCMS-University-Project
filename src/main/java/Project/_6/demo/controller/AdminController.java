package Project._6.demo.controller;

import Project._6.demo.dto.AdminReplyDTO;
import Project._6.demo.entity.AdminReply;
import Project._6.demo.entity.Concern;
import Project._6.demo.service.AdminService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Admin Dashboard - shows all concerns with stats and combined filters
     */
    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "timePeriod", required = false) String timePeriod,
            @RequestParam(value = "category", required = false) String category,
            Model model) {

        if (!isAdminLoggedIn(session)) {
            return "redirect:/login";
        }

        // Resolve time period to date range
        LocalDateTime from = null;
        LocalDateTime to = LocalDateTime.now();

        if (timePeriod != null && !timePeriod.isEmpty() && !timePeriod.equals("All")) {
            switch (timePeriod) {
                case "Today":
                    from = LocalDate.now().atStartOfDay();
                    break;
                case "Last7Days":
                    from = LocalDate.now().minusDays(7).atStartOfDay();
                    break;
                case "Last30Days":
                    from = LocalDate.now().minusDays(30).atStartOfDay();
                    break;
                case "Older":
                    to = LocalDate.now().minusDays(30).atStartOfDay();
                    from = LocalDate.of(2000, 1, 1).atStartOfDay();
                    break;
                default:
                    from = null;
                    to = null;
            }
        } else {
            to = null;
        }

        List<Concern> concerns = adminService.getFilteredConcerns(status, category, from, to);
        List<Concern> allConcerns = adminService.getAllConcerns();

        long instituteCount = allConcerns.stream().filter(c -> "Institute Problem".equals(c.getCategory())).count();
        long registrationCount = allConcerns.stream().filter(c -> "Registration".equals(c.getCategory())).count();
        long administrativeCount = allConcerns.stream().filter(c -> "Administrative".equals(c.getCategory())).count();
        long financialCount = allConcerns.stream().filter(c -> "Financial".equals(c.getCategory())).count();
        long otherCount = allConcerns.stream().filter(c -> "Other".equals(c.getCategory())).count();

        model.addAttribute("concerns", concerns);
        model.addAttribute("selectedStatus", status != null ? status : "All");
        model.addAttribute("selectedTimePeriod", timePeriod != null ? timePeriod : "All");
        model.addAttribute("selectedCategory", category != null ? category : "All");
        model.addAttribute("instituteCount", instituteCount);
        model.addAttribute("registrationCount", registrationCount);
        model.addAttribute("administrativeCount", administrativeCount);
        model.addAttribute("financialCount", financialCount);
        model.addAttribute("otherCount", otherCount);
        model.addAttribute("totalConcerns", adminService.getTotalConcerns());
        model.addAttribute("pendingCount", adminService.getPendingCount());
        model.addAttribute("inProgressCount", adminService.getInProgressCount());
        model.addAttribute("completeCount", adminService.getCompleteCount());

        return "admin-dashboard";
    }

    @GetMapping("/edu-dashboard")
    public String showEduDashboard(HttpSession session, Model model) {
        if (!isAdminLoggedIn(session)) {
            return "redirect:/login";
        }

        // Filter: only education category
        List<Concern> eduConcerns = adminService.getFilteredConcerns("All", "Education (Creative and IT)", null, null);

        model.addAttribute("concerns", eduConcerns);
        model.addAttribute("totalConcerns", eduConcerns.size());
        model.addAttribute("pendingCount", eduConcerns.stream().filter(c -> "Pending".equals(c.getStatus())).count());
        model.addAttribute("inProgressCount", eduConcerns.stream().filter(c -> "In Progress".equals(c.getStatus())).count());
        model.addAttribute("completeCount", eduConcerns.stream().filter(c -> "Complete".equals(c.getStatus())).count());

        return "admin-edu-dashboard";
    }

    /**
     * View a single concern with its replies
     */
    @GetMapping("/concern/{id}")
    public String viewConcern(@PathVariable("id") Integer id, HttpSession session, Model model) {
        if (!isAdminLoggedIn(session)) {
            return "redirect:/login";
        }
        Concern concern = adminService.getConcernById(id);
        List<AdminReply> replies = adminService.getRepliesForConcern(id);

        model.addAttribute("concern", concern);
        model.addAttribute("replies", replies);
        model.addAttribute("replyDTO", new AdminReplyDTO());

        return "admin-concern-detail";
    }

    /**
     * Submit a reply to a concern
     */
    @PostMapping("/concern/{id}/reply")
    public String submitReply(@PathVariable("id") Integer id,
                              @ModelAttribute AdminReplyDTO replyDTO,
                              @RequestParam(value = "resolutionFile", required = false) MultipartFile resolutionFile,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (!isAdminLoggedIn(session)) {
            return "redirect:/login";
        }
        try {
            replyDTO.setConcernId(id);
            adminService.submitReply(replyDTO, resolutionFile);
            redirectAttributes.addFlashAttribute("successMessage", "Reply submitted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to submit reply: " + e.getMessage());
        }
        return "redirect:/admin/concern/" + id;
    }

    /**
     * Delete a reply from a concern
     */
    @PostMapping("/concern/{concernId}/reply/{replyId}/delete")
    public String deleteReply(@PathVariable("concernId") Integer concernId,
                              @PathVariable("replyId") Integer replyId,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (!isAdminLoggedIn(session)) {
            return "redirect:/login";
        }
        try {
            adminService.deleteReply(concernId, replyId);
            redirectAttributes.addFlashAttribute("successMessage", "Reply deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete reply: " + e.getMessage());
        }
        return "redirect:/admin/concern/" + concernId;
    }

    /**
     * Update the latest reply from a concern
     */
    @PostMapping("/concern/{concernId}/reply/{replyId}/update")
    public String updateReply(@PathVariable("concernId") Integer concernId,
                              @PathVariable("replyId") Integer replyId,
                              @RequestParam("replyMessage") String replyMessage,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (!isAdminLoggedIn(session)) {
            return "redirect:/login";
        }
        try {
            adminService.updateLatestReply(concernId, replyId, replyMessage);
            redirectAttributes.addFlashAttribute("successMessage", "Reply updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update reply: " + e.getMessage());
        }
        return "redirect:/admin/concern/" + concernId;
    }

    /**
     * Delete a concern from dashboard list
     */
    @PostMapping("/concern/{id}/delete")
    public String deleteConcern(@PathVariable("id") Integer concernId,
                                @RequestParam(value = "redirectTo", defaultValue = "dashboard") String redirectTo,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (!isAdminLoggedIn(session)) {
            return "redirect:/login";
        }
        try {
            adminService.deleteConcern(concernId);
            redirectAttributes.addFlashAttribute("successMessage", "Concern deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete concern: " + e.getMessage());
        }
        if ("edu-dashboard".equals(redirectTo)) {
            return "redirect:/admin/edu-dashboard";
        }
        return "redirect:/admin/dashboard";
    }

    /**
     * Update concern status
     */
    @PostMapping("/concern/{id}/status")
    public String updateStatus(@PathVariable("id") Integer id,
                               @RequestParam("status") String status,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        if (!isAdminLoggedIn(session)) {
            return "redirect:/login";
        }
        try {
            adminService.updateConcernStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Status updated to: " + status);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update status: " + e.getMessage());
        }
        return "redirect:/admin/concern/" + id;
    }

    /**
     * Reassign concern category/department
     */
    @PostMapping("/concern/{id}/category")
    public String updateCategory(@PathVariable("id") Integer id,
                                 @RequestParam("category") String category,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (!isAdminLoggedIn(session)) {
            return "redirect:/login";
        }
        try {
            adminService.updateConcernCategory(id, category);
            redirectAttributes.addFlashAttribute("successMessage", "Concern department updated to: " + category);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update department: " + e.getMessage());
        }
        return "redirect:/admin/concern/" + id;
    }

    /**
     * Check if admin is logged in
     */
    private boolean isAdminLoggedIn(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("adminLoggedIn"));
    }
}
