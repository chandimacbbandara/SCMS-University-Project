package Project._6.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Analytics_Report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsReport {

    @Id
    @Column(name = "ReportID")
    private Integer reportId;

    @Column(name = "TimePeriod", length = 50)
    private String timePeriod;

    @Column(name = "TotalConcerns")
    private Integer totalConcerns;

    @Column(name = "AvgResolutionTime", precision = 10, scale = 2)
    private BigDecimal avgResolutionTime;

    @Column(name = "MostFrequentCategory", length = 50)
    private String mostFrequentCategory;

    @Column(name = "SentimentTrendScore", precision = 5, scale = 2)
    private BigDecimal sentimentTrendScore;

    @Column(name = "EvidenceImageCount")
    private Integer evidenceImageCount;

    @Column(name = "AdminID_FK")
    private Integer adminIdFk;

    @Column(name = "CreatedTime")
    private LocalDateTime createdTime;

    @PrePersist
    protected void onCreate() {
        this.createdTime = LocalDateTime.now();
    }
}
