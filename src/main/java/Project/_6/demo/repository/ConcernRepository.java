package Project._6.demo.repository;

import Project._6.demo.entity.Concern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ConcernRepository extends JpaRepository<Concern, Integer> {
    List<Concern> findByStudent_StudentId(String studentId);
    List<Concern> findAllByOrderByCreatedTimeDesc();
    List<Concern> findByStatusOrderByCreatedTimeDesc(String status);
    long countByStatus(String status);

    @Query(value = "SELECT COALESCE(MAX(ConcernID), 0) + 1 FROM Concern", nativeQuery = true)
    Integer getNextConcernId();

    // Category count
    long countByCategory(String category);

    long countByAdmin_UserId(Integer adminUserId);
    List<Concern> findByAdmin_UserId(Integer adminUserId);

    // Time range queries
    List<Concern> findByCreatedTimeBetweenOrderByCreatedTimeDesc(LocalDateTime from, LocalDateTime to);
    List<Concern> findByStatusAndCreatedTimeBetweenOrderByCreatedTimeDesc(String status, LocalDateTime from, LocalDateTime to);
}
