package com.teamcollab.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(unique = true, nullable = false)
    private String username;
    @JsonIgnore
    private String hashedPassword;
    private String role = "member";
    private Integer points = 0;
    @Column(name = "current_project_id")
    private Integer currentProjectId;

    @ManyToOne
    @JoinColumn(name = "current_project_id", insertable = false, updatable = false)
    private Project currentProject;

    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER)
    private List<ProjectMember> projectMemberships;
}
