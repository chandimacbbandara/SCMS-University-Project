package Project._6.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "[User]")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "UserID")
    private Integer userId;

    @Column(name = "Email", unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "Password", nullable = false, length = 255)
    private String password;

    @Column(name = "Gender", length = 10)
    private String gender;

    @Column(name = "Phone_number", length = 15)
    private String phoneNumber;

    @Column(name = "First_Name", length = 100)
    private String firstName;

    @Column(name = "Last_Name", length = 100)
    private String lastName;

    @Column(name = "Address_1st_Lane", length = 255)
    private String address1stLane;

    @Column(name = "Address_2nd_Lane", length = 255)
    private String address2ndLane;

    @Column(name = "Address_3rd_Lane", length = 255)
    private String address3rdLane;

    @Column(name = "Registration_Status", length = 20)
    private String registrationStatus;
}
