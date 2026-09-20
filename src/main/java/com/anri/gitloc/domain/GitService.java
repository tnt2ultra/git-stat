package com.anri.gitloc.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Сервис, привязанный к Git-репозиторию.
 */
@Entity
@Table(
        name = "git_services",
        uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "name"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GitService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ServiceGroup serviceGroup;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "repo_url", nullable = false, length = 1024)
    private String repoUrl;

    @Column(name = "default_branch", length = 255)
    private String defaultBranch;

    @Column(name = "last_head_sha", length = 64)
    private String lastHeadSha;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 50)
    private SyncStatus syncStatus = SyncStatus.NEVER_SYNCED;

    @OneToOne(mappedBy = "service", fetch = FetchType.LAZY)
    private ServiceLocSnapshot snapshot;
}