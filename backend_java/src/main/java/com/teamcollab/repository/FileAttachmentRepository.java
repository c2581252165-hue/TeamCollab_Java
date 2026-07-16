package com.teamcollab.repository;
import com.teamcollab.entity.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Integer> {
    List<FileAttachment> findByTaskId(Integer taskId);
    Optional<FileAttachment> findFirstByTaskIdAndFilenameOrderByVersionDesc(Integer taskId, String filename);
}
