package Project._6.demo.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommunityModerationResultDTO {

    private String decision;
    private String reason;
    private Integer riskScore;
    private String correctedText;

    public CommunityModerationResultDTO(String decision, String reason, Integer riskScore) {
        this.decision = decision;
        this.reason = reason;
        this.riskScore = riskScore;
    }
}
