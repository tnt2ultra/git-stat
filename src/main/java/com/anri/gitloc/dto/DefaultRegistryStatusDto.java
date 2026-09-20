package com.anri.gitloc.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Статус наличия предустановленного CSV-реестра.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DefaultRegistryStatusDto {

    /**
     * Имя файла в classpath.
     */
    private String filename;

    /**
     * Доступен ли файл для импорта.
     */
    private boolean available;
}