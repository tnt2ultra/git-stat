package com.anri.gitloc.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Текущий снимок количества строк в Java-файлах сервиса.
 * <p>
 * Используется суррогатный первичный ключ id.
 * Связь с сервисом выполнена через отдельный внешний ключ service_id.
 * Такой маппинг является наиболее надёжным для Spring Data JPA и Hibernate.
 */
@Entity
@Table(name = "service_loc_snapshots")
@Getter
@Setter
@NoArgsConstructor
public class ServiceLocSnapshot {

    /**
     * Суррогатный первичный ключ снимка.
     * <p>
     * Генерируется базой данных.
     * Не должен назначаться вручную.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Сервис, к которому относится снимок.
     * <p>
     * Один сервис может иметь только один текущий снимок.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "service_id",
            nullable = false,
            unique = true
    )
    private GitService service;

    /**
     * Текущее количество строк продуктового Java-кода.
     */
    @Column(name = "code_lines", nullable = false)
    private long codeLines;

    /**
     * Текущее количество строк Java-тестов.
     */
    @Column(name = "test_lines", nullable = false)
    private long testLines;

    /**
     * Количество строк в исключённых Java-файлах.
     * <p>
     * Сюда попадают generated code, protobuf, openapi-generated,
     * миграции Flyway/Liquibase и другие исключённые файлы.
     */
    @Column(name = "generated_excluded_lines", nullable = false)
    private long generatedExcludedLines;

    /**
     * Количество файлов продуктового кода.
     */
    @Column(name = "code_files", nullable = false)
    private int codeFiles;

    /**
     * Количество тестовых файлов.
     */
    @Column(name = "test_files", nullable = false)
    private int testFiles;

    /**
     * Количество исключённых файлов.
     */
    @Column(name = "generated_files", nullable = false)
    private int generatedFiles;

    /**
     * Ветка, по которой снят snapshot.
     */
    @Column(name = "branch", length = 255)
    private String branch;

    /**
     * SHA коммита HEAD, по которому снят snapshot.
     */
    @Column(name = "head_sha", length = 64)
    private String headSha;

    /**
     * Момент синхронизации.
     */
    @Column(name = "synced_at")
    private Instant syncedAt;
}