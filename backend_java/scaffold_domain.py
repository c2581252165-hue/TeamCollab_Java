import os

base_dir = "d:/ruanjian/backend_java/src/main/java/com/teamcollab"

def write_file(path, content):
    os.makedirs(os.path.dirname(os.path.join(base_dir, path)), exist_ok=True)
    with open(os.path.join(base_dir, path), "w", encoding="utf-8") as f:
        f.write(content.strip() + "\n")

# Entities
write_file("entity/User.java", """
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

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<ProjectMember> projectMemberships;
}
""")

write_file("entity/Project.java", """
package com.teamcollab.entity;
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
    private List<ProjectMember> members;
}
""")

write_file("entity/ProjectMember.java", """
package com.teamcollab.entity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import javax.persistence.*;

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
    private User user;

    @ManyToOne
    @JoinColumn(name = "project_id")
    @JsonIgnoreProperties("members")
    private Project project;
}
""")

write_file("entity/Task.java", """
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
    private String commitLink;
    private Integer aiReviewScore;
    private String aiReviewComment;
    @Column(name = "project_id")
    private Integer projectId;
    @Column(name = "assigned_to_id")
    private Integer assignedToId;
    private Date createdAt = new Date();
    private Date deadline;
    private Boolean isDeleted = false;

    @ManyToOne
    @JoinColumn(name = "assigned_to_id", insertable = false, updatable = false)
    private User assignee;
}
""")

# Repositories
for entity in ["User", "Project", "ProjectMember", "Task"]:
    write_file(f"repository/{entity}Repository.java", f"""
package com.teamcollab.repository;
import com.teamcollab.entity.{entity};
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface {entity}Repository extends JpaRepository<{entity}, Integer> {{
}}
""")

write_file("repository/UserRepository.java", """
package com.teamcollab.repository;
import com.teamcollab.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    User findByUsername(String username);
}
""")

write_file("repository/ProjectMemberRepository.java", """
package com.teamcollab.repository;
import com.teamcollab.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Integer> {
    List<ProjectMember> findByUserId(Integer userId);
    ProjectMember findByUserIdAndProjectId(Integer userId, Integer projectId);
    List<ProjectMember> findByProjectIdAndIsApproved(Integer projectId, Boolean isApproved);
}
""")

write_file("repository/TaskRepository.java", """
package com.teamcollab.repository;
import com.teamcollab.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Integer> {
    List<Task> findByProjectIdAndIsDeletedFalse(Integer projectId);
    Integer countByProjectIdAndIsDeletedFalse(Integer projectId);
    Integer countByProjectIdAndStatusAndIsDeletedFalse(Integer projectId, String status);
    Integer countByProjectIdAndAssignedToIdAndStatusAndIsDeletedFalse(Integer projectId, Integer assignedToId, String status);
}
""")

# Security UserDetailsService
write_file("security/UserDetailsServiceImpl.java", """
package com.teamcollab.security;
import com.teamcollab.entity.User;
import com.teamcollab.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.ArrayList;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired private UserRepository userRepository;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user == null) throw new UsernameNotFoundException("User not found");
        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getHashedPassword(), new ArrayList<>());
    }
}
""")

# DTOs
write_file("dto/LoginDto.java", """
package com.teamcollab.dto;
import lombok.Data;
@Data
public class LoginDto {
    private String username;
    private String password;
}
""")

write_file("dto/RegisterDto.java", """
package com.teamcollab.dto;
import lombok.Data;
@Data
public class RegisterDto {
    private String username;
    private String password;
    private String role;
}
""")

print("Entities and Repositories scaffolded.")
