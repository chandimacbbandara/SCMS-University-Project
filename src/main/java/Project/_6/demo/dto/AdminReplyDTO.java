package Project._6.demo.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminReplyDTO {

    private Integer concernId;
    private String replyMessage;
    private String newStatus;
}
