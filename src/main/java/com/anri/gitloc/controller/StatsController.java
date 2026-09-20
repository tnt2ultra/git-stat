package com.anri.gitloc.controller;

import com.anri.gitloc.dto.AuthorStatsDto;
import com.anri.gitloc.dto.GroupStatsDto;
import com.anri.gitloc.dto.ServiceStatsDto;
import com.anri.gitloc.dto.SummaryDto;
import com.anri.gitloc.repository.GitServiceRepository;
import com.anri.gitloc.repository.ServiceGroupRepository;
import com.anri.gitloc.repository.ServiceLocSnapshotRepository;
import com.anri.gitloc.repository.AuthorServiceStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API статистики.
 */
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final ServiceGroupRepository serviceGroupRepository;
    private final GitServiceRepository gitServiceRepository;
    private final ServiceLocSnapshotRepository serviceLocSnapshotRepository;
    private final AuthorServiceStatRepository authorServiceStatRepository;

    @GetMapping("/summary")
    public ResponseEntity<SummaryDto> summary() {
        ServiceLocSnapshotRepository.SummaryProjection projection = serviceLocSnapshotRepository.summarize();

        long serviceCount = projection.getServiceCount() == null ? 0L : projection.getServiceCount();
        long codeLines = projection.getCodeLines() == null ? 0L : projection.getCodeLines();
        long testLines = projection.getTestLines() == null ? 0L : projection.getTestLines();
        long groupCount = serviceGroupRepository.count();

        SummaryDto dto = new SummaryDto();
        dto.setGroupCount(groupCount);
        dto.setServiceCount(serviceCount);
        dto.setCodeLines(codeLines);
        dto.setTestLines(testLines);
        dto.setTotalLines(codeLines + testLines);
        dto.setTestToCodeRatio(codeLines == 0 ? null : (double) testLines / codeLines);

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/groups")
    public ResponseEntity<List<GroupStatsDto>> groups() {
        return ResponseEntity.ok(serviceLocSnapshotRepository.findGroupStats());
    }

    @GetMapping("/services")
    public ResponseEntity<List<ServiceStatsDto>> services(@RequestParam(required = false) Long groupId) {
        return ResponseEntity.ok(serviceLocSnapshotRepository.findServiceStats(groupId));
    }

    @GetMapping("/services/{serviceId}")
    public ResponseEntity<ServiceStatsDto> service(@PathVariable Long serviceId) {
        return serviceLocSnapshotRepository.findServiceStatsById(serviceId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/authors")
    public ResponseEntity<List<AuthorStatsDto>> authors(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long serviceId
    ) {
        return ResponseEntity.ok(authorServiceStatRepository.findAuthorStats(groupId, serviceId));
    }
}