package Project._6.demo.repository;

import Project._6.demo.entity.Concern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConcernRepository extends JpaRepository<Concern, Integer> {
    List<Concern> findByStudent_StudentId(String studentId);
    List<Concern> findByStudent_UserId(Integer userId);
    Optional<Concern> findByConcernIdAndStudent_UserId(Integer concernId, Integer userId);
    List<Concern> findAllByOrderByCreatedTimeDesc();

    @Query("SELECT c.category, c.status FROM Concern c WHERE c.status != 'Draft' OR c.status IS NULL")
    List<Object[]> findAllAnalyticsData();
    List<Concern> findByStatusOrderByCreatedTimeDesc(String status);
    long countByStatus(String status);

    @Query(value = "SELECT COALESCE(MAX(ConcernID), 0) + 1 FROM Concern", nativeQuery = true)
    Integer getNextConcernId();

        @Query(value = "SELECT CASE WHEN EXTRA LIKE '%auto_increment%' THEN 1 ELSE 0 END " +
            "FROM information_schema.columns " +
            "WHERE table_schema = DATABASE() AND table_name = 'Concern' AND column_name = 'ConcernID' LIMIT 1", nativeQuery = true)
    Integer isConcernIdIdentity();

    // Category count
    long countByCategory(String category);

    long countByAdmin_UserId(Integer adminUserId);
    long countByAdmin_UserIdAndCategory(Integer adminUserId, String category);
    List<Concern> findByAdmin_UserId(Integer adminUserId);
    List<Concern> findByAdmin_UserIdAndCategory(Integer adminUserId, String category);

    // Time range queries
    List<Concern> findByCreatedTimeBetweenOrderByCreatedTimeDesc(LocalDateTime from, LocalDateTime to);
    List<Concern> findByStatusAndCreatedTimeBetweenOrderByCreatedTimeDesc(String status, LocalDateTime from, LocalDateTime to);
}
