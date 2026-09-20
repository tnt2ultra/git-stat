package com.anri.gitloc.service;

import com.anri.gitloc.domain.JavaCategory;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Классификатор Java-файлов на код, тесты и исключённые файлы.
 */
@Component
public class JavaFileClassifier {

    private static final String[] GENERATED_MARKERS = {
            "/generated/",
            "/gen/",
            "generated-sources",
            "generated-test-sources",
            "/target/generated",
            "/build/generated",
            "/openapi/",
            "/swagger/",
            "/protobuf/",
            "/proto/",
            "/grpc/",
            "/flyway/",
            "/liquibase/",
            "/db/migration/",
            "/migration/",
            "/migrations/"
    };

    private static final String[] TEST_PATH_MARKERS = {
            "/test/",
            "/tests/",
            "/src/test/",
            "/it/",
            "/e2e/",
            "/integration-test/",
            "/integration-tests/",
            "/functional-test/",
            "/functional-tests/"
    };

    private static final String[] TEST_FILE_SUFFIXES = {
            "test.java",
            "tests.java",
            "it.java",
            "testcase.java",
            "spec.java",
            "specification.java"
    };

    /**
     * Определяет категорию файла по пути.
     *
     * @param path путь файла в репозитории
     * @return категория файла
     */
    public JavaCategory classify(String path) {
        if (path == null || path.isBlank()) {
            return JavaCategory.EXCLUDED;
        }

        String lower = path.toLowerCase(Locale.ROOT);

        if (!lower.endsWith(".java")) {
            return JavaCategory.EXCLUDED;
        }

        if (isGenerated(lower)) {
            return JavaCategory.EXCLUDED;
        }

        if (isTest(lower)) {
            return JavaCategory.TEST;
        }

        return JavaCategory.CODE;
    }

    private boolean isGenerated(String lowerPath) {
        for (String marker : GENERATED_MARKERS) {
            if (lowerPath.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTest(String lowerPath) {
        for (String marker : TEST_PATH_MARKERS) {
            if (lowerPath.contains(marker)) {
                return true;
            }
        }

        for (String suffix : TEST_FILE_SUFFIXES) {
            if (lowerPath.endsWith(suffix)) {
                return true;
            }
        }

        return false;
    }
}