package com.anri.gitloc.controller;

import com.anri.gitloc.dto.ImportResultDto;
import com.anri.gitloc.service.CsvImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * API реестра сервисов.
 */
@RestController
@RequestMapping("/api/v1/registry")
@RequiredArgsConstructor
public class RegistryController {

    private final CsvImportService csvImportService;

    /**
     * Импорт CSV-реестра.
     *
     * @param file CSV-файл
     * @return результат импорта
     * @throws IOException ошибка чтения файла
     */
    @PostMapping("/import")
    public ResponseEntity<ImportResultDto> importCsv(@RequestParam("file") MultipartFile file) throws IOException {
        ImportResultDto result = csvImportService.importCsv(file);

        if (result.getErrors().isEmpty()) {
            return ResponseEntity.ok(result);
        }

        return ResponseEntity.badRequest().body(result);
    }
}