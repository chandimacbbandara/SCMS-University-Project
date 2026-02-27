package Project._6.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NotificationID")
    private Integer notificationId;

    @Column(name = "Title", length = 255)
    private String title;

    @Column(name = "Message", columnDefinition = "VARCHAR(MAX)")
    private String message;

    @Column(name = "Type", length = 50)
    private String type; // SUBMITTED, IN_PROGRESS, COMPLETE

    @Column(name = "IsRead")
    private Boolean isRead = false;

    @Column(name = "SentTime")
    private LocalDateTime sentTime;

    @ManyToOne
    @JoinColumn(name = "StudentID_FK")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "ConcernID_FK")
    private Concern concern;

    @PrePersist
    protected void onCreate() {
        this.sentTime = LocalDateTime.now();
        if (this.isRead == null) {
            this.isRead = false;
        }
    }
}
