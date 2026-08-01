package com.amar.slackclone.workspace;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkspaceRepository
        extends JpaRepository<Workspace, Long> {

    boolean existsBySlug(String slug);

    Optional<Workspace> findBySlug(String slug);
}