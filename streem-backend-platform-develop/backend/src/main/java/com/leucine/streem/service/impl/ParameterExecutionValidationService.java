package com.leucine.streem.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.collections.ObjectType;
import com.leucine.streem.collections.Property;
import com.leucine.streem.collections.*;
import com.leucine.streem.constant.*;
import com.leucine.streem.constant.Action;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.mapper.IParameterMapper;
import com.leucine.streem.dto.response.Error;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.ExceptionType;
import com.leucine.streem.exception.ParameterExecutionException;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.model.helper.parameter.*;
import com.leucine.streem.repository.*;
import com.leucine.streem.service.IParameterExecutionValidationService;
import com.leucine.streem.util.DateTimeUtils;
import com.leucine.streem.util.JsonUtils;
import com.leucine.streem.util.Utility;
import com.leucine.streem.util.ValidationUtils;
import com.leucine.streem.validator.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.leucine.streem.constant.CollectionMisc.PropertyType.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParameterExecutionValidationService implements IParameterExecutionValidationService {
  private final IRelationValueRepository relationValueRepository;
  private final IEntityObjectRepository entityObjectRepository;
  private final IParameterRepository parameterRepository;
  private final IParameterValueRepository parameterValueRepository;
  private final IVariationRepository variationRepository;
  private final IFacilityRepository facilityRepository;
  private final IParameterMapper parameterMapper;
  private final IObjectTypeRepository objectTypeRepository;
  private final IJobRepository jobRepository;

  @Override
  public void validateNumberParameterValidations(Long jobId, Long parameterValueId, Long parameterId, JsonNode validations, String inputValue) throws StreemException, ResourceNotFoundException, IOException, ParameterExecutionException {

    if (!Utility.isEmpty(validations)) {
      ParameterValue parameterValue = parameterValueRepository.getReferenceById(parameterValueId);
      Parameter parameter = parameterRepository.getReferenceById(parameterId);
      List<Error> errorList = new ArrayList<>();


      List<ParameterValidationDto> parameterValidationDtoList = JsonUtils.jsonToCollectionType(validations, List.class, ParameterValidationDto.class);
      for (ParameterValidationDto parameterValidationDto : parameterValidationDtoList) {
        Map<String, ResourceParameterPropertyValidationDto> resourceParameterPropertyValidationDtoMap = new HashMap<>();
        if (!Utility.isEmpty(parameterValidationDto.getResourceParameterValidations())) {
          resourceParameterPropertyValidationDtoMap = parameterValidationDto.getResourceParameterValidations()
            .stream().collect(Collectors.toMap(ResourceParameterPropertyValidationDto::getId, Function.identity()));
        }
        if (parameterValue.isHasVariations()) {
          List<Variation> variations = variationRepository.findAllByParameterValueIdAndType(parameterValue.getId(), Action.Variation.VALIDATION);

          for (Variation variation : variations) {
            ResourceParameterPropertyValidationDto resourceParameterPropertyValidationDto = JsonUtils.readValue(variation.getNewDetails().toString(), ResourceParameterPropertyValidationDto.class);
            if (!Utility.isEmpty(resourceParameterPropertyValidationDtoMap.get(resourceParameterPropertyValidationDto.getId()))) {
              resourceParameterPropertyValidationDtoMap.put(resourceParameterPropertyValidationDto.getId(), resourceParameterPropertyValidationDto);
            }
          }
          parameterValidationDto.setResourceParameterValidations(new ArrayList<>(resourceParameterPropertyValidationDtoMap.values()));
        }
        // TODO Refactor
        if (!Utility.isEmpty(parameterValidationDto.getRelationPropertyValidations())) {
          for (ParameterRelationPropertyValidationDto validation : parameterValidationDto.getRelationPropertyValidations()) {
            ParameterDetailsDto parameterDetailsDto = new ParameterDetailsDto();
            parameterDetailsDto.setParameterExecutionId(parameterValueId.toString());
            parameterDetailsDto.setParameterId(parameterId.toString());
            parameterDetailsDto.setType(parameter.getType());
            parameterDetailsDto.setTargetEntityType(parameter.getTargetEntityType());

            parameterDetailsDto.setExceptionApprovalType(parameterValidationDto.getExceptionApprovalType());

            RelationValue relationValue = relationValueRepository.findByRelationIdAndJobId(Long.parseLong(validation.getRelationId()), jobId);
            // TODO relation is optional how will you tackle ?
            if (!Utility.isNull(relationValue)) {
              EntityObject entityObject = entityObjectRepository.findById(relationValue.getCollection(), relationValue.getObjectId())
                .orElseThrow(() -> new ResourceNotFoundException(relationValue.getObjectId(), ErrorCode.ENTITY_OBJECT_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
              Map<String, PropertyValue> propertyValueMap = entityObject.getProperties().stream().collect(Collectors.toMap(pv -> pv.getId().toString(), Function.identity()));

              PropertyValue propertyValue = propertyValueMap.get(validation.getPropertyId());

              ConstraintValidator validator = null;
              if (Utility.isEmpty(propertyValue)) {
                ValidationUtils.invalidate(validation.getId(), ErrorCode.PARAMETER_RELATION_PROPERTY_VALIDATION_COULD_NOT_RUN_MISSING_DATA, validation.getErrorMessage());
              }
              String propertyInput = propertyValue.getValue();
              // TODO For now this is always number hence this is written like this
              if (Utility.isEmpty(propertyInput)) {
                ValidationUtils.invalidate(validation.getId(), ErrorCode.PARAMETER_RELATION_PROPERTY_VALIDATION_COULD_NOT_RUN_MISSING_DATA, validation.getErrorMessage());
              }
              switch (validation.getConstraint()) {
                case EQ -> {
                  validator = new EqualValueValidator(Double.parseDouble(propertyInput), validation.getErrorMessage());
                  validator.validate(inputValue);
                }
                case LT -> {
                  validator = new LessThanValidator(Double.parseDouble(propertyInput), validation.getErrorMessage());
                  validator.validate(inputValue);
                }
                case GT -> {
                  validator = new GreaterThanValidator(Double.parseDouble(propertyInput), validation.getErrorMessage());
                  validator.validate(inputValue);
                }
                case LTE -> {
                  validator = new LessThanOrEqualValidator(Double.parseDouble(propertyInput), validation.getErrorMessage());
                  validator.validate(inputValue);
                }
                case GTE -> {
                  validator = new GreaterThanOrEqualValidator(Double.parseDouble(propertyInput), validation.getErrorMessage());
                  validator.validate(inputValue);
                }
                case NE -> {
                  validator = new NotEqualValueValidator(Double.parseDouble(propertyInput), validation.getErrorMessage());
                  validator.validate(inputValue);
                }
              }
              if (null != validator && !validator.isValid()) {
                parameterDetailsDto.setRuleId(parameterValidationDto.getRuleId());
                parameterDetailsDto.setRuleDetails(JsonUtils.valueToNode(parameterValidationDto.getRelationPropertyValidations()));
                ValidationUtils.addError(parameterId.toString(), errorList, ErrorCode.NUMBER_PARAMETER_RELATION_PROPERTY_VALIDATION_ERROR, validator.getErrorMessage(), parameterDetailsDto);
              }
            }
          }
        }

        if (!Utility.isEmpty(parameterValidationDto.getResourceParameterValidations())) {
          for (ResourceParameterPropertyValidationDto validation : parameterValidationDto.getResourceParameterValidations()) {
            ParameterDetailsDto parameterDetailsDto = new ParameterDetailsDto();
            parameterDetailsDto.setParameterExecutionId(parameterValueId.toString());
            parameterDetailsDto.setParameterId(parameterId.toString());
            parameterDetailsDto.setType(parameter.getType());
            parameterDetailsDto.setTargetEntityType(parameter.getTargetEntityType());

            parameterDetailsDto.setExceptionApprovalType(parameterValidationDto.getExceptionApprovalType());

            Long parameterForValidationId = Long.valueOf(validation.getParameterId());
            parameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, parameterForValidationId);
            Parameter parameterForValidation = parameterRepository.findById(parameterForValidationId).get();
            if (null != parameterValue) {
              if (!parameterValue.getState().equals(State.ParameterExecution.EXECUTED)) {
                ValidationUtils.invalidate(validation.getId(), ErrorCode.NUMBER_PARAMETER_RESOURCE_VALIDATION_ERROR);
              }
              ResourceParameter resourceParameter = JsonUtils.readValue(parameterForValidation.getData().toString(), ResourceParameter.class);
              List<ResourceParameterChoiceDto> parameterChoices = JsonUtils.jsonToCollectionType(parameterValue.getChoices(), List.class, ResourceParameterChoiceDto.class);
              if (!Utility.isEmpty(parameterChoices)) {
                for (ResourceParameterChoiceDto resourceParameterChoice : parameterChoices) {
                  EntityObject entityObject = entityObjectRepository.findById(resourceParameter.getObjectTypeExternalId(), resourceParameterChoice.getObjectId())
                    .orElseThrow(() -> new ResourceNotFoundException(resourceParameterChoice.getObjectId(), ErrorCode.ENTITY_OBJECT_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
                  Map<String, PropertyValue> propertyValueMap = entityObject.getProperties().stream().collect(Collectors.toMap(pv -> pv.getId().toString()
                    , Function.identity()));
                  PropertyValue propertyValue = propertyValueMap.get(validation.getPropertyId());

                  ConstraintValidator validator = null;
                  if (Utility.isEmpty(propertyValue)) {
                    ValidationUtils.invalidate(validation.getId(), ErrorCode.PARAMETER_RELATION_PROPERTY_VALIDATION_COULD_NOT_RUN_MISSING_DATA, validation.getErrorMessage());
                  }

                  String propertyInput = propertyValue.getValue();
                  if (Utility.isEmpty(propertyInput)) {
                    ValidationUtils.invalidate(validation.getId(), ErrorCode.PARAMETER_RELATION_PROPERTY_VALIDATION_COULD_NOT_RUN_MISSING_DATA, validation.getErrorMessage());
                  }
                  switch (validation.getConstraint()) {
                    case EQ -> {
                      validator = new EqualValueValidator(Double.parseDouble(propertyInput), validation.getErrorMessage());
                      validator.validate(inputValue);
                    }
                    case LT -> {
                      validator = new LessThanValidator(Double.parseDouble(propertyInput), validation.getErrorMessage());
                      validator.validate(inputValue);
                    }
                    case GT -> {
                      validator = new GreaterThanValidator(Double.parseDouble(propertyInput), validation.getErrorMessage());
                      validator.validate(inputValue);
                    }
                    case LTE -> {
                      validator = new LessThanOrEqualValidator(Double.parseDouble(propertyInput), validation.getErrorMessage());
                      validator.validate(inputValue);
                    }
                    case GTE -> {
                      validator = new GreaterThanOrEqualValidator(Double.parseDouble(propertyInput), validation.getErrorMessage());
                      validator.validate(inputValue);
                    }
                    case NE -> {
                      validator = new NotEqualValueValidator(Double.parseDouble(propertyInput), validation.getErrorMessage());
                      validator.validate(inputValue);
                    }
                  }
                  if (null != validator && !validator.isValid()) {
                    parameterDetailsDto.setRuleId(parameterValidationDto.getRuleId());
                    parameterDetailsDto.setRuleDetails(JsonUtils.valueToNode(parameterValidationDto.getResourceParameterValidations()));
                    ValidationUtils.addError(parameterId.toString(), errorList, ErrorCode.NUMBER_PARAMETER_RELATION_PROPERTY_VALIDATION_ERROR, validator.getErrorMessage(), parameterDetailsDto);
                  }
                }
              }
            }
          }
        }

        if (!Utility.isEmpty(parameterValidationDto.getCustomValidations())) {
          for (CustomRelationPropertyValidationDto validation : parameterValidationDto.getCustomValidations()) {
            ParameterDetailsDto parameterDetailsDto = new ParameterDetailsDto();
            parameterDetailsDto.setParameterExecutionId(parameterValueId.toString());
            parameterDetailsDto.setParameterId(parameterId.toString());
            parameterDetailsDto.setType(parameter.getType());
            parameterDetailsDto.setTargetEntityType(parameter.getTargetEntityType());

            parameterDetailsDto.setExceptionApprovalType(parameterValidationDto.getExceptionApprovalType());

            ConstraintValidator validator = null;
            double propertyInput = Double.parseDouble(validation.getValue());
            switch (validation.getConstraint()) {
              case EQ -> {
                validator = new EqualValueValidator(propertyInput, validation.getErrorMessage());
                validator.validate(inputValue);
              }
              case LT -> {
                validator = new LessThanValidator(propertyInput, validation.getErrorMessage());
                validator.validate(inputValue);
              }
              case GT -> {
                validator = new GreaterThanValidator(propertyInput, validation.getErrorMessage());
                validator.validate(inputValue);
              }
              case LTE -> {
                validator = new LessThanOrEqualValidator(propertyInput, validation.getErrorMessage());
                validator.validate(inputValue);
              }
              case GTE -> {
                validator = new GreaterThanOrEqualValidator(propertyInput, validation.getErrorMessage());
                validator.validate(inputValue);
              }
              case NE -> {
                validator = new NotEqualValueValidator(propertyInput, validation.getErrorMessage());
                validator.validate(inputValue);
              }
            }
            if (null != validator && !validator.isValid()) {
              parameterDetailsDto.setRuleId(parameterValidationDto.getRuleId());
              parameterDetailsDto.setRuleDetails(JsonUtils.valueToNode(parameterValidationDto.getCustomValidations()));
              ValidationUtils.addError(parameterId.toString(), errorList, ErrorCode.NUMBER_PARAMETER_CUSTOM_VALIDATION_ERROR, validator.getErrorMessage(), parameterDetailsDto);
            }
          }
        }

        if (!Utility.isEmpty(parameterValidationDto.getCriteriaValidations())) {

          for (CriteriaValidationDto criteriaValidationDto : parameterValidationDto.getCriteriaValidations()) {
            ParameterDetailsDto parameterDetailsDto = new ParameterDetailsDto();
            parameterDetailsDto.setParameterExecutionId(parameterValueId.toString());
            parameterDetailsDto.setParameterId(parameterId.toString());
            parameterDetailsDto.setType(parameter.getType());
            parameterDetailsDto.setTargetEntityType(parameter.getTargetEntityType());

            parameterDetailsDto.setExceptionApprovalType(parameterValidationDto.getExceptionApprovalType());


            double input = Double.parseDouble(inputValue);
            Operator.Parameter operator = Operator.Parameter.valueOf(criteriaValidationDto.getOperator());

            switch (operator) {
              case BETWEEN:
                double lowerValue = criteriaValidationDto.getCriteriaType().equals(Type.SelectorType.PARAMETER) ?
                  getLatestParameterValue(jobId, criteriaValidationDto.getLowerValueParameterId()) : Double.parseDouble(criteriaValidationDto.getLowerValue());
                double upperValue = criteriaValidationDto.getCriteriaType().equals(Type.SelectorType.PARAMETER) ?
                  getLatestParameterValue(jobId, criteriaValidationDto.getUpperValueParameterId()) : Double.parseDouble(criteriaValidationDto.getUpperValue());
                if (input < lowerValue || input > upperValue) {
                  parameterDetailsDto.setRuleId(parameterValidationDto.getRuleId());
                  parameterDetailsDto.setRuleDetails(JsonUtils.valueToNode(parameterValidationDto.getCriteriaValidations()));
                  ValidationUtils.addError(parameterId.toString(), errorList, ErrorCode.NUMBER_PARAMETER_RELATION_PROPERTY_VALIDATION_ERROR, criteriaValidationDto.getErrorMessage(), parameterDetailsDto);
                }
                break;
              case EQUAL_TO:
                double numberValue = criteriaValidationDto.getCriteriaType().equals(Type.SelectorType.PARAMETER) ?
                  getLatestParameterValue(jobId, criteriaValidationDto.getValueParameterId()) : Double.parseDouble(criteriaValidationDto.getValue());
                if (input != numberValue) {
                  parameterDetailsDto.setRuleId(parameterValidationDto.getRuleId());
                  parameterDetailsDto.setRuleDetails(JsonUtils.valueToNode(parameterValidationDto.getCriteriaValidations()));
                  ValidationUtils.addError(parameterId.toString(), errorList, ErrorCode.NUMBER_PARAMETER_RELATION_PROPERTY_VALIDATION_ERROR, criteriaValidationDto.getErrorMessage(), parameterDetailsDto);
                }
                break;
              case LESS_THAN:
                numberValue = criteriaValidationDto.getCriteriaType().equals(Type.SelectorType.PARAMETER) ?
                  getLatestParameterValue(jobId, criteriaValidationDto.getValueParameterId()) : Double.parseDouble(criteriaValidationDto.getValue());
                if (input >= numberValue) {
                  parameterDetailsDto.setRuleId(parameterValidationDto.getRuleId());
                  parameterDetailsDto.setRuleDetails(JsonUtils.valueToNode(parameterValidationDto.getCriteriaValidations()));
                  ValidationUtils.addError(parameterId.toString(), errorList, ErrorCode.NUMBER_PARAMETER_RELATION_PROPERTY_VALIDATION_ERROR, criteriaValidationDto.getErrorMessage(), parameterDetailsDto);
                }
                break;
              case LESS_THAN_EQUAL_TO:
                numberValue = criteriaValidationDto.getCriteriaType().equals(Type.SelectorType.PARAMETER) ?
                  getLatestParameterValue(jobId, criteriaValidationDto.getValueParameterId()) : Double.parseDouble(criteriaValidationDto.getValue());
                if (input > numberValue) {
                  parameterDetailsDto.setRuleId(parameterValidationDto.getRuleId());
                  parameterDetailsDto.setRuleDetails(JsonUtils.valueToNode(parameterValidationDto.getCriteriaValidations()));
                  ValidationUtils.addError(parameterId.toString(), errorList, ErrorCode.NUMBER_PARAMETER_RELATION_PROPERTY_VALIDATION_ERROR, criteriaValidationDto.getErrorMessage(), parameterDetailsDto);
                }
                break;
              case MORE_THAN:
                numberValue = criteriaValidationDto.getCriteriaType().equals(Type.SelectorType.PARAMETER) ?
                  getLatestParameterValue(jobId, criteriaValidationDto.getValueParameterId()) : Double.parseDouble(criteriaValidationDto.getValue());
                if (input <= numberValue) {
                  parameterDetailsDto.setRuleId(parameterValidationDto.getRuleId());
                  parameterDetailsDto.setRuleDetails(JsonUtils.valueToNode(parameterValidationDto.getCriteriaValidations()));
                  ValidationUtils.addError(parameterId.toString(), errorList, ErrorCode.NUMBER_PARAMETER_RELATION_PROPERTY_VALIDATION_ERROR, criteriaValidationDto.getErrorMessage(), parameterDetailsDto);
                }
                break;
              case MORE_THAN_EQUAL_TO:
                numberValue = criteriaValidationDto.getCriteriaType().equals(Type.SelectorType.PARAMETER) ?
                  getLatestParameterValue(jobId, criteriaValidationDto.getValueParameterId()) : Double.parseDouble(criteriaValidationDto.getValue());
                if (input < numberValue) {
                  parameterDetailsDto.setRuleId(parameterValidationDto.getRuleId());
                  parameterDetailsDto.setRuleDetails(JsonUtils.valueToNode(parameterValidationDto.getCriteriaValidations()));
                  ValidationUtils.addError(parameterId.toString(), errorList, ErrorCode.NUMBER_PARAMETER_RELATION_PROPERTY_VALIDATION_ERROR, criteriaValidationDto.getErrorMessage(), parameterDetailsDto);
                }
                break;
            }
          }
        }

      }
      if (!Utility.isEmpty(errorList)) {
        throw new ParameterExecutionException(errorList);
      }
    }
  }

  @Override
  public void validateParameterValueChoice(String objectId, String objectTypeExternalId, JsonNode validations, String parameterId, Long jobId, boolean isScheduled) throws StreemException, ResourceNotFoundException, IOException, ParameterExecutionException {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (isScheduled) {
      Long currentFacilityId = jobRepository.getFacilityIdByJobId(jobId);
      principalUser.setCurrentFacilityId(currentFacilityId);
    }
    Facility facility = facilityRepository.getReferenceById(principalUser.getCurrentFacilityId());

    List<ParameterValidationDto> parameterValidationDtoList = JsonUtils.jsonToCollectionType(validations, List.class, ParameterValidationDto.class);
    //TODO: optimise database calls
    Parameter parameter = parameterRepository.getReferenceById(Long.valueOf(parameterId));
    ParameterValue parameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, Long.valueOf(parameterId));


    String facilityTimeZone = facility.getTimeZone();
    if (!Utility.isEmpty(parameterValidationDtoList)) {
      List<Error> errorList = new ArrayList<>();
      for (ParameterValidationDto parameterValidationDto : parameterValidationDtoList) {
        for (ParameterRelationPropertyValidationDto validation : parameterValidationDto.getPropertyValidations()) {
          ParameterDetailsDto parameterDetailsDto = new ParameterDetailsDto();
          parameterDetailsDto.setParameterExecutionId(parameterValue.getId().toString());
          parameterDetailsDto.setParameterId(parameter.getId().toString());
          parameterDetailsDto.setType(parameter.getType());
          parameterDetailsDto.setTargetEntityType(parameter.getTargetEntityType());

          parameterDetailsDto.setExceptionApprovalType(parameterValidationDto.getExceptionApprovalType());
          // TODO relation is optional how will you tackle ?
          EntityObject entityObject = entityObjectRepository.findById(objectTypeExternalId, objectId)
            .orElseThrow(() -> new ResourceNotFoundException(objectId, ErrorCode.ENTITY_OBJECT_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
          Map<String, PropertyValue> propertyValueMap = entityObject.getProperties().stream().collect(Collectors.toMap(pv -> pv.getId().toString(), Function.identity()));

          PropertyValue propertyValue = propertyValueMap.get(validation.getPropertyId());

          ConstraintValidator validator = null;
          if (Utility.isEmpty(propertyValue)) {
            ValidationUtils.invalidate(validation.getId(), ErrorCode.RESOURCE_PARAMETER_RELATION_PROPERTY_VALIDATION_COULD_NOT_RUN_MISSING_DATA);
          }
          String propertyInput = propertyValue.getValue();
          String value = null;
          if (validation.getSelector().equals(Type.SelectorType.PARAMETER)) {
            if (validation.getPropertyInputType().equals(SINGLE_LINE) || validation.getPropertyInputType().equals(MULTI_LINE)) {
              value = getLatestParameterValueForText(jobId, validation.getReferencedParameterId());
            } else if (validation.getPropertyInputType().equals(SINGLE_SELECT) || validation.getPropertyInputType().equals(MULTI_SELECT)) {
              ParameterValue referencedParameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, Long.valueOf(validation.getReferencedParameterId()));
              if (Utility.isEmpty(referencedParameterValue)) {
                ValidationUtils.invalidate(validation.getId(), ErrorCode.RESOURCE_PARAMETER_RELATION_PROPERTY_VALIDATION_COULD_NOT_RUN_MISSING_DATA, validation.getErrorMessage());
              }
              if (referencedParameterValue.getState() != State.ParameterExecution.EXECUTED) {
                ValidationUtils.invalidate(validation.getId(), ErrorCode.NUMBER_PARAMETER_RESOURCE_VALIDATION_ERROR);
              }
              Optional<ObjectType> objectType = objectTypeRepository.findById(entityObject.getObjectTypeId().toString());
              Property property = null;
              if (objectType.isPresent()) {
                List<Property> allProperties = objectType.get().getProperties();
                property = allProperties.stream()
                  .filter(p -> p.getId().equals(new ObjectId(validation.getPropertyId()))).findFirst()
                  .orElseThrow(() -> new ResourceNotFoundException(validation.getPropertyId(), ErrorCode.PROPERTY_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
                // Continue with your logic using the 'prop' object
              }
              List<PropertyOption> options = getSelectedPropertyOptions(referencedParameterValue, property);
              validation.setOptions(options);
            } else {
              ParameterValue referencedParameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, Long.valueOf(validation.getReferencedParameterId()));
              if (referencedParameterValue.getState() != State.ParameterExecution.EXECUTED) {
                ValidationUtils.invalidate(validation.getId(), ErrorCode.NUMBER_PARAMETER_RESOURCE_VALIDATION_ERROR);
              }
              value = getLatestParameterValue(jobId, validation.getReferencedParameterId()).toString();
            }

          } else {
            value = validation.getValue();
          }
          switch (validation.getPropertyInputType()) {
            case DATE -> {
              if (Utility.isEmpty(propertyInput)) {
                ValidationUtils.invalidate(validation.getId(), ErrorCode.RESOURCE_PARAMETER_RELATION_PROPERTY_VALIDATION_COULD_NOT_RUN_MISSING_DATA);
              }
              value = String.valueOf((int) Math.floor(Double.parseDouble(value)));
              validator = new DateValidator(DateTimeUtils.atStartOfDay(DateTimeUtils.now(), facilityTimeZone), Long.valueOf(value), validation.getErrorMessage(), validation.getDateUnit(), validation.getConstraint(), facility.getTimeZone());
              validator.validate(Long.valueOf(propertyInput));
            }
            case DATE_TIME -> {
              Long secondsOffset = Math.round(Double.parseDouble(value) * 3600);
              if (Utility.isEmpty(propertyInput)) {
                ValidationUtils.invalidate(validation.getId(), ErrorCode.RESOURCE_PARAMETER_RELATION_PROPERTY_VALIDATION_COULD_NOT_RUN_MISSING_DATA);
              }

              validator = new DateTimeValidator(DateTimeUtils.now(), secondsOffset, validation.getErrorMessage(), CollectionMisc.DateUnit.SECONDS, validation.getConstraint(), facility.getTimeZone());
              validator.validate(propertyInput);
            }
            case NUMBER -> {
              if (Utility.isEmpty(propertyInput)) {
                ValidationUtils.invalidate(validation.getId(), ErrorCode.RESOURCE_PARAMETER_RELATION_PROPERTY_VALIDATION_COULD_NOT_RUN_MISSING_DATA);
              }
              var validationValue = Double.parseDouble(value);
              switch (validation.getConstraint()) {
                case EQ -> {
                  validator = new EqualValueValidator(validationValue, validation.getErrorMessage());
                  validator.validate(Double.parseDouble(propertyInput));
                }
                case LT -> {
                  validator = new LessThanValidator(validationValue, validation.getErrorMessage());
                  validator.validate(propertyInput);
                }
                case GT -> {
                  validator = new GreaterThanValidator(validationValue, validation.getErrorMessage());
                  validator.validate(propertyInput);
                }
                case LTE -> {
                  validator = new LessThanOrEqualValidator(validationValue, validation.getErrorMessage());
                  validator.validate(propertyInput);
                }
                case GTE -> {
                  validator = new GreaterThanOrEqualValidator(validationValue, validation.getErrorMessage());
                  validator.validate(propertyInput);
                }
                case NE -> {
                  validator = new NotEqualValueValidator(validationValue, validation.getErrorMessage());
                  validator.validate(Double.parseDouble(propertyInput));
                }
              }
            }
            case SINGLE_LINE, MULTI_LINE -> {
              if (Utility.isEmpty(propertyInput)) {
                ValidationUtils.invalidate(validation.getId(), ErrorCode.RESOURCE_PARAMETER_RELATION_PROPERTY_VALIDATION_COULD_NOT_RUN_MISSING_DATA);
              }
              switch (validation.getConstraint()) {
                case EQ -> {
                  validator = new StringEqualValidator(value, validation.getErrorMessage());
                  validator.validate(propertyInput);
                }
                case NE -> {
                  validator = new StringNotEqualValidator(value, validation.getErrorMessage());
                  validator.validate(propertyInput);
                }
              }
            }
            case SINGLE_SELECT, MULTI_SELECT -> {
              List<PropertyOption> choices = propertyValue.getChoices();
              if (!Utility.isEmpty(choices) && !Utility.isEmpty(validation.getOptions())) {
                Set<String> validationChoices = validation.getOptions().stream().map(v -> v.getId().toString()).collect(Collectors.toSet());
                Set<String> propertyChoices = choices.stream().map(PropertyOption::getId).map(Object::toString).collect(Collectors.toSet());
                switch (validation.getConstraint()) {
                  case EQ -> {
                    validator = new EqualValueValidator(propertyChoices, validation.getErrorMessage());
                    validator.validate(validationChoices);
                  }
                  case NE -> {
                    validator = new NotEqualValueValidator(propertyChoices, validation.getErrorMessage());
                    validator.validate(validationChoices);
                  }
                  case ALL -> {
                    validator = new AllValuesValidator(propertyChoices, validation.getErrorMessage());
                    validator.validate(validationChoices);
                  }
                  case NOT_ALL -> {
                    validator = new NotAllValuesValidator(propertyChoices, validation.getErrorMessage());
                    validator.validate(validationChoices);
                  }
                  case ANY -> {
                    validator = new AnyValueValidator(propertyChoices, validation.getErrorMessage());
                    validator.validate(validationChoices);
                  }
                  case NIN -> {
                    validator = new NotInValuesValidator(propertyChoices, validation.getErrorMessage());
                    validator.validate(validationChoices);
                  }
                }
              } else {
                ValidationUtils.invalidate(validation.getId(), ErrorCode.RESOURCE_PARAMETER_RELATION_PROPERTY_VALIDATION_COULD_NOT_RUN_MISSING_DATA);
              }
            }
          }
          parameterDetailsDto.setRuleId(parameterValidationDto.getRuleId());
          parameterDetailsDto.setRuleDetails(JsonUtils.valueToNode(parameterValidationDto.getPropertyValidations()));
          validateParameterRelation(validation, errorList, validator, entityObject, parameterId, parameterDetailsDto);
        }
      }
      if (!Utility.isEmpty(errorList)) {
        throw new ParameterExecutionException(errorList);
      }
    }
  }

  @Override
  public void validateIfCorrectionCanBeInitiated(ParameterValue parameterValue) throws StreemException {
    List<Error> errorList = new ArrayList<>();

    Long parameterId = parameterValue.getParameterId();

    boolean isParameterUsedInAutoInitialisation = parameterRepository.isParameterUsedInAutoInitialization(parameterId);

    if (isParameterUsedInAutoInitialisation) {
      ValidationUtils.addError(parameterId, errorList, ErrorCode.CORRECTION_AUTO_INITIALIZED_PARAMETER);
    }

    if (!Utility.isEmpty(errorList)) {
      ValidationUtils.invalidate(String.valueOf(parameterValue.getParameterId()), errorList);
    }
  }

  @Override
  public void validateDateAndDateTimeParameterValidations(Long jobId, JsonNode validations, String inputValue, boolean isDateTimeParameter, String facilityTimeZone, Long parameterId) throws StreemException, IOException, ParameterExecutionException {

    List<ParameterValidationDto> parameterValidationDtoList = JsonUtils.jsonToCollectionType(validations, List.class, ParameterValidationDto.class);
    Parameter parameter = parameterRepository.getReferenceById(parameterId);
    ParameterValue parameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, parameterId);
    if (!Utility.isEmpty(parameterValidationDtoList)) {
      List<Error> errorList = new ArrayList<>();
      for (ParameterValidationDto parameterValidationDto : parameterValidationDtoList) {
        for (DateParameterValidationDto validation : parameterValidationDto.getDateTimeParameterValidations()) {
          ParameterDetailsDto parameterDetailsDto = new ParameterDetailsDto();
          parameterDetailsDto.setParameterExecutionId(parameterValue.getId().toString());
          parameterDetailsDto.setParameterId(parameter.getId().toString());
          parameterDetailsDto.setType(parameter.getType());
          parameterDetailsDto.setTargetEntityType(parameter.getTargetEntityType());
          parameterDetailsDto.setExceptionApprovalType(parameterValidationDto.getExceptionApprovalType());
          parameterDetailsDto.setRuleId(parameterValidationDto.getRuleId());
          parameterDetailsDto.setRuleDetails(JsonUtils.valueToNode(parameterValidationDto.getPropertyValidations()));

          ConstraintValidator validator = null;
          if (validation.getSelector() == Type.SelectorType.CONSTANT) {
            String value = validation.getValue();
            if (!isDateTimeParameter) {
              validator = new DateValidator(DateTimeUtils.atStartOfDay(DateTimeUtils.now(), facilityTimeZone), Long.valueOf(value), validation.getErrorMessage(), validation.getDateUnit(), validation.getConstraint(), facilityTimeZone);
              validator.validate(String.valueOf(DateTimeUtils.getLocalDateEpoch(Long.parseLong(inputValue), facilityTimeZone)));

            } else {
              validator = new DateTimeValidator(DateTimeUtils.now(), Long.valueOf(value), validation.getErrorMessage(), validation.getDateUnit(), validation.getConstraint(), facilityTimeZone);
              validator.validate(inputValue);

            }
          } else if (validation.getSelector() == Type.SelectorType.PARAMETER) {
            String referencedParameterId = validation.getReferencedParameterId();
            boolean isReferencedParameterValueHidden = isParameterHidden(jobId, referencedParameterId);
            if (isReferencedParameterValueHidden) {
              continue;
            }
            Long parameterInputValue = getLatestParameterValueForDate(jobId, referencedParameterId);
            if (!isDateTimeParameter) {
              validator = new DateValidator(parameterInputValue, 0L, validation.getErrorMessage(), CollectionMisc.DateUnit.MINUTES, validation.getConstraint(), facilityTimeZone);
            } else {
              validator = new DateTimeValidator(parameterInputValue, 0L, validation.getErrorMessage(), CollectionMisc.DateUnit.MINUTES, validation.getConstraint(), facilityTimeZone);
            }
            validator.validate(inputValue);
          }
          validateDateAndDateTimeParameterValidation(parameterValue.getId(), validation, errorList, validator, parameterDetailsDto);
        }
      }
      if (!Utility.isEmpty(errorList)) {
        throw new ParameterExecutionException(errorList);
      }
    }
  }


  private static void validateParameterRelation(ParameterRelationPropertyValidationDto validation, List<Error> errorList, ConstraintValidator validator, EntityObject entityObject, String parameterId, ParameterDetailsDto parameterDetailsDto) throws StreemException {
    if (validator != null && !validator.isValid()) {
      String errorMessage = getValidatorErrorMessage(entityObject, validator.getErrorMessage());
      ValidationUtils.addError(parameterId, errorList, ErrorCode.RESOURCE_PARAMETER_RELATION_PROPERTY_VALIDATION_ERROR, errorMessage, parameterDetailsDto);
    }
  }

  private static String getValidatorErrorMessage(EntityObject entityObject, String validatorErrorMessage) {
    if (!Utility.isEmpty(entityObject)) {
      return String.format("%s (ID:%s): %s",
        entityObject.getDisplayName(),
        entityObject.getExternalId(),
        validatorErrorMessage);
    } else {
      return validatorErrorMessage;
    }
  }


  /**
   * This method checks if the parameter is executed partially or not.
   * If the parameter is executed partially then it returns true else false.
   * Here we check for user payload.
   *
   * @param parameter
   * @param data
   * @param isExecutedForCorrection
   * @return
   * @throws IOException
   */
  @Override
  public boolean isParameterExecutedPartially(Parameter parameter, String data, boolean isExecutedForCorrection, List<ParameterValueMediaMapping> parameterValueMediaMappings,
                                              List<TempParameterValueMediaMapping> tempParameterValueMediaMapping) throws IOException {
    switch (parameter.getType()) {
      case SINGLE_LINE, MULTI_LINE, NUMBER, CALCULATION, DATE, DATE_TIME, SHOULD_BE -> {
        TextParameter multiLineParameter = JsonUtils.readValue(data, TextParameter.class);
        return Utility.trimAndCheckIfEmpty(multiLineParameter.getInput());
      }
      case MEDIA, FILE_UPLOAD -> {
        if (isExecutedForCorrection) {
          return tempParameterValueMediaMapping.stream().allMatch(TempParameterValueMediaMapping::isArchived);
        } else {
          return parameterValueMediaMappings.stream().allMatch(ParameterValueMediaMapping::isArchived);
        }
      }
      case RESOURCE, MULTI_RESOURCE -> {
        ResourceParameter resourceParameter = JsonUtils.readValue(data, ResourceParameter.class);
        return Utility.isEmpty(resourceParameter.getChoices());
      }
      case SINGLE_SELECT, MULTISELECT -> {
        List<MultiSelectParameter> parameters = JsonUtils.jsonToCollectionType(data, List.class, MultiSelectParameter.class);
        for (MultiSelectParameter multiSelectParameter : parameters) {
          String state = multiSelectParameter.getState();
          if (State.Selection.SELECTED.equals(State.Selection.valueOf(state))) {
            return false;
          }
        }
        return true;
      }
      case CHECKLIST -> {
        boolean isAllSelected = true;
        List<ChecklistParameter> parameters = JsonUtils.jsonToCollectionType(data, List.class, ChecklistParameter.class);
        for (ChecklistParameter checklistParameter : parameters) {
          String state = checklistParameter.getState();
          if (State.Selection.NOT_SELECTED.equals(State.Selection.valueOf(state))) {
            isAllSelected = false;
          }
        }
        return !isAllSelected;
      }
    }
    return false;
  }

  /**
   * This method checks parameter value is incomplete or not.
   * Here we check for stored database value.
   *
   * @param parameter
   * @param data
   * @param isExecutedForCorrection
   * @return
   * @throws IOException
   */
  @Override
  public boolean isParameterValueIncomplete(Parameter parameter, String data, boolean isExecutedForCorrection, List<ParameterValueMediaMapping> parameterValueMediaMappings,
                                            List<TempParameterValueMediaMapping> tempParameterValueMediaMapping, List<Media> correctionMediaList, String correctionValue, JsonNode correctionChoice) throws IOException {
    switch (parameter.getType()) {
      case SINGLE_LINE, MULTI_LINE, NUMBER, CALCULATION, DATE, DATE_TIME, SHOULD_BE -> {
        return Utility.isEmpty(isExecutedForCorrection ? correctionValue : data);
      }
      case MEDIA, FILE_UPLOAD, SIGNATURE -> {
        if (isExecutedForCorrection) {
          List<MediaDto> previousMedias = parameterMapper.getMedias(parameterValueMediaMappings);
          return areAllPreviousMediaArchived(previousMedias, correctionMediaList);
        } else {
          return parameterValueMediaMappings.stream().allMatch(ParameterValueMediaMapping::isArchived);
        }
      }
      case RESOURCE, MULTI_RESOURCE -> {
        List<ResourceParameterChoiceDto> choices = new ArrayList<>();
        if (isExecutedForCorrection) {
          choices = JsonUtils.jsonToCollectionType(correctionChoice, List.class, ResourceParameterChoiceDto.class);
        } else {
          choices = JsonUtils.jsonToCollectionType(data, List.class, ResourceParameterChoiceDto.class);
        }
        return Utility.isEmpty(choices);
      }
      case SINGLE_SELECT, MULTISELECT, YES_NO -> {
        Map<String, String> choices = JsonUtils.readValue(isExecutedForCorrection ? correctionChoice.toString() : data, new TypeReference<>() {
        });
        if (choices == null || choices.isEmpty()) {
          return true;  // Return true if 'choices' is null or empty
        }
        for (String status : choices.values()) {
          if (status.equals(State.Selection.SELECTED.name())) {
            return false;
          }
        }
        return true;
      }
      case CHECKLIST -> {
        boolean isAllSelected = true;
        Map<String, String> choices = JsonUtils.readValue(data, new TypeReference<>() {
        });
        for (String status : choices.values()) {
          if (status.equals(State.Selection.NOT_SELECTED.name())) {
            isAllSelected = false;
            break;
          }
        }
        return !isAllSelected;
      }
    }
    return false;
  }

  public void validateLeastCount(Long parameterId, LeastCount leastCount, BigDecimal inputValue, Long jobId) throws StreemException {
    if (leastCount.getSelector() == Type.SelectorType.CONSTANT || leastCount.getSelector() == Type.SelectorType.PARAMETER) {
      BigDecimal leastCountValue;
      if (leastCount.getSelector() == Type.SelectorType.CONSTANT) {
        leastCountValue = new BigDecimal(leastCount.getValue());
      } else {
        String leastCountReferencedParameterId = leastCount.getReferencedParameterId();
        ParameterValue referencedParameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, Long.valueOf(leastCountReferencedParameterId));

        if (referencedParameterValue.getState() != State.ParameterExecution.EXECUTED) {
          ValidationUtils.invalidate(String.valueOf(parameterId), ErrorCode.LEAST_COUNT_LINKED_PARAMETER_NOT_EXECUTED, ErrorCode.LEAST_COUNT_LINKED_PARAMETER_NOT_EXECUTED.getDescription());
        }
        if (Utility.isEmpty(referencedParameterValue.getValue())) {
          ValidationUtils.invalidate(String.valueOf(parameterId), ErrorCode.LEAST_COUNT_LINKED_PARAMETER_VALUE_NOT_FOUND, ErrorCode.LEAST_COUNT_LINKED_PARAMETER_VALUE_NOT_FOUND.getDescription());
        }
        leastCountValue = new BigDecimal(referencedParameterValue.getValue());
      }

      BigDecimal nearestMultiple = inputValue.divide(leastCountValue, 0, RoundingMode.HALF_UP).multiply(leastCountValue);
      if (inputValue.doubleValue() != nearestMultiple.doubleValue()) {
        ValidationUtils.invalidate(String.valueOf(parameterId), ErrorCode.ENTERED_VALUE_CANNOT_BE_ACCEPTED_AS_LEASTCOUNT_OF_PARAMETER, ErrorCode.ENTERED_VALUE_CANNOT_BE_ACCEPTED_AS_LEASTCOUNT_OF_PARAMETER.getDescription() + leastCountValue);
      }
    }
  }

  private Double getLatestParameterValue(Long jobId, String parameterId) throws StreemException {
    ParameterValue parameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, Long.valueOf(parameterId));
    String value = parameterValue.getValue();
    State.ParameterExecution parameterExecutionState = parameterValue.getState();
    boolean isParameterExecuted = parameterExecutionState == State.ParameterExecution.EXECUTED;
    if (Utility.isEmpty(value) || !isParameterExecuted) {
      ValidationUtils.invalidate(parameterId, ErrorCode.NUMBER_PARAMETER_CRITERIA_VALIDATION_ERROR);
    }
    return Double.valueOf(value);
  }

  private Boolean isParameterHidden(Long jobId, String parameterId) {
    ParameterValue parameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, Long.valueOf(parameterId));
    return parameterValue.isHidden();
  }

  private Long getLatestParameterValueForDate(Long jobId, String parameterId) throws StreemException {
    ParameterValue parameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, Long.valueOf(parameterId));
    String value = parameterValue.getValue();
    State.ParameterExecution parameterExecutionState = parameterValue.getState();
    boolean isParameterExecuted = parameterExecutionState == State.ParameterExecution.EXECUTED;
    if (Utility.isEmpty(value) || !isParameterExecuted) {
      ValidationUtils.invalidate(parameterId, ErrorCode.DATE_PARAMETER_VALIDATION_ERROR);
    }
    return Long.valueOf(value);
  }

  private String getLatestParameterValueForText(Long jobId, String parameterId) throws StreemException {
    ParameterValue parameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, Long.valueOf(parameterId));
    String value = parameterValue.getValue();
    State.ParameterExecution parameterExecutionState = parameterValue.getState();
    boolean isParameterExecuted = parameterExecutionState == State.ParameterExecution.EXECUTED;
    if (Utility.isEmpty(value) || !isParameterExecuted) {
      ValidationUtils.invalidate(parameterId, ErrorCode.NUMBER_PARAMETER_CRITERIA_VALIDATION_ERROR);
    }
    return value;
  }

  // For Correction
  private static boolean areAllPreviousMediaArchived(List<MediaDto> previousMediaList, List<Media> currentMediaList) {
    // Collect IDs of all previous media
    List<MediaDto> previousNonArchivedMediaList = previousMediaList.stream()
      .filter(media -> !media.isArchived())
      .toList();

    Set<Long> previousMediaIds = previousNonArchivedMediaList.stream()
      .map(media -> Long.valueOf(media.getId()))
      .collect(Collectors.toSet());

    // Collect IDs of all current media
    Set<Long> currentMediaIds = currentMediaList.stream()
      .map(Media::getId)
      .collect(Collectors.toSet());

    // Ensure all previous media are archived in the current media list
    for (MediaDto previousMedia : previousNonArchivedMediaList) {
      Long previousMediaId = Long.valueOf(previousMedia.getId());
      if (currentMediaIds.contains(previousMediaId)) {
        // Find the matching media in the current list
        Media matchingMedia = currentMediaList.stream()
          .filter(media -> media.getId().equals(previousMediaId))
          .findFirst()
          .orElse(null);
        // If the matching media is not archived, return false
        if (matchingMedia != null && !matchingMedia.isArchived()) {
          return false;
        }
      } else {
        // If a previous media item is not found in the current media list, it means it's archived
        return false;
      }
    }

    // Check if any new media items in the current media list are not archived
    for (Media currentMedia : currentMediaList) {
      if (!previousMediaIds.contains(currentMedia.getId())) {
        if (!currentMedia.isArchived()) {
          return false;
        }
      }
    }

    return true;
  }


  private static void validateDateAndDateTimeParameterValidation(
    Long parameterValueId,
    DateParameterValidationDto validation,
    List<Error> errorList,
    ConstraintValidator validator,
    ParameterDetailsDto parameterDetailsDto) throws StreemException {

    if (validator != null && !validator.isValid()) {
      String errorMessage = validator.getErrorMessage();
      ValidationUtils.addError(
        String.valueOf(parameterValueId),
        errorList,
        ErrorCode.DATE_DATE_TIME_PARAMETER_RELATION_PROPERTY_VALIDATION_ERROR,
        errorMessage,
        parameterDetailsDto
      );
    }
  }

  private List<PropertyOption> getSelectedPropertyOptions(ParameterValue parameterValue, Property property) throws IOException {
    Map<String, String> choices = JsonUtils.readValue(parameterValue.getChoices().toString(), new TypeReference<>() {
    });

    Set<String> selectedChoices = choices.entrySet().stream()
      .filter(e -> e.getValue().equals(State.Selection.SELECTED.name()))
      .map(Map.Entry::getKey)
      .collect(Collectors.toSet());

    return selectedChoices.stream()
      .map(id -> {
        PropertyOption propertyOption = property.getOptions().stream()
          .filter(choice -> choice.getId().toString().equals(id))
          .findFirst()
          .orElse(null);
        return new PropertyOption(new ObjectId(id), propertyOption != null ? propertyOption.getDisplayName() : null);
      })
      .collect(Collectors.toList());
  }
}
