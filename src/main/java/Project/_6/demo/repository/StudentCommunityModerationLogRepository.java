package Project._6.demo.repository;

import Project._6.demo.entity.StudentCommunityModerationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentCommunityModerationLogRepository extends JpaRepository<StudentCommunityModerationLog, Integer> {
}
