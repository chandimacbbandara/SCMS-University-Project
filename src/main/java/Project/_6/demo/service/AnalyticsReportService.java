package Project._6.demo.service;

import Project._6.demo.dto.AnalyticsReportDTO;
import Project._6.demo.entity.AnalyticsReport;
import Project._6.demo.repository.AnalyticsReportRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnalyticsReportService {

    private final AnalyticsReportRepository analyticsReportRepository;

    public AnalyticsReportService(AnalyticsReportRepository analyticsReportRepository) {
        this.analyticsReportRepository = analyticsReportRepository;
    }

    @Transactional
    public AnalyticsReport createReport(AnalyticsReportDTO dto) {
        AnalyticsReport report = new AnalyticsReport();
        assignReportIdIfRequired(report);
        report.setTimePeriod(dto.getTimePeriod() == null || dto.getTimePeriod().isBlank() ? "All Time" : dto.getTimePeriod());
        report.setTotalConcerns(dto.getTotalConcerns());
        report.setAvgResolutionTime(dto.getAvgResolutionTime());
        report.setMostFrequentCategory(dto.getMostFrequentCategory());
        report.setSentimentTrendScore(dto.getSentimentTrendScore());
        report.setEvidenceImageCount(dto.getEvidenceImageCount());
        report.setAdminIdFk(dto.getAdminIdFk());
        return analyticsReportRepository.save(report);
    }

    public List<AnalyticsReport> getAllReports() {
        return analyticsReportRepository.findAllByOrderByCreatedTimeDesc();
    }

    private void assignReportIdIfRequired(AnalyticsReport report) {
        if (report == null || report.getReportId() != null) {
            return;
        }

        Integer identityFlag = analyticsReportRepository.isReportIdIdentity();
        boolean isIdentity = identityFlag != null && identityFlag == 1;
        if (!isIdentity) {
            report.setReportId(analyticsReportRepository.getNextReportId());
        }
    }
}
