package com.leucine.streem.dto;

import com.leucine.streem.collections.EntityObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {
    private EntityObject entityObject;
    private ImportAction action;
    private String message;
    private int rowNumber;
}
