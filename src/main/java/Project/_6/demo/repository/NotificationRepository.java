package Project._6.demo.repository;

import Project._6.demo.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findByStudent_UserIdOrderBySentTimeDesc(Integer userId);

    List<Notification> findByStudent_UserIdAndIsReadFalseOrderBySentTimeDesc(Integer userId);

    long countByStudent_UserIdAndIsReadFalse(Integer userId);

    List<Notification> findByConcern_ConcernIdOrderBySentTimeDesc(Integer concernId);

    void deleteByConcern_ConcernId(Integer concernId);

    void deleteByStudent_UserId(Integer userId);

    // Broadcast notifications (no specific student, sent to all)
    List<Notification> findByTargetAudienceAndStudentIsNullOrderBySentTimeDesc(String targetAudience);

    List<Notification> findByTypeOrderBySentTimeDesc(String type);
}
