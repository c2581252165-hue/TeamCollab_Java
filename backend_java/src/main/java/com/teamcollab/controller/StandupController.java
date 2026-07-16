package com.teamcollab.controller;
import com.teamcollab.entity.Log;
import com.teamcollab.entity.Standup;
import com.teamcollab.entity.User;
import com.teamcollab.repository.LogRepository;
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

    @Autowired private LogRepository logRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username);
    }

    @GetMapping
    public List<Standup> getStandups() {
        User current = getCurrentUser();
        return standupRepository.findByProjectIdOrderByDateDesc(current.getCurrentProjectId());
    }

    @PostMapping
    public Standup createStandup(@RequestBody Standup standup) {
        User current = getCurrentUser();
        standup.setUserId(current.getId());
        standup.setProjectId(current.getCurrentProjectId());
        standupRepository.save(standup);

        Log log = new Log();
        log.setUserId(current.getId());
        log.setProjectId(current.getCurrentProjectId());
        log.setAction("standup");
        log.setTargetType("Standup");
        log.setDetails("提交了每日站会更新");
        logRepository.save(log);

        return standup;
    }
}
