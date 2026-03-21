package Project._6.demo.repository;

import Project._6.demo.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {
    Optional<Feedback> findByConcern_ConcernId(Integer concernId);
    boolean existsByConcern_ConcernId(Integer concernId);
    void deleteByConcern_ConcernId(Integer concernId);

    // Find all feedbacks for concerns handled by a specific admin
    List<Feedback> findByConcern_Admin_UserId(Integer adminUserId);
}
