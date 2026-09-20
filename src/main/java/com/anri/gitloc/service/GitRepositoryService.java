package com.anri.gitloc.service;

import com.anri.gitloc.config.AppProperties;
import com.anri.gitloc.domain.GitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

/**
 * Сервис низкоуровневых операций с Git-репозиторием.
 */
@Service
@RequiredArgsConstructor
public class GitRepositoryService {

    private static final Pattern BRANCH_PATTERN = Pattern.compile("^[A-Za-z0-9._/-]+$");

    private final AppProperties appProperties;
    private final GitCommandService gitCommandService;
    private final GitStorageService gitStorageService;

    /**
     * Ссылка на Java-файл в дереве коммита: путь и oid содержимого.
     *
     * @param path путь файла относительно корня репозитория
     * @param oid  идентификатор blob-объекта
     */
    public record JavaFileRef(String path, String oid) {
    }

    /**
     * Клонирует или обновляет bare-репозиторий сервиса.
     *
     * @param service сервис
     */
    public void ensureRepository(GitService service) {
        Path repoPath = gitStorageService.getRepoPath(service.getId());

        if (!Files.exists(repoPath.resolve("HEAD"))) {
            gitStorageService.deleteRepo(service.getId());

            try {
                Files.createDirectories(repoPath.getParent());
            } catch (IOException e) {
                throw new GitException("Не удалось создать каталог для репозитория", e);
            }

            String url = buildAuthenticatedUrl(service.getRepoUrl());
            gitCommandService.run(null, List.of("clone", "--bare", url, repoPath.toString()));
        } else {
            gitCommandService.run(repoPath, List.of("remote", "update", "--prune"));
        }
    }

    /**
     * Возвращает ветку для анализа.
     * <p>
     * Если requestedBranch пустая, возвращается ветка по умолчанию.
     * Иначе выполняется валидация и проверка существования ветки.
     *
     * @param repoPath         путь к репозиторию
     * @param requestedBranch  запрошенная ветка или null/пустая строка для default
     * @return имя ветки для анализа
     */
    public String resolveBranch(Path repoPath, String requestedBranch) {
        if (requestedBranch == null || requestedBranch.isBlank()) {
            String detected = defaultBranch(repoPath);
            if (detected == null || detected.isBlank()) {
                throw new GitException("Не удалось определить ветку по умолчанию");
            }
            return detected;
        }

        String branch = requestedBranch.trim();
        validateBranchName(branch);

        String commit = tryRevParse(repoPath, branch + "^{commit}");

        if (commit.isBlank()) {
            commit = tryRevParse(repoPath, "refs/heads/" + branch + "^{commit}");
        }

        if (commit.isBlank()) {
            throw new GitException("Ветка не найдена в репозитории: " + branch);
        }

        return branch;
    }

    /**
     * Возвращает SHA текущего HEAD для указанной ветки.
     *
     * @param repoPath путь к репозиторию
     * @param branch   ветка
     * @return SHA
     */
    public String headSha(Path repoPath, String branch) {
        String ref = toRef(branch);
        return gitCommandService.run(repoPath, List.of("rev-parse", ref + "^{commit}")).trim();
    }

    /**
     * Возвращает имя default branch.
     *
     * @param repoPath путь к репозиторию
     * @return имя ветки или HEAD
     */
    public String defaultBranch(Path repoPath) {
        try {
            String value = gitCommandService.run(repoPath, List.of("symbolic-ref", "--short", "HEAD")).trim();
            if (!value.isBlank()) {
                return value;
            }
        } catch (GitException e) {
            // fallback ниже
        }

        try {
            String value = gitCommandService.run(repoPath, List.of("rev-parse", "--abbrev-ref", "HEAD")).trim();
            if (!value.isBlank()) {
                return value;
            }
        } catch (GitException e) {
            // fallback ниже
        }

        return "HEAD";
    }

    /**
     * Возвращает вывод git log с numstat для выбранной ветки.
     * <p>
     * Pathspec намеренно не используется, чтобы отбор .java-файлов был
     * идентичен отбору в parseSnapshot и не зависел от семантики pathspec git.
     * Фильтрация по расширению выполняется на стороне Java-парсера.
     *
     * @param repoPath путь к репозиторию
     * @param branch   ветка
     * @return вывод команды
     */
    public String logJavaNumstat(Path repoPath, String branch) {
        String ref = toRef(branch);

        return gitCommandService.run(
                repoPath,
                List.of(
                        "log",
                        "--no-merges",
                        "--no-renames",
                        "--reverse",
                        "--date=iso-strict",
                        "--pretty=format:%H%x09%ad%x09%ae%x09%an%x09%s",
                        "--numstat",
                        ref
                )
        );
    }

    /**
     * Возвращает список Java-файлов в выбранной ветке вместе с их oid.
     *
     * @param repoPath путь к репозиторию
     * @param branch   ветка
     * @return список ссылок на Java-файлы
     */
    public List<JavaFileRef> listJavaFiles(Path repoPath, String branch) {
        String ref = toRef(branch);

        String output = gitCommandService.run(
                repoPath,
                List.of("ls-tree", "-r", ref)
        );

        List<JavaFileRef> result = new ArrayList<>();

        for (String line : output.lines().toList()) {
            if (line.isBlank()) {
                continue;
            }

            // Формат строки: <mode> SP <type> SP <oid> TAB <path>
            int tab = line.indexOf('\t');
            if (tab < 0) {
                continue;
            }

            String meta = line.substring(0, tab);
            String path = line.substring(tab + 1);

            String[] parts = meta.split(" ");
            if (parts.length < 3) {
                continue;
            }

            String type = parts[1];
            String oid = parts[2];

            if (!"blob".equals(type)) {
                continue;
            }

            if (!path.toLowerCase(Locale.ROOT).endsWith(".java")) {
                continue;
            }

            result.add(new JavaFileRef(path, oid));
        }

        return result;
    }

    /**
     * Считает количество строк для списка Java-файлов через git cat-file --batch.
     * <p>
     * Запросы выполняются по oid, а не по branch:path, что исключает сбои
     * резолвинга и молчаливое получение нуля строк.
     *
     * @param repoPath путь к репозиторию
     * @param files    список ссылок на файлы
     * @return карта путь -> количество строк
     */
    public Map<String, Long> countLines(Path repoPath, List<JavaFileRef> files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Long> result = new LinkedHashMap<>();

        Process process = gitCommandService.startProcess(repoPath, List.of("cat-file", "--batch"));

        CompletableFuture<String> stderrFuture =
                CompletableFuture.supplyAsync(() -> readStream(process.getErrorStream()));

        try (OutputStream os = process.getOutputStream();
             InputStream is = process.getInputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {

            for (JavaFileRef file : files) {
                writer.write(file.oid());
                writer.newLine();
                writer.flush();

                String header = readLine(is);
                if (header == null) {
                    break;
                }

                if (header.equals("missing")) {
                    result.put(file.path(), 0L);
                    continue;
                }

                String[] parts = header.split(" ");
                if (parts.length < 3) {
                    result.put(file.path(), 0L);
                    continue;
                }

                long size;
                try {
                    size = Long.parseLong(parts[2]);
                } catch (NumberFormatException e) {
                    result.put(file.path(), 0L);
                    continue;
                }

                long lines = readBlobAndCountLines(is, size);

                // После содержимого объекта git cat-file --batch выводит перевод строки.
                is.read();

                if ("blob".equals(parts[1])) {
                    result.put(file.path(), lines);
                } else {
                    result.put(file.path(), 0L);
                }
            }

            writer.flush();
            os.close();

            int exitCode = process.waitFor();
            String stderr = stderrFuture.get();

            if (exitCode != 0) {
                throw new GitException(gitCommandService.sanitize(stderr));
            }

            return result;
        } catch (IOException e) {
            process.destroyForcibly();
            throw new GitException(gitCommandService.sanitize(e.getMessage()), e);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new GitException("Подсчёт строк прерван", e);
        } catch (ExecutionException e) {
            process.destroyForcibly();
            throw new GitException(gitCommandService.sanitize(String.valueOf(e.getCause())), e);
        }
    }

    private String buildAuthenticatedUrl(String repoUrl) {
        String token = appProperties.getGit().getToken();

        if (token == null || token.isBlank()) {
            return repoUrl;
        }

        try {
            URI original = new URI(repoUrl);
            URI authenticated = new URI(
                    original.getScheme(),
                    "oauth2:" + token,
                    original.getHost(),
                    original.getPort(),
                    original.getPath(),
                    original.getQuery(),
                    original.getFragment()
            );
            return authenticated.toString();
        } catch (URISyntaxException e) {
            throw new GitException("Некорректный URL репозитория", e);
        }
    }

    private String tryRevParse(Path repoPath, String rev) {
        try {
            return gitCommandService.run(repoPath, List.of("rev-parse", "--verify", "--quiet", rev)).trim();
        } catch (GitException e) {
            return "";
        }
    }

    private void validateBranchName(String branch) {
        if (branch == null || branch.isBlank()) {
            throw new GitException("Название ветки пустое");
        }

        if (branch.length() > 255) {
            throw new GitException("Название ветки слишком длинное");
        }

        if (!BRANCH_PATTERN.matcher(branch).matches()) {
            throw new GitException(
                    "Некорректное название ветки. Допустимы латинские буквы, цифры и символы . _ / -"
            );
        }

        if (branch.startsWith("-")) {
            throw new GitException("Название ветки не должно начинаться с дефиса");
        }

        if (branch.startsWith("/") || branch.endsWith("/")) {
            throw new GitException("Название ветки не должно начинаться или заканчиваться слэшем");
        }

        if (branch.contains("..")) {
            throw new GitException("Название ветки не должно содержать две точки подряд");
        }

        if (branch.contains("//")) {
            throw new GitException("Название ветки не должно содержать два слэша подряд");
        }

        if (branch.endsWith(".lock")) {
            throw new GitException("Название ветки не должно заканчиваться на .lock");
        }
    }

    private String toRef(String branch) {
        if (branch == null || branch.isBlank()) {
            return "HEAD";
        }

        // Если имя ветки начинается с дефиса, явно указываем refs/heads,
        // чтобы Git не воспринял его как опцию.
        if (branch.startsWith("-")) {
            return "refs/heads/" + branch;
        }

        return branch;
    }

    private String readLine(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        int b;

        while ((b = inputStream.read()) != -1) {
            if (b == '\n') {
                break;
            }
            if (b != '\r') {
                sb.append((char) b);
            }
        }

        if (sb.isEmpty() && b == -1) {
            return null;
        }

        return sb.toString();
    }

    private long readBlobAndCountLines(InputStream inputStream, long size) throws IOException {
        byte[] buffer = new byte[8192];
        long read = 0;
        long lines = 0;
        boolean lastCharNewline = true;

        while (read < size) {
            int toRead = (int) Math.min(buffer.length, size - read);
            int r = inputStream.read(buffer, 0, toRead);

            if (r == -1) {
                throw new EOFException("Неожиданный конец потока git cat-file");
            }

            for (int i = 0; i < r; i++) {
                if (buffer[i] == '\n') {
                    lines++;
                    lastCharNewline = true;
                } else {
                    lastCharNewline = false;
                }
            }

            read += r;
        }

        if (size > 0 && !lastCharNewline) {
            lines++;
        }

        return lines;
    }

    private String readStream(InputStream inputStream) {
        try (InputStream is = inputStream) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GitException(gitCommandService.sanitize(e.getMessage()), e);
        }
    }
}