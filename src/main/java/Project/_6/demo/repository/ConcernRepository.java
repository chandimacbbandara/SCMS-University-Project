package Project._6.demo.repository;

import Project._6.demo.entity.Concern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConcernRepository extends JpaRepository<Concern, Integer> {
    List<Concern> findByStudent_StudentId(String studentId);
}
