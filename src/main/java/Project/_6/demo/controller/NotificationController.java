package Project._6.demo.controller;

import Project._6.demo.entity.Notification;
import Project._6.demo.service.NotificationService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Get all notifications for the logged-in student (JSON API).
     */
    @GetMapping
    public ResponseEntity<?> getNotifications(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("studentUserId");
        if (userId == null) {
            return ResponseEntity.status(401).body("Not logged in");
        }

        List<Notification> allNotifications = notificationService.getNotificationsForStudent(userId);

        long unreadCount = notificationService.getUnreadCount(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("unreadCount", unreadCount);
        response.put("notifications", allNotifications.stream().map(n -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", n.getNotificationId());
            map.put("title", n.getTitle());
            map.put("message", n.getMessage());
            map.put("type", n.getType());
            map.put("isRead", n.getIsRead());
            map.put("sentTime", n.getSentTime().toString());
            if (n.getConcern() != null) {
                map.put("concernId", n.getConcern().getConcernId());
                map.put("concernSubject", n.getConcern().getSubject());
                map.put("concernStatus", n.getConcern().getStatus());
            }
            return map;
        }).collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    /**
     * Get unread count only (for badge updates via polling).
     */
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("studentUserId");
        if (userId == null) {
            return ResponseEntity.status(401).body("Not logged in");
        }
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    /**
     * Mark a single notification as read.
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable("id") Integer id, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("studentUserId");
        if (userId == null) {
            return ResponseEntity.status(401).body("Not logged in");
        }
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * Mark all notifications as read.
     */
    @PostMapping("/mark-all-read")
    public ResponseEntity<?> markAllAsRead(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("studentUserId");
        if (userId == null) {
            return ResponseEntity.status(401).body("Not logged in");
        }
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * Remove a single notification from the student's list.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> removeNotification(@PathVariable("id") Integer id, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("studentUserId");
        if (userId == null) {
            return ResponseEntity.status(401).body("Not logged in");
        }
        notificationService.hideNotification(id, userId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
