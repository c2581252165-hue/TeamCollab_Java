package com.teamcollab.entity;
import lombok.Data;
import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "notifications")
@Data
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "user_id")
    private Integer userId;
    private String message;
    private String type;
    private Boolean isRead = false;
    private Date createdAt = new Date();
}
