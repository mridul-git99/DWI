package com.leucine.streem.dto;

import com.leucine.streem.constant.State;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.projection.ParameterValueView;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParameterValueViewDto implements ParameterValueView {
  private Long parameterId;
  private Long id;
  private String value;
  private String choices;
  private String data;
  private String reason;
  private Type.Parameter type;
  private Long mediaId;
  private State.ParameterExecution state;
  private State.TaskExecution taskExecutionState;
  private String validations;
  private String impactedBy;
  private boolean hasExceptions;
  private boolean hidden;
  private boolean hasCorrections;
  private Long taskId;
  private String label;
  private Long taskExecutionId;
  private Long parameterValueId;
  private Boolean hasActiveException;

  // Only override the boolean getters that would have naming conflicts
  @Override
  public Boolean getHasExceptions() {
    return hasExceptions;
  }

  @Override
  public Boolean getHidden() {
    return hidden;
  }

  @Override
  public Boolean getHasCorrections() {
    return hasCorrections;
  }

  // Factory method to create from a ParameterValueView
  public static ParameterValueViewDto fromView(ParameterValueView view) {
    return ParameterValueViewDto.builder()
      .parameterId(view.getParameterId())
      .id(view.getId())
      .value(view.getValue())
      .choices(view.getChoices())
      .data(view.getData())
      .reason(view.getReason())
      .type(view.getType())
      .mediaId(view.getMediaId())
      .state(view.getState())
      .taskExecutionState(view.getTaskExecutionState())
      .validations(view.getValidations())
      .impactedBy(view.getImpactedBy())
      .hasExceptions(Boolean.TRUE.equals(view.getHasExceptions()))
      .hidden(Boolean.TRUE.equals(view.getHidden()))
      .hasCorrections(Boolean.TRUE.equals(view.getHasCorrections()))
      .taskId(view.getTaskId())
      .label(view.getLabel())
      .taskExecutionId(view.getTaskExecutionId())
      .parameterValueId(view.getParameterValueId())
      .hasActiveException(view.getHasActiveException())
      .build();
  }

  // Factory method to convert a list of ParameterValueView objects to ParameterValueDTOs
  public static List<ParameterValueViewDto> fromViewList(List<ParameterValueView> views) {
    if (views == null) {
      return Collections.emptyList();
    }
    return views.stream()
      .map(ParameterValueViewDto::fromView)
      .collect(Collectors.toList());
  }
}
