package com.leucine.streem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.leucine.streem.collections.ObjectType;
import com.leucine.streem.collections.Property;
import com.leucine.streem.collections.*;
import com.leucine.streem.collections.partial.PartialEntityObject;
import com.leucine.streem.constant.*;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.mapper.IAutomationMapper;
import com.leucine.streem.dto.mapper.ITaskMapper;
import com.leucine.streem.dto.mapper.IUserMapper;
import com.leucine.streem.dto.projection.JobProcessInfoView;
import com.leucine.streem.dto.request.ArchiveObjectRequest;
import com.leucine.streem.dto.request.AutomationRequest;
import com.leucine.streem.dto.request.CreateObjectAutomationRequest;
import com.leucine.streem.dto.request.EntityObjectValueRequest;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.ExceptionType;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.model.helper.parameter.ResourceParameter;
import com.leucine.streem.repository.*;
import com.leucine.streem.service.IEntityObjectService;
import com.leucine.streem.service.IFacilityService;
import com.leucine.streem.service.IJobAuditService;
import com.leucine.streem.service.ITaskAutomationService;
import com.leucine.streem.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskAutomationService implements ITaskAutomationService {
  private final ITaskRepository taskRepository;
  private final IAutomationMapper automationMapper;
  private final IAutomationRepository automationRepository;
  private final IEntityObjectRepository entityObjectRepository;
  private final IParameterRepository parameterRepository;
  private final IParameterValueRepository parameterValueRepository;
  private final ITaskAutomationMappingRepository taskAutomationMappingRepository;
  private final IUserRepository userRepository;
  private final ITaskMapper taskMapper;
  private final IJobRepository jobRepository;
  private final IEntityObjectService entityObjectService;
  private final IObjectTypeRepository objectTypeRepository;
  private final ITaskExecutionRepository taskExecutionRepository;
  private final IJobAuditService jobAuditService;
  private final IUserMapper userMapper;
  private final IFacilityService facilityService;

  @Override
  public TaskDto addTaskAutomation(Long taskId, AutomationRequest automationRequest) throws ResourceNotFoundException, JsonProcessingException, StreemException {
    log.info("[addTaskAutomations] Request to add task automations, taskId: {}, automationRequest: {}", taskId, automationRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User user = userRepository.findById(principalUser.getId())
      .orElseThrow(() -> new ResourceNotFoundException(principalUser.getId(), ErrorCode.USER_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Task task = taskRepository.getReferenceById(taskId);
    if (automationRequest.getTriggerType().equals(Type.AutomationTriggerType.TASK_STARTED)) {
      checkIfAllReferencedParametersInAutomationBelongsToSameTask(automationRequest, task);
    }

    // For current version because create job is managed by UI, we cannot have multiple create job actions at the same time
    if (Type.AutomationActionType.CREATE_OBJECT.equals(automationRequest.getActionType()) || Type.AutomationActionType.BULK_CREATE_OBJECT.equals(automationRequest.getActionType())) {
      boolean createObjectOrBulkCreateObjectAutomationExists = taskAutomationMappingRepository.automationExistsByTaskIdAndTriggerTypeAndAutomationActionTypes(taskId, automationRequest.getTriggerType(), List.of(Type.AutomationActionType.CREATE_OBJECT, Type.AutomationActionType.BULK_CREATE_OBJECT));
      if (createObjectOrBulkCreateObjectAutomationExists) {
        ValidationUtils.invalidate(taskId, ErrorCode.CREATE_OBJECT_OR_BULK_CREATE_OBJECT_AUTOMATION_ALREADY_EXISTS);
      }
    }

    Automation automation = automationMapper.toEntity(automationRequest);

    // TODO: currently considering all the saves as modified by user, need to change this logic
    automation.setModifiedBy(user);
    // TODO: currently considering all the saves as modified by user, need to change this logic
    // TODO: not consistent with how we add many to many mappings change this logic
    automation.setCreatedBy(user);
    if (Utility.isEmpty(automation.getId())) {
      automation.setCreatedBy(user);
      automation.setId(IdGenerator.getInstance().nextId());
    }

    automationRepository.save(automation);

    task.addAutomation(automation, automationRequest, user);
    return taskMapper.toDto(taskRepository.save(task));
  }

  // TODO: can be optimized
  private void checkIfAllReferencedParametersInAutomationBelongsToSameTask(AutomationRequest automationRequest, Task task) throws JsonProcessingException, StreemException {
    log.info("[checkIfAllReferencedParametersInAutomationBelongsToSameTask] Request to validate if parameters are in same task for start task automation with automationRequest: {}, task: {}", automationRequest, task);
    if (automationRequest.getActionType().equals(Type.AutomationActionType.INCREASE_PROPERTY) ||
      automationRequest.getActionType().equals(Type.AutomationActionType.DECREASE_PROPERTY) ||
      automationRequest.getActionType().equals(Type.AutomationActionType.SET_PROPERTY) ||
      automationRequest.getActionType().equals(Type.AutomationActionType.SET_RELATION) ||
        automationRequest.getActionType().equals(Type.AutomationActionType.BULK_CREATE_OBJECT))
     {
      AutomationActionForResourceParameterDto resourceParameterAction = JsonUtils.readValue(automationRequest.getActionDetails().toString(), AutomationActionForResourceParameterDto.class);
      if (!Utility.isEmpty(resourceParameterAction.getReferencedParameterId())) {
        boolean resourceParameterExist = task.getParameters().stream().anyMatch(parameter -> String.valueOf(parameter.getId()).equals(resourceParameterAction.getReferencedParameterId()));
        if (resourceParameterExist) {
          ValidationUtils.invalidate(resourceParameterAction.getReferencedParameterId(), ErrorCode.START_TASK_AUTOMATION_NOT_ALLOWED_FOR_SAME_TASK_PARAMETER);
        }
      }
      if (!Utility.isEmpty(resourceParameterAction.getParameterId())) {
        boolean parameterExist = task.getParameters().stream().anyMatch(parameter -> String.valueOf(parameter.getId()).equals(resourceParameterAction.getParameterId()));
        if (parameterExist) {
          ValidationUtils.invalidate(resourceParameterAction.getReferencedParameterId(), ErrorCode.START_TASK_AUTOMATION_NOT_ALLOWED_FOR_SAME_TASK_PARAMETER);
        }
      }
    } else if (automationRequest.getActionType().equals(Type.AutomationActionType.ARCHIVE_OBJECT)) {
      AutomationActionArchiveObjectDto objectArchiveAction = JsonUtils.readValue(automationRequest.getActionDetails().toString(), AutomationActionArchiveObjectDto.class);
      if (!Utility.isEmpty(objectArchiveAction.getReferencedParameterId())) {
        boolean parameterExist = task.getParameters().stream().anyMatch(parameter -> String.valueOf(parameter.getId()).equals(objectArchiveAction.getReferencedParameterId()));
        if (parameterExist) {
          ValidationUtils.invalidate(objectArchiveAction.getReferencedParameterId(), ErrorCode.START_TASK_AUTOMATION_NOT_ALLOWED_FOR_SAME_TASK_PARAMETER);
        }
      }
    } else if (automationRequest.getActionType().equals(Type.AutomationActionType.CREATE_OBJECT)) {
      AutomationObjectCreationActionDto objectCreationActionDto = JsonUtils.readValue(automationRequest.getActionDetails().toString(), AutomationObjectCreationActionDto.class);
      if (!Utility.isEmpty(objectCreationActionDto.getConfiguration())) {
        List<PropertyParameterMappingDto> propertyParameterMappingDtoList = objectCreationActionDto.getConfiguration();
        Set<String> mappedParameterIds = propertyParameterMappingDtoList.stream().map(PropertyParameterMappingDto::getParameterId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> taskParameterIds = task.getParameters().stream().map(Parameter::getId).map(Object::toString).collect(Collectors.toSet());
        boolean containsCommonElements = !Collections.disjoint(taskParameterIds, mappedParameterIds);
        if (containsCommonElements) {
          Optional<String> firstCommonElement = taskParameterIds.stream()
            .filter(mappedParameterIds::contains)
            .findFirst();
          ValidationUtils.invalidate(firstCommonElement.get(), ErrorCode.START_TASK_AUTOMATION_NOT_ALLOWED_FOR_SAME_TASK_PARAMETER);
        }
      }
    }
  }

  @Override
  public TaskDto updateAutomation(Long taskId, Long automationId, AutomationRequest automationRequest) throws ResourceNotFoundException, StreemException, JsonProcessingException {
    log.info("[updateTaskAutomation] Request to update automation, automationId: {}, automationRequest: {}", automationId, automationRequest);

    Automation automation = automationRepository.findById(automationId)
      .orElseThrow(() -> new ResourceNotFoundException(automationId, ErrorCode.AUTOMATION_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    automationMapper.update(automationRequest, automation);

    TaskAutomationMapping taskAutomationMapping = taskAutomationMappingRepository.findByTaskIdAndAutomationId(taskId, automationId);
    if (automationRequest.getTriggerType() == Type.AutomationTriggerType.TASK_STARTED) {
      checkIfAllReferencedParametersInAutomationBelongsToSameTask(automationRequest, taskAutomationMapping.getTask());
    }
    if (!Utility.isEmpty(automationRequest.getOrderTree())) {
      taskAutomationMapping.setOrderTree(automationRequest.getOrderTree());
    }
    if (!Utility.isEmpty(automationRequest.getDisplayName())) {
      taskAutomationMapping.setDisplayName(automationRequest.getDisplayName());
    }
    Task task = taskRepository.getReferenceById(taskId);
    if (automationRequest.getTriggerType().equals(Type.AutomationTriggerType.TASK_STARTED)) {
      checkIfAllReferencedParametersInAutomationBelongsToSameTask(automationRequest, task);
    }

    automationRepository.save(automation);
    return taskMapper.toDto(taskRepository.findById(taskId).get());
  }

  @Override
  public TaskDto deleteTaskAutomation(Long taskId, Long automationId) throws ResourceNotFoundException {
    log.info("[deleteTaskAutomation] Request to delete task automation, taskId: {}, automationId: {}", taskId, automationId);

    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.findById(principalUser.getId())
      .orElseThrow(() -> new ResourceNotFoundException(principalUser.getId(), ErrorCode.USER_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    taskAutomationMappingRepository.deleteByTaskIdAndAutomationId(taskId, automationId);

    Automation automation = automationRepository.findById(automationId)
      .orElseThrow(() -> new ResourceNotFoundException(automationId, ErrorCode.AUTOMATION_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    automation.setArchived(true);
    automation.setModifiedBy(principalUserEntity);
    automationRepository.save(automation);

    return taskMapper.toDto(taskRepository.findById(taskId).get());
  }

  @Override
  public List<AutomationResponseDto> completeTaskAutomations(Long taskId, Long jobId, List<CreateObjectAutomationRequest> createObjectAutomationRequests, String automationReason, Type.AutomationTriggerType automationTriggerType) throws IOException, ResourceNotFoundException, StreemException {
    log.info("[completeTaskAutomations] Request to complete task automation, taskId: {}, jobId:{}, automationReason: {}", taskId, jobId, automationReason);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    List<Automation> automations = taskAutomationMappingRepository.findAllAutomationsByTaskIdAndTriggerType(taskId, automationTriggerType);

    JobProcessInfoView jobInfo = jobRepository.findJobProcessInfo(jobId);

    String reason = Misc.CHANGED_AS_PER_PROCESS;
    TaskExecution taskExecution;
    Map<Long, EntityObjectValueRequest> automationEntityObjectValueRequestMap = new HashMap<>();
    if (!Utility.isEmpty(createObjectAutomationRequests)) {
      for (CreateObjectAutomationRequest createObjectAutomationRequest : createObjectAutomationRequests) {
        automationEntityObjectValueRequestMap.put(createObjectAutomationRequest.getAutomationId(), createObjectAutomationRequest.getEntityObjectValueRequest());
      }
    }
    User user = userRepository.findById(User.SYSTEM_USER_ID).get();
    PrincipalUser systemPrincipalUser = userMapper.toPrincipalUser(user);

    Set<Long> automationIds = new HashSet<>();
    for (Automation automation : automations) {
      automationIds.add(automation.getId());
    }
    List<TaskAutomationMapping> taskAutomationMappings = taskAutomationMappingRepository.findTaskAutomationMappingByAutomationIdIn(automationIds.stream().toList());
    Map<Long, String> taskAutomationMappingMap = taskAutomationMappings.stream().collect(Collectors.toMap(
      tam -> tam.getTaskAutomationId().getAutomationId(), TaskAutomationMapping::getDisplayName
    ));

    List<AutomationResponseDto> executedAutomations = new ArrayList<>();
    for (Automation automation : automations) {
      String displayName = taskAutomationMappingMap.get(automation.getId());
      // TODO Handle these with switch cases
      switch (automation.getActionType()) {
        case INCREASE_PROPERTY, DECREASE_PROPERTY -> {
          // TODO needs testing, add comments
          if (Type.TargetEntityType.RESOURCE_PARAMETER.equals(automation.getTargetEntityType())) {
            AutomationActionForResourceParameterDto resourceParameterAction = JsonUtils.readValue(automation.getActionDetails().toString(), AutomationActionForResourceParameterDto.class);
            Type.SelectorType selector = resourceParameterAction.getSelector();
            Long referencedParameterId = Long.valueOf(resourceParameterAction.getReferencedParameterId());
            Parameter referencedParameter = parameterRepository.findById(referencedParameterId).get();
            ParameterValue referencedParameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, referencedParameterId);

            boolean skipExecution = skipAutomationExecution(referencedParameterValue, referencedParameter);
            if (skipExecution) {
              Parameter parameter = null;
              if (selector != Type.SelectorType.CONSTANT) {
                Long parameterId = Long.valueOf(resourceParameterAction.getParameterId());
                parameter = parameterRepository.findById(parameterId).orElse(null);
              }
              jobAuditService.skipIncreaseOrDecreaseAutomation(taskId, jobId, automation.getActionType(), referencedParameter, parameter, resourceParameterAction, systemPrincipalUser, displayName);
              continue;
            } else if (isParameterNotExecuted(referencedParameterValue, referencedParameter) ) {
              ValidationUtils.invalidate(automation.getId(), ErrorCode.AUTOMATION_WILL_NOT_RUN_DUE_TO_MISSING_RESOURCES);
            }
            ResourceParameter resourceParameter = JsonUtils.readValue(referencedParameter.getData().toString(), ResourceParameter.class);
            List<ResourceParameterChoiceDto> parameterChoices = JsonUtils.jsonToCollectionType(referencedParameterValue.getChoices(), List.class, ResourceParameterChoiceDto.class);
            if (!Utility.isEmpty(parameterChoices)) {
              for (ResourceParameterChoiceDto resourceParameterChoice : parameterChoices) {
                EntityObject entityObject = getEntityObject(resourceParameter.getObjectTypeExternalId(), resourceParameterChoice.getObjectId());
                Map<String, PropertyValue> propertyValueMap = entityObject.getProperties().stream().collect(Collectors.toMap(pv -> pv.getId().toString()
                  , Function.identity()));

                PropertyValue propertyValue = getAndValidatePropertyValue(automation, propertyValueMap, resourceParameterAction, resourceParameter);

                String valueToUpdate;
                if (Objects.equals(resourceParameterAction.getSelector(), Type.SelectorType.PARAMETER)) {
                  Long parameterId = Long.valueOf(resourceParameterAction.getParameterId());
                  Parameter parameter = parameterRepository.findById(parameterId).orElseThrow();
                  ParameterValue parameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, parameterId);

                  if (skipAutomationExecution(parameterValue, parameter)) {
                    jobAuditService.skipIncreaseOrDecreaseAutomation(taskId, jobId, automation.getActionType(), referencedParameter, parameter, resourceParameterAction, systemPrincipalUser, displayName);
                    continue;
                  } else if (isParameterNotExecuted(parameterValue, parameter) ) {
                      ValidationUtils.invalidate(automation.getId(), ErrorCode.AUTOMATION_WILL_NOT_RUN_DUE_TO_MISSING_RESOURCES);
                  }
                  if (Utility.isEmpty(parameterValue) || Utility.isEmpty(parameterValue.getValue())) {
                    ValidationUtils.invalidate(automation.getId(), ErrorCode.PROCESS_RELATION_AUTOMATION_ACTION_ERROR);
                  }
                  valueToUpdate = parameterValue.getValue();
                } else {
                  valueToUpdate = resourceParameterAction.getValue();
                  if (Utility.isEmpty(valueToUpdate)) {
                    ValidationUtils.invalidate(automation.getId(), ErrorCode.PROCESS_RELATION_AUTOMATION_ACTION_ERROR);
                  }
                }

                switch (automation.getActionType()) {
                  case INCREASE_PROPERTY -> {
                    BigDecimal value = new BigDecimal(propertyValue.getValue());
                    BigDecimal valueToAdd = new BigDecimal(valueToUpdate);
                    value = value.add(valueToAdd);
                    propertyValue.setValue(value.toPlainString());
                  }
                  case DECREASE_PROPERTY -> {
                    BigDecimal value = new BigDecimal(propertyValue.getValue());
                    BigDecimal valueToSubtract = new BigDecimal(valueToUpdate);
                    value = value.subtract(valueToSubtract);
                    propertyValue.setValue(value.toPlainString());
                  }
                }
                EntityObjectValueRequest entityObjectValueRequest = new EntityObjectValueRequest();
                Map<String, List<PartialEntityObject>> partialEntityObjectMap = new HashMap<>();
                getSelectedRelations(entityObject, partialEntityObjectMap);
                entityObjectValueRequest.setRelations(partialEntityObjectMap);
                entityObjectValueRequest.setProperties(new HashMap<>(Map.of(resourceParameterAction.getPropertyId(), propertyValue.getValue())));
                entityObjectValueRequest.setObjectTypeId(resourceParameter.getObjectTypeId());
                entityObjectValueRequest.setReason(reason);
                entityObjectService.update(entityObject.getId().toString(), entityObjectValueRequest, jobInfo);

                if (selector == Type.SelectorType.CONSTANT) {
                  jobAuditService.increaseOrDecreasePropertyAutomation(taskId, jobId, automation.getActionType(), valueToUpdate, referencedParameter, null, resourceParameterAction, systemPrincipalUser, displayName);
                } else if (selector == Type.SelectorType.PARAMETER) {
                  Long parameterId = Long.valueOf(resourceParameterAction.getParameterId());
                  Parameter parameter = parameterRepository.findById(parameterId).get();
                  jobAuditService.increaseOrDecreasePropertyAutomation(taskId, jobId, automation.getActionType(), valueToUpdate, referencedParameter, parameter, resourceParameterAction, systemPrincipalUser, displayName);
                }
                executedAutomations.add(AutomationResponseDto.builder().id(automation.getId().toString()).actionType(automation.getActionType()).build());
              }
            } else {
              ValidationUtils.invalidate(automation.getId(), ErrorCode.AUTOMATION_WILL_NOT_RUN_DUE_TO_MISSING_RESOURCES);
            }
          }
        }
        case SET_PROPERTY -> {
          // TODO task gets completed
          AutomationSetPropertyBaseDto automationBase = JsonUtils.readValue(automation.getActionDetails().toString(), AutomationSetPropertyBaseDto.class);

          if (automationBase.getPropertyInputType().equals(CollectionMisc.PropertyType.DATE_TIME) || automationBase.getPropertyInputType().equals(CollectionMisc.PropertyType.DATE)) {
            AutomationActionDateTimeDto automationActionDateTimeDto = JsonUtils.readValue(automation.getActionDetails().toString(), AutomationActionDateTimeDto.class);
            Long referencedParameterId = Long.valueOf(automationActionDateTimeDto.getReferencedParameterId());
            ParameterValue referencedParameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, referencedParameterId);
            Type.SelectorType selector = automationActionDateTimeDto.getSelector();
            Parameter referencedParameter = parameterRepository.findById(referencedParameterId).get();
            if (skipAutomationExecution(referencedParameterValue, referencedParameter)) {
              jobAuditService.skipSetPropertyAutomation(taskId, jobId, automationActionDateTimeDto, null, referencedParameter, systemPrincipalUser, displayName);
              continue;
            } else if (isParameterNotExecuted(referencedParameterValue, referencedParameter) ) {
              ValidationUtils.invalidate(automation.getId(), ErrorCode.AUTOMATION_WILL_NOT_RUN_DUE_TO_MISSING_RESOURCES);
            }
            ResourceParameter resourceParameter = JsonUtils.readValue(referencedParameter.getData().toString(), ResourceParameter.class);

            List<ResourceParameterChoiceDto> parameterChoices = JsonUtils.jsonToCollectionType(referencedParameterValue.getChoices(), List.class, ResourceParameterChoiceDto.class);

            if (!Utility.isEmpty(parameterChoices)) {
              for (ResourceParameterChoiceDto resourceParameterChoice : parameterChoices) {

                EntityObject entityObject = getEntityObject(resourceParameter.getObjectTypeExternalId(), resourceParameterChoice.getObjectId());
                ObjectType objectType = objectTypeRepository.findById(entityObject.getObjectTypeId().toString()).get();

                Map<String, PropertyValue> propertyValueMap = entityObject.getProperties().stream().collect(Collectors.toMap(pv -> pv.getId().toString()
                  , Function.identity()));

                Set<String> propertyIds = getAllPropertyIdsOfObjectType(objectType);

                if (!propertyIds.contains(automationActionDateTimeDto.getPropertyId())) {
                  ValidationUtils.invalidate(automation.getId(), ErrorCode.RESOURCE_PARAMETER_AUTOMATION_ACTION_ERROR);
                }

                PropertyValue propertyValue = propertyValueMap.get(automationActionDateTimeDto.getPropertyId());

                Long baseTime = 0L;
                Long endTime = null;
                if (Objects.equals(selector, Type.SelectorType.PARAMETER)) {
                  Parameter parameter = parameterRepository.findById(Long.valueOf(automationActionDateTimeDto.getParameterId())).get();
                  ParameterValue parameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, Long.valueOf(automationActionDateTimeDto.getParameterId()));
                  if (skipAutomationExecution(parameterValue, parameter)) {
                    jobAuditService.skipSetPropertyAutomation(taskId, jobId, automationActionDateTimeDto, null, referencedParameter, systemPrincipalUser, displayName);
                    continue;
                  } else if (isParameterNotExecuted(referencedParameterValue, referencedParameter) ) {
                    ValidationUtils.invalidate(automation.getId(), ErrorCode.AUTOMATION_WILL_NOT_RUN_DUE_TO_MISSING_RESOURCES);
                  }
                  if (Utility.isEmpty(parameterValue) || Utility.isEmpty(parameterValue.getValue())) {
                    ValidationUtils.invalidate(automation.getId(), ErrorCode.PROCESS_RELATION_AUTOMATION_ACTION_ERROR);
                  }
                  endTime = Long.valueOf(parameterValue.getValue());
                } else {
                  switch (automationActionDateTimeDto.getCaptureProperty()) {
                    case START_TIME, END_TIME: {
                      baseTime = getTaskStartOrEndTime(jobId, automationActionDateTimeDto, baseTime);
                      FacilityDto facilityDto = facilityService.getFacility(principalUser.getCurrentFacilityId());
                      Double offsetValue = calculateOffsetValue(automationActionDateTimeDto, jobId, taskId, systemPrincipalUser, displayName, referencedParameter, automation);
                      if (offsetValue == null) continue;
                      endTime = processDateTimeAdjustment(automationActionDateTimeDto, baseTime, facilityDto, offsetValue, automation);
                      break;
                    }
                    case CONSTANT: {
                      baseTime = automationActionDateTimeDto.getValue();
                      FacilityDto facilityDto = facilityService.getFacility(principalUser.getCurrentFacilityId());
                      Double offsetValue = calculateOffsetValue(automationActionDateTimeDto, jobId, taskId, systemPrincipalUser, displayName, referencedParameter, automation);
                      if (offsetValue == null) continue;
                      endTime = processDateTimeAdjustment(automationActionDateTimeDto, baseTime, facilityDto, offsetValue, automation);
                      break;
                    }
                    case PARAMETER: {
                      Parameter parameter = parameterRepository.findById(Long.valueOf(automationActionDateTimeDto.getParameterId())).get();
                      ParameterValue parameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, Long.valueOf(automationActionDateTimeDto.getParameterId()));
                      if (skipAutomationExecution(parameterValue, parameter)) {
                        jobAuditService.skipSetPropertyAutomation(taskId, jobId, automationActionDateTimeDto, null, referencedParameter, systemPrincipalUser, displayName);
                        continue;
                      } else if (isParameterNotExecuted(referencedParameterValue, referencedParameter) ) {
                        ValidationUtils.invalidate(automation.getId(), ErrorCode.AUTOMATION_WILL_NOT_RUN_DUE_TO_MISSING_RESOURCES);
                      }
                      if (Utility.isEmpty(parameterValue) || Utility.isEmpty(parameterValue.getValue())) {
                        ValidationUtils.invalidate(automation.getId(), ErrorCode.PROCESS_RELATION_AUTOMATION_ACTION_ERROR);
                      }
                      baseTime = Long.valueOf(parameterValue.getValue());

                      FacilityDto facilityDto = facilityService.getFacility(principalUser.getCurrentFacilityId());
                      Double offsetValue = calculateOffsetValue(automationActionDateTimeDto, jobId, taskId, systemPrincipalUser, displayName, referencedParameter, automation);
                      if (offsetValue == null) continue;
                      endTime = processDateTimeAdjustment(automationActionDateTimeDto, baseTime, facilityDto, offsetValue, automation);
                      break;
                    }
                  }
                }
                propertyValue.setValue(String.valueOf(endTime));
                EntityObjectValueRequest entityObjectValueRequest = new EntityObjectValueRequest();
                Map<String, List<PartialEntityObject>> partialEntityObjectMap = new HashMap<>();
                getSelectedRelations(entityObject, partialEntityObjectMap);
                entityObjectValueRequest.setRelations(partialEntityObjectMap);
                entityObjectValueRequest.setProperties(new HashMap<>(Map.of(automationActionDateTimeDto.getPropertyId(), propertyValue.getValue())));
                entityObjectValueRequest.setObjectTypeId(resourceParameter.getObjectTypeId());
                entityObjectValueRequest.setReason(reason);

                entityObjectService.update(entityObject.getId().toString(), entityObjectValueRequest, jobInfo);
                jobAuditService.setPropertyAutomation(endTime, taskId, jobId, automationActionDateTimeDto, null, referencedParameter, systemPrincipalUser, displayName);
                executedAutomations.add(AutomationResponseDto.builder().id(automation.getId().toString()).actionType(automation.getActionType()).build());
              }
            } else {
              ValidationUtils.invalidate(automation.getId(), ErrorCode.AUTOMATION_WILL_NOT_RUN_DUE_TO_MISSING_RESOURCES);
            }
          } else if (automationBase.getPropertyInputType().equals(CollectionMisc.PropertyType.NUMBER)) {
            AutomationActionSetPropertyDto automationSetProperty = JsonUtils.readValue(automation.getActionDetails().toString(), AutomationActionSetPropertyDto.class);
            Long referencedParameterId = Long.valueOf(automationSetProperty.getReferencedParameterId());
            ParameterValue referencedParameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, referencedParameterId);
            Type.SelectorType selector = automationSetProperty.getSelector();
            Parameter referencedParameter = parameterRepository.findById(referencedParameterId).get();
            if (skipAutomationExecution(referencedParameterValue, referencedParameter)) {
              jobAuditService.skipSetPropertyAutomation(taskId, jobId, null, automationSetProperty, referencedParameter, systemPrincipalUser, displayName);
              continue;
            } else if (isParameterNotExecuted(referencedParameterValue, referencedParameter) ) {
              ValidationUtils.invalidate(automation.getId(), ErrorCode.AUTOMATION_WILL_NOT_RUN_DUE_TO_MISSING_RESOURCES);
            }
            ResourceParameter resourceParameter = JsonUtils.readValue(referencedParameter.getData().toString(), ResourceParameter.class);
            List<ResourceParameterChoiceDto> parameterChoices = JsonUtils.jsonToCollectionType(referencedParameterValue.getChoices(), List.class, ResourceParameterChoiceDto.class);
            if (!Utility.isEmpty(parameterChoices)) {
              for (ResourceParameterChoiceDto resourceParameterChoice : parameterChoices) {
                EntityObject entityObject = getEntityObject(resourceParameter.getObjectTypeExternalId(), resourceParameterChoice.getObjectId());

                ObjectType objectType = objectTypeRepository.findById(entityObject.getObjectTypeId().toString()).get();

                Set<String> propertyIds = getAllPropertyIdsOfObjectType(objectType);

                if (!propertyIds.contains(automationSetProperty.getPropertyId())) {
                  ValidationUtils.invalidate(automation.getId(), ErrorCode.RESOURCE_PARAMETER_AUTOMATION_ACTION_ERROR);
                }

                EntityObjectValueRequest entityObjectValueRequest = new EntityObjectValueRequest();

                String valueToUpdate = null;
                if (Objects.equals(selector, Type.SelectorType.PARAMETER)) {
                  Parameter parameter = parameterRepository.findById(Long.valueOf(automationSetProperty.getParameterId())).get();
                  ParameterValue parameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, Long.valueOf(automationSetProperty.getParameterId()));
                  if (skipAutomationExecution(parameterValue, parameter)) {
                    jobAuditService.skipSetPropertyAutomation(taskId, jobId, null, automationSetProperty, referencedParameter, systemPrincipalUser, displayName);
                    continue;
                  } else if (isParameterNotExecuted(parameterValue,referencedParameter) ) {
                    ValidationUtils.invalidate(automation.getId(), ErrorCode.AUTOMATION_WILL_NOT_RUN_DUE_TO_MISSING_RESOURCES);
                  }
                  if (Utility.isEmpty(parameterValue) || Utility.isEmpty(parameterValue.getValue())) {
                    ValidationUtils.invalidate(automation.getId(), ErrorCode.PROCESS_RELATION_AUTOMATION_ACTION_ERROR);
                  }
                  valueToUpdate = parameterValue.getValue();
                } else {
                  valueToUpdate = String.valueOf(automationSetProperty.getValue());
                  if (Utility.isEmpty(valueToUpdate)) {
                    ValidationUtils.invalidate(automation.getId(), ErrorCode.PROCESS_RELATION_AUTOMATION_ACTION_ERROR);
                  }
                }


                Map<String, List<PartialEntityObject>> partialEntityObjectMap = new HashMap<>();
                getSelectedRelations(entityObject, partialEntityObjectMap);
                entityObjectValueRequest.setRelations(partialEntityObjectMap);
                entityObjectValueRequest.setProperties(new HashMap<>(Map.of(automationSetProperty.getPropertyId(), valueToUpdate)));
                entityObjectValueRequest.setObjectTypeId(resourceParameter.getObjectTypeId());
                entityObjectValueRequest.setReason(reason);

                entityObjectService.update(entityObject.getId().toString(), entityObjectValueRequest, jobInfo);
                jobAuditService.setPropertyAutomation(null, taskId, jobId, null, automationSetProperty, referencedParameter, systemPrincipalUser, displayName);
                executedAutomations.add(AutomationResponseDto.builder().id(automation.getId().toString()).actionType(automation.getActionType()).build());
              }
            } else {
              ValidationUtils.invalidate(automation.getId(), ErrorCode.AUTOMATION_WILL_NOT_RUN_DUE_TO_MISSING_RESOURCES);
            }
          }
          else if (automationBase.getPropertyInputType().equals(CollectionMisc.PropertyType.SINGLE_SELECT)) {
            AutomationActionSetPropertyDto automationSetProperty = JsonUtils.readValue(automation.getActionDetails().toString(), AutomationActionSetPropertyDto.class);

            Long referencedParameterId = Long.valueOf(automationSetProperty.getReferencedParameterId());
            ParameterValue referencedParameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, referencedParameterId);
            Parameter referencedParameter = parameterRepository.findById(referencedParameterId).get();
            ResourceParameter resourceParameter = JsonUtils.readValue(referencedParameter.getData().toString(), ResourceParameter.class);
            if (skipAutomationExecution(referencedParameterValue, referencedParameter)) {
              jobAuditService.skipSetPropertyAutomation(taskId, jobId, null, automationSetProperty, referencedParameter, systemPrincipalUser, displayName);
              continue;
            } else if (isParameterNotExecuted(referencedParameterValue, referencedParameter) ) {
              ValidationUtils.invalidate(automation.getId(), ErrorCode.AUTOMATION_WILL_NOT_RUN_DUE_TO_MISSING_RESOURCES);
            }
            if (automationSetProperty.getSelector() == Type.SelectorType.PARAMETER) {
              Parameter automationSetPropertyReferencedParameter = parameterRepository.findById(Long.valueOf(automationSetProperty.getParameterId())).get();
              ParameterValue parameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, Long.valueOf(automationSetProperty.getParameterId()));
              if (skipAutomationExecution(parameterValue, automationSetPropertyReferencedParameter)) {
                jobAuditService.skipSetPropertyAutomation(taskId, jobId, null, automationSetProperty, referencedParameter, systemPrincipalUser, displayName);
                continue;
              } else if (isParameterNotExecuted(referencedParameterValue, referencedParameter) ) {
                ValidationUtils.invalidate(automation.getId(), ErrorCode.AUTOMATION_WILL_NOT_RUN_DUE_TO_MISSING_RESOURCES);
              }
              Map<String, String> singleSelectChoices = JsonUtils.convertValue(parameterValue.getChoices(), new TypeReference<>() {
              });
              List<ResourceParameterChoiceDto> parameterChoices = JsonUtils.jsonToCollectionType(referencedParameterValue.getChoices(), List.class, ResourceParameterChoiceDto.class);

              for (ResourceParameterChoiceDto resourceParameterChoiceDto : parameterChoices) {

                EntityObject entityObject = getEntityObject(resourceParameter.getObjectTypeExternalId(), resourceParameterChoiceDto.getObjectId());
                ObjectType objectType = objectTypeRepository.findById(entityObject.getObjectTypeId().toString()).get();
                Set<String> propertyIds = getAllPropertyIdsOfObjectType(objectType);

                if (!propertyIds.contains(automationSetProperty.getPropertyId())) {
                  ValidationUtils.invalidate(automation.getId(), ErrorCode.RESOURCE_PARAMETER_AUTOMATION_ACTION_ERROR);
                }
                List<String> selectedChoices = new ArrayList<>();
                for (Map.Entry<String, String> parameter : singleSelectChoices.entrySet()) {
                  if (State.Selection.SELECTED.equals(State.Selection.valueOf(parameter.getValue()))) {
                    selectedChoices.add(parameter.getKey());
                  }
                }

                for (String selectedChoice : selectedChoices) {
                  EntityObjectValueRequest entityObjectValueRequest = new EntityObjectValueRequest();
                  Map<String, List<PartialEntityObject>> partialEntityObjectMap = new HashMap<>();
                  getSelectedRelations(entityObject, partialEntityObjectMap);
                  entityObjectValueRequest.setRelations(partialEntityObjectMap);
                  entityObjectValueRequest.setProperties(new HashMap<>(Map.of(automationSetProperty.getPropertyId(), List.of(selectedChoice))));
                  entityObjectValueRequest.setObjectTypeId(resourceParameter.getObjectTypeId());
                  entityObjectValueRequest.setReason(reason);

                  entityObjectService.update(entityObject.getId().toString(), entityObjectValueRequest, jobInfo);
                }
              }

              jobAuditService.setPropertyAutomation(null, taskId, jobId, null, automationSetProperty, referencedParameter, systemPrincipalUser, displayName);
              executedAutomations.add(AutomationResponseDto.builder().id(automation.getId().toString()).actionType(automation.getActionType()).build());
            } else {
              List<ResourceParameterChoiceDto> parameterChoices = JsonUtils.jsonToCollectionType(referencedParameterValue.getChoices(), List.class, ResourceParameterChoiceDto.class);
              if (!Utility.isEmpty(parameterChoices)) {
                for (ResourceParameterChoiceDto resourceParameterChoice : parameterChoices) {
                  EntityObject entityObject = getEntityObject(resourceParameter.getObjectTypeExternalId(), resourceParameterChoice.getObjectId());

                  ObjectType objectType = objectTypeRepository.findById(entityObject.getObjectTypeId().toString()).get();

                  Set<String> propertyIds = getAllPropertyIdsOfObjectType(objectType);

                  if (!propertyIds.contains(automationSetProperty.getPropertyId())) {
                    ValidationUtils.invalidate(automation.getId(), ErrorCode.RESOURCE_PARAMETER_AUTOMATION_ACTION_ERROR);
                  }

                  EntityObjectValueRequest entityObjectValueRequest = new EntityObjectValueRequest();

                  Object value = automationSetProperty.getValue() == null ? automationSetProperty.getChoices().stream()
                    .map(propertyOption -> propertyOption.getId().toString()).toList()
                    : automationSetProperty.getValue();

                  Map<String, List<PartialEntityObject>> partialEntityObjectMap = new HashMap<>();
                  getSelectedRelations(entityObject, partialEntityObjectMap);
                  entityObjectValueRequest.setRelations(partialEntityObjectMap);
                  entityObjectValueRequest.setProperties(new HashMap<>(Map.of(automationSetProperty.getPropertyId(), value)));
                  entityObjectValueRequest.setObjectTypeId(resourceParameter.getObjectTypeId());
                  entityObjectValueRequest.setReason(reason);

                  entityObjectService.update(entityObject.getId().toString(), entityObjectValueRequest, jobInfo);
                  jobAuditService.setPropertyAutomation(null, taskId, jobId, null, automationSetProperty, referencedParameter, systemPrincipalUser, displayName);
                  executedAutomations.add(AutomationResponseDto.builder().id(automation.getId().toString()).actionType(automation.getActionType()).build());
                }
              } else {
                ValidationUtils.invalidate(automation.getId(), ErrorCode.AUTOMATION_WILL_NOT_RUN_DUE_TO_MISSING_RESOURCES);
              }
            }
          }
          else {
            AutomationActionSetPropertyDto automationSetProperty = JsonUtils.readValue(automation.getActionDetails().toString(), AutomationActionSetPropertyDto.class);

            Long referencedParameterId = Long.valueOf(automationSetProperty.getReferencedParameterId());
            ParameterValue referencedParameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, referencedParameterId);
            Parameter referencedParameter = parameterRepository.findById(referencedParameterId).get();
            ResourceParameter resourceParameter = JsonUtils.readValue(referencedParameter.getData().toString(), ResourceParameter.class);
            if (skipAutomationExecution(referencedParameterValue, referencedParameter)) {
              jobAuditService.skipSetPropertyAutomation(taskId, jobId, null, automationSetProperty, referencedParameter, systemPrincipalUser, displayName);
              continue;
            } else if (isParameterNotExecuted(referencedParameterValue, referencedParameter) ) {
              ValidationUtils.invalidate(automation.getId(), ErrorCode.AUTOMATION_WILL_NOT_RUN_DUE_TO_MISSING_RESOURCES);
            }
            List<ResourceParameterChoiceDto> parameterChoices = JsonUtils.jsonToCollectionType(referencedParameterValue.getChoices(), List.class, ResourceParameterChoiceDto.class);
            if (!Utility.isEmpty(parameterChoices)) {
              for (ResourceParameterChoiceDto resourceParameterChoice : parameterChoices) {
                EntityObject entityObject = getEntityObject(resourceParameter.getObjectTypeExternalId(), resourceParameterChoice.getObjectId());

                ObjectType objectType = objectTypeRepository.findById(entityObject.getObjectTypeId().toString()).get();

                Set<String> propertyIds = getAllPropertyIdsOfObjectType(objectType);

                if (!propertyIds.contains(automationSetProperty.getPropertyId())) {
                  ValidationUtils.invalidate(automation.getId(), ErrorCode.RESOURCE_PARAMETER_AUTOMATION_ACTION_ERROR);
                }

                EntityObjectValueRequest entityObjectValueRequest = new EntityObjectValueRequest();

                Object value = automationSetProperty.getValue() == null ? automationSetProperty.getChoices().stream()
                  .map(propertyOption -> propertyOption.getId().toString()).toList()
                  : automationSetProperty.getValue();

                Map<String, List<PartialEntityObject>> partialEntityObjectMap = new HashMap<>();
                getSelectedRelations(entityObject, partialEntityObjectMap);
                entityObjectValueRequest.setRelations(partialEntityObjectMap);
                entityObjectValueRequest.setProperties(new HashMap<>(Map.of(automationSetProperty.getPropertyId(), value)));
                entityObjectValueRequest.setObjectTypeId(resourceParameter.getObjectTypeId());
                entityObjectValueRequest.setReason(reason);

                entityObjectService.update(entityObject.getId().toString(), entityObjectValueRequest, jobInfo);
                jobAuditService.setPropertyAutomation(null, taskId, jobId, null, automationSetProperty, referencedParameter, systemPrincipalUser, displayName);
                executedAutomations.add(AutomationResponseDto.builder().id(automation.getId().toString()).actionType(automation.getActionType()).build());
              }
            } else {
              ValidationUtils.invalidate(automation.getId(), ErrorCode.AUTOMATION_WILL_NOT_RUN_DUE_TO_MISSING_RESOURCES);
            }
          }
        }
        //System executed Task Automation Set Status: "Cleaning Status" of "Area" in Task "Area Selection"(ID:1.1) was set to "Cleaned" in the Task "Update Quantity" (ID: 3.1) of the Stage "Disinfectant Preparation" (ID: 2)
        case ARCHIVE_OBJECT -> {
          AutomationActionArchiveObjectDto automationActionArchiveObjectDto = JsonUtils.readValue(automation.getActionDetails().toString(), AutomationActionArchiveObjectDto.class);
          Parameter referencedParameter = parameterRepository.findById(Long.valueOf(automationActionArchiveObjectDto.getReferencedParameterId())).get();
          ParameterValue referencedParameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, (Long.valueOf(automationActionArchiveObjectDto.getReferencedParameterId())));
          if (skipAutomationExecution(referencedParameterValue, referencedParameter)) {
            jobAuditService.skipArchiveObjectAutomation(taskId, jobId, referencedParameter, automationActionArchiveObjectDto, systemPrincipalUser, displayName);
            continue;
          } else if (isParameterNotExecuted(referencedParameterValue, referencedParameter) ) {
            ValidationUtils.invalidate(automation.getId(), ErrorCode.AUTOMATION_WILL_NOT_RUN_DUE_TO_MISSING_RESOURCES);
          }
          List<ResourceParameterChoiceDto> parameterChoices = JsonUtils.jsonToCollectionType(referencedParameterValue.getChoices(), List.class, ResourceParameterChoiceDto.class);

          if (Utility.isEmpty(parameterChoices)) {
            ValidationUtils.invalidate(automation.getId(), ErrorCode.RESOURCE_PARAMETER_AUTOMATION_ACTION_ERROR);
          }
          for (ResourceParameterChoiceDto resourceParameterChoice : parameterChoices) {
            entityObjectService.archiveObject(new ArchiveObjectRequest(resourceParameterChoice.getCollection(), reason), resourceParameterChoice.getObjectId(), jobInfo);
            jobAuditService.archiveObjectAutomation(taskId, jobId, resourceParameterChoice, referencedParameter, automationActionArchiveObjectDto, systemPrincipalUser, displayName);

          }
          executedAutomations.add(AutomationResponseDto.builder().id(automation.getId().toString()).actionType(automation.getActionType()).build());
        }

        case SET_RELATION -> {
          //TODO: add validations & refactor
          AutomationActionMappedRelationDto automationActionMappedRelationDto = JsonUtils.readValue(automation.getActionDetails().toString(), AutomationActionMappedRelationDto.class);
          ParameterValue parameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, Long.valueOf(automationActionMappedRelationDto.getParameterId()));
          ParameterValue referencedParameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, Long.valueOf(automationActionMappedRelationDto.getReferencedParameterId()));

          Parameter referencedParameter = parameterRepository.findById(referencedParameterValue.getParameterId()).get();
          Parameter parameter = parameterRepository.findById(parameterValue.getParameterId()).get();

          if (skipAutomationExecution(referencedParameterValue, referencedParameter)) {
            jobAuditService.skipSetRelationAutomation(taskId, jobId, automationActionMappedRelationDto, null, referencedParameter, parameter, systemPrincipalUser, displayName);
            continue;
          }else if (isParameterNotExecuted(referencedParameterValue, referencedParameter) ) {
            ValidationUtils.invalidate(automation.getId(), ErrorCode.AUTOMATION_WILL_NOT_RUN_DUE_TO_MISSING_RESOURCES);
          }
          if (skipAutomationExecution(parameterValue, parameter)) {
            jobAuditService.skipSetRelationAutomation(taskId, jobId, automationActionMappedRelationDto, null, referencedParameter, parameter, systemPrincipalUser, displayName);
            continue;
          } else if (isParameterNotExecuted(parameterValue,referencedParameter) ) {
            ValidationUtils.invalidate(automation.getId(), ErrorCode.AUTOMATION_WILL_NOT_RUN_DUE_TO_MISSING_RESOURCES);
          }
          ResourceParameter resourceParameter = JsonUtils.readValue(referencedParameter.getData().toString(), ResourceParameter.class);
          List<ResourceParameterChoiceDto> referencedParameterChoices = JsonUtils.jsonToCollectionType(referencedParameterValue.getChoices(), List.class, ResourceParameterChoiceDto.class);
          List<ResourceParameterChoiceDto> parameterChoices = JsonUtils.jsonToCollectionType(parameterValue.getChoices(), List.class, ResourceParameterChoiceDto.class);

          if (Utility.isEmpty(referencedParameterChoices) || Utility.isEmpty(parameterChoices)) {
            ValidationUtils.invalidate(automation.getId(), ErrorCode.AUTOMATION_WILL_NOT_RUN_DUE_TO_MISSING_RESOURCES);
          }

          for (ResourceParameterChoiceDto resourceParameterChoiceDto : referencedParameterChoices) {
            EntityObject entityObject = getEntityObject(resourceParameter.getObjectTypeExternalId(), resourceParameterChoiceDto.getObjectId());

            Map<String, List<PartialEntityObject>> partialEntityObjectMap = new HashMap<>();
            // Setting the current relations also in request so these relations doesn't get reset
            getSelectedRelations(entityObject, partialEntityObjectMap);
            partialEntityObjectMap.remove(automationActionMappedRelationDto.getRelationId());

            for (ResourceParameterChoiceDto parameterChoiceDto : parameterChoices) {
              getPartialEntityObjectMap(partialEntityObjectMap, parameterChoiceDto, new ObjectId(automationActionMappedRelationDto.getRelationId()));
            }

            EntityObjectValueRequest entityObjectValueRequest = new EntityObjectValueRequest();
            entityObjectValueRequest.setObjectTypeId(resourceParameter.getObjectTypeId());
            entityObjectValueRequest.setRelations(partialEntityObjectMap);
            entityObjectValueRequest.setReason(reason);

            entityObjectService.update(entityObject.getId().toString(), entityObjectValueRequest, jobInfo);
            String resourceParameterChoices = formatResourceParameterChoices(parameterChoices);
            jobAuditService.setRelationAutomation(taskId, jobId, automationActionMappedRelationDto, entityObject, resourceParameterChoices, referencedParameter, parameter, systemPrincipalUser, displayName);
          }
          executedAutomations.add(AutomationResponseDto.builder().id(automation.getId().toString()).actionType(automation.getActionType()).build());
        }
        case CREATE_OBJECT -> {
          AutomationObjectCreationActionDto automationObjectCreationActionDto = JsonUtils.readValue(automation.getActionDetails().toString(), AutomationObjectCreationActionDto.class);

          if (!automationEntityObjectValueRequestMap.containsKey(automation.getId())) {
            ValidationUtils.invalidate(automation.getId(), ErrorCode.AUTOMATION_WILL_NOT_RUN_DUE_TO_MISSING_RESOURCES);
          }
          List<PropertyParameterMappingDto> propertyParameterMappingDtos = Optional.ofNullable(automationObjectCreationActionDto.getConfiguration()).orElseGet(ArrayList::new);

          for (PropertyParameterMappingDto propertyParameterMappingDto : propertyParameterMappingDtos) {
            if (!Utility.isEmpty(propertyParameterMappingDto.getParameterId())) {
              Long parameterId = Long.valueOf(propertyParameterMappingDto.getParameterId());

              Parameter referencedParameter = parameterRepository.findById(parameterId).orElse(null);
              ParameterValue referencedParameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, parameterId);

              if (referencedParameter == null || referencedParameterValue == null) {
                ValidationUtils.invalidate(automation.getId(), ErrorCode.PROCESS_RELATION_AUTOMATION_ACTION_ERROR);
              }
              if (isParameterNotExecuted(referencedParameterValue, referencedParameter)) {
                ValidationUtils.invalidate(automation.getId(), ErrorCode.AUTOMATION_WILL_NOT_RUN_DUE_TO_MISSING_RESOURCES);
              }
            }
          }
          EntityObjectValueRequest entityObjectValueRequest = automationEntityObjectValueRequestMap.get(automation.getId());
          entityObjectValueRequest.setReason(reason);
          EntityObject entityObject = entityObjectService.save(entityObjectValueRequest, jobInfo);
          jobAuditService.createObjectAutomation(taskId, jobId, entityObject, automationObjectCreationActionDto, systemPrincipalUser, displayName);
          String createdObjectExternalId = null;
          String createdObjectId = null;
          String propertyDisplayName = null;
          Optional<PropertyValue> optionalPropertyValue = entityObject.getProperties().stream()
            .filter(propertyValue -> propertyValue.getExternalId().equals(CollectionKey.EXTERNAL_ID))
            .findFirst();
          Optional<PropertyValue> objectIdentifierPropertyValue = entityObject.getProperties().stream()
            .filter(propertyValue -> propertyValue.getExternalId().equals(CollectionKey.EXTERNAL_ID))
            .findFirst();
          if (optionalPropertyValue.isPresent()) {
            createdObjectExternalId = objectIdentifierPropertyValue.get().getValue();
            createdObjectId = entityObject.getId().toString();
            propertyDisplayName = optionalPropertyValue.get().getDisplayName();
          }

          CreateObjectAutomationResponseDto createObjectAutomationResponseDto = CreateObjectAutomationResponseDto.builder()
            .createdObjectExternalId(createdObjectExternalId)
            .createdObjectId(createdObjectId)
            .build();
          executedAutomations.add(AutomationResponseDto.builder().id(automation.getId().toString()).actionType(automation.getActionType())
            .createObjectAutomationResponseDto(List.of(createObjectAutomationResponseDto))
            .propertyDisplayName(propertyDisplayName).build());
        }
        case BULK_CREATE_OBJECT -> {
          AutomationObjectCreationActionDto automationObjectCreationActionDto = JsonUtils.readValue(automation.getActionDetails().toString(), AutomationObjectCreationActionDto.class);

          int bulkCount = 0;
          Type.SelectorType selector = automationObjectCreationActionDto.getSelector();
          if (selector == null || selector == Type.SelectorType.PARAMETER) {
            Long referencedParameterId = Long.valueOf(automationObjectCreationActionDto.getReferencedParameterId());
            ParameterValue referencedParameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, referencedParameterId);
            bulkCount = Integer.parseInt(referencedParameterValue.getValue());
            if (Utility.isEmpty(referencedParameterValue.getValue())) {
              ValidationUtils.invalidate(automation.getId(), ErrorCode.AUTOMATION_WILL_NOT_RUN_DUE_TO_MISSING_RESOURCES);
            }
          } else {
            bulkCount = Integer.parseInt(automationObjectCreationActionDto.getBulkCount());
          }
          if (!automationEntityObjectValueRequestMap.containsKey(automation.getId())) {
            ValidationUtils.invalidate(automation.getId(), ErrorCode.AUTOMATION_WILL_NOT_RUN_DUE_TO_MISSING_RESOURCES);
          }

          EntityObjectValueRequest entityObjectValueRequest = automationEntityObjectValueRequestMap.get(automation.getId());
          entityObjectValueRequest.setReason(reason);

          List<EntityObject> createdObjects = new ArrayList<>();
          List<CreateObjectAutomationResponseDto> createObjectAutomationResponseDtos = new ArrayList<>();
          for (int i = 0; i < bulkCount; i++) {
            EntityObject entityObject = entityObjectService.save(entityObjectValueRequest, jobInfo);
            jobAuditService.createObjectAutomation(taskId, jobId, entityObject, automationObjectCreationActionDto, systemPrincipalUser, displayName);
            createdObjects.add(entityObject);
          }
          String createdObjectExternalId = null;
          String createdObjectId = null;
          String propertyDisplayName = null;
          for (EntityObject entityObject : createdObjects) {
            Optional<PropertyValue> objectIdentifierPropertyValue = entityObject.getProperties().stream()
              .filter(propertyValue -> propertyValue.getExternalId().equals(CollectionKey.EXTERNAL_ID))
              .findFirst();
            if (objectIdentifierPropertyValue.isPresent()) {
              createdObjectExternalId = objectIdentifierPropertyValue.get().getValue();
              propertyDisplayName = objectIdentifierPropertyValue.get().getDisplayName();
              createdObjectId = entityObject.getId().toString();
            }
            CreateObjectAutomationResponseDto createObjectAutomationResponseDto = CreateObjectAutomationResponseDto.builder()
              .createdObjectExternalId(createdObjectExternalId)
              .createdObjectId(createdObjectId)
              .build();
            createObjectAutomationResponseDtos.add(createObjectAutomationResponseDto);
          }
          executedAutomations.add(AutomationResponseDto.builder().id(automation.getId().toString()).actionType(automation.getActionType())
            .createObjectAutomationResponseDto(createObjectAutomationResponseDtos)
            .propertyDisplayName(propertyDisplayName).build());
        }
      }
    }

    return executedAutomations;
  }

  private Long getTaskStartOrEndTime(Long jobId, AutomationActionDateTimeDto automationActionDateTimeDto, Long timeRequired) {
    TaskExecution taskExecution;
    if (Objects.requireNonNull(automationActionDateTimeDto.getEntityType()) == Type.EntityType.TASK) {
      taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(Long.valueOf(automationActionDateTimeDto.getEntityId()), jobId);
      if (Type.AutomationDateTimeCaptureType.START_TIME.equals(automationActionDateTimeDto.getCaptureProperty())) {
        timeRequired = taskExecution.getStartedAt();
      } else {
        timeRequired = taskExecution.getEndedAt();
      }
    }
    return timeRequired;
  }

  private long calculateAdjustedTime(long timeRequired, String timeZone, TemporalUnit unit, Double offsetValue) {
    ZonedDateTime zonedDateTime = DateTimeUtils.convertEpochToZonedDateTime(timeRequired, timeZone);

    zonedDateTime = applyRecursiveAdjustment(zonedDateTime, unit, BigDecimal.valueOf(offsetValue));
    // For DAYS, MONTHS, and YEARS adjustments, set the time to the end of the day
    if (unit == ChronoUnit.DAYS || unit == ChronoUnit.MONTHS || unit == ChronoUnit.YEARS) {
      zonedDateTime = zonedDateTime.with(LocalTime.MAX);
    }
    return DateTimeUtils.convertZonedDateTimeToEpoch(zonedDateTime);
  }

  private ZonedDateTime applyRecursiveAdjustment(ZonedDateTime zonedDateTime, TemporalUnit unit, BigDecimal offsetValue) {
    long wholeUnits = (long) offsetValue.doubleValue();

    BigDecimal inputValue = offsetValue;
    BigDecimal wholeUnitsBigDecimal = BigDecimal.valueOf(wholeUnits);
    BigDecimal fractionalPartBigDecimal = inputValue.subtract(wholeUnitsBigDecimal);
    double fractionalPart = fractionalPartBigDecimal.doubleValue();


    zonedDateTime = zonedDateTime.plus(wholeUnits, unit);

    if (fractionalPart > 0) {
      if (unit == ChronoUnit.YEARS) {
        return applyRecursiveAdjustment(zonedDateTime, ChronoUnit.MONTHS, fractionalPartBigDecimal.multiply(BigDecimal.valueOf(12)));
      } else if (unit == ChronoUnit.MONTHS) {
        int daysInMonth = zonedDateTime.getMonth().length(zonedDateTime.toLocalDate().isLeapYear()); // Handles leap year
        return applyRecursiveAdjustment(zonedDateTime, ChronoUnit.DAYS, fractionalPartBigDecimal.multiply(BigDecimal.valueOf(daysInMonth)));
      } else if (unit == ChronoUnit.DAYS) {
        return applyRecursiveAdjustment(zonedDateTime, ChronoUnit.HOURS, fractionalPartBigDecimal.multiply(BigDecimal.valueOf(24)));
      } else if (unit == ChronoUnit.HOURS) {
        return applyRecursiveAdjustment(zonedDateTime, ChronoUnit.MINUTES, fractionalPartBigDecimal.multiply(BigDecimal.valueOf(60)));
      } else if (unit == ChronoUnit.MINUTES) {
        return applyRecursiveAdjustment(zonedDateTime, ChronoUnit.SECONDS, fractionalPartBigDecimal.multiply(BigDecimal.valueOf(60)));
      } else if (unit == ChronoUnit.SECONDS) {
        long fractionalSeconds = (long) (fractionalPart);
        return zonedDateTime.plusSeconds(fractionalSeconds);
      }
    }

    return zonedDateTime;
  }

  private long processDateTimeAdjustment(AutomationActionDateTimeDto automationActionDateTimeDto, long timeRequired, FacilityDto facilityDto, Double offsetValue, Automation automation) throws StreemException {
    long dateTimeValue = 0;
    switch (automationActionDateTimeDto.getOffsetDateUnit()) {
      case SECONDS ->
        dateTimeValue = calculateAdjustedTime(timeRequired, facilityDto.getTimeZone(), ChronoUnit.SECONDS, offsetValue);
      case MINUTES ->
        dateTimeValue = calculateAdjustedTime(timeRequired, facilityDto.getTimeZone(), ChronoUnit.MINUTES, offsetValue);
      case HOURS ->
        dateTimeValue = calculateAdjustedTime(timeRequired, facilityDto.getTimeZone(), ChronoUnit.HOURS, offsetValue);
      case DAYS ->
        dateTimeValue = calculateAdjustedTime(timeRequired, facilityDto.getTimeZone(), ChronoUnit.DAYS, offsetValue);
      case MONTHS ->
        dateTimeValue = calculateAdjustedTime(timeRequired, facilityDto.getTimeZone(), ChronoUnit.MONTHS, offsetValue);
      case YEARS ->
        dateTimeValue = calculateAdjustedTime(timeRequired, facilityDto.getTimeZone(), ChronoUnit.YEARS, offsetValue);
      default -> ValidationUtils.invalidate(automation.getId(), ErrorCode.RESOURCE_PARAMETER_AUTOMATION_ACTION_ERROR);
    }
    return dateTimeValue;
  }

  private Double calculateOffsetValue(AutomationActionDateTimeDto automationActionDateTimeDto, long jobId, long taskId, PrincipalUser systemPrincipalUser, String displayName, Parameter referencedParameter, Automation automation) throws StreemException {
    Double offsetValue = null;
    switch (automationActionDateTimeDto.getOffsetSelector()) {
      case CONSTANT:
        offsetValue = Double.parseDouble(automationActionDateTimeDto.getOffsetValue().toString());
        break;
      case PARAMETER:
        Parameter parameter = parameterRepository.findById(Long.valueOf(automationActionDateTimeDto.getOffsetParameterId())).get();
        ParameterValue parameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, Long.valueOf(automationActionDateTimeDto.getOffsetParameterId()));
        if (skipAutomationExecution(parameterValue, parameter)) {
          jobAuditService.skipSetPropertyAutomation(taskId, jobId, automationActionDateTimeDto, null, referencedParameter, systemPrincipalUser, displayName);
          return null;
        } else if (isParameterNotExecuted(parameterValue,referencedParameter) ) {
          ValidationUtils.invalidate(automation.getId(), ErrorCode.AUTOMATION_WILL_NOT_RUN_DUE_TO_MISSING_RESOURCES);
        }
        if (Utility.isEmpty(parameterValue) || Utility.isEmpty(parameterValue.getValue())) {
          ValidationUtils.invalidate(automation.getId(), ErrorCode.PROCESS_RELATION_AUTOMATION_ACTION_ERROR);
        }
        offsetValue = Double.parseDouble(parameterValue.getValue());
        break;
    }
    return offsetValue;
  }

  private PropertyValue getAndValidatePropertyValue(Automation automation, Map<String, PropertyValue> propertyValueMap, AutomationActionForResourceParameterDto resourceParameterAction, ResourceParameter resourceParameter) throws StreemException {
    log.info("[getAndValidatePropertyValue] Request to get and validate property value: {}, propertyValueMap: {}, resourceParameterAction: {}, resourceParameter: {}", automation, propertyValueMap, resourceParameterAction, resourceParameter);
    PropertyValue propertyValue = propertyValueMap.get(resourceParameterAction.getPropertyId());
    if (Utility.isNull(propertyValue) || Utility.isNull(propertyValue.getValue())) {
      ValidationUtils.invalidate(automation.getId(), ErrorCode.AUTOMATION_CANNOT_BE_PERFORMED_ON_EMPTY_PROPERTY);
    }
    Optional<Property> optionalProperty = objectTypeRepository.findPropertyByIdAndObjectTypeExternalId(resourceParameter.getObjectTypeExternalId(), propertyValue.getId());
    if (!Utility.isEmpty(optionalProperty)) {
      Property property = optionalProperty.get();
      if (property.getUsageStatus() == CollectionMisc.UsageStatus.DEPRECATED.get()) {
        ValidationUtils.invalidate(automation.getId(), ErrorCode.CANNOT_PERFORM_AUTOMATION_ACTION_ON_ARCHIVED_PROPERTY);
      }
    }
    return propertyValue;
  }


  private static void getSelectedRelations(EntityObject entityObject, Map<String, List<PartialEntityObject>> partialEntityObjectMap) {
    log.info("[getSelectedRelations] request to get selected relations for entityObject: {}, partialEntityObjectMap: {}", entityObject, partialEntityObjectMap);
    for (MappedRelation mappedRelation : entityObject.getRelations()) {
      List<PartialEntityObject> partialEntityObjects = new ArrayList<>();
      for (MappedRelationTarget mappedRelationTarget : mappedRelation.getTargets()) {
        PartialEntityObject partialEntityObject = new PartialEntityObject();
        partialEntityObject.setCollection(mappedRelationTarget.getCollection());
        partialEntityObject.setId(mappedRelationTarget.getId());
        partialEntityObject.setExternalId(mappedRelationTarget.getExternalId());
        partialEntityObject.setDisplayName(mappedRelationTarget.getDisplayName());
        partialEntityObjects.add(partialEntityObject);
      }
      partialEntityObjectMap.put(mappedRelation.getId().toString(), partialEntityObjects);
    }
  }

  private static void getPartialEntityObjectMap(Map<String, List<PartialEntityObject>> partialEntityObjectMap, ResourceParameterChoiceDto parameterChoiceDto, ObjectId relationId) {
    log.info("[getPartialEntityObjectMap] request to get partial entity object with partialEntityObjectMap: {}, parameterChoiceDto: {}, relationId: {}", partialEntityObjectMap, parameterChoiceDto, relationId);
    //TODO: convert to mapper
    PartialEntityObject partialEntityObject = new PartialEntityObject();
    partialEntityObject.setId(new ObjectId(parameterChoiceDto.getObjectId()));
    partialEntityObject.setDisplayName(parameterChoiceDto.getObjectDisplayName());
    partialEntityObject.setCollection(parameterChoiceDto.getCollection());
    partialEntityObject.setExternalId(parameterChoiceDto.getObjectExternalId());

    List<PartialEntityObject> partialEntityObjects = partialEntityObjectMap.get(relationId.toString());
    if (partialEntityObjects == null) {
      partialEntityObjects = new ArrayList<>();
    }
    partialEntityObjects.add(partialEntityObject);
    partialEntityObjectMap.put(relationId.toString(), partialEntityObjects);
  }

  private static Set<String> getAllPropertyIdsOfObjectType(ObjectType objectType) {
    log.info("[getAllPropertyIdsOfObjectType] objectType: {}", objectType);
    return objectType.getProperties().stream()
      .filter(property -> property.getUsageStatus() == CollectionMisc.UsageStatus.ACTIVE.get())
      .map(property -> property.getId().toString()).collect(Collectors.toSet());
  }

  private EntityObject getEntityObject(String externalId, String objectId) throws ResourceNotFoundException {
    log.info("[getEntityObject] externalId: {}, objectId: {}", externalId, objectId);
    return entityObjectRepository.findById(externalId, objectId)
      .orElseThrow(() -> new ResourceNotFoundException(objectId, ErrorCode.ENTITY_OBJECT_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
  }

  public boolean skipAutomationExecution(ParameterValue parameterValue, Parameter parameter) {
    log.info("[skipAutomationExecution] parameterValue: {}, parameter: {}", parameterValue, parameter);
    return parameterValue.isHidden()
      || (!parameter.isMandatory() && (parameterValue.getState() == State.ParameterExecution.NOT_STARTED
      || parameterValue.getState() == State.ParameterExecution.BEING_EXECUTED));
  }
  public boolean isParameterNotExecuted(ParameterValue parameterValue, Parameter parameter) {
    log.info("[isParameterNotExecuted] parameterValue: {}, parameter: {}", parameterValue, parameter);
    return (!parameterValue.isHidden() && parameter.isMandatory() &&  parameterValue.getState() != State.ParameterExecution.EXECUTED);
  }

  private static String formatResourceParameterChoices(List<ResourceParameterChoiceDto> resourceParameterChoiceDtos) {
    log.info("[formatResourceParameterChoices] resourceParameterChoiceDtos: {} ", resourceParameterChoiceDtos);
    StringBuilder result = new StringBuilder();
    for (ResourceParameterChoiceDto resourceParameterChoiceDto : resourceParameterChoiceDtos) {
      result.append(resourceParameterChoiceDto.getObjectDisplayName())
        .append(" (ID:")
        .append(resourceParameterChoiceDto.getObjectExternalId())
        .append("), ");
    }
    if (!result.isEmpty()) {
      result.setLength(result.length() - 2);
    }
    return result.toString();
  }

}
