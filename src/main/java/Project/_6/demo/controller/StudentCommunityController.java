package Project._6.demo.controller;

import Project._6.demo.dto.*;
import Project._6.demo.entity.Notification;
import Project._6.demo.entity.StudentCommunityPost;
import Project._6.demo.entity.StudentCommunityReply;
import Project._6.demo.service.NotificationService;
import Project._6.demo.service.StudentCommunityService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/student/community")
public class StudentCommunityController {

    private final StudentCommunityService communityService;
    private final NotificationService notificationService;

    public StudentCommunityController(StudentCommunityService communityService,
                                      NotificationService notificationService) {
        this.communityService = communityService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public String showCommunity(HttpSession session, Model model) {
        Integer userId = getLoggedStudentId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        if (!communityService.hasAcceptedRules(userId)) {
            return "redirect:/student/community/rules";
        }

        List<StudentCommunityPost> posts = communityService.getActivePosts();
        Map<Integer, List<StudentCommunityReply>> repliesMap = communityService.getRepliesMap(posts);

        model.addAttribute("studentName", session.getAttribute("studentName"));
        model.addAttribute("posts", posts);
        model.addAttribute("repliesMap", repliesMap);
        model.addAttribute("categories", communityService.getAllowedCategories());
        model.addAttribute("postDTO", new CommunityPostDTO());
        addNotificationAttributes(model, userId);

        return "student-community";
    }

    @GetMapping("/rules")
    public String showRules(HttpSession session, Model model) {
        Integer userId = getLoggedStudentId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        if (communityService.hasAcceptedRules(userId)) {
            return "redirect:/student/community";
        }

        model.addAttribute("studentName", session.getAttribute("studentName"));
        model.addAttribute("rulesVersion", StudentCommunityService.RULES_VERSION);
        addNotificationAttributes(model, userId);
        return "student-community-rules";
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

    @PostMapping("/rules/accept")
    public String acceptRules(HttpSession session, RedirectAttributes redirectAttributes) {
        Integer userId = getLoggedStudentId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        communityService.acceptRules(userId);
        redirectAttributes.addFlashAttribute("successMessage", "Rules accepted. Welcome to Community Talk.");
        return "redirect:/student/community";
    }

    @PostMapping("/posts")
    public String createPost(@ModelAttribute CommunityPostDTO dto,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Integer userId = getLoggedStudentId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            communityService.createPost(userId, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Your post is now live.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/student/community";
    }

    @PostMapping("/posts/{postId}/update")
    public String updatePost(@PathVariable Integer postId,
                             @ModelAttribute CommunityPostDTO dto,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Integer userId = getLoggedStudentId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            communityService.updatePost(postId, userId, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Post updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/student/community";
    }

    @PostMapping("/posts/{postId}/delete")
    public String deletePost(@PathVariable Integer postId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Integer userId = getLoggedStudentId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            communityService.deletePost(postId, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Post deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/student/community";
    }

    @PostMapping("/posts/{postId}/replies")
    public String createReply(@PathVariable Integer postId,
                              @ModelAttribute CommunityReplyDTO dto,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Integer userId = getLoggedStudentId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            communityService.createReply(postId, userId, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Reply posted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/student/community";
    }

    @PostMapping("/replies/{replyId}/update")
    public String updateReply(@PathVariable Integer replyId,
                              @ModelAttribute CommunityReplyDTO dto,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Integer userId = getLoggedStudentId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            communityService.updateReply(replyId, userId, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Reply updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/student/community";
    }

    @PostMapping("/replies/{replyId}/delete")
    public String deleteReply(@PathVariable Integer replyId,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Integer userId = getLoggedStudentId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            communityService.deleteReply(replyId, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Reply deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/student/community";
    }

    @PostMapping("/moderate")
    @ResponseBody
    public ResponseEntity<CommunityModerationResultDTO> moderateMessage(@RequestBody CommunityModerationRequestDTO dto,
                                                                         HttpSession session) {
        Integer userId = getLoggedStudentId(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommunityModerationResultDTO("BLOCK", "Please log in again.", 100));
        }

        CommunityModerationResultDTO result = communityService.runLiveModeration(
                userId,
                dto.getMessage(),
                dto.getContentType()
        );

        return ResponseEntity.ok(result);
    }

    private Integer getLoggedStudentId(HttpSession session) {
        return (Integer) session.getAttribute("studentUserId");
    }
}
