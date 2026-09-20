package com.anri.gitloc.service;

import com.anri.gitloc.domain.GitService;
import com.anri.gitloc.domain.JobStatus;
import com.anri.gitloc.domain.SyncJob;
import com.anri.gitloc.dto.SyncJobDto;
import com.anri.gitloc.repository.GitServiceRepository;
import com.anri.gitloc.repository.SyncJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Сервис управления задачами синхронизации.
 */
@Service
@RequiredArgsConstructor
public class SyncJobService {

    private final SyncJobRepository syncJobRepository;
    private final GitServiceRepository gitServiceRepository;

    @Transactional
    public SyncJob start(Long serviceId) {
        GitService service = gitServiceRepository.getReferenceById(serviceId);

        SyncJob job = new SyncJob();
        job.setService(service);
        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(Instant.now());

        return syncJobRepository.save(job);
    }

    @Transactional
    public void success(Long jobId, GitAnalysisResult result) {
        SyncJob job = syncJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Задача синхронизации не найдена: " + jobId));

        job.setStatus(JobStatus.SUCCESS);
        job.setFinishedAt(Instant.now());
        job.setCommitsProcessed(result.getCommitsProcessed());
        job.setFilesProcessed(result.getFilesProcessed());
        job.setErrorMessage(null);

        syncJobRepository.save(job);
    }

    @Transactional
    public void fail(Long jobId, Exception e) {
        SyncJob job = syncJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Задача синхронизации не найдена: " + jobId));

        job.setStatus(JobStatus.FAILED);
        job.setFinishedAt(Instant.now());

        String message = e.getMessage();
        if (message != null && message.length() > 2000) {
            message = message.substring(0, 2000);
        }

        job.setErrorMessage(message);
        syncJobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public SyncJobDto getDto(Long jobId) {
        SyncJob job = syncJobRepository.findByIdWithService(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Задача синхронизации не найдена: " + jobId));
        return SyncJobDto.from(job);
    }

    @Transactional(readOnly = true)
    public List<SyncJobDto> getAllDto() {
        return syncJobRepository.findAllByOrderByStartedAtDesc()
                .stream()
                .map(SyncJobDto::from)
                .toList();
    }
}