package com.anri.gitloc.repository;

import com.anri.gitloc.domain.ServiceGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Репозиторий групп сервисов.
 */
public interface ServiceGroupRepository extends JpaRepository<ServiceGroup, Long> {

    Optional<ServiceGroup> findByName(String name);
}