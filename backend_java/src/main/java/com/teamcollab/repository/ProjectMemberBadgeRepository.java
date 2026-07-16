package com.teamcollab.repository;
import com.teamcollab.entity.ProjectMemberBadge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberBadgeRepository extends JpaRepository<ProjectMemberBadge, Integer> {
    boolean existsByProjectMemberIdAndBadgeType(Integer projectMemberId, String badgeType);
}
