package com.teamcollab.controller;

import com.teamcollab.entity.*;
import com.teamcollab.repository.*;
import com.teamcollab.dto.FocusRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired private TaskRepository taskRepository;
    @Autowired private ProjectMemberRepository projectMemberRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private LogRepository logRepository;
    @Autowired private ProjectMemberBadgeRepository projectMemberBadgeRepository;
    @Autowired private FileAttachmentRepository fileAttachmentRepository;
    @Autowired private NotificationRepository notificationRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username);
    }

    private void createLog(Integer userId, Integer projectId, String action, String targetType, Integer targetId, String details) {
        Log log = new Log();
        log.setUserId(userId);
        log.setProjectId(projectId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetails(details);
        logRepository.save(log);
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

    private void requireTaskAssignee(Task task, User user) {
        if (task.getAssignedToId() == null || !task.getAssignedToId().equals(user.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Only the assignee can submit this task.");
        }
    }

    @GetMapping
    public List<Task> getTasks(@RequestParam(required = false) Integer project_id) {
        if (project_id != null) {
            return taskRepository.findByProjectIdAndIsDeletedFalse(project_id);
        }
        return taskRepository.findAll();
    }

    @GetMapping("/deleted")
    public List<Task> getDeletedTasks(@RequestParam(required = false) Integer project_id) {
        // Need to add custom query to repository for IsDeletedTrue if project_id is given. Let's filter in memory for simplicity if small, or better, stream it.
        List<Task> all = taskRepository.findAll();
        List<Task> result = new ArrayList<>();
        for (Task t : all) {
            if (t.getIsDeleted() != null && t.getIsDeleted() && (project_id == null || t.getProjectId().equals(project_id))) {
                result.add(t);
            }
        }
        return result;
    }

    @PostMapping
    public Task createTask(@RequestBody Task task) {
        User current = getCurrentUser();
        ProjectMember mem = getMembership(task.getProjectId(), current.getId(), true);
        requireLeaderOrTeacher(mem, current);
        
        task.setIsDeleted(false);
        taskRepository.save(task);
        createLog(current.getId(), task.getProjectId(), "create", "Task", task.getId(), "创建了任务《" + task.getTitle() + "》");
        return task;
    }

    @PutMapping("/{id}")
    public Task updateTask(@PathVariable Integer id, @RequestBody Map<String, Object> updates) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        User current = getCurrentUser();
        ProjectMember mem = getMembership(task.getProjectId(), current.getId(), true);

        if (updates.containsKey("status")) {
            String newStatus = updates.get("status").toString();
            if ("done".equals(newStatus) && !"done".equals(task.getStatus())) {
                requireLeaderOrTeacher(mem, current);
                createLog(current.getId(), task.getProjectId(), "complete", "Task", task.getId(), "将任务《" + task.getTitle() + "》标记为已完成");
                
                if (task.getAssignedToId() != null) {
                    User assignee = userRepository.findById(task.getAssignedToId()).orElse(null);
                    if (assignee != null) {
                        assignee.setPoints(assignee.getPoints() + task.getWeight());
                        userRepository.save(assignee);
                        createLog(current.getId(), task.getProjectId(), "points", "User", assignee.getId(), "为成员 " + assignee.getUsername() + " 发放了 " + task.getWeight() + " 积分");
                    }
                }
            } else if ("review".equals(newStatus) && !"review".equals(task.getStatus())) {
                requireTaskAssignee(task, current);
                createLog(current.getId(), task.getProjectId(), "update_status", "Task", task.getId(), "将任务《" + task.getTitle() + "》提交审核");
                if (mem != null) {
                    int currentHour = LocalTime.now().getHour();
                    if (currentHour >= 0 && currentHour <= 5) {
                        if (!projectMemberBadgeRepository.existsByProjectMemberIdAndBadgeType(mem.getId(), "night_owl")) {
                            ProjectMemberBadge b = new ProjectMemberBadge();
                            b.setProjectMemberId(mem.getId());
                            b.setBadgeType("night_owl");
                            projectMemberBadgeRepository.save(b);
                        }
                    }
                    if ("test".equals(task.getPhase())) {
                        if (!projectMemberBadgeRepository.existsByProjectMemberIdAndBadgeType(mem.getId(), "bug_hunter")) {
                            ProjectMemberBadge b = new ProjectMemberBadge();
                            b.setProjectMemberId(mem.getId());
                            b.setBadgeType("bug_hunter");
                            projectMemberBadgeRepository.save(b);
                        }
                    }
                }
            } else if ("in_progress".equals(newStatus) && "review".equals(task.getStatus())) {
                createLog(current.getId(), task.getProjectId(), "update_status", "Task", task.getId(), "驳回了任务《" + task.getTitle() + "》");
                if (task.getAssignedToId() != null) {
                    Notification n = new Notification();
                    n.setUserId(task.getAssignedToId());
                    n.setMessage("您的任务《" + task.getTitle() + "》已被组长驳回修改，请检查后重新提交。");
                    n.setType("task_rejected");
                    notificationRepository.save(n);
                }
            } else if (!newStatus.equals(task.getStatus())) {
                createLog(current.getId(), task.getProjectId(), "update_status", "Task", task.getId(), "将任务《" + task.getTitle() + "》状态变更为 " + newStatus);
            }
            task.setStatus(newStatus);
        } else if (!updates.isEmpty()) {
            createLog(current.getId(), task.getProjectId(), "edit", "Task", task.getId(), "编辑了任务《" + task.getTitle() + "》的信息");
        }

        if (updates.containsKey("title")) task.setTitle(updates.get("title").toString());
        if (updates.containsKey("description")) task.setDescription(updates.get("description").toString());
        if (updates.containsKey("phase")) task.setPhase(updates.get("phase").toString());
        if (updates.containsKey("weight")) task.setWeight(Integer.parseInt(updates.get("weight").toString()));
        if (updates.containsKey("priority")) task.setPriority(updates.get("priority").toString());
        if (updates.containsKey("commit_link")) {
            requireTaskAssignee(task, current);
            task.setCommitLink(updates.get("commit_link") != null ? updates.get("commit_link").toString() : null);
        } else if (updates.containsKey("commitLink")) {
            requireTaskAssignee(task, current);
            task.setCommitLink(updates.get("commitLink") != null ? updates.get("commitLink").toString() : null);
        }
        if (updates.containsKey("deadline")) {
            Object deadlineObj = updates.get("deadline");
            if (deadlineObj == null || deadlineObj.toString().isEmpty()) {
                task.setDeadline(null);
            } else {
                try {
                    task.setDeadline(new java.text.SimpleDateFormat("yyyy-MM-dd").parse(deadlineObj.toString()));
                } catch (Exception e) {
                    // ignore invalid date
                }
            }
        }
        if (updates.containsKey("assigned_to_id")) {
            Object assignedObj = updates.get("assigned_to_id");
            task.setAssignedToId(assignedObj == null ? null : Integer.parseInt(assignedObj.toString()));
        }

        return taskRepository.save(task);
    }

    @DeleteMapping("/{id}")
    public Task deleteTask(@PathVariable Integer id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        User current = getCurrentUser();
        requireLeaderOrTeacher(getMembership(task.getProjectId(), current.getId(), true), current);
        
        task.setIsDeleted(true);
        taskRepository.save(task);
        createLog(current.getId(), task.getProjectId(), "delete", "Task", task.getId(), "删除了任务《" + task.getTitle() + "》");
        return task;
    }

    @PostMapping("/{id}/restore")
    public Task restoreTask(@PathVariable Integer id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        User current = getCurrentUser();
        requireLeaderOrTeacher(getMembership(task.getProjectId(), current.getId(), true), current);
        
        task.setIsDeleted(false);
        taskRepository.save(task);
        createLog(current.getId(), task.getProjectId(), "restore", "Task", task.getId(), "从回收站恢复了任务《" + task.getTitle() + "》");
        return task;
    }

    @PostMapping("/{id}/take")
    public Task takeTask(@PathVariable Integer id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        User current = getCurrentUser();
        if (getMembership(task.getProjectId(), current.getId(), true) == null) throw new RuntimeException("You are not a member of this project.");
        if (task.getAssignedToId() != null) throw new RuntimeException("Task is already assigned.");
        
        task.setAssignedToId(current.getId());
        taskRepository.save(task);
        createLog(current.getId(), task.getProjectId(), "take", "Task", task.getId(), "主动认领了任务《" + task.getTitle() + "》");
        return task;
    }

    @PostMapping("/{id}/attachments")
    public Map<String, Object> uploadAttachment(@PathVariable Integer id, @RequestParam("file") MultipartFile file) throws Exception {
        Task task = taskRepository.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        User current = getCurrentUser();
        
        Optional<FileAttachment> prev = fileAttachmentRepository.findFirstByTaskIdAndFilenameOrderByVersionDesc(id, file.getOriginalFilename());
        int newVersion = prev.isPresent() ? prev.get().getVersion() + 1 : 1;
        
        File uploadDir = new File("uploads");
        if (!uploadDir.exists()) uploadDir.mkdirs();
        
        String filePath = "uploads/" + id + "_" + newVersion + "_" + file.getOriginalFilename();
        Path path = Paths.get(filePath);
        Files.write(path, file.getBytes());
        
        FileAttachment att = new FileAttachment();
        att.setTaskId(id);
        att.setUploaderId(current.getId());
        att.setFilename(file.getOriginalFilename());
        att.setFilePath(filePath);
        att.setVersion(newVersion);
        fileAttachmentRepository.save(att);
        
        Map<String, Object> res = new HashMap<>();
        res.put("message", "File uploaded");
        res.put("version", newVersion);
        return res;
    }

    @PostMapping("/{id}/focus")
    public Map<String, Object> logFocus(@PathVariable Integer id, @RequestBody FocusRequest req) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        User current = getCurrentUser();
        ProjectMember mem = getMembership(task.getProjectId(), current.getId(), true);
        if (mem == null) throw new RuntimeException("Not a member of this project.");
        
        mem.setFocusMinutes(mem.getFocusMinutes() + req.getMinutes());
        if (mem.getFocusMinutes() >= 100) {
            if (!projectMemberBadgeRepository.existsByProjectMemberIdAndBadgeType(mem.getId(), "focus_master")) {
                ProjectMemberBadge b = new ProjectMemberBadge();
                b.setProjectMemberId(mem.getId());
                b.setBadgeType("focus_master");
                projectMemberBadgeRepository.save(b);
            }
        }
        projectMemberRepository.save(mem);
        
        createLog(current.getId(), task.getProjectId(), "focus", "Task", task.getId(), "在任务《" + task.getTitle() + "》上专注了 " + req.getMinutes() + " 分钟");
        
        Map<String, Object> res = new HashMap<>();
        res.put("message", "Focus logged");
        res.put("total_focus_minutes", mem.getFocusMinutes());
        return res;
    }
}
