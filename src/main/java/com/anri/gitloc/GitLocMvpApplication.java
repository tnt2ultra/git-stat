package com.anri.gitloc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа MVP-сервиса статистики Java-строк по Git-репозиториям.
 */
@SpringBootApplication
public class GitLocMvpApplication {

    public static void main(String[] args) {
        SpringApplication.run(GitLocMvpApplication.class, args);
    }
}