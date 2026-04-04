package Project._6.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Student_Community_Post")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentCommunityPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PostID")
    private Integer postId;

    @Column(name = "Title", nullable = false, length = 160)
    private String title;

    @Column(name = "Message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "Category", nullable = false, length = 80)
    private String category;

    @Column(name = "Status", nullable = false, length = 20)
    private String status;

    @Column(name = "CreatedTime", nullable = false)
    private LocalDateTime createdTime;

    @Column(name = "UpdatedTime", nullable = false)
    private LocalDateTime updatedTime;

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
