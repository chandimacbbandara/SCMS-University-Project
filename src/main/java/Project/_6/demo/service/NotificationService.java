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

    /**
     * Create a notification when a student submits a concern.
     * Step 1: "Concern Submitted"
     */
    @Transactional
    public Notification notifyConcernSubmitted(Concern concern) {
        Notification notification = new Notification();
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
        Notification notification = new Notification();
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
        Notification notification = new Notification();
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
}
