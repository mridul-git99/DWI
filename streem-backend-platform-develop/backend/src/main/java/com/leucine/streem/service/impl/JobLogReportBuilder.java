package com.leucine.streem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leucine.streem.collections.CustomView;
import com.leucine.streem.collections.CustomViewColumn;
import com.leucine.streem.collections.CustomViewFilter;
import com.leucine.streem.collections.JobLog;
import com.leucine.streem.collections.JobLogData;
import com.leucine.streem.collections.JobLogMediaData;
import com.leucine.streem.collections.JobLogResource;
import com.leucine.streem.constant.Type;
import com.leucine.streem.model.ChecklistPropertyValue;
import com.leucine.streem.dto.FacilityDto;
import com.leucine.streem.dto.GeneratedPdfDataDto;
import com.leucine.streem.dto.ResourceParameterChoiceDto;
import com.leucine.streem.model.Checklist;
import com.leucine.streem.model.ChecklistPropertyValue;
import com.leucine.streem.model.Property;
import com.leucine.streem.model.helper.search.SearchCriteria;
import com.leucine.streem.model.helper.search.SearchFilter;
import com.leucine.streem.service.CssClasses;
import com.leucine.streem.service.HtmlTemplateEngine;
import com.leucine.streem.service.IPdfReportBuilder;
import com.leucine.streem.util.DateTimeUtils;
import com.leucine.streem.util.PdfGeneratorUtil;
import com.leucine.streem.util.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Builder for Job Log Report PDF type
 * Handles the generation of tabular job log reports
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobLogReportBuilder implements IPdfReportBuilder {

    private static final int COLS_PER_SHEET = 20;
    
    // System filters that should be excluded from display (only truly internal filters)
    private static final Set<String> SYSTEM_FILTERS = Set.of(
        "logs.triggerType", "logs.identifierValue", "facilityId", "checklistId"
    );
    
    private final ObjectMapper objectMapper;

    @Override
    public String buildReport(GeneratedPdfDataDto variables) throws JsonProcessingException {
        // Get job log type for dynamic handling
        Type.JobLogType jobLogType = variables.getJobLogType();
        
        StringBuilder content = new StringBuilder();
        
        // Build header section using dynamic approach
        content.append(buildHeaderSection(variables, jobLogType));
        
        // Build filters section using dynamic approach
        content.append(buildFiltersSection(variables, jobLogType));
        
        // Add page break using HtmlTemplateEngine
        content.append(HtmlTemplateEngine.pageBreakWithStyle(CssClasses.PAGE_BREAK_AFTER_STYLE));
        
        // Build job logs table using dynamic approach
        content.append(buildJobLogsTable(variables, jobLogType));
        
        return content.toString();
    }

    @Override
    public Type.PdfType getSupportedReportType() {
        return Type.PdfType.JOB_LOGS;
    }

    private Map<String, ResourceParameterChoiceDto> buildResourceParameterChoiceMap(List<JobLog> jobLogs) {
        Map<String, ResourceParameterChoiceDto> resourceParameterChoiceDtoMap = new HashMap<>();
        for (JobLog jobLog : jobLogs) {
            Map<String, ResourceParameterChoiceDto> jobResourceMap = jobLog.getLogs().stream()
                .filter(jobLogData -> jobLogData.getTriggerType() == Type.JobLogTriggerType.RESOURCE)
                .flatMap(jobLogData -> jobLogData.getResourceParameters().values().stream())
                .flatMap(jobLogResource -> jobLogResource.getChoices().stream())
                .collect(Collectors.toMap(
                    ResourceParameterChoiceDto::getObjectId,
                    resourceParameterChoiceDto -> resourceParameterChoiceDto,
                    (existingChoice, newChoice) -> newChoice
                ));
            resourceParameterChoiceDtoMap.putAll(jobResourceMap);
        }
        return resourceParameterChoiceDtoMap;
    }

    private String buildHeaderSection(GeneratedPdfDataDto variables, Type.JobLogType jobLogType) {
        // Dynamic title based on job log type and custom view name
        String title = getReportTitle(variables, jobLogType);
        String titleHtml = HtmlTemplateEngine.h1(title, CssClasses.TITLE);
        
        // Process details section
        String processDetailsTitle = HtmlTemplateEngine.h4("Process Details", CssClasses.SECTION_TITLE);
        
        // Build detail table using template engine
        Map<String, String> details = buildDetailsMap(variables, jobLogType);
        String detailTable = HtmlTemplateEngine.keyValueTable(details, CssClasses.DETAIL_TABLE);
        String detailPanel = HtmlTemplateEngine.panel(detailTable, CssClasses.DETAIL_PANEL);
        
        return titleHtml + processDetailsTitle + detailPanel;
    }

    private void buildHeaderSection(StringBuilder jobLogSection, Checklist checklistInfo, FacilityDto facilityDto,
                                   List<ChecklistPropertyValue> checklistPropertyValues, 
                                   String customViewName, String processName) {
        
        String viewName = customViewName != null ? customViewName : "Job Logs";
        String procName = processName != null ? processName : 
                         (checklistInfo != null ? checklistInfo.getName() : "");
        
        jobLogSection.append("<h1 class=\"title\">").append(safeHtmlEscape(viewName)).append("</h1>")
            .append("<h4 class=\"section-title\">Process Details</h4>")
            .append("<div class=\"detail-panel\">")
            .append("<table class=\"detail-table\">")
            .append("<tr><th>Process ID:</th><td>")
            .append(safeHtmlEscape(checklistInfo != null ? checklistInfo.getCode() : ""))
            .append("</td></tr>")
            .append("<tr><th>Process Name:</th><td>")
            .append(safeHtmlEscape(procName))
            .append("</td></tr>")
            .append("<tr><th>Facility Name:</th><td>")
            .append(safeHtmlEscape(facilityDto != null ? facilityDto.getName() : ""))
            .append("</td></tr>");

        // Add checklist properties
        if (checklistPropertyValues != null) {
            for (ChecklistPropertyValue checklistPropertyValue : checklistPropertyValues) {
                if (checklistPropertyValue != null && checklistPropertyValue.getFacilityUseCasePropertyMapping() != null) {
                    Property property = checklistPropertyValue.getFacilityUseCasePropertyMapping().getProperty();
                    if (property != null) {
                        jobLogSection.append("<tr><th>")
                            .append(safeHtmlEscape(property.getLabel()))
                            .append(":</th><td>")
                            .append(safeHtmlEscape(checklistPropertyValue.getValue()))
                            .append("</td></tr>");
                    }
                }
            }
        }
        jobLogSection.append("</table></div>");
    }

    private String getReportTitle(GeneratedPdfDataDto variables, Type.JobLogType jobLogType) {
        // Base title based on job log type
        String baseTitle = jobLogType == Type.JobLogType.ASSETS_LOGS ? 
                          "Asset Job Logs" : "Job Logs";
        
        // If custom view name exists, append it to the base title
        if (variables.getCustomViewName() != null) {
            return baseTitle + " - " + variables.getCustomViewName();
        }
        
        // Return base title if no custom view name
        return baseTitle;
    }

    private Map<String, String> buildDetailsMap(GeneratedPdfDataDto variables, Type.JobLogType jobLogType) {
        Map<String, String> details = new LinkedHashMap<>();
        
        // Common details
        if (variables.getChecklist() != null) {
            details.put("Process ID", variables.getChecklist().getCode());
            details.put("Process Name", variables.getProcessName() != null ? 
                       variables.getProcessName() : variables.getChecklist().getName());
        }
        
        if (variables.getFacility() != null) {
            details.put("Facility Name", variables.getFacility().getName());
        }
        
        // Type-specific details (only if available)
        if (jobLogType == Type.JobLogType.ASSETS_LOGS) {
            addAssetSpecificDetails(details, variables);
        }
        
        // Add checklist properties (same for both types)
        addChecklistProperties(details, variables);
        
        return details;
    }

    private void addAssetSpecificDetails(Map<String, String> details, GeneratedPdfDataDto variables) {
        if (variables.getObjectTypeDisplayName() != null) {
            details.put("Object Type", variables.getObjectTypeDisplayName());
        }
        if (variables.getObjectDisplayName() != null) {
            details.put("Object Name", variables.getObjectDisplayName());
        }
        if (variables.getObjectExternalId() != null) {
            details.put("Object ID", variables.getObjectExternalId());
        }
    }

    private void addChecklistProperties(Map<String, String> details, GeneratedPdfDataDto variables) {
        // First try to get properties from checklistPropertyValues (original approach)
        List<ChecklistPropertyValue> checklistPropertyValues = variables.getChecklistPropertyValues();
        if (checklistPropertyValues != null && !checklistPropertyValues.isEmpty()) {
            for (ChecklistPropertyValue checklistPropertyValue : checklistPropertyValues) {
                if (checklistPropertyValue != null && checklistPropertyValue.getFacilityUseCasePropertyMapping() != null) {
                    Property property = checklistPropertyValue.getFacilityUseCasePropertyMapping().getProperty();
                    if (property != null) {
                        details.put(property.getLabel(), checklistPropertyValue.getValue());
                    }
                }
            }
        } else {
            // Fallback: try to get properties from checklist object (same as JobReportBuilder)
            if (variables.getChecklist() != null && variables.getChecklist().getChecklistPropertyValues() != null) {
                for (ChecklistPropertyValue checklistPropertyValue : variables.getChecklist().getChecklistPropertyValues()) {
                    if (checklistPropertyValue != null && checklistPropertyValue.getFacilityUseCasePropertyMapping() != null) {
                        Property property = checklistPropertyValue.getFacilityUseCasePropertyMapping().getProperty();
                        if (property != null) {
                            details.put(property.getLabel(), checklistPropertyValue.getValue());
                        }
                    }
                }
            }
        }
    }

    private String buildFiltersSection(GeneratedPdfDataDto variables, Type.JobLogType jobLogType) {
        // Prefer CustomView filters if available, for correct parameter mapping
        CustomView customView = getCustomView(variables);
        if (customView != null && customView.getFilters() != null && !customView.getFilters().isEmpty()) {
            List<CustomViewFilter> meaningfulFilters = customView.getFilters().stream()
                .filter(filter -> filter != null && 
                        filter.getValue() != null && 
                        !filter.getValue().isEmpty() && 
                        !"checklistId".equals(filter.getKey()))
                .collect(Collectors.toList());
            
            if (!meaningfulFilters.isEmpty()) {
                String filtersTitle = HtmlTemplateEngine.h4("Filters Applied", CssClasses.SECTION_TITLE);
                StringBuilder filtersContent = new StringBuilder();
                for (int i = 0; i < meaningfulFilters.size(); i++) {
                    CustomViewFilter filter = meaningfulFilters.get(i);
                    String filterValue = filter.getValue().get(0) != null ? filter.getValue().get(0).toString() : "-";

                    // Map operator to user-friendly string
                    String constraint = filter.getConstraint();
                    if (constraint == null || constraint.trim().isEmpty()) {
                        constraint = "=";
                    }
                    constraint = PdfBuilderServiceHelpers.getOperatorDisplayName(constraint);

                    // Map field and value for parameterValues
                    String displayName = filter.getDisplayName();
                    String formattedFilterValue = filterValue;
                    if (filter.getKey() != null && filter.getKey().startsWith("parameterValues.")) {
                        // Use CustomViewColumn displayName for the filter label
                        String paramId = filter.getKey().substring("parameterValues.".length());
                        String paramName = paramId;
                        String valueDisplay = filterValue;

                        // Find the column in CustomView.columns
                        CustomView customViewForColumns = getCustomView(variables);
                        if (customViewForColumns != null && customViewForColumns.getColumns() != null) {
                            for (CustomViewColumn col : customViewForColumns.getColumns()) {
                                if (paramId.equals(col.getId())) {
                                    if (col.getDisplayName() != null && !col.getDisplayName().isEmpty()) {
                                        paramName = col.getDisplayName();
                                    }
                                    // Try to map valueDisplay from JobLogData (use Value, not IdentifierValue)
                                    if (variables.getJobLogs() != null) {
                                        outer:
                                        for (JobLog jobLog : variables.getJobLogs()) {
                                            for (JobLogData logData : jobLog.getLogs()) {
                                                if (logData.getEntityId().equals(paramId) &&
                                                    logData.getTriggerType() == Type.JobLogTriggerType.PARAMETER_VALUE) {
                                                    if (logData.getValue() != null && !logData.getValue().isEmpty()) {
                                                        valueDisplay = logData.getValue();
                                                        break outer;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                        displayName = paramName;
                        formattedFilterValue = valueDisplay;
                    } else if (displayName == null || displayName.trim().isEmpty()) {
                        displayName = filter.getKey() != null ? filter.getKey() : "Filter " + (i + 1);
                    }

                    // Format the filter value for other types if needed
                    formattedFilterValue = formatFilterValue(displayName, formattedFilterValue, filter.getKey(), variables.getFacility());

                    filtersContent.append("<div style=\"margin-bottom: 5px;\">")
                        .append("Filter ").append(i + 1).append(" - Where: ")
                        .append("<span style=\"display: inline-block; padding: 2px 8px; border: 1px solid #000; margin: 0 5px;\">")
                        .append(safeHtmlEscape(displayName))
                        .append("</span> Condition: ")
                        .append("<span style=\"display: inline-block; padding: 2px 8px; border: 1px solid #000; margin: 0 5px;\">")
                        .append(safeHtmlEscape(constraint))
                        .append("</span> Value: ")
                        .append("<span style=\"display: inline-block; padding: 2px 8px; border: 1px solid #000; margin: 0 5px;\">")
                        .append(safeHtmlEscape(formattedFilterValue))
                        .append("</span>")
                        .append("</div>");
                }
                String filtersPanel = HtmlTemplateEngine.panel(filtersContent.toString(), CssClasses.DETAIL_PANEL);
                return filtersTitle + filtersPanel;
            }
        }

        // Fallback to JSON filters (if no CustomView filters)
        String filtersJson = variables.getFilters();
        if (filtersJson != null && !filtersJson.trim().isEmpty() && !"{}".equals(filtersJson.trim())) {
            return buildFiltersFromJson(filtersJson, variables.getFacility());
        }

        return "";
    }
    
    /**
     * Build filters section from JSON string (SearchFilter format)
     * This handles your case with JSON filters like:
     * {"op":"AND","fields":[{"field":"state","op":"EQ","values":["COMPLETED_WITH_EXCEPTION"]}]}
     */
    private String buildFiltersFromJson(String filtersJson, FacilityDto facility) {
        try {
            // Parse filters JSON using SearchFilter structure
            SearchFilter searchFilter = objectMapper.readValue(filtersJson, SearchFilter.class);
            
            if (searchFilter.getFields() == null || searchFilter.getFields().isEmpty()) {
                return ""; // No filter fields to display
            }
            
            // Filter out system filters and get meaningful filters
            List<SearchCriteria> meaningfulFilters = searchFilter.getFields().stream()
                .filter(filter -> filter != null && 
                        filter.getField() != null &&
                        filter.getValues() != null && 
                        !filter.getValues().isEmpty() &&
                        !isSystemFilter(filter.getField()))
                .collect(Collectors.toList());
            
            if (meaningfulFilters.isEmpty()) {
                return ""; // No meaningful filters to display
            }
            
            // Build filters display
            String filtersTitle = HtmlTemplateEngine.h4("Filters Applied", CssClasses.SECTION_TITLE);
            
            StringBuilder filtersContent = new StringBuilder();
            for (int i = 0; i < meaningfulFilters.size(); i++) {
                SearchCriteria filter = meaningfulFilters.get(i);
                String filterHtml = buildJsonFilterItem(filter, i + 1, facility);
                filtersContent.append(filterHtml);
            }
            
            String filtersPanel = HtmlTemplateEngine.panel(filtersContent.toString(), CssClasses.DETAIL_PANEL);
            return filtersTitle + filtersPanel;
            
        } catch (Exception e) {
            // Log error and show generic message as fallback
            log.warn("[buildFiltersFromJson] Failed to parse filters for display: {}", e.getMessage());
            
            // Show generic message as fallback
            String filtersTitle = HtmlTemplateEngine.h4("Filters Applied", CssClasses.SECTION_TITLE);
            String genericMessage = "<div style=\"margin-bottom: 5px;\">Custom filters have been applied to this report.</div>";
            String filtersPanel = HtmlTemplateEngine.panel(genericMessage, CssClasses.DETAIL_PANEL);
            return filtersTitle + filtersPanel;
        }
    }
    
    /**
     * Build filters section from CustomView (legacy approach)
     */
    private String buildFiltersFromCustomView(GeneratedPdfDataDto variables) {
        CustomView customView = getCustomView(variables);
        if (customView == null || customView.getFilters() == null) {
            return "";
        }
        
        List<CustomViewFilter> meaningfulFilters = customView.getFilters().stream()
            .filter(filter -> filter != null && 
                    filter.getValue() != null && 
                    !filter.getValue().isEmpty() && 
                    !"checklistId".equals(filter.getKey()))
            .collect(Collectors.toList());
        
        if (meaningfulFilters.isEmpty()) {
            return "";
        }
        
        String filtersTitle = HtmlTemplateEngine.h4("Filters Applied", CssClasses.SECTION_TITLE);
        
        StringBuilder filtersContent = new StringBuilder();
        for (int i = 0; i < meaningfulFilters.size(); i++) {
            CustomViewFilter filter = meaningfulFilters.get(i);
            String filterHtml = buildFilterItem(filter, i + 1, variables.getFacility());
            filtersContent.append(filterHtml);
        }
        
        String filtersPanel = HtmlTemplateEngine.panel(filtersContent.toString(), CssClasses.DETAIL_PANEL);
        return filtersTitle + filtersPanel;
    }
    
    /**
     * Check if a filter field is a system filter that should be excluded from display
     */
    private boolean isSystemFilter(String fieldName) {
        return SYSTEM_FILTERS.contains(fieldName);
    }
    
    /**
     * Build filter item from JSON SearchCriteria
     */
    private String buildJsonFilterItem(SearchCriteria filter, int filterNumber, FacilityDto facility) {
        String displayName = getFilterDisplayName(filter.getField());
        String constraint = filter.getOp() != null ? 
                           PdfBuilderServiceHelpers.getOperatorDisplayName(filter.getOp()) : "=";
        String filterValue = filter.getValue() != null ? filter.getValue().toString() : "-";
        
        // Format the filter value based on its type
        String formattedFilterValue = formatJsonFilterValue(displayName, filterValue, filter.getField(), facility);
        
        return HtmlTemplateEngine.filterItem(filterNumber, displayName, constraint, formattedFilterValue);
    }
    
    /**
     * Get display name for a filter field
     */
    private String getFilterDisplayName(String fieldName) {
        // Convert field names to user-friendly display names
        switch (fieldName) {
            case "createdAt": return "Created At";
            case "modifiedAt": return "Modified At";
            case "startedAt": return "Started At";
            case "endedAt": return "Ended At";
            case "state": return "Job State";
            case "code": return "Job Code";
            case "checklistCode": return "Process Code";
            case "checklistName": return "Process Name";
            default: return fieldName; // Use field name as fallback
        }
    }
    
    /**
     * Format filter value for JSON filters (enhanced with parameter value support)
     */
    private String formatJsonFilterValue(String displayName, String filterValue, String fieldName, FacilityDto facilityDto) {
        // Check if this is a parameter value filter (e.g., parameterValues.641596052903608320)
        if (fieldName.startsWith("parameterValues.")) {
            // For now, just return the value as-is (mapping is handled in buildFiltersSection)
            return filterValue;
        }
        
        // Check if this is a job state filter
        if ("state".equals(fieldName) || "Job State".equals(displayName)) {
            return formatJobStateValue(filterValue);
        }
        
        // Check if this is a date/time filter
        if (displayName.toLowerCase().contains("date") || 
            displayName.toLowerCase().contains("time") ||
            fieldName.toLowerCase().contains("date") ||
            fieldName.toLowerCase().contains("time") ||
            fieldName.toLowerCase().contains("at")) {
            try {
                // Try to parse the value as a long (timestamp)
                long timestamp = Long.parseLong(filterValue);
                
                // Apply timezone offset if available
                if (facilityDto != null && facilityDto.getTimeZone() != null) {
                    String timezoneId = facilityDto.getTimeZone();
                    String zoneOffsetString = PdfBuilderServiceHelpers.getZoneOffsetString(timezoneId);
                    timestamp = DateTimeUtils.addOffSetToTime(timestamp, zoneOffsetString);
                }
                
                // Format based on whether it's a date or date/time
                if (displayName.toLowerCase().contains("date") && !displayName.toLowerCase().contains("time")) {
                    // Date only
                    String dateFormat = facilityDto != null && facilityDto.getDateFormat() != null ? 
                                       facilityDto.getDateFormat() : DateTimeUtils.DEFAULT_DATE_FORMAT;
                    return DateTimeUtils.getFormattedDatePattern(timestamp, dateFormat);
                } else {
                    // Date and time
                    String dateTimeFormat = facilityDto != null && facilityDto.getDateTimeFormat() != null ? 
                                           facilityDto.getDateTimeFormat() : DateTimeUtils.DEFAULT_DATE_TIME_FORMAT;
                    return DateTimeUtils.getFormattedDateTimeOfPattern(timestamp, dateTimeFormat);
                }
            } catch (NumberFormatException e) {
                // Not a timestamp, keep original value
                return filterValue;
            }
        }
        // Check if this is a number filter
        else if (displayName.toLowerCase().contains("number") || 
                fieldName.toLowerCase().contains("number")) {
            try {
                double numericValue = Double.parseDouble(filterValue);
                return Utility.roundUpDecimalPlaces(numericValue, null);
            } catch (NumberFormatException e) {
                // Not a number, keep original value
                return filterValue;
            }
        }

        return filterValue;
    }
    
    /**
     * Format job state enum values to user-friendly display names
     */
    private String formatJobStateValue(String stateValue) {
        if (stateValue == null) {
            return "-";
        }
        
        // Convert job state enum values to display names
        switch (stateValue.toUpperCase()) {
            case "COMPLETED_WITH_EXCEPTION": return "Completed With Exception";
            case "COMPLETED": return "Completed";
            case "IN_PROGRESS": return "In Progress";
            case "NOT_STARTED": return "Not Started";
            case "BEING_BUILT": return "Being Built";
            case "READY_FOR_SIGNING": return "Ready For Signing";
            case "SIGNED_OFF": return "Signed Off";
            case "DEPRECATED": return "Deprecated";
            case "PUBLISHED": return "Published";
            case "UNPUBLISHED": return "Unpublished";
            default: return stateValue; // Return original value if not recognized
        }
    }

    private String buildFilterItem(CustomViewFilter filter, int filterNumber, FacilityDto facility) {
        String displayName = filter.getDisplayName() != null ? 
                            filter.getDisplayName() : filter.getKey();
        String constraint = filter.getConstraint() != null ? 
                           PdfBuilderServiceHelpers.getOperatorDisplayName(filter.getConstraint()) : "=";
        String filterValue = filter.getValue().get(0) != null ? 
                            filter.getValue().get(0).toString() : "-";
        String value = formatFilterValue(displayName, filterValue, filter.getKey(), facility);
        
        return HtmlTemplateEngine.filterItem(filterNumber, displayName, constraint, value);
    }

    private CustomView getCustomView(GeneratedPdfDataDto variables) {
        return variables.getCustomViews() != null && !variables.getCustomViews().isEmpty() ? 
               variables.getCustomViews().get(0) : null;
    }

    // Removed unused buildFiltersSection(StringBuilder, CustomView, FacilityDto)

    private String formatFilterValue(String displayName, String filterValue, String key, FacilityDto facilityDto) {
        // Check if this is a date/time filter
        if (displayName.toLowerCase().contains("date") || 
            displayName.toLowerCase().contains("time") ||
            key.toLowerCase().contains("date") ||
            key.toLowerCase().contains("time") ||
            key.toLowerCase().contains("at")) {
            try {
                // Try to parse the value as a long (timestamp)
                long timestamp = Long.parseLong(filterValue);
                
                // Apply timezone offset if available
                if (facilityDto != null && facilityDto.getTimeZone() != null) {
                    String timezoneId = facilityDto.getTimeZone();
                    String zoneOffsetString = PdfBuilderServiceHelpers.getZoneOffsetString(timezoneId);
                    timestamp = DateTimeUtils.addOffSetToTime(timestamp, zoneOffsetString);
                }
                
                // Format based on whether it's a date or date/time
                if (displayName.toLowerCase().contains("date") && !displayName.toLowerCase().contains("time")) {
                    // Date only
                    String dateFormat = facilityDto != null && facilityDto.getDateFormat() != null ? 
                                       facilityDto.getDateFormat() : DateTimeUtils.DEFAULT_DATE_FORMAT;
                    return DateTimeUtils.getFormattedDatePattern(timestamp, dateFormat);
                } else {
                    // Date and time
                    String dateTimeFormat = facilityDto != null && facilityDto.getDateTimeFormat() != null ? 
                                           facilityDto.getDateTimeFormat() : DateTimeUtils.DEFAULT_DATE_TIME_FORMAT;
                    return DateTimeUtils.getFormattedDateTimeOfPattern(timestamp, dateTimeFormat);
                }
            } catch (NumberFormatException e) {
                // Not a timestamp, keep original value
                return filterValue;
            }
        }
        // Check if this is a number filter
        else if (displayName.toLowerCase().contains("number") || 
                key.toLowerCase().contains("number")) {
            try {
                double numericValue = Double.parseDouble(filterValue);
                return Utility.roundUpDecimalPlaces(numericValue, null);
            } catch (NumberFormatException e) {
                // Not a number, keep original value
                return filterValue;
            }
        }

        return filterValue;
    }

    private String buildJobLogsTable(GeneratedPdfDataDto variables, Type.JobLogType jobLogType) {
        CustomView customView = getCustomView(variables);
        List<JobLog> jobLogs = variables.getJobLogs();
        
        if (customView == null && Utility.isEmpty(jobLogs)) {
            return HtmlTemplateEngine.p("No data available", null);
        }
        
        List<CustomViewColumn> columns = getColumns(variables);
        if (columns.isEmpty()) {
            return HtmlTemplateEngine.p("No columns configured", null);
        }
        
        // Separate pinned and non-pinned columns
        List<CustomViewColumn> pinnedColumns = columns.stream()
            .filter(CustomViewColumn::isPinned)
            .collect(Collectors.toList());
        
        List<CustomViewColumn> nonPinnedColumns = columns.stream()
            .filter(col -> !col.isPinned())
            .collect(Collectors.toList());
        
        // Check if Job ID column exists in the custom view
        boolean hasJobIdColumn = columns.stream()
            .anyMatch(col -> "code".equals(col.getId()) || "Job ID".equalsIgnoreCase(col.getDisplayName()));
        
        // Do not show default Job ID column if there are no pinned columns and no Job ID column
        boolean showDefaultJobId = false;
        
        // Build resource parameter choice map
        Map<String, ResourceParameterChoiceDto> resourceMap = buildResourceParameterChoiceMap(jobLogs);
        
        StringBuilder tableContent = new StringBuilder();
        
        // Calculate how many non-pinned columns can fit per page
        int pinnedColumnsCount = pinnedColumns.size();
        int jobIdColumnCount = showDefaultJobId ? 1 : 0;
        int availableSpacePerPage = COLS_PER_SHEET - pinnedColumnsCount - jobIdColumnCount;
        
        // If no space for non-pinned columns, adjust
        if (availableSpacePerPage <= 0) {
            availableSpacePerPage = 1; // At least 1 non-pinned column per page
        }
        
        // Process non-pinned columns in chunks, always including pinned columns on each page
        if (nonPinnedColumns.isEmpty()) {
            // Only pinned columns exist
            List<CustomViewColumn> pageColumns = new ArrayList<>(pinnedColumns);
            String tableHtml = buildTableChunk(pageColumns, jobLogs, resourceMap, variables.getFacility(), showDefaultJobId);
            tableContent.append(tableHtml);
        } else {
            // Process non-pinned columns in chunks
            for (int start = 0; start < nonPinnedColumns.size(); start += availableSpacePerPage) {
                int end = Math.min(start + availableSpacePerPage, nonPinnedColumns.size());
                List<CustomViewColumn> nonPinnedPack = nonPinnedColumns.subList(start, end);
                
                // Create page columns: pinned columns + current non-pinned chunk
                List<CustomViewColumn> pageColumns = new ArrayList<>();
                pageColumns.addAll(pinnedColumns); // Always include pinned columns first
                pageColumns.addAll(nonPinnedPack);  // Add current chunk of non-pinned columns
                
                String tableHtml = buildTableChunk(pageColumns, jobLogs, resourceMap, variables.getFacility(), showDefaultJobId);
                tableContent.append(tableHtml);
                
                if (end < nonPinnedColumns.size()) {
                    tableContent.append(HtmlTemplateEngine.pageBreakWithStyle(CssClasses.PAGE_BREAK_AFTER_STYLE));
                }
            }
        }
        
        return HtmlTemplateEngine.div(tableContent.toString(), CssClasses.JOB_LOG_TABLE);
    }

    private List<CustomViewColumn> getColumns(GeneratedPdfDataDto variables) {
        return variables.getColumnsList() != null ? variables.getColumnsList() : List.of();
    }

    private String buildTableChunk(List<CustomViewColumn> columns, List<JobLog> jobLogs, 
                                  Map<String, ResourceParameterChoiceDto> resourceMap, 
                                  FacilityDto facility, boolean showDefaultJobId) {
        
        // Columns are already sorted and processed in buildJobLogsTable
        // No need to re-sort or re-calculate showDefaultJobId here
        
        // Calculate column width
        int totalColumns = columns.size() + (showDefaultJobId ? 1 : 0);
        float columnWidthPercent = 100.0f / totalColumns;
        String columnStyle = "width: " + columnWidthPercent + "%;";
        
        // Build headers using template engine
        StringBuilder headerContent = new StringBuilder();
        
        // Add default Job ID column if needed
        if (showDefaultJobId) {
            headerContent.append(HtmlTemplateEngine.thWithStyle("Job ID", CssClasses.PINNED_COLUMN, columnStyle));
        }
        
        for (CustomViewColumn column : columns) {
            String displayName = column != null ? column.getDisplayName() : "";
            String cssClass = column.isPinned() ? CssClasses.PINNED_COLUMN : null;
            headerContent.append(HtmlTemplateEngine.thWithStyle(displayName, cssClass, columnStyle));
        }
        
        String headerRow = HtmlTemplateEngine.tr(headerContent.toString());
        String tableHeader = HtmlTemplateEngine.thead(headerRow);
        
        // Build rows using template engine
        StringBuilder rowsContent = new StringBuilder();
        for (JobLog jobLog : jobLogs) {
            String rowHtml = buildTableRow(jobLog, columns, resourceMap, facility, columnStyle, showDefaultJobId);
            rowsContent.append(rowHtml);
        }
        
        String tableBody = HtmlTemplateEngine.tbody(rowsContent.toString());
        
        // Create table with fixed layout
        String tableStyle = CssClasses.TABLE_FIXED_LAYOUT_STYLE;
        return HtmlTemplateEngine.tableWithStyle(tableHeader + tableBody, null, tableStyle);
    }

    private String buildTableRow(JobLog jobLog, List<CustomViewColumn> columns, 
                                Map<String, ResourceParameterChoiceDto> resourceMap, 
                                FacilityDto facilityDto, String columnStyle, boolean showDefaultJobId) {
        
        StringBuilder cellsContent = new StringBuilder();
        
        // Add default Job ID cell only if needed
        if (showDefaultJobId) {
            cellsContent.append(HtmlTemplateEngine.tdWithStyle(
                jobLog.getCode(), CssClasses.PINNED_COLUMN, columnStyle));
        }
        
        // Data cells
        Map<String, Object> rowMap = PdfGeneratorUtil.buildRowMap(jobLog);
        for (CustomViewColumn column : columns) {
            String cellValue = buildCellValue(column, jobLog, rowMap, resourceMap, facilityDto, null);
            
            // Apply pinned styling to cells that correspond to pinned columns
            String cssClass = column.isPinned() ? CssClasses.PINNED_COLUMN : null;
            String cellStyle = columnStyle + " " + CssClasses.MAX_WIDTH_300_WRAP_STYLE;
            
            cellsContent.append(HtmlTemplateEngine.tdWithStyle(cellValue, cssClass, cellStyle));
        }
        
        return HtmlTemplateEngine.tr(cellsContent.toString());
    }


    private String buildCellValue(CustomViewColumn c, JobLog log, Map<String, Object> row,
                                 Map<String, ResourceParameterChoiceDto> resourceParameterChoiceDtoMap,
                                 FacilityDto facilityDto, CustomView customView) {
        
        String key = c.getTriggerType();

        // Special handling for resource parameters
        if (Type.JobLogTriggerType.RESOURCE_PARAMETER.name().equals(key)) {
            return handleResourceParameterColumn(c, log, resourceParameterChoiceDtoMap);
        } 
        // Special handling for verification columns
        else if (isVerificationColumn(key)) {
            return handleVerificationColumn(c, log, facilityDto);
        } else {
            return handleRegularColumn(c, log, row, resourceParameterChoiceDtoMap, facilityDto, customView);
        }
    }

    private String handleResourceParameterColumn(CustomViewColumn c, JobLog log,
                                               Map<String, ResourceParameterChoiceDto> resourceParameterChoiceDtoMap) {
        
        // Find the matching JobLogData for this resource parameter
        JobLogData resourceLogData = null;
        for (JobLogData logData : log.getLogs()) {
            if (logData.getTriggerType() == Type.JobLogTriggerType.RESOURCE_PARAMETER && 
                logData.getEntityId().equals(c.getId())) {
                resourceLogData = logData;
                break;
            }
        }
        
        String formattedValue = "-";
        if (resourceLogData != null) {
            try {
                // Check if we have an identifier value to look up
                if (resourceLogData.getIdentifierValue() != null) {
                    // Split identifier values and value arrays for multiple resources
                    String[] identifierValueArr = resourceLogData.getIdentifierValue().split(",");
                    
                    StringBuilder valueBuilder = new StringBuilder();
                    for (int i = 0; i < identifierValueArr.length; i++) {
                        if (!Utility.isEmpty(identifierValueArr[i])) {
                            String trimmedId = identifierValueArr[i].trim();
                            
                            // Try to find in the global resource map first
                            ResourceParameterChoiceDto choice = resourceParameterChoiceDtoMap.get(trimmedId);
                            String displayName;
                            
                            if (choice != null) {
                                // Found in the global map
                                displayName = choice.getObjectDisplayName() + " (ID: " + choice.getObjectExternalId() + ")";
                            } else {
                                // Fall back to the helper method
                                displayName = PdfBuilderServiceHelpers.getResourceDisplayNameFromIdentifier(
                                    trimmedId, 
                                    resourceLogData.getResourceParameters()
                                );
                            }
                            
                            if (valueBuilder.length() > 0) {
                                valueBuilder.append(", ");
                            }
                            valueBuilder.append(displayName);
                        }
                    }
                    
                    if (valueBuilder.length() > 0) {
                        formattedValue = valueBuilder.toString();
                    }

                } else if (resourceLogData.getTriggerType() == Type.JobLogTriggerType.RESOURCE) {
                    // Handle RESOURCE trigger type
                    StringBuilder valueBuilder = new StringBuilder();
                    for (Map.Entry<String, JobLogResource> entry : resourceLogData.getResourceParameters().entrySet()) {
                        JobLogResource resource = entry.getValue();
                        if (resource.getChoices() != null && !resource.getChoices().isEmpty()) {
                            if (valueBuilder.length() > 0) {
                                valueBuilder.append(", ");
                            }
                            valueBuilder.append(resource.getDisplayName()).append(": ");
                            
                            StringJoiner choiceJoiner = new StringJoiner(", ");
                            for (ResourceParameterChoiceDto choice : resource.getChoices()) {
                                choiceJoiner.add(choice.getObjectDisplayName() + " (ID: " + choice.getObjectExternalId() + ")");
                            }
                            valueBuilder.append(choiceJoiner);
                        }
                    }
                    
                    if (valueBuilder.length() > 0) {
                        formattedValue = valueBuilder.toString();
                    }
                } else {
                    // Fall back to processing resource parameters directly
                    StringBuilder valueBuilder = new StringBuilder();
                    for (Map.Entry<String, JobLogResource> entry : resourceLogData.getResourceParameters().entrySet()) {
                        JobLogResource resource = entry.getValue();
                        if (resource.getChoices() != null && !resource.getChoices().isEmpty()) {
                            for (ResourceParameterChoiceDto choice : resource.getChoices()) {
                                if (valueBuilder.length() > 0) {
                                    valueBuilder.append(", ");
                                }
                                valueBuilder.append(choice.getObjectDisplayName())
                                    .append(" (ID: ")
                                    .append(choice.getObjectExternalId())
                                    .append(")");
                            }
                        }
                    }
                    
                    if (valueBuilder.length() > 0) {
                        formattedValue = valueBuilder.toString();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                formattedValue = "Error: " + e.getMessage();
            }
        }

        return safeHtmlEscape(formattedValue);
    }

    private String handleRegularColumn(CustomViewColumn c, JobLog log, Map<String, Object> row,
                                     Map<String, ResourceParameterChoiceDto> resourceParameterChoiceDtoMap,
                                     FacilityDto facilityDto, CustomView customView) {
        
        // Step 1: Get column type directly from CustomView (like Excel)
        Type.JobLogColumnType columnType;
        try {
            columnType = Type.JobLogColumnType.valueOf(c.getType());
        } catch (IllegalArgumentException e) {
            // Invalid column type, default to TEXT
            columnType = Type.JobLogColumnType.TEXT;
        }
        
        // Step 2: Find the matching JobLogData
        JobLogData jobLogData = null;
        for (JobLogData logData : log.getLogs()) {
            if (logData.getEntityId().equals(c.getId()) && 
                logData.getTriggerType().name().equals(c.getTriggerType())) {
                jobLogData = logData;
                break;
            }
        }
        
        // Step 3: If no JobLogData found, try to get value from row map
        String value = "";
        String identifierValue = "";
        List<JobLogMediaData> mediaData = null;
        Map<String, JobLogResource> resourceParameters = null;
        
        if (jobLogData != null) {
            value = jobLogData.getValue() != null ? jobLogData.getValue() : "";
            identifierValue = jobLogData.getIdentifierValue() != null ? jobLogData.getIdentifierValue() : "";
            mediaData = jobLogData.getMedias();
            resourceParameters = jobLogData.getResourceParameters();
            
            // Debug logging to check what data we have
        } else {
            // Try to get value from row map as fallback
            Object rowValue = row.get(c.getId());
            if (rowValue != null) {
                value = rowValue.toString();
            }
        }
        
        // Step 4: For DATE/DATE_TIME columns, handle timezone and format properly
        if (columnType == Type.JobLogColumnType.DATE || columnType == Type.JobLogColumnType.DATE_TIME) {
            if (!Utility.isEmpty(value)) {
                try {
                    long timestamp = Long.parseLong(value);
                    
                    // Convert seconds to milliseconds if needed (10-digit = seconds)
                    if (timestamp > 0 && timestamp < 10000000000L) {
                        timestamp = timestamp * 1000;
                    }
                    
                    // Apply timezone offset if available
                    if (facilityDto != null && facilityDto.getTimeZone() != null) {
                        String zoneOffsetString = PdfBuilderServiceHelpers.getZoneOffsetString(facilityDto.getTimeZone());
                        timestamp = DateTimeUtils.addOffSetToTime(timestamp / 1000, zoneOffsetString) * 1000;
                    }
                    
                    // Format based on column type
                    String formattedDateTime;
                    if (columnType == Type.JobLogColumnType.DATE) {
                        String dateFormat = (facilityDto != null && facilityDto.getDateFormat() != null) ? 
                                           facilityDto.getDateFormat() : DateTimeUtils.DEFAULT_DATE_FORMAT;
                        formattedDateTime = DateTimeUtils.getFormattedDatePattern(timestamp / 1000, dateFormat);
                    } else {
                        String dateTimeFormat = (facilityDto != null && facilityDto.getDateTimeFormat() != null) ? 
                                               facilityDto.getDateTimeFormat() : DateTimeUtils.DEFAULT_DATE_TIME_FORMAT;
                        formattedDateTime = DateTimeUtils.getFormattedDateTimeOfPattern(timestamp / 1000, dateTimeFormat);
                    }
                    
                    return safeHtmlEscape(formattedDateTime);
                    
                } catch (Exception e) {
                    // If formatting fails, display as-is
                    return safeHtmlEscape(value);
                }
            } else {
                return "-"; // No timestamp value available
            }
        }
        
        // Step 5: Check if this is a select-type parameter and handle specially
        if (isSelectTypeParameter(c.getTriggerType()) && value != null && !value.trim().isEmpty()) {
            String formattedSelectValue = formatSelectParameterValue(value, c, log);
            if (formattedSelectValue != null) {
                return safeHtmlEscape(formattedSelectValue);
            }
        }
        
        // Step 6: For all other columns, use the existing formatter
        String formattedValue = PdfBuilderServiceHelpers.formatJobLogCellValue(
            columnType, 
            value, 
            identifierValue,
            resourceParameters,
            resourceParameterChoiceDtoMap,
            facilityDto != null ? facilityDto.getTimeZone() : null,
            facilityDto != null ? facilityDto.getDateTimeFormat() : null,
            facilityDto != null ? facilityDto.getDateFormat() : null,
            mediaData
        );
        
        // Step 7: Handle HTML escaping
        if (columnType == Type.JobLogColumnType.FILE && formattedValue.contains("<a href=")) {
            return formattedValue; // Don't escape HTML for hyperlinks
        } else {
            return safeHtmlEscape(formattedValue);
        }
    }

    /**
     * Check if a column is a verification column
     * @param triggerType The trigger type to check
     * @return true if it's a verification column, false otherwise
     */
    private boolean isVerificationColumn(String triggerType) {
        return Type.JobLogTriggerType.PARAMETER_SELF_VERIFIED_BY.name().equals(triggerType) ||
               Type.JobLogTriggerType.PARAMETER_SELF_VERIFIED_AT.name().equals(triggerType) ||
               Type.JobLogTriggerType.PARAMETER_PEER_VERIFIED_BY.name().equals(triggerType) ||
               Type.JobLogTriggerType.PARAMETER_PEER_VERIFIED_AT.name().equals(triggerType) ||
               Type.JobLogTriggerType.PARAMETER_PEER_STATUS.name().equals(triggerType);
    }

    /**
     * Handle verification columns to format them as sentences instead of just IDs
     * @param column The column configuration
     * @param facilityDto The facility for timezone/format settings
     * @return Formatted verification sentence
     */
    private String handleVerificationColumn(CustomViewColumn column, JobLog jobLog, FacilityDto facilityDto) {
        String triggerType = column.getTriggerType();
        
        // Debug: Log what we're looking for
        log.debug("Looking for verification data - Column ID: {}, Trigger Type: {}", column.getId(), triggerType);
        
        // Find matching JobLogData entries for this parameter
        List<JobLogData> verificationData = new ArrayList<>();
        for (JobLogData logData : jobLog.getLogs()) {
            // Debug: Log each JobLogData entry
            log.debug("JobLogData - EntityId: {}, TriggerType: {}, Value: {}", 
                     logData.getEntityId(), logData.getTriggerType(), logData.getValue());
            
            if (logData.getEntityId().equals(column.getId()) && 
                Type.VERIFICATION_TRIGGER_TYPES.contains(logData.getTriggerType())) {
                verificationData.add(logData);
                log.debug("Found matching verification data: {}", logData);
            }
        }
        
        if (verificationData.isEmpty()) {
            log.debug("No verification data found for column: {}", column.getId());
            return "-";
        }
        
        // Group verification data by verification type (self/peer)
        Map<String, Map<String, String>> verificationInfo = new HashMap<>();
        
        for (JobLogData data : verificationData) {
            String verificationType = getVerificationTypeFromTrigger(data.getTriggerType());
            
            verificationInfo.computeIfAbsent(verificationType, k -> new HashMap<>());
            Map<String, String> info = verificationInfo.get(verificationType);
            
            switch (data.getTriggerType()) {
                case PARAMETER_SELF_VERIFIED_BY:
                case PARAMETER_PEER_VERIFIED_BY:
                    // Format user ID as display name
                    String userDisplayName = formatUserDisplayName(data.getValue());
                    info.put("by", userDisplayName);
                    break;
                case PARAMETER_SELF_VERIFIED_AT:
                case PARAMETER_PEER_VERIFIED_AT:
                    info.put("at", formatVerificationTimestamp(data.getValue(), facilityDto));
                    break;
                case PARAMETER_PEER_STATUS:
                    info.put("status", data.getValue());
                    break;
            }
        }
        
        // Build verification sentences
        List<String> sentences = new ArrayList<>();
        
        // Handle self verification
        if (verificationInfo.containsKey("self")) {
            Map<String, String> selfInfo = verificationInfo.get("self");
            String sentence = buildVerificationSentence("Self", selfInfo);
            if (sentence != null) {
                sentences.add(sentence);
            }
        }
        
        // Handle peer verification
        if (verificationInfo.containsKey("peer")) {
            Map<String, String> peerInfo = verificationInfo.get("peer");
            String sentence = buildVerificationSentence("Peer", peerInfo);
            if (sentence != null) {
                sentences.add(sentence);
            }
        }
        
        // Return the specific verification type requested by the column
        if (Type.JobLogTriggerType.PARAMETER_SELF_VERIFIED_BY.name().equals(triggerType) ||
            Type.JobLogTriggerType.PARAMETER_SELF_VERIFIED_AT.name().equals(triggerType)) {
            // Return self verification sentence
            return sentences.stream()
                .filter(s -> s.contains("Self") || s.contains("Performed"))
                .findFirst()
                .orElse("-");
        } else if (Type.JobLogTriggerType.PARAMETER_PEER_VERIFIED_BY.name().equals(triggerType) ||
                   Type.JobLogTriggerType.PARAMETER_PEER_VERIFIED_AT.name().equals(triggerType) ||
                   Type.JobLogTriggerType.PARAMETER_PEER_STATUS.name().equals(triggerType)) {
            // Return peer verification sentence
            return sentences.stream()
                .filter(s -> s.contains("Peer") || s.contains("Performed"))
                .findFirst()
                .orElse("-");
        }
        
        // Fallback: return all sentences joined
        return sentences.isEmpty() ? "-" : String.join("; ", sentences);
    }

    /**
     * Get verification type (self/peer) from trigger type
     */
    private String getVerificationTypeFromTrigger(Type.JobLogTriggerType triggerType) {
        switch (triggerType) {
            case PARAMETER_SELF_VERIFIED_BY:
            case PARAMETER_SELF_VERIFIED_AT:
                return "self";
            case PARAMETER_PEER_VERIFIED_BY:
            case PARAMETER_PEER_VERIFIED_AT:
            case PARAMETER_PEER_STATUS:
                return "peer";
            default:
                return "-";
        }
    }

    /**
     * Format verification timestamp with facility timezone and format
     */
    private String formatVerificationTimestamp(String timestamp, FacilityDto facilityDto) {
        if (timestamp == null || timestamp.trim().isEmpty()) {
            return "-";
        }
        
        try {
            long timestampLong = Long.parseLong(timestamp);
            
            // Apply timezone offset if available
            if (facilityDto != null && facilityDto.getTimeZone() != null) {
                String zoneOffsetString = PdfBuilderServiceHelpers.getZoneOffsetString(facilityDto.getTimeZone());
                timestampLong = DateTimeUtils.addOffSetToTime(timestampLong, zoneOffsetString);
            }
            
            // Use facility date/time format or default
            String dateTimeFormat = (facilityDto != null && facilityDto.getDateTimeFormat() != null) ? 
                                   facilityDto.getDateTimeFormat() : DateTimeUtils.DEFAULT_DATE_TIME_FORMAT;
            
            return DateTimeUtils.getFormattedDateTimeOfPattern(timestampLong, dateTimeFormat);
        } catch (NumberFormatException e) {
            return timestamp; // Return original if not a valid timestamp
        }
    }

    /**
     * Format user ID to display name
     * For now, just return the user ID as-is since we don't have user lookup data
     * In the future, this could be enhanced to look up actual user names
     */
    private String formatUserDisplayName(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return "-";
        }
        
        // For now, just return the user ID
        // In the future, this could be enhanced to format as "First Last (ID: userId)"
        return userId.trim();
    }

    /**
     * Build verification sentence from verification info
     */
    private String buildVerificationSentence(String verificationType, Map<String, String> info) {
        String by = info.get("by");
        String at = info.get("at");
        String status = info.get("status");
        
        if (by == null && at == null) {
            return null; // No verification data
        }
        
        StringBuilder sentence = new StringBuilder();
        sentence.append("Performed at ");
        sentence.append(at != null ? at : "-");
        sentence.append(", by ");
        sentence.append(by != null ? by : "-");
        
        // Add status for peer verification if available
        if ("Peer".equals(verificationType) && status != null && !status.trim().isEmpty()) {
            sentence.append(" (Status: ").append(status).append(")");
        }
        
        return sentence.toString();
    }

    /**
     * Check if a trigger type represents a select-type parameter
     * @param triggerType The trigger type to check
     * @return true if it's a select-type parameter, false otherwise
     */
    private boolean isSelectTypeParameter(String triggerType) {
        return Type.JobLogTriggerType.PARAMETER_VALUE.name().equals(triggerType) || Type.JobLogTriggerType.PROCESS_PARAMETER_VALUE.name().equals(triggerType);
    }

    /**
     * Format select parameter value by converting IDs to display names
     * @param value The raw JSON value containing selected IDs
     * @param column The column configuration
     * @param jobLog The job log containing parameter definition data
     * @return Formatted value with display names instead of IDs
     */
    private String formatSelectParameterValue(String value, CustomViewColumn column, JobLog jobLog) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        try {
            Type.Parameter parameterType = inferParameterTypeFromValue(value);
            
            String formattedValue = PdfBuilderServiceHelpers.formatSelectOrResourceParameterValue(
                parameterType, value, null);
            if (formattedValue != null && !formattedValue.equals(value)) {
                return formattedValue;
            }
            
        } catch (Exception e) {
            log.warn("Failed to format select parameter value: {}", e.getMessage());
        }
        
        // If formatting fails or returns the same value, return the original value
        return value;
    }

    /**
     * Infer parameter type from the JSON value structure
     * This is a best-effort approach since we don't have direct access to parameter definition
     */
    private Type.Parameter inferParameterTypeFromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Try to parse as JSON to understand the structure
            if (value.startsWith("{") && value.endsWith("}")) {
                // Looks like a JSON object - could be select parameter
                if (value.contains("SELECTED") || value.contains("NOT_SELECTED")) {
                    // Check if it has multiple entries (multiselect) or single (single select)
                    long selectedCount = value.chars().filter(ch -> ch == '"').count() / 2;
                    if (selectedCount > 2) {
                        return Type.Parameter.MULTISELECT;
                    } else {
                        return Type.Parameter.SINGLE_SELECT;
                    }
                }
                // Could be YES_NO parameter
                if (value.toLowerCase().contains("yes") || value.toLowerCase().contains("no")) {
                    return Type.Parameter.YES_NO;
                }
            }
        } catch (Exception e) {
            // If parsing fails, return null to use default handling
            log.debug("Could not infer parameter type from value: {}", value);
        }
        
        // Default to SINGLE_SELECT if we can't determine the type
        return Type.Parameter.SINGLE_SELECT;
    }


    /**
     * Helper method to safely escape HTML, handling null values
     * @param value The value to escape
     * @return The escaped value, or an empty string if the value is null
     */
    private String safeHtmlEscape(Object value) {
        return HtmlUtils.htmlEscape(value != null ? value.toString() : "");
    }
}
