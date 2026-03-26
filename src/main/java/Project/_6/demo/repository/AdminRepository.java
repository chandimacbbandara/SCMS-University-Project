package Project._6.demo.repository;

import Project._6.demo.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Integer> {
    Optional<Admin> findByStaffId(String staffId);
    Optional<Admin> findByStaffIdIgnoreCase(String staffId);
    Optional<Admin> findByUser_EmailIgnoreCase(String email);
    boolean existsByStaffIdIgnoreCase(String staffId);
    boolean existsByUser_UserId(Integer userId);
}
