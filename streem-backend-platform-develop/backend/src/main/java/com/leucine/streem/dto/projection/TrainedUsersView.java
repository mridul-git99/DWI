package com.leucine.streem.dto.projection;

public interface TrainedUsersView {
  String getId();
  String getUserId();

  String getEmployeeId();

  String getFirstName();

  String getLastName();
  String getEmailId();

  String getUserGroupId();

  String getUserGroupName();

  String getUserGroupDescription();
  String getTaskId();

  boolean getStatus();
  String getLowerFirstName();
  String getLowerLastName();
  String getLowerUserGroupName();
}
