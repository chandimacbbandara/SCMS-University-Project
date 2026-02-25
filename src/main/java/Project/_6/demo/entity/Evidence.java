package Project._6.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.jspecify.annotations.Nullable;

@Entity
@Data
@Table(name = "evidence")
public class Evidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="evidence_id")
    private Long evidenceId;


    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_type")
    private String fileType;


    @Column(name = "file_path")
    private String filePath;

    @ManyToOne
    @JoinColumn(name="concern_id", nullable=false)
    private Concern concern;

    public void setFileName(@Nullable String originalFilename) {
    }

    public void setFileType(@Nullable String contentType) {
    }
}