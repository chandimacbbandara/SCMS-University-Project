package Project._6.demo.repository;

import Project._6.demo.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {
    @Query(value = "SELECT COALESCE(MAX(FeedbackID), 0) + 1 FROM Feedback", nativeQuery = true)
    Integer getNextFeedbackId();

    Optional<Feedback> findByConcern_ConcernId(Integer concernId);
    boolean existsByConcern_ConcernId(Integer concernId);
    void deleteByConcern_ConcernId(Integer concernId);

    // Find all feedbacks for concerns handled by a specific admin
    List<Feedback> findByConcern_Admin_UserId(Integer adminUserId);
}
