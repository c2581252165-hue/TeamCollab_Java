package com.teamcollab.repository;
import com.teamcollab.entity.PurchaseHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PurchaseHistoryRepository extends JpaRepository<PurchaseHistory, Integer> {
    List<PurchaseHistory> findByProjectIdOrderByPurchasedAtDesc(Integer projectId);
}
