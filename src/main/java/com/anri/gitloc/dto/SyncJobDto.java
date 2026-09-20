package com.anri.gitloc.dto;

import com.anri.gitloc.domain.JobStatus;
import com.anri.gitloc.domain.SyncJob;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * DTO задачи синхронизации.
 */
@Getter
@Setter
public class SyncJobDto {

    private Long id;
    private Long serviceId;
    private String serviceName;
    private JobStatus status;
    private Instant startedAt;
    private Instant finishedAt;
    private Long commitsProcessed;
    private Long filesProcessed;
    private String errorMessage;

    public static SyncJobDto from(SyncJob job) {
        SyncJobDto dto = new SyncJobDto();
        dto.id = job.getId();
        dto.serviceId = job.getService().getId();
        dto.serviceName = job.getService().getName();
        dto.status = job.getStatus();
        dto.startedAt = job.getStartedAt();
        dto.finishedAt = job.getFinishedAt();
        dto.commitsProcessed = job.getCommitsProcessed();
        dto.filesProcessed = job.getFilesProcessed();
        dto.errorMessage = job.getErrorMessage();
        return dto;
    }
}