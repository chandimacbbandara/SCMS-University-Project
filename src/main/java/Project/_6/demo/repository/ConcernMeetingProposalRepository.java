package Project._6.demo.repository;

import Project._6.demo.entity.ConcernMeetingProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConcernMeetingProposalRepository extends JpaRepository<ConcernMeetingProposal, Integer> {

    List<ConcernMeetingProposal> findByConcern_ConcernIdOrderByCreatedTimeDesc(Integer concernId);

    Optional<ConcernMeetingProposal> findFirstByConcern_ConcernIdOrderByCreatedTimeDesc(Integer concernId);

    Optional<ConcernMeetingProposal> findByProposalIdAndConcern_ConcernId(Integer proposalId, Integer concernId);

    List<ConcernMeetingProposal> findByConcern_ConcernIdInOrderByCreatedTimeDesc(List<Integer> concernIds);

    void deleteByConcern_ConcernId(Integer concernId);
}
