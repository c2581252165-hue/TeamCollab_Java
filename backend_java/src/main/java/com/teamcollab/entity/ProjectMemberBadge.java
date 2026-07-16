package com.teamcollab.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "project_member_badges")
@Data
public class ProjectMemberBadge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "project_member_id")
    private Integer projectMemberId;

    @Column(name = "badge_type")
    private String badgeType;

    @Column(name = "earned_at")
    private Date earnedAt = new Date();

    @ManyToOne
    @JoinColumn(name = "project_member_id", insertable = false, updatable = false)
    @JsonIgnoreProperties("badges")
    private ProjectMember projectMember;
}
