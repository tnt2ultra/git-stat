package com.anri.gitloc.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Статистика автора по одному сервису на основе коммитов.
 */
@Entity
@Table(
        name = "author_service_stats",
        uniqueConstraints = @UniqueConstraint(columnNames = {"service_id", "author_email"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthorServiceStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private GitService service;

    @Column(name = "author_email", nullable = false, length = 320)
    private String authorEmail;

    @Column(name = "author_name", length = 255)
    private String authorName;

    @Column(name = "commits", nullable = false)
    private long commits;

    @Column(name = "added_code_lines", nullable = false)
    private long addedCodeLines;

    @Column(name = "removed_code_lines", nullable = false)
    private long removedCodeLines;

    @Column(name = "added_test_lines", nullable = false)
    private long addedTestLines;

    @Column(name = "removed_test_lines", nullable = false)
    private long removedTestLines;

    @Column(name = "last_commit_at")
    private Instant lastCommitAt;
}