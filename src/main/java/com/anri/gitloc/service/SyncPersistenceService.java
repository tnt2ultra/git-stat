package com.anri.gitloc.service;

import com.anri.gitloc.domain.AuthorServiceStat;
import com.anri.gitloc.domain.GitService;
import com.anri.gitloc.domain.ServiceLocSnapshot;
import com.anri.gitloc.domain.SyncStatus;
import com.anri.gitloc.repository.AuthorServiceStatRepository;
import com.anri.gitloc.repository.GitServiceRepository;
import com.anri.gitloc.repository.ServiceLocSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Сохранение результатов анализа в H2.
 */
@Service
@RequiredArgsConstructor
public class SyncPersistenceService {

    private final GitServiceRepository gitServiceRepository;
    private final AuthorServiceStatRepository authorServiceStatRepository;
    private final ServiceLocSnapshotRepository serviceLocSnapshotRepository;

    /**
     * Сохраняет результат синхронизации сервиса.
     *
     * @param serviceId идентификатор сервиса
     * @param result    результат анализа
     */
    @Transactional
    public void save(Long serviceId, GitAnalysisResult result) {
        GitService service = gitServiceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Сервис не найден: " + serviceId));

        authorServiceStatRepository.deleteByServiceId(serviceId);

        List<AuthorServiceStat> stats = new ArrayList<>();

        for (GitAnalysisResult.AuthorStatData data : result.getAuthorStats()) {
            AuthorServiceStat stat = new AuthorServiceStat();
            stat.setService(service);
            stat.setAuthorEmail(data.getAuthorEmail());
            stat.setAuthorName(data.getAuthorName());
            stat.setCommits(data.getCommits());
            stat.setAddedCodeLines(data.getAddedCodeLines());
            stat.setRemovedCodeLines(data.getRemovedCodeLines());
            stat.setAddedTestLines(data.getAddedTestLines());
            stat.setRemovedTestLines(data.getRemovedTestLines());
            stat.setLastCommitAt(
                    data.getLastCommitAt() == null
                            ? Instant.now()
                            : data.getLastCommitAt()
            );
            stats.add(stat);
        }

        authorServiceStatRepository.saveAll(stats);

        ServiceLocSnapshot snapshot = serviceLocSnapshotRepository.findByServiceId(serviceId)
                .orElseGet(ServiceLocSnapshot::new);

        snapshot.setService(service);
        snapshot.setCodeLines(result.getLoc().getCodeLines());
        snapshot.setTestLines(result.getLoc().getTestLines());
        snapshot.setGeneratedExcludedLines(result.getLoc().getGeneratedExcludedLines());
        snapshot.setCodeFiles(result.getLoc().getCodeFiles());
        snapshot.setTestFiles(result.getLoc().getTestFiles());
        snapshot.setGeneratedFiles(result.getLoc().getGeneratedFiles());
        snapshot.setBranch(result.getBranch());
        snapshot.setHeadSha(result.getHeadSha());
        snapshot.setSyncedAt(Instant.now());

        serviceLocSnapshotRepository.save(snapshot);

        service.setLastHeadSha(result.getHeadSha());
        service.setDefaultBranch(result.getBranch());
        service.setLastSyncAt(Instant.now());
        service.setSyncStatus(SyncStatus.OK);

        gitServiceRepository.save(service);
    }
}