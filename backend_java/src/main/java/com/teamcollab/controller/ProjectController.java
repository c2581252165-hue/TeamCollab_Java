package com.teamcollab.controller;

import com.teamcollab.entity.*;
import com.teamcollab.repository.*;
import com.teamcollab.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectMemberRepository projectMemberRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private LogRepository logRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username);
    }

    private ProjectMember getMembership(Integer projectId, Integer userId, boolean requireApproved) {
        ProjectMember m = projectMemberRepository.findByUserIdAndProjectId(userId, projectId);
        if (m == null || (requireApproved && !m.getIsApproved())) return null;
        return m;
    }

    private void requireLeaderOrTeacher(ProjectMember mem, User user) {
        if ((mem == null || !"leader".equals(mem.getRole())) && !"teacher".equals(user.getRole())) {
            throw new RuntimeException("Not authorized");
        }
    }

    @GetMapping
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    @GetMapping("/my")
    public List<Map<String, Object>> getMyProjects() {
        User user = getCurrentUser();
        List<ProjectMember> memberships = projectMemberRepository.findByUserId(user.getId());
        List<Map<String, Object>> res = new ArrayList<>();
        for (ProjectMember m : memberships) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("user_id", m.getUser().getId());
            map.put("project_id", m.getProject().getId());
            map.put("is_approved", m.getIsApproved());
            map.put("role", m.getRole());
            map.put("focus_minutes", m.getFocusMinutes());
            map.put("project", m.getProject());
            res.add(map);
        }
        return res;
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
    public Map<String, String> joinProject(@RequestBody JoinProjectRequest req) {
        Project proj = projectRepository.findByInviteCode(req.getInviteCode());
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

        return Collections.singletonMap("message", "Application sent to leader for approval.");
    }

    @PostMapping("/switch")
    public Map<String, String> switchProject(@RequestBody SwitchProjectRequest req) {
        User current = getCurrentUser();
        ProjectMember mem = getMembership(req.getProjectId(), current.getId(), true);
        if (mem == null) throw new RuntimeException("You are not an approved member of this project");

        current.setCurrentProjectId(req.getProjectId());
        userRepository.save(current);
        return Collections.singletonMap("message", "Switched to project " + req.getProjectId());
    }

    @GetMapping("/{id}/dashboard")
    public Map<String, Object> getDashboardStats(@PathVariable Integer id) {
        int totalTasks = taskRepository.countByProjectIdAndIsDeletedFalse(id);
        int doneTasks = taskRepository.countByProjectIdAndStatusAndIsDeletedFalse(id, "done");
        
        List<ProjectMember> members = projectMemberRepository.findByProjectIdAndIsApproved(id, true);
        List<Map<String, Object>> workload = new ArrayList<>();
        
        for (ProjectMember m : members) {
            int inProgress = taskRepository.countByProjectIdAndAssignedToIdAndStatusAndIsDeletedFalse(id, m.getUser().getId(), "in_progress");
            List<String> badges = (m.getBadges() != null) ? m.getBadges().stream().map(ProjectMemberBadge::getBadgeType).collect(Collectors.toList()) : new ArrayList<>();
            
            Map<String, Object> w = new HashMap<>();
            w.put("id", m.getUser().getId());
            w.put("username", m.getUser().getUsername());
            w.put("in_progress", inProgress);
            w.put("points", m.getUser().getPoints());
            w.put("role", "teacher".equals(m.getUser().getRole()) ? "teacher" : m.getRole());
            w.put("badges", badges);
            workload.add(w);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("total_tasks", totalTasks);
        result.put("done_tasks", doneTasks);
        result.put("workload", workload);
        return result;
    }

    @GetMapping("/{id}/applications")
    public List<ProjectMember> getApplications(@PathVariable Integer id) {
        User user = getCurrentUser();
        ProjectMember mem = getMembership(id, user.getId(), true);
        requireLeaderOrTeacher(mem, user);
        return projectMemberRepository.findByProjectIdAndIsApproved(id, false);
    }

    @GetMapping("/{project_id}/members")
    public List<Map<String, Object>> getMembers(@PathVariable("project_id") Integer projectId) {
        if (getMembership(projectId, getCurrentUser().getId(), true) == null) throw new RuntimeException("Not authorized");
        List<ProjectMember> memberships = projectMemberRepository.findByProjectIdAndIsApproved(projectId, true);
        List<Map<String, Object>> res = new ArrayList<>();
        for (ProjectMember m : memberships) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("user_id", m.getUser().getId());
            map.put("project_id", m.getProject().getId());
            map.put("is_approved", m.getIsApproved());
            map.put("role", m.getRole());
            map.put("focus_minutes", m.getFocusMinutes());
            map.put("user", m.getUser());
            map.put("project", m.getProject());
            map.put("badges", m.getBadges());
            res.add(map);
        }
        return res;
    }

    @PostMapping("/{id}/approve/{user_id}")
    public Map<String, String> approveMember(@PathVariable Integer id, @PathVariable("user_id") Integer userId) {
        User current = getCurrentUser();
        requireLeaderOrTeacher(getMembership(id, current.getId(), true), current);
        
        ProjectMember app = projectMemberRepository.findByUserIdAndProjectId(userId, id);
        if (app == null || app.getIsApproved()) throw new RuntimeException("Application not found");
        app.setIsApproved(true);
        projectMemberRepository.save(app);
        return Collections.singletonMap("message", "Member approved");
    }

    @PostMapping("/{id}/reject/{user_id}")
    public Map<String, String> rejectMember(@PathVariable Integer id, @PathVariable("user_id") Integer userId) {
        User current = getCurrentUser();
        requireLeaderOrTeacher(getMembership(id, current.getId(), true), current);
        
        ProjectMember app = projectMemberRepository.findByUserIdAndProjectId(userId, id);
        if (app == null || app.getIsApproved()) throw new RuntimeException("Application not found");
        projectMemberRepository.delete(app);
        return Collections.singletonMap("message", "Member rejected");
    }

    @PostMapping("/{id}/template")
    public Map<String, String> applyTemplate(@PathVariable Integer id) {
        User current = getCurrentUser();
        requireLeaderOrTeacher(getMembership(id, current.getId(), true), current);
        
        String[][] templates = {
            {"需求分析与规格说明", "完成项目的需求调研并输出需求规格说明书。", "requirement", "5"},
            {"系统架构与数据库设计", "完成系统架构设计图与数据库ER图，并输出设计文档。", "design", "4"},
            {"核心功能编码实现", "完成前后端核心业务逻辑开发与联调。", "coding", "8"},
            {"系统测试与验收", "完成单元测试、集成测试与系统验收，输出测试报告。", "test", "3"}
        };
        
        for (String[] t : templates) {
            Task task = new Task();
            task.setProjectId(id);
            task.setTitle(t[0]);
            task.setDescription(t[1]);
            task.setPhase(t[2]);
            task.setWeight(Integer.parseInt(t[3]));
            taskRepository.save(task);
        }
        return Collections.singletonMap("message", "Template applied successfully");
    }

    @PutMapping("/{id}/doc")
    public Map<String, String> updateDocUrl(@PathVariable Integer id, @RequestBody ProjectDocUpdate req) {
        if (getMembership(id, getCurrentUser().getId(), true) == null) throw new RuntimeException("Not authorized");
        Project proj = projectRepository.findById(id).orElseThrow(() -> new RuntimeException("Project not found"));
        proj.setExternalDocUrl(req.getUrl());
        projectRepository.save(proj);
        return Collections.singletonMap("message", "Document URL updated successfully");
    }

    @GetMapping("/{id}/logs")
    public List<Map<String, Object>> getProjectLogs(@PathVariable Integer id) {
        if (getMembership(id, getCurrentUser().getId(), true) == null) throw new RuntimeException("Not authorized");
        List<Log> logs = logRepository.findByProjectIdOrderByTimestampDesc(id);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Log log : logs) {
            Map<String, Object> l = new HashMap<>();
            l.put("id", log.getId());
            l.put("action", log.getAction());
            l.put("username", log.getUser() != null ? log.getUser().getUsername() : "系统");
            l.put("timestamp", log.getTimestamp().toString());
            l.put("details", log.getDetails());
            result.add(l);
        }
        return result;
    }

    @PutMapping("/{id}/members/{user_id}/points")
    public Map<String, Object> updateMemberPoints(@PathVariable Integer id, @PathVariable("user_id") Integer userId, @RequestBody PointUpdate req) {
        User current = getCurrentUser();
        ProjectMember currentMem = getMembership(id, current.getId(), true);
        if (currentMem == null || !"leader".equals(currentMem.getRole())) throw new RuntimeException("Not authorized, must be leader");
        
        ProjectMember targetMem = getMembership(id, userId, true);
        if (targetMem == null) throw new RuntimeException("Member not found in project");
        
        User targetUser = targetMem.getUser();
        targetUser.setPoints(targetUser.getPoints() + req.getAmount());
        userRepository.save(targetUser);
        
        String actionStr = req.getAmount() > 0 ? "为 " + targetUser.getUsername() + " 增加了 " + req.getAmount() + " 积分" : "扣除了 " + targetUser.getUsername() + " " + (-req.getAmount()) + " 积分";
        
        Log log = new Log();
        log.setAction("积分变更");
        log.setDetails(actionStr + "。原因: " + req.getReason());
        log.setUserId(current.getId());
        log.setProjectId(id);
        logRepository.save(log);
        
        Map<String, Object> res = new HashMap<>();
        res.put("message", "Points updated successfully");
        res.put("new_points", targetUser.getPoints());
        return res;
    }

    @DeleteMapping("/{id}/members/{user_id}")
    public Map<String, String> kickMember(@PathVariable Integer id, @PathVariable("user_id") Integer userId) {
        User current = getCurrentUser();
        ProjectMember currentMem = getMembership(id, current.getId(), true);
        if (currentMem == null) throw new RuntimeException("Not authorized, must be a project member");
        
        boolean isCurrentTeacher = "teacher".equals(current.getRole());
        boolean isCurrentLeader = "leader".equals(currentMem.getRole());
        
        if (!isCurrentTeacher && !isCurrentLeader) throw new RuntimeException("Must be a leader or teacher to kick members");
        if (userId.equals(current.getId())) throw new RuntimeException("Cannot kick yourself");
        
        ProjectMember targetMem = getMembership(id, userId, true);
        if (targetMem == null) throw new RuntimeException("Member not found in project");
        
        User targetUser = targetMem.getUser();
        boolean isTargetTeacher = "teacher".equals(targetUser.getRole());
        boolean isTargetLeader = "leader".equals(targetMem.getRole());

        if (isCurrentTeacher && !isCurrentLeader) {
            if (isTargetLeader) throw new RuntimeException("指导教师不能剔除该项目的学生组长");
            if (isTargetTeacher) throw new RuntimeException("指导教师不能互相剔除");
        } else if (!isCurrentTeacher && isCurrentLeader) {
            if (isTargetTeacher) throw new RuntimeException("学生组长不能剔除指导教师");
        }
        
        projectMemberRepository.delete(targetMem);
        
        Log log = new Log();
        log.setAction("剔除成员");
        log.setDetails("将 " + targetUser.getUsername() + " 移出了项目");
        log.setUserId(current.getId());
        log.setProjectId(id);
        logRepository.save(log);
        
        return Collections.singletonMap("message", "Member kicked successfully");
    }

    @PostMapping("/{id}/focus")
    public Map<String, Object> addFocusMinutes(@PathVariable Integer id, @RequestBody Map<String, Integer> req) {
        User current = getCurrentUser();
        ProjectMember mem = getMembership(id, current.getId(), true);
        if (mem == null) throw new RuntimeException("Not authorized");
        
        Integer minutes = req.get("minutes");
        int earnedPoints = 0;
        
        if (minutes != null && minutes > 0) {
            mem.setFocusMinutes((mem.getFocusMinutes() == null ? 0 : mem.getFocusMinutes()) + minutes);
            projectMemberRepository.save(mem);
            
            earnedPoints = minutes / 5;
            if (earnedPoints > 0) {
                current.setPoints((current.getPoints() == null ? 0 : current.getPoints()) + earnedPoints);
                userRepository.save(current);
                
                Log log = new Log();
                log.setAction("专注模式");
                log.setDetails("完成了 " + minutes + " 分钟的专注任务，获得 " + earnedPoints + " 积分");
                log.setUserId(current.getId());
                log.setProjectId(id);
                logRepository.save(log);
            }
        }
        
        Map<String, Object> res = new HashMap<>();
        res.put("message", "Focus minutes added successfully");
        res.put("earned_points", earnedPoints);
        return res;
    }
}
