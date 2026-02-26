package Project._6.demo.controller;

import Project._6.demo.dto.AdminReplyDTO;
import Project._6.demo.entity.AdminReply;
import Project._6.demo.entity.Concern;
import Project._6.demo.service.AdminService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String showDashboard(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "timePeriod", required = false) String timePeriod,
            @RequestParam(value = "category", required = false) String category,
            Model model) {

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

        model.addAttribute("concerns", concerns);
        model.addAttribute("selectedStatus", status != null ? status : "All");
        model.addAttribute("selectedTimePeriod", timePeriod != null ? timePeriod : "All");
        model.addAttribute("selectedCategory", category != null ? category : "All");
        model.addAttribute("totalConcerns", adminService.getTotalConcerns());
        model.addAttribute("pendingCount", adminService.getPendingCount());
        model.addAttribute("inProgressCount", adminService.getInProgressCount());
        model.addAttribute("completeCount", adminService.getCompleteCount());

        return "admin-dashboard";
    }

    /**
     * View a single concern with its replies
     */
    @GetMapping("/concern/{id}")
    public String viewConcern(@PathVariable("id") Integer id, Model model) {
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
                              RedirectAttributes redirectAttributes) {
        try {
            replyDTO.setConcernId(id);
            adminService.submitReply(replyDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Reply submitted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to submit reply: " + e.getMessage());
        }
        return "redirect:/admin/concern/" + id;
    }

    /**
     * Update concern status
     */
    @PostMapping("/concern/{id}/status")
    public String updateStatus(@PathVariable("id") Integer id,
                               @RequestParam("status") String status,
                               RedirectAttributes redirectAttributes) {
        try {
            adminService.updateConcernStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Status updated to: " + status);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update status: " + e.getMessage());
        }
        return "redirect:/admin/concern/" + id;
    }
}
