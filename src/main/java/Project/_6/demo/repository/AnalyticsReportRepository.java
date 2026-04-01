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

    @Query(value = "SELECT CAST(COLUMNPROPERTY(OBJECT_ID('dbo.Analytics_Report'), 'ReportID', 'IsIdentity') AS INT)", nativeQuery = true)
    Integer isReportIdIdentity();

    List<AnalyticsReport> findAllByOrderByCreatedTimeDesc();
}
