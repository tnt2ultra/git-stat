package com.anri.gitloc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Конфигурация приложения.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Git git = new Git();

    @Data
    public static class Git {
        /**
         * Каталог для локальных bare-репозиториев.
         */
        private String storagePath = "./git-repos";

        /**
         * Таймаут выполнения Git-команд в секундах.
         */
        private int timeoutSeconds = 600;

        /**
         * Токен GitLab для read_repository.
         */
        private String token = "";

        /**
         * Разрешённый хост корпоративного GitLab.
         */
        private String allowedHost = "";
    }
}