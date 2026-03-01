package Project._6.demo.repository;

import Project._6.demo.entity.AdminReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminReplyRepository extends JpaRepository<AdminReply, Integer> {
    List<AdminReply> findByConcern_ConcernIdOrderByReplyTimeDesc(Integer concernId);
    List<AdminReply> findByConcern_Category(String category);
}
