package com.leucine.streem.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.projection.IdView;
import com.leucine.streem.dto.projection.ObjectPropertyRelationChecklistView;
import com.leucine.streem.dto.response.Error;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.Automation;
import com.leucine.streem.model.Parameter;
import com.leucine.streem.model.helper.parameter.ChoiceParameterBase;
import com.leucine.streem.model.helper.parameter.ResourceParameter;
import com.leucine.streem.model.helper.parameter.SingleSelectParameter;
import com.leucine.streem.repository.*;
import com.leucine.streem.service.IParameterValidationService;
import com.leucine.streem.util.JsonUtils;
import com.leucine.streem.util.Utility;
import com.leucine.streem.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParameterValidationService implements IParameterValidationService {
  private final IParameterRepository parameterRepository;
  private final IAutoInitializedParameterRepository autoInitializedParameterRepository;
  private final IAutomationRepository automationRepository;
  private final IInterlockRepository interlockRepository;
  private final ITaskAutomationMappingRepository taskAutomationMappingRepository;

  @Override
  public void validateIfParameterCanBeArchived(Long parameterId, Long checklistId, boolean isUnmap) throws StreemException {
    String hideRulesJson = """
      [{"hide": {"parameters": ["%s"]}}]
      """.formatted(parameterId.toString());

    String showRulesJson = """
      [{"show": {"parameters": ["%s"]}}]
      """.formatted(parameterId.toString());


    List<IdView> parameterIdViewUsedInRules = parameterRepository.getAllParametersWhereParameterIsUsedInRules(hideRulesJson, showRulesJson, parameterId);

    List<Error> errorList = new ArrayList<>();

    if (!Utility.isEmpty(parameterIdViewUsedInRules)) {
      for (IdView idView : parameterIdViewUsedInRules) {
        ValidationUtils.addError(idView.getId(), errorList, ErrorCode.PARAMETER_USED_IN_RULES);
      }
    }

    if (!isUnmap) {
      List<IdView> parameterIdViewUsedInFilters = parameterRepository.getAllParametersWhereParameterIsUsedInPropertyFilters(parameterId.toString());
      List<IdView> autoInitializedParameterIdView = autoInitializedParameterRepository.getAllAutoInitializedParametersWhereParameterIsUsed(parameterId);
      List<IdView> parameterUsedInAutomationsIdView = automationRepository.getAllParametersWhereParameterIdUsedInAutomation(parameterId.toString());
      List<IdView> parameterHavingResourceValidations = parameterRepository.getAllParametersWhereParameterIsUsedInResourceValidations(parameterId.toString());
      List<IdView> interlocksWhereParameterIsUsed = interlockRepository.getAllParameterWhereParameterIdUsedInInterlocks(parameterId.toString(), checklistId);
      List<IdView> parameterUsedInCalculation = parameterRepository.getAllParameterIdsWhereParameterIsUsedInCalculation(parameterId.toString(), checklistId);
      List<IdView> parameterUsedInLeastCount = parameterRepository.getParameterIdWhereParameterIsUsedInLeastCount(parameterId.toString(), checklistId);
      List<IdView> parameterUsedInNumberCriteriaValidation = parameterRepository.getParameterIdWhereParameterIsUsedInNumberCriteriaValidation(parameterId.toString(), checklistId);
      List<IdView> parameterHavingDateAndDateTimeValidations = parameterRepository.getAllParametersWhereParameterIsUsedDateAndDateTimeValidations(parameterId.toString());
      List<IdView> parameterIdViewUsedInValidations = parameterRepository.getAllParametersWhereParameterIsUsedInPropertyValidations(parameterId.toString());

      boolean linkedParameterExistsByParameterId = parameterRepository.isLinkedParameterExistsByParameterId(checklistId, parameterId.toString());

      if (!Utility.isEmpty(autoInitializedParameterIdView)) {
        for (IdView idView : autoInitializedParameterIdView) {
          ValidationUtils.addError(idView.getId(), errorList, ErrorCode.PARAMETER_USED_IN_AUTO_INITIALIZE);
        }
      }

      if (!Utility.isEmpty(parameterUsedInAutomationsIdView)) {
        for (IdView idView : parameterUsedInAutomationsIdView) {
          ValidationUtils.addError(idView.getId(), errorList, ErrorCode.PARAMETER_USED_IN_AUTOMATION);
        }
      }

      if (!Utility.isEmpty(parameterHavingResourceValidations)) {
        for (IdView idView : parameterHavingResourceValidations) {
          ValidationUtils.addError(idView.getId(), errorList, ErrorCode.PARAMETER_USED_IN_PROPERTY_VALIDATIONS);
        }
      }

      if (linkedParameterExistsByParameterId) {
        ValidationUtils.addError(parameterId, errorList, ErrorCode.PARAMETER_USED_IN_AUTO_INITIALIZE);
      }

      if (!Utility.isEmpty(interlocksWhereParameterIsUsed)) {
        for (IdView idView : interlocksWhereParameterIsUsed) {
          ValidationUtils.addError(idView.getId(), errorList, ErrorCode.PARAMETER_USED_IN_INTERLOCKS);
        }
      }

      if (!Utility.isEmpty(parameterIdViewUsedInFilters)) {
        for (IdView idView : parameterIdViewUsedInFilters) {
          ValidationUtils.addError(idView.getId(), errorList, ErrorCode.PARAMETER_USED_IN_PROPERTY_FILTERS);
        }
      }

      if(!Utility.isEmpty(parameterIdViewUsedInValidations)){
        for (IdView idView : parameterIdViewUsedInValidations) {
          ValidationUtils.addError(idView.getId(), errorList, ErrorCode.PARAMETER_USED_IN_PROPERTY_VALIDATIONS);
        }
      }
      if (!Utility.isEmpty(parameterUsedInCalculation)) {
        for (IdView idView : parameterUsedInCalculation) {
          ValidationUtils.addError(idView.getId(), errorList, ErrorCode.PARAMETER_USED_IN_CALCULATION);
        }
      }
      if (!Utility.isEmpty(parameterUsedInLeastCount)) {
        for (IdView idView : parameterUsedInLeastCount) {
          ValidationUtils.addError(idView.getId(), errorList, ErrorCode.PARAMETER_USED_IN_LEAST_COUNT);
        }
      }

      if (!Utility.isEmpty(parameterUsedInNumberCriteriaValidation)) {
        for (IdView idView : parameterUsedInNumberCriteriaValidation) {
          ValidationUtils.addError(idView.getId(), errorList, ErrorCode.PARAMETER_USED_IN_NUMBER_CRITERIA_VALIDATION);
        }
      }

      if (!Utility.isEmpty(parameterHavingDateAndDateTimeValidations)) {
        for (IdView idView : parameterHavingDateAndDateTimeValidations) {
          ValidationUtils.addError(idView.getId(), errorList, ErrorCode.PARAMETER_USED_IN_DATE_OR_DATETIME_VALIDATIONS);
        }
      }

    }

    if (!Utility.isEmpty(errorList)) {
      ValidationUtils.invalidate(parameterId.toString(), errorList);
    }
  }

  @Override
  public void validateIfParameterCanBeUpdated(Long parameterId, Long checklistId, JsonNode parameterUpdateRequest) throws StreemException, IOException {
    String hideRulesJson = """
      [{"hide": {"parameters": ["%s"]}}]
      """.formatted(parameterId.toString());

    String showRulesJson = """
      [{"show": {"parameters": ["%s"]}}]
      """.formatted(parameterId.toString());
    List<Error> errorList = new ArrayList<>();
    List<IdView> parameterIdViewUsedInRules = parameterRepository.getAllParametersWhereParameterIsUsedInRules(hideRulesJson, showRulesJson, parameterId);
    boolean linkedParameterExistsByParameterId = parameterRepository.isLinkedParameterExistsByParameterId(checklistId, parameterId.toString());

    Parameter parameter = parameterRepository.findById(parameterId).get();
    switch (parameter.getType()) {
      case RESOURCE, MULTI_RESOURCE -> {
        ResourceParameter resourceParameterCurrentDetails = JsonUtils.readValue(parameter.getData().toString(), ResourceParameter.class);
        ResourceParameter resourceParameterUpdateDetails = JsonUtils.readValue(parameterUpdateRequest.toString(), ResourceParameter.class);


        List<IdView> parameterIdViewUsedInFilters = parameterRepository.getAllParametersWhereParameterIsUsedInPropertyFilters(parameterId.toString());
        List<IdView> parameterUsedInAutomationsIdView = automationRepository.getAllParametersWhereParameterIdUsedInAutomation(parameterId.toString());
        List<IdView> parameterHavingResourceValidations = parameterRepository.getAllParametersWhereParameterIsUsedInResourceValidations(parameterId.toString());
        List<IdView> interlocksWhereParameterIsUsed = interlockRepository.getAllParameterWhereParameterIdUsedInInterlocks(parameterId.toString(), checklistId);
        List<IdView> parameterIdViewUsedInValidations = parameterRepository.getAllParametersWhereParameterIsUsedInPropertyValidations(parameterId.toString());

        //Currently Only Resource Parameter is Checked when object type is updated
        if (!Objects.equals(resourceParameterUpdateDetails.getObjectTypeId(), resourceParameterCurrentDetails.getObjectTypeId())) {

          if (!Utility.isEmpty(parameterIdViewUsedInFilters)) {
            for (IdView idView : parameterIdViewUsedInFilters) {
              Parameter dependentParameter = parameterRepository.findById(idView.getId()).get();
              boolean isProcessParameter = dependentParameter.getTargetEntityType() == Type.ParameterTargetEntityType.PROCESS;
              boolean isUnMappedParameter = dependentParameter.getTargetEntityType() == Type.ParameterTargetEntityType.UNMAPPED;
              String errorFormatForNonTaskParameter = "Resource Parameter is used for resource filter in the process parameters.";
              String errorMessage = generateErrorMessage(idView, isUnMappedParameter ? errorFormatForNonTaskParameter.replace("process", "unmapped") : isProcessParameter ? errorFormatForNonTaskParameter : "Resource Parameter is used for resource filter in the: Stage: %s, Task: %s.", isProcessParameter, false);
              ValidationUtils.addError(idView.getId().toString(), errorList, ErrorCode.PARAMETER_USED_IN_PROPERTY_FILTERS, errorMessage);
            }
          }

          if (!Utility.isEmpty(parameterUsedInAutomationsIdView)) {
            for (IdView idView : parameterUsedInAutomationsIdView) {
              Automation automation = automationRepository.findById(idView.getId()).orElse(null);
              if (!Objects.isNull(automation)) {
                ObjectPropertyRelationChecklistView parameterChecklistView = taskAutomationMappingRepository.getChecklistAndTaskInfoByAutomationId(automation.getId());
                if (!Utility.isEmpty(parameterChecklistView)) {
                  String stageName = parameterChecklistView.getStageName();
                  String taskName = parameterChecklistView.getTaskName();
                  String errorMessage = "Resource Parameter is used for automation in the: Stage: %s, Task: %s.".formatted(stageName, taskName);
                  ValidationUtils.addError(idView.getId().toString(), errorList, ErrorCode.PARAMETER_USED_IN_AUTOMATION, errorMessage);
                }
              } else {
                ValidationUtils.addError(idView.getId(), errorList, ErrorCode.PARAMETER_USED_IN_AUTOMATION);
              }
            }
          }

          if(!Utility.isEmpty(parameterIdViewUsedInValidations)){
            for (IdView idView : parameterIdViewUsedInValidations) {
              ValidationUtils.addError(idView.getId(), errorList, ErrorCode.PARAMETER_USED_IN_PROPERTY_VALIDATIONS);
            }
          }

          if (!Utility.isEmpty(parameterHavingResourceValidations)) {
            for (IdView idView : parameterHavingResourceValidations) {
              Parameter dependentParameter = parameterRepository.findById(idView.getId()).get();
              boolean isProcessParameter = dependentParameter.getTargetEntityType() == Type.ParameterTargetEntityType.PROCESS;
              boolean isUnMappedParameter = dependentParameter.getTargetEntityType() == Type.ParameterTargetEntityType.UNMAPPED;
              String errorFormatForNonTaskParameter = "Resource Parameter is used for resource validation in the process parameters.";
              String errorMessage = generateErrorMessage(idView, isUnMappedParameter ? errorFormatForNonTaskParameter.replace("process", "unmapped") : isProcessParameter ? errorFormatForNonTaskParameter : "Resource Parameter is used for resource validation in the: Stage: %s, Task: %s.", isProcessParameter, true);
              ValidationUtils.addError(idView.getId().toString(), errorList, ErrorCode.PARAMETER_USED_IN_PROPERTY_VALIDATIONS, errorMessage);
            }
          }

          if (!Utility.isEmpty(interlocksWhereParameterIsUsed)) {
            for (IdView idView : interlocksWhereParameterIsUsed) {
              ObjectPropertyRelationChecklistView parameterChecklistView = interlockRepository.getChecklistAndTaskInfoByInterlockId(idView.getId());
              if (!Utility.isEmpty(parameterChecklistView)) {
                String stageName = parameterChecklistView.getStageName();
                String taskName = parameterChecklistView.getTaskName();
                String errorMessage = "Resource Parameter is used for Interlocks in the: Stage: %s, Task: %s.".formatted(stageName, taskName);
                ValidationUtils.addError(idView.getId().toString(), errorList, ErrorCode.PARAMETER_USED_IN_INTERLOCKS, errorMessage);

              } else {
                ValidationUtils.addError(idView.getId(), errorList, ErrorCode.PARAMETER_USED_IN_INTERLOCKS);
              }
            }
          }

          if (!Utility.isEmpty(parameterIdViewUsedInRules)) {
            for (IdView idView : parameterIdViewUsedInRules) {
              ValidationUtils.addError(idView.getId(), errorList, ErrorCode.PARAMETER_USED_IN_RULES);
            }
          }

          if (linkedParameterExistsByParameterId) {
            ValidationUtils.addError(parameterId, errorList, ErrorCode.PARAMETER_USED_IN_AUTO_INITIALIZE);
          }

        }
      }
      case SINGLE_SELECT -> {
        List<SingleSelectParameter> oldSingleSelectParameterChoices = JsonUtils.jsonToCollectionType(parameter.getData().toString(), List.class, SingleSelectParameter.class);
        List<SingleSelectParameter> newSingleSelectParameterChoices = JsonUtils.jsonToCollectionType(parameterUpdateRequest.toString(), List.class, SingleSelectParameter.class);

        Set<String> oldChoicesIds = oldSingleSelectParameterChoices.stream()
          .map(ChoiceParameterBase::getId)
          .collect(Collectors.toSet());

        Set<String> newChoicesIds = newSingleSelectParameterChoices.stream()
          .map(ChoiceParameterBase::getId)
          .collect(Collectors.toSet());

        //If Rules are applied removing and adding of options is prevented
        if (!oldChoicesIds.equals(newChoicesIds)) {
          if (!Utility.isEmpty(parameterIdViewUsedInRules)) {
            for (IdView idView : parameterIdViewUsedInRules) {
              ValidationUtils.addError(idView.getId(), errorList, ErrorCode.PARAMETER_USED_IN_RULES);
            }
          }


          if (linkedParameterExistsByParameterId) {
            ValidationUtils.addError(parameterId, errorList, ErrorCode.PARAMETER_USED_IN_AUTO_INITIALIZE);
          }
        }
      }


    }
    if (!Utility.isEmpty(errorList)) {
      ValidationUtils.invalidate(String.valueOf(parameterId), errorList);
    }
  }

  private String generateErrorMessage(IdView idView, String messageFormat, boolean isProcessParameter, boolean isUsedInResourceValidation) {
    ObjectPropertyRelationChecklistView parameterChecklistView = isUsedInResourceValidation ? parameterRepository.getChecklistAndTaskInfoByParameterIdForResourceValidation(idView.getId()) : parameterRepository.getChecklistAndTaskInfoByParameterId(idView.getId());
    String taskName;
    String stageName;
    if (!isProcessParameter) {
      taskName = parameterChecklistView.getTaskName();
      stageName = parameterChecklistView.getStageName();
      return String.format(messageFormat, stageName, taskName);
    } else {
      return String.format(messageFormat);
    }
  }
}
