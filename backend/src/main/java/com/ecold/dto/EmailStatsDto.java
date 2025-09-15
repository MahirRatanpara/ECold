package com.ecold.dto;

import lombok.Data;

@Data
public class EmailStatsDto {
    private Long totalSent;
    private Long totalFailed;
    private Long totalScheduled;
    private Double successRate;
    private Long todaysSent;
    private Long thisWeekSent;
}