package com.leucine.streem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.collections.*;
import com.leucine.streem.collections.partial.PartialEntityObject;
import com.leucine.streem.constant.CollectionKey;
import com.leucine.streem.constant.CollectionMisc;
import com.leucine.streem.constant.UsageStatus;
import com.leucine.streem.dto.ImportResult;
import com.leucine.streem.dto.ImportSummary;
import com.leucine.streem.dto.request.EntityObjectValueRequest;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.ExceptionType;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.Facility;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.repository.IEntityObjectRepository;
import com.leucine.streem.repository.IFacilityRepository;
import com.leucine.streem.repository.IObjectTypeRepository;
import com.leucine.streem.service.IEntityObjectChangeLogService;
import com.leucine.streem.service.IEntityObjectExportImportService;
import com.leucine.streem.service.IEntityObjectService;
import com.leucine.streem.util.DateTimeUtils;
import com.leucine.streem.util.JsonUtils;
import com.leucine.streem.util.Utility;
import com.leucine.streem.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.types.ObjectId;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import static com.leucine.streem.util.WorkbookUtils.getXSSFFont;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.leucine.streem.constant.Misc.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntityObjectExportImportService implements IEntityObjectExportImportService {
  private final IEntityObjectRepository entityObjectRepository;
  private final IObjectTypeRepository objectTypeRepository;
  private final IEntityObjectService entityObjectService;
  private final IFacilityRepository facilityRepository;
  private final com.leucine.streem.dto.mapper.IUserInfoMapper userInfoMapper;
  private final IEntityObjectChangeLogService entityObjectChangeLogService;

  @Override
  public byte[] exportToExcel(String objectTypeId) throws ResourceNotFoundException, StreemException {
    log.info("[exportToExcel] Request to export EntityObjects to Excel, objectTypeId: {}", objectTypeId);

    // Validate and fetch ObjectType
    ObjectType objectType = objectTypeRepository.findById(objectTypeId)
      .orElseThrow(() -> new ResourceNotFoundException(objectTypeId, ErrorCode.OBJECT_TYPE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    // ✅ Filter out deprecated properties and relations
    objectType.setProperties(
      objectType.getProperties().stream()
        .filter(property -> property.getUsageStatus() != UsageStatus.DEPRECATED.getCode())
        .collect(Collectors.toList())
    );

    objectType.setRelations(
      objectType.getRelations().stream()
        .filter(relation -> relation.getUsageStatus() != UsageStatus.DEPRECATED.getCode())
        .collect(Collectors.toList())
    );

    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    String facilityId = principalUser.getCurrentFacilityId().toString();

    // Get facility for timezone handling
    Facility facility = facilityRepository.getReferenceById(principalUser.getCurrentFacilityId());

    List<EntityObject> entityObjects = entityObjectRepository.findAllByUsageStatusAndFacilityId(
        objectType.getExternalId(),
        UsageStatus.ACTIVE.getCode(),
        facilityId
    );

    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("EntityObjects");

      // Create header row
      Row headerRow = sheet.createRow(0);
      List<String> headers = createHeaders(objectType);
      for (int i = 0; i < headers.size(); i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(headers.get(i));

        // Style header cells
        CellStyle headerStyle = workbook.createCellStyle();
        XSSFFont headerFont = getXSSFFont((XSSFWorkbook) workbook, "Arial", (short) 11);
        headerStyle.setFont(headerFont);
        cell.setCellStyle(headerStyle);
      }

      // Create data rows
      for (int i = 0; i < entityObjects.size(); i++) {
        Row dataRow = sheet.createRow(i + 1);
        EntityObject entityObject = entityObjects.get(i);
        List<String> rowData = createRowData(entityObject, objectType, facility);

        for (int j = 0; j < rowData.size(); j++) {
          Cell cell = dataRow.createCell(j);
          cell.setCellValue(rowData.get(j));
        }
      }

      // Set fixed column widths to avoid font system calls in headless environments
      for (int i = 0; i < headers.size(); i++) {
        // Set reasonable fixed width instead of auto-sizing
        sheet.setColumnWidth(i, 4000); // ~15 characters width
      }

      workbook.write(outputStream);
      return outputStream.toByteArray();

    } catch (IOException e) {
      log.error("[exportToExcel] Error creating Excel file", e);
      throw new StreemException("Error creating Excel file: " + e.getMessage());
    }
  }

  private List<String> createHeaders(ObjectType objectType) {
    List<String> headers = new ArrayList<>();

    // Property columns - use externalId for column names (exclude system properties and archived properties)
    for (Property property : objectType.getProperties()) {
      if (!CREATE_PROPERTIES.contains(property.getExternalId()) && 
          property.getUsageStatus() == UsageStatus.ACTIVE.getCode()) {
        headers.add("property_" + property.getExternalId());
      }
    }

    // Relation columns - use externalId for column names (exclude archived relations)
    for (Relation relation : objectType.getRelations()) {
      if (relation.getUsageStatus() == UsageStatus.ACTIVE.getCode()) {
        headers.add("relation_" + relation.getExternalId());
      }
    }

    return headers;
  }

  private List<String> createRowData(EntityObject entityObject, ObjectType objectType, Facility facility) {
    List<String> rowData = new ArrayList<>();

    // Property values - create map for efficient lookup using externalId as key
    Map<String, PropertyValue> propertyValueMap = new HashMap<>();
    if (entityObject.getProperties() != null) {
      for (PropertyValue pv : entityObject.getProperties()) {
        propertyValueMap.put(pv.getExternalId(), pv);
      }
    }

    // Add property values in the same order as headers (exclude system properties and archived properties)
    for (Property property : objectType.getProperties()) {
      if (!CREATE_PROPERTIES.contains(property.getExternalId()) && 
          property.getUsageStatus() == UsageStatus.ACTIVE.getCode()) {
        PropertyValue propertyValue = propertyValueMap.get(property.getExternalId());
        if (!Utility.isEmpty(propertyValue)) {
          if (isDateProperty(property)) {
            rowData.add(formatDatePropertySafely(propertyValue.getValue(), property.getInputType(), facility));
          } else if (CollectionMisc.PROPERTY_DROPDOWN_TYPES.contains(property.getInputType()) && !Utility.isEmpty(propertyValue.getChoices())) {
            // Flatten choices to comma-separated displayNames
            String choiceDisplayNames = propertyValue.getChoices().stream()
              .map(choice -> choice.getDisplayName())
              .collect(Collectors.joining(","));
            rowData.add(choiceDisplayNames);
          } else {
            rowData.add(safeToString(propertyValue.getValue()));
          }
        } else {
          rowData.add("");
        }
      }
    }

    // Relation values - create map for efficient lookup using externalId as key
    Map<String, MappedRelation> relationMap = new HashMap<>();
    if (entityObject.getRelations() != null) {
      for (MappedRelation mr : entityObject.getRelations()) {
        relationMap.put(mr.getExternalId(), mr);
      }
    }

    // Add relation values in the same order as headers (exclude archived relations)
    for (Relation relation : objectType.getRelations()) {
      if (relation.getUsageStatus() == UsageStatus.ACTIVE.getCode()) {
        MappedRelation mappedRelation = relationMap.get(relation.getExternalId());
        if (mappedRelation != null && mappedRelation.getTargets() != null && !mappedRelation.getTargets().isEmpty()) {
          // Flatten targets to comma-separated externalIds
          String targetExternalIds = mappedRelation.getTargets().stream()
            .map(target -> target.getExternalId())
            .collect(Collectors.joining(","));
          rowData.add(targetExternalIds);
        } else {
          rowData.add("");
        }
      }
    }

    return rowData;
  }

  private String safeToString(Object obj) {
    return obj != null ? obj.toString() : "";
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ImportSummary importFromExcel(String objectTypeId, MultipartFile file) throws ResourceNotFoundException, StreemException {
    log.info("[importFromExcel] Request to import EntityObjects from Excel, objectTypeId: {}", objectTypeId);

    // Validate file
    if (file.isEmpty()) {
      throw new StreemException("Excel file is empty");
    }

    if (!file.getOriginalFilename().endsWith(".xlsx") && !file.getOriginalFilename().endsWith(".xls")) {
      throw new StreemException("Invalid file format. Only Excel files (.xlsx, .xls) are supported");
    }

    // Validate and fetch ObjectType
    ObjectType objectType = objectTypeRepository.findById(objectTypeId)
      .orElseThrow(() -> new ResourceNotFoundException(objectTypeId, ErrorCode.OBJECT_TYPE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    // ✅ Filter and set active properties/ relations
    objectType.setProperties(
      objectType.getProperties().stream()
        .filter(property -> property.getUsageStatus() == UsageStatus.ACTIVE.getCode())
        .collect(Collectors.toList())
    );

    objectType.setRelations(
      objectType.getRelations().stream()
        .filter(relation -> relation.getUsageStatus() == UsageStatus.ACTIVE.getCode())
        .collect(Collectors.toList())
    );

    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    String currentFacilityId = principalUser.getCurrentFacilityId().toString();

    // Get facility for date format validation
    Facility facility = facilityRepository.getReferenceById(principalUser.getCurrentFacilityId());

    List<ImportResult> results = new ArrayList<>();
    int created = 0, updated = 0, skipped = 0, failed = 0;

    try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
      Sheet sheet = workbook.getSheetAt(0);

      if (sheet.getPhysicalNumberOfRows() == 0) {
        throw new StreemException("Excel sheet is empty");
      }

      // Parse headers from first row
      Row headerRow = sheet.getRow(0);
      if (headerRow == null) {
        throw new StreemException("Excel sheet has no header row");
      }

      List<String> headers = parseExcelHeaders(headerRow);

      // Create mapping tables
      Map<String, ObjectId> propertyMapping = createPropertyMapping(objectType, headers);
      Map<String, ObjectId> relationMapping = createRelationMapping(objectType, headers);

      // Validate mandatory fields are present in Excel headers
      validateMandatoryFields(objectType, headers);

      // Process each data row with duplicate detection
      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) continue;


        ImportResult result = processRowWithDuplicateCheck(
          row, headers, objectType, propertyMapping, relationMapping,
          currentFacilityId, facility, i + 1
        );
        results.add(result);

        // Update counters based on action
        switch (result.getAction()) {
          case CREATED -> created++;
          case UPDATED -> updated++;
          case SKIPPED -> skipped++;
        }

      }

      log.info("[importFromExcel] Import completed - Created: {}, Updated: {}, Skipped: {}, Failed: {}",
        created, updated, skipped, failed);

      return new ImportSummary(results, sheet.getLastRowNum(), created, updated, skipped, failed);

    } catch (IOException e) {
      log.error("[importFromExcel] Error reading Excel file", e);
      throw new StreemException("Error reading Excel file: " + e.getMessage());
    }
  }

  private List<String> parseExcelHeaders(Row headerRow) throws StreemException {
    List<String> headers = new ArrayList<>();
    for (Cell cell : headerRow) {
      if (cell != null && cell.getCellType() == CellType.STRING) {
        if (!(cell.getStringCellValue().trim().startsWith("relation") || cell.getStringCellValue().trim().startsWith("property"))) {
          ValidationUtils.invalidate(cell.getStringCellValue().trim(), ErrorCode.INCORRECT_EXCEL_COLUMN_FOUND);
        }
        headers.add(cell.getStringCellValue().trim());
      } else {
        headers.add("");
      }
    }
    return headers;
  }

  private Map<String, ObjectId> createPropertyMapping(ObjectType objectType, List<String> headers) throws StreemException {
    Map<String, ObjectId> mapping = new HashMap<>();

    for (Property property : objectType.getProperties()) {
      String expectedHeader = "property_" + property.getExternalId();
      if (headers.contains(expectedHeader)) {
        if (property.getUsageStatus() == UsageStatus.DEPRECATED.getCode()) {
          ValidationUtils.invalidate(property.getExternalId(), ErrorCode.ENTITY_OBJECT_PROPERTIES_MISSING);
        }
        mapping.put(expectedHeader, property.getId());
      }
    }

    return mapping;
  }

  private Map<String, ObjectId> createRelationMapping(ObjectType objectType, List<String> headers) throws StreemException {
    Map<String, ObjectId> mapping = new HashMap<>();

    for (Relation relation : objectType.getRelations()) {
      String expectedHeader = "relation_" + relation.getExternalId();
      if (headers.contains(expectedHeader)) {
        if (relation.getUsageStatus() == UsageStatus.DEPRECATED.getCode()) {
          ValidationUtils.invalidate(relation.getExternalId(), ErrorCode.ENTITY_OBJECT_RELATIONS_MISSING);
        }
        mapping.put(expectedHeader, relation.getId());
      }
    }

    return mapping;
  }

  private EntityObjectValueRequest createEntityObjectRequest(Row row, List<String> headers, ObjectType objectType,
                                                             Map<String, ObjectId> propertyMapping, Map<String, ObjectId> relationMapping, Facility facility) throws StreemException, ResourceNotFoundException {

    EntityObjectValueRequest request = new EntityObjectValueRequest();
    request.setObjectTypeId(objectType.getId().toString());
    request.setReason("Bulk import from Excel");

    Map<String, Object> properties = new HashMap<>();
    Map<String, List<PartialEntityObject>> relations = new HashMap<>();

    // Process each cell in the row
    for (int cellIndex = 0; cellIndex < headers.size() && cellIndex < row.getLastCellNum(); cellIndex++) {
      String header = headers.get(cellIndex);
      Cell cell = row.getCell(cellIndex);

      if (header.startsWith("property_")) {
        ObjectId propertyId = propertyMapping.get(header);
        if (propertyId != null) {
          Property property = findPropertyById(objectType, propertyId);
          // Enhanced: Pass property context for Excel date detection
          String cellValue = getCellValueAsString(cell, property, facility);
          // ALWAYS add property, even if empty (null means clear the property)
          Object parsedValue = Utility.isEmpty(cellValue) ? null : parsePropertyValue(cellValue, property, facility);
          properties.put(propertyId.toString(), parsedValue);
        }
      } else if (header.startsWith("relation_")) {
        ObjectId relationId = relationMapping.get(header);
        if (relationId != null) {
          // For relations, no property context needed
          String cellValue = getCellValueAsString(cell, null, facility);
          if (!Utility.isEmpty(cellValue)) {
            Relation relation = findRelationById(objectType, relationId);
            List<PartialEntityObject> targets = parseRelationTargets(cellValue, relation);
            relations.put(relationId.toString(), targets);
          } else {
            // Empty relation means clear it
            relations.put(relationId.toString(), new ArrayList<>());
          }
        }
      }
    }

    request.setProperties(properties);
    request.setRelations(relations);

    return request;
  }

  private String getCellValueAsString(Cell cell, Property property, Facility facility) {
    if (cell == null) {
      return "";
    }

    switch (cell.getCellType()) {
      case STRING:
        return cell.getStringCellValue().trim();
      case NUMERIC:
        if (DateUtil.isCellDateFormatted(cell)) {
          // if excel detects date properties, convert to facility format string
          if (property != null && isDateProperty(property)) {
            Date excelDate = cell.getDateCellValue();
            return convertExcelDateToFacilityFormat(excelDate, property.getInputType(), facility);
          }
          return String.valueOf(cell.getDateCellValue().getTime());
        } else {
          // Handle both integers and decimals
          double numericValue = cell.getNumericCellValue();
          if (numericValue == Math.floor(numericValue)) {
            return String.valueOf((long) numericValue);
          } else {
            return String.valueOf(numericValue);
          }
        }
      case BOOLEAN:
        return String.valueOf(cell.getBooleanCellValue());
      case FORMULA:
        return cell.getCellFormula();
      default:
        return "";
    }
  }

  private Property findPropertyById(ObjectType objectType, ObjectId propertyId) {
    return objectType.getProperties().stream()
      .filter(p -> p.getId().equals(propertyId))
      .findFirst()
      .orElse(null);
  }

  private Relation findRelationById(ObjectType objectType, ObjectId relationId) {
    return objectType.getRelations().stream()
      .filter(r -> r.getId().equals(relationId))
      .findFirst()
      .orElse(null);
  }

  private Object parsePropertyValue(String cellValue, Property property, Facility facility) throws StreemException, ResourceNotFoundException {
    if (CollectionMisc.PROPERTY_DROPDOWN_TYPES.contains(property.getInputType())) {
      // Handle multi-select choices
      return parseChoices(cellValue, property);
    } else if (isDateProperty(property)) {
      // Both Excel dates and manual text use same validation
      Long timestamp = parseDateTimeWithFacilityFormat(cellValue, property.getInputType(), facility);
      return timestamp != null ? timestamp.toString() : null;
    } else if (property.getInputType() == CollectionMisc.PropertyType.NUMBER) {
      return parseNumberValue(cellValue);
    } else {
      return cellValue;
    }
  }

  private String parseNumberValue(String cellValue) throws StreemException {
    if (Utility.isEmpty(cellValue)) {
      return null;
    }
    
    try {
      // Validate it's a valid number
      Double.valueOf(cellValue.trim());
      // Return original format (already correctly formatted by getCellValueAsString)
      return cellValue.trim();
      
    } catch (NumberFormatException e) {
      ValidationUtils.invalidate(cellValue, ErrorCode.ENTITY_OBJECT_PROPERTY_INVALID_INPUT);
      return null;
    }
  }

  private List<String> parseChoices(String cellValue, Property property) throws ResourceNotFoundException, StreemException {
    String[] displayNames = cellValue.split(",");
    Set<String> seen = new HashSet<>(); // For duplicate detection
    List<String> choiceIds = new ArrayList<>();
    
    Map<String, PropertyOption> optionMap = property.getOptions().stream()
      .collect(Collectors.toMap(PropertyOption::getDisplayName, Function.identity()));

    // ✅ EARLY VALIDATION: Check array length for single-select BEFORE processing
    if (property.getInputType() == CollectionMisc.PropertyType.SINGLE_SELECT && displayNames.length > 1) {
      ValidationUtils.invalidate("",ErrorCode.ENTITY_OBJECT_PROPERTY_INVALID_INPUT);
    }
    // ✅ UNIFIED PROCESSING
    for (String displayName : displayNames) {
      String trimmed = displayName.trim();
      if (trimmed.isEmpty()) continue;
      // ✅ DUPLICATE CHECK
      if (!seen.add(trimmed)) {
        ValidationUtils.invalidate("",ErrorCode.ENTITY_OBJECT_PROPERTY_INVALID_INPUT);
      }
      // ✅ VALIDATE CHOICE EXISTS
      PropertyOption option = optionMap.get(trimmed);
      if (Utility.isEmpty(option)) {
        throw new ResourceNotFoundException(trimmed, ErrorCode.ENTITY_OBJECT_PROPERTY_INVALID_INPUT, ExceptionType.ENTITY_NOT_FOUND);
      }
      // ✅ ADD TO RESULT
      choiceIds.add(option.getId().toString());
    }
    
    return choiceIds;
  }

  private List<PartialEntityObject> parseRelationTargets(String cellValue, Relation relation) throws ResourceNotFoundException, StreemException {
    String[] externalIds = cellValue.split(",");
    Set<String> seen = new HashSet<>(); // For duplicate detection
    List<PartialEntityObject> targets = new ArrayList<>();
    
    // Get target collection info (same for both cardinalities)
    ObjectType targetObjectType = objectTypeRepository.findById(relation.getObjectTypeId())
      .orElseThrow(() -> new ResourceNotFoundException(relation.getObjectTypeId(), ErrorCode.OBJECT_TYPE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    String targetCollection = targetObjectType.getExternalId();
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    String facilityId = principalUser.getCurrentFacilityId().toString();

    // ✅ EARLY VALIDATION: Check array length for one-to-one
    if (relation.getTarget().getCardinality() == CollectionMisc.Cardinality.ONE_TO_ONE && externalIds.length > 1) {
      ValidationUtils.invalidate("",ErrorCode.ENTITY_OBJECT_PROPERTY_INVALID_INPUT);
    }
    // ✅ UNIFIED PROCESSING
    for (String externalId : externalIds) {
      String trimmed = externalId.trim();
      if (trimmed.isEmpty()) continue;
      
      // ✅ DUPLICATE CHECK
      if (!seen.add(trimmed)) {
        ValidationUtils.invalidate("",ErrorCode.ENTITY_OBJECT_PROPERTY_INVALID_INPUT);
      }
      // ✅ VALIDATE TARGET EXISTS AND CREATE PARTIAL OBJECT
      Optional<EntityObject> targetObjectOpt = entityObjectRepository
        .findByExternalIdAndUsageStatusAndFacilityId(targetCollection, trimmed, UsageStatus.ACTIVE.getCode(), facilityId);

      if (targetObjectOpt.isPresent()) {
        EntityObject targetObject = targetObjectOpt.get();
        PartialEntityObject partialObject = new PartialEntityObject();
        partialObject.setId(targetObject.getId());
        partialObject.setExternalId(trimmed);
        partialObject.setCollection(targetCollection);
        partialObject.setDisplayName(targetObject.getDisplayName());
        targets.add(partialObject);
      } else {
        throw new ResourceNotFoundException(trimmed, ErrorCode.ENTITY_OBJECT_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND);
      }
    }
    return targets;
  }

  private ImportResult processRowWithDuplicateCheck(Row row, List<String> headers, ObjectType objectType,
                                                    Map<String, ObjectId> propertyMapping, Map<String, ObjectId> relationMapping,
                                                    String facilityId, Facility facility, int rowNumber) throws StreemException, ResourceNotFoundException, JsonProcessingException {

    // 1. Extract externalId from Excel row first
    String externalId = extractExternalIdFromRow(row, headers, objectType, propertyMapping);

    if (Utility.isEmpty(externalId)) {
      // If no external ID, create full request and save new object
      EntityObjectValueRequest request = createEntityObjectRequest(row, headers, objectType, propertyMapping, relationMapping, facility);
      // Validate mandatory fields have values for CREATE
      validateMandatoryFieldValues(request, objectType);
      EntityObject created = entityObjectService.save(request, null);
      return new ImportResult(created, com.leucine.streem.dto.ImportAction.CREATED,
        "Created new object", rowNumber);
    }

    // 2. Check for existing object using unique index fields
    Optional<EntityObject> existingOpt = entityObjectRepository
      .findByExternalIdAndUsageStatusAndFacilityId(
        objectType.getExternalId(), externalId, UsageStatus.ACTIVE.getCode(), facilityId);

    if (existingOpt.isPresent()) {
      EntityObject existing = existingOpt.get();

      // 3. Create delta request with ONLY changed values
      EntityObjectValueRequest deltaRequest = createDeltaUpdateRequest(
        row, headers, objectType, propertyMapping, relationMapping, existing, facility);

      // 4. Check if there are any changes
      if (deltaRequest.getProperties().isEmpty() && deltaRequest.getRelations().isEmpty()) {
        return new ImportResult(existing, com.leucine.streem.dto.ImportAction.SKIPPED,
          "No changes detected", rowNumber);
      } else {
        // Validate mandatory fields in delta request
        validateMandatoryFieldValues(deltaRequest, objectType);

        // 5. Use our custom selective update method to avoid clearing unchanged relations
        EntityObject updated = updateEntityObjectSelectively(existing, deltaRequest, objectType);
        return new ImportResult(updated, com.leucine.streem.dto.ImportAction.UPDATED,
          "Updated " + deltaRequest.getProperties().size() + " properties and " +
          deltaRequest.getRelations().size() + " relations", rowNumber);
      }
    } else {
      // CREATE: Object doesn't exist - create full request
      EntityObjectValueRequest request = createEntityObjectRequest(row, headers, objectType, propertyMapping, relationMapping, facility);
      // Validate mandatory fields have values for CREATE
      validateMandatoryFieldValues(request, objectType);
      EntityObject created = entityObjectService.save(request, null);
      return new ImportResult(created, com.leucine.streem.dto.ImportAction.CREATED,
        "Created new object", rowNumber);
    }
  }

  private String extractExternalIdFromRow(Row row, List<String> headers, ObjectType objectType, Map<String, ObjectId> propertyMapping) {
    // Find the external_id property in the object type
    for (Property property : objectType.getProperties()) {
      if (CollectionKey.EXTERNAL_ID.equals(property.getExternalId())) {
        String expectedHeader = "property_" + property.getExternalId();
        if (headers.contains(expectedHeader)) {
          int cellIndex = headers.indexOf(expectedHeader);
          if (cellIndex >= 0 && cellIndex < row.getLastCellNum()) {
            Cell cell = row.getCell(cellIndex);
            return getCellValueAsString(cell, property, null);
          }
        }
      }
    }
    return null;
  }

  private EntityObjectValueRequest createDeltaUpdateRequest(Row row, List<String> headers, ObjectType objectType,
                                                            Map<String, ObjectId> propertyMapping, Map<String, ObjectId> relationMapping,
                                                            EntityObject existingObject, Facility facility) throws StreemException, ResourceNotFoundException {
    EntityObjectValueRequest request = new EntityObjectValueRequest();
    request.setObjectTypeId(objectType.getId().toString());
    request.setReason("Bulk import from Excel - Delta Update");

    Map<String, Object> changedProperties = new HashMap<>();
    Map<String, List<PartialEntityObject>> changedRelations = new HashMap<>();

    // Create maps for existing values for efficient lookup
    Map<String, PropertyValue> existingPropertyMap = new HashMap<>();
    if (existingObject.getProperties() != null) {
      for (PropertyValue pv : existingObject.getProperties()) {
        existingPropertyMap.put(pv.getId().toString(), pv);
      }
    }

    Map<String, MappedRelation> existingRelationMap = new HashMap<>();
    if (existingObject.getRelations() != null) {
      for (MappedRelation mr : existingObject.getRelations()) {
        existingRelationMap.put(mr.getId().toString(), mr);
      }
    }

    // Check properties for changes - ONLY process fields present in Excel headers
    for (Property property : objectType.getProperties()) {
      if (CREATE_PROPERTIES.contains(property.getExternalId()) ||
          property.getUsageStatus() != UsageStatus.ACTIVE.getCode()) {
        continue; // Skip system and archived properties
      }

      String expectedHeader = "property_" + property.getExternalId();

      if (headers.contains(expectedHeader)) {
        // Property exists in Excel - get Excel value and compare
        int cellIndex = headers.indexOf(expectedHeader);
        Object excelValue = null;

        if (cellIndex >= 0 && cellIndex < row.getLastCellNum()) {
          Cell cell = row.getCell(cellIndex);
          String cellValue = getCellValueAsString(cell, property, facility);
          excelValue = Utility.isEmpty(cellValue) ? null : parsePropertyValue(cellValue, property, facility);
        }

        // Get existing value
        Object existingValue = getExistingPropertyValue(existingPropertyMap.get(property.getId().toString()), property);

        // Compare values - include change if different
        if (!Objects.equals(excelValue, existingValue)) {
          changedProperties.put(property.getId().toString(), excelValue);
        } else if (((property.getFlags() >> CollectionMisc.Flag.IS_MANDATORY.get() & 1)) == 1 && Utility.isEmpty(excelValue) && Utility.isEmpty(existingValue)) {
          // Both null/empty for mandatory field - throw error immediately
          ValidationUtils.invalidate("", ErrorCode.OBJECT_TYPE_MANDATORY_PROPERTIES_NOT_SET);
        }
      }
      // If property not in Excel headers, skip completely (don't touch existing value)
    }

    // Check relations for changes - ONLY process fields present in Excel headers
    for (Relation relation : objectType.getRelations()) {
      if (relation.getUsageStatus() != UsageStatus.ACTIVE.getCode()) {
        continue; // Skip archived relations
      }

      String expectedHeader = "relation_" + relation.getExternalId();

      if (headers.contains(expectedHeader)) {
        // Relation exists in Excel - get Excel value and compare
        int cellIndex = headers.indexOf(expectedHeader);
        List<PartialEntityObject> excelTargets = new ArrayList<>();

        if (cellIndex >= 0 && cellIndex < row.getLastCellNum()) {
          Cell cell = row.getCell(cellIndex);
          String cellValue = getCellValueAsString(cell, null, facility);
          if (!Utility.isEmpty(cellValue)) {
            excelTargets = parseRelationTargets(cellValue, relation);
          }
        }

        // Get existing value
        List<String> existingTargetIds = getExistingRelationTargets(existingRelationMap.get(relation.getId().toString()));

        // Compare values
        List<String> excelTargetIds = excelTargets.stream()
          .map(target -> target.getExternalId())
          .sorted()
          .collect(Collectors.toList());

        if (!Objects.equals(excelTargetIds, existingTargetIds)) {
          changedRelations.put(relation.getId().toString(), excelTargets);
        } else if (((relation.getFlags() >> CollectionMisc.Flag.IS_MANDATORY.get() & 1)) == 1
          && excelTargets.isEmpty() && existingTargetIds.isEmpty()) {

          // Both empty for mandatory relation - throw error immediately
          ValidationUtils.invalidate("", ErrorCode.OBJECT_TYPE_MANDATORY_RELATIONS_NOT_SET);
        }
      }
      // If relation not in Excel headers, skip completely (don't touch existing value)
    }

    request.setProperties(changedProperties);
    request.setRelations(changedRelations);
    return request;
  }

  private Object getExistingPropertyValue(PropertyValue propertyValue, Property property) {
    if (propertyValue == null) {
      return null;
    }

    if (CollectionMisc.PROPERTY_DROPDOWN_TYPES.contains(property.getInputType())) {
      if (propertyValue.getChoices() != null && !propertyValue.getChoices().isEmpty()) {
        // For multi-select, return list of choice IDs
        return propertyValue.getChoices().stream()
          .map(c -> c.getId().toString())
          .collect(Collectors.toList());
      }
    } else {
      // For regular values
      return propertyValue.getValue();
    }
    return null;
  }

  private List<String> getExistingRelationTargets(MappedRelation mappedRelation) {
    if (mappedRelation == null || mappedRelation.getTargets() == null || mappedRelation.getTargets().isEmpty()) {
      return new ArrayList<>();
    }

    return mappedRelation.getTargets().stream()
      .map(target -> target.getExternalId())
      .sorted()
      .collect(Collectors.toList());
  }


  private Long parseDateTimeWithFacilityFormat(String cellValue, CollectionMisc.PropertyType inputType, Facility facility) throws StreemException {
    if (Utility.isEmpty(cellValue)) return null;

    try {
      // Validate against facility's date format
      return parseFormattedDateString(cellValue, inputType, facility);

    } catch (Exception e) {
      String expectedFormat = inputType == CollectionMisc.PropertyType.DATE ?
                             facility.getDateFormat() : facility.getDateTimeFormat();

      String errorMessage = String.format(
        ErrorCode.ENTITY_OBJECT_DATE_FORMAT_INVALID.getDescription(),
        inputType.name().toLowerCase().replace("_", " "),
        inputType.name().toLowerCase().replace("_", " "),
        expectedFormat
      );

      ValidationUtils.invalidate(cellValue, ErrorCode.ENTITY_OBJECT_DATE_FORMAT_INVALID, errorMessage);
      return null;
    }
  }

  private Long parseFormattedDateString(String dateString, CollectionMisc.PropertyType inputType, Facility facility) throws StreemException {
    try {
      String pattern = inputType == CollectionMisc.PropertyType.DATE ?
                      facility.getDateFormat() : facility.getDateTimeFormat();

      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
      ZoneId facilityZone = ZoneId.of(facility.getTimeZone());

      if (inputType == CollectionMisc.PropertyType.DATE) {
        // Parse date in facility timezone, then convert to UTC
        LocalDate localDate = LocalDate.parse(dateString, formatter);
        // ✅ FIXED: Use end of day for DATE type
        ZonedDateTime facilityDateTime = localDate.atTime(LocalTime.MAX).atZone(facilityZone);
        return facilityDateTime.toEpochSecond();
      } else {
        // Parse datetime in facility timezone, then convert to UTC
        LocalDateTime localDateTime = LocalDateTime.parse(dateString, formatter);
        ZonedDateTime facilityDateTime = localDateTime.atZone(facilityZone);
        return facilityDateTime.toEpochSecond();
      }

    } catch (DateTimeParseException e) {
      throw new StreemException("Date parsing failed: " + e.getMessage());
    }
  }

  private String formatDatePropertySafely(Object value, CollectionMisc.PropertyType inputType, Facility facility) {
    if (Utility.isEmpty(value)) return "";

    try {
      Long timestamp = Long.parseLong(value.toString());
      if (inputType == CollectionMisc.PropertyType.DATE) {
        String formattedDate = DateTimeUtils.getFormattedDateForFacility(timestamp, facility);
        return formattedDate != null ? formattedDate : "";
      } else {
        String formattedDateTime = DateTimeUtils.getFormattedDateTimeForFacility(timestamp, facility);
        return formattedDateTime != null ? formattedDateTime : "";
      }
    } catch (Exception e) {
      log.warn("[formatDatePropertySafely] Error formatting date: {}", value, e);
      return value.toString();
    }
  }

  private void validateMandatoryFields(ObjectType objectType, List<String> headers) throws StreemException {
    // Check mandatory properties
    for (Property property : objectType.getProperties()) {
      if (((property.getFlags() >> CollectionMisc.Flag.IS_MANDATORY.get() & 1)) == 1 &&
          property.getUsageStatus() == UsageStatus.ACTIVE.getCode()) {
        String expectedHeader = "property_" + property.getExternalId();
        if (!headers.contains(expectedHeader)) {
          ValidationUtils.invalidate("", ErrorCode.ENTITY_OBJECT_PROPERTIES_MISSING);
        }
      }
    }

    // Check mandatory relations
    for (Relation relation : objectType.getRelations()) {
      if (((relation.getFlags() >> CollectionMisc.Flag.IS_MANDATORY.get() & 1)) == 1 &&
          relation.getUsageStatus() == UsageStatus.ACTIVE.getCode()) {
        String expectedHeader = "relation_" + relation.getExternalId();
        if (!headers.contains(expectedHeader)) {
          ValidationUtils.invalidate("", ErrorCode.ENTITY_OBJECT_RELATIONS_MISSING);
        }
      }
    }
  }

  private void validateMandatoryFieldValues(EntityObjectValueRequest request, ObjectType objectType) throws StreemException {
    // Check mandatory properties that are being changed
    for (Map.Entry<String, Object> entry : request.getProperties().entrySet()) {
      String propertyId = entry.getKey();
      Object value = entry.getValue();

      // Find the property definition
      Property property = findPropertyById(objectType, new ObjectId(propertyId));
      if (requiresUserInput(property)) {
        // Validate the value is not null/empty
        if (Utility.isEmpty(value)) {
          ValidationUtils.invalidate("", ErrorCode.OBJECT_TYPE_MANDATORY_PROPERTIES_NOT_SET);
        }
      }
    }

    // Check mandatory relations that are being changed
    for (Map.Entry<String, List<PartialEntityObject>> entry : request.getRelations().entrySet()) {
      String relationId = entry.getKey();
      List<PartialEntityObject> targets = entry.getValue();
      // Find the relation definition
      Relation relation = findRelationById(objectType, new ObjectId(relationId));
      if (relation != null && isMandatoryRelation(relation) && isActiveRelation(relation)) {
        // Validate the targets list is not null/empty
        if (Utility.isEmpty(targets)) {
          ValidationUtils.invalidate("", ErrorCode.OBJECT_TYPE_MANDATORY_RELATIONS_NOT_SET);
        }
      }
    }
  }

  private EntityObject updateEntityObjectSelectively(EntityObject existingObject,
                                                    EntityObjectValueRequest deltaRequest,
                                                    ObjectType objectType) throws JsonProcessingException, ResourceNotFoundException, StreemException {

    // ✅ AUDIT: Create temporary copy for change tracking BEFORE modifications
    EntityObject oldEntityObject = JsonUtils.readValue(JsonUtils.writeValueAsString(existingObject), EntityObject.class);

    // Update only changed properties
    updateChangedProperties(existingObject, deltaRequest.getProperties(), objectType);

    // Update only changed relations
    updateChangedRelations(existingObject, deltaRequest.getRelations(), objectType);

    // Update metadata
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    existingObject.setModifiedAt(DateTimeUtils.now());
    existingObject.setModifiedBy(userInfoMapper.toUserInfo(principalUser));

    // Save directly to repository (bypass EntityObjectService)
    EntityObject updatedEntityObject = entityObjectRepository.save(existingObject, objectType.getExternalId());

    // ✅ AUDIT: Log changes AFTER save
    saveObjectLogs(principalUser, oldEntityObject, updatedEntityObject, deltaRequest.getReason(), null);

    return updatedEntityObject;
  }

  private void updateChangedProperties(EntityObject object,
                                     Map<String, Object> changedProperties,
                                     ObjectType objectType) throws StreemException {
    if (changedProperties.isEmpty()) return;

    // Create map of existing properties for quick lookup
    Map<String, PropertyValue> existingPropertyMap = object.getProperties().stream()
        .collect(Collectors.toMap(pv -> pv.getId().toString(), Function.identity()));

    // Update only the properties that changed
    for (Map.Entry<String, Object> entry : changedProperties.entrySet()) {
      String propertyId = entry.getKey();
      Object newValue = entry.getValue();

      PropertyValue existingProperty = existingPropertyMap.get(propertyId);
      if (existingProperty != null) {
        // Update existing property value
        updatePropertyValue(existingProperty, newValue, objectType);

        // Update searchable map and handle special properties
        Property property = findPropertyById(objectType, new ObjectId(propertyId));
        if (property != null) {
          // Sync displayName property to EntityObject.displayName
          if (CollectionKey.DISPLAY_NAME.equals(property.getExternalId())) {
            object.setDisplayName(newValue != null ? newValue.toString() : null);
          }
          
          updateSearchableMapForProperty(object, property, newValue);
        }
      }
    }
  }

  private void updateChangedRelations(EntityObject object,
                                    Map<String, List<PartialEntityObject>> changedRelations,
                                    ObjectType objectType) {
    if (changedRelations.isEmpty()) return;

    // Create map of existing relations for quick lookup
    Map<String, MappedRelation> existingRelationMap = object.getRelations().stream()
        .collect(Collectors.toMap(mr -> mr.getId().toString(), Function.identity()));

    // Update only the relations that changed
    for (Map.Entry<String, List<PartialEntityObject>> entry : changedRelations.entrySet()) {
      String relationId = entry.getKey();
      List<PartialEntityObject> newTargets = entry.getValue();

      // Find relation definition
      Relation relation = findRelationById(objectType, new ObjectId(relationId));
      if (relation == null) continue;

      // Create new MappedRelation
      MappedRelation newMappedRelation = createMappedRelationFromTargets(relation, newTargets);

      // Replace or add the relation
      MappedRelation existingRelation = existingRelationMap.get(relationId);
      if (existingRelation != null) {
        // Replace existing relation
        int index = object.getRelations().indexOf(existingRelation);
        object.getRelations().set(index, newMappedRelation);
      } else {
        // Add new relation (shouldn't happen in updates, but just in case)
        object.getRelations().add(newMappedRelation);
      }

      // Update searchable map
      updateSearchableMapForRelation(object, relationId, newTargets);
    }
  }

  private void updatePropertyValue(PropertyValue propertyValue, Object newValue, ObjectType objectType) {
    if (newValue == null) {
      propertyValue.setValue(null);
      propertyValue.setChoices(new ArrayList<>());
    } else if (newValue instanceof List) {
      // Handle choice properties
      List<String> choiceIds = (List<String>) newValue;
      Property property = findPropertyById(objectType, propertyValue.getId());
      if (property != null && CollectionMisc.PROPERTY_DROPDOWN_TYPES.contains(property.getInputType())) {
        // Convert choice IDs to PropertyOptions
        List<PropertyOption> choices = choiceIds.stream()
            .map(choiceId -> property.getOptions().stream()
                .filter(option -> option.getId().toString().equals(choiceId))
                .findFirst().orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        propertyValue.setChoices(choices);
        propertyValue.setValue(null);
      }
    } else {
      // Handle regular properties
      propertyValue.setValue(newValue.toString());
      propertyValue.setChoices(new ArrayList<>());
    }
  }

  private MappedRelation createMappedRelationFromTargets(Relation relation, List<PartialEntityObject> targets) {
    MappedRelation mappedRelation = new MappedRelation();
    mappedRelation.setId(relation.getId());
    mappedRelation.setExternalId(relation.getExternalId());
    mappedRelation.setDisplayName(relation.getDisplayName());
    mappedRelation.setObjectTypeId(relation.getObjectTypeId());
    mappedRelation.setFlags(relation.getFlags());

    // Convert PartialEntityObject to MappedRelationTarget
    List<MappedRelationTarget> mappedTargets = targets.stream()
        .sorted(Comparator.comparing(PartialEntityObject::getExternalId))
        .map(target -> {
          MappedRelationTarget mappedTarget = new MappedRelationTarget();
          mappedTarget.setId(target.getId());
          mappedTarget.setType(relation.getTarget().getType());
          mappedTarget.setCollection(target.getCollection());
          mappedTarget.setExternalId(target.getExternalId());
          mappedTarget.setDisplayName(target.getDisplayName());
          return mappedTarget;
        })
        .collect(Collectors.toList());

    mappedRelation.setTargets(mappedTargets);
    return mappedRelation;
  }

  private void updateSearchableMapForProperty(EntityObject object, Property property, Object value) throws StreemException {
    if (value == null) {
      object.getSearchable().put(property.getId(), null);
    } else if (CollectionMisc.PROPERTY_DROPDOWN_TYPES.contains(property.getInputType())) {
      if (value instanceof List) {
        List<String> choiceIds = (List<String>) value;
        if (property.getInputType() == CollectionMisc.PropertyType.SINGLE_SELECT && !choiceIds.isEmpty()) {
          object.getSearchable().put(property.getId(), choiceIds.get(0));
        } else {
          object.getSearchable().put(property.getId(), choiceIds);
        }
      }
    } else {
      switch (property.getInputType()) {
        case DATE, DATE_TIME -> {
          try {
            Long timestamp = Long.valueOf(value.toString());
            object.getSearchable().put(property.getId(), timestamp);
          } catch (NumberFormatException e) {
            object.getSearchable().put(property.getId(), value.toString());
          }
        }
        case NUMBER -> {
          try {
            Double number = Double.valueOf(value.toString());
            object.getSearchable().put(property.getId(), number);
          } catch (NumberFormatException e) {
            // ✅ FIX: Throw validation error instead of defaulting to 0
            ValidationUtils.invalidate(value.toString(), ErrorCode.ENTITY_OBJECT_PROPERTY_INVALID_INPUT, 
              "Invalid number format for searchable field");
          }
        }
        case SINGLE_LINE, MULTI_LINE -> object.getSearchable().put(property.getId(), value.toString());
      }
    }
  }

  private void updateSearchableMapForRelation(EntityObject object, String relationId, List<PartialEntityObject> targets) {
    List<String> targetIds = targets.stream()
        .map(target -> target.getId().toString())
        .collect(Collectors.toList());
    object.getSearchable().put(new ObjectId(relationId), targetIds);
  }

  // ✅ AUDIT: Add audit logging method (same as EntityObjectService)
  private void saveObjectLogs(PrincipalUser principalUser, EntityObject oldEntityObject, EntityObject updatedEntityObject, String reason, Object jobProcessInfoView) throws ResourceNotFoundException {
    entityObjectChangeLogService.save(principalUser, oldEntityObject, updatedEntityObject, reason, null);
  }

  // ✅ Helper methods for clean flag checking
  private boolean isMandatory(Property property) {
    return ((property.getFlags() >> CollectionMisc.Flag.IS_MANDATORY.get()) & 1) == 1;
  }

  private boolean isAutoGenerate(Property property) {
    return ((property.getFlags() >> CollectionMisc.Flag.IS_AUTOGENERATE.get()) & 1) == 1;
  }

  private boolean isActive(Property property) {
    return property.getUsageStatus() == UsageStatus.ACTIVE.getCode();
  }

  private boolean requiresUserInput(Property property) {
    return property != null && isActive(property) && !isAutoGenerate(property) && isMandatory(property);
  }

  private boolean isMandatoryRelation(Relation relation) {
    return ((relation.getFlags() >> CollectionMisc.Flag.IS_MANDATORY.get()) & 1) == 1;
  }

  private boolean isActiveRelation(Relation relation) {
    return relation.getUsageStatus() == UsageStatus.ACTIVE.getCode();
  }

  private boolean isDateProperty(Property property) {
    return property.getInputType() == CollectionMisc.PropertyType.DATE ||
      property.getInputType() == CollectionMisc.PropertyType.DATE_TIME;
  }

  private String convertExcelDateToFacilityFormat(Date excelDate, CollectionMisc.PropertyType inputType, Facility facility) {
    long epochSeconds = DateTimeUtils.getEpochTime(excelDate);
    return inputType == CollectionMisc.PropertyType.DATE ?
      DateTimeUtils.getFormattedDateForFacility(epochSeconds, facility) :
      DateTimeUtils.getFormattedDateTimeForFacility(epochSeconds, facility);
  }

}
