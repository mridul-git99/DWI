package com.leucine.streem.service.impl;

import com.leucine.streem.ObjectTypeCustomView;
import com.leucine.streem.collections.CustomView;
import com.leucine.streem.collections.CustomViewColumn;
import com.leucine.streem.collections.helper.MongoFilter;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.request.CustomViewRequest;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.ExceptionType;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.JobLogColumn;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.repository.*;
import com.leucine.streem.service.ICustomViewService;
import com.leucine.streem.util.DateTimeUtils;
import com.leucine.streem.util.Utility;
import com.leucine.streem.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomViewService implements ICustomViewService {
  private final IChecklistRepository checklistRepository;
  private final ICustomViewRepository customViewRepository;
  private final IObjectTypeCustomViewRepository objectTypeCustomViewRepository;
  private final IFacilityRepository facilityRepository;
  private final IFacilityUseCaseMappingRepository facilityUseCaseMappingRepository;
  private final MongoTemplate mongoTemplate;

  @Override
  public List<CustomView> getAllCustomViews(String filters) {
    log.info("[getAllCustomViews] Request to get custom views, filters: {}", filters);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    // TODO return only selected fields
    Query query = MongoFilter.buildQueryWithFacilityId(filters, String.valueOf(principalUser.getCurrentFacilityId()));
    long count = mongoTemplate.count(query, CustomView.class);
    List<CustomView> customViews = mongoTemplate.find(query, CustomView.class);
    return customViews;
  }

  @Override
  public CustomView getCustomViewById(String customViewId) throws ResourceNotFoundException {
    log.info("[getCustomViewById] Request to get a custom view, customViewId: {}", customViewId);

    return customViewRepository.findById(customViewId)
      .orElseThrow(() -> new ResourceNotFoundException(customViewId, ErrorCode.CUSTOM_VIEW_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
  }

  @Override
  public CustomView createCustomView(Long checklistId, CustomViewRequest customViewRequest) throws ResourceNotFoundException, StreemException {
    log.info("[createCustomView] Request to create custom view, checklistId: {}, customViewRequest: {}", checklistId, customViewRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Checklist checklist = checklistRepository.findById(checklistId)
      .orElseThrow(() -> new ResourceNotFoundException(checklistId, ErrorCode.PROCESS_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    customViewRequest.setUseCaseId(checklist.getUseCaseId());
    var customView = createCustomViewFromRequest(checklist.getId(), customViewRequest, principalUser);
    customViewRepository.save(customView);

    return customView;
  }

  @Override
  public CustomView createCustomView(CustomViewRequest customViewRequest) throws StreemException {
    log.info("[createCustomView] Request to create custom view, customViewRequest: {}", customViewRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    var customView = createCustomViewFromRequest(null, customViewRequest, principalUser);
    customViewRepository.save(customView);

    return customView;
  }

  @Override
  public CustomView editCustomView(String customViewId, CustomViewRequest customViewRequest) throws ResourceNotFoundException {
    log.info("[editCustomView] Request to edit custom view, customViewId: {}, customViewRequest: {}", customViewId, customViewRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    CustomView customView = customViewRepository.findById(customViewId)
      .orElseThrow(() -> new ResourceNotFoundException(customViewId, ErrorCode.CUSTOM_VIEW_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    customView.setColumns(customViewRequest.getColumns());
    customView.setFilters(customViewRequest.getFilters());
    if (!Utility.isEmpty(customViewRequest.getLabel())) {
      customView.setLabel(customViewRequest.getLabel());
    }
    customView.setModifiedBy(String.valueOf(principalUser.getId()));
    customView.setModifiedAt(DateTimeUtils.now());

    customViewRepository.save(customView);
    return customView;
  }

  @Override
  public BasicDto archiveCustomView(String customViewId) throws ResourceNotFoundException {
    log.info("[archiveCustomView] Archive custom view, customViewId: {}", customViewId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    CustomView customView = customViewRepository.findById(customViewId)
      .orElseThrow(() -> new ResourceNotFoundException(customViewId, ErrorCode.CUSTOM_VIEW_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    customView.setArchived(true);
    customView.setModifiedBy(String.valueOf(principalUser.getId()));
    customView.setModifiedAt(DateTimeUtils.now());

    customViewRepository.save(customView);

    BasicDto basicDto = new BasicDto();
    basicDto.setMessage("success");
    return basicDto;
  }

  private CustomView createCustomViewFromRequest(Long checklistId, CustomViewRequest customViewRequest, PrincipalUser principalUser) throws StreemException {
    var customView = new CustomView();
    if (null != checklistId) {
      customView.setProcessId(String.valueOf(checklistId));
    }

    Long facilityId = principalUser.getCurrentFacilityId();
    Long useCaseId = customViewRequest.getUseCaseId();
    Facility facility = facilityRepository.getOne(facilityId);
    FacilityUseCaseMapping facilityUseCaseMapping = facilityUseCaseMappingRepository.findByFacilityIdAndUseCaseId(facility.getId(), useCaseId);

    UseCase useCase = facilityUseCaseMapping != null ? facilityUseCaseMapping.getUseCase() : null;

    if (useCase == null) {
      ValidationUtils.invalidate(useCaseId, ErrorCode.USE_CASE_NOT_FOUND);
    }

    customView.setUseCaseId(String.valueOf(customViewRequest.getUseCaseId()));
    customView.setFacilityId(String.valueOf(principalUser.getCurrentFacilityId()));
    customView.setTargetType(customViewRequest.getTargetType());
    customView.setFilters(customViewRequest.getFilters());
    customView.setColumns(customViewRequest.getColumns());
    if (Utility.trimAndCheckIfEmpty(customViewRequest.getLabel())) {
      ValidationUtils.invalidate(customViewRequest.getLabel(), ErrorCode.CUSTOM_VIEW_LABEL_INVALID);
    }
    customView.setLabel(customViewRequest.getLabel());
    Long time = DateTimeUtils.now();
    customView.setCreatedAt(time);
    customView.setModifiedAt(time);
    customView.setCreatedBy(String.valueOf(principalUser.getId()));
    customView.setModifiedBy(String.valueOf(principalUser.getId()));

    return customView;
  }

  @Override
  public void reConfigureCustomView(Long checklistId, List<JobLogColumn> jobLogColumns) {
    Map<String, JobLogColumn> jobLogColumnMap = jobLogColumns.stream()
      .collect(Collectors.toMap(
        jobLogColumn -> jobLogColumn.getId() + jobLogColumn.getTriggerType().toString(),
        Function.identity()
      ));

    Page<CustomView> customViews = customViewRepository.findByProcessIdAndTargetType(
      checklistId.toString(), Type.ConfigurableViewTargetType.PROCESS, null);

    customViews.forEach(customView -> updateCustomViewColumns(customView, jobLogColumnMap));

    customViewRepository.saveAll(customViews);
  }

  @Override
  public Page<ObjectTypeCustomView> getAllObjectTypeCustomViews(String filters, Pageable pageable) {
    log.info("[getAllObjectTypeCustomViews] Request to get object type custom views, filters: {}, pageable: {}", filters, pageable);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    Query query = MongoFilter.buildQueryWithFacilityId(filters, String.valueOf(principalUser.getCurrentFacilityId()));
    long count = mongoTemplate.count(query, ObjectTypeCustomView.class);
    query.with(pageable);
    List<ObjectTypeCustomView> objectTypeCustomViews = mongoTemplate.find(query, ObjectTypeCustomView.class);
    return PageableExecutionUtils.getPage(objectTypeCustomViews, pageable, () -> count);

  }

  @Override
  public ObjectTypeCustomView createObjectTypeCustomView(String objectTypeId, CustomViewRequest customViewRequest) throws StreemException {
    log.info("[createObjectTypeCustomView] Request to create object type custom view, objectTypeId: {}, customViewRequest: {}", objectTypeId, customViewRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    var objectTypeCustomView = createObjectTypeCustomViewFromRequest(objectTypeId, customViewRequest, principalUser);

    ObjectTypeCustomView exsitingObjectTypeCustomView = objectTypeCustomViewRepository.findByObjectTypeIdAndLabel(objectTypeCustomView.getObjectTypeId(), objectTypeCustomView.getLabel());
    if (exsitingObjectTypeCustomView != null) {
      ValidationUtils.invalidate(objectTypeCustomView.getLabel(), ErrorCode.VIEW_NAME_IS_ALREADY_IN_USE);
    }
    objectTypeCustomViewRepository.save(objectTypeCustomView);
    return objectTypeCustomView;
  }

  @Override
  public ObjectTypeCustomView editObjectTypeCustomView(String customViewId, CustomViewRequest customViewRequest) throws ResourceNotFoundException {
    log.info("[editObjectTypeCustomView] Request to edit object type custom view, customViewId: {}, customViewRequest: {}", customViewId, customViewRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    ObjectTypeCustomView objectTypeCustomView = objectTypeCustomViewRepository.findById(customViewId)
      .orElseThrow(() -> new ResourceNotFoundException(customViewId, ErrorCode.CUSTOM_VIEW_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    objectTypeCustomView.setColumns(customViewRequest.getColumns());
    objectTypeCustomView.setFilters(customViewRequest.getFilters());
    if (!Utility.isEmpty(customViewRequest.getLabel())) {
      objectTypeCustomView.setLabel(customViewRequest.getLabel());
    }
    objectTypeCustomView.setModifiedBy(String.valueOf(principalUser.getId()));
    objectTypeCustomView.setModifiedAt(DateTimeUtils.now());

    objectTypeCustomViewRepository.save(objectTypeCustomView);
    return objectTypeCustomView;
  }

  @Override
  public BasicDto archiveObjectTypeCustomView(String customViewId) throws ResourceNotFoundException {
    log.info("[archiveObjectTypeCustomView] Archive object type custom view, customViewId: {}", customViewId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    ObjectTypeCustomView objectTypeCustomView = objectTypeCustomViewRepository.findById(customViewId)
      .orElseThrow(() -> new ResourceNotFoundException(customViewId, ErrorCode.CUSTOM_VIEW_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    objectTypeCustomView.setArchived(true);
    objectTypeCustomView.setModifiedBy(String.valueOf(principalUser.getId()));
    objectTypeCustomView.setModifiedAt(DateTimeUtils.now());

    objectTypeCustomViewRepository.save(objectTypeCustomView);

    BasicDto basicDto = new BasicDto();
    basicDto.setMessage("success");
    return basicDto;
  }

  @Override
  public BasicDto unarchiveObjectTypeCustomView(String customViewId) throws ResourceNotFoundException {
    log.info("unarchiveObjectTypeCustomView] Archive object type custom view, customViewId: {}", customViewId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    ObjectTypeCustomView objectTypeCustomView = objectTypeCustomViewRepository.findById(customViewId)
      .orElseThrow(() -> new ResourceNotFoundException(customViewId, ErrorCode.CUSTOM_VIEW_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    objectTypeCustomView.setArchived(false);
    objectTypeCustomView.setModifiedBy(String.valueOf(principalUser.getId()));
    objectTypeCustomView.setModifiedAt(DateTimeUtils.now());

    objectTypeCustomViewRepository.save(objectTypeCustomView);

    BasicDto basicDto = new BasicDto();
    basicDto.setMessage("success");
    return basicDto;
  }

  private ObjectTypeCustomView createObjectTypeCustomViewFromRequest(String objectTypeId, CustomViewRequest customViewRequest, PrincipalUser principalUser) throws StreemException {

    var objectTypeCustomView = new ObjectTypeCustomView();
    if (null != objectTypeId) {
      objectTypeCustomView.setObjectTypeId(objectTypeId);
    }
    Long facilityId = principalUser.getCurrentFacilityId();
    Long useCaseId = customViewRequest.getUseCaseId();
    Facility facility = facilityRepository.getOne(facilityId);
    FacilityUseCaseMapping facilityUseCaseMapping = facilityUseCaseMappingRepository.findByFacilityIdAndUseCaseId(facility.getId(), useCaseId);

    UseCase useCase = facilityUseCaseMapping != null ? facilityUseCaseMapping.getUseCase() : null;

    if (useCase == null) {
      ValidationUtils.invalidate(useCaseId, ErrorCode.USE_CASE_NOT_FOUND);
    }

    objectTypeCustomView.setUseCaseId(String.valueOf(customViewRequest.getUseCaseId()));
    objectTypeCustomView.setFacilityId(String.valueOf(principalUser.getCurrentFacilityId()));
    objectTypeCustomView.setFilters(customViewRequest.getFilters());
    objectTypeCustomView.setColumns(customViewRequest.getColumns());
    if (Utility.trimAndCheckIfEmpty(customViewRequest.getLabel())) {
      ValidationUtils.invalidate(customViewRequest.getLabel(), ErrorCode.CUSTOM_VIEW_LABEL_INVALID);
    }
    objectTypeCustomView.setLabel(customViewRequest.getLabel());
    Long time = DateTimeUtils.now();
    objectTypeCustomView.setCreatedAt(time);
    objectTypeCustomView.setModifiedAt(time);
    objectTypeCustomView.setCreatedBy(String.valueOf(principalUser.getId()));
    objectTypeCustomView.setModifiedBy(String.valueOf(principalUser.getId()));

    return objectTypeCustomView;
  }


  private void updateCustomViewColumns(CustomView customView, Map<String, JobLogColumn> jobLogColumnMap) {
    customView.getFilters().forEach(customViewFilter -> {
      if (customViewFilter.getId() == null) {
        customViewFilter.setId(UUID.randomUUID().toString());
      }
    });
    customView.getColumns().forEach(customViewColumn -> {
      String columnKey = customViewColumn.getId() + customViewColumn.getTriggerType();
      if (jobLogColumnMap.containsKey(columnKey)) {
        updateCustomViewColumn(customViewColumn, jobLogColumnMap.get(columnKey));
      }
    });
  }

  private void updateCustomViewColumn(CustomViewColumn customViewColumn, JobLogColumn jobLogColumn) {
    if (!Objects.equals(jobLogColumn.getDisplayName(), customViewColumn.getDisplayName())) {
      customViewColumn.setDisplayName(jobLogColumn.getDisplayName());
    }
    if (!Objects.equals(jobLogColumn.getType().toString(), customViewColumn.getType())) {
      customViewColumn.setType(jobLogColumn.getType().toString());
    }
  }
}
