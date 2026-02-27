package Project._6.demo.controller;

import Project._6.demo.dto.ConcernSubmissionDTO;
import Project._6.demo.entity.Concern;
import Project._6.demo.service.ConcernService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

@Controller
public class ConcernController {

    private final ConcernService concernService;

    public ConcernController(ConcernService concernService) {
        this.concernService = concernService;
    }

    // Serve the concern submission form
    @GetMapping("/submit-concern")
    public String showConcernForm(HttpSession session, Model model) {
        if (session.getAttribute("loggedInStudent") == null) {
            return "redirect:/login";
        }
        model.addAttribute("concernDTO", new ConcernSubmissionDTO());
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
        }

        return "redirect:/submit-concern";
    }
}
