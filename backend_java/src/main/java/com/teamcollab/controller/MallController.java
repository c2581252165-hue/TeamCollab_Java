package com.teamcollab.controller;

import com.teamcollab.entity.*;
import com.teamcollab.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/mall")
public class MallController {

    @Autowired private MallItemRepository mallItemRepository;
    @Autowired private PurchaseHistoryRepository purchaseHistoryRepository;
    @Autowired private ProjectMemberRepository projectMemberRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private LogRepository logRepository;

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

    @GetMapping("/items/{project_id}")
    public List<MallItem> getMallItems(@PathVariable("project_id") Integer projectId) {
        User current = getCurrentUser();
        if (getMembership(projectId, current.getId(), true) == null) {
            throw new RuntimeException("Not authorized");
        }

        List<MallItem> items = mallItemRepository.findByProjectId(projectId);
        if (items.isEmpty()) {
            String[][] defaults = {
                {"免写一次站会券", "可以偷懒一天不写每日站会，免除组长责罚", "100", "🎫"},
                {"要求组长请喝奶茶", "凭此券可向组长兑换一杯不超过20元的奶茶", "500", "🧋"},
                {"划水豁免权一天", "今天不分配任何任务，专心摸鱼", "300", "🎣"},
                {"指定一人帮你写Bug", "把你的某个棘手Bug强制转移给其他组员（需组长同意）", "800", "🐛"}
            };
            for (String[] d : defaults) {
                MallItem item = new MallItem();
                item.setProjectId(projectId);
                item.setName(d[0]);
                item.setDescription(d[1]);
                item.setPrice(Integer.parseInt(d[2]));
                item.setIcon(d[3]);
                mallItemRepository.save(item);
            }
            items = mallItemRepository.findByProjectId(projectId);
        }
        return items;
    }

    @PostMapping("/items")
    public MallItem createMallItem(@RequestBody MallItem item) {
        User current = getCurrentUser();
        requireLeaderOrTeacher(getMembership(item.getProjectId(), current.getId(), true), current);
        return mallItemRepository.save(item);
    }

    @DeleteMapping("/items/{item_id}")
    public Map<String, String> deleteMallItem(@PathVariable("item_id") Integer itemId) {
        MallItem item = mallItemRepository.findById(itemId).orElseThrow(() -> new RuntimeException("Item not found"));
        User current = getCurrentUser();
        requireLeaderOrTeacher(getMembership(item.getProjectId(), current.getId(), true), current);
        mallItemRepository.delete(item);
        return Collections.singletonMap("message", "Item deleted");
    }

    @PostMapping("/purchase/{item_id}")
    public PurchaseHistory purchaseItem(@PathVariable("item_id") Integer itemId) {
        MallItem item = mallItemRepository.findById(itemId).orElseThrow(() -> new RuntimeException("Item not found"));
        User current = getCurrentUser();
        if (getMembership(item.getProjectId(), current.getId(), true) == null) {
            throw new RuntimeException("Not authorized");
        }

        if (current.getPoints() < item.getPrice()) {
            throw new RuntimeException("Insufficient points");
        }

        current.setPoints(current.getPoints() - item.getPrice());
        userRepository.save(current);

        PurchaseHistory history = new PurchaseHistory();
        history.setProjectId(item.getProjectId());
        history.setUserId(current.getId());
        history.setItemId(item.getId());
        purchaseHistoryRepository.save(history);

        createLog(current.getId(), item.getProjectId(), "purchase", "Mall", item.getId(), "花费 " + item.getPrice() + " 积分兑换了【" + item.getName() + "】");

        return history;
    }

    @GetMapping("/history/{project_id}")
    public List<PurchaseHistory> getPurchaseHistory(@PathVariable("project_id") Integer projectId) {
        User current = getCurrentUser();
        if (getMembership(projectId, current.getId(), true) == null) {
            throw new RuntimeException("Not authorized");
        }
        return purchaseHistoryRepository.findByProjectIdOrderByPurchasedAtDesc(projectId);
    }
}
