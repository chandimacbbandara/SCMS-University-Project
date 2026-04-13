package Project._6.demo.controller;

import Project._6.demo.dto.AdminReplyDTO;
import Project._6.demo.dto.CommunityModerationRequestDTO;
import Project._6.demo.dto.CommunityModerationResultDTO;
import Project._6.demo.entity.AdminReply;
import Project._6.demo.entity.Concern;
import Project._6.demo.entity.ConcernMeetingProposal;
import Project._6.demo.entity.ConcernMeetingSlot;
import Project._6.demo.entity.Feedback;
import Project._6.demo.service.AdminService;
import Project._6.demo.service.ConcernMeetingService;
import Project._6.demo.service.CommunityModerationService;
import Project._6.demo.service.FeedbackService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final ConcernMeetingService concernMeetingService;
    private final FeedbackService feedbackService;
    private final Project._6.demo.service.StudentCommunityService communityService;
    private final CommunityModerationService moderationService;

    public AdminController(AdminService adminService,
                           ConcernMeetingService concernMeetingService,
                           FeedbackService feedbackService,
                           Project._6.demo.service.StudentCommunityService communityService,
                           CommunityModerationService moderationService) {
        this.adminService = adminService;
        this.concernMeetingService = concernMeetingService;
        this.feedbackService = feedbackService;
        this.communityService = communityService;
        this.moderationService = moderationService;
    }

    /**
     * Admin Dashboard - shows all concerns with stats and combined filters
     */
    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "timePeriod", required = false) String timePeriod,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "prioritySort", required = false) String prioritySort,
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

        String selectedPriority = normalizePriorityFilter(priority);
        String selectedPrioritySort = normalizePrioritySort(prioritySort);

        List<Concern> concerns = adminService.getFilteredConcerns(status, category, from, to).stream()
            .filter(concern -> isAllPriorities(selectedPriority)
                || selectedPriority.equalsIgnoreCase(defaultPriority(concern.getAiPriorityLevel())))
            .toList();
        concerns = applyPrioritySort(concerns, selectedPrioritySort);
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
        model.addAttribute("selectedPriority", selectedPriority);
        model.addAttribute("selectedPrioritySort", selectedPrioritySort);
        model.addAttribute("instituteCount", instituteCount);
        model.addAttribute("registrationCount", registrationCount);
        model.addAttribute("administrativeCount", administrativeCount);
        model.addAttribute("financialCount", financialCount);
        model.addAttribute("otherCount", otherCount);
        model.addAttribute("totalConcerns", adminService.getTotalConcerns());
        model.addAttribute("pendingCount", adminService.getPendingCount());
        model.addAttribute("inProgressCount", adminService.getInProgressCount());
        model.addAttribute("completeCount", adminService.getCompleteCount());

        String adminEmail = (String) session.getAttribute("adminEmail");
        List<Feedback> adminFeedbacks = adminService.getAdminByEmail(adminEmail)
            .map(admin -> adminService.getFeedbackHistoryByAdminUserId(admin.getUserId()))
            .orElseGet(List::of);

        long totalFeedback = adminFeedbacks.size();
        long star5 = adminFeedbacks.stream().filter(f -> f.getRating() != null && f.getRating() == 5).count();
        long star4 = adminFeedbacks.stream().filter(f -> f.getRating() != null && f.getRating() == 4).count();
        long star3 = adminFeedbacks.stream().filter(f -> f.getRating() != null && f.getRating() == 3).count();
        long star2 = adminFeedbacks.stream().filter(f -> f.getRating() != null && f.getRating() == 2).count();
        long star1 = adminFeedbacks.stream().filter(f -> f.getRating() != null && f.getRating() == 1).count();

        double averageRating = totalFeedback == 0
            ? 0.0
            : adminFeedbacks.stream()
            .filter(f -> f.getRating() != null)
            .mapToInt(Feedback::getRating)
            .average()
            .orElse(0.0);

        model.addAttribute("adminFeedbackCount", totalFeedback);
        model.addAttribute("adminAverageRating", String.format(Locale.US, "%.1f", averageRating));
        model.addAttribute("adminStar5Count", star5);
        model.addAttribute("adminStar4Count", star4);
        model.addAttribute("adminStar3Count", star3);
        model.addAttribute("adminStar2Count", star2);
        model.addAttribute("adminStar1Count", star1);
        model.addAttribute("adminStar5Percent", totalFeedback == 0 ? 0.0 : (star5 * 100.0) / totalFeedback);
        model.addAttribute("adminStar4Percent", totalFeedback == 0 ? 0.0 : (star4 * 100.0) / totalFeedback);
        model.addAttribute("adminStar3Percent", totalFeedback == 0 ? 0.0 : (star3 * 100.0) / totalFeedback);
        model.addAttribute("adminStar2Percent", totalFeedback == 0 ? 0.0 : (star2 * 100.0) / totalFeedback);
        model.addAttribute("adminStar1Percent", totalFeedback == 0 ? 0.0 : (star1 * 100.0) / totalFeedback);

        return "admin-dashboard";
    }

    @GetMapping("/edu-dashboard")
    public String showEduDashboard(HttpSession session,
                                   @RequestParam(value = "status", required = false) String status,
                                   @RequestParam(value = "priority", required = false) String priority,
                                   @RequestParam(value = "prioritySort", required = false) String prioritySort,
                                   Model model) {
        if (!isAdminLoggedIn(session)) {
            return "redirect:/login";
        }

        String selectedStatus = normalizeStatusFilter(status);
        String selectedPriority = normalizePriorityFilter(priority);
        String selectedPrioritySort = normalizePrioritySort(prioritySort);

        // Base list for stats
        List<Concern> allEduConcerns = adminService.getFilteredConcerns("All", "Education (Creative and IT)", null, null);

        // Filtered list for table
        List<Concern> eduConcerns = adminService
                .getFilteredConcerns(selectedStatus, "Education (Creative and IT)", null, null)
                .stream()
                .filter(concern -> isAllPriorities(selectedPriority)
                        || selectedPriority.equalsIgnoreCase(defaultPriority(concern.getAiPriorityLevel())))
                .toList();
        eduConcerns = applyPrioritySort(eduConcerns, selectedPrioritySort);

        model.addAttribute("concerns", eduConcerns);
        model.addAttribute("totalConcerns", allEduConcerns.size());
        model.addAttribute("pendingCount", allEduConcerns.stream().filter(c -> "Pending".equals(c.getStatus())).count());
        model.addAttribute("inProgressCount", allEduConcerns.stream()
            .filter(c -> "In Progress".equals(c.getStatus()) || "Meeting Scheduled".equals(c.getStatus()))
            .count());
        model.addAttribute("completeCount", allEduConcerns.stream().filter(c -> "Complete".equals(c.getStatus())).count());
        model.addAttribute("selectedEduStatus", selectedStatus);
        model.addAttribute("selectedEduPriority", selectedPriority);
        model.addAttribute("selectedEduPrioritySort", selectedPrioritySort);

        return "admin-edu-dashboard";
    }

    @GetMapping("/feedback")
    public String showFeedbackDashboard(HttpSession session, Model model) {
        if (!isAdminLoggedIn(session)) {
            return "redirect:/login";
        }

        String adminEmail = (String) session.getAttribute("adminEmail");
        List<Feedback> adminFeedbackHistory = adminService.getAdminByEmail(adminEmail)
            .map(admin -> adminService.getFeedbackHistoryByAdminUserId(admin.getUserId()))
            .orElseGet(List::of);

        List<Feedback> feedbackHistory = adminService.getFeedbackHistory();
        List<Feedback> ratingSource = adminFeedbackHistory;

        long totalFeedback = ratingSource.size();
        long star5 = ratingSource.stream().filter(f -> f.getRating() != null && f.getRating() == 5).count();
        long star4 = ratingSource.stream().filter(f -> f.getRating() != null && f.getRating() == 4).count();
        long star3 = ratingSource.stream().filter(f -> f.getRating() != null && f.getRating() == 3).count();
        long star2 = ratingSource.stream().filter(f -> f.getRating() != null && f.getRating() == 2).count();
        long star1 = ratingSource.stream().filter(f -> f.getRating() != null && f.getRating() == 1).count();

        double averageRating = totalFeedback == 0
            ? 0.0
            : ratingSource.stream()
            .filter(f -> f.getRating() != null)
            .mapToInt(Feedback::getRating)
            .average()
            .orElse(0.0);

        Map<String, Double> departmentAverages = adminService.getDepartmentAverageRatings(feedbackHistory);
        Map<String, Integer[]> departmentStarCounts = adminService.getDepartmentStarCounts(feedbackHistory);

        List<String> deptRatingLabels = new ArrayList<>(departmentAverages.keySet());
        List<Double> deptRatingValues = deptRatingLabels.stream()
                .map(label -> departmentAverages.getOrDefault(label, 0.0))
                .toList();

        String topRatedDepartment = null;
        Double topRatedAverage = null;
        if (!departmentAverages.isEmpty()) {
            topRatedDepartment = departmentAverages.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

            topRatedAverage = departmentAverages.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(entry -> Math.round(entry.getValue() * 100.0) / 100.0)
                    .orElse(null);
        }

        model.addAttribute("feedbackHistory", feedbackHistory);
        model.addAttribute("departmentStarCounts", departmentStarCounts);
        model.addAttribute("deptRatingLabels", deptRatingLabels);
        model.addAttribute("deptRatingValues", deptRatingValues);
        model.addAttribute("topRatedDepartment", topRatedDepartment);
        model.addAttribute("topRatedAverage", topRatedAverage);
        model.addAttribute("adminFeedbackCount", totalFeedback);
        model.addAttribute("adminAverageRating", String.format(Locale.US, "%.1f", averageRating));
        model.addAttribute("adminStar5Count", star5);
        model.addAttribute("adminStar4Count", star4);
        model.addAttribute("adminStar3Count", star3);
        model.addAttribute("adminStar2Count", star2);
        model.addAttribute("adminStar1Count", star1);
        model.addAttribute("adminStar5Percent", totalFeedback == 0 ? 0.0 : (star5 * 100.0) / totalFeedback);
        model.addAttribute("adminStar4Percent", totalFeedback == 0 ? 0.0 : (star4 * 100.0) / totalFeedback);
        model.addAttribute("adminStar3Percent", totalFeedback == 0 ? 0.0 : (star3 * 100.0) / totalFeedback);
        model.addAttribute("adminStar2Percent", totalFeedback == 0 ? 0.0 : (star2 * 100.0) / totalFeedback);
        model.addAttribute("adminStar1Percent", totalFeedback == 0 ? 0.0 : (star1 * 100.0) / totalFeedback);

        return "admin-feedback";
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
        List<AdminReply> currentConcernReplies = adminService.getRepliesForConcern(id);
        List<AdminReply> replies = adminService.getConversationTimelineForConcern(id);
        List<Concern> linkedThread = adminService.getLinkedConcernChain(id);
        List<Concern> linkedConcerns = linkedThread.stream()
            .filter(threadConcern -> threadConcern != null && !id.equals(threadConcern.getConcernId()))
            .toList();

        Integer latestAdminReplyId = currentConcernReplies.stream()
                .filter(reply -> !isStudentMessage(reply))
                .reduce((first, second) -> second)
                .map(AdminReply::getReplyId)
                .orElse(null);
        Feedback feedback = feedbackService.getFeedbackByConcernId(id).orElse(null);
        List<ConcernMeetingProposal> meetingProposals = concernMeetingService.getProposalHistory(id);
        ConcernMeetingProposal latestMeetingProposal = meetingProposals.isEmpty() ? null : meetingProposals.get(0);
        List<Integer> proposalIds = meetingProposals.stream()
                .map(ConcernMeetingProposal::getProposalId)
                .toList();
        Map<Integer, List<ConcernMeetingSlot>> meetingSlotsMapByProposalId = concernMeetingService
                .getSlotsMapByProposalIds(proposalIds);

        model.addAttribute("concern", concern);
        model.addAttribute("replies", replies);
        model.addAttribute("linkedConcerns", linkedConcerns);
        model.addAttribute("latestAdminReplyId", latestAdminReplyId);
        model.addAttribute("feedback", feedback);
        model.addAttribute("replyDTO", new AdminReplyDTO());
        model.addAttribute("latestMeetingProposal", latestMeetingProposal);
        model.addAttribute("meetingProposals", meetingProposals);
        model.addAttribute("meetingSlotsMapByProposalId", meetingSlotsMapByProposalId);

        return "admin-concern-detail";
    }

    @PostMapping("/concern/{id}/meeting/propose")
    public String proposeMeetingSlots(@PathVariable("id") Integer concernId,
                                      @RequestParam("slotStarts") List<String> slotStarts,
                                      @RequestParam("slotEnds") List<String> slotEnds,
                                      @RequestParam(value = "adminNote", required = false) String adminNote,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        if (!isAdminLoggedIn(session)) {
            return "redirect:/login";
        }

        Concern concern = adminService.getConcernById(concernId);
        if (concern != null
                && ConcernMeetingService.STATUS_COMPLETE.equalsIgnoreCase(concern.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Cannot schedule a meeting for a completed concern.");
            return "redirect:/admin/concern/" + concernId;
        }

        Integer adminUserId = (Integer) session.getAttribute("adminUserId");
        try {
            concernMeetingService.proposeMeetingSlots(concernId, adminUserId, slotStarts, slotEnds, adminNote);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Physical meeting slots shared with the student successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to schedule meeting: " + e.getMessage());
        }

        return "redirect:/admin/concern/" + concernId;
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
            Integer adminUserId = (Integer) session.getAttribute("adminUserId");
            adminService.submitReply(replyDTO, resolutionFile, adminUserId);
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
     * Reject a concern from dashboard list (soft-delete from UI).
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
            redirectAttributes.addFlashAttribute("successMessage", "Concern rejected and removed from dashboard.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reject concern: " + e.getMessage());
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
     * View Student Community Chat
     */
    @GetMapping("/community")
    public String showCommunity(HttpSession session, Model model) {
        if (!isAdminLoggedIn(session)) {
            return "redirect:/login";
        }

        List<Project._6.demo.entity.StudentCommunityPost> posts = communityService.getActivePosts();
        Map<Integer, List<Project._6.demo.entity.StudentCommunityReply>> repliesMap = communityService.getRepliesMap(posts);

        model.addAttribute("adminName", session.getAttribute("adminEmail") != null ? "Admin" : "Admin");
        model.addAttribute("posts", posts);
        model.addAttribute("repliesMap", repliesMap);
        model.addAttribute("categories", communityService.getAllowedCategories());

        return "admin-community-chat";
    }

    /**
     * Moderator Delete Post
     */
    @PostMapping("/community/post/{id}/delete")
    public String deletePost(@PathVariable("id") Integer id,
                             @RequestParam(value = "reason", required = false, defaultValue = "Moderator deletion") String reason,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        if (!isAdminLoggedIn(session)) {
            return "redirect:/login";
        }
        
        Integer adminId = (Integer) session.getAttribute("adminId");
        if (adminId == null) {
            // fallback if ID not strictly stored, though it usually is
            adminId = 0;
        }

        try {
            communityService.deletePostAsModerator(id, adminId, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Post deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete post: " + e.getMessage());
        }
        return "redirect:/admin/community";
    }

    /**
     * Moderator Delete Reply
     */
    @PostMapping("/community/reply/{id}/delete")
    public String deleteReply(@PathVariable("id") Integer id,
                              @RequestParam(value = "reason", required = false, defaultValue = "Moderator deletion") String reason,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (!isAdminLoggedIn(session)) {
            return "redirect:/login";
        }

        Integer adminId = (Integer) session.getAttribute("adminId");
        if (adminId == null) {
            adminId = 0;
        }

        try {
            communityService.deleteReplyAsModerator(id, adminId, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Reply deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete reply: " + e.getMessage());
        }
        return "redirect:/admin/community";
    }

    /**
     * Moderator Reply to Post
     */
    @PostMapping("/community/post/{id}/reply")
    public String replyToPost(@PathVariable("id") Integer id,
                              @RequestParam("content") String content,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (!isAdminLoggedIn(session)) {
            return "redirect:/login";
        }

        try {
            // You can mark admin replies differently if needed or use a separate service/flag.
            // But we can just create a reply as normal using a placeholder studentId or use a dedicated admin logic.
            // Alternatively, wait, the standard service requires `studentId`. So we need a special method.
            String adminDisplayName = (String) session.getAttribute("adminDisplayName");
            communityService.addAdminReply(id, content, adminDisplayName != null ? adminDisplayName : "Admin");
            redirectAttributes.addFlashAttribute("successMessage", "Reply added successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to add reply: " + e.getMessage());
        }
        return "redirect:/admin/community";
    }

    /**
     * Moderator Reply to Post (template-compatible route)
     */
    @PostMapping("/community/post/{id}/replies")
    public String replyToPostTemplateRoute(@PathVariable("id") Integer id,
                                           @RequestParam("message") String message,
                                           HttpSession session,
                                           RedirectAttributes redirectAttributes) {
        if (!isAdminLoggedIn(session)) {
            return "redirect:/login";
        }

        try {
            CommunityModerationResultDTO moderation = moderationService.moderateText(message, "reply");
            if (!"ALLOW".equals(moderation.getDecision())) {
                redirectAttributes.addFlashAttribute("errorMessage", moderation.getReason());
                return "redirect:/admin/community";
            }

            String adminDisplayName = (String) session.getAttribute("adminDisplayName");
            communityService.addAdminReply(id, message, adminDisplayName != null ? adminDisplayName : "Admin");
            redirectAttributes.addFlashAttribute("successMessage", "Reply added successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to add reply: " + e.getMessage());
        }
        return "redirect:/admin/community";
    }

    @PostMapping("/community/moderate")
    @ResponseBody
    public ResponseEntity<CommunityModerationResultDTO> moderateAdminMessage(@RequestBody CommunityModerationRequestDTO dto,
                                                                              HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommunityModerationResultDTO("BLOCK", "Please log in again.", 100, ""));
        }

        String contentType = dto.getContentType() == null ? "reply" : dto.getContentType();
        CommunityModerationResultDTO result = moderationService.moderateLiveText(dto.getMessage(), contentType);
        return ResponseEntity.ok(result);
    }

    private List<Concern> applyPrioritySort(List<Concern> concerns, String selectedPrioritySort) {
        if (concerns == null || concerns.isEmpty() || "Default".equalsIgnoreCase(selectedPrioritySort)) {
            return concerns;
        }

        if ("LowToHigh".equalsIgnoreCase(selectedPrioritySort)) {
            return concerns.stream()
                    .sorted(Comparator.comparingInt((Concern concern) -> lowToHighPriorityRank(concern.getAiPriorityLevel()))
                            .thenComparing(Concern::getCreatedTime, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        }

        return concerns.stream()
                .sorted(Comparator.comparingInt((Concern concern) -> highToLowPriorityRank(concern.getAiPriorityLevel()))
                        .thenComparing(Concern::getCreatedTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private int highToLowPriorityRank(String value) {
        String priority = defaultPriority(value);
        if ("High".equalsIgnoreCase(priority)) {
            return 0;
        }
        if ("Medium".equalsIgnoreCase(priority)) {
            return 1;
        }
        if ("Low".equalsIgnoreCase(priority)) {
            return 2;
        }
        return 3;
    }

    private int lowToHighPriorityRank(String value) {
        String priority = defaultPriority(value);
        if ("Low".equalsIgnoreCase(priority)) {
            return 0;
        }
        if ("Medium".equalsIgnoreCase(priority)) {
            return 1;
        }
        if ("High".equalsIgnoreCase(priority)) {
            return 2;
        }
        return 3;
    }

    private String normalizePriorityFilter(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "All";
        }

        String normalized = value.trim();
        if ("High".equalsIgnoreCase(normalized)
                || "Medium".equalsIgnoreCase(normalized)
                || "Low".equalsIgnoreCase(normalized)) {
            return normalized;
        }
        return "All";
    }

    private String normalizePrioritySort(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Default";
        }

        String normalized = value.trim();
        if ("HighToLow".equalsIgnoreCase(normalized)
                || "LowToHigh".equalsIgnoreCase(normalized)
                || "Default".equalsIgnoreCase(normalized)) {
            return normalized;
        }
        return "Default";
    }

    private boolean isAllPriorities(String value) {
        return value == null || value.trim().isEmpty() || "All".equalsIgnoreCase(value.trim());
    }

    private String defaultPriority(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Medium";
        }
        return value.trim();
    }

    private String normalizeStatusFilter(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "All";
        }

        String normalized = value.trim();
        if ("All".equalsIgnoreCase(normalized)
                || "Pending".equalsIgnoreCase(normalized)
                || "In Progress".equalsIgnoreCase(normalized)
                || "Meeting Scheduled".equalsIgnoreCase(normalized)
                || "Complete".equalsIgnoreCase(normalized)) {
            return normalized;
        }
        return "All";
    }

    private boolean isStudentMessage(AdminReply reply) {
        return reply != null
                && reply.getSenderRole() != null
                && "STUDENT".equalsIgnoreCase(reply.getSenderRole().trim());
    }

    /**
     * Check if admin is logged in
     */
    private boolean isAdminLoggedIn(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("adminLoggedIn"));
    }
}
