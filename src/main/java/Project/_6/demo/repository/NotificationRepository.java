package Project._6.demo.repository;

import Project._6.demo.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    @Query(value = "SELECT COALESCE(MAX(NotificationID), 0) + 1 FROM Notification", nativeQuery = true)
    Integer getNextNotificationId();

        @Query(value = "SELECT CASE WHEN EXTRA LIKE '%auto_increment%' THEN 1 ELSE 0 END " +
            "FROM information_schema.columns " +
            "WHERE table_schema = DATABASE() AND table_name = 'Notification' AND column_name = 'NotificationID' LIMIT 1", nativeQuery = true)
    Integer isNotificationIdIdentity();

    List<Notification> findByStudent_UserIdAndIsHiddenFalseOrderBySentTimeDesc(Integer userId);

    List<Notification> findByStudent_UserIdAndTypeOrderBySentTimeDesc(Integer userId, String type);

    List<Notification> findByStudent_UserIdAndIsReadFalseAndIsHiddenFalseOrderBySentTimeDesc(Integer userId);

    long countByStudent_UserIdAndIsReadFalseAndIsHiddenFalse(Integer userId);

    List<Notification> findByConcern_ConcernIdOrderBySentTimeDesc(Integer concernId);

    void deleteByConcern_ConcernId(Integer concernId);

    void deleteByStudent_UserId(Integer userId);

    // Broadcast notifications (no specific student, sent to all)
    List<Notification> findByTargetAudienceAndStudentIsNullOrderBySentTimeDesc(String targetAudience);

    List<Notification> findByTypeOrderBySentTimeDesc(String type);

    List<Notification> findByTypeAndStudentIsNullOrderBySentTimeDesc(String type);

    List<Notification> findByTypeAndStudentIsNotNullAndAdminIdFkAndTargetAudienceAndTitleAndMessageAndSentTime(
            String type,
            Integer adminIdFk,
            String targetAudience,
            String title,
            String message,
            LocalDateTime sentTime
    );
}
