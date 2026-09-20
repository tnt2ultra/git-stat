package com.anri.gitloc.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Результат анализа Git-репозитория.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitAnalysisResult {

    private String headSha;
    private String branch;
    private long commitsProcessed;
    private long filesProcessed;
    private List<AuthorStatData> authorStats;
    private LocData loc;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorStatData {
        private String authorEmail;
        private String authorName;
        private long commits;
        private long addedCodeLines;
        private long removedCodeLines;
        private long addedTestLines;
        private long removedTestLines;
        private Instant lastCommitAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocData {
        private long codeLines;
        private long testLines;
        private long generatedExcludedLines;
        private int codeFiles;
        private int testFiles;
        private int generatedFiles;
    }
}