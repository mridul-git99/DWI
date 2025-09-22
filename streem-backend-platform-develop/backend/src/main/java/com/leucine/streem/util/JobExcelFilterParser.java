package com.leucine.streem.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leucine.streem.dto.JobExcelFilterParams;
import com.leucine.streem.model.helper.PrincipalUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to parse JSON filters into typed parameters for Job Excel static query
 * Replaces dynamic EntityManager query building with parameterized approach
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobExcelFilterParser {
    
    private final ObjectMapper objectMapper;
    
    /**
     * Parse JSON filters and objectId into typed parameters
     * Follows the same pattern as getJobIdsHavingObjectInChoicesForAllParameters
     * 
     * @param filters JSON filter string
     * @param objectId Object ID for resource filtering
     * @param user Principal user for security context
     * @return Parsed filter parameters
     */
    public JobExcelFilterParams parseFilters(String filters, String objectId, PrincipalUser user) {
        log.debug("[parseFilters] Parsing filters: {}, objectId: {}", filters, objectId);
        
        JobExcelFilterParams params = new JobExcelFilterParams();
        
        // Set security filters (always present)
        params.setOrganisationId(user.getOrganisationId());
        params.setFacilityId(user.getCurrentFacilityId());
        
        // Parse objectId using the EXACT same pattern as getJobIdsHavingObjectInChoicesForAllParameters
        if (!Utility.isEmpty(objectId)) {
            String jsonChoices = String.format("""
                [
                    {
                        "objectId": "%s"
                    }
                ]
                """, objectId);
            params.setObjectIdChoicesJson(jsonChoices);
            log.debug("[parseFilters] ObjectId filter applied: {}", jsonChoices);
        }
        
        // Parse JSON filters
        if (!Utility.isEmpty(filters)) {
            try {
                JsonNode filtersNode = objectMapper.readTree(filters);
                if (filtersNode.has("fields") && filtersNode.get("fields").isArray()) {
                    for (JsonNode field : filtersNode.get("fields")) {
                        parseFilterField(field, params);
                    }
                }
                log.debug("[parseFilters] Parsed filter parameters: {}", params);
            } catch (Exception e) {
                log.error("[parseFilters] Error parsing filters: {}", filters, e);
                // Continue with basic security filters even if JSON parsing fails
            }
        }
        
        return params;
    }
    
    /**
     * Parse individual filter field from JSON
     * 
     * @param field JSON field node
     * @param params Filter parameters to populate
     */
    private void parseFilterField(JsonNode field, JobExcelFilterParams params) {
        if (!field.has("field") || !field.has("op")) {
            return;
        }
        
        String fieldName = field.get("field").asText();
        String op = field.get("op").asText();
        
        log.debug("[parseFilterField] Processing field: {}, op: {}", fieldName, op);
        
        switch (fieldName) {
            case "state":
                if ("ANY".equals(op) && field.has("values")) {
                    params.setStateFilter(extractStringList(field.get("values")));
                }
                break;
                
            case "useCaseId":
                if ("EQ".equals(op) && field.has("values") && field.get("values").size() > 0) {
                    params.setUseCaseIdFilter(field.get("values").get(0).asLong());
                }
                break;
                
            case "checklistAncestorId":
                if ("EQ".equals(op) && field.has("values") && field.get("values").size() > 0) {
                    params.setChecklistAncestorIdFilter(field.get("values").get(0).asLong());
                }
                break;
                
            case "code":
                if ("LIKE".equals(op)) {
                    String value = extractLikeValue(field);
                    if (value != null) {
                        params.setCodeFilter(value);
                    }
                }
                break;
                
            case "checklist.name":
                if ("LIKE".equals(op)) {
                    String value = extractLikeValue(field);
                    if (value != null) {
                        params.setChecklistNameFilter(value);
                    }
                }
                break;
                
            case "expectedEndDate":
                if ("LT".equals(op) && field.has("values") && field.get("values").size() > 0) {
                    params.setExpectedEndDateLt(field.get("values").get(0).asLong());
                }
                break;
                
            case "expectedStartDate":
                if ("LT".equals(op) && field.has("values") && field.get("values").size() > 0) {
                    params.setExpectedStartDateLt(field.get("values").get(0).asLong());
                } else if ("GT".equals(op) && field.has("values") && field.get("values").size() > 0) {
                    params.setExpectedStartDateGt(field.get("values").get(0).asLong());
                } else if ("IS_NOT_SET".equals(op)) {
                    params.setExpectedStartDateIsNull(true);
                }
                break;
                
            case "startedAt":
                if ("GOE".equals(op) && field.has("values") && field.get("values").size() > 0) {
                    params.setStartedAtGte(field.get("values").get(0).asLong());
                } else if ("LOE".equals(op) && field.has("values") && field.get("values").size() > 0) {
                    params.setStartedAtLte(field.get("values").get(0).asLong());
                }
                break;
                
            case "createdBy.id":
                if ("EQ".equals(op) && field.has("values") && field.get("values").size() > 0) {
                    params.setCreatedById(field.get("values").get(0).asLong());
                }
                break;
                
            default:
                log.debug("[parseFilterField] Unhandled field: {}", fieldName);
                break;
        }
    }
    
    /**
     * Extract string list from JSON values node
     * 
     * @param valuesNode JSON values array
     * @return String list
     */
    private List<String> extractStringList(JsonNode valuesNode) {
        if (valuesNode == null || !valuesNode.isArray()) {
            return null;
        }
        
        List<String> values = new ArrayList<>();
        for (JsonNode valueNode : valuesNode) {
            values.add(valueNode.asText());
        }
        
        return values;
    }
    
    /**
     * Extract string array from JSON values node
     * 
     * @param valuesNode JSON values array
     * @return String array
     */
    private String[] extractStringArray(JsonNode valuesNode) {
        List<String> list = extractStringList(valuesNode);
        return list != null ? list.toArray(new String[0]) : null;
    }
    
    /**
     * Extract LIKE value from field node
     * Handles both "value" and "values" fields for LIKE operations
     * 
     * @param field JSON field node
     * @return Extracted value for LIKE operation
     */
    private String extractLikeValue(JsonNode field) {
        // Try "value" field first (single value)
        if (field.has("value")) {
            return field.get("value").asText();
        }
        
        // Try "values" array (take first value)
        if (field.has("values") && field.get("values").isArray() && field.get("values").size() > 0) {
            return field.get("values").get(0).asText();
        }
        
        return null;
    }
    
    /**
     * Validate filter parameters for potential issues
     * 
     * @param params Filter parameters to validate
     * @return true if valid, false otherwise
     */
    public boolean validateFilterParams(JobExcelFilterParams params) {
        if (params == null) {
            log.warn("[validateFilterParams] Filter parameters are null");
            return false;
        }
        
        if (params.getOrganisationId() == null) {
            log.warn("[validateFilterParams] Organisation ID is required");
            return false;
        }
        
        // Validate date logic
        if (params.getExpectedStartDateGt() != null && params.getExpectedStartDateLt() != null) {
            if (params.getExpectedStartDateGt() >= params.getExpectedStartDateLt()) {
                log.warn("[validateFilterParams] Invalid date range: start {} >= end {}", 
                    params.getExpectedStartDateGt(), params.getExpectedStartDateLt());
                return false;
            }
        }
        
        // Validate startedAt date logic
        if (params.getStartedAtGte() != null && params.getStartedAtLte() != null) {
            if (params.getStartedAtGte() >= params.getStartedAtLte()) {
                log.warn("[validateFilterParams] Invalid startedAt date range: start {} >= end {}", 
                    params.getStartedAtGte(), params.getStartedAtLte());
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Create filter parameters with only security filters
     * Used as fallback when filter parsing fails
     * 
     * @param user Principal user
     * @return Basic filter parameters
     */
    public JobExcelFilterParams createBasicFilterParams(PrincipalUser user) {
        return new JobExcelFilterParams(user.getOrganisationId(), user.getCurrentFacilityId());
    }
}
