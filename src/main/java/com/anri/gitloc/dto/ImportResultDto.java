package com.anri.gitloc.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Результат импорта CSV-реестра.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImportResultDto {

    private int groupsCreated;
    private int servicesCreated;
    private int servicesUpdated;
    private List<CsvRowErrorDto> errors;
}