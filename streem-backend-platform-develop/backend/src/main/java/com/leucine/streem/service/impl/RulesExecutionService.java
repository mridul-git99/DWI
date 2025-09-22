package com.leucine.streem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.constant.Operator;
import com.leucine.streem.constant.State;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.projection.ParameterValueView;
import com.leucine.streem.dto.request.ParameterExecuteRequest;
import com.leucine.streem.dto.request.ParameterRequest;
import com.leucine.streem.model.Parameter;
import com.leucine.streem.model.ParameterValue;
import com.leucine.streem.model.helper.BaseEntity;
import com.leucine.streem.model.helper.parameter.ChoiceParameterBase;
import com.leucine.streem.model.helper.parameter.SingleSelectParameter;
import com.leucine.streem.repository.IParameterRepository;
import com.leucine.streem.repository.IParameterValueRepository;
import com.leucine.streem.repository.impl.ParameterValueRepositoryImpl;
import com.leucine.streem.service.IRulesExecutionService;
import com.leucine.streem.util.JsonUtils;
import com.leucine.streem.util.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RulesExecutionService implements IRulesExecutionService {
  private final IParameterValueRepository parameterValueRepository;
  private final IParameterRepository parameterRepository;

  @Override
  @Transactional
  public RuleHideShowDto updateRules(Long jobId, Parameter parameter, ParameterValue parameterValue) throws IOException {
    Set<Long> show = new HashSet<>();
    Set<Long> hide = new HashSet<>();
    Map<Long, Set<RuleImpactedByDto>> rulesImpactedByMap = new HashMap<>();

    Map<Long, ParameterValueViewDto> parameterIdParameterValueView = new HashMap<>();

    if (!Utility.isEmpty(parameter.getRules())) {
      getCurrentHiddenState(parameter, jobId, show, hide, rulesImpactedByMap, parameterIdParameterValueView);
    }

    RuleHideShowDto ruleHideShowDto = evaluateRules(jobId, parameter, show, hide, rulesImpactedByMap, parameterValue, parameterIdParameterValueView);


    if (!ruleHideShowDto.getHide().isEmpty()) {
      Set<Long> hideIds = ruleHideShowDto.getHide().stream()
        .map(Long::valueOf)
        .collect(Collectors.toSet());

      parameterValueRepository.recallVerificationStateForHiddenParameterValues(hideIds);

      parameterValueRepository.recallVerificationStateForHiddenParameterValuesWithExceptions(hideIds);
    }

    return ruleHideShowDto;
  }

  @Override
  @Transactional
  public RuleHideShowDto tempExecuteRules(Map<Long, ParameterExecuteRequest> parameterExecuteRequestMap, Long checklistId) throws IOException {
    List<ParameterRequest> parameterRequestList = new ArrayList<>();
    for (Map.Entry<Long, ParameterExecuteRequest> entry : parameterExecuteRequestMap.entrySet()) {
      parameterRequestList.add(entry.getValue().getParameter());
    }

    return executeRulesTemporarily(parameterRequestList, checklistId);
  }


  /**
   * This function is used to get current hidden state of a parameter and calculate impactedBy
   *
   * @param parameter          To apply rules present on parameter
   * @param jobId              To apply rules on a job with id
   * @param show               Set of rules to be shown
   * @param hide               Set of rules to be hidden
   * @param rulesImpactedByMap Map of rules impacted by a parameter where key is parameterId of impacted parameter and value is set of rules ids and impacting parameter ids
   * @throws IOException
   */
  private void getCurrentHiddenState(Parameter parameter, Long jobId, Set<Long> show, Set<Long> hide, Map<Long, Set<RuleImpactedByDto>> rulesImpactedByMap, Map<Long, ParameterValueViewDto> parameterIdParameterValueView) throws IOException {
    List<RuleDto> parameterRules = JsonUtils.jsonToCollectionType(parameter.getRules(), List.class, RuleDto.class);

    // To get all the impacted parameters from the rules
    Set<Long> parameterAffectedByRule = new HashSet<>();

    parameterRules.stream()
      .filter(ruleDto -> !Utility.isEmpty(ruleDto.getHide()))
      .flatMap(ruleDto -> ruleDto.getHide().getParameters().stream().map(Long::valueOf))
      .forEach(parameterAffectedByRule::add);


    parameterRules.stream()
      .filter(ruleDto -> !Utility.isEmpty(ruleDto.getShow()))
      .flatMap(ruleDto -> ruleDto.getShow().getParameters().stream().map(Long::valueOf))
      .forEach(parameterAffectedByRule::add);

    Set<Long> parameterIdListForRuleExecution = new HashSet<>(parameterAffectedByRule);
    parameterIdListForRuleExecution.add(parameter.getId());

    List<ParameterValueView> parameterValueViewList = parameterValueRepository.getParameterPartialDataByIds(parameterIdListForRuleExecution, jobId);
    List<ParameterValueViewDto> parameterValueViewDtoList = ParameterValueViewDto.fromViewList(parameterValueViewList);
    parameterIdParameterValueView.putAll(parameterValueViewDtoList.stream()
      .collect(Collectors.toMap(ParameterValueViewDto::getParameterId, Function.identity())));


    for (Long parameterId : parameterAffectedByRule) {
      ParameterValueViewDto parameterValue = parameterIdParameterValueView.get(parameterId);

      //TODO: Find why parameterValue.getParameterId() is null but not below
      if (parameterValue.getHidden()) {
        hide.add(parameterValue.getParameterValueId());
      } else {
        show.add(parameterValue.getParameterValueId());
      }

      if (!Utility.isEmpty(parameterValue.getImpactedBy())) {
        Set<RuleImpactedByDto> ruleImpactedByDtoList = JsonUtils.jsonToCollectionType(parameterValue.getImpactedBy(), Set.class, RuleImpactedByDto.class);
        if (Utility.isEmpty(ruleImpactedByDtoList)) {
          ruleImpactedByDtoList = new HashSet<>();
        }
        rulesImpactedByMap.put(parameterValue.getParameterValueId(), ruleImpactedByDtoList);
      }

    }
  }

  /**
   * This function is used to evaluate rules for a parameter based on current hidden state of parameter and parameter type (Resource or Single Select)
   *
   * @param jobId                         To apply rules on a job with id
   * @param parameter                     To apply rules present on parameter
   * @param show                          Set of rules to be shown
   * @param hide                          Set of rules to be hidden
   * @param rulesImpactedByMap            Map of rules impacted by a parameter where key is parameterId of impacted parameter and value is set of rules ids and impacting parameter ids
   * @param parameterValue
   * @param parameterIdParameterValueView
   * @return
   * @throws IOException
   */
  private RuleHideShowDto evaluateRules(Long jobId, Parameter parameter, Set<Long> show, Set<Long> hide, Map<Long, Set<RuleImpactedByDto>> rulesImpactedByMap, ParameterValue parameterValue, Map<Long, ParameterValueViewDto> parameterIdParameterValueView) throws IOException {

    if (!Utility.isEmpty(parameter.getRules())) {
      List<RuleDto> parameterRules = JsonUtils.jsonToCollectionType(parameter.getRules(), List.class, RuleDto.class);
      Map<String, List<RuleDto>> rulesMap = parameterRules.stream().collect(Collectors.groupingBy(r -> r.getInput()[0], Collectors.mapping(Function.identity(), Collectors.toList())));


      if (!Utility.isEmpty(parameterValue.getChoices())) {
        resetImpactedByForAGivenParameter(parameter, show, hide, parameterRules, rulesImpactedByMap, jobId, parameterIdParameterValueView);

        if (parameter.getType() == Type.Parameter.SINGLE_SELECT) {
          Map<String, String> selectedChoice = JsonUtils.readValue(parameterValue.getChoices().toString(), new TypeReference<>() {
          });
          applyRulesOfSingleSelectParameter(rulesMap, selectedChoice, show, hide, parameter, rulesImpactedByMap, parameterValue, parameterIdParameterValueView);

        } else if (parameter.getType() == Type.Parameter.RESOURCE) {
          applyRulesOfResourceParameter(rulesMap, parameterValue.getChoices(), show, hide, parameter, rulesImpactedByMap, parameterValue, parameterIdParameterValueView);

        }
        updateParameterHiddenStateAndImpactedBy(jobId, rulesImpactedByMap, parameterIdParameterValueView);
      }
      if ((parameter.getType() == Type.Parameter.NUMBER || parameter.getType() == Type.Parameter.CALCULATION)) {
        resetImpactedByForAGivenParameter(parameter, show, hide, parameterRules, rulesImpactedByMap, jobId, parameterIdParameterValueView);
        applyRulesOfNumberParameter(parameterRules, parameterValue.getValue(), show, hide, parameter, rulesImpactedByMap, parameterValue, parameterIdParameterValueView);

      }
    }

    if (!Utility.isEmpty(show)) {
      parameterValueRepository.updateParameterValueVisibility(show, false);
      show.forEach(pv -> {
        if (parameterIdParameterValueView.containsKey(pv)) {
          parameterIdParameterValueView.get(pv).setHidden(false);
        }
      });
    }

    if (!Utility.isEmpty(hide)) {
      parameterValueRepository.updateParameterValueVisibility(hide, true);
      hide.forEach(pv -> {
        if (parameterIdParameterValueView.containsKey(pv)) {
          parameterIdParameterValueView.get(pv).setHidden(true);
        }
      });
    }

    RuleHideShowDto ruleHideShowDto = new RuleHideShowDto();
    ruleHideShowDto.setHide(hide.stream().

      map(String::valueOf).

      collect(Collectors.toSet()));
    ruleHideShowDto.setShow(show.stream().

      map(String::valueOf).

      collect(Collectors.toSet()));

    return ruleHideShowDto;
  }

  private void updateParameterHiddenStateAndImpactedBy(Long jobId, Map<Long, Set<RuleImpactedByDto>> rulesImpactedByMap, Map<Long, ParameterValueViewDto> parameterIdParameterValueView) throws JsonProcessingException {
    if (!Utility.isEmpty(rulesImpactedByMap)) {
      Map<Long, ParameterValue> parameterAndParameterValueMap = parameterValueRepository.findByJobIdAndIdsIn(jobId, rulesImpactedByMap.keySet()).stream()
        .collect(Collectors.toMap(BaseEntity::getId, Function.identity()));

      for (Map.Entry<Long, Set<RuleImpactedByDto>> entry : rulesImpactedByMap.entrySet()) {
        Long p = entry.getKey();
        Set<RuleImpactedByDto> impactedBy = entry.getValue();

        ParameterValue pv = parameterAndParameterValueMap.get(p);
        ParameterValueViewDto parameterValueView = parameterIdParameterValueView.get(pv.getParameter().getId());

        if (!Utility.isEmpty(pv)) {
          pv.setImpactedBy(JsonUtils.valueToNode(impactedBy));
          parameterValueView.setImpactedBy(JsonUtils.writeValueAsString(impactedBy));
        }
      }

      parameterValueRepository.saveAll(parameterAndParameterValueMap.values());

    }
  }

  /**
   * Reset impacted by for a given parameter
   *
   * @param parameter                     Parameter whose impacted by would be reset
   * @param show                          Set of rules to be shown
   * @param hide                          Set of rules to be hidden
   * @param parameterRules                List of rules of the parameter
   * @param rulesImpactedByMap            Map of rules impacted by a parameter where key is parameterId of impacted parameter and value is set of rules ids and impacting parameter ids
   * @param jobId
   * @param parameterIdParameterValueView
   */
  private void resetImpactedByForAGivenParameter(Parameter parameter, Set<Long> show, Set<Long> hide, List<RuleDto> parameterRules, Map<Long, Set<RuleImpactedByDto>> rulesImpactedByMap, Long jobId, Map<Long, ParameterValueViewDto> parameterIdParameterValueView) {
    for (RuleDto ruleDto : parameterRules) {
      if (!Utility.isEmpty(ruleDto.getShow())) {
        processRules(ruleDto.getShow().getParameters(), parameter, show, hide, rulesImpactedByMap, ruleDto, jobId, parameterIdParameterValueView);
      }

      if (!Utility.isEmpty(ruleDto.getHide())) {
        processRules(ruleDto.getHide().getParameters(), parameter, hide, show, rulesImpactedByMap, ruleDto, jobId, parameterIdParameterValueView);
      }

    }

  }

  /**
   * This function is used to manipulate parameter Id type from String to Long and apply reset rules on impacted parameters
   *
   * @param parameterList                 List of parameter ids
   * @param parameter                     Parameter whose impacted by would be reset
   * @param show                          Set of rules to be shown
   * @param hide                          Set of rules to be hidden
   * @param rulesImpactedByMap            Map of rules impacted by a parameter where key is parameterId of impacted parameter and value is set of rules ids and impacting parameter ids
   * @param ruleDto
   * @param jobId
   * @param parameterIdParameterValueView
   */
  private void processRules(List<String> parameterList, Parameter parameter, Set<Long> show, Set<Long> hide, Map<Long, Set<RuleImpactedByDto>> rulesImpactedByMap, RuleDto ruleDto, Long jobId, Map<Long, ParameterValueViewDto> parameterIdParameterValueView) {

    parameterList.stream()
      .map(Long::valueOf)
      .forEach(p -> resetRules(parameter, show, hide, rulesImpactedByMap, ruleDto, p, jobId, parameterIdParameterValueView));

  }

  /**
   * reset parameters impactedBy set for all the parameters which will be affected by currenty parmeter rules
   *
   * @param parameter                     Parameter whose impacted by would be reset
   * @param show                          Set of rules to be shown
   * @param hide                          Set of rules to be hidden
   * @param rulesImpactedByMap            Map of rules impacted by a parameter where key is parameterId of impacted parameter and value is set of rules ids and impacting parameter ids
   * @param ruleDto                       Rule to be removed from impacted by
   * @param toShowOrHide                  Parameter id of impacted parameter
   * @param jobId
   * @param parameterIdParameterValueView
   */
  private void resetRules(Parameter parameter, Set<Long> show, Set<Long> hide, Map<Long, Set<RuleImpactedByDto>> rulesImpactedByMap, RuleDto ruleDto, Long toShowOrHide, Long jobId, Map<Long, ParameterValueViewDto> parameterIdParameterValueView) {

    ParameterValueViewDto parameterValue;
    ParameterValueViewDto toShowOrHideParameterValue;
    Set<RuleImpactedByDto> impactedByOfToShowParameter;
    State.TaskExecution taskExecutionStateOfToShowOrHideParameterValue = null;

    boolean isCjfParameter = parameter.getTargetEntityType() == Type.ParameterTargetEntityType.PROCESS;
    parameterValue = parameterIdParameterValueView.get(parameter.getId());

    toShowOrHideParameterValue = parameterIdParameterValueView.get(toShowOrHide);

    if (!Utility.isEmpty(toShowOrHideParameterValue)) {
      impactedByOfToShowParameter = rulesImpactedByMap.get(toShowOrHideParameterValue.getParameterValueId());
      // Since cjf parameters dont have task execution id
      if (!isCjfParameter) {
        taskExecutionStateOfToShowOrHideParameterValue = toShowOrHideParameterValue.getTaskExecutionState();
      }
    } else {
      impactedByOfToShowParameter = rulesImpactedByMap.get(toShowOrHide);
    }


    boolean isCjfParameterOrIsTaskNotStarted = isCjfParameter || State.TaskExecution.NOT_STARTED == taskExecutionStateOfToShowOrHideParameterValue || Objects.equals(parameterValue.getTaskExecutionId(), toShowOrHideParameterValue.getTaskExecutionId());


    if (isCjfParameterOrIsTaskNotStarted) {
      if (Utility.isEmpty(impactedByOfToShowParameter)) {
        impactedByOfToShowParameter = Collections.emptySet();
      }
      impactedByOfToShowParameter.removeIf(impactedBy -> (Objects.equals(impactedBy.getRuleId(), ruleDto.getId()) && Objects.equals(impactedBy.getParameterValueId(), parameterValue.getParameterValueId())) || Objects.equals(impactedBy.getParameterId(), parameter.getId()));
      if (Utility.isEmpty(impactedByOfToShowParameter)) {
        if (!Utility.isEmpty(toShowOrHideParameterValue)) {
          show.remove(toShowOrHideParameterValue.getParameterValueId());
          hide.add(toShowOrHideParameterValue.getParameterValueId());
        } else {
          show.remove(toShowOrHide);
          hide.add(toShowOrHide);
        }
      }
    }
  }

  /**
   * This function is used to apply rules on a Resource parameter
   *
   * @param rulesMap                      Map of rules where key is rule id and value is list of rules
   * @param selectedResources             List of selected resources of a resource parameter
   * @param show                          Set of rules to be shown
   * @param hide                          Set of rules to be hidden
   * @param parameter                     Parameter of resource parameter
   * @param rulesImpactedByMap            Map of rules impacted by a parameter where key is parameterId of impacted parameter and value is set of rules ids and impacting parameter ids
   * @param parameterValueId              Parameter value id of resource parameter
   * @param parameterIdParameterValueView
   * @throws IOException
   */
  private void applyRulesOfResourceParameter(Map<String, List<RuleDto>> rulesMap, JsonNode selectedResources, Set<Long> show, Set<Long> hide, Parameter parameter, Map<Long, Set<RuleImpactedByDto>> rulesImpactedByMap, ParameterValue parameterValueId, Map<Long, ParameterValueViewDto> parameterIdParameterValueView) throws IOException {
    if (Utility.isEmpty(selectedResources)) {
      return;
    }
    List<ResourceParameterChoiceDto> selectedResourceList = JsonUtils.jsonToCollectionType(selectedResources, List.class, ResourceParameterChoiceDto.class);
    for (ResourceParameterChoiceDto choice : selectedResourceList) {
      if (rulesMap.containsKey(choice.getObjectId())) {
        List<RuleDto> rules = rulesMap.get(choice.getObjectId());

        applyRules(show, hide, parameter, rulesImpactedByMap, rules, parameterValueId, parameterIdParameterValueView);

      }
    }

  }

  /**
   * This function is used to apply rules on a Single Select parameter
   *
   * @param rulesMap                      Map of rules where key is rule id and value is list of rules
   * @param selectedChoice                Map of selected choices where key is choice id and value is selected or not
   * @param show                          Set of rules to be shown
   * @param hide                          Set of rules to be hidden
   * @param parameter                     Parameter id of single select parameter
   * @param rulesImpactedByMap            Map of rules impacted by a parameter where key is parameterId of impacted parameter and value is set of rules ids and impacting parameter ids
   * @param parameterValue                Parameter value id of single select parameter
   * @param parameterIdParameterValueView
   */
  private void applyRulesOfSingleSelectParameter(Map<String, List<RuleDto>> rulesMap, Map<String, String> selectedChoice, Set<Long> show, Set<Long> hide, Parameter parameter, Map<Long, Set<RuleImpactedByDto>> rulesImpactedByMap, ParameterValue parameterValue, Map<Long, ParameterValueViewDto> parameterIdParameterValueView) {
    for (Map.Entry<String, String> entry : selectedChoice.entrySet()) {
      var key = entry.getKey();
      var value = entry.getValue();

      if (State.Selection.SELECTED.name().equals(value) && rulesMap.containsKey(key)) {
        List<RuleDto> rules = rulesMap.get(key);

        applyRules(show, hide, parameter, rulesImpactedByMap, rules, parameterValue, parameterIdParameterValueView);

      }
    }
  }

  /**
   * This function is used to apply rules on a Number parameter
   *
   * @param ruleDtos           Map of rules where key is rule id and value is list of rules
   * @param value              Value of number parameter
   * @param show               Set of rules to be shown
   * @param hide               Set of rules to be hidden
   * @param parameter          Parameter id of number parameter
   * @param rulesImpactedByMap Map of rules impacted by a parameter where key is parameterId of impacted parameter and value is set of rules ids and impacting parameter ids
   * @param parameterValue     Parameter value id of number parameter
   */

  private void applyRulesOfNumberParameter(List<RuleDto> ruleDtos, String value, Set<Long> show, Set<Long> hide, Parameter parameter, Map<Long, Set<RuleImpactedByDto>> rulesImpactedByMap, ParameterValue parameterValue, Map<Long, ParameterValueViewDto> parameterIdParameterValueView) {
    if (Utility.isEmpty(value)) {
      return;
    }

    List<RuleDto> ruleDtoToApply = new ArrayList<>();
    for (RuleDto rule : ruleDtos) {
      Operator.Rules constraint = rule.getConstraint();
      BigDecimal ruleValue;
      BigDecimal inputValue = new BigDecimal(value);

      switch (constraint) {
        case EQ -> {
          ruleValue = new BigDecimal(rule.getInput()[0]);
          if (ruleValue.compareTo(inputValue) == 0) {
            ruleDtoToApply.add(rule);
          }
        }
        case GT -> {
          ruleValue = new BigDecimal(rule.getInput()[0]);
          if (inputValue.compareTo(ruleValue) > 0) {
            ruleDtoToApply.add(rule);
          }
        }
        case LT -> {
          ruleValue = new BigDecimal(rule.getInput()[0]);
          if (inputValue.compareTo(ruleValue) < 0) {
            ruleDtoToApply.add(rule);
          }
        }
        case NE -> {
          ruleValue = new BigDecimal(rule.getInput()[0]);
          if (ruleValue.compareTo(inputValue) != 0) {
            ruleDtoToApply.add(rule);
          }
        }
        case GTE -> {
          ruleValue = new BigDecimal(rule.getInput()[0]);
          if (inputValue.compareTo(ruleValue) >= 0) {
            ruleDtoToApply.add(rule);
          }
        }
        case LTE -> {
          ruleValue = new BigDecimal(rule.getInput()[0]);
          if (inputValue.compareTo(ruleValue) <= 0) {
            ruleDtoToApply.add(rule);
          }
        }
        case BETWEEN -> {
          String[] range = rule.getInput();
          BigDecimal lowerBound = new BigDecimal(range[0]);
          BigDecimal upperBound = new BigDecimal(range[1]);
          if (inputValue.compareTo(lowerBound) > 0 && inputValue.compareTo(upperBound) < 0) {
            ruleDtoToApply.add(rule);
          }
        }

      }
    }
    applyRules(show, hide, parameter, rulesImpactedByMap, ruleDtoToApply, parameterValue, parameterIdParameterValueView);
  }

  /**
   * This function is a common logic for applying rules on a parameter
   *
   * @param show                          Set of rules to be shown
   * @param hide                          Set of rules to be hidden
   * @param parameter                     Parameter of parameter
   * @param rulesImpactedByMap            Map of rules impacted by a parameter where key is parameterId of impacted parameter and value is set of rules ids and impacting parameter ids
   * @param rules                         List of rules to be applied
   * @param parameterValue
   * @param parameterIdParameterValueView
   */
  private void applyRules(Set<Long> show, Set<Long> hide, Parameter parameter, Map<Long, Set<RuleImpactedByDto>> rulesImpactedByMap, List<RuleDto> rules, ParameterValue parameterValue, Map<Long, ParameterValueViewDto> parameterIdParameterValueView) {
    for (RuleDto rule : rules) {
      if (null != rule.getHide()) {
        rule.getHide().getParameters().stream()
          .map(Long::valueOf).toList()
          .forEach(toHide -> evaluateHiddenImpactedBy(show, hide, parameter, rulesImpactedByMap, rule, toHide, parameterValue, parameterIdParameterValueView));

      }

      if (null != rule.getShow()) {
        rule.getShow().getParameters().stream()
          .map(Long::valueOf).toList()
          .forEach(toShow -> evaluateShowImpactedBy(hide, show, parameter, rulesImpactedByMap, rule, toShow, parameterValue, parameterIdParameterValueView));

      }
    }
  }

  /**
   * This function is used to evaluate impacted by of parameters to be hidden
   *
   * @param show                          Set of rules to be shown
   * @param hide                          Set of rules to be hidden
   * @param parameter                     Parameter id of parameter
   * @param rulesImpactedByMap            Map of rules impacted by a parameter where key is parameterId of impacted parameter and value is set of rules ids and impacting parameter ids
   * @param rule                          Rule to be applied
   * @param toHide                        Parameter id of parameter to be hidden
   * @param parameterValue
   * @param parameterIdParameterValueView
   */
  private void evaluateHiddenImpactedBy(Set<Long> show, Set<Long> hide, Parameter parameter, Map<Long, Set<RuleImpactedByDto>> rulesImpactedByMap, RuleDto rule, Long toHide, ParameterValue parameterValue, Map<Long, ParameterValueViewDto> parameterIdParameterValueView) {
    ParameterValueViewDto toHideParameterValue = parameterIdParameterValueView.get(toHide);

    Set<RuleImpactedByDto> impactedByOfToHideParameter;
    State.TaskExecution taskExecutionStateOfParameterToBeHidden = null;

    boolean isCjfParameter = parameter.getTargetEntityType() == Type.ParameterTargetEntityType.PROCESS;

    // This checks tells that we are applying branching rules for CJF parameters. Since during temp execute call CJF parameters dont have task execution id & parameter values we do this way
    if (!Utility.isEmpty(toHideParameterValue)) {
      if (!isCjfParameter) {
        taskExecutionStateOfParameterToBeHidden = toHideParameterValue.getTaskExecutionState();
      }
      impactedByOfToHideParameter = rulesImpactedByMap.get(toHideParameterValue.getParameterValueId());
    } else {
      impactedByOfToHideParameter = rulesImpactedByMap.get(toHide);
    }


    boolean isCjfParameterOrIsTaskNotStarted = isCjfParameter || State.TaskExecution.NOT_STARTED == taskExecutionStateOfParameterToBeHidden || Objects.equals(parameterValue.getTaskExecutionId(), toHideParameterValue.getTaskExecutionId());


    if (isCjfParameterOrIsTaskNotStarted) {

      if (Utility.isEmpty(impactedByOfToHideParameter)) {
        if (!Utility.isEmpty(toHideParameterValue)) {
          show.remove(toHideParameterValue.getParameterValueId());
          hide.add(toHideParameterValue.getParameterValueId());
        } else {
          show.remove(toHide);
          hide.add(toHide);
        }
        impactedByOfToHideParameter = new HashSet<>();
      }


      if (!Utility.isEmpty(toHideParameterValue)) {
        impactedByOfToHideParameter.add(new RuleImpactedByDto(rule.getId(), parameterValue.getId(), parameter.getId()));
        rulesImpactedByMap.put(toHideParameterValue.getParameterValueId(), impactedByOfToHideParameter);
      } else {
        impactedByOfToHideParameter.add(new RuleImpactedByDto(rule.getId(), null, parameter.getId()));
        rulesImpactedByMap.put(toHide, impactedByOfToHideParameter);
      }

    }


  }

  /**
   * This function is used to evaluate impacted by of parameters to be shown
   *
   * @param hide                          Set of rules to be hidden
   * @param show                          Set of rules to be shown
   * @param parameter                     Parameter id of parameter
   * @param rulesImpactedByMap            Map of rules impacted by a parameter where key is parameterId of impacted parameter and value is set of rules ids and impacting parameter ids
   * @param rule                          Rule to be applied
   * @param toShow                        Parameter id of parameter to be shown
   * @param parameterValue
   * @param parameterIdParameterValueView
   */
  private void evaluateShowImpactedBy(Set<Long> hide, Set<Long> show, Parameter parameter, Map<Long, Set<RuleImpactedByDto>> rulesImpactedByMap, RuleDto rule, Long toShow, ParameterValue parameterValue, Map<Long, ParameterValueViewDto> parameterIdParameterValueView) {
    ParameterValueViewDto toShowParameterValue = parameterIdParameterValueView.get(toShow);


    Set<RuleImpactedByDto> impactedByOfToHideParameter;
    State.TaskExecution taskExecutionStateOfParameterToBeShown = null;

    boolean isCjfParameter = parameter.getTargetEntityType() == Type.ParameterTargetEntityType.PROCESS;


    if (!Utility.isEmpty(toShowParameterValue)) {
      impactedByOfToHideParameter = rulesImpactedByMap.get(toShowParameterValue.getParameterValueId());
      if (!isCjfParameter) {
        taskExecutionStateOfParameterToBeShown = toShowParameterValue.getTaskExecutionState();
      }
    } else {
      impactedByOfToHideParameter = rulesImpactedByMap.get(toShow);
    }
    boolean isCjfParameterOrIsTaskNotStarted = isCjfParameter || State.TaskExecution.NOT_STARTED == taskExecutionStateOfParameterToBeShown || Objects.equals(parameterValue.getTaskExecutionId(), toShowParameterValue.getTaskExecutionId());


    if (isCjfParameterOrIsTaskNotStarted) {
      if (Utility.isEmpty(impactedByOfToHideParameter)) {
        if (!Utility.isEmpty(toShowParameterValue)) {
          hide.remove(toShowParameterValue.getParameterValueId());
          show.add(toShowParameterValue.getParameterValueId());
        } else {
          hide.remove(toShow);
          show.add(toShow);
        }
        impactedByOfToHideParameter = new HashSet<>();
      }

      if (!Utility.isEmpty(toShowParameterValue)) {
        impactedByOfToHideParameter.add(new RuleImpactedByDto(rule.getId(), parameterValue.getId(), parameter.getId()));
        rulesImpactedByMap.put(toShowParameterValue.getParameterValueId(), impactedByOfToHideParameter);
      } else {
        impactedByOfToHideParameter.add(new RuleImpactedByDto(rule.getId(), null, parameter.getId()));
        rulesImpactedByMap.put(toShow, impactedByOfToHideParameter);
      }
    }

  }

  /**
   * This function is used to get rules impacted by a parameter
   *
   * @param parameterRequestList List of CJF parameters to be evaluated
   * @param checklistId
   * @return
   * @throws IOException
   */
  private RuleHideShowDto executeRulesTemporarily(List<ParameterRequest> parameterRequestList, Long checklistId) throws IOException {

    Set<Long> parameterSet = parameterRequestList.stream().map(ParameterRequest::getId).collect(Collectors.toSet());

    Map<Long, Parameter> parameterMap = parameterRepository.findAllById(parameterSet).stream()
      .collect(Collectors.toMap(BaseEntity::getId, Function.identity()));


    Map<Long, ParameterRequest> parameterRequestMap = parameterRequestList.stream()
      .collect(Collectors.toMap(ParameterRequest::getId, Function.identity()));

    List<Parameter> processParameterList = parameterRepository.getParametersByChecklistIdAndTargetEntityType(checklistId, Type.ParameterTargetEntityType.PROCESS);


    Set<Long> show = new HashSet<>();
    Set<Long> hide = new HashSet<>();

    Map<Long, Set<RuleImpactedByDto>> impactedByMap = getDefaultHiddenStateAndImpactedByForCJFParameters(processParameterList, show, hide);


    for (Map.Entry<Long, ParameterRequest> parameterExecuteRequestEntry : parameterRequestMap.entrySet()) {
      Parameter parameter = parameterMap.get(parameterExecuteRequestEntry.getKey());
      JsonNode rules = parameterMap.get(parameterExecuteRequestEntry.getKey()).getRules();


      if (!Utility.isEmpty(rules)) {
        List<RuleDto> parameterRules = JsonUtils.jsonToCollectionType(rules, List.class, RuleDto.class);
        // key is the value of the rule on selection of which the rules should be applied, values is all the rules for that value
        Map<String, List<RuleDto>> rulesMap = parameterRules.stream()
          .collect(Collectors.groupingBy(r -> r.getInput()[0], Collectors.mapping(Function.identity(), Collectors.toList())));


        JsonNode data = parameterExecuteRequestEntry.getValue().getData();
        if (parameter.getType() == Type.Parameter.SINGLE_SELECT) {
          Map<String, String> selectedChoices = new HashMap<>();
          List<ChoiceParameterBase> parameters = JsonUtils.jsonToCollectionType(data, List.class, SingleSelectParameter.class);
          setParameterSelectionState(selectedChoices, parameters);
          applyRulesOfSingleSelectParameter(rulesMap, selectedChoices, show, hide, parameter, impactedByMap, null, new HashMap<>());

        } else if (parameter.getType() == Type.Parameter.RESOURCE) {
          // TODO Bring consistencies in temporary and actual execution API calls
          applyRulesOfResourceParameter(rulesMap, data.get("choices"), show, hide, parameter, impactedByMap, null, new HashMap<>());

        } else if (parameter.getType() == Type.Parameter.NUMBER) {
          String value = data.get("input").asText();
          applyRulesOfNumberParameter(parameterRules, value, show, hide, parameter, impactedByMap, null, new HashMap<>());
        }

      }


    }
    RuleHideShowDto ruleHideShowDto = new RuleHideShowDto();
    ruleHideShowDto.setHide(hide.stream().map(String::valueOf).collect(Collectors.toSet()));
    ruleHideShowDto.setShow(show.stream().map(String::valueOf).collect(Collectors.toSet()));

    return ruleHideShowDto;
  }

  /**
   * This function calculated default hidden state of CJF parameters and calculates impacted by of CJF parameters
   *
   * @param parameterList List of CJF parameters
   * @param show          Set of parameters to be shown
   * @param hide          Set of parameters to be hidden
   * @return
   */
  private static Map<Long, Set<RuleImpactedByDto>> getDefaultHiddenStateAndImpactedByForCJFParameters(List<Parameter> parameterList, Set<Long> show, Set<Long> hide) {

    Map<Long, Set<RuleImpactedByDto>> impactedByMap = new HashMap<>();

    parameterList.forEach(parameter -> {
      if (parameter.isHidden()) {
        hide.add(parameter.getId());
      } else {
        show.add(parameter.getId());
      }
      impactedByMap.put(parameter.getId(), new HashSet<>());
    });


    return impactedByMap;
  }

  /**
   * This function stores selected choices states of item selection parameter in a map
   *
   * @param parameterChoices Map of parameter id and selected choice
   * @param parameters       List of single select parameters
   * @return
   */
  private void setParameterSelectionState(Map<String, String> parameterChoices, List<ChoiceParameterBase> parameters) {
    for (ChoiceParameterBase choiceParameter : parameters) {
      String id = choiceParameter.getId();
      String state = choiceParameter.getState();
      if (null != state) {
        if (State.Selection.SELECTED.equals(State.Selection.valueOf(state))) {
          parameterChoices.put(id, State.Selection.SELECTED.name());
        } else {
          parameterChoices.put(id, State.Selection.NOT_SELECTED.name());
        }
      }
    }
  }

}
