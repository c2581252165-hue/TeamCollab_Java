package com.teamcollab.repository;
import com.teamcollab.entity.Standup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StandupRepository extends JpaRepository<Standup, Integer> {
    List<Standup> findByProjectId(Integer projectId);
    List<Standup> findByProjectIdOrderByDateDesc(Integer projectId);
}
