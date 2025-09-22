package com.leucine.streem.dto;

import com.leucine.streem.constant.State;
import com.leucine.streem.constant.Type;

public interface TaskExecutionLiteView {
  String getId();
  int getOrderTree();
  State.TaskExecution getState();
  Type.TaskExecutionType getType();
  String getTaskId();
  boolean getHidden();
}
