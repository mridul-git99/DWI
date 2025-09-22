package com.leucine.streem.dto.projection;

public interface TaskAssigneeView {
  String getUserId();
  String getFirstName();
  String getLastName();
  String getEmployeeId();
  boolean getCompletelyAssigned();
  int getAssignedTasks();
  String getUserGroupId();
  String getUserGroupName();
}
