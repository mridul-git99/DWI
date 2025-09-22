package com.leucine.streem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leucine.streem.collections.EntityObject;
import com.leucine.streem.collections.changelogs.EntityObjectChangeLog;
import com.leucine.streem.collections.changelogs.ChangeLogInputData;
import com.leucine.streem.constant.CollectionMisc;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.FacilityDto;
import com.leucine.streem.dto.GeneratedPdfDataDto;
import com.leucine.streem.service.HtmlTemplateEngine;
import com.leucine.streem.service.IPdfReportBuilder;
import com.leucine.streem.util.DateTimeUtils;
import com.leucine.streem.util.Utility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.leucine.streem.service.CssClasses.*;

/**
 * Builder for Object Audit Log Report PDF type
 * Handles the generation of entity object audit log reports
 */
@Component
@RequiredArgsConstructor
public class ObjectAuditLogReportBuilder implements IPdfReportBuilder {

    @Override
    public String buildReport(GeneratedPdfDataDto variables) throws JsonProcessingException {
        return buildObjectAuditLogSection(variables);
    }

    @Override
    public Type.PdfType getSupportedReportType() {
        return Type.PdfType.OBJECT_AUDIT_LOGS;
    }

    /**
     * Builds the Object Audit Log section for PDF reports
     * @param variables The GeneratedPdfDataDto containing all data needed for the report
     * @return Formatted HTML for the Object Audit Log section
     */
    private String buildObjectAuditLogSection(GeneratedPdfDataDto variables) throws JsonProcessingException {
        StringBuilder objectAuditLogSection = new StringBuilder();
        List<EntityObjectChangeLog> changeLogs = variables.getChangeLogs();
        EntityObject objectType = variables.getObjectType();
        FacilityDto facilityDto = variables.getFacility();
        String filters = variables.getFilters();

        // Add title
        objectAuditLogSection.append(HtmlTemplateEngine.h1("Audit Logs", TITLE));

        // Object Details section
        objectAuditLogSection.append(HtmlTemplateEngine.h4("Object Details", SECTION_TITLE));
        
        StringBuilder detailTableContent = new StringBuilder();
        
        // Object Type
        String objectTypeName = "_________________";
        if (objectType != null && objectType.getObjectType() != null) {
            objectTypeName = objectType.getObjectType().getDisplayName();
        }
        detailTableContent.append(HtmlTemplateEngine.tableRow("Object Type", objectTypeName));
        
        // Object ID
        String objectId = "________________";
        if (objectType != null && objectType.getExternalId() != null) {
            objectId = objectType.getExternalId();
        }
        detailTableContent.append(HtmlTemplateEngine.tableRow("Object ID", objectId));
        
        // Object Name
        String objectName = "________________";
        if (objectType != null && objectType.getDisplayName() != null) {
            objectName = objectType.getDisplayName();
        }
        detailTableContent.append(HtmlTemplateEngine.tableRow("Object Name", objectName));
        
        // Created By (if available)
        if (objectType != null && objectType.getCreatedBy() != null) {
            String createdBy = Utility.getFullNameAndEmployeeId(
                objectType.getCreatedBy().getFirstName(), 
                objectType.getCreatedBy().getLastName(), 
                objectType.getCreatedBy().getEmployeeId());
            detailTableContent.append(HtmlTemplateEngine.tableRow("Created By", createdBy));
        }
        
        // Created At (if available)
        if (objectType != null && objectType.getCreatedAt() != null) {
            String createdAt = formatEpochWithFacilitySettings(objectType.getCreatedAt(), facilityDto, false);
            detailTableContent.append(HtmlTemplateEngine.tableRow("Created At", createdAt));
        }
        
        objectAuditLogSection.append(HtmlTemplateEngine.panel(
            HtmlTemplateEngine.table(detailTableContent.toString(), DETAIL_TABLE), 
            DETAIL_PANEL
        ));

        // Filters Applied section (if filters exist)
        if (filters != null && !filters.isEmpty() && !filters.equals("{}")) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode filtersNode = objectMapper.readTree(filters);
                
                if (filtersNode.has("fields") && filtersNode.get("fields").isArray() && 
                    filtersNode.get("fields").size() > 0) {
                    
                    List<JsonNode> nonDefaultFilters = new ArrayList<>();
                    JsonNode fieldsNode = filtersNode.get("fields");
                    
                    // Collect non-default filters
                    for (int i = 0; i < fieldsNode.size(); i++) {
                        JsonNode field = fieldsNode.get(i);
                        String fieldName = field.has("field") ? field.get("field").asText() : "";
                        
                        if (!"objectId".equals(fieldName) && !"facilityId".equals(fieldName)) {
                            nonDefaultFilters.add(field);
                        }
                    }
                    
                    if (!nonDefaultFilters.isEmpty()) {
                        objectAuditLogSection.append(HtmlTemplateEngine.h4("Filters Applied", SECTION_TITLE));
                        
                        StringBuilder filtersContent = new StringBuilder();
                        
                        for (int i = 0; i < nonDefaultFilters.size(); i++) {
                            JsonNode field = nonDefaultFilters.get(i);
                            String fieldName = field.has("field") ? field.get("field").asText() : "Unknown";
                            String displayFieldName = getDisplayFieldName(fieldName);
                            
                            String operator = field.has("op") ? field.get("op").asText() : "=";
                            // Map operator code to user-friendly display name
                            operator = PdfBuilderServiceHelpers.getOperatorDisplayName(operator);

                            // Extract value, checking for complex objects with label field
                            String value = "Unknown";
                            if (field.has("values") && field.get("values").isArray() && field.get("values").size() > 0) {
                                JsonNode valueNode = field.get("values").get(0);
                                
                                // Check if the value is a complex object with a label field
                                if (valueNode.isObject() && valueNode.has("label")) {
                                    value = valueNode.get("label").asText();
                                } else {
                                    value = valueNode.asText();
                                }
                            } else if (field.has("value")) {
                                // Also check the singular "value" field
                                JsonNode valueNode = field.get("value");
                                
                                // Check if the value is a complex object with a label field
                                if (valueNode.isObject() && valueNode.has("label")) {
                                    value = valueNode.get("label").asText();
                                } else {
                                    value = valueNode.asText();
                                }
                            }
                            
                            String displayValue = getDisplayFilterValue(fieldName, value, changeLogs, facilityDto);
                            
                            filtersContent.append(HtmlTemplateEngine.filterItem(
                                i + 1, displayFieldName, operator, displayValue
                            ));
                        }
                        
                        objectAuditLogSection.append(HtmlTemplateEngine.panel(filtersContent.toString(), DETAIL_PANEL));
                    }
                }
            } catch (Exception e) {
                // If there's an error parsing the filters, just skip this section
            }
        }

        // Add page break before the change history table
        objectAuditLogSection.append(HtmlTemplateEngine.pageBreak());

        // Change History table with fixed layout to ensure proper column widths
        objectAuditLogSection.append(HtmlTemplateEngine.div(
            HtmlTemplateEngine.tableWithStyle(
                HtmlTemplateEngine.thead(
                    HtmlTemplateEngine.tr(
                        HtmlTemplateEngine.thWithStyle("Changed Done To", null, WIDTH_20_PERCENT_STYLE) +
                        HtmlTemplateEngine.thWithStyle("Changed To", null, WIDTH_20_PERCENT_STYLE) +
                        HtmlTemplateEngine.thWithStyle("Changed Done At", null, WIDTH_20_PERCENT_STYLE) +
                        HtmlTemplateEngine.thWithStyle("Changed Done By", null, WIDTH_20_PERCENT_STYLE) +
                        HtmlTemplateEngine.thWithStyle("Reason", null, WIDTH_20_PERCENT_STYLE)
                    )
                ) + HtmlTemplateEngine.tbody(buildChangeHistoryRows(changeLogs, facilityDto)),
                OBJECT_AUDIT_LOG_TABLE,
                "width:100%; table-layout:fixed;"
            ),
            "object-audit-log-table"
        ));

        return objectAuditLogSection.toString();
    }

    /**
     * Formats epoch timestamp using facility timezone and date/time format settings
     * @param epochTime The epoch timestamp to format
     * @param facilityDto The facility containing timezone and format settings
     * @param isDateOnly Whether to format as date only (true) or datetime (false)
     * @return Formatted date/time string using facility settings
     */
    private String formatEpochWithFacilitySettings(Long epochTime, FacilityDto facilityDto, boolean isDateOnly) {
        // Convert epoch to facility timezone
        var zonedDateTime = DateTimeUtils.convertEpochToZonedDateTime(epochTime, facilityDto.getTimeZone());
        
        // Get appropriate format from facility
        String format = isDateOnly ? facilityDto.getDateFormat() : facilityDto.getDateTimeFormat();
        
        // Format using facility format
        return zonedDateTime.format(DateTimeFormatter.ofPattern(format));
    }

    /**
     * Builds the change history table rows
     */
    private String buildChangeHistoryRows(List<EntityObjectChangeLog> changeLogs, FacilityDto facilityDto) {
        StringBuilder rows = new StringBuilder();
        
        // Sort change logs by modification time (newest first)
        if (changeLogs != null && !changeLogs.isEmpty()) {

            for (EntityObjectChangeLog changeLog : changeLogs) {
                String entityName = "-";
                String newValue = "-";

                // QR Code (shortcode) change
                if (changeLog.getShortCode() != null && changeLog.getShortCode().getNewShortCode() != null) {
                    entityName = "QR Code";
                    newValue = "New QR Code";
                }
                // Usage Status change
                else if (changeLog.getUsageStatus() != null && changeLog.getUsageStatus().getNewStatus() != null) {
                    entityName = "Usage Status";
                    Integer status = changeLog.getUsageStatus().getNewStatus();
                    if (status == 1) {
                        newValue = "Active";
                    } else if (status == 7) {
                        newValue = "Archived";
                    } else {
                        newValue = String.valueOf(status);
                    }
                }
                // Default logic for other changes
                else {
                    entityName = !Utility.isEmpty(changeLog.getEntityDisplayName()) ?
                        HtmlUtils.htmlEscape(changeLog.getEntityDisplayName()) : "-";

                    if (changeLog.getNewEntityData() != null && !changeLog.getNewEntityData().isEmpty()) {
                        List<String> values = new ArrayList<>();
                        for (ChangeLogInputData inputData : changeLog.getNewEntityData()) {
                            if (inputData != null && inputData.input() != null) {
                                // Check if this is a date/datetime field that needs formatting
                                CollectionMisc.ChangeLogInputType inputType = changeLog.getEntityInputType();
                                String inputValue = inputData.input();
                                if (inputType == CollectionMisc.ChangeLogInputType.DATE ||
                                    inputType == CollectionMisc.ChangeLogInputType.DATE_TIME) {
                                    try {
                                        Long epochTime = Long.parseLong(inputValue);
                                        if (inputType == CollectionMisc.ChangeLogInputType.DATE) {
                                            values.add(formatEpochWithFacilitySettings(epochTime, facilityDto, true));
                                        } else {
                                            values.add(formatEpochWithFacilitySettings(epochTime, facilityDto, false));
                                        }
                                    } catch (NumberFormatException e) {
                                        values.add(HtmlUtils.htmlEscape(inputValue));
                                    }
                                } else {
                                    values.add(HtmlUtils.htmlEscape(inputValue));
                                }
                            }
                        }
                        newValue = String.join(", ", values);
                    }
                }

                String modifiedAt = changeLog.getModifiedAt() != null ?
                    formatEpochWithFacilitySettings(changeLog.getModifiedAt(), facilityDto, false) : "-";
                
                String modifiedBy = changeLog.getModifiedBy() != null ?
                    HtmlUtils.htmlEscape(Utility.getFullNameAndEmployeeId(
                        changeLog.getModifiedBy().getFirstName(), 
                        changeLog.getModifiedBy().getLastName(), 
                        changeLog.getModifiedBy().getEmployeeId())) : "-";
                
                String reason = !Utility.isEmpty(changeLog.getReason()) ?
                    HtmlUtils.htmlEscape(changeLog.getReason()) : "-";
                
                // Format reason field if info is available
                if (changeLog.getInfo() != null) {
                    String processName = changeLog.getInfo().processName();
                    String processCode = changeLog.getInfo().processCode();
                    String jobCode = changeLog.getInfo().jobCode();
                    
                    if (processName != null && processCode != null) {
                        reason = "Changed as per Process: " + processName +
                                " (ID: " + processCode + ")" +
                                (jobCode != null ? " (JOB ID: " + jobCode + ")" : "");
                    }
                }
                
                rows.append(HtmlTemplateEngine.tr(
                    HtmlTemplateEngine.td(entityName, null) +
                    HtmlTemplateEngine.td(newValue, null) +
                    HtmlTemplateEngine.td(modifiedAt, null) +
                    HtmlTemplateEngine.td(modifiedBy, null) +
                    HtmlTemplateEngine.td(reason, null)
                ));
            }
        } else {
            rows.append(HtmlTemplateEngine.tr(
                HtmlTemplateEngine.td("No change history available", null) +
                HtmlTemplateEngine.td("", null) +
                HtmlTemplateEngine.td("", null) +
                HtmlTemplateEngine.td("", null) +
                HtmlTemplateEngine.td("", null)
            ));
        }
        
        return rows.toString();
    }

    /**
     * Helper method to get display field name for filters
     * @param fieldName The original field name
     * @return User-friendly display name
     */
    private String getDisplayFieldName(String fieldName) {
        switch (fieldName) {
            case "modifiedAt":
                return "Changed Done At";
            case "entityId":
                return "Changed Done To";
            case "modifiedBy.id":
                return "Changed Done By";
            default:
                return fieldName;
        }
    }

    /**
     * Helper method to get display filter value
     * @param fieldName The field name
     * @param value The original value
     * @param changeLogs The list of change logs to search for additional information
     * @param facilityDto The facility containing timezone and format settings
     * @return User-friendly display value
     */
    private String getDisplayFilterValue(String fieldName, String value, List<EntityObjectChangeLog> changeLogs, FacilityDto facilityDto) throws JsonProcessingException {
        if (value == null || value.isEmpty()) {
            return "_________________";
        }
        
        // Check if the value is a JSON object with label and value fields
        if (value.startsWith("{") && value.endsWith("}") && value.contains("\"label\"")) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode valueNode = objectMapper.readTree(value);
            if (valueNode.has("label")) {
                // Return the label instead of the raw value
                return valueNode.get("label").asText();
            }
        }
        
        if ("modifiedBy.id".equals(fieldName)) {
            // Try to find user info in change logs
            for (EntityObjectChangeLog log : changeLogs) {
                if (log.getModifiedBy() != null && value.equals(log.getModifiedBy().getId())) {
                    return Utility.getFullNameAndEmployeeId(
                        log.getModifiedBy().getFirstName(), 
                        log.getModifiedBy().getLastName(), 
                        log.getModifiedBy().getEmployeeId());
                }
            }
        } else if ("modifiedAt".equals(fieldName)) {
            // Format timestamp as date/time using facility settings
            try {
                long timestamp = Long.parseLong(value);
                return formatEpochWithFacilitySettings(timestamp, facilityDto, false);
            } catch (NumberFormatException e) {
                // If it's not a valid number, just use the original value
            }
        } else if ("entityId".equals(fieldName)) {
            // Try to get entity display name
            for (EntityObjectChangeLog log : changeLogs) {
                if (value.equals(log.getEntityId())) {
                    return log.getEntityDisplayName() != null ?
                          log.getEntityDisplayName() : value;
                }
            }
            
            // If we couldn't find the entity in the change logs, check if it's a complex object
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode valueNode = objectMapper.readTree(value);
                if (valueNode.has("label")) {
                    // Return the label instead of the raw value
                    return valueNode.get("label").asText();
                }
            } catch (Exception e) {
                // Not a JSON object, continue with normal processing
            }
        } else if ("entityDisplayName".equals(fieldName)) {
            // For entity display name, just return the value directly
            return value;
        } else if ("entityExternalId".equals(fieldName)) {
            // For entity external ID, just return the value directly
            return value;
        } else if ("reason".equals(fieldName)) {
            // For reason, just return the value directly
            return value;
        }
        
        return value;
    }
}
