package Project._6.demo.repository;

import Project._6.demo.entity.Tip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipRepository extends JpaRepository<Tip, Long> {
    List<Tip> findAllByOrderByCreatedAtDesc();
}
