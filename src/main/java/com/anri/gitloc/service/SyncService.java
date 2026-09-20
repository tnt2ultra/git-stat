package com.anri.gitloc.service;

import com.anri.gitloc.domain.GitService;
import com.anri.gitloc.dto.SyncJobDto;
import com.anri.gitloc.repository.GitServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Оркестратор синхронизации сервисов.
 */
@Service
@RequiredArgsConstructor
public class SyncService {

    private final GitServiceRepository gitServiceRepository;
    private final GitAnalysisService gitAnalysisService;
    private final SyncPersistenceService syncPersistenceService;
    private final SyncJobService syncJobService;

    /**
     * Синхронизирует один сервис.
     *
     * @param serviceId идентификатор сервиса
     * @return DTO задачи синхронизации
     */
    public SyncJobDto syncOne(Long serviceId) {
        GitService service = gitServiceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Сервис не найден: " + serviceId));

        var job = syncJobService.start(serviceId);

        try {
            GitAnalysisResult result = gitAnalysisService.analyze(service);
            syncPersistenceService.save(serviceId, result);
            syncJobService.success(job.getId(), result);
        } catch (Exception e) {
            syncJobService.fail(job.getId(), e);
        }

        return syncJobService.getDto(job.getId());
    }

    /**
     * Синхронизирует все сервисы.
     *
     * @return список задач синхронизации
     */
    public List<SyncJobDto> syncAll() {
        List<GitService> services = gitServiceRepository.findAllByOrderByIdAsc();
        List<SyncJobDto> jobs = new ArrayList<>();

        for (GitService service : services) {
            jobs.add(syncOne(service.getId()));
        }

        return jobs;
    }
}