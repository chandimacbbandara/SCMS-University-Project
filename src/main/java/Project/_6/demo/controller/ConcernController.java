package Project._6.demo.controller;

import Project._6.demo.entity.Concern;
import Project._6.demo.service.ConcernService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/concerns")
public class ConcernController {

    @Autowired
    private ConcernService concernService;

    // ✅ Submit concern (with optional file upload)
    @PostMapping(value = "/submit", consumes = "multipart/form-data")
    public ResponseEntity<?> submitConcern(
            @RequestParam("studentId") Long studentId,
            @RequestParam String subject,
            @RequestParam String message,
            @RequestParam(required = false) String aiPriorityLevel,
            @RequestParam(required = false) MultipartFile file) {

        try {
            Concern concern = new Concern();
            concern.setSubject(subject);
            concern.setMessage(message);
            concern.setAiPriorityLevel(aiPriorityLevel);

            Concern saved = concernService.submitConcern(studentId, concern, file);
            return ResponseEntity.ok(saved);

        } catch (IOException e) {
            return ResponseEntity.badRequest().body("File upload failed: " + e.getMessage());
        }
    }

    // ✅ Get concerns of a student
    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentConcerns(@PathVariable Long studentId) {
        return ResponseEntity.ok(concernService.getStudentConcerns(studentId));
    }

    // ✅ Withdraw a concern
    @PutMapping("/withdraw/{concernId}")
    public ResponseEntity<?> withdrawConcern(@PathVariable Integer concernId) {
        return ResponseEntity.ok(concernService.withdrawConcern(concernId));
    }

    // ✅ Quick test endpoint
    @GetMapping("/test")
    public String test() {
        return "Concern Controller Working!";
    }
}