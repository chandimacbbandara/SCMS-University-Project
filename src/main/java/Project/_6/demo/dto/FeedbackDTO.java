package Project._6.demo.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackDTO {
    private Integer concernId;
    private Integer rating;
    private String comments;
}
