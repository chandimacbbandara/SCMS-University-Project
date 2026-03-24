package Project._6.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Student_Community_Rules_Acceptance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentCommunityRulesAcceptance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AcceptanceID")
    private Integer acceptanceId;

    @Column(name = "RulesVersion", nullable = false, length = 20)
    private String rulesVersion;

    @Column(name = "AcceptedAt", nullable = false)
    private LocalDateTime acceptedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StudentID_FK", nullable = false)
    private Student student;

    @PrePersist
    protected void onCreate() {
        if (this.acceptedAt == null) {
            this.acceptedAt = LocalDateTime.now();
        }
    }
}
