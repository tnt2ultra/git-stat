package com.anri.gitloc.service;

import com.anri.gitloc.config.AppProperties;
import com.anri.gitloc.dto.CsvRowErrorDto;
import com.anri.gitloc.dto.DefaultRegistryStatusDto;
import com.anri.gitloc.dto.ImportResultDto;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Импорт CSV-реестра сервисов.
 */
@Service
@RequiredArgsConstructor
public class CsvImportService {

    private static final String[] EXPECTED_HEADERS = {
            "название группы",
            "название сервиса",
            "url репозитория в корпоративном GitLab"
    };

    private static final String DEFAULT_REGISTRY_FILE = "catena.csv";

    private final AppProperties appProperties;
    private final RegistryPersistenceService registryPersistenceService;

    /**
     * Импортирует CSV-файл, загруженный пользователем.
     *
     * @param file CSV-файл
     * @return результат импорта
     * @throws IOException ошибка чтения файла
     */
    public ImportResultDto importCsv(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return singleError(0, "file", "Файл пуст");
        }

        try (InputStream inputStream = file.getInputStream()) {
            return importCsv(inputStream, file.getOriginalFilename());
        }
    }

    /**
     * Импортирует CSV из потока.
     *
     * @param inputStream поток с CSV-содержимым
     * @param filename    имя файла для диагностических сообщений
     * @return результат импорта
     * @throws IOException ошибка чтения потока
     */
    public ImportResultDto importCsv(InputStream inputStream, String filename) throws IOException {
        if (inputStream == null) {
            return singleError(0, "file", "Поток файла пуст");
        }

        String content = readContentStripBom(inputStream);

        if (content.isBlank()) {
            return singleError(0, "file", "Файл пуст");
        }

        CSVFormat detectFormat = baseFormat();

        try (CSVParser detectParser = CSVParser.parse(content, detectFormat)) {
            List<CSVRecord> allRecords = detectParser.getRecords();

            if (allRecords.isEmpty()) {
                return singleError(1, "header", "В файле нет заголовка");
            }

            List<String> actualHeader = allRecords.get(0).toList();

            if (!actualHeader.equals(List.of(EXPECTED_HEADERS))) {
                return singleError(
                        1,
                        "header",
                        "Неверный заголовок. Ожидаются колонки: " + String.join("; ", EXPECTED_HEADERS)
                );
            }
        }

        CSVFormat dataFormat = baseFormat().builder()
                .setHeader(EXPECTED_HEADERS)
                .setSkipHeaderRecord(true)
                .setAllowMissingColumnNames(false)
                .get();

        List<ParsedRow> rows = new ArrayList<>();
        List<CsvRowErrorDto> errors = new ArrayList<>();

        try (CSVParser parser = CSVParser.parse(content, dataFormat)) {
            for (CSVRecord record : parser) {
                int line = resolveLineNumber(content, record);

                try {
                    String groupName = record.get(EXPECTED_HEADERS[0]);
                    String serviceName = record.get(EXPECTED_HEADERS[1]);
                    String repoUrl = record.get(EXPECTED_HEADERS[2]);

                    rows.add(new ParsedRow(
                            line,
                            groupName,
                            serviceName,
                            repoUrl
                    ));
                } catch (IllegalArgumentException e) {
                    errors.add(new CsvRowErrorDto(
                            line,
                            "row",
                            "Не удалось прочитать строку: " + e.getMessage()
                    ));
                }
            }
        }

        errors.addAll(validateRows(rows));

        if (!errors.isEmpty()) {
            return new ImportResultDto(0, 0, 0, errors);
        }

        return registryPersistenceService.importRows(rows);
    }

    /**
     * Импортирует предустановленный файл catena.csv из classpath.
     *
     * @return результат импорта
     * @throws IOException ошибка чтения ресурса
     */
    public ImportResultDto importDefault() throws IOException {
        ClassPathResource resource = new ClassPathResource(DEFAULT_REGISTRY_FILE);

        if (!resource.exists()) {
            return singleError(
                    0,
                    "file",
                    "Файл " + DEFAULT_REGISTRY_FILE + " не найден в src/main/resources"
            );
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return importCsv(inputStream, DEFAULT_REGISTRY_FILE);
        }
    }

    /**
     * Возвращает информацию о доступности предустановленного файла.
     *
     * @return статус default-реестра
     */
    public DefaultRegistryStatusDto getDefaultRegistryStatus() {
        ClassPathResource resource = new ClassPathResource(DEFAULT_REGISTRY_FILE);
        return new DefaultRegistryStatusDto(DEFAULT_REGISTRY_FILE, resource.exists());
    }

    /**
     * Базовый формат CSV для файла с разделителем ";".
     *
     * @return формат CSV
     */
    private CSVFormat baseFormat() {
        return CSVFormat.DEFAULT.builder()
                .setDelimiter(';')
                .setQuote('"')
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .get();
    }

    /**
     * Формирует результат импорта с одной ошибкой.
     *
     * @param line    номер строки
     * @param field   поле
     * @param message сообщение об ошибке
     * @return результат импорта
     */
    private ImportResultDto singleError(int line, String field, String message) {
        return new ImportResultDto(
                0,
                0,
                0,
                List.of(new CsvRowErrorDto(line, field, message))
        );
    }

    /**
     * Валидирует распарсенные строки.
     *
     * @param rows строки CSV
     * @return список ошибок
     */
    private List<CsvRowErrorDto> validateRows(List<ParsedRow> rows) {
        List<CsvRowErrorDto> errors = new ArrayList<>();
        Set<String> groupServiceKeys = new HashSet<>();
        Set<String> urls = new HashSet<>();

        for (ParsedRow row : rows) {
            if (row.groupName() == null || row.groupName().isBlank()) {
                errors.add(new CsvRowErrorDto(
                        row.line(),
                        EXPECTED_HEADERS[0],
                        "Название группы пустое"
                ));
            }

            if (row.serviceName() == null || row.serviceName().isBlank()) {
                errors.add(new CsvRowErrorDto(
                        row.line(),
                        EXPECTED_HEADERS[1],
                        "Название сервиса пустое"
                ));
            }

            String urlError = validateUrl(row.repoUrl());
            if (urlError != null) {
                errors.add(new CsvRowErrorDto(
                        row.line(),
                        EXPECTED_HEADERS[2],
                        urlError
                ));
            }

            String key = row.groupName() + "\u0000" + row.serviceName();

            if (!groupServiceKeys.add(key)) {
                errors.add(new CsvRowErrorDto(
                        row.line(),
                        "group+service",
                        "Дубликат пары группа и сервис"
                ));
            }

            if (row.repoUrl() != null && !row.repoUrl().isBlank() && !urls.add(row.repoUrl())) {
                errors.add(new CsvRowErrorDto(
                        row.line(),
                        EXPECTED_HEADERS[2],
                        "Дубликат URL репозитория"
                ));
            }
        }

        return errors;
    }

    /**
     * Валидирует URL репозитория GitLab.
     *
     * @param url проверяемый URL
     * @return сообщение об ошибке или null, если URL корректен
     */
    private String validateUrl(String url) {
        if (url == null || url.isBlank()) {
            return "URL пустой";
        }

        try {
            URI uri = new URI(url);

            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                return "Разрешён только протокол HTTPS";
            }

            if (uri.getUserInfo() != null) {
                return "URL не должен содержать учетные данные";
            }

            if (uri.getHost() == null || uri.getHost().isBlank()) {
                return "В URL не задан хост";
            }

            String allowedHost = appProperties.getGit().getAllowedHost();
            if (allowedHost != null && !allowedHost.isBlank()
                    && !allowedHost.equalsIgnoreCase(uri.getHost())) {
                return "Хост не входит в список разрешённых: " + allowedHost;
            }

            if (uri.getPath() == null || uri.getPath().isBlank() || "/".equals(uri.getPath())) {
                return "В URL не задан путь репозитория";
            }

            return null;
        } catch (URISyntaxException e) {
            return "Некорректный URL";
        }
    }

    /**
     * Читает содержимое потока и удаляет BOM, если он есть.
     *
     * @param inputStream входной поток
     * @return содержимое файла без BOM
     * @throws IOException ошибка чтения
     */
    private String readContentStripBom(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        String content = new String(bytes, StandardCharsets.UTF_8);

        if (!content.isEmpty() && content.charAt(0) == '\uFEFF') {
            content = content.substring(1);
        }

        return content;
    }

    /**
     * Вычисляет номер строки начала CSV-записи.
     * <p>
     * Использует символьную позицию записи, чтобы корректно работать даже тогда,
     * когда значения содержат переносы строк внутри кавычек.
     *
     * @param content содержимое CSV-файла
     * @param record  запись CSV
     * @return номер строки, начиная с 1
     */
    private int resolveLineNumber(String content, CSVRecord record) {
        long charPos = record.getCharacterPosition();

        if (charPos < 0 || charPos > content.length()) {
            long recordNumber = record.getRecordNumber();

            if (recordNumber < 1) {
                recordNumber = 1;
            }

            long approximateLine = recordNumber + 1;

            return approximateLine > Integer.MAX_VALUE
                    ? Integer.MAX_VALUE
                    : (int) approximateLine;
        }

        int limit = (int) Math.min(charPos, content.length());
        int line = 1;

        for (int i = 0; i < limit; i++) {
            char c = content.charAt(i);

            if (c == '\n') {
                line++;
            } else if (c == '\r' && (i + 1 >= limit || content.charAt(i + 1) != '\n')) {
                line++;
            }
        }

        return line;
    }

    /**
     * Внутренняя запись распарсенного CSV.
     *
     * @param line        номер строки
     * @param groupName   название группы
     * @param serviceName название сервиса
     * @param repoUrl     URL репозитория
     */
    record ParsedRow(int line, String groupName, String serviceName, String repoUrl) {
    }
}