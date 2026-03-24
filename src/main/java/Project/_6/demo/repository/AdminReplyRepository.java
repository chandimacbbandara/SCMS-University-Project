package Project._6.demo.repository;

import Project._6.demo.entity.AdminReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminReplyRepository extends JpaRepository<AdminReply, Integer> {
    @Query(value = "SELECT COALESCE(MAX(ReplyID), 0) + 1 FROM Admin_reply", nativeQuery = true)
    Integer getNextReplyId();

    List<AdminReply> findByConcern_ConcernIdOrderByReplyTimeDesc(Integer concernId);
    List<AdminReply> findByConcern_Category(String category);
    Optional<AdminReply> findFirstByConcern_ConcernIdOrderByReplyTimeDesc(Integer concernId);
    void deleteByConcern_ConcernId(Integer concernId);
}
