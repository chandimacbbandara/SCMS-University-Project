package Project._6.demo.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConcernSubmissionDTO {

    private String studentId;
    private String firstName;
    private String lastName;
    private String email;
    private String subject;
    private String message;
    private String category;
}
