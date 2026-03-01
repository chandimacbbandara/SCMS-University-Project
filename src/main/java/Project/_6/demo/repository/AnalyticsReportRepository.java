package Project._6.demo.repository;

import Project._6.demo.entity.AnalyticsReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalyticsReportRepository extends JpaRepository<AnalyticsReport, Integer> {
    List<AnalyticsReport> findAllByOrderByCreatedTimeDesc();
}
