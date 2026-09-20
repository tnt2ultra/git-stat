package com.anri.gitloc.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Статистика по группе сервисов.
 */
@Getter
@Setter
public class GroupStatsDto {

    private Long groupId;
    private String groupName;
    private long serviceCount;
    private long codeLines;
    private long testLines;
    private long totalLines;
    private Double testToCodeRatio;

    public GroupStatsDto(Long groupId,
                         String groupName,
                         long serviceCount,
                         long codeLines,
                         long testLines) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.serviceCount = serviceCount;
        this.codeLines = codeLines;
        this.testLines = testLines;
        this.totalLines = codeLines + testLines;
        this.testToCodeRatio = codeLines == 0 ? null : (double) testLines / codeLines;
    }
}