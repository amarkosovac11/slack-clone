package com.amar.slackclone.workspace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceMemberRepository
                extends JpaRepository<WorkspaceMember, Long> {

        @EntityGraph(attributePaths = {
                        "workspace",
                        "workspace.owner"
        })
        List<WorkspaceMember> findAllByUserId(Long userId);

        @EntityGraph(attributePaths = "user")
        List<WorkspaceMember> findAllByWorkspaceId(Long workspaceId);

        Optional<WorkspaceMember> findByWorkspaceIdAndUserId(
                        Long workspaceId,
                        Long userId);

        boolean existsByWorkspaceIdAndUserId(
                        Long workspaceId,
                        Long userId);

        @Query("""
                select distinct wm.user from WorkspaceMember wm
                where wm.workspace.id in (select mine.workspace.id from WorkspaceMember mine where mine.user.id = :userId)
                  and wm.user.id <> :userId order by wm.user.displayName
                """)
        List<com.amar.slackclone.user.User> findMessageableUsers(@Param("userId") Long userId);

        @Query("""
                select count(wm) > 0 from WorkspaceMember wm
                where wm.user.id = :otherUserId and wm.workspace.id in
                    (select mine.workspace.id from WorkspaceMember mine where mine.user.id = :userId)
                """)
        boolean shareWorkspace(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);
}
