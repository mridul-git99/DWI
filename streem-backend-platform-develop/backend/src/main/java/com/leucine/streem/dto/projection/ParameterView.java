package com.leucine.streem.dto.projection;

public interface ParameterView {
  Long getParameterId();
  boolean getCorrectionEnabled();
  boolean getHidden();
  Long getTaskId();

  String getData();
  Long getChecklistId();

}
