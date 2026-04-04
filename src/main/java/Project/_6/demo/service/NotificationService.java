package Project._6.demo.service;

import Project._6.demo.entity.Concern;
import Project._6.demo.entity.ConcernMeetingProposal;
import Project._6.demo.entity.ConcernMeetingSlot;
import Project._6.demo.entity.Notification;
import Project._6.demo.entity.Student;
import Project._6.demo.repository.NotificationRepository;
import Project._6.demo.repository.StudentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final DateTimeFormatter EMAIL_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final NotificationRepository notificationRepository;
    private final StudentRepository studentRepository;
    private final EmailVerificationService emailVerificationService;
    private final Object broadcastBackfillLock = new Object();

    public NotificationService(NotificationRepository notificationRepository,
                               StudentRepository studentRepository,
                               EmailVerificationService emailVerificationService) {
        this.notificationRepository = notificationRepository;
        this.studentRepository = studentRepository;
        this.emailVerificationService = emailVerificationService;
    }

    private Notification newNotification() {
        Notification notification = new Notification();
        assignNotificationIdIfRequired(notification);
        return notification;
    }

    /**
     * Create a notification when a student submits a concern.
     * Step 1: "Concern Submitted"
     */
    @Transactional
    public Notification notifyConcernSubmitted(Concern concern) {
        Notification notification = newNotification();
        notification.setTitle("Concern Submitted Successfully");
        notification.setMessage("Your concern \"" + concern.getSubject() + "\" (REF: CON-" + concern.getConcernId()
                + ") has been submitted and is awaiting admin review.");
        notification.setType("SUBMITTED");
        notification.setStudent(concern.getStudent());
        notification.setConcern(concern);
        Notification saved = notificationRepository.save(notification);

        CompletableFuture.runAsync(() -> {
            emailVerificationService.sendConcernStatusEmail(concern, "SUBMITTED");
        });

        return saved;
    }

    /**
     * Create a notification when admin marks the concern as "In Progress" (mark as read).
     * Step 2: "Concern In Progress"
     */
    @Transactional
    public Notification notifyConcernInProgress(Concern concern) {
        Notification notification = newNotification();
        notification.setTitle("Concern Under Review");
        notification.setMessage("Your concern \"" + concern.getSubject() + "\" (REF: CON-" + concern.getConcernId()
                + ") has been reviewed by an administrator and is now being processed.");
        notification.setType("IN_PROGRESS");
        notification.setStudent(concern.getStudent());
        notification.setConcern(concern);
        Notification saved = notificationRepository.save(notification);

        CompletableFuture.runAsync(() -> {
            emailVerificationService.sendConcernStatusEmail(concern, "READ");
        });

        return saved;
    }

    /**
     * Create a notification when admin replies and marks concern as "Complete".
     * Step 3: "Concern Resolved"
     */
    @Transactional
    public Notification notifyConcernComplete(Concern concern) {
        Notification notification = newNotification();
        notification.setTitle("Concern Resolved");
        notification.setMessage("Your concern \"" + concern.getSubject() + "\" (REF: CON-" + concern.getConcernId()
                + ") has been resolved by the admin. Check the reply for more details.");
        notification.setType("COMPLETE");
        notification.setStudent(concern.getStudent());
        notification.setConcern(concern);
        Notification saved = notificationRepository.save(notification);

        CompletableFuture.runAsync(() -> {
            emailVerificationService.sendConcernStatusEmail(concern, "REPLIED");
        });

        return saved;
    }

    @Transactional
    public Notification notifyMeetingSlotsProposed(Concern concern,
                                                   ConcernMeetingProposal proposal,
                                                   List<ConcernMeetingSlot> slots) {
        Notification notification = newNotification();
        notification.setTitle("Physical Meeting Time Slots Available");
        notification.setMessage("Admin has shared available meeting slots for concern CON-"
                + concern.getConcernId() + ". Please review and book the best time.");
        notification.setType("MEETING_PROPOSED");
        notification.setStudent(concern.getStudent());
        notification.setConcern(concern);
        Notification saved = notificationRepository.save(notification);

        CompletableFuture.runAsync(() -> {
            emailVerificationService.sendMeetingSlotsProposalEmail(concern, proposal, slots);
        });

        return saved;
    }

    @Transactional
    public Notification notifyMeetingSlotBooked(Concern concern,
                                                ConcernMeetingProposal proposal,
                                                ConcernMeetingSlot slot) {
        Notification notification = newNotification();
        notification.setTitle("Physical Meeting Slot Confirmed");
        notification.setMessage("You booked a physical meeting for concern CON-" + concern.getConcernId()
            + " on " + formatSlot(slot) + ". Concern status is now Meeting Scheduled until admin completes it.");
        notification.setType("MEETING_BOOKED");
        notification.setStudent(concern.getStudent());
        notification.setConcern(concern);
        Notification saved = notificationRepository.save(notification);

        CompletableFuture.runAsync(() -> {
            emailVerificationService.sendMeetingBookedEmailToStudent(concern, slot);
            emailVerificationService.sendMeetingBookedEmailToAdmin(concern, proposal, slot);
        });

        return saved;
    }

    @Transactional
    public Notification notifyMeetingProposalDeclined(Concern concern,
                                                      ConcernMeetingProposal proposal) {
        Notification notification = newNotification();
        notification.setTitle("Meeting Reschedule Requested");
        notification.setMessage("You marked the proposed meeting slots as unavailable for concern CON-"
                + concern.getConcernId() + ". Admin will share new options.");
        notification.setType("MEETING_DECLINED");
        notification.setStudent(concern.getStudent());
        notification.setConcern(concern);
        Notification saved = notificationRepository.save(notification);

        CompletableFuture.runAsync(() -> {
            emailVerificationService.sendMeetingDeclinedEmailToStudent(concern, proposal);
            emailVerificationService.sendMeetingDeclinedEmailToAdmin(concern, proposal);
        });

        return saved;
    }

    /**
     * Send an email when a concern is deleted by an administrator.
     */
    public void sendConcernDeletedEmail(Concern concern) {
        CompletableFuture.runAsync(() -> {
            emailVerificationService.sendConcernStatusEmail(concern, "DELETED");
        });
    }

    /**
     * Send an email when a concern is moved to a different department/category.
     */
    public void sendConcernDepartmentChangedEmail(Concern concern, String previousDepartment, String newDepartment) {
        CompletableFuture.runAsync(() -> {
            emailVerificationService.sendConcernDepartmentChangedEmail(concern, previousDepartment, newDepartment);
        });
    }

    /**
     * Get all notifications for a student ordered by newest first.
     */
    public List<Notification> getNotificationsForStudent(Integer userId) {
        ensureBroadcastCopiesForStudent(userId);
        return notificationRepository.findByStudent_UserIdAndIsHiddenFalseOrderBySentTimeDesc(userId);
    }

    /**
     * Get unread notifications for a student.
     */
    public List<Notification> getUnreadNotifications(Integer userId) {
        return notificationRepository.findByStudent_UserIdAndIsReadFalseAndIsHiddenFalseOrderBySentTimeDesc(userId);
    }

    /**
     * Count unread notifications for a student.
     */
    public long getUnreadCount(Integer userId) {
        ensureBroadcastCopiesForStudent(userId);
        return notificationRepository.countByStudent_UserIdAndIsReadFalseAndIsHiddenFalse(userId);
    }

    /**
     * Mark a single notification as read.
     */
    @Transactional
    public void markAsRead(Integer notificationId, Integer userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (notification.getStudent() == null
                || notification.getStudent().getUserId() == null
                || !notification.getStudent().getUserId().equals(userId)) {
            throw new RuntimeException("Notification not found for this student");
        }
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    /**
     * Mark all notifications as read for a student.
     */
    @Transactional
    public void markAllAsRead(Integer userId) {
        List<Notification> unread = notificationRepository.findByStudent_UserIdAndIsReadFalseAndIsHiddenFalseOrderBySentTimeDesc(userId);
        for (Notification n : unread) {
            n.setIsRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    /**
     * Hide a notification for a specific student so it no longer appears in the list.
     */
    @Transactional
    public void hideNotification(Integer notificationId, Integer userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (notification.getStudent() == null
                || notification.getStudent().getUserId() == null
                || !notification.getStudent().getUserId().equals(userId)) {
            throw new RuntimeException("Notification not found for this student");
        }

        notification.setIsHidden(true);
        notificationRepository.save(notification);
    }

    /**
     * Get tracking steps (all notifications) for a specific concern.
     */
    public List<Notification> getTrackingForConcern(Integer concernId) {
        return notificationRepository.findByConcern_ConcernIdOrderBySentTimeDesc(concernId);
    }

    /**
     * Delete all notifications tied to a specific concern.
     */
    @Transactional
    public void deleteByConcernId(Integer concernId) {
        notificationRepository.deleteByConcern_ConcernId(concernId);
    }

    /**
     * Create a broadcast notification sent by the owner to ALL students.
     */
    @Transactional
    public Notification createBroadcastNotification(String title, String message, String targetAudience, Integer adminIdFk) {
        if (title == null || title.trim().isEmpty()) {
            throw new RuntimeException("Notification title is required.");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new RuntimeException("Notification message is required.");
        }

        String normalizedAudience = normalizeTargetAudience(targetAudience);

        synchronized (broadcastBackfillLock) {
            Notification masterNotification = newNotification();
            masterNotification.setTitle(title.trim());
            masterNotification.setMessage(message.trim());
            masterNotification.setType("BROADCAST");
            masterNotification.setTargetAudience(normalizedAudience);
            masterNotification.setAdminIdFk(adminIdFk);
            masterNotification.setStudent(null);
            masterNotification.setConcern(null);
            Notification savedMaster = notificationRepository.save(masterNotification);

            List<Student> recipients = resolveTargetStudents(normalizedAudience);
            if (!recipients.isEmpty()) {
                List<Notification> recipientNotifications = new ArrayList<>(recipients.size());
                for (Student student : recipients) {
                    Notification studentNotification = newNotification();
                    studentNotification.setTitle(savedMaster.getTitle());
                    studentNotification.setMessage(savedMaster.getMessage());
                    studentNotification.setType(savedMaster.getType());
                    studentNotification.setTargetAudience(savedMaster.getTargetAudience());
                    studentNotification.setAdminIdFk(savedMaster.getAdminIdFk());
                    studentNotification.setStudent(student);
                    studentNotification.setConcern(null);
                    studentNotification.setSentTime(savedMaster.getSentTime());
                    studentNotification.setIsRead(false);
                    recipientNotifications.add(studentNotification);
                }
                notificationRepository.saveAll(recipientNotifications);
            }

            return savedMaster;
        }
    }

    /**
     * Get all broadcast notifications (for owner to see sent history).
     */
    public List<Notification> getAllBroadcastNotifications() {
        return notificationRepository.findByTypeAndStudentIsNullOrderBySentTimeDesc("BROADCAST");
    }

    /**
     * Get a specific notification by its ID
     */
    public Notification getNotificationById(Integer id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found with ID: " + id));
    }

    /**
     * Delete a notification
     */
    @Transactional
    public void deleteNotification(Integer id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found."));

        if (isBroadcastMaster(notification)) {
            List<Notification> relatedStudentCopies = findRelatedBroadcastCopies(notification);
            if (!relatedStudentCopies.isEmpty()) {
                notificationRepository.deleteAll(relatedStudentCopies);
            }
        }

        notificationRepository.delete(notification);
    }

    /**
     * Update an existing broadcast notification (only allowed within 24 hours of creation)
     */
    @Transactional
    public Notification updateBroadcastNotification(Integer id, String title, String message, String targetAudience) {
        Notification notification = getNotificationById(id);
        if (!isBroadcastMaster(notification)) {
            throw new RuntimeException("Only original broadcast notifications can be updated.");
        }

        List<Notification> relatedStudentCopies = findRelatedBroadcastCopies(notification);

        if (notification.getSentTime() != null) {
            java.time.Duration duration = java.time.Duration.between(notification.getSentTime(), java.time.LocalDateTime.now());
            if (duration.toHours() >= 24) {
                throw new RuntimeException("Warning: Notifications older than 24 hours cannot be modified.");
            }
        }

        if (title == null || title.trim().isEmpty()) {
            throw new RuntimeException("Notification title is required.");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new RuntimeException("Notification message is required.");
        }

        String normalizedAudience = notification.getTargetAudience();
        if (targetAudience != null && !targetAudience.trim().isEmpty()) {
            normalizedAudience = targetAudience.trim();
        }

        notification.setTitle(title.trim());
        notification.setMessage(message.trim());
        notification.setTargetAudience(normalizedAudience);

        for (Notification relatedStudentCopy : relatedStudentCopies) {
            relatedStudentCopy.setTitle(notification.getTitle());
            relatedStudentCopy.setMessage(notification.getMessage());
            relatedStudentCopy.setTargetAudience(notification.getTargetAudience());
        }

        Notification saved = notificationRepository.save(notification);
        if (!relatedStudentCopies.isEmpty()) {
            notificationRepository.saveAll(relatedStudentCopies);
        }

        return saved;
    }

    private String formatSlot(ConcernMeetingSlot slot) {
        if (slot == null || slot.getStartTime() == null || slot.getEndTime() == null) {
            return "the selected time";
        }
        return slot.getStartTime().format(EMAIL_TIME_FORMAT)
                + " - "
                + slot.getEndTime().format(EMAIL_TIME_FORMAT);
    }

    private String normalizeTargetAudience(String targetAudience) {
        if (targetAudience == null || targetAudience.trim().isEmpty()) {
            return "ALL_STUDENTS";
        }
        return targetAudience.trim();
    }

    private List<Student> resolveTargetStudents(String targetAudience) {
        List<Student> approvedStudents = studentRepository.findByUser_RegistrationStatus("Approved");
        if ("ALL_STUDENTS".equalsIgnoreCase(targetAudience)) {
            return approvedStudents;
        }

        return approvedStudents.stream()
                .filter(student -> isStudentInAudience(student, targetAudience))
                .toList();
    }

    @Transactional
    protected void ensureBroadcastCopiesForStudent(Integer userId) {
        if (userId == null) {
            return;
        }

        synchronized (broadcastBackfillLock) {
            Student student = studentRepository.findById(userId).orElse(null);
            if (student == null) {
                return;
            }

            List<Notification> broadcastMasters = notificationRepository.findByTypeAndStudentIsNullOrderBySentTimeDesc("BROADCAST");
            if (broadcastMasters.isEmpty()) {
                return;
            }

            List<Notification> existingBroadcastCopies = notificationRepository.findByStudent_UserIdAndTypeOrderBySentTimeDesc(userId, "BROADCAST");
            Set<String> validMasterKeys = broadcastMasters.stream()
                    .filter(master -> isStudentInAudience(student, normalizeTargetAudience(master.getTargetAudience())))
                    .map(master -> buildBroadcastKey(
                            master.getAdminIdFk(),
                            normalizeTargetAudience(master.getTargetAudience()),
                            master.getTitle(),
                            master.getMessage(),
                            master.getSentTime()
                    ))
                    .collect(Collectors.toCollection(HashSet::new));

            hideDuplicateBroadcastCopies(existingBroadcastCopies, validMasterKeys);

            Set<String> existingKeys = existingBroadcastCopies.stream()
                    .map(this::buildBroadcastKey)
                    .filter(validMasterKeys::contains)
                    .collect(Collectors.toSet());

            List<Notification> missingCopies = new ArrayList<>();
            for (Notification master : broadcastMasters) {
                String normalizedAudience = normalizeTargetAudience(master.getTargetAudience());
                if (!isStudentInAudience(student, normalizedAudience)) {
                    continue;
                }

                String key = buildBroadcastKey(
                        master.getAdminIdFk(),
                        normalizedAudience,
                        master.getTitle(),
                        master.getMessage(),
                        master.getSentTime()
                );
                if (existingKeys.contains(key)) {
                    continue;
                }

                Notification studentNotification = newNotification();
                studentNotification.setTitle(master.getTitle());
                studentNotification.setMessage(master.getMessage());
                studentNotification.setType(master.getType());
                studentNotification.setTargetAudience(normalizedAudience);
                studentNotification.setAdminIdFk(master.getAdminIdFk());
                studentNotification.setStudent(student);
                studentNotification.setConcern(null);
                studentNotification.setSentTime(master.getSentTime());
                studentNotification.setIsRead(false);
                studentNotification.setIsHidden(false);

                missingCopies.add(studentNotification);
                existingKeys.add(key);
            }

            if (!missingCopies.isEmpty()) {
                notificationRepository.saveAll(missingCopies);
            }
        }
    }

    private void hideDuplicateBroadcastCopies(List<Notification> existingBroadcastCopies, Set<String> validMasterKeys) {
        if (existingBroadcastCopies == null || existingBroadcastCopies.isEmpty()) {
            return;
        }

        Map<String, List<Notification>> groupedByBroadcast = new LinkedHashMap<>();
        for (Notification notification : existingBroadcastCopies) {
            groupedByBroadcast
                    .computeIfAbsent(buildBroadcastKey(notification), key -> new ArrayList<>())
                    .add(notification);
        }

        List<Notification> duplicatesToHide = new ArrayList<>();
        for (Map.Entry<String, List<Notification>> entry : groupedByBroadcast.entrySet()) {
            String key = entry.getKey();
            List<Notification> group = entry.getValue();

            if (!validMasterKeys.contains(key)) {
                for (Notification candidate : group) {
                    if (!Boolean.TRUE.equals(candidate.getIsHidden())) {
                        candidate.setIsHidden(true);
                        duplicatesToHide.add(candidate);
                    }
                }
                continue;
            }

            if (group.size() < 2) {
                continue;
            }

            Notification keep = selectBroadcastCopyToKeep(group);
            Integer keepId = keep.getNotificationId();

            for (Notification candidate : group) {
                if (keepId != null && keepId.equals(candidate.getNotificationId())) {
                    continue;
                }
                if (!Boolean.TRUE.equals(candidate.getIsHidden())) {
                    candidate.setIsHidden(true);
                    duplicatesToHide.add(candidate);
                }
            }
        }

        if (!duplicatesToHide.isEmpty()) {
            notificationRepository.saveAll(duplicatesToHide);
        }
    }

    private Notification selectBroadcastCopyToKeep(List<Notification> copies) {
        for (Notification copy : copies) {
            if (!Boolean.TRUE.equals(copy.getIsHidden())) {
                return copy;
            }
        }
        return copies.get(0);
    }

    private boolean isStudentInAudience(Student student, String targetAudience) {
        if (student == null) {
            return false;
        }

        String normalizedAudience = normalizeTargetAudience(targetAudience);
        if ("ALL_STUDENTS".equalsIgnoreCase(normalizedAudience)) {
            return true;
        }

        return student.getCategory() != null
                && student.getCategory().trim().equalsIgnoreCase(normalizedAudience);
    }

    private String buildBroadcastKey(Notification notification) {
        return buildBroadcastKey(
                notification.getAdminIdFk(),
                notification.getTargetAudience(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getSentTime()
        );
    }

    private String buildBroadcastKey(Integer adminIdFk,
                                     String targetAudience,
                                     String title,
                                     String message,
                                     LocalDateTime sentTime) {
        String adminKey = adminIdFk == null ? "null" : adminIdFk.toString();
        String audienceKey = normalizeTargetAudience(targetAudience);
        String titleKey = title == null ? "" : title;
        String messageKey = message == null ? "" : message;
        String sentTimeKey = sentTime == null ? "null" : sentTime.toString();
        return adminKey + "|" + audienceKey + "|" + titleKey + "|" + messageKey + "|" + sentTimeKey;
    }

    private boolean isBroadcastMaster(Notification notification) {
        return notification != null
                && "BROADCAST".equalsIgnoreCase(notification.getType())
                && notification.getStudent() == null;
    }

    private List<Notification> findRelatedBroadcastCopies(Notification masterNotification) {
        return notificationRepository.findByTypeAndStudentIsNotNullAndAdminIdFkAndTargetAudienceAndTitleAndMessageAndSentTime(
                masterNotification.getType(),
                masterNotification.getAdminIdFk(),
                masterNotification.getTargetAudience(),
                masterNotification.getTitle(),
                masterNotification.getMessage(),
                masterNotification.getSentTime()
        );
    }

    private void assignNotificationIdIfRequired(Notification notification) {
        if (notification == null || notification.getNotificationId() != null) {
            return;
        }

        Integer identityFlag = notificationRepository.isNotificationIdIdentity();
        boolean isIdentity = identityFlag != null && identityFlag == 1;
        if (!isIdentity) {
            notification.setNotificationId(notificationRepository.getNextNotificationId());
        }
    }
}
