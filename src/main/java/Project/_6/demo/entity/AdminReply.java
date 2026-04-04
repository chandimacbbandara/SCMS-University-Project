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

    @Id
    @Column(name = "ReplyID")
    private Integer replyId;

    @Column(name = "Reply_Message", nullable = false, columnDefinition = "TEXT")
    private String replyMessage;

    @Column(name = "Resolution_Screenshot", length = 500)
    private String resolutionScreenshotPath;

    @Column(name = "Reply_Time")
    private LocalDateTime replyTime;

    @ManyToOne
    @JoinColumn(name = "AdminID_FK")
    private Admin admin;

    @ManyToOne
    @JoinColumn(name = "ConcernID_FK")
    private Concern concern;

    @PrePersist
    protected void onCreate() {
        this.replyTime = LocalDateTime.now();
    }
}
