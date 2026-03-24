package Project._6.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {

    @Id
    @Column(name = "FeedbackID")
    private Integer feedbackId;

    @Column(name = "Rating", nullable = false)
    private Integer rating; // 1-5

    @Column(name = "Comments", columnDefinition = "VARCHAR(MAX)")
    private String comments;

    @Column(name = "SubmissionTime")
    private LocalDateTime submissionTime;

    @ManyToOne
    @JoinColumn(name = "ConcernID_FK")
    private Concern concern;

    @PrePersist
    protected void onCreate() {
        this.submissionTime = LocalDateTime.now();
    }
}
