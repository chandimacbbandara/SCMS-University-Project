package Project._6.demo.repository;

import Project._6.demo.entity.StudentCommunityReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentCommunityReplyRepository extends JpaRepository<StudentCommunityReply, Integer> {
    List<StudentCommunityReply> findByPost_PostIdAndStatusOrderByCreatedTimeAsc(Integer postId, String status);
    Optional<StudentCommunityReply> findByReplyIdAndStudent_UserId(Integer replyId, Integer userId);
}
