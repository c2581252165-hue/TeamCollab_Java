import os

base_dir = "d:/ruanjian/backend_java/src/main/java/com/teamcollab"

def write_file(path, content):
    os.makedirs(os.path.dirname(os.path.join(base_dir, path)), exist_ok=True)
    with open(os.path.join(base_dir, path), "w", encoding="utf-8") as f:
        f.write(content.strip() + "\n")

write_file("controller/ProjectController.java", """
package com.teamcollab.controller;

import com.teamcollab.entity.Project;
import com.teamcollab.entity.ProjectMember;
import com.teamcollab.entity.User;
import com.teamcollab.repository.ProjectMemberRepository;
import com.teamcollab.repository.ProjectRepository;
import com.teamcollab.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectMemberRepository projectMemberRepository;
    @Autowired private UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username);
    }

    @GetMapping
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    @GetMapping("/my")
    public List<ProjectMember> getMyProjects() {
        return projectMemberRepository.findByUserId(getCurrentUser().getId());
    }

    @PostMapping
    public Project createProject(@RequestBody Project project) {
        User currentUser = getCurrentUser();
        project.setInviteCode(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        projectRepository.save(project);

        ProjectMember member = new ProjectMember();
        member.setUser(currentUser);
        member.setProject(project);
        member.setIsApproved(true);
        member.setRole("leader");
        projectMemberRepository.save(member);

        currentUser.setCurrentProjectId(project.getId());
        userRepository.save(currentUser);

        return project;
    }

    @PostMapping("/join")
    public Map<String, String> joinProject(@RequestBody Map<String, String> payload) {
        String inviteCode = payload.get("invite_code");
        Project proj = projectRepository.findByInviteCode(inviteCode);
        if (proj == null) throw new RuntimeException("Invalid invite code");
        
        User current = getCurrentUser();
        if (projectMemberRepository.findByUserIdAndProjectId(current.getId(), proj.getId()) != null) {
            throw new RuntimeException("Already applied or joined");
        }

        ProjectMember member = new ProjectMember();
        member.setUser(current);
        member.setProject(proj);
        member.setIsApproved(false);
        projectMemberRepository.save(member);

        return Map.of("message", "Application sent to leader for approval.");
    }

    @PostMapping("/switch")
    public Map<String, String> switchProject(@RequestBody Map<String, Integer> payload) {
        Integer projectId = payload.get("project_id");
        User current = getCurrentUser();
        ProjectMember mem = projectMemberRepository.findByUserIdAndProjectId(current.getId(), projectId);
        if (mem == null || !mem.getIsApproved()) throw new RuntimeException("Not approved");

        current.setCurrentProjectId(projectId);
        userRepository.save(current);
        return Map.of("message", "Switched to project " + projectId);
    }

    @GetMapping("/{id}/members")
    public List<ProjectMember> getMembers(@PathVariable Integer id) {
        return projectMemberRepository.findByProjectIdAndIsApproved(id, true);
    }
}
""")

write_file("controller/TaskController.java", """
package com.teamcollab.controller;

import com.teamcollab.entity.ProjectMember;
import com.teamcollab.entity.Task;
import com.teamcollab.entity.User;
import com.teamcollab.repository.ProjectMemberRepository;
import com.teamcollab.repository.TaskRepository;
import com.teamcollab.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired private TaskRepository taskRepository;
    @Autowired private ProjectMemberRepository projectMemberRepository;
    @Autowired private UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username);
    }

    @GetMapping
    public List<Task> getTasks(@RequestParam(required = false) Integer project_id) {
        if (project_id != null) {
            return taskRepository.findByProjectIdAndIsDeletedFalse(project_id);
        }
        return taskRepository.findAll();
    }

    @PostMapping
    public Task createTask(@RequestBody Task task) {
        User current = getCurrentUser();
        ProjectMember mem = projectMemberRepository.findByUserIdAndProjectId(current.getId(), task.getProjectId());
        if (mem == null || (!"leader".equals(mem.getRole()) && !"teacher".equals(current.getRole()))) {
            throw new RuntimeException("Only Leader or Teacher can create tasks.");
        }
        return taskRepository.save(task);
    }

    @PutMapping("/{id}")
    public Task updateTask(@PathVariable Integer id, @RequestBody Map<String, Object> updates) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        User current = getCurrentUser();

        if (updates.containsKey("status")) {
            String newStatus = updates.get("status").toString();
            if ("done".equals(newStatus) && !"done".equals(task.getStatus())) {
                ProjectMember mem = projectMemberRepository.findByUserIdAndProjectId(current.getId(), task.getProjectId());
                if (mem == null || (!"leader".equals(mem.getRole()) && !"teacher".equals(current.getRole()))) {
                    throw new RuntimeException("Only Leader or Teacher can approve a task and mark it as Done.");
                }
                // Add points
                if (task.getAssignedToId() != null) {
                    User assignee = userRepository.findById(task.getAssignedToId()).orElse(null);
                    if (assignee != null) {
                        assignee.setPoints(assignee.getPoints() + task.getWeight());
                        userRepository.save(assignee);
                    }
                }
            }
            task.setStatus(newStatus);
        }

        if (updates.containsKey("title")) task.setTitle(updates.get("title").toString());
        if (updates.containsKey("description")) task.setDescription(updates.get("description").toString());
        if (updates.containsKey("phase")) task.setPhase(updates.get("phase").toString());
        if (updates.containsKey("weight")) task.setWeight(Integer.parseInt(updates.get("weight").toString()));

        return taskRepository.save(task);
    }
}
""")

write_file("repository/ProjectRepository.java", """
package com.teamcollab.repository;
import com.teamcollab.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Integer> {
    Project findByInviteCode(String inviteCode);
}
""")

print("Controllers scaffolded.")
