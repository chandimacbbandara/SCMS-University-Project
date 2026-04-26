package Project._6.demo.repository;

import Project._6.demo.entity.OverallFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OverallFeedbackRepository extends JpaRepository<OverallFeedback, Integer> {

    Optional<OverallFeedback> findByStudentUserId(Integer studentUserId);

    boolean existsByStudentUserId(Integer studentUserId);

    List<OverallFeedback> findAllByOrderBySubmittedAtDesc();

    List<OverallFeedback> findAllByOrderByRatingDesc();

    List<OverallFeedback> findAllByOrderByRatingAsc();

    List<OverallFeedback> findAllByOrderBySubmittedAtAsc();
}
