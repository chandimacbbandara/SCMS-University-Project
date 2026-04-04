package Project._6.demo.repository;

import Project._6.demo.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    @Query(value = "SELECT COALESCE(MAX(UserID), 0) + 1 FROM Users", nativeQuery = true)
    Integer getNextUserId();

    @Query(value = "SELECT CASE WHEN EXTRA LIKE '%auto_increment%' THEN 1 ELSE 0 END " +
            "FROM information_schema.columns " +
            "WHERE table_schema = DATABASE() AND table_name = 'Users' AND column_name = 'UserID' LIMIT 1", nativeQuery = true)
    Integer isUserIdIdentity();

    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findByRegistrationStatus(String registrationStatus);
}
