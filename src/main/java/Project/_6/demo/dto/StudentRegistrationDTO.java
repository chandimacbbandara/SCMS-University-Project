package Project._6.demo.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentRegistrationDTO {

    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String confirmPassword;
    private String studentId;
}
