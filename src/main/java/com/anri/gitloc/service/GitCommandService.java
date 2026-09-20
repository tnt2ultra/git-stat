package com.anri.gitloc.service;

import com.anri.gitloc.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Сервис выполнения нативных Git-команд.
 */
@Service
@RequiredArgsConstructor
public class GitCommandService {

    private final AppProperties appProperties;

    /**
     * Выполняет Git-команду и возвращает stdout.
     *
     * @param repo рабочая директория репозитория или null
     * @param args аргументы git
     * @return stdout команды
     */
    public String run(Path repo, List<String> args) {
        List<String> command = buildCommand(repo, args);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().put("GIT_TERMINAL_PROMPT", "0");
        pb.redirectErrorStream(false);

        try {
            Process process = pb.start();

            CompletableFuture<String> stdoutFuture =
                    CompletableFuture.supplyAsync(() -> readStream(process.getInputStream()));
            CompletableFuture<String> stderrFuture =
                    CompletableFuture.supplyAsync(() -> readStream(process.getErrorStream()));

            boolean finished = process.waitFor(appProperties.getGit().getTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new GitException("Таймаут выполнения Git-команды");
            }

            int exitCode = process.exitValue();
            String stdout = stdoutFuture.get();
            String stderr = stderrFuture.get();

            if (exitCode != 0) {
                throw new GitException(sanitize(stderr));
            }

            return stdout;
        } catch (IOException e) {
            throw new GitException(sanitize(e.getMessage()), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GitException("Выполнение Git-команды прервано", e);
        } catch (ExecutionException e) {
            throw new GitException(sanitize(String.valueOf(e.getCause())), e);
        }
    }

    /**
     * Запускает Git-процесс для потокового взаимодействия.
     *
     * @param repo рабочая директория репозитория
     * @param args аргументы git
     * @return запущенный процесс
     */
    public Process startProcess(Path repo, List<String> args) {
        List<String> command = buildCommand(repo, args);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().put("GIT_TERMINAL_PROMPT", "0");
        pb.redirectErrorStream(false);

        try {
            return pb.start();
        } catch (IOException e) {
            throw new GitException(sanitize(e.getMessage()), e);
        }
    }

    /**
     * Маскирует токен в текстах ошибок и логов.
     *
     * @param text исходный текст
     * @return текст без токена
     */
    public String sanitize(String text) {
        if (text == null) {
            return null;
        }

        String token = appProperties.getGit().getToken();
        if (token == null || token.isBlank()) {
            return text;
        }

        String result = text.replace(token, "***");
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        return result.replace(encodedToken, "***");
    }

    private List<String> buildCommand(Path repo, List<String> args) {
        List<String> command = new ArrayList<>();
        command.add("git");

        // Отключаем экранирование путей, чтобы парсер получал сырые UTF-8 пути.
        command.add("-c");
        command.add("core.quotePath=false");

        if (repo != null) {
            command.add("-C");
            command.add(repo.toString());
        }

        command.addAll(args);
        return command;
    }

    private String readStream(InputStream inputStream) {
        try (InputStream is = inputStream) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}