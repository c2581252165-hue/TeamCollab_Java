package com.teamcollab.entity;

import lombok.Data;
import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "file_attachments")
@Data
public class FileAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "task_id")
    private Integer taskId;

    @Column(name = "uploader_id")
    private Integer uploaderId;

    private String filename;
    
    @Column(name = "file_path")
    private String filePath;
    
    private Integer version = 1;
    
    @Column(name = "uploaded_at")
    private Date uploadedAt = new Date();

    @ManyToOne
    @JoinColumn(name = "task_id", insertable = false, updatable = false)
    private Task task;

    @ManyToOne
    @JoinColumn(name = "uploader_id", insertable = false, updatable = false)
    private User uploader;
}
