package com.leucine.streem.constant;

public final class Email {
  private Email() {
  }

  /* Attributes */
  public static final String ATTRIBUTE_LOGIN_URL = "streemlogin";
  public static final String ATTRIBUTE_JOB = "job";
  public static final String ATTRIBUTE_CHECKLIST = "checklist";

  public static final String ATTRIBUTE_USER_NAME = "UserName";
  public static final String ATTRIBUTE_INITIATOR_NAME = "InitiatorName";
  public static final String ATTRIBUTE_INITIATOR_DESCRIPTION = "InitiatorDescription";


//TODO: clean up all attributes value (should use camel not pascal case)
  public static final String ATTRIBUTE_JOB_ID = "JobId";
  public static final String ATTRIBUTE_JOB_LINK = "JobLink";
  public static final String ATTRIBUTE_JOB_CODE = "JobCode";

  public static final String ATTRIBUTE_PARAMETER = "Parameter";

  public static final String ATTRIBUTE_TASK_NAME = "TaskName";
  public static final String ATTRIBUTE_PROCESS_NAME = "ProcessName";
  public static final String ATTRIBUTE_TASK_LINK = "TaskLink";
  public static final String ATTRIBUTE_CORRECTOR_NAME = "CorrectorName";
  public static final String ATTRIBUTE_CORRECTOR_DESCRIPTION = "CorrectorDescription";
  public static final String ATTRIBUTE_PREREQUISITE_TASKS = "PreRequisiteTaskNames";

  public static final String ATTRIBUTE_REVIEWER_NAME = "ReviewerName";
  public static final String ATTRIBUTE_REVIEWER_DESCRIPTION = "ReviewerDescription";
  public static final String ATTRIBUTE_GROUP_LINK = "GroupLink";
  public static final String ATTRIBUTE_USER_GROUP_NAME = "UserGroupName";
  public static final String ATTRIBUTE_USER_GROUP_ARCHIVAL_REASON = "UserGroupArchivalReason";
  public static final String ATTRIBUTE_SYSTEM_ADMIN = "SystemAdmin";
  public static final String ATTRIBUTE_SINGLE_USER_REMOVAL_REASON = "SingleUserRemovalReason";
  public static final String ATTRIBUTE_TASKS = "Tasks";


  /* Template */
  public static final String TEMPLATE_USER_ASSIGNED_TO_JOB = "USER_ASSIGNED_TO_JOB";
  public static final String TEMPLATE_USER_UNASSIGNED_FROM_JOB = "USER_UNASSIGNED_FROM_JOB";
  public static final String TEMPLATE_PARAMETER_APPROVAL_REQUEST = "APPROVAl_REQUEST";
  public static final String TEMPLATE_REVIEWER_ASSIGNED_TO_CHECKLIST = "REVIEWER_ASSIGNED_TO_CHECKLIST";
  public static final String TEMPLATE_REVIEWER_UNASSIGNED_FROM_CHECKLIST = "REVIEWER_UNASSIGNED_FROM_CHECKLIST";
  public static final String TEMPLATE_PROTOTYPE_READY_FOR_SIGNING = "PROTOTYPE_READY_FOR_SIGNING";
  public static final String TEMPLATE_PROTOTYPE_SIGNING_REQUEST = "PROTOTYPE_SIGNING_REQUEST";
  public static final String TEMPLATE_PROTOTYPE_REQUESTED_CHANGES = "PROTOTYPE_REQUESTED_CHANGES";
  public static final String TEMPLATE_REVIEW_SUBMIT_REQUEST = "PROTOTYPE_REVIEW_SUBMIT_REQUEST";
  public static final String TEMPLATE_AUTHOR_CHECKLIST_CONFIGURATION = "AUTHOR_CHECKLIST_CONFIGURATION";
  public static final String TEMPLATE_PEER_VERIFICATION = "TEMPLATE_PEER_VERIFICATION";
  public static final String TEMPLATE_JOB_START_DUE = "TEMPLATE_JOB_START_DUE";
  public static final String TEMPLATE_JOB_OVER_DUE = "TEMPLATE_JOB_OVER_DUE";
  public static final String TEMPLATE_CORRECTION_REQUESTED = "CORRECTION_REQUESTED";
  public static final String TEMPLATE_APPROVAL_REQUESTED = "APPROVAL_REQUESTED";
  public static final String TEMPLATE_CORRECTION_APPROVED = "CORRECTION_APPROVED";
  public static final String TEMPLATE_CORRECTION_REJECTED = "CORRECTION_REJECTED";
  public static final String TEMPLATE_EXCEPTION_REQUESTED = "PARAMETER_EXCEPTION_REQUESTED";
  public static final String TEMPLATE_EXCEPTION_APPROVED = "PARAMETER_EXCEPTION_APPROVED";
  public static final String TEMPLATE_EXCEPTION_REJECTED = "PARAMETER_EXCEPTION_REJECTED";
  public static final String TEMPLATE_ADDED_TO_USER_GROUP = "ADDED_TO_USER_GROUP";
  public static final String TEMPLATE_REMOVED_FROM_USER_GROUP = "REMOVED_FROM_USER_GROUP";
  public static final String TEMPLATE_USER_GROUP_ARCHIVED = "USER_GROUP_ARCHIVED";
  public static final String TEMPLATE_DEPENDANT_TASK_READY_TO_START = "DEPENDANT_TASK_READY_TO_START";

  /* New Subject */
  public static final String SUBJECT_CORRECTION_REQUESTED = "Correction Requested for a Parameter";
  public static final String SUBJECT_APPROVAL_REQUESTED = "Approval Requested for a Parameter Correction";
  public static final String SUBJECT_CORRECTION_APPROVED = "Parameter Correction Approved";
  public static final String SUBJECT_CORRECTION_REJECTED = "Parameter Correction Rejected";
  public static final String SUBJECT_EXCEPTION_APPROVAL_REQUESTED = "Approval Requested for a Parameter Exception";
  public static final String SUBJECT_EXCEPTION_APPROVED = "Parameter Exception Approved";
  public static final String SUBJECT_EXCEPTION_REJECTED = "Parameter Exception Rejected";
  public static final String SUBJECT_ADDED_TO_USER_GROUP = "Welcome to the User Group!";
  public static final String SUBJECT_REMOVED_FROM_USER_GROUP = "User Group Membership Update";
  public static final String SUBJECT_USER_GROUP_ARCHIVED = "Important Update: [User Group Name] is Now Archived";

  /* Subject */
  public static final String SUBJECT_USER_ASSIGNED_TO_JOB = "You are assigned to a Job";
  public static final String SUBJECT_USER_UNASSIGNED_FROM_JOB = "You are unassigned from a Job";
  public static final String SUBJECT_PARAMETER_APPROVAL_REQUEST = "A Job needs your Approval for a Deviation";
  public static final String SUBJECT_REVIEWER_ASSIGNED_TO_CHECKLIST = "A Prototype has been submitted for your Review";
  public static final String SUBJECT_REVIEWER_UNASSIGNED_FROM_CHECKLIST = "You are unassigned from Checklist";
  public static final String SUBJECT_PROTOTYPE_READY_FOR_SIGNING = "A Prototype is Ready for Signing";
  public static final String SUBJECT_PROTOTYPE_SIGNING_REQUEST = "A Prototype needs your sign";
  public static final String SUBJECT_PROTOTYPE_REQUESTED_CHANGES = "Modification requested by Reviewers";
  public static final String SUBJECT_REVIEW_SUBMIT_REQUEST = "Finish your Review";
  public static final String SUBJECT_AUTHOR_CHECKLIST_CONFIGURATION = "Configure a Checklist";
  public static final String SUBJECT_PEER_VERIFICATION = "A Job needs your Verification";
  public static final String SUBJECT_TEMPLATE_JOB_START_DUE = "A Scheduled Job is now due to be started";
  public static final String SUBJECT_TEMPLATE_JOB_OVER_DUE = "A Scheduled Job is now overdue";
  public static final String PREREQUISITE_TASKS_COMPLETED = "Prerequisite Tasks Completed - Your Task is Ready for Execution";


}
