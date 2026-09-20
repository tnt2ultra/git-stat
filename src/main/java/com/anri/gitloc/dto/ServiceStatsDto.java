package com.anri.gitloc.dto;

import com.anri.gitloc.domain.SyncStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Статистика по одному сервису.
 */
@Getter
@Setter
public class ServiceStatsDto {

    private Long serviceId;
    private String groupName;
    private String serviceName;
    private String repoUrl;
    private String defaultBranch;
    private String lastHeadSha;
    private long codeLines;
    private long testLines;
    private long totalLines;
    private Double testToCodeRatio;
    private Instant lastSyncAt;
    private SyncStatus syncStatus;

    public ServiceStatsDto(Long serviceId,
                           String groupName,
                           String serviceName,
                           String repoUrl,
                           String defaultBranch,
                           String lastHeadSha,
                           long codeLines,
                           long testLines,
                           Instant lastSyncAt,
                           SyncStatus syncStatus) {
        this.serviceId = serviceId;
        this.groupName = groupName;
        this.serviceName = serviceName;
        this.repoUrl = repoUrl;
        this.defaultBranch = defaultBranch;
        this.lastHeadSha = lastHeadSha;
        this.codeLines = codeLines;
        this.testLines = testLines;
        this.totalLines = codeLines + testLines;
        this.testToCodeRatio = codeLines == 0 ? null : (double) testLines / codeLines;
        this.lastSyncAt = lastSyncAt;
        this.syncStatus = syncStatus;
    }
}