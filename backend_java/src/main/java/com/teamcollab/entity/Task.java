package com.teamcollab.entity;
import lombok.Data;
import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "tasks")
@Data
public class Task {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String title;
    private String description;
    private String status = "todo";
    private String phase = "none";
    private Integer weight = 1;
    private String tags;
    @Column(name = "commit_link")
    @com.fasterxml.jackson.annotation.JsonProperty("commit_link")
    private String commitLink;
    private Integer aiReviewScore;
    private String aiReviewComment;
    @Column(name = "project_id")
    private Integer projectId;
    @Column(name = "assigned_to_id")
    private Integer assignedToId;
    private String priority = "medium";
    private Date createdAt = new Date();
    private Date deadline;
    private Boolean isDeleted = false;

    @ManyToOne
    @JoinColumn(name = "assigned_to_id", insertable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonProperty("assigned_to")
    private User assignedTo;
}
