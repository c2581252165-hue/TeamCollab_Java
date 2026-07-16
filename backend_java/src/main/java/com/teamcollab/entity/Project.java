package com.teamcollab.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "projects")
@Data
public class Project {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    private String description;
    private Date createdAt = new Date();
    private String externalDocUrl;
    @Column(unique = true)
    private String inviteCode;

    @OneToMany(mappedBy = "project")
    @JsonIgnore
    private List<ProjectMember> members;
}
