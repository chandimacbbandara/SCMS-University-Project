package Project._6.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Admin_reply")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminReply {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_STUDENT = "STUDENT";

    @Id
    @Column(name = "ReplyID")
    private Integer replyId;

    @Column(name = "Reply_Message", nullable = false, columnDefinition = "TEXT")
    private String replyMessage;

    @Column(name = "Resolution_Screenshot", length = 500)
    private String resolutionScreenshotPath;

    @Column(name = "Reply_Time")
    private LocalDateTime replyTime;

    @Column(name = "Sender_Role", length = 20)
    private String senderRole;

    @ManyToOne
    @JoinColumn(name = "AdminID_FK")
    private Admin admin;

    @ManyToOne
    @JoinColumn(name = "ConcernID_FK")
    private Concern concern;

    @PrePersist
    protected void onCreate() {
        this.replyTime = LocalDateTime.now();
        if (this.senderRole == null || this.senderRole.isBlank()) {
            this.senderRole = ROLE_ADMIN;
        }
    }
}
