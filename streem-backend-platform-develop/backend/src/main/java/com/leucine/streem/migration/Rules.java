package com.leucine.streem.migration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.RuleDto;
import com.leucine.streem.dto.RuleEntityIdDto;
import com.leucine.streem.model.Parameter;
import com.leucine.streem.model.ParameterRule;
import com.leucine.streem.model.ParameterRuleMapping;
import com.leucine.streem.model.compositekey.ParameterRuleMappingCompositeKey;
import com.leucine.streem.repository.IParameterRepository;
import com.leucine.streem.repository.IParameterRuleMappingRepository;
import com.leucine.streem.repository.IParameterRuleRepository;
import com.leucine.streem.util.IdGenerator;
import com.leucine.streem.util.JsonUtils;
import com.leucine.streem.util.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class Rules implements Serializable {
  @Serial
  private static final long serialVersionUID = 3375793751172478618L;

  private final IParameterRepository parameterRepository;
  private final IParameterRuleRepository parameterRuleRepository;
  private final IParameterRuleMappingRepository parameterRuleMappingRepository;

  public BasicDto execute() {
    List<Parameter> parameters = parameterRepository.findAll();

    for (Parameter parameter : parameters) {
      try {
        setParameterRules(parameter.getRules(), parameter);

      } catch (Exception e) {
        log.error("Error performing rules migration for parameter id: {}, with rules: {}", parameter.getId(), parameter.getRules());
      }
    }
    return new BasicDto(null, "Success", null);
  }
  private void setParameterRules(JsonNode rules, Parameter parameter) throws JsonProcessingException {
    List<RuleDto> ruleDtos = JsonUtils.readValue(rules.toString(),
      new TypeReference<>() {
      });

    List<ParameterRuleMapping> parameterRuleMappings = new ArrayList<>();

    parameterRuleMappingRepository.deleteAllByTriggeringParameterId(parameter.getId());

    for (RuleDto ruleDto : ruleDtos) {
      boolean visibility = !Utility.isEmpty(ruleDto.getShow());
      ParameterRule parameterRule = new ParameterRule();
      parameterRule.setId(IdGenerator.getInstance().nextId());
      parameterRule.setRuleId(ruleDto.getId());
      parameterRule.setOperator(String.valueOf(ruleDto.getConstraint()));
      parameterRule.setInput(ruleDto.getInput());

      List<Parameter> impactedParameters;

      if (visibility) {
        impactedParameters = parameterRepository.findAllById(
          ruleDto.getShow()
            .getParameters()
            .stream()
            .map(Long::valueOf)
            .collect(Collectors.toSet())
        );
      } else {
        impactedParameters = parameterRepository.findAllById(
          ruleDto.getHide()
            .getParameters()
            .stream()
            .map(Long::valueOf)
            .collect(Collectors.toSet())
        );
      }
      parameterRule.setVisibility(visibility);

      parameterRule = parameterRuleRepository.save(parameterRule);
      for (Parameter impactedParameter : impactedParameters) {
        log.info("[setParameterRules] impactedParameter: {}", impactedParameter.getId());
        ParameterRuleMappingCompositeKey parameterRuleMappingCompositeKey = new ParameterRuleMappingCompositeKey();
        parameterRuleMappingCompositeKey.setImpactedParameterId(impactedParameter.getId());
        parameterRuleMappingCompositeKey.setParameterRuleId(parameterRule.getId());
        parameterRuleMappingCompositeKey.setTriggeringParameterId(parameter.getId());
        parameterRuleMappings.add(new ParameterRuleMapping(parameterRuleMappingCompositeKey, parameterRule, impactedParameter, parameter));
      }
      parameterRuleMappingRepository.saveAll(parameterRuleMappings);

    }
  }

  public BasicDto fixChecklistId(String checklistId) {
    List<Parameter> parameters = parameterRepository.findAllParametersWithRulesForChecklistId(Long.valueOf(checklistId));
    for (Parameter parameter : parameters) {
      try {
        fixRulesFor(parameter);
      } catch (Exception e) {
        log.error("Error performing rules migration for parameter id: {}, with rules: {}: message: {}", parameter.getId(), parameter.getRules(), e.getMessage());
      }
    }
    return new BasicDto(null, "Success", null);
  }

  private void fixRulesFor(Parameter parameter) throws JsonProcessingException {
    List<RuleDto> ruleDtos = JsonUtils.readValue(parameter.getRules().toString(), new TypeReference<>() {
    });
    for (RuleDto ruleDto : ruleDtos) {
      if (!Utility.isEmpty(ruleDto.getShow())) {
        List<String> show = new ArrayList<>();
        for (String parameterId : ruleDto.getShow().getParameters()) {
          if (!parameterId.equals("null") && parameterRepository.existsByIdAndChecklistId(Long.valueOf(parameterId), parameter.getChecklistId())) {
            show.add(parameterId);
          } else {
            log.info("Parameter with id: {} does not exist for checklist id: {}", parameterId, parameter.getChecklistId());
          }
        }
        ruleDto.setShow(new RuleEntityIdDto(new ArrayList<>(), new ArrayList<>(), show));
      }
      if (!Utility.isEmpty(ruleDto.getHide())) {
        List<String> hide = new ArrayList<>();
        for (String parameterId : ruleDto.getHide().getParameters()) {
          if (!parameterId.equals("null") && parameterRepository.existsByIdAndChecklistId(Long.valueOf(parameterId), parameter.getChecklistId())) {
            hide.add(parameterId);
          } else {
            log.info("Parameter with id: {} does not exist for checklist id: {}", parameterId, parameter.getChecklistId());
          }
        }
        ruleDto.setHide(new RuleEntityIdDto(new ArrayList<>(), new ArrayList<>(), hide));
      }
    }
    parameter.setRules(JsonUtils.valueToNode(ruleDtos));
    parameterRepository.save(parameter);
  }
}
