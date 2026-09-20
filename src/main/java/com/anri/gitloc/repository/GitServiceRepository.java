package com.anri.gitloc.repository;

import com.anri.gitloc.domain.GitService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий сервисов.
 */
public interface GitServiceRepository extends JpaRepository<GitService, Long> {

    /**
     * Находит сервис по идентификатору группы и названию сервиса.
     *
     * @param groupId идентификатор группы
     * @param name    название сервиса
     * @return найденный сервис или empty
     */
    @Query("select s from GitService s where s.serviceGroup.id = :groupId and s.name = :name")
    Optional<GitService> findByGroupIdAndName(
            @Param("groupId") Long groupId,
            @Param("name") String name
    );

    /**
     * Возвращает все сервисы, отсортированные по идентификатору.
     *
     * @return список сервисов
     */
    List<GitService> findAllByOrderByIdAsc();
}