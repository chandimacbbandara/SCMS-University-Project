package Project._6.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import Project._6.demo.entity.Concern;
import java.util.List;

public interface ConcernRepository extends JpaRepository<Concern, Integer> {

    List<Concern> findByStudentStudentId(Long studentId);
}