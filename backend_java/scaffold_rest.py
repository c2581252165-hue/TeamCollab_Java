import os

base_dir = "d:/ruanjian/backend_java/src/main/java/com/teamcollab"

def write_file(path, content):
    os.makedirs(os.path.dirname(os.path.join(base_dir, path)), exist_ok=True)
    with open(os.path.join(base_dir, path), "w", encoding="utf-8") as f:
        f.write(content.strip() + "\n")

# Entities
write_file("entity/Standup.java", """
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
""")

write_file("entity/Notification.java", """
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
""")

# Repositories
write_file("repository/StandupRepository.java", """
package com.teamcollab.repository;
import com.teamcollab.entity.Standup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StandupRepository extends JpaRepository<Standup, Integer> {
    List<Standup> findByProjectId(Integer projectId);
}
""")

write_file("repository/NotificationRepository.java", """
package com.teamcollab.repository;
import com.teamcollab.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Integer userId);
}
""")

# Controllers
write_file("controller/StandupController.java", """
package com.teamcollab.controller;
import com.teamcollab.entity.Standup;
import com.teamcollab.entity.User;
import com.teamcollab.repository.StandupRepository;
import com.teamcollab.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/standups")
public class StandupController {
    @Autowired private StandupRepository standupRepository;
    @Autowired private UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username);
    }

    @GetMapping
    public List<Standup> getStandups(@RequestParam Integer project_id) {
        return standupRepository.findByProjectId(project_id);
    }

    @PostMapping
    public Standup createStandup(@RequestBody Standup standup) {
        User current = getCurrentUser();
        standup.setUserId(current.getId());
        return standupRepository.save(standup);
    }
}
""")

write_file("controller/NotificationController.java", """
package com.teamcollab.controller;
import com.teamcollab.entity.Notification;
import com.teamcollab.entity.User;
import com.teamcollab.repository.NotificationRepository;
import com.teamcollab.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username);
    }

    @GetMapping
    public List<Notification> getNotifications() {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(getCurrentUser().getId());
    }

    @PostMapping("/{id}/read")
    public Notification markAsRead(@PathVariable Integer id) {
        Notification notification = notificationRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }
}
""")

print("Standup and Notification scaffolded.")
