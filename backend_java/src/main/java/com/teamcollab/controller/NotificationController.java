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
