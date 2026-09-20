package com.anri.gitloc.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Агрегированная статистика сотрудника.
 */
@Getter
@Setter
public class AuthorStatsDto {

    private String authorEmail;
    private String authorName;
    private long commits;
    private long addedCodeLines;
    private long removedCodeLines;
    private long addedTestLines;
    private long removedTestLines;
    private long netCodeLines;
    private long netTestLines;
    private long serviceCount;

    public AuthorStatsDto(String authorEmail,
                          String authorName,
                          long commits,
                          long addedCodeLines,
                          long removedCodeLines,
                          long addedTestLines,
                          long removedTestLines,
                          long serviceCount) {
        this.authorEmail = authorEmail;
        this.authorName = authorName;
        this.commits = commits;
        this.addedCodeLines = addedCodeLines;
        this.removedCodeLines = removedCodeLines;
        this.addedTestLines = addedTestLines;
        this.removedTestLines = removedTestLines;
        this.netCodeLines = addedCodeLines - removedCodeLines;
        this.netTestLines = addedTestLines - removedTestLines;
        this.serviceCount = serviceCount;
    }
}