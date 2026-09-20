package com.anri.gitloc.controller;

import com.anri.gitloc.dto.SyncJobDto;
import com.anri.gitloc.service.SyncJobService;
import com.anri.gitloc.service.SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API синхронизации.
 */
@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;
    private final SyncJobService syncJobService;

    @PostMapping("/services/{serviceId}")
    public ResponseEntity<SyncJobDto> syncService(@PathVariable Long serviceId) {
        return ResponseEntity.ok(syncService.syncOne(serviceId));
    }

    @PostMapping("/all")
    public ResponseEntity<List<SyncJobDto>> syncAll() {
        return ResponseEntity.ok(syncService.syncAll());
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<SyncJobDto>> jobs() {
        return ResponseEntity.ok(syncJobService.getAllDto());
    }
}