package Project._6.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "Student")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Student implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "UserID")
    private Integer userId;

    @OneToOne(fetch = FetchType.EAGER)
    @MapsId
    @JoinColumn(name = "UserID")
    private User user;

    @Column(name = "StudentID", unique = true, nullable = false, length = 20)
    private String studentId;

    @Column(name = "DOB")
    private LocalDate dob;

    @Lob
    @Column(name = "StudentDPhoto", columnDefinition = "LONGBLOB")
    private byte[] studentPhoto;

    @Column(name = "Category", length = 50)
    private String category;
}
