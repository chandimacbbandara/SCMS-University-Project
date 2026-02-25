package Project._6.demo.service;

import Project._6.demo.entity.Concern;
import Project._6.demo.entity.Evidence;
import Project._6.demo.entity.Student;
import Project._6.demo.repository.ConcernRepository;
import Project._6.demo.repository.EvidenceRepository;
import Project._6.demo.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ConcernService {

    @Autowired
    private ConcernRepository concernRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private FileStorageService fileStorageService;

    // Submit concern WITH optional file
    public Concern submitConcern(Long studentId, Concern concern, MultipartFile file) throws IOException {

        if (studentId == null) {
            throw new IllegalArgumentException("studentId cannot be null");
        }
        if (concern == null) {
            throw new IllegalArgumentException("concern cannot be null");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));

        concern.setStudent(student);

        // Set default status if not already set
        if (concern.getStatus() == null || concern.getStatus().isBlank()) {
            concern.setStatus("Pending");
        }

        // Save evidence as byte array in Concern table if file exists
        if (file != null && !file.isEmpty()) {
            concern.setEvidence(file.getBytes());
        }

        return concernRepository.save(concern);
    }

    public List<Concern> getStudentConcerns(Long studentId) {
        return concernRepository.findByStudentStudentId(studentId);
    }

    public Concern withdrawConcern(Integer concernId) {
        Concern concern = concernRepository.findById(concernId)
                .orElseThrow(() -> new RuntimeException("Concern not found with id: " + concernId));

        concern.setStatus("Withdrawn");
        return concernRepository.save(concern);
    }


    // Submit concern WITHOUT file (helper method)
    public Concern submitConcernWithoutFile(Long studentId, String subject, String message, String aiPriorityLevel) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));

        Concern concern = new Concern();
        concern.setStudent(student);
        concern.setSubject(subject);
        concern.setMessage(message);
        concern.setAiPriorityLevel(aiPriorityLevel);
        concern.setStatus("Pending");

        return concernRepository.save(concern);
    }
}