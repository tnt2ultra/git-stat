package com.anri.gitloc.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Сводная статистика по всем данным.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SummaryDto {

    private long groupCount;
    private long serviceCount;
    private long codeLines;
    private long testLines;
    private long totalLines;
    private Double testToCodeRatio;
}