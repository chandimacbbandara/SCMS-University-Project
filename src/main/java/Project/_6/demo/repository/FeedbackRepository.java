package Project._6.demo.repository;

import Project._6.demo.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {
    Optional<Feedback> findTopByConcern_ConcernIdOrderBySubmissionTimeDesc(Integer concernId);
    List<Feedback> findAllByConcern_ConcernId(Integer concernId);
    List<Feedback> findAllByOrderBySubmissionTimeDesc();
    boolean existsByConcern_ConcernId(Integer concernId);
    void deleteByConcern_ConcernId(Integer concernId);

    @Query("""
            SELECT f
            FROM Feedback f
            WHERE (f.adminReply IS NOT NULL AND f.adminReply.admin.userId = :adminUserId)
               OR (f.adminReply IS NULL AND f.concern.admin IS NOT NULL AND f.concern.admin.userId = :adminUserId)
            ORDER BY f.submissionTime DESC
            """)
    List<Feedback> findByRatedAdminUserId(@Param("adminUserId") Integer adminUserId);
}
