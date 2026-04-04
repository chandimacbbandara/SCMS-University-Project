package Project._6.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Concern_Meeting_Proposal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConcernMeetingProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProposalID")
    private Integer proposalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ConcernID_FK", nullable = false)
    private Concern concern;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdminID_FK", nullable = false)
    private Admin admin;

    @Column(name = "Proposal_Status", length = 60, nullable = false)
    private String proposalStatus;

    @Column(name = "Admin_Note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "Student_Response_Note", columnDefinition = "TEXT")
    private String studentResponseNote;

    @Column(name = "Created_Time", nullable = false)
    private LocalDateTime createdTime;

    @Column(name = "Responded_Time")
    private LocalDateTime respondedTime;

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startTime ASC")
    private List<ConcernMeetingSlot> slots = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdTime = LocalDateTime.now();
        if (this.proposalStatus == null || this.proposalStatus.isBlank()) {
            this.proposalStatus = "PENDING_STUDENT_SELECTION";
        }
    }
}
