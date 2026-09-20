package com.anri.gitloc.repository;

import com.anri.gitloc.domain.ServiceLocSnapshot;
import com.anri.gitloc.dto.GroupStatsDto;
import com.anri.gitloc.dto.ServiceStatsDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий текущих снимков LOC.
 */
public interface ServiceLocSnapshotRepository extends JpaRepository<ServiceLocSnapshot, Long> {

    /**
     * Находит снимок по идентификатору сервиса.
     * <p>
     * Используется явный JPQL, потому что идентификатор снимка больше
     * не совпадает с идентификатором сервиса на уровне маппинга.
     *
     * @param serviceId идентификатор сервиса
     * @return снимок или empty
     */
    @Query("select s from ServiceLocSnapshot s where s.service.id = :serviceId")
    Optional<ServiceLocSnapshot> findByServiceId(@Param("serviceId") Long serviceId);

    /**
     * Возвращает статистику по группам сервисов.
     *
     * @return список статистики по группам
     */
    @Query("""
            select new com.anri.gitloc.dto.GroupStatsDto(
                g.id,
                g.name,
                count(s.id),
                coalesce(sum(loc.codeLines), 0),
                coalesce(sum(loc.testLines), 0)
            )
            from ServiceGroup g
            left join g.services s
            left join s.snapshot loc
            group by g.id, g.name
            order by g.name
            """)
    List<GroupStatsDto> findGroupStats();

    /**
     * Возвращает статистику по сервисам.
     *
     * @param groupId идентификатор группы или null для всех групп
     * @return список статистики по сервисам
     */
    @Query("""
            select new com.anri.gitloc.dto.ServiceStatsDto(
                s.id,
                g.name,
                s.name,
                s.repoUrl,
                s.defaultBranch,
                s.lastHeadSha,
                coalesce(loc.codeLines, 0),
                coalesce(loc.testLines, 0),
                s.lastSyncAt,
                s.syncStatus
            )
            from GitService s
            join s.serviceGroup g
            left join s.snapshot loc
            where (:groupId is null or g.id = :groupId)
            order by g.name, s.name
            """)
    List<ServiceStatsDto> findServiceStats(@Param("groupId") Long groupId);

    /**
     * Возвращает статистику по одному сервису.
     *
     * @param serviceId идентификатор сервиса
     * @return статистика сервиса или empty
     */
    @Query("""
            select new com.anri.gitloc.dto.ServiceStatsDto(
                s.id,
                g.name,
                s.name,
                s.repoUrl,
                s.defaultBranch,
                s.lastHeadSha,
                coalesce(loc.codeLines, 0),
                coalesce(loc.testLines, 0),
                s.lastSyncAt,
                s.syncStatus
            )
            from GitService s
            join s.serviceGroup g
            left join s.snapshot loc
            where s.id = :serviceId
            """)
    Optional<ServiceStatsDto> findServiceStatsById(@Param("serviceId") Long serviceId);

    /**
     * Возвращает сводную статистику по всем снимкам.
     *
     * @return проекция сводки
     */
    @Query("""
            select
                count(loc.id) as serviceCount,
                coalesce(sum(loc.codeLines), 0) as codeLines,
                coalesce(sum(loc.testLines), 0) as testLines
            from ServiceLocSnapshot loc
            """)
    SummaryProjection summarize();

    /**
     * Проекция сводной статистики.
     */
    interface SummaryProjection {
        Long getServiceCount();

        Long getCodeLines();

        Long getTestLines();
    }
}