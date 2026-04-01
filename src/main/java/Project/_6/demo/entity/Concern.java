package Project._6.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Concern")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Concern {

    @Id
    @Column(name = "ConcernID")
    private Integer concernId;

    @Column(name = "Subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "Message", nullable = false, columnDefinition = "VARCHAR(MAX)")
    private String message;

    @Column(name = "Evidence", length = 500)
    private String evidencePath;

    @Column(name = "Category", length = 100)
    private String category;

    @Column(name = "AI_Priority_Level", length = 20)
    private String aiPriorityLevel;

    @Column(name = "Status", length = 50)
    private String status;

    @Column(name = "CreatedTime")
    private LocalDateTime createdTime;

    @Column(name = "Meeting_Status", length = 60)
    private String meetingStatus;

    @Column(name = "Meeting_Booked_Start_Time")
    private LocalDateTime meetingBookedStartTime;

    @Column(name = "Meeting_Booked_End_Time")
    private LocalDateTime meetingBookedEndTime;

    @Column(name = "Meeting_Booked_At")
    private LocalDateTime meetingBookedAt;

    @ManyToOne
    @JoinColumn(name = "StudentID_FK")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "AdminID_FK")
    private Admin admin;

    @PrePersist
    protected void onCreate() {
        this.createdTime = LocalDateTime.now();
        if (this.status == null) {
            this.status = "Pending";
        }
        if (this.meetingStatus == null) {
            this.meetingStatus = "NONE";
        }
    }
}
