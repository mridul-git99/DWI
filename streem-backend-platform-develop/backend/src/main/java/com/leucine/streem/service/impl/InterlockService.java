package com.leucine.streem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.collections.EntityObject;
import com.leucine.streem.collections.PropertyOption;
import com.leucine.streem.collections.PropertyValue;
import com.leucine.streem.constant.*;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.ResourceParameterChoiceDto;
import com.leucine.streem.dto.mapper.IInterlockMapper;
import com.leucine.streem.dto.request.InterlockDto;
import com.leucine.streem.dto.request.InterlockRequest;
import com.leucine.streem.dto.request.InterlockResourcePropertyValidationDto;
import com.leucine.streem.dto.request.InterlockValidationDto;
import com.leucine.streem.dto.response.Error;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.ExceptionType;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.model.helper.parameter.ResourceParameter;
import com.leucine.streem.repository.*;
import com.leucine.streem.service.IInterlockService;
import com.leucine.streem.util.DateTimeUtils;
import com.leucine.streem.util.JsonUtils;
import com.leucine.streem.util.Utility;
import com.leucine.streem.util.ValidationUtils;
import com.leucine.streem.validator.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InterlockService implements IInterlockService {

  private final IEntityObjectRepository entityObjectRepository;
  private final IInterlockRepository interlockRepository;
  private final IInterlockMapper interlockMapper;
  private final IParameterValueRepository parameterValueRepository;
  private final IParameterRepository parameterRepository;
  private final IFacilityRepository facilityRepository;
  private final ITaskRepository taskRepository;

  public InterlockService(IEntityObjectRepository entityObjectRepository, IInterlockRepository interlockRepository, IInterlockMapper interlockMapper, IParameterValueRepository parameterValueRepository, IParameterRepository parameterRepository, IFacilityRepository facilityRepository, ITaskRepository taskRepository) {
    this.entityObjectRepository = entityObjectRepository;
    this.interlockRepository = interlockRepository;
    this.interlockMapper = interlockMapper;

    this.parameterValueRepository = parameterValueRepository;
    this.parameterRepository = parameterRepository;
    this.facilityRepository = facilityRepository;
    this.taskRepository = taskRepository;
  }

  @Override
  public InterlockDto addInterlockForTask(Task task, InterlockRequest interlockRequest, User principalUserEntity) throws StreemException, ResourceNotFoundException, JsonProcessingException {
    log.info("[addInterlockForTask] Request to save add Interlock, interlockRequest: {}", interlockRequest);


    validateInterlockInput(task, interlockRequest);

    Interlock interlock = interlockRepository.findFirstByTargetEntityTypeAndTargetEntityId(Type.InterlockTargetEntityType.TASK, task.getId())
      .orElseGet(Interlock::new);
    interlock.setTargetEntityId(task.getId());
    interlock.setTargetEntityType(Type.InterlockTargetEntityType.TASK);
    interlock.setValidations(interlockRequest.getValidations());
    interlock.setCreatedBy(principalUserEntity);
    interlock.setModifiedBy(principalUserEntity);
    Interlock savedInterlock = interlockRepository.save(interlock);
    taskRepository.updateHasInterlocks(interlock.getTargetEntityId(), true);
    return interlockMapper.toDto(savedInterlock);
  }


  @Override
  public InterlockDto getAndCreateInterlockByTaskId(Task task, User principalUserEntity) throws StreemException, ResourceNotFoundException, JsonProcessingException {
    log.info("[getAndCreateInterlockByTaskId] Request to get Interlock for taskId: {}", task.getId());

    //TODO fix this we are saving if we dont have interlock for task which is weird
    Interlock interlock = interlockRepository.findFirstByTargetEntityTypeAndTargetEntityId(Type.InterlockTargetEntityType.TASK, task.getId())
      .orElseGet(Interlock::new);
    interlock.setTargetEntityId(task.getId());
    interlock.setTargetEntityType(Type.InterlockTargetEntityType.TASK);
    interlock.setCreatedBy(principalUserEntity);
    interlock.setModifiedBy(principalUserEntity);
    Interlock savedInterlock = interlockRepository.save(interlock);
    return interlockMapper.toDto(savedInterlock);
  }

  @Override
  public InterlockDto getInterlockByTaskId(Task task, User principalUserEntity) throws StreemException, ResourceNotFoundException, JsonProcessingException {
    Optional<Interlock> optionalInterlock = interlockRepository.findFirstByTargetEntityTypeAndTargetEntityId(Type.InterlockTargetEntityType.TASK, task.getId());
    if (optionalInterlock.isPresent()) {
      Interlock interlock = optionalInterlock.get();
      interlock.setTargetEntityId(task.getId());
      interlock.setTargetEntityType(Type.InterlockTargetEntityType.TASK);
      interlock.setCreatedBy(principalUserEntity);
      interlock.setModifiedBy(principalUserEntity);

      InterlockDto interlockDto = new InterlockDto();
      interlockDto.setId(String.valueOf(interlock.getId()));
      interlockDto.setValidations(interlock.getValidations());
      return interlockDto;
    }
    return new InterlockDto();
  }

  @Override
  public InterlockDto updateInterlockForTask(Task task, InterlockRequest interlockRequest, User principalUserEntity) throws StreemException, ResourceNotFoundException, JsonProcessingException {
    log.info("[updateInterlockForTask] Request to update Interlock, interlockRequest: {}", interlockRequest);

    validateInterlockInput(task, interlockRequest);
    Interlock interlock = interlockRepository.findFirstByTargetEntityTypeAndTargetEntityId(Type.InterlockTargetEntityType.TASK, task.getId())
      .orElseThrow(() -> new ResourceNotFoundException(task.getIdAsString(), ErrorCode.INTERLOCK_NOT_FOUND_FOR_TASK, ExceptionType.ENTITY_NOT_FOUND));
    interlock.setValidations(interlockRequest.getValidations());
    interlock.setModifiedBy(principalUserEntity);
    Interlock savedInterlock = interlockRepository.save(interlock);
    return interlockMapper.toDto(savedInterlock);
  }

  @Override
  public BasicDto deleteInterlockByTaskId(String interlockId, Long taskId, User principalUserEntity) throws StreemException, ResourceNotFoundException, JsonProcessingException {
    log.info("[deleteInterlockByTaskId] Request to delete Interlock for taskId: {}", taskId);

    Interlock interlock = interlockRepository.findFirstByTargetEntityTypeAndTargetEntityId(Type.InterlockTargetEntityType.TASK, taskId)
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.INTERLOCK_NOT_FOUND_FOR_TASK, ExceptionType.ENTITY_NOT_FOUND));

    InterlockValidationDto interlockValidationDto = JsonUtils.readValue(interlock.getValidations().toString(), InterlockValidationDto.class);
    interlockValidationDto.getResourceParameterValidations().removeIf(
      interlockResourcePropertyValidationDto ->
        Objects.equals(interlockResourcePropertyValidationDto.getId(), interlockId)
    );
    Interlock updatedInterlock = interlock.setValidations(JsonUtils.valueToNode(interlockValidationDto));
    updatedInterlock.setModifiedBy(principalUserEntity);
    interlockRepository.save(updatedInterlock);
    taskRepository.deleteHasInterlocks(interlock.getId());
    taskRepository.removeHasInterlocks(interlock.getTargetEntityId());
    BasicDto basicDto = new BasicDto();
    basicDto.setId(String.valueOf(taskId))
      .setMessage("success");
    return basicDto;
  }

  @Override
  public void validateInterlockForTaskExecution(Long taskId, String jobIdAsString, Type.InterlockTriggerType triggerType) throws StreemException, ResourceNotFoundException, IOException {

    log.info("[validateInterlockForTask] Request to validate Interlock for taskId: {}", taskId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Facility facility = facilityRepository.findById(principalUser.getCurrentFacilityId())
      .orElseThrow(() -> new ResourceNotFoundException(principalUser.getCurrentFacilityId(), ErrorCode.FACILITY_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    String facilityTimeZone = facility.getTimeZone();
    Optional<Interlock> interlockOptional = interlockRepository.findFirstByTargetEntityTypeAndTargetEntityId(Type.InterlockTargetEntityType.TASK, taskId);
    Interlock interlock;
    if (interlockOptional.isEmpty()) {
      return;
    } else {
      interlock = interlockOptional.get();
    }
    Long jobId = Long.valueOf(jobIdAsString);


    if (!Utility.isEmpty(interlock.getValidations())) {
      InterlockValidationDto interlockValidationDto = JsonUtils.readValue(interlock.getValidations().toString(), InterlockValidationDto.class);
      List<Error> errors = new ArrayList<>();

      if (!Utility.isEmpty(interlockValidationDto.getResourceParameterValidations())) {
        for (InterlockResourcePropertyValidationDto validation : interlockValidationDto.getResourceParameterValidations()) {
          if (validation.getTriggerType() == triggerType) {
            Long parameterId = Long.valueOf(validation.getParameterId());

            ParameterValue parameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, parameterId);
            Parameter parameterForValidation = parameterRepository.findById(parameterId).get();
            if (null != parameterValue && (parameterForValidation.getType() == Type.Parameter.RESOURCE || parameterForValidation.getType() == Type.Parameter.MULTI_RESOURCE)) {
              if (parameterValue.getParameter().isMandatory() && parameterValue.getState() != State.ParameterExecution.EXECUTED && !parameterValue.isHidden()) {
                ValidationUtils.invalidate(parameterValue.getId(), ErrorCode.INTERLOCK_CONDITION_CANNOT_BE_VALIDATED_DUE_TO_MISSING_RESOURCES);
              }
              ResourceParameter resourceParameter = JsonUtils.readValue(parameterForValidation.getData().toString(), ResourceParameter.class);
              List<ResourceParameterChoiceDto> parameterChoices = JsonUtils.jsonToCollectionType(parameterValue.getChoices(), List.class, ResourceParameterChoiceDto.class);
              if((!parameterValue.getParameter().isMandatory() && (Objects.isNull(parameterChoices) && Utility.isEmpty(parameterChoices))) || parameterValue.isHidden()) {
                continue;
              }
              if (!Objects.isNull(parameterChoices) && !Utility.isEmpty(parameterChoices)) {
                for (ResourceParameterChoiceDto resourceParameterChoice : parameterChoices) {
                  String value = validation.getValue();
                  EntityObject entityObject = entityObjectRepository.findById(resourceParameter.getObjectTypeExternalId(), resourceParameterChoice.getObjectId())
                    .orElseThrow(() -> new ResourceNotFoundException(resourceParameterChoice.getObjectId(), ErrorCode.ENTITY_OBJECT_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
                  boolean isObjectArchived = entityObject.getUsageStatus() == UsageStatus.DEPRECATED.getCode();
                  if (isObjectArchived) {
                    throw new ResourceNotFoundException(resourceParameterChoice.getObjectId(), ErrorCode.CANNOT_PERFORM_INTERLOCK_VALIDATION_ON_ARCHIVED_OBJECT, ExceptionType.BAD_REQUEST);
                  }
                  Map<String, PropertyValue> propertyValueMap = entityObject.getProperties().stream().collect(Collectors.toMap(pv -> pv.getId().toString()
                    , Function.identity()));
                  PropertyValue propertyValue = propertyValueMap.get(validation.getPropertyId());

                  ConstraintValidator validator = null;
                  if (Utility.isEmpty(propertyValue)) {
                    ValidationUtils.invalidate(validation.getId(), ErrorCode.PARAMETER_RELATION_PROPERTY_VALIDATION_COULD_NOT_RUN_MISSING_DATA, ErrorCode.PARAMETER_RELATION_PROPERTY_VALIDATION_COULD_NOT_RUN_MISSING_DATA.getDescription());
                  }

                  String propertyInput = propertyValue.getValue();

                  switch (validation.getPropertyInputType()) {
                    case DATE -> {
                      if (Utility.isEmpty(propertyInput)) {
                        ValidationUtils.addError(validation.getId(), errors, ErrorCode.INTERLOCK_SELECTED_RESOURCE_PROPERTY_VALUE_IS_NOT_UPDATED, ErrorCode.INTERLOCK_SELECTED_RESOURCE_PROPERTY_VALUE_IS_NOT_UPDATED.getDescription());
                      } else {
                        validator = new DateValidator(DateTimeUtils.atStartOfDay(DateTimeUtils.now(), facilityTimeZone), Long.valueOf(value), validation.getErrorMessage(), validation.getDateUnit(), validation.getConstraint(), facility.getTimeZone());
                        validator.validate(Long.valueOf(propertyInput));

                        if (!validator.isValid()) {
                          ValidationUtils.addError(validation.getId(), errors, ErrorCode.DATE_INTERLOCK_VALIDATION_ERROR, validator.getErrorMessage());
                        }
                      }
                    }
                    case DATE_TIME -> {
                      if (Utility.isEmpty(propertyInput)) {
                        ValidationUtils.addError(validation.getId(), errors, ErrorCode.INTERLOCK_SELECTED_RESOURCE_PROPERTY_VALUE_IS_NOT_UPDATED, ErrorCode.INTERLOCK_SELECTED_RESOURCE_PROPERTY_VALUE_IS_NOT_UPDATED.getDescription());
                      } else {
                        validator = new DateTimeValidator(DateTimeUtils.now(), Long.valueOf(value), validation.getErrorMessage(), validation.getDateUnit(), validation.getConstraint(),facility.getTimeZone());
                        validator.validate(propertyInput);

                        if (!validator.isValid()) {
                          ValidationUtils.addError(validation.getId(), errors, ErrorCode.DATE_TIME_INTERLOCK_VALIDATION_ERROR, validator.getErrorMessage());
                        }
                      }
                    }
                    case NUMBER -> {
                      if (Utility.isEmpty(propertyInput)) {
                        ValidationUtils.addError(validation.getId(), errors, ErrorCode.INTERLOCK_SELECTED_RESOURCE_PROPERTY_VALUE_IS_NOT_UPDATED, ErrorCode.INTERLOCK_SELECTED_RESOURCE_PROPERTY_VALUE_IS_NOT_UPDATED.getDescription());
                      } else {
                        switch (validation.getConstraint()) {
                          case EQ -> {
                            validator = new EqualValueValidator(Double.parseDouble(value), validation.getErrorMessage());
                            validator.validate(propertyInput);
                          }
                          case LT -> {
                            validator = new LessThanValidator(Double.parseDouble(value), validation.getErrorMessage());
                            validator.validate(propertyInput);
                          }
                          case GT -> {
                            validator = new GreaterThanValidator(Double.parseDouble(value), validation.getErrorMessage());
                            validator.validate(propertyInput);
                          }
                          case LTE -> {
                            validator = new LessThanOrEqualValidator(Double.parseDouble(value), validation.getErrorMessage());
                            validator.validate(propertyInput);
                          }
                          case GTE -> {
                            validator = new GreaterThanOrEqualValidator(Double.parseDouble(value), validation.getErrorMessage());
                            validator.validate(propertyInput);
                          }
                          case NE -> {
                            validator = new NotEqualValueValidator(Double.parseDouble(value), validation.getErrorMessage());
                            validator.validate(propertyInput);
                          }
                        }
                        if (null != validator && !validator.isValid()) {
                          ValidationUtils.addError(validation.getId(), errors, ErrorCode.NUMBER_PARAMETER_INTERLOCK_VALIDATION_ERROR, validator.getErrorMessage());
                        }
                      }
                    }
                    case SINGLE_SELECT -> {
                      List<PropertyOption> choices = propertyValue.getChoices();
                      if (!Utility.isEmpty(choices) && !Utility.isEmpty(validation.getChoices())) {
                        String propertyChoice = choices.get(0).getId().toString();
                        String choice = validation.getChoices().get(0).getId().toString();
                        switch (validation.getConstraint()) {
                          case EQ -> {
                            validator = new StringEqualValidator(propertyChoice, validation.getErrorMessage());
                            validator.validate(choice);
                          }
                          case NE -> {
                            validator = new StringNotEqualValidator(propertyChoice, validation.getErrorMessage());
                            validator.validate(choice);
                          }
                        }
                        if (null != validator && !validator.isValid()) {
                          ValidationUtils.addError(validation.getId(), errors, ErrorCode.SINGLE_SELECT_INTERLOCK_VALIDATION_ERROR, validator.getErrorMessage());
                        }
                      } else {
                        ValidationUtils.addError(validation.getId(), errors, ErrorCode.INTERLOCK_SELECTED_RESOURCE_PROPERTY_VALUE_IS_NOT_UPDATED, ErrorCode.INTERLOCK_SELECTED_RESOURCE_PROPERTY_VALUE_IS_NOT_UPDATED.getDescription());
                      }
                    }
                  }
                }
              } else {
                ValidationUtils.invalidate(validation.getId(), ErrorCode.INTERLOCK_CONDITION_CANNOT_BE_VALIDATED_DUE_TO_MISSING_RESOURCES);
              }
            }
          }

        }
      }
      if (!errors.isEmpty()) {
        ValidationUtils.invalidate(ErrorMessage.ERROR_DURING_VALIDATION, errors);
      }
    }
  }


  private void validateInterlockInput(Task task, InterlockRequest interlockRequest) throws JsonProcessingException, ResourceNotFoundException, StreemException {
    InterlockValidationDto interlockValidation = JsonUtils.readValue(interlockRequest.getValidations().toString(), InterlockValidationDto.class);
    if (!Utility.isEmpty(interlockValidation.getResourceParameterValidations())) {
      for (InterlockResourcePropertyValidationDto validation : interlockValidation.getResourceParameterValidations()) {
        boolean constraintExists = Arrays.stream(CollectionMisc.PropertyValidationConstraint.values()).anyMatch(e -> e.name().equals(validation.getConstraint().toString()));
        boolean propertyTypeExists = Arrays.stream(CollectionMisc.PropertyType.values()).anyMatch(e -> e.name().equals(validation.getPropertyInputType().toString()));
        boolean triggerTypeExists = Arrays.stream(Type.InterlockTriggerType.values()).anyMatch(e -> e.name().equals(validation.getTriggerType().toString()));

        if (task == null || validation.getParameterId() == null || (validation.getValue() == null && Utility.isEmpty(validation.getChoices())) || validation.getPropertyId() == null
            || validation.getPropertyExternalId() == null || validation.getPropertyDisplayName() == null || validation.getErrorMessage() == null
            || !propertyTypeExists || !constraintExists || !triggerTypeExists) {
          ValidationUtils.invalidate(ErrorCode.INTERLOCK_INPUT_CANNOT_BE_EMPTY.getDescription(), ErrorCode.INTERLOCK_INPUT_CANNOT_BE_EMPTY);
        }

        if (validation.getTriggerType().equals(Type.InterlockTriggerType.TASK_STARTED)) {
          checkIfAllReferencedParametersInInterlockBelongsToSameTask(validation, task);
        }

      }
    }
  }

  private void checkIfAllReferencedParametersInInterlockBelongsToSameTask(InterlockResourcePropertyValidationDto validation, Task task) throws JsonProcessingException, StreemException {
    {
      if (!Utility.isEmpty(validation.getParameterId())) {
        boolean resourceParameterExist = task.getParameters().stream().anyMatch(parameter -> String.valueOf(parameter.getId()).equals(validation.getParameterId()));
        if (resourceParameterExist) {
          ValidationUtils.invalidate(validation.getParameterId(), ErrorCode.START_TASK_INTERLOCK_NOT_ALLOWED_FOR_SAME_TASK_PARAMETER);
        }
      }
    }
  }

  @Override
  public void validateTaskInterlocks(Task task, List<Error> errorList) throws JsonProcessingException {
    Optional<Interlock> optionalInterlock = interlockRepository.findFirstByTargetEntityTypeAndTargetEntityId(Type.InterlockTargetEntityType.TASK, task.getId());
    if (optionalInterlock.isPresent()) {
      Interlock interlock = optionalInterlock.get();
      InterlockRequest interlockRequest = new InterlockRequest();
      interlockRequest.setId(String.valueOf(interlock.getId()));
      interlockRequest.setValidations(interlock.getValidations());

      InterlockValidationDto interlockValidation = JsonUtils.readValue(interlockRequest.getValidations().toString(), InterlockValidationDto.class);
      if (!Utility.isEmpty(interlockValidation.getResourceParameterValidations())) {
        for (InterlockResourcePropertyValidationDto validation : interlockValidation.getResourceParameterValidations()) {
          if (!Utility.isEmpty(validation.getParameterId()) && validation.getTriggerType().equals(Type.InterlockTriggerType.TASK_STARTED)) {
            boolean resourceParameterExist = task.getParameters().stream().anyMatch(parameter -> String.valueOf(parameter.getId()).equals(validation.getParameterId()));
            if (resourceParameterExist) {
              ValidationUtils.addError(task.getId(), errorList, ErrorCode.START_TASK_INTERLOCK_NOT_ALLOWED_FOR_SAME_TASK_PARAMETER);
              break;
            }
          }
        }
      }
    }
  }
}
