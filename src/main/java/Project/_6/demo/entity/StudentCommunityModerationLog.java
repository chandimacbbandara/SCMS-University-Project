package Project._6.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Student_Community_Moderation_Log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentCommunityModerationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LogID")
    private Integer logId;

    @Column(name = "ContentType", nullable = false, length = 20)
    private String contentType;

    @Column(name = "Decision", nullable = false, length = 10)
    private String decision;

    @Column(name = "Reasons", length = 1000)
    private String reasons;

    @Column(name = "RiskScore")
    private Integer riskScore;

    @Column(name = "CreatedTime", nullable = false)
    private LocalDateTime createdTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StudentID_FK")
    private Student student;

    @PrePersist
    protected void onCreate() {
        if (this.createdTime == null) {
            this.createdTime = LocalDateTime.now();
        }
    }
}
