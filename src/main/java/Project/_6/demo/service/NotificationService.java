package Project._6.demo.service;

import Project._6.demo.entity.Concern;
import Project._6.demo.entity.Notification;
import Project._6.demo.repository.NotificationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    private Notification newNotification() {
        return new Notification();
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
        return notificationRepository.save(notification);
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
        return notificationRepository.save(notification);
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
        return notificationRepository.save(notification);
    }

    /**
     * Get all notifications for a student ordered by newest first.
     */
    public List<Notification> getNotificationsForStudent(Integer userId) {
        return notificationRepository.findByStudent_UserIdOrderBySentTimeDesc(userId);
    }

    /**
     * Get unread notifications for a student.
     */
    public List<Notification> getUnreadNotifications(Integer userId) {
        return notificationRepository.findByStudent_UserIdAndIsReadFalseOrderBySentTimeDesc(userId);
    }

    /**
     * Count unread notifications for a student.
     */
    public long getUnreadCount(Integer userId) {
        return notificationRepository.countByStudent_UserIdAndIsReadFalse(userId);
    }

    /**
     * Mark a single notification as read.
     */
    @Transactional
    public void markAsRead(Integer notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    /**
     * Mark all notifications as read for a student.
     */
    @Transactional
    public void markAllAsRead(Integer userId) {
        List<Notification> unread = notificationRepository.findByStudent_UserIdAndIsReadFalseOrderBySentTimeDesc(userId);
        for (Notification n : unread) {
            n.setIsRead(true);
        }
        notificationRepository.saveAll(unread);
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

        Notification notification = newNotification();
        notification.setTitle(title.trim());
        notification.setMessage(message.trim());
        notification.setType("BROADCAST");
        notification.setTargetAudience(targetAudience != null ? targetAudience : "ALL_STUDENTS");
        notification.setAdminIdFk(adminIdFk);
        notification.setStudent(null);
        notification.setConcern(null);
        return notificationRepository.save(notification);
    }

    /**
     * Get all broadcast notifications (for owner to see sent history).
     */
    public List<Notification> getAllBroadcastNotifications() {
        return notificationRepository.findByTypeOrderBySentTimeDesc("BROADCAST");
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
        if (!notificationRepository.existsById(id)) {
            throw new RuntimeException("Notification not found.");
        }
        notificationRepository.deleteById(id);
    }

    /**
     * Update an existing broadcast notification (only allowed within 24 hours of creation)
     */
    @Transactional
    public Notification updateBroadcastNotification(Integer id, String title, String message, String targetAudience) {
        Notification notification = getNotificationById(id);

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

        notification.setTitle(title.trim());
        notification.setMessage(message.trim());
        if (targetAudience != null && !targetAudience.trim().isEmpty()) {
            notification.setTargetAudience(targetAudience);
        }

        return notificationRepository.save(notification);
    }
}
