package Project._6.demo.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsReportDTO {

    private String timePeriod;
    private Integer totalConcerns;
    private BigDecimal avgResolutionTime;
    private String mostFrequentCategory;
    private BigDecimal sentimentTrendScore;
    private Integer evidenceImageCount;
    private Integer adminIdFk;
}
