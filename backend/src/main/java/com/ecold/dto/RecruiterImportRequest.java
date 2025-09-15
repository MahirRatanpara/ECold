package com.ecold.dto;

import lombok.Data;

@Data
public class RecruiterImportRequest {
    private String csvData;
    private String source; // CSV, EXCEL, GOOGLE_SHEET
}