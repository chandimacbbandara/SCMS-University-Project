package Project._6.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Admin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Admin {

    @Id
    @Column(name = "UserID")
    private Integer userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "UserID")
    private User user;

    @Column(name = "StaffID", unique = true, nullable = false, length = 20)
    private String staffId;
}
