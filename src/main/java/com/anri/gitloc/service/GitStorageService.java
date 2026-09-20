package com.anri.gitloc.service;

import com.anri.gitloc.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Сервис хранения локальных bare-репозиториев.
 */
@Service
@RequiredArgsConstructor
public class GitStorageService {

    private final AppProperties appProperties;

    /**
     * Возвращает путь к bare-репозиторию сервиса.
     *
     * @param serviceId идентификатор сервиса
     * @return путь к репозиторию
     */
    public Path getRepoPath(Long serviceId) {
        return Paths.get(appProperties.getGit().getStoragePath())
                .resolve("service-" + serviceId)
                .resolve("repo.git");
    }

    /**
     * Удаляет локальный репозиторий сервиса, если он существует.
     *
     * @param serviceId идентификатор сервиса
     */
    public void deleteRepo(Long serviceId) {
        if (serviceId == null) {
            return;
        }

        Path path = getRepoPath(serviceId);
        if (!Files.exists(path)) {
            return;
        }

        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            throw new GitException("Не удалось удалить кэш репозитория: " + p, e);
                        }
                    });
        } catch (IOException e) {
            throw new GitException("Не удалось удалить кэш репозитория", e);
        }
    }
}