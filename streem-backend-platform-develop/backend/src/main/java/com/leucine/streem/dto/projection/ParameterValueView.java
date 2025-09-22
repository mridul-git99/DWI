package com.leucine.streem.dto.projection;

import com.leucine.streem.constant.State;
import com.leucine.streem.constant.Type;

public interface ParameterValueView {
  Long getParameterId();

  Long getId();

  String getValue();

  String getChoices();

  String getData();

  String getReason();

  Type.Parameter getType();

  Long getMediaId();

  State.ParameterExecution getState();

  State.TaskExecution getTaskExecutionState();

  String getValidations();

  String getImpactedBy();

  Boolean getHasExceptions();

  Boolean getHidden();

  Boolean getHasCorrections();

  Long getTaskId();

  String getLabel();

  Long getTaskExecutionId();
  Long getParameterValueId();
  Boolean getHasActiveException();
}
