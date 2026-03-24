package Project._6.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Student_Community_Reply")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentCommunityReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReplyID")
    private Integer replyId;

    @Column(name = "Message", nullable = false, columnDefinition = "VARCHAR(MAX)")
    private String message;

    @Column(name = "Status", nullable = false, length = 20)
    private String status;

    @Column(name = "CreatedTime", nullable = false)
    private LocalDateTime createdTime;

    @Column(name = "UpdatedTime", nullable = false)
    private LocalDateTime updatedTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PostID_FK", nullable = false)
    private StudentCommunityPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StudentID_FK", nullable = false)
    private Student student;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdTime = now;
        this.updatedTime = now;
        if (this.status == null) {
            this.status = "ACTIVE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedTime = LocalDateTime.now();
    }
}
