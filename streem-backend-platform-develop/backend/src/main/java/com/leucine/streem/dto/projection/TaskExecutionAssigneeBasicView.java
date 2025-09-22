package com.leucine.streem.dto.projection;

import com.leucine.streem.constant.State;

public interface TaskExecutionAssigneeBasicView {
  String getUserId();
  String getFirstName();
  String getLastName();
  String getEmployeeId();
  String getEmail();
  String getTaskExecutionId();
  State.TaskExecutionAssignee getAssigneeState();
  boolean getIsActionPerformed();
  String getUserGroupId();
  String getUserGroupName();
  String getUserGroupDescription();
  String getUserName();
}
