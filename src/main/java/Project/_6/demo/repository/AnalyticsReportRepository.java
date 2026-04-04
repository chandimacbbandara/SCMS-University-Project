package Project._6.demo.repository;

import Project._6.demo.entity.AnalyticsReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalyticsReportRepository extends JpaRepository<AnalyticsReport, Integer> {
    @Query(value = "SELECT COALESCE(MAX(ReportID), 0) + 1 FROM Analytics_Report", nativeQuery = true)
    Integer getNextReportId();

        @Query(value = "SELECT CASE WHEN EXTRA LIKE '%auto_increment%' THEN 1 ELSE 0 END " +
            "FROM information_schema.columns " +
            "WHERE table_schema = DATABASE() AND table_name = 'Analytics_Report' AND column_name = 'ReportID' LIMIT 1", nativeQuery = true)
    Integer isReportIdIdentity();

    List<AnalyticsReport> findAllByOrderByCreatedTimeDesc();
}
