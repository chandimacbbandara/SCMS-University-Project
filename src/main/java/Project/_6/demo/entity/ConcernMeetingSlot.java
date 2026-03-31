package Project._6.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Concern_Meeting_Slot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConcernMeetingSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SlotID")
    private Integer slotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProposalID_FK", nullable = false)
    private ConcernMeetingProposal proposal;

    @Column(name = "Start_Time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "End_Time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "Slot_Status", length = 40, nullable = false)
    private String slotStatus;

    @PrePersist
    protected void onCreate() {
        if (this.slotStatus == null || this.slotStatus.isBlank()) {
            this.slotStatus = "AVAILABLE";
        }
    }
}
