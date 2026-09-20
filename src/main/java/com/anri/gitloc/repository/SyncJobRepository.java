package com.anri.gitloc.repository;

import com.anri.gitloc.domain.SyncJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий задач синхронизации.
 */
public interface SyncJobRepository extends JpaRepository<SyncJob, Long> {

    @Query("select j from SyncJob j left join fetch j.service where j.id = :id")
    Optional<SyncJob> findByIdWithService(@Param("id") Long id);

    List<SyncJob> findAllByOrderByStartedAtDesc();
}