package com.anri.gitloc.repository;

import com.anri.gitloc.domain.AuthorServiceStat;
import com.anri.gitloc.dto.AuthorStatsDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Репозиторий статистики авторов по сервисам.
 */
public interface AuthorServiceStatRepository extends JpaRepository<AuthorServiceStat, Long> {

    /**
     * Удаляет статистику автора по конкретному сервису.
     * <p>
     * Используется bulk delete.
     * clearAutomatically не включён, чтобы не откреплять GitService
     * в рамках той же транзакции сохранения.
     *
     * @param serviceId идентификатор сервиса
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from AuthorServiceStat a where a.service.id = :serviceId")
    void deleteByServiceId(@Param("serviceId") Long serviceId);

    /**
     * Возвращает агрегированную статистику по сотрудникам.
     *
     * @param groupId   идентификатор группы или null
     * @param serviceId идентификатор сервиса или null
     * @return список статистики по авторам
     */
    @Query("""
            select new com.anri.gitloc.dto.AuthorStatsDto(
                a.authorEmail,
                max(a.authorName),
                sum(a.commits),
                sum(a.addedCodeLines),
                sum(a.removedCodeLines),
                sum(a.addedTestLines),
                sum(a.removedTestLines),
                count(distinct s.id)
            )
            from AuthorServiceStat a
            join a.service s
            where (:groupId is null or s.serviceGroup.id = :groupId)
              and (:serviceId is null or s.id = :serviceId)
            group by a.authorEmail
            order by sum(a.addedCodeLines) desc
            """)
    List<AuthorStatsDto> findAuthorStats(
            @Param("groupId") Long groupId,
            @Param("serviceId") Long serviceId
    );
}