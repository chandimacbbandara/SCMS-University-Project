package Project._6.demo.controller;

import Project._6.demo.dto.ConcernSubmissionDTO;
import Project._6.demo.dto.FeedbackDTO;
import Project._6.demo.entity.AdminReply;
import Project._6.demo.entity.Concern;
import Project._6.demo.entity.Feedback;
import Project._6.demo.service.ConcernService;
import Project._6.demo.service.FeedbackService;

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

    public ConcernController(ConcernService concernService, FeedbackService feedbackService) {
        this.concernService = concernService;
        this.feedbackService = feedbackService;
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
        model.addAttribute("concerns", concerns);
        model.addAttribute("repliesMap", repliesMap);
        model.addAttribute("feedbackMap", feedbackMap);
        return "concern-history";
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
            feedbackService.submitFeedback(feedbackDTO);
            redirectAttributes.addFlashAttribute("feedbackSuccess", "Thank you for your feedback!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("feedbackError", e.getMessage());
        }
        return "redirect:/student/concern-history";
    }
}

