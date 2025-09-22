package com.leucine.streem.dto.projection;

public interface TaskExecutionAssigneeDetailsView {
  String getUserId();
  String getFirstName();
  String getLastName();
  String getEmployeeId();
  boolean getCompletelyAssigned();
  int getAssignedTasks();
  int getSignedOffTasks();
  int getPendingSignOffs();
  String getUserGroupId();
  String getUserGroupName();
}
