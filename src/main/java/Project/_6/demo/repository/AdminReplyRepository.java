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

        @Query(value = "SELECT CASE WHEN EXTRA LIKE '%auto_increment%' THEN 1 ELSE 0 END " +
            "FROM information_schema.columns " +
            "WHERE table_schema = DATABASE() AND table_name = 'Admin_reply' AND column_name = 'ReplyID' LIMIT 1", nativeQuery = true)
    Integer isReplyIdIdentity();

    List<AdminReply> findByConcern_ConcernIdOrderByReplyTimeDesc(Integer concernId);
    List<AdminReply> findByConcern_ConcernIdOrderByReplyTimeAsc(Integer concernId);
    List<AdminReply> findByConcern_ConcernIdIn(List<Integer> concernIds);
    List<AdminReply> findByConcern_Category(String category);
    List<AdminReply> findByAdmin_UserId(Integer adminUserId);
    List<AdminReply> findByAdmin_UserIdAndConcern_Category(Integer adminUserId, String category);
    Optional<AdminReply> findFirstByConcern_ConcernIdOrderByReplyTimeDesc(Integer concernId);
    void deleteByConcern_ConcernId(Integer concernId);
}
