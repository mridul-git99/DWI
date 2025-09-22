package com.leucine.streem.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leucine.streem.constant.State;
import com.leucine.streem.dto.JobDownloadDto;
import com.leucine.streem.dto.JobExcelFilterParams;
import com.leucine.streem.dto.ResourceParameterChoiceDto;
import com.leucine.streem.dto.projection.ChecklistPropertyView;
import com.leucine.streem.dto.projection.JobExcelProjection;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.model.ChecklistPropertyValue;
import com.leucine.streem.model.Facility;
import com.leucine.streem.model.FacilityUseCasePropertyMapping;
import com.leucine.streem.model.Job;
import com.leucine.streem.model.ParameterValue;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.model.helper.search.SearchCriteria;
import com.leucine.streem.model.helper.search.SearchFilter;
import com.leucine.streem.model.helper.search.SearchOperator;
import com.leucine.streem.constant.Type;
import com.leucine.streem.repository.IChecklistRepository;
import com.leucine.streem.repository.IFacilityRepository;
import com.leucine.streem.repository.IJobRepository;
import com.leucine.streem.repository.IParameterValueRepository;
import com.leucine.streem.repository.IUseCaseRepository;
import com.leucine.streem.repository.IUserRepository;
import com.leucine.streem.service.IJobExcelService;
import com.leucine.streem.util.DateTimeUtils;
import com.leucine.streem.util.JobExcelFilterParser;
import com.leucine.streem.util.JsonUtils;
import com.leucine.streem.util.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import com.leucine.streem.model.Property;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.leucine.streem.util.DateTimeUtils.calculateTimezoneOffset;

/**
 * Service implementation for Job Excel generation functionality.
 *
 * @businessCategory Job Management
 * @businessDescription Provides streamlined Excel export functionality for jobs including
 *                     job details and dynamic checklist properties in flat table format.
 * @technicalDescription Implements memory-efficient Excel generation with batch data fetching,
 *                      optimized for performance and simplified user experience.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobExcelService implements IJobExcelService {

  private final IChecklistRepository checklistRepository;
  private final IUserRepository userRepository;
  private final IUseCaseRepository useCaseRepository;
  private final IParameterValueRepository parameterValueRepository;
  private final ObjectMapper objectMapper;
  private final JobExcelFilterParser jobExcelFilterParser;
  private final IJobRepository jobRepository;
  private final IFacilityRepository facilityRepository;

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * Instance field to store unique property names collected during checklist property processing.
   * Eliminates need for complex data containers while maintaining single-pass optimization.
   */
  private Set<String> uniquePropertyNames;

  @Override
  @Transactional(readOnly = true, timeout = 600) // 10 min timeout for bulk operations
  public ByteArrayInputStream generateJobsExcel(String filters, String objectId) throws IOException, ResourceNotFoundException {
    log.info("[generateJobsExcel] Request to generate jobs Excel with BULK processing, filters: {}, objectId: {}", filters, objectId);

    try {
      // PHASE 1: Bulk Data Loading (2 optimized queries)
      List<JobDownloadDto> allJobs = bulkLoadJobsWithCreatorInfo(filters, objectId);
      
      if (allJobs.isEmpty()) {
        log.warn("[generateJobsExcel] No jobs found for given filters");
        return generateEmptyExcel(filters, objectId);
      }
      

      // Extract checklist IDs for bulk properties loading
      Set<Long> checklistIds = extractChecklistIds(allJobs);

      Map<Long, List<ChecklistPropertyValue>> allProperties = bulkLoadAllProperties(checklistIds);

      // PHASE 3: Excel Generation (single pass)

      return generateExcelFromMemoryData(allJobs, allProperties, filters, objectId);

    } catch (Exception e) {
      log.error("[generateJobsExcel] Error generating Excel file: {}", e.getMessage(), e);
      throw new IOException("Failed to generate Excel file: " + e.getMessage(), e);
    }
  }

  // ===== BULK DATA LOADING METHODS =====
  
  /**
   * Bulk loads all jobs with creator information using static query with typed parameters
   */
  private List<JobDownloadDto> bulkLoadJobsWithCreatorInfo(String filters, String objectId) {
    log.debug("[bulkLoadJobsWithCreatorInfo] Bulk loading jobs with filters: {}, objectId: {}", filters, objectId);
    
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    
    try {
      JobExcelFilterParams filterParams = jobExcelFilterParser.parseFilters(filters, objectId, principalUser);
      if (!jobExcelFilterParser.validateFilterParams(filterParams)) {
        log.warn("[bulkLoadJobsWithCreatorInfo] Invalid filter parameters, using basic security filters");
        filterParams = jobExcelFilterParser.createBasicFilterParams(principalUser);
      }

      Long facilityId = principalUser.getCurrentFacilityId();
      Facility facility = facilityRepository.getReferenceById(facilityId);
      String timezoneOffset = calculateTimezoneOffset(facility);
      String dateTimeFormat = facility.getDateTimeFormat();

      
      // Execute static query with typed parameters
      List<JobExcelProjection> jobs = jobRepository.findJobsForExcelDownload(
          filterParams.getOrganisationId(),
          filterParams.getFacilityId(),
          filterParams.getStateFilter(),
          filterParams.getUseCaseIdFilter(),
          filterParams.getChecklistAncestorIdFilter(),
          filterParams.getCodeFilter(),
          filterParams.getChecklistNameFilter(),
          filterParams.getExpectedEndDateLt(),
          filterParams.getExpectedStartDateGt(),
          filterParams.getExpectedStartDateLt(),
          filterParams.getExpectedStartDateIsNull(),
          filterParams.getStartedAtGte(),
          filterParams.getStartedAtLte(),
          filterParams.getObjectIdChoicesJson(),
          filterParams.getCreatedById()
      );
      
      // ✅ Convert JobExcelProjection to DTOs using pre-calculated timezone info (no DB calls in loop)
      return jobs.stream()
          .map(job -> convertJobProjectionToDto(job, timezoneOffset, dateTimeFormat))
          .collect(Collectors.toList());
          
    } catch (Exception e) {
      log.error("[bulkLoadJobsWithCreatorInfo] Error executing static query: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to execute bulk job query: " + e.getMessage(), e);
    }
  }
  
  /**
   * Bulk loads all properties for given checklist IDs using WITH clause + unnest
   */
  private Map<Long, List<ChecklistPropertyValue>> bulkLoadAllProperties(Set<Long> checklistIds) {
    log.debug("[bulkLoadAllProperties] Bulk loading properties for {} checklists", checklistIds.size());
    
    if (checklistIds.isEmpty()) {
      this.uniquePropertyNames = Collections.emptySet();
      return Collections.emptyMap();
    }
    
    // Direct Set usage with simplified IN clause
    List<ChecklistPropertyView> results = checklistRepository.bulkLoadPropertiesForChecklists(checklistIds);
    
    // Single iteration - collect both data structures simultaneously
    Map<Long, List<ChecklistPropertyValue>> propertiesByChecklist = new HashMap<>();
    this.uniquePropertyNames = new TreeSet<>(); // TreeSet for automatic sorting
    
    for (ChecklistPropertyView view : results) {
      // Collect unique property names (TreeSet automatically sorts and deduplicates)
      this.uniquePropertyNames.add(view.getPropertyLabel());
      
      // Create and group property objects
      ChecklistPropertyValue cpv = checklistPropertyValue(
          view.getChecklistId(), 
          view.getPropertyLabel(), 
          view.getPropertyValue()
      );
      
      propertiesByChecklist
          .computeIfAbsent(view.getChecklistId(), k -> new ArrayList<>())
          .add(cpv);
    }
    
    log.info("[bulkLoadAllProperties] Bulk loading completed: {} properties for {} checklists, {} unique names", 
             results.size(), checklistIds.size(), this.uniquePropertyNames.size());
    
    return propertiesByChecklist;
  }
  

  /**
   * Formats timestamp using efficient offset addition (following JobLogService pattern)
   */
  private String formatTimestampWithOffset(Long timestamp, String timezoneOffset, String dateTimeFormat) {
    if (Utility.isEmpty(timestamp)) return "";
    long adjustedTimestamp = DateTimeUtils.addOffSetToTime(timestamp, timezoneOffset);
    return DateTimeUtils.getFormattedDateTimeOfPattern(adjustedTimestamp, dateTimeFormat);
  }

  private JobDownloadDto convertJobProjectionToDto(JobExcelProjection job, String timezoneOffset, String dateTimeFormat) {
    try {
      // Use Utility method for consistent creator formatting
      String formattedCreator = Utility.getFullNameAndEmployeeId(
          job.getFirstName(), 
          job.getLastName(), 
          job.getEmployeeId()
      );
      
      // ✅ Use efficient timezone offset formatting (like JobLogService)
      String formattedCreatedAt = formatTimestampWithOffset(job.getCreatedAt(), timezoneOffset, dateTimeFormat);
      
      return new JobDownloadDto(
          String.valueOf(job.getId()),
          job.getCode(),
          job.getState(),
          formattedCreator,
          formattedCreatedAt,
          String.valueOf(job.getChecklistsId()),
          job.getChecklistCode(),
          job.getChecklistName()
      );
      
    } catch (Exception e) {
      log.error("[convertJobProjectionToDto] Error converting JobExcelProjection to JobDownloadDto: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to convert JobExcelProjection to DTO: " + e.getMessage(), e);
    }
  }

  /**
   * Generates Excel from pre-loaded memory data (single pass)
   */
  private ByteArrayInputStream generateExcelFromMemoryData(
      List<JobDownloadDto> allJobs,
      Map<Long, List<ChecklistPropertyValue>> allProperties,
      String filters,
      String objectId) throws IOException {
    
    log.debug("[generateExcelFromMemoryData] Generating Excel from {} jobs in memory", allJobs.size());
    
    // Create workbook
    Workbook workbook = new XSSFWorkbook();
    Sheet jobsSheet = createJobsSheet(workbook, "Jobs Data");
    
    // Extract all unique property names
    Set<String> allPropertyNames = this.uniquePropertyNames != null ? 
        this.uniquePropertyNames : Collections.emptySet();
    
    // Create headers
    int currentRow = createHeaders(jobsSheet, 0, allPropertyNames);
    
    // Create all job rows in single pass
    for (JobDownloadDto job : allJobs) {
      currentRow = createJobRow(jobsSheet, currentRow, job, allProperties, allPropertyNames);
    }
    
    // Finalize
    autoSizeColumns(jobsSheet, getColumnCount(allPropertyNames));
    createFilterDetailsSheet(workbook, filters, objectId);
    
    return convertWorkbookToByteArray(workbook);
  }


  // Helper method for lightweight property objects
  private ChecklistPropertyValue checklistPropertyValue(Long checklistId, String propertyLabel, String propertyValue) {
    ChecklistPropertyValue cpv = new ChecklistPropertyValue();
    cpv.setChecklistId(checklistId);
    cpv.setValue(propertyValue);
    
    // Create minimal nested objects for Excel generation
    Property property = new Property();
    property.setLabel(propertyLabel);
    
    FacilityUseCasePropertyMapping mapping = new FacilityUseCasePropertyMapping();
    mapping.setProperty(property);
    
    cpv.setFacilityUseCasePropertyMapping(mapping);
    
    return cpv;
  }

  // ===== EXCEL GENERATION METHODS =====

  /**
   * Creates and configures the jobs sheet.
   */
  private Sheet createJobsSheet(Workbook workbook, String sheetName) {
    Sheet sheet = workbook.createSheet(sheetName);

    // Set default column width
    sheet.setDefaultColumnWidth(15);

    return sheet;
  }


  /**
   * Creates header row with fixed and dynamic columns.
   */
  private int createHeaders(Sheet sheet, int rowIndex, Set<String> propertyNames) {
    Row headerRow = sheet.createRow(rowIndex);
    int colIndex = 0;

    // Fixed columns
    headerRow.createCell(colIndex++).setCellValue("Job ID");
    headerRow.createCell(colIndex++).setCellValue("Job State");
    headerRow.createCell(colIndex++).setCellValue("Job created by");
    headerRow.createCell(colIndex++).setCellValue("Job created at");
    headerRow.createCell(colIndex++).setCellValue("Checklist Code");
    headerRow.createCell(colIndex++).setCellValue("Checklist Name");

    // Dynamic property columns (sorted alphabetically)
    for (String propertyName : propertyNames) {
      headerRow.createCell(colIndex++).setCellValue(propertyName);
    }

    // Style header row
    styleHeaderRow(headerRow);

    return rowIndex + 1;
  }

  /**
   * Applies professional styling to header row using headless-safe font creation.
   */
  private void styleHeaderRow(Row headerRow) {
    Workbook workbook = headerRow.getSheet().getWorkbook();
    CellStyle headerStyle = workbook.createCellStyle();
    XSSFFont headerFont = new XSSFFont();
    headerFont.setFontName("Arial");
    headerFont.setFontHeightInPoints((short) 12);
    headerFont.setBold(true);

    headerStyle.setFont(headerFont);
    headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    headerStyle.setBorderBottom(BorderStyle.THIN);
    headerStyle.setBorderTop(BorderStyle.THIN);
    headerStyle.setBorderRight(BorderStyle.THIN);
    headerStyle.setBorderLeft(BorderStyle.THIN);

    // Apply style to all header cells
    for (Cell cell : headerRow) {
      cell.setCellStyle(headerStyle);
    }
  }

  /**
   * Creates job row using pre-formatted data (no database calls needed)
   */
  private int createJobRow(Sheet sheet, int rowIndex, JobDownloadDto job,
                           Map<Long, List<ChecklistPropertyValue>> propertiesByChecklist,
                           Set<String> allPropertyNames) {

    Row dataRow = sheet.createRow(rowIndex);
    int colIndex = 0;
    Long checklistId = Long.parseLong(job.getChecklistId());

    // Fixed columns - all data already pre-formatted!
    dataRow.createCell(colIndex++).setCellValue(job.getCode());
    dataRow.createCell(colIndex++).setCellValue(job.getState());
    dataRow.createCell(colIndex++).setCellValue(job.getCreatedBy());    // ✅ Pre-formatted
    dataRow.createCell(colIndex++).setCellValue(job.getCreatedAt());    // ✅ Pre-formatted
    dataRow.createCell(colIndex++).setCellValue(job.getChecklistCode());
    dataRow.createCell(colIndex++).setCellValue(job.getChecklistName());

    // Dynamic property columns
    Map<String, String> jobProperties = getChecklistsProperties(checklistId, propertiesByChecklist);
    for (String propertyName : allPropertyNames) {
      dataRow.createCell(colIndex++).setCellValue(jobProperties.getOrDefault(propertyName, ""));
    }

    return rowIndex + 1;
  }

  // ===== DATA FORMATTING METHODS =====


  /**
   * Maps property labels to values for a specific checklist.
   */
  private Map<String, String> getChecklistsProperties(Long checklistId,
                                                      Map<Long, List<ChecklistPropertyValue>> propertiesByChecklist) {
    List<ChecklistPropertyValue> properties = propertiesByChecklist.getOrDefault(checklistId, new ArrayList<>());

    return properties.stream()
      .collect(Collectors.toMap(
        prop -> prop.getFacilityUseCasePropertyMapping().getProperty().getLabel(),
        ChecklistPropertyValue::getValue,
        (existing, replacement) -> existing // Keep first value if duplicates
      ));
  }

  // ===== UTILITY METHODS =====

  /**
   * Sets fixed column widths for better readability without triggering font calculations.
   * This avoids AWT font system calls that cause issues in headless environments.
   */
  private void autoSizeColumns(Sheet sheet, int columnCount) {
    for (int i = 0; i < columnCount; i++) {
      try {
        // Set reasonable fixed widths instead of auto-sizing to avoid font system calls
        int columnWidth;
        if (i < 6) {
          // Fixed columns (Job Code, State, Creator, etc.) - medium width
          columnWidth = 5000; // ~18 characters
        } else {
          // Dynamic property columns - wider for property values
          columnWidth = 6000; // ~22 characters
        }
        sheet.setColumnWidth(i, columnWidth);
      } catch (Exception e) {
        log.warn("[autoSizeColumns] Error setting column width {}: {}", i, e.getMessage());
      }
    }
  }

  /**
   * Converts workbook to ByteArrayInputStream safely with proper resource cleanup.
   */
  private ByteArrayInputStream convertWorkbookToByteArray(Workbook workbook) throws IOException {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      workbook.write(outputStream);
      return new ByteArrayInputStream(outputStream.toByteArray());
    } finally {
      try {
        workbook.close();
      } catch (IOException e) {
        log.warn("[convertWorkbookToByteArray] Error closing workbook: {}", e.getMessage());
      }
    }
  }

  /**
   * ✅ ADD - Updated for JobDownloadDto
   */
  private Set<Long> extractChecklistIds(List<JobDownloadDto> jobs) {
    return jobs.stream()
        .map(job -> Long.parseLong(job.getChecklistId()))
        .collect(Collectors.toSet());
  }

  /**
   * Calculates total column count (fixed + dynamic properties).
   */
  private int getColumnCount(Set<String> propertyNames) {
    return 6 + propertyNames.size(); // 6 fixed + properties
  }

  /**
   * Generates empty Excel file with headers only when no jobs are found.
   */
  private ByteArrayInputStream generateEmptyExcel(String filters, String objectId) throws IOException {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = createJobsSheet(workbook, "Jobs Data");
    createHeaders(sheet, 0, Collections.emptySet());

    // Also create filter details sheet for empty Excel
    createFilterDetailsSheet(workbook, filters, objectId);

    return convertWorkbookToByteArray(workbook);
  }

  // ===== FILTER DETAILS SHEET METHODS =====

  /**
   * Creates the second sheet containing filter information using headless-safe column sizing
   */
  private void createFilterDetailsSheet(Workbook workbook, String filters, String objectId) throws IOException {
    log.debug("[createFilterDetailsSheet] Creating filter details sheet with objectId: {}", objectId);

    Sheet detailsSheet = workbook.createSheet("Filter Details");

    // Create header style
    CellStyle headerStyle = createHeaderStyle(workbook);

    int rowIndex = 0;

    // Add filter information
    rowIndex = addFilterInformation(detailsSheet, filters, rowIndex, headerStyle);

    // Add object filter if objectId is provided
    if (!Utility.isEmpty(objectId)) {
      rowIndex = addObjectFilter(detailsSheet, objectId, rowIndex, headerStyle);
    }

    // Set fixed column widths to avoid font system calls in headless environments
    detailsSheet.setColumnWidth(0, 4000);  // ~15 characters for filter labels
    detailsSheet.setColumnWidth(1, 8000);  // ~30 characters for filter values
  }

  /**
   * Formats timestamp from filter values to human-readable date
   */
  private String formatTimestampFromValues(List<Object> values) {
    if (Utility.isEmpty(values)) {
      return null;
    }

    try {
      long timestamp = Long.parseLong(values.get(0).toString());
      if (timestamp == 0) {
        return null; // Don't format zero timestamps
      }
      return DateTimeUtils.getFormattedDateTime(timestamp);
    } catch (Exception e) {
      return null;
    }
  }


  /**
   * Represents a recognized job filter pattern with business-friendly display name.
   * Used for converting technical filter criteria into user-friendly descriptions.
   */
  private static class JobFilterPattern {
    private final String displayName;
    private final SearchCriteria sourceCriteria;

    public JobFilterPattern(String displayName, SearchCriteria sourceCriteria) {
      this.displayName = displayName;
      this.sourceCriteria = sourceCriteria;
    }

    public String getDisplayName() { return displayName; }
    public SearchCriteria getSourceCriteria() { return sourceCriteria; }
  }


  /**
   * @param criteriaList List of search criteria to analyze
   * @return JobFilterPattern if a known pattern is detected, null otherwise
   */
  private JobFilterPattern detectJobFilterPattern(List<SearchCriteria> criteriaList) {
    for (SearchCriteria criteria : criteriaList) {
      String field = criteria.getField();
      String operator = criteria.getOp();
      
      // Use our enum-driven approach to identify field types
      Type.JobExcelFilterType fieldType = getJobExcelFilterType(field);
      
      if (fieldType != null) {
        switch (fieldType) {
          case EXPECTED_END_DATE:
            if ("LT".equals(operator)) {
              String formattedDate = formatTimestampFromValues(criteria.getValues());
              String displayName = formattedDate != null ?
                  String.format("Over Due (before %s)", formattedDate) :
                  "Over Due";
              return new JobFilterPattern(displayName, criteria);
            }
            break;
            
          case EXPECTED_START_DATE:
            if ("IS_NOT_SET".equals(operator)) {
              return new JobFilterPattern("Unscheduled", criteria);
            } else if ("GT".equals(operator) && criteria.getValues().contains("0")) {
              return new JobFilterPattern("Scheduled", criteria);
            } else if ("LT".equals(operator)) {
              String formattedDate = formatTimestampFromValues(criteria.getValues());
              String displayName = formattedDate != null ?
                  String.format("Start Delayed (before %s)", formattedDate) :
                  "Start Delayed";
              return new JobFilterPattern(displayName, criteria);
            }
            break;
        }
      }
    }
    return null;
  }

  /**
   * Helper method to get JobExcelFilterType for a field using enum-driven approach
   */
  private Type.JobExcelFilterType getJobExcelFilterType(String field) {
    try {
      return Arrays.stream(Type.JobExcelFilterType.values())
          .filter(f -> f.getValue().contains(field))
          .findFirst()
          .orElse(null);
    } catch (Exception e) {
      log.debug("[getJobExcelFilterType] Error finding JobExcelFilterType for field: {}", field);
      return null;
    }
  }

  /**
   * Parses and displays filter information in human-readable format using SearchFilter
   */
  private int addFilterInformation(Sheet sheet, String filters, int startRow, CellStyle headerStyle) {
    try {
      if (Utility.isEmpty(filters)) {
        return addNoFiltersMessage(sheet, startRow, headerStyle);
      }

      SearchFilter searchFilter = objectMapper.readValue(filters, SearchFilter.class);
      return addSearchFilterInformation(sheet, searchFilter, startRow, headerStyle);

    } catch (Exception e) {
      log.warn("[addFilterInformation] Failed to parse SearchFilter: {}", e.getMessage());
      return addErrorMessage(sheet, startRow, headerStyle);
    }
  }

  /**
   * Adds "No filters applied" message
   */
  private int addNoFiltersMessage(Sheet sheet, int startRow, CellStyle headerStyle) {
    int currentRow = startRow;
    
    Row headerRow = sheet.createRow(currentRow++);
    Cell headerCell = headerRow.createCell(0);
    headerCell.setCellValue("Applied Filters");
    headerCell.setCellStyle(headerStyle);
    
    Row noFiltersRow = sheet.createRow(currentRow++);
    Cell noFiltersCell = noFiltersRow.createCell(1);
    noFiltersCell.setCellValue("No filters applied");
    
    return currentRow;
  }

  /**
   * Adds error message when filter parsing fails
   */
  private int addErrorMessage(Sheet sheet, int startRow, CellStyle headerStyle) {
    int currentRow = startRow;
    
    Row headerRow = sheet.createRow(currentRow++);
    Cell headerCell = headerRow.createCell(0);
    headerCell.setCellValue("Applied Filters");
    headerCell.setCellStyle(headerStyle);
    
    Row errorRow = sheet.createRow(currentRow++);
    Cell errorCell = errorRow.createCell(1);
    errorCell.setCellValue("Unable to parse filter information");
    
    return currentRow;
  }
  /**
   * Enhanced filter details using SearchFilter structure
   */
  private int addSearchFilterInformation(Sheet sheet, SearchFilter searchFilter, int startRow, CellStyle headerStyle) {
    int currentRow = startRow;

    // Header row
    Row headerRow = sheet.createRow(currentRow++);
    Cell headerCell = headerRow.createCell(0);
    headerCell.setCellValue("Applied Filters");
    headerCell.setCellStyle(headerStyle);

    if (Utility.isEmpty(searchFilter.getFields())) {
      Row noFiltersRow = sheet.createRow(currentRow++);
      Cell noFiltersCell = noFiltersRow.createCell(1);
      noFiltersCell.setCellValue("No user filters applied (only organization/facility filters)");
      return currentRow;
    }

    // Filter out security filters and process user filters
    List<SearchCriteria> userFilters = searchFilter.getFields().stream()
        .filter(criteria -> !isSecurityFilter(criteria.getField()))
        .collect(Collectors.toList());

    if (userFilters.isEmpty()) {
      Row noFiltersRow = sheet.createRow(currentRow++);
      Cell noFiltersCell = noFiltersRow.createCell(1);
      noFiltersCell.setCellValue("No user filters applied (only organization/facility filters)");
      return currentRow;
    }

    // Check for business patterns first
    JobFilterPattern jobFilterPattern = detectJobFilterPattern(userFilters);

    // Show all filters, but replace business pattern filter with business-friendly name
    int filterNumber = 1;
    for (SearchCriteria criteria : userFilters) {
      if (jobFilterPattern != null && criteria.equals(jobFilterPattern.getSourceCriteria())) {
        // Replace business pattern filter with business-friendly name
        Row businessRow = sheet.createRow(currentRow++);
        Cell filterLabelCell = businessRow.createCell(0);
        filterLabelCell.setCellValue("Filter " + filterNumber);
        filterLabelCell.setCellStyle(headerStyle);

        Cell businessCell = businessRow.createCell(1);
        businessCell.setCellValue(jobFilterPattern.getDisplayName());
      } else {
        // Show regular filter using SearchCriteria directly
        currentRow = addSearchCriteriaRow(sheet, criteria, filterNumber, currentRow, headerStyle);
      }
      filterNumber++;
    }

    return currentRow;
  }

  /**
   * Add a row for a single SearchCriteria with enhanced formatting
   */
  private int addSearchCriteriaRow(Sheet sheet, SearchCriteria criteria, int filterNumber, int rowIndex, CellStyle headerStyle) {
    Row filterRow = sheet.createRow(rowIndex);

    // Filter number/label
    Cell filterLabelCell = filterRow.createCell(0);
    filterLabelCell.setCellValue("Filter " + filterNumber);
    filterLabelCell.setCellStyle(headerStyle);

    // Filter details using SearchOperator enum for consistent display
    Cell filterValueCell = filterRow.createCell(1);
    String filterDescription = formatSearchCriteriaDescription(criteria);
    filterValueCell.setCellValue(filterDescription);

    return rowIndex + 1;
  }

  /**
   * Format SearchCriteria into human-readable text using SearchOperator enum
   */
  private String formatSearchCriteriaDescription(SearchCriteria criteria) {
    String fieldName = getFieldDisplayName(criteria.getField());
    String operator = getOperatorDisplayName(criteria.getOp());
    String values = formatFilterValues(criteria.getField(), criteria.getValues());

    return String.format("Where %s %s %s", fieldName, operator, values);
  }

  /**
   * Check if field is a security filter (to skip in display)
   */
  private boolean isSecurityFilter(String field) {
    return Job.ORGANISATION_ID.equals(field) || Job.FACILITY_ID.equals(field);
  }


  /**
   * Maps internal field names to user-friendly display names using Type enums like JobLogService
   */
  private String getFieldDisplayName(String field) {
    // First try CustomViewFilterType (existing)
    try {
      var filterKey = Arrays.stream(Type.CustomViewFilterType.values())
          .filter(f -> f.getValue().contains(field))
          .findFirst()
          .orElse(null);
          
      if (Utility.isNotNull(filterKey)) {
        // Use Utility.toDisplayName() for consistent enum formatting
        return Utility.toDisplayName(filterKey);
      }
    } catch (Exception e) {
      log.debug("[getFieldDisplayName] Error finding CustomViewFilterType for field: {}", field);
    }
    
    // Then try our JobExcelFilterType (new)
    try {
      var jobExcelFilterKey = Arrays.stream(Type.JobExcelFilterType.values())
          .filter(f -> f.getValue().contains(field))
          .findFirst()
          .orElse(null);
          
      if (Utility.isNotNull(jobExcelFilterKey)) {
        // Use Utility.toDisplayName() for consistent enum formatting
        return Utility.toDisplayName(jobExcelFilterKey);
      }
    } catch (Exception e) {
      log.debug("[getFieldDisplayName] Error finding JobExcelFilterType for field: {}", field);
    }
    
    // Final fallback - generic camelCase conversion
    String spaced = field.replaceAll("([a-z])([A-Z])", "$1 $2");
    return spaced.substring(0, 1).toUpperCase() + spaced.substring(1);
  }

  /**
   * Maps internal operators to user-friendly display names using existing SearchOperator enum
   */
  private String getOperatorDisplayName(String operator) {
    try {
      // Use existing SearchOperator enum with built-in display names
      return SearchOperator.valueOf(operator.toUpperCase()).getOperator();
    } catch (IllegalArgumentException e) {
      // Fallback for unknown operators or case variations
      log.debug("[getOperatorDisplayName] Unknown operator: {}, using as-is", operator);
      return operator;
    }
  }

  /**
   * Formats filter values based on field type using both CustomViewFilterType and JobExcelFilterType enums
   */
  private String formatFilterValues(String field, List<Object> values) {
    if (Utility.isEmpty(values)) {
      return "No values";
    }

    // Try CustomViewFilterType first
    String customViewResult = tryFormatWithCustomViewFilterType(field, values);
    if (Utility.isNotNull(customViewResult)) {
      return customViewResult;
    }
    
    // Try JobExcelFilterType second
    String jobExcelResult = tryFormatWithJobExcelFilterType(field, values);
    if (Utility.isNotNull(jobExcelResult)) {
      return jobExcelResult;
    }
    
    // Final fallback
    return formatFallbackValues(field, values);
  }

  /**
   * Attempts to format values using CustomViewFilterType enum
   */
  private String tryFormatWithCustomViewFilterType(String field, List<Object> values) {
    try {
      var filterKey = Arrays.stream(Type.CustomViewFilterType.values())
          .filter(f -> f.getValue().contains(field))
          .findFirst()
          .orElse(null);
          
      if (Utility.isEmpty(filterKey)) {
        return null;
      }
      
      return switch (filterKey) {
        case ENDED_AT, CREATED_AT, MODIFIED_AT, STARTED_AT -> formatDateTimeValues(values);
        case JOB_STATE -> formatJobStateValues(values);
        default -> formatGenericValues(values);
      };
    } catch (Exception e) {
      log.debug("[tryFormatWithCustomViewFilterType] Error for field: {}", field);
      return null;
    }
  }

  /**
   * Attempts to format values using JobExcelFilterType enum
   */
  private String tryFormatWithJobExcelFilterType(String field, List<Object> values) {
    try {
      var filterKey = Arrays.stream(Type.JobExcelFilterType.values())
          .filter(f -> f.getValue().contains(field))
          .findFirst()
          .orElse(null);
          
      if (Utility.isEmpty(filterKey)) {
        return null;
      }
      
      return switch (filterKey) {
        case EXPECTED_START_DATE, EXPECTED_END_DATE -> formatDateTimeValues(values);
        case USE_CASE_ID -> formatUseCaseValue(values);
        case CHECKLIST_ANCESTOR_ID -> formatChecklistValues(values);
        case CREATED_BY_ID -> formatCreatedByValues(values);
        default -> formatGenericValues(values);
      };
    } catch (Exception e) {
      log.debug("[tryFormatWithJobExcelFilterType] Error for field: {}", field);
      return null;
    }
  }

  /**
   * Formats timestamp values to human-readable dates
   */
  private String formatDateTimeValues(List<Object> values) {
    return values.stream()
        .map(v -> {
          try {
            return DateTimeUtils.getFormattedDateTime(Long.parseLong(v.toString()));
          } catch (Exception e) {
            return v.toString();
          }
        })
        .collect(Collectors.joining(", "));
  }

  /**
   * Formats job state values using State.Job enum
   */
  private String formatJobStateValues(List<Object> values) {
    return values.stream()
        .map(v -> {
          try {
            return State.Job.valueOf(v.toString()).getDisplayName();
          } catch (Exception e) {
            return v.toString();
          }
        })
        .collect(Collectors.joining(", "));
  }

  /**
   * Formats use case ID value by looking up use case name
   */
  private String formatUseCaseValue(List<Object> values) {
    if (!Utility.isEmpty(values)) {
      try {
        Long useCaseId = Long.parseLong(values.get(0).toString());
        return useCaseRepository.findById(useCaseId)
            .map(useCase -> useCase.getLabel())
            .orElse("Use Case ID: " + useCaseId);
      } catch (Exception e) {
        return "Use Case ID: " + values.get(0).toString();
      }
    }
    return "No use case";
  }

  /**
   * Formats checklist values by looking up checklist names
   */
  private String formatChecklistValues(List<Object> values) {
    List<String> checklistNames = lookupChecklistNames(values);
    return String.join(", ", checklistNames);
  }

  /**
   * Formats values as generic strings
   */
  private String formatGenericValues(List<Object> values) {
    return values.stream()
        .map(Object::toString)
        .collect(Collectors.joining(", "));
  }

  /**
   * Formats created by values by looking up user names
   */
  private String formatCreatedByValues(List<Object> values) {
    List<String> userNames = lookupUserNames(values);
    return String.join(", ", userNames);
  }

  /**
   * Handles fallback formatting for fields not in any enum
   */
  private String formatFallbackValues(String field, List<Object> values) {
    if ("createdBy.id".equals(field)) {
      List<String> userNames = lookupUserNames(values);
      return String.join(", ", userNames);
    }
    return formatGenericValues(values);
  }

  /**
   * Looks up user names for given user IDs using consistent Utility formatting
   */
  private List<String> lookupUserNames(List<Object> userIds) {
    List<String> userNames = new ArrayList<>();

    for (Object userIdObj : userIds) {
      try {
        Long userId = Long.parseLong(userIdObj.toString());
        userRepository.findById(userId).ifPresentOrElse(
            user -> {
              String fullName = Utility.getFullNameAndEmployeeId(
                  user.getFirstName(), 
                  user.getLastName(), 
                  user.getEmployeeId()
              );
              userNames.add(fullName);
            },
            () -> userNames.add("User ID: " + userId)
        );
      } catch (Exception e) {
        userNames.add("User ID: " + userIdObj.toString());
      }
    }

    return userNames;
  }

  /**
   * Looks up checklist names for given checklist IDs following JobLogService pattern
   */
  private List<String> lookupChecklistNames(List<Object> checklistIds) {
    List<String> checklistNames = new ArrayList<>();

    for (Object checklistIdObj : checklistIds) {
      try {
        Long checklistId = Long.parseLong(checklistIdObj.toString());
        checklistRepository.findById(checklistId).ifPresentOrElse(
            checklist -> {
              String name = checklist.getName() + " (" + checklist.getCode() + ")";
              checklistNames.add(name);
            },
            () -> checklistNames.add("Process ID: " + checklistId)
        );
      } catch (Exception e) {
        checklistNames.add("Process ID: " + checklistIdObj.toString());
      }
    }

    return checklistNames;
  }

  private int addObjectFilter(Sheet sheet, String objectId, int rowIndex, CellStyle headerStyle) throws IOException {
    Row objectFilterRow = sheet.createRow(rowIndex);

    Cell filterLabelCell = objectFilterRow.createCell(0);
    filterLabelCell.setCellValue("Object Filter");
    filterLabelCell.setCellStyle(headerStyle);

    String objectFilterDescription = lookupEntityObjectDisplayInfo(objectId);

    Cell filterValueCell = objectFilterRow.createCell(1);
    filterValueCell.setCellValue(objectFilterDescription);

    return rowIndex + 1;
  }

  private String lookupEntityObjectDisplayInfo(String objectId) throws IOException {
    log.debug("[lookupEntityObjectDisplayInfo] Looking up display info for objectId: {}", objectId);

    // Create JSON pattern to search for this objectId
    String jsonChoices = String.format("""
      [
          {
              "objectId": "%s"
          }
      ]
      """, objectId);

    log.debug("[lookupEntityObjectDisplayInfo] Using JSON pattern: {}", jsonChoices);

    // Get the first parameter value that contains this objectId
    ParameterValue parameterValue = parameterValueRepository.findFirstByObjectInChoices(jsonChoices);

    log.debug("[lookupEntityObjectDisplayInfo] Found parameter value: {}", parameterValue != null ? "Yes" : "No");

    // Parse using ResourceParameterChoiceDto for type safety
    List<ResourceParameterChoiceDto> choices = JsonUtils.jsonToCollectionType(
        parameterValue.getChoices(), List.class, ResourceParameterChoiceDto.class);

    log.debug("[lookupEntityObjectDisplayInfo] Parsed {} ResourceParameterChoiceDto objects", choices.size());

    // Find matching choice by objectId
    ResourceParameterChoiceDto matchingChoice = choices.stream()
        .filter(choice -> objectId.equals(choice.getObjectId()))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No matching choice found for objectId: " + objectId));

    log.debug("[lookupEntityObjectDisplayInfo] Found matching choice - collection: '{}', displayName: '{}', externalId: '{}'",
             matchingChoice.getCollection(), matchingChoice.getObjectDisplayName(), matchingChoice.getObjectExternalId());

    String collectionName = formatCollectionName(matchingChoice.getCollection());
    String displayInfo = formatObjectDisplayInfo(matchingChoice.getObjectDisplayName(), matchingChoice.getObjectExternalId());

    String result = String.format("Where %s is equal to %s", collectionName, displayInfo);
    log.debug("[lookupEntityObjectDisplayInfo] Returning formatted sentence: {}", result);
    return result;
  }


  private String formatCollectionName(String collection) {
    if (!Utility.isEmpty(collection)) {
      // Convert camelCase to spaced words and capitalize first letter
      String spaced = collection.replaceAll("([a-z])([A-Z])", "$1 $2");
      return spaced.substring(0, 1).toUpperCase() + spaced.substring(1);
    }
    return "Unknown Object Type";
  }

  private String formatObjectDisplayInfo(String objectDisplayName, String objectExternalId) {
    if (!Utility.isEmpty(objectDisplayName) && !Utility.isEmpty(objectExternalId)) {
      return String.format("%s (ID: %s)", objectDisplayName, objectExternalId);
    }
    return "Unknown Object";
  }

  /**
   * Creates header cell style using headless-safe font creation
   */
  private CellStyle createHeaderStyle(Workbook workbook) {
    CellStyle headerStyle = workbook.createCellStyle();
    XSSFFont headerFont = new XSSFFont();
    headerFont.setFontName("Arial");
    headerFont.setFontHeightInPoints((short) 12);
    headerFont.setBold(true);
    headerStyle.setFont(headerFont);
    headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    headerStyle.setBorderBottom(BorderStyle.THIN);
    headerStyle.setBorderTop(BorderStyle.THIN);
    headerStyle.setBorderRight(BorderStyle.THIN);
    headerStyle.setBorderLeft(BorderStyle.THIN);
    return headerStyle;
  }


}
