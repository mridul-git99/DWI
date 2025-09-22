package com.leucine.streem.dto.projection.impl;

import com.leucine.streem.dto.projection.TrainedUsersView;

public class TrainedUsersViewImpl implements TrainedUsersView {

  private String id;
  private String userid;
  private String usergroupid;
  private String taskid;
  private String usergroupname;
  private String usergroupdescription;
  private String employeeid;
  private String firstname;
  private String lastname;
  private String emailid;

  // Constructor
  public TrainedUsersViewImpl(String userid, String usergroupid, String taskid, String usergroupname,
                              String usergroupdescription, String employeeid, String firstname,
                              String lastname, String emailid) {
    this.userid = userid;
    this.usergroupid = usergroupid;
    this.taskid = taskid;
    this.usergroupname = usergroupname;
    this.usergroupdescription = usergroupdescription;
    this.employeeid = employeeid;
    this.firstname = firstname;
    this.lastname = lastname;
    this.emailid = emailid;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getUserId() {
    return userid;
  }

  @Override
  public String getUserGroupId() {
    return usergroupid;
  }

  @Override
  public String getTaskId() {
    return taskid;
  }

  @Override
  public String getUserGroupName() {
    return usergroupname;
  }

  @Override
  public String getUserGroupDescription() {
    return usergroupdescription;
  }

  @Override
  public String getEmployeeId() {
    return employeeid;
  }

  @Override
  public String getFirstName() {
    return firstname;
  }

  @Override
  public String getLastName() {
    return lastname;
  }

  @Override
  public String getEmailId() {
    return emailid;
  }

  // Implement other interface methods
  @Override
  public boolean getStatus() {
    // Assuming you need to calculate or determine this somehow, otherwise return a default value
    return true; // Or implement as needed
  }

  @Override
  public String getLowerFirstName() {
    return firstname != null ? firstname.toLowerCase() : null;
  }

  @Override
  public String getLowerLastName() {
    return lastname != null ? lastname.toLowerCase() : null;
  }

  @Override
  public String getLowerUserGroupName() {
    return usergroupname != null ? usergroupname.toLowerCase() : null;
  }
}

