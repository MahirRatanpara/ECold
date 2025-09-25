package com.ecold.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateWeekSummaryDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long recruitersCount;
    private String dateRangeLabel; // e.g., "Jan 1-7, 2024" or "Sep 16-22, 2024"
}