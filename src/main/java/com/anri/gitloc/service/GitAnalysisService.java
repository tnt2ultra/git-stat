package com.anri.gitloc.service;

import com.anri.gitloc.domain.GitService;
import com.anri.gitloc.domain.JavaCategory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Сервис анализа Git-истории и текущего состояния репозитория.
 */
@Service
@RequiredArgsConstructor
public class GitAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(GitAnalysisService.class);

    private final GitRepositoryService gitRepositoryService;
    private final GitStorageService gitStorageService;
    private final JavaFileClassifier javaFileClassifier;

    /**
     * Выполняет полный анализ сервиса.
     *
     * @param service сервис
     * @return результат анализа
     */
    public GitAnalysisResult analyze(GitService service) {
        Path repoPath = gitStorageService.getRepoPath(service.getId());

        gitRepositoryService.ensureRepository(service);

        String headSha = gitRepositoryService.headSha(repoPath);
        String branch = gitRepositoryService.defaultBranch(repoPath);

        LogParseResult logParseResult = parseLog(repoPath);
        GitAnalysisResult.LocData locData = parseSnapshot(repoPath);

        return GitAnalysisResult.builder()
                .headSha(headSha)
                .branch(branch)
                .commitsProcessed(logParseResult.commitsProcessed)
                .filesProcessed(logParseResult.filesProcessed)
                .authorStats(logParseResult.authorStats)
                .loc(locData)
                .build();
    }

    private LogParseResult parseLog(Path repoPath) {
        String output = gitRepositoryService.logJavaNumstat(repoPath);

        Map<String, AuthorAccumulator> accumulators = new HashMap<>();
        long filesProcessed = 0;

        String currentEmail = null;
        String currentName = null;
        Instant currentInstant = null;
        boolean pendingCommit = false;

        for (String line : output.lines().toList()) {
            if (line.isBlank()) {
                continue;
            }

            String[] parts = line.split("\t", -1);

            if (parts.length >= 5 && isSha(parts[0])) {
                currentEmail = normalizeEmail(parts[2]);
                currentName = parts[3] == null ? "" : parts[3].trim();
                currentInstant = parseInstant(parts[1]);
                pendingCommit = true;
                continue;
            }

            if (parts.length == 3 && currentEmail != null) {
                if ("-".equals(parts[0]) || "-".equals(parts[1])) {
                    continue;
                }

                long additions;
                long deletions;

                try {
                    additions = Long.parseLong(parts[0]);
                    deletions = Long.parseLong(parts[1]);
                } catch (NumberFormatException e) {
                    continue;
                }

                String path = parts[2];

                // Явная фильтрация по расширению, так как pathspec в git log не используется.
                if (!path.toLowerCase(Locale.ROOT).endsWith(".java")) {
                    continue;
                }

                JavaCategory category = javaFileClassifier.classify(path);

                if (category == JavaCategory.EXCLUDED) {
                    continue;
                }

                filesProcessed++;

                AuthorAccumulator acc = accumulators.get(currentEmail);

                if (acc == null) {
                    acc = new AuthorAccumulator(currentName);
                    accumulators.put(currentEmail, acc);
                }

                if (currentName != null && !currentName.isBlank()) {
                    acc.authorName = currentName;
                }

                if (pendingCommit) {
                    acc.commits++;
                    pendingCommit = false;
                }

                if (category == JavaCategory.CODE) {
                    acc.addedCodeLines += additions;
                    acc.removedCodeLines += deletions;
                } else if (category == JavaCategory.TEST) {
                    acc.addedTestLines += additions;
                    acc.removedTestLines += deletions;
                }

                if (currentInstant != null) {
                    acc.lastCommitAt = currentInstant;
                }
            }
        }

        List<GitAnalysisResult.AuthorStatData> authorStats = new ArrayList<>();

        for (Map.Entry<String, AuthorAccumulator> entry : accumulators.entrySet()) {
            AuthorAccumulator acc = entry.getValue();

            authorStats.add(
                    GitAnalysisResult.AuthorStatData.builder()
                            .authorEmail(entry.getKey())
                            .authorName(acc.authorName)
                            .commits(acc.commits)
                            .addedCodeLines(acc.addedCodeLines)
                            .removedCodeLines(acc.removedCodeLines)
                            .addedTestLines(acc.addedTestLines)
                            .removedTestLines(acc.removedTestLines)
                            .lastCommitAt(acc.lastCommitAt)
                            .build()
            );
        }

        long commitsProcessed = authorStats.stream().mapToLong(GitAnalysisResult.AuthorStatData::getCommits).sum();

        return new LogParseResult(commitsProcessed, filesProcessed, authorStats);
    }

    private GitAnalysisResult.LocData parseSnapshot(Path repoPath) {
        List<GitRepositoryService.JavaFileRef> files = gitRepositoryService.listJavaFiles(repoPath);
        Map<String, Long> lineCounts = gitRepositoryService.countLines(repoPath, files);

        log.debug("Snapshot: найдено Java-файлов = {}, посчитано строк для = {}",
                files.size(), lineCounts.size());

        long codeLines = 0;
        long testLines = 0;
        long generatedExcludedLines = 0;

        int codeFiles = 0;
        int testFiles = 0;
        int generatedFiles = 0;

        for (GitRepositoryService.JavaFileRef file : files) {
            JavaCategory category = javaFileClassifier.classify(file.path());
            long lines = lineCounts.getOrDefault(file.path(), 0L);

            if (category == JavaCategory.EXCLUDED) {
                generatedExcludedLines += lines;
                generatedFiles++;
            } else if (category == JavaCategory.TEST) {
                testLines += lines;
                testFiles++;
            } else if (category == JavaCategory.CODE) {
                codeLines += lines;
                codeFiles++;
            }
        }

        return GitAnalysisResult.LocData.builder()
                .codeLines(codeLines)
                .testLines(testLines)
                .generatedExcludedLines(generatedExcludedLines)
                .codeFiles(codeFiles)
                .testFiles(testFiles)
                .generatedFiles(generatedFiles)
                .build();
    }

    private boolean isSha(String value) {
        if (value == null || value.length() != 40) {
            return false;
        }

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
            if (!hex) {
                return false;
            }
        }

        return true;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return "unknown";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private Instant parseInstant(String date) {
        try {
            return OffsetDateTime.parse(date).toInstant();
        } catch (Exception e) {
            return Instant.now();
        }
    }

    private static class LogParseResult {
        private final long commitsProcessed;
        private final long filesProcessed;
        private final List<GitAnalysisResult.AuthorStatData> authorStats;

        private LogParseResult(long commitsProcessed,
                               long filesProcessed,
                               List<GitAnalysisResult.AuthorStatData> authorStats) {
            this.commitsProcessed = commitsProcessed;
            this.filesProcessed = filesProcessed;
            this.authorStats = authorStats;
        }
    }

    private static class AuthorAccumulator {
        private String authorName;
        private long commits;
        private long addedCodeLines;
        private long removedCodeLines;
        private long addedTestLines;
        private long removedTestLines;
        private Instant lastCommitAt;

        private AuthorAccumulator(String authorName) {
            this.authorName = authorName;
        }
    }
}