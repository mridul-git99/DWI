package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobLogsColumnDto {
    private String id;
    private String name;
    private List<ChecklistJobLogColumnDto> jobLogColumns  = new ArrayList<>();
}
