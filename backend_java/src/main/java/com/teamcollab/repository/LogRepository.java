package com.teamcollab.repository;
import com.teamcollab.entity.Log;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LogRepository extends JpaRepository<Log, Integer> {
    List<Log> findByProjectIdOrderByTimestampDesc(Integer projectId);
}
