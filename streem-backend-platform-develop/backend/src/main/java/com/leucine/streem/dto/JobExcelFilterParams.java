package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO class to hold all filter parameters for Job Excel static query
 * Used to replace dynamic EntityManager queries with parameterized static query
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobExcelFilterParams {
    
    // State filter - list for IN operation
    private List<String> stateFilter;
    
    // Direct field filters
    private Long useCaseIdFilter;
    private Long checklistAncestorIdFilter;
    private String codeFilter;
    private String checklistNameFilter;
    
    // Date filters
    private Long expectedEndDateLt;
    private Long expectedStartDateGt;
    private Long expectedStartDateLt;
    private Boolean expectedStartDateIsNull;
    
    // Started date filters
    private Long startedAtGte;
    private Long startedAtLte;
    
    // ObjectId filter (using same pattern as getJobIdsHavingObjectInChoicesForAllParameters)
    private String objectIdChoicesJson;
    
    // Created by filter
    private Long createdById;
    
    // Security filters (always present)
    private Long organisationId;
    private Long facilityId;
    
    /**
     * Constructor for basic security filters
     */
    public JobExcelFilterParams(Long organisationId, Long facilityId) {
        this.organisationId = organisationId;
        this.facilityId = facilityId;
    }
    
    /**
     * Check if any filters are applied (excluding security filters)
     */
    public boolean hasFilters() {
        return stateFilter != null ||
               useCaseIdFilter != null ||
               checklistAncestorIdFilter != null ||
               codeFilter != null ||
               checklistNameFilter != null ||
               expectedEndDateLt != null ||
               expectedStartDateGt != null ||
               expectedStartDateLt != null ||
               expectedStartDateIsNull != null ||
               startedAtGte != null ||
               startedAtLte != null ||
               objectIdChoicesJson != null ||
               createdById != null;
    }
    
    /**
     * Check if objectId filter is applied
     */
    public boolean hasObjectIdFilter() {
        return objectIdChoicesJson != null && !objectIdChoicesJson.trim().isEmpty();
    }
    
    /**
     * Check if date filters are applied
     */
    public boolean hasDateFilters() {
        return expectedEndDateLt != null ||
               expectedStartDateGt != null ||
               expectedStartDateLt != null ||
               expectedStartDateIsNull != null ||
               startedAtGte != null ||
               startedAtLte != null;
    }
    
    /**
     * Check if text search filters are applied
     */
    public boolean hasTextFilters() {
        return codeFilter != null || checklistNameFilter != null;
    }
}
