package Project._6.demo.repository;

import Project._6.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRegistrationStatus(String registrationStatus);

    @Query(value = "SELECT COALESCE(MAX(UserID), 0) + 1 FROM [User]", nativeQuery = true)
    Integer getNextUserId();
}
