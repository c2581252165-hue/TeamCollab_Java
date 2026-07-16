package com.teamcollab.entity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "project_members")
@Data
public class ProjectMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Boolean isApproved = false;
    private String role = "member";
    private Integer focusMinutes = 0;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    @ManyToOne
    @JoinColumn(name = "project_id")
    @JsonIgnoreProperties("members")
    private Project project;

    @Column(name = "user_id", insertable = false, updatable = false)
    private Integer userId;

    @Column(name = "project_id", insertable = false, updatable = false)
    private Integer projectId;

    @OneToMany(mappedBy = "projectMember", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProjectMemberBadge> badges;
}
