package com.leucine.streem.dto.projection;

public interface TaskExecutionAssigneeView {
  String getUserId();
  String getFirstName();
  String getLastName();
  String getEmployeeId();
  boolean getCompletelyAssigned();
  int getAssignedTasks();
  String getUserGroupId();
  String getUserGroupName();

}
