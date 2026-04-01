package Project._6.demo.controller;

import Project._6.demo.dto.ConcernSubmissionDTO;
import Project._6.demo.dto.FeedbackDTO;
import Project._6.demo.entity.AdminReply;
import Project._6.demo.entity.Concern;
import Project._6.demo.entity.ConcernMeetingProposal;
import Project._6.demo.entity.ConcernMeetingSlot;
import Project._6.demo.entity.Feedback;
import Project._6.demo.entity.Notification;
import Project._6.demo.service.ConcernMeetingService;
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
    private final ConcernMeetingService concernMeetingService;
    private final FeedbackService feedbackService;
    private final NotificationService notificationService;

    public ConcernController(ConcernService concernService,
                             ConcernMeetingService concernMeetingService,
                             FeedbackService feedbackService,
                             NotificationService notificationService) {
        this.concernService = concernService;
        this.concernMeetingService = concernMeetingService;
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
        model.addAttribute("isEdit", false);
        model.addAttribute("isDraftConcern", false);
        model.addAttribute("draftEnabled", true);
        addNotificationAttributes(model, userId);
        return "submit-concern";
    }

    @GetMapping("/student/concern/{id}/edit")
    public String showConcernEditForm(@PathVariable("id") Integer concernId,
                                      HttpSession session,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        if (session.getAttribute("loggedInStudent") == null) {
            return "redirect:/login";
        }

        Integer userId = (Integer) session.getAttribute("studentUserId");
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            var student = concernService.getStudentByUserId(userId);
            Concern concern = concernService.getEditableConcernForStudent(concernId, userId);
            boolean isDraftConcern = concern.getStatus() != null
                    && "Draft".equalsIgnoreCase(concern.getStatus().trim());

            ConcernSubmissionDTO concernDTO = new ConcernSubmissionDTO();
            concernDTO.setStudentId(student.getStudentId());
            concernDTO.setFirstName(student.getUser().getFirstName());
            concernDTO.setLastName(student.getUser().getLastName());
            concernDTO.setEmail(student.getUser().getEmail());
            concernDTO.setCategory(concern.getCategory());
            concernDTO.setSubject(concern.getSubject());
            concernDTO.setMessage(concern.getMessage());

            model.addAttribute("concernDTO", concernDTO);
            model.addAttribute("loggedStudent", student);
            model.addAttribute("isEdit", true);
            model.addAttribute("isDraftConcern", isDraftConcern);
            model.addAttribute("draftEnabled", isDraftConcern);
            model.addAttribute("editingConcernId", concern.getConcernId());
            model.addAttribute("existingEvidencePath", concern.getEvidencePath());
            addNotificationAttributes(model, userId);
            return "submit-concern";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/student/concern-history";
        }
    }

    // Handle concern form submission
    @PostMapping("/submit-concern")
    public String submitConcern(
            @ModelAttribute ConcernSubmissionDTO concernDTO,
            @RequestParam(value = "action", defaultValue = "submit") String action,
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

        boolean saveAsDraft = "draft".equalsIgnoreCase(action);

        try {
            Concern saved = saveAsDraft
                    ? concernService.saveConcernDraft(concernDTO, evidence)
                    : concernService.submitConcern(concernDTO, evidence);

            if (saveAsDraft) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Concern draft saved successfully. You can continue and submit it later.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Your concern has been submitted successfully! Reference ID: CON-" + saved.getConcernId());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    (saveAsDraft ? "Failed to save draft: " : "Failed to submit concern: ") + e.getMessage());
            return "redirect:/submit-concern";
        }

        return saveAsDraft ? "redirect:/student/concern-drafts" : "redirect:/student/concern-history";
    }

    @PostMapping("/student/concern/update")
    public String updateConcern(
            @RequestParam("concernId") Integer concernId,
            @ModelAttribute ConcernSubmissionDTO concernDTO,
            @RequestParam(value = "action", defaultValue = "submit") String action,
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

        boolean saveAsDraft = "draft".equalsIgnoreCase(action);
        boolean wasDraft = false;
        try {
            Concern editableConcern = concernService.getEditableConcernForStudent(concernId, userId);
            wasDraft = editableConcern.getStatus() != null
                    && "Draft".equalsIgnoreCase(editableConcern.getStatus().trim());

            concernService.updateConcernByStudent(concernId, userId, concernDTO, evidence, saveAsDraft);

            if (saveAsDraft) {
                redirectAttributes.addFlashAttribute("successMessage", "Draft updated successfully.");
                return "redirect:/student/concern-drafts";
            }

            if (wasDraft) {
                redirectAttributes.addFlashAttribute("successMessage", "Draft submitted successfully.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Concern updated successfully.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            if (saveAsDraft || wasDraft) {
                return "redirect:/student/concern-drafts";
            }
        }

        return "redirect:/student/concern-history";
    }

    @GetMapping("/student/concern-drafts")
    public String showConcernDrafts(HttpSession session, Model model) {
        if (session.getAttribute("loggedInStudent") == null) {
            return "redirect:/login";
        }

        Integer userId = (Integer) session.getAttribute("studentUserId");
        if (userId == null) {
            return "redirect:/login";
        }

        List<Concern> draftConcerns = concernService.getDraftConcernsByStudentUserId(userId);
        model.addAttribute("draftConcerns", draftConcerns);
        addNotificationAttributes(model, userId);
        return "student-concern-drafts";
    }

    @PostMapping("/student/concern/draft/submit")
    public String submitDraftConcern(@RequestParam("concernId") Integer concernId,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        if (session.getAttribute("loggedInStudent") == null) {
            return "redirect:/login";
        }

        Integer studentUserId = (Integer) session.getAttribute("studentUserId");
        if (studentUserId == null) {
            return "redirect:/login";
        }

        try {
            Concern submittedConcern = concernService.submitDraftByStudent(concernId, studentUserId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Draft submitted successfully! Reference ID: CON-" + submittedConcern.getConcernId());
            return "redirect:/student/concern-history";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/student/concern-drafts";
        }
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
        Map<Integer, ConcernMeetingProposal> latestMeetingProposalMap = concernMeetingService.getLatestProposalMap(concerns);
        List<Integer> latestProposalIds = latestMeetingProposalMap.values().stream()
                .map(ConcernMeetingProposal::getProposalId)
                .toList();
        Map<Integer, List<ConcernMeetingSlot>> meetingSlotsMapByProposalId = concernMeetingService
                .getSlotsMapByProposalIds(latestProposalIds);

        model.addAttribute("concerns", concerns);
        model.addAttribute("repliesMap", repliesMap);
        model.addAttribute("feedbackMap", feedbackMap);
        model.addAttribute("feedbackActionAllowedMap", feedbackActionAllowedMap);
        model.addAttribute("feedbackUpdateAllowedMap", feedbackUpdateAllowedMap);
        model.addAttribute("latestMeetingProposalMap", latestMeetingProposalMap);
        model.addAttribute("meetingSlotsMapByProposalId", meetingSlotsMapByProposalId);
        addNotificationAttributes(model, userId);
        return "concern-history";
    }

    @PostMapping("/student/concern/{id}/meeting/book")
    public String bookMeetingSlot(@PathVariable("id") Integer concernId,
                                  @RequestParam("proposalId") Integer proposalId,
                                  @RequestParam("slotId") Integer slotId,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        if (session.getAttribute("loggedInStudent") == null) {
            return "redirect:/login";
        }

        Integer studentUserId = (Integer) session.getAttribute("studentUserId");
        if (studentUserId == null) {
            return "redirect:/login";
        }

        try {
            concernMeetingService.bookMeetingSlot(concernId, proposalId, slotId, studentUserId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Meeting slot booked successfully. Your concern status is now Meeting Scheduled.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/student/concern-history";
    }

    @PostMapping("/student/concern/{id}/meeting/decline")
    public String declineMeetingSlotOptions(@PathVariable("id") Integer concernId,
                                            @RequestParam("proposalId") Integer proposalId,
                                            @RequestParam(value = "reason", required = false) String reason,
                                            HttpSession session,
                                            RedirectAttributes redirectAttributes) {
        if (session.getAttribute("loggedInStudent") == null) {
            return "redirect:/login";
        }

        Integer studentUserId = (Integer) session.getAttribute("studentUserId");
        if (studentUserId == null) {
            return "redirect:/login";
        }

        try {
            concernMeetingService.declineMeetingSlots(concernId, proposalId, studentUserId, reason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "You requested new meeting slots. Concern status moved back to Pending until admin shares another schedule.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/student/concern-history";
    }

    private void addNotificationAttributes(Model model, Integer userId) {
        List<Notification> personalNotifications = notificationService.getNotificationsForStudent(userId);
        List<Notification> broadcastNotifications = notificationService.getAllBroadcastNotifications();
        List<Notification> allNotifications = new java.util.ArrayList<>(personalNotifications);
        allNotifications.addAll(broadcastNotifications);
        allNotifications.sort((a, b) -> b.getSentTime().compareTo(a.getSentTime()));

        long unreadCount = notificationService.getUnreadCount(userId);

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

    @PostMapping("/student/concern/delete")
    public String deleteStudentConcern(@RequestParam("concernId") Integer concernId,
                                       @RequestParam(value = "returnTo", defaultValue = "history") String returnTo,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        if (session.getAttribute("loggedInStudent") == null) {
            return "redirect:/login";
        }

        Integer studentUserId = (Integer) session.getAttribute("studentUserId");
        String redirectUrl = "drafts".equalsIgnoreCase(returnTo)
                ? "redirect:/student/concern-drafts"
                : "redirect:/student/concern-history";

        try {
            concernService.deleteConcernByStudentIfPending(concernId, studentUserId);
            redirectAttributes.addFlashAttribute("successMessage", "Concern deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return redirectUrl;
    }
}

