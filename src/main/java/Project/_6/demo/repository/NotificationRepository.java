package Project._6.demo.repository;

import Project._6.demo.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    @Query(value = "SELECT COALESCE(MAX(NotificationID), 0) + 1 FROM Notification", nativeQuery = true)
    Integer getNextNotificationId();

    List<Notification> findByStudent_UserIdOrderBySentTimeDesc(Integer userId);

    List<Notification> findByStudent_UserIdAndIsReadFalseOrderBySentTimeDesc(Integer userId);

    long countByStudent_UserIdAndIsReadFalse(Integer userId);

    List<Notification> findByConcern_ConcernIdOrderBySentTimeDesc(Integer concernId);

    void deleteByConcern_ConcernId(Integer concernId);

    // Broadcast notifications (no specific student, sent to all)
    List<Notification> findByTargetAudienceAndStudentIsNullOrderBySentTimeDesc(String targetAudience);

    List<Notification> findByTypeOrderBySentTimeDesc(String type);
}
