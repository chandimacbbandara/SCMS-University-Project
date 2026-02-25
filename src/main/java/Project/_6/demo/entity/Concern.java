package Project._6.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "Concern")
@Data
public class Concern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="ConcernID")
    private Integer concernId;

    @Column(name = "Subject", nullable = false)
    private String subject;

    @Lob
    @Column(name = "Message", nullable = false)
    private String message;

    @Lob
    @Column(name = "Evidence")
    private byte[] evidence;

    @Column(name = "AI_Priority_Level", length = 20)
    private String aiPriorityLevel;

    @Column(name = "Status", length = 50)
    private String status;

    @Column(name = "CreatedTime", insertable = false, updatable = false, columnDefinition = "DATETIME DEFAULT GETDATE()")
    private LocalDateTime createdTime;

    @ManyToOne
    @JoinColumn(name = "StudentID_FK")
    private Student student;

    @Column(name = "AdminID_FK")
    private Integer adminId;
}
