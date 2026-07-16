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
