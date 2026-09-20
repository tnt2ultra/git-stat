package com.anri.gitloc.domain;

/**
 * Категория Java-файла.
 */
public enum JavaCategory {
    /**
     * Продуктовый код.
     */
    CODE,

    /**
     * Тестовый код.
     */
    TEST,

    /**
     * Файл исключён из статистики, например generated code или миграции.
     */
    EXCLUDED
}