package com.teamcollab.entity;
import lombok.Data;
import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "standups")
@Data
public class Standup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "user_id")
    private Integer userId;
    @Column(name = "project_id")
    private Integer projectId;
    private Date date = new Date();
    private String yesterday;
    private String today;
    private String blockers;

    @ManyToOne
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
}
