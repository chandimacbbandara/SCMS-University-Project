package Project._6.demo.repository;

import Project._6.demo.entity.AdminReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminReplyRepository extends JpaRepository<AdminReply, Integer> {
    List<AdminReply> findByConcern_ConcernIdOrderByReplyTimeDesc(Integer concernId);
    List<AdminReply> findByConcern_Category(String category);
    List<AdminReply> findByAdmin_UserId(Integer adminUserId);
    Optional<AdminReply> findFirstByConcern_ConcernIdOrderByReplyTimeDesc(Integer concernId);
    void deleteByConcern_ConcernId(Integer concernId);
}
