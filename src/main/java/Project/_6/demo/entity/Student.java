package Project._6.demo.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "Student")
@Data
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="StudentID")
    private Long studentId;

    private String name;
    private String email;
    @JsonIgnore
    private String password;


}