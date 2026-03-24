package Project._6.demo.repository;

import Project._6.demo.entity.StudentCommunityRulesAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentCommunityRulesAcceptanceRepository extends JpaRepository<StudentCommunityRulesAcceptance, Integer> {
    Optional<StudentCommunityRulesAcceptance> findTopByStudent_UserIdOrderByAcceptedAtDesc(Integer userId);
}
