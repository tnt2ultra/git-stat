package com.anri.gitloc.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ошибка строки CSV.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CsvRowErrorDto {

    private int line;
    private String field;
    private String message;
}