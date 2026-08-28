package com.amar.slackclone.workspace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface WorkspaceRepository
        extends JpaRepository<Workspace, Long> {

    boolean existsBySlug(String slug);

    Optional<Workspace> findBySlug(String slug);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select w from Workspace w where w.id=:id")
    Optional<Workspace> findByIdForUpdate(@Param("id") Long id);
}
