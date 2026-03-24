package Project._6.demo.repository;

import Project._6.demo.entity.StudentCommunityPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentCommunityPostRepository extends JpaRepository<StudentCommunityPost, Integer> {
    List<StudentCommunityPost> findByStatusOrderByCreatedTimeDesc(String status);
    Optional<StudentCommunityPost> findByPostIdAndStudent_UserId(Integer postId, Integer userId);
}
