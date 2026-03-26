package Project._6.demo;

import Project._6.demo.entity.Concern;
import Project._6.demo.entity.AdminReply;
import Project._6.demo.repository.AdminReplyRepository;
import Project._6.demo.entity.User;
import Project._6.demo.repository.AdminRepository;
import Project._6.demo.repository.ConcernRepository;
import Project._6.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Optional;

@SpringBootApplication
public class StudentConcernManagementSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentConcernManagementSystemApplication.class, args);
    }

    @Bean
    public CommandLineRunner removePredefinedAdmin(UserRepository userRepository,
                                                   AdminRepository adminRepository,
                                                   AdminReplyRepository adminReplyRepository,
                                                   ConcernRepository concernRepository) {
        return args -> {
            Optional<User> predefinedAdminUser = userRepository.findByEmailIgnoreCase("admin@akb.edu");
            if (predefinedAdminUser.isEmpty()) {
                return;
            }

            User user = predefinedAdminUser.get();
            Integer userId = user.getUserId();

            List<Concern> assignedConcerns = concernRepository.findByAdmin_UserId(userId);
            if (!assignedConcerns.isEmpty()) {
                for (Concern concern : assignedConcerns) {
                    concern.setAdmin(null);
                }
                concernRepository.saveAll(assignedConcerns);
            }

            List<AdminReply> adminReplies = adminReplyRepository.findByAdmin_UserId(userId);
            if (!adminReplies.isEmpty()) {
                for (AdminReply reply : adminReplies) {
                    reply.setAdmin(null);
                }
                adminReplyRepository.saveAll(adminReplies);
            }

            if (adminRepository.existsByUser_UserId(userId)) {
                adminRepository.deleteById(userId);
            }
            userRepository.deleteById(userId);
        };
    }
}
