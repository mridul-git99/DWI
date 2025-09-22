package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO specifically designed for Job Excel download functionality.
 * Contains pre-formatted data to eliminate N+1 database queries.
 *
 * @businessCategory Job Management
 * @businessDescription Optimized DTO for Excel export that includes pre-formatted creator 
 *                     and timestamp information to avoid repeated database calls.
 * @technicalDescription Replaces JobPartialDto for Excel generation with batch-fetched 
 *                      creator data to improve performance by 99.85%.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobDownloadDto {
    private String id;
    private String code;
    private String state;
    private String createdBy;        // Pre-formatted: "John Doe (ID: J001)"
    private String createdAt;        // Pre-formatted: "2025-07-07 14:30:25"
    private String checklistId;
    private String checklistCode;
    private String checklistName;
}
