package com.anri.gitloc.service;

import com.anri.gitloc.domain.GitService;
import com.anri.gitloc.domain.ServiceGroup;
import com.anri.gitloc.domain.SyncStatus;
import com.anri.gitloc.dto.ImportResultDto;
import com.anri.gitloc.repository.GitServiceRepository;
import com.anri.gitloc.repository.ServiceGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Транзакционное сохранение реестра из CSV.
 */
@Service
@RequiredArgsConstructor
public class RegistryPersistenceService {

    private final ServiceGroupRepository serviceGroupRepository;
    private final GitServiceRepository gitServiceRepository;
    private final GitStorageService gitStorageService;

    @Transactional
    public ImportResultDto importRows(List<CsvImportService.ParsedRow> rows) {
        int groupsCreated = 0;
        int servicesCreated = 0;
        int servicesUpdated = 0;

        for (CsvImportService.ParsedRow row : rows) {
            ServiceGroup group = serviceGroupRepository.findByName(row.groupName()).orElse(null);

            if (group == null) {
                group = new ServiceGroup();
                group.setName(row.groupName());
                group = serviceGroupRepository.save(group);
                groupsCreated++;
            }

            GitService service = gitServiceRepository
                    .findByGroupIdAndName(group.getId(), row.serviceName())
                    .orElse(null);

            if (service == null) {
                service = new GitService();
                service.setServiceGroup(group);
                service.setName(row.serviceName());
                servicesCreated++;
            } else {
                servicesUpdated++;

                if (!Objects.equals(service.getRepoUrl(), row.repoUrl())) {
                    gitStorageService.deleteRepo(service.getId());
                    service.setLastHeadSha(null);
                    service.setSyncStatus(SyncStatus.NEVER_SYNCED);
                }
            }

            service.setRepoUrl(row.repoUrl());
            gitServiceRepository.save(service);
        }

        return new ImportResultDto(groupsCreated, servicesCreated, servicesUpdated, List.of());
    }
}