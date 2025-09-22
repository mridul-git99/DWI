package com.leucine.streem.constant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class Misc {
  public static final String RELATIVE_FILE_PATH = "medias";
  public static final List<String> AUTHOR_ROLES = List.of("ACCOUNT_OWNER", "FACILITY_ADMIN", "SUPERVISOR", "PROCESS_PUBLISHER");
  public static final List<String> GLOBAL_AUTHOR_ROLES = List.of("GLOBAL_ADMIN", "ACCOUNT_OWNER");
  public static final List<String> TASK_ROLES = List.of("ACCOUNT_OWNER", "FACILITY_ADMIN", "SUPERVISOR", "OPERATOR", "PROCESS_PUBLISHER");
  public static final List<String> REVIEWERS_ROLES = List.of("ACCOUNT_OWNER", "FACILITY_ADMIN", "SUPERVISOR", "PROCESS_PUBLISHER");
  public static final List<String> GLOBAL_REVIEWERS_ROLES = List.of("GLOBAL_ADMIN", "ACCOUNT_OWNER");
  public static final Set<String> SHOULD_BE_PARAMETER_REVIEWER = Set.of("SUPERVISOR", "PROCESS_PUBLISHER", "FACILITY_ADMIN");
  public static final String SUPERVISOR_ROLE = "SUPERVISOR";
  public static final String ACCOUNT_OWNER = "ACCOUNT_OWNER";
  public static final String PROCESS_PUBLISHER_ROLE = "PROCESS_PUBLISHER";
  public static final String FACILITY_ADMIN_ROLE = "FACILITY_ADMIN";
  public static final Long ALL_FACILITY_ID = -1L;
  public static final List<String> CHECKLIST_ARCHIVAL_ROLES = List.of("ACCOUNT_OWNER", "FACILITY_ADMIN", "PROCESS_PUBLISHER");
  public static final List<String> GLOBAL_CHECKLIST_ARCHIVAL_ROLES = List.of("ACCOUNT_OWNER", "GLOBAL_ADMIN");
  public static final String RELATION_TARGET_PATH = "/objects/partial?collection=";

  public static final String CREATED_AT = "Created At";
  public static final String CREATED_BY = "Created By";
  public static final String UPDATED_AT = "Updated At";
  public static final String UPDATED_BY = "Updated By";
  public static final String USAGE_STATUS = "Usage Status";

  public static final String CREATED_AT_EXTERNAL_ID = "createdAt";
  public static final String CREATED_BY_EXTERNAL_ID = "createdBy";
  public static final String UPDATED_AT_EXTERNAL_ID = "updatedAt";
  public static final String UPDATED_BY_EXTERNAL_ID = "updatedBy";
  public static final String USAGE_STATUS_EXTERNAL_ID = "usageStatus";

  public static final Set<String> CREATE_PROPERTIES = Set.of(CREATED_AT_EXTERNAL_ID, CREATED_BY_EXTERNAL_ID, UPDATED_AT_EXTERNAL_ID, UPDATED_BY_EXTERNAL_ID, USAGE_STATUS_EXTERNAL_ID);
  public static final Set<String> UPDATE_PROPERTIES = Set.of(UPDATED_AT_EXTERNAL_ID, UPDATED_BY_EXTERNAL_ID);

  public static final String CREATED_AT_TIME = "created at time";
  public static final String UPDATED_AT_TIME = "updated at time";
  public static final String CREATED_BY_USER = "created by user";
  public static final String UPDATED_BY_USER = "updated by user";
  public static final String USAGE_STATUS_PLACE_HOLDER = "active or deprecated";

  public static final String CHANGED_AS_PER_PROCESS = "Changed as per Process: ";

  public enum UserType {
    LOCAL, AZURE, OKTA, SYSTEM
  }

  public static final String VARIABLE_NAME_REGEX = "^[a-zA-Z_][a-zA-Z_0-9]{0,45}$";
  public static final String SYSTEM_USER_ID = "1";
  public static final String SYSTEM_USER_EMPLOYEE_ID = "SYSTEM";
  public static final String SYSTEM_USER_FIRST_NAME = "System";
  public static final String STRING_CHAR_REGEX = "^[A-Za-z0-9\\s_-]{1,255}$";

  public static final List<String> ASSIGNEE_ROLES = List.of("ACCOUNT_OWNER", "FACILITY_ADMIN", "SUPERVISOR", "PROCESS_PUBLISHER");
  public static final Set<String> MATERIAL_PARAMETER_EXTENSION_TYPES = Set.of("png", "jpeg", "jpg");
  public static final Set<String> PROCESS_RECALL_ROLES_FOR_FACILITY = Set.of("ACCOUNT_OWNER", "FACILITY_ADMIN", "PROCESS_PUBLISHER");
  public static final Set<String> PROCESS_RECALL_ROLES_FOR_GLOBAL_FACILITY = Set.of("GLOBAL_ADMIN", "ACCOUNT_OWNER");
  public static final Set<State.ParameterExecution> PARAMETER_EXECUTABLE_STATES = Set.of(State.ParameterExecution.EXECUTED, State.ParameterExecution.BEING_EXECUTED, State.ParameterExecution.BEING_EXECUTED_AFTER_REJECTED);
  public static final Set<State.TaskExecution> TASK_EXECUTION_EXECUTABLE_STATES = Set.of(State.TaskExecution.IN_PROGRESS, State.TaskExecution.PAUSED);
  public static final Set<String> TASK_EXECUTION_PENDING_STATES = Set.of(State.TaskExecution.IN_PROGRESS.toString(), State.TaskExecution.PAUSED.toString(), State.TaskExecution.NOT_STARTED.toString());
  public static final Set<String> JOB_PENDING_STATES = Set.of(State.Job.ASSIGNED.toString(), State.Job.IN_PROGRESS.toString());
  public static final Set<State.TaskExecution> TASK_EXECUTION_PENDING_STATES_ENUMS = Set.of(State.TaskExecution.IN_PROGRESS, State.TaskExecution.PAUSED, State.TaskExecution.NOT_STARTED);
  public static final Set<State.Job> JOB_PENDING_STATES_ENUMS = Set.of(State.Job.ASSIGNED, State.Job.IN_PROGRESS);

  public static final Set<State.TaskExecution> TASK_DEPENDENCY_ACCEPTABLE_STATES = Set.of(State.TaskExecution.COMPLETED, State.TaskExecution.COMPLETED_WITH_EXCEPTION, State.TaskExecution.SKIPPED);
  public static final Set<Type.TaskExecutionType> TASK_DEPENDENCY_ACCEPTABLE_TYPES = Set.of(Type.TaskExecutionType.MASTER, Type.TaskExecutionType.RECURRING);
  public static final List<String> USER_GROUP_ROLES = List.of("SYSTEM_ADMIN", "GLOBAL_ADMIN");

  public static final List<State.ChecklistCollaborator> stateList = List.of(State.ChecklistCollaborator.NOT_STARTED, State.ChecklistCollaborator.NOT_STARTED, State.ChecklistCollaborator.NOT_STARTED, State.ChecklistCollaborator.COMMENTED_OK, State.ChecklistCollaborator.REQUESTED_NO_CHANGES, State.ChecklistCollaborator.SIGNED, State.ChecklistCollaborator.SIGNED, State.ChecklistCollaborator.SIGNED);
  public static final List<Type.Collaborator> typeList = List.of(Type.Collaborator.PRIMARY_AUTHOR, Type.Collaborator.REVIEWER, Type.Collaborator.REVIEWER, Type.Collaborator.REVIEWER, Type.Collaborator.REVIEWER, Type.Collaborator.SIGN_OFF_USER, Type.Collaborator.SIGN_OFF_USER, Type.Collaborator.SIGN_OFF_USER);
  public static final List<Integer> phaseList = List.of(1, 1, 1, 2, 2, 1, 1, 1);
  public static final List<Integer> orderTreeList = List.of(1, 1, 1, 1, 1, 1, 2, 3);
  public static final List<State.ChecklistCollaboratorPhaseType> phaseTypeList = List.of(State.ChecklistCollaboratorPhaseType.BUILD, State.ChecklistCollaboratorPhaseType.REVIEW, State.ChecklistCollaboratorPhaseType.REVIEW, State.ChecklistCollaboratorPhaseType.REVIEW, State.ChecklistCollaboratorPhaseType.REVIEW, State.ChecklistCollaboratorPhaseType.SIGN_OFF, State.ChecklistCollaboratorPhaseType.SIGN_OFF, State.ChecklistCollaboratorPhaseType.SIGN_OFF);
}
