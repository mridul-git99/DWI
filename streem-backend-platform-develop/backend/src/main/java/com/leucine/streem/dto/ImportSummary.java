package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportSummary {
    private List<ImportResult> results;
    private int totalRows;
    private int created;
    private int updated;
    private int skipped;
    private int failed;
}
