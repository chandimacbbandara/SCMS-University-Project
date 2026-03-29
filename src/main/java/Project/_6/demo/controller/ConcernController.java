package Project._6.demo.controller;

import Project._6.demo.dto.ConcernSubmissionDTO;
import Project._6.demo.dto.FeedbackDTO;
import Project._6.demo.entity.AdminReply;
import Project._6.demo.entity.Concern;
import Project._6.demo.entity.Feedback;
import Project._6.demo.entity.Notification;
import Project._6.demo.service.ConcernService;
import Project._6.demo.service.FeedbackService;
import Project._6.demo.service.NotificationService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Controller
public class ConcernController {

    private final ConcernService concernService;
    private final FeedbackService feedbackService;
    private final NotificationService notificationService;

    public ConcernController(ConcernService concernService,
                             FeedbackService feedbackService,
                             NotificationService notificationService) {
        this.concernService = concernService;
        this.feedbackService = feedbackService;
        this.notificationService = notificationService;
    }

    // Serve the concern submission form
    @GetMapping("/submit-concern")
    public String showConcernForm(HttpSession session, Model model) {
        if (session.getAttribute("loggedInStudent") == null) {
            return "redirect:/login";
        }
        Integer userId = (Integer) session.getAttribute("studentUserId");
        var student = concernService.getStudentByUserId(userId);
        model.addAttribute("concernDTO", new ConcernSubmissionDTO());
        model.addAttribute("loggedStudent", student);
        addNotificationAttributes(model, userId);
        return "submit-concern";
    }

    // Handle concern form submission
    @PostMapping("/submit-concern")
    public String submitConcern(
            @ModelAttribute ConcernSubmissionDTO concernDTO,
            @RequestParam(value = "evidence", required = false) MultipartFile evidence,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (session.getAttribute("loggedInStudent") == null) {
            return "redirect:/login";
        }

        Integer userId = (Integer) session.getAttribute("studentUserId");
        if (userId == null) {
            return "redirect:/login";
        }

        var student = concernService.getStudentByUserId(userId);
        concernDTO.setStudentId(student.getStudentId());
        concernDTO.setFirstName(student.getUser().getFirstName());
        concernDTO.setLastName(student.getUser().getLastName());
        concernDTO.setEmail(student.getUser().getEmail());

        if (concernDTO.getSubject() == null || concernDTO.getSubject().trim().isEmpty()
                || concernDTO.getMessage() == null || concernDTO.getMessage().trim().isEmpty()
                || concernDTO.getCategory() == null || concernDTO.getCategory().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please fill in Subject, Category, and Message.");
            return "redirect:/submit-concern";
        }

        try {
            Concern saved = concernService.submitConcern(concernDTO, evidence);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Your concern has been submitted successfully! Reference ID: CON-" + saved.getConcernId());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to submit concern: " + e.getMessage());
            return "redirect:/submit-concern";
        }

        return "redirect:/student/concern-history";
    }

    // Concern History page
    @GetMapping("/student/concern-history")
    public String showConcernHistory(HttpSession session, Model model) {
        if (session.getAttribute("loggedInStudent") == null) {
            return "redirect:/login";
        }
        Integer userId = (Integer) session.getAttribute("studentUserId");
        List<Concern> concerns = concernService.getConcernsByStudentUserId(userId);
        Map<Integer, List<AdminReply>> repliesMap = concernService.getRepliesMap(concerns);
        Map<Integer, Feedback> feedbackMap = feedbackService.getFeedbackMap(concerns);
        Map<Integer, Boolean> feedbackActionAllowedMap = feedbackService.getFeedbackActionAllowedMap(concerns);
        Map<Integer, Boolean> feedbackUpdateAllowedMap = feedbackService.getFeedbackUpdateAllowedMap(concerns);
        model.addAttribute("concerns", concerns);
        model.addAttribute("repliesMap", repliesMap);
        model.addAttribute("feedbackMap", feedbackMap);
        model.addAttribute("feedbackActionAllowedMap", feedbackActionAllowedMap);
        model.addAttribute("feedbackUpdateAllowedMap", feedbackUpdateAllowedMap);
        addNotificationAttributes(model, userId);
        return "concern-history";
    }

    private void addNotificationAttributes(Model model, Integer userId) {
        List<Notification> personalNotifications = notificationService.getNotificationsForStudent(userId);
        List<Notification> broadcastNotifications = notificationService.getAllBroadcastNotifications();
        List<Notification> allNotifications = new java.util.ArrayList<>(personalNotifications);
        allNotifications.addAll(broadcastNotifications);
        allNotifications.sort((a, b) -> b.getSentTime().compareTo(a.getSentTime()));

        long unreadCount = notificationService.getUnreadCount(userId)
                + broadcastNotifications.stream().filter(n -> !Boolean.TRUE.equals(n.getIsRead())).count();

        model.addAttribute("notifications", allNotifications);
        model.addAttribute("unreadCount", unreadCount);
    }

    // Submit feedback for a completed concern
    @PostMapping("/student/feedback")
    public String submitFeedback(
            @ModelAttribute FeedbackDTO feedbackDTO,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (session.getAttribute("loggedInStudent") == null) {
            return "redirect:/login";
        }
        try {
            Integer studentUserId = (Integer) session.getAttribute("studentUserId");
            feedbackService.submitFeedback(feedbackDTO, studentUserId);
            redirectAttributes.addFlashAttribute("feedbackSuccess", "Thank you for your feedback!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("feedbackError", e.getMessage());
        }
        return "redirect:/student/concern-history";
    }

    @PostMapping("/student/feedback/update")
    public String updateFeedback(@ModelAttribute FeedbackDTO feedbackDTO,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (session.getAttribute("loggedInStudent") == null) {
            return "redirect:/login";
        }

        Integer studentUserId = (Integer) session.getAttribute("studentUserId");
        try {
            feedbackService.updateFeedback(feedbackDTO.getConcernId(), studentUserId, feedbackDTO);
            redirectAttributes.addFlashAttribute("feedbackSuccess", "Your feedback has been updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("feedbackError", e.getMessage());
        }

        return "redirect:/student/concern-history";
    }

    @PostMapping("/student/feedback/delete")
    public String deleteFeedback(@RequestParam("concernId") Integer concernId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (session.getAttribute("loggedInStudent") == null) {
            return "redirect:/login";
        }

        Integer studentUserId = (Integer) session.getAttribute("studentUserId");
        try {
            feedbackService.deleteFeedback(concernId, studentUserId);
            redirectAttributes.addFlashAttribute("feedbackSuccess", "Your feedback has been deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("feedbackError", e.getMessage());
        }

        return "redirect:/student/concern-history";
    }
}

