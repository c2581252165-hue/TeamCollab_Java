package com.teamcollab.repository;
import com.teamcollab.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Integer> {
    List<ProjectMember> findByUserId(Integer userId);
    ProjectMember findByUserIdAndProjectId(Integer userId, Integer projectId);
    List<ProjectMember> findByProjectIdAndIsApproved(Integer projectId, Boolean isApproved);
}
