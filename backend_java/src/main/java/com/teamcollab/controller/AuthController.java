package com.teamcollab.controller;

import com.teamcollab.dto.RegisterDto;
import com.teamcollab.entity.Project;
import com.teamcollab.entity.ProjectMember;
import com.teamcollab.entity.Task;
import com.teamcollab.entity.User;
import com.teamcollab.repository.ProjectMemberRepository;
import com.teamcollab.repository.ProjectRepository;
import com.teamcollab.repository.TaskRepository;
import com.teamcollab.repository.UserRepository;
import com.teamcollab.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private UserRepository userRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectMemberRepository projectMemberRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestParam String username, @RequestParam String password) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("Incorrect username or password (User not found)");
        }
        if (!passwordEncoder.matches(password, user.getHashedPassword())) {
            throw new RuntimeException("Incorrect username or password (Invalid password)");
        }
        String token = jwtUtil.generateToken(username);
        Map<String, Object> response = new HashMap<>();
        response.put("access_token", token);
        response.put("token_type", "bearer");
        response.put("user", user);
        return response;
    }

    @PostMapping("/register")
    public User register(@RequestBody RegisterDto registerDto) {
        if (userRepository.findByUsername(registerDto.getUsername()) != null) {
            throw new RuntimeException("Username already registered");
        }
        User newUser = new User();
        newUser.setUsername(registerDto.getUsername());
        newUser.setHashedPassword(passwordEncoder.encode(registerDto.getPassword()));
        newUser.setRole(registerDto.getRole());
        userRepository.save(newUser);

        // Ensure tutorial-only users exist without public default credentials.
        User u1 = userRepository.findByUsername("Alice(AI组员)");
        if (u1 == null) {
            u1 = new User();
            u1.setUsername("Alice(AI组员)");
            u1.setHashedPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            u1.setRole("member");
            u1.setPoints(15);
            userRepository.save(u1);
        }
        User u2 = userRepository.findByUsername("Bob(AI讲师)");
        if (u2 == null) {
            u2 = new User();
            u2.setUsername("Bob(AI讲师)");
            u2.setHashedPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            u2.setRole("teacher");
            u2.setPoints(30);
            userRepository.save(u2);
        }

        Project tutorialProj = new Project();
        tutorialProj.setName("新手教程团队 - " + newUser.getUsername());
        tutorialProj.setDescription("系统为您专属生成的新手引导团队，数据绝对隔离");
        tutorialProj.setInviteCode(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        projectRepository.save(tutorialProj);

        ProjectMember m1 = new ProjectMember();
        m1.setUser(u1); m1.setProject(tutorialProj); m1.setIsApproved(true);
        projectMemberRepository.save(m1);
        
        ProjectMember m2 = new ProjectMember();
        m2.setUser(u2); m2.setProject(tutorialProj); m2.setIsApproved(true);
        projectMemberRepository.save(m2);

        Task t1 = new Task();
        t1.setTitle("探索工作台核心数据"); t1.setDescription("熟悉各类项目指标统计");
        t1.setPhase("requirement"); t1.setWeight(1); t1.setProjectId(tutorialProj.getId());
        t1.setAssignedToId(u1.getId()); t1.setStatus("done");
        taskRepository.save(t1);

        Task t2 = new Task();
        t2.setTitle("体验拖拽式敏捷看板"); t2.setDescription("把这张卡片拖到【已完成】列试试看");
        t2.setPhase("coding"); t2.setWeight(3); t2.setProjectId(tutorialProj.getId());
        t2.setAssignedToId(u2.getId()); t2.setStatus("in_progress");
        t2.setDeadline(new java.util.Date(System.currentTimeMillis() + 3 * 86400000L));
        taskRepository.save(t2);

        Task t3 = new Task();
        t3.setTitle("让AI生成一键周报"); t3.setDescription("点击右下角的悬浮按钮，让AI总结现在的进度吧");
        t3.setPhase("test"); t3.setWeight(2); t3.setProjectId(tutorialProj.getId());
        t3.setDeadline(new java.util.Date(System.currentTimeMillis() + 86400000L));
        taskRepository.save(t3);

        ProjectMember member = new ProjectMember();
        member.setUser(newUser); member.setProject(tutorialProj);
        member.setIsApproved(true); member.setRole("leader");
        projectMemberRepository.save(member);

        newUser.setCurrentProjectId(tutorialProj.getId());
        userRepository.save(newUser);

        return newUser;
    }

    @GetMapping("/me")
    public User getMe() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username);
    }
}
