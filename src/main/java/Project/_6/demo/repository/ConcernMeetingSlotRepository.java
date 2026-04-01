package Project._6.demo.repository;

import Project._6.demo.entity.ConcernMeetingSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConcernMeetingSlotRepository extends JpaRepository<ConcernMeetingSlot, Integer> {

    List<ConcernMeetingSlot> findByProposal_ProposalIdOrderByStartTimeAsc(Integer proposalId);

    Optional<ConcernMeetingSlot> findBySlotIdAndProposal_ProposalId(Integer slotId, Integer proposalId);

    List<ConcernMeetingSlot> findByProposal_ProposalIdInOrderByStartTimeAsc(List<Integer> proposalIds);
}
