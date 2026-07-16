package com.teamcollab.repository;
import com.teamcollab.entity.MallItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MallItemRepository extends JpaRepository<MallItem, Integer> {
    List<MallItem> findByProjectId(Integer projectId);
}
