package com.leucine.streem.constant;

import lombok.Getter;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class Type {

  public static final Set<Parameter> NON_EXECUTABLE_PARAMETER_TYPES = Collections.unmodifiableSet(EnumSet.of(Parameter.MATERIAL, Parameter.INSTRUCTION));

  public static final Set<Parameter> CHOICE_PARAMETER_TYPES = Collections.unmodifiableSet(EnumSet.of(Parameter.MULTISELECT, Parameter.SINGLE_SELECT, Parameter.CHECKLIST, Parameter.YES_NO, Parameter.MULTI_RESOURCE, Parameter.RESOURCE));

  public static final Set<Collaborator> AUTHOR_TYPES = Collections.unmodifiableSet(EnumSet.of(Collaborator.AUTHOR, Collaborator.PRIMARY_AUTHOR));

  public static final Set<Parameter> ALLOWED_PARAMETER_TYPES_FOR_CALCULATION_PARAMETER = Collections.unmodifiableSet(EnumSet.of(Parameter.CALCULATION, Parameter.NUMBER, Parameter.SHOULD_BE));

  public static final Set<Parameter> ALLOWED_PARAMETER_TYPES_NUMBER_PARAMETER_VALIDATION = Collections.unmodifiableSet(EnumSet.of(Parameter.RESOURCE));

  public static final Set<Parameter> PARAMETER_MEDIA_TYPES = Collections.unmodifiableSet(EnumSet.of(Parameter.MEDIA, Parameter.FILE_UPLOAD, Parameter.SIGNATURE));

  public static final Set<Type.AutomationActionType> ACTION_TYPES_WITH_NO_REFERENCED_PARAMETER_ID = Set.of(Type.AutomationActionType.ARCHIVE_OBJECT, Type.AutomationActionType.CREATE_OBJECT);

  public static final Set<Type.VerificationType> VERIFICATION_TYPES = Collections.unmodifiableSet(EnumSet.of(com.leucine.streem.constant.Type.VerificationType.SELF, com.leucine.streem.constant.Type.VerificationType.PEER, com.leucine.streem.constant.Type.VerificationType.BOTH));

  public static final Set<Parameter> RESOURCE_PARAMETER_TYPES = Collections.unmodifiableSet(EnumSet.of(Parameter.MULTI_RESOURCE, Parameter.RESOURCE));

  public static final Set<Parameter> SELECT_PARAMETER_TYPES = Collections.unmodifiableSet(EnumSet.of(Parameter.MULTISELECT, Parameter.SINGLE_SELECT,Parameter.CHECKLIST, Parameter.YES_NO));

  public enum PropertyType {
    CHECKLIST,
    JOB
  }

  public enum VerificationType {
    NONE,
    SELF,
    PEER,
    BOTH,
  }

  public enum Parameter {
    CALCULATION,
    CHECKLIST,
    DATE,
    DATE_TIME,
    INSTRUCTION,
    MATERIAL,
    MEDIA,
    FILE_UPLOAD,
    MULTISELECT,
    NUMBER,
    SHOULD_BE,
    RESOURCE,
    MULTI_RESOURCE,
    SINGLE_SELECT,
    SINGLE_LINE,
    SIGNATURE,
    MULTI_LINE,
    YES_NO
  }

  public enum EntityType {
    @Deprecated
    ACTIVITY("ACT"),
    CHECKLIST("CHK"),
    SCHEDULER("SCH"),
    JOB("JOB"),
    TASK("TSK"),
    CORRECTION("CORR"),
    STAGE("STG"),
    PARAMETER_EXCEPTION("XCT"),
    JOB_ANNOTATION("ANO"),
    ACTION("ACTN");


    private final String code;

    EntityType(String code) {
      this.code = code;
    }

    public String getCode() {
      return code;
    }
  }

  public enum Collaborator {
    PRIMARY_AUTHOR,
    AUTHOR,
    REVIEWER,
    SIGN_OFF_USER,
  }

  public enum TaskException {
    COMPLETED_WITH_EXCEPTION,
    ERROR_CORRECTION,
    PARAMETER_DEVIATION,
    SKIPPED,
    DURATION_EXCEPTION,
    YES_NO
  }

  public enum JobLogColumnType {
    DATE_TIME("Date Time"),
    DATE("Date"),
    TEXT("Text"),
    FILE("File");

    private final String label;

    JobLogColumnType(String label) {
      this.label = label;
    }

    public String get() {
      return label;
    }
  }

  public enum JobLogTriggerType {
    CHK_ID, // common
    JOB_ID, // common
    JOB_STATE, // common
    CHK_NAME, // common
    PARAMETER_VALUE,
    PROCESS_PARAMETER_VALUE,
    RELATION_VALUE,
    RESOURCE_PARAMETER,
    JOB_PROPERTY_VALUE,
    CHK_PROPERTY_VALUE,
    JOB_START_TIME, // common
    TSK_START_TIME,
    TSK_END_TIME,
    JOB_END_TIME, // common
    JOB_CREATED_AT, // common
    JOB_CREATED_BY, // common
    JOB_MODIFIED_BY, // common
    JOB_STARTED_BY, // common
    JOB_ENDED_BY, // common
    TSK_STARTED_BY, // common
    TSK_ENDED_BY, // common
    RESOURCE,
    PARAMETER_SELF_VERIFIED_BY,
    PARAMETER_SELF_VERIFIED_AT,
    PARAMETER_PEER_VERIFIED_BY,
    PARAMETER_PEER_VERIFIED_AT,
    ANNOTATION_REMARK,
    ANNOTATION_MEDIA,
    PARAMETER_PEER_STATUS,
    JOB_MODIFIED_AT,
    JOB_CWE_REASON,
    JOB_CWE_COMMENT,
    JOB_CWE_FILE

  }

  public final static Set<Type.JobLogTriggerType> VERIFICATION_TRIGGER_TYPES = Set.of(JobLogTriggerType.PARAMETER_SELF_VERIFIED_AT, JobLogTriggerType.PARAMETER_SELF_VERIFIED_BY, JobLogTriggerType.PARAMETER_PEER_VERIFIED_AT, JobLogTriggerType.PARAMETER_PEER_VERIFIED_BY, JobLogTriggerType.PARAMETER_PEER_STATUS);

  public enum AutomationType {
    PROCESS_BASED,
    OBJECT_BASED,
  }

  public enum AutomationTriggerType {
    JOB_STARTED,
    JOB_COMPLETED,
    TASK_STARTED,
    TASK_COMPLETED,
  }

  @Getter
  public enum AutomationActionType {
    SET_PROPERTY("Set Property"),
    INCREASE_PROPERTY("Increase Property"),
    DECREASE_PROPERTY("Decrease Property"),
    CLEAR_PROPERTY("Clear Property"),
    CREATE_OBJECT("Create Object"),
    BULK_CREATE_OBJECT("Bulk Create Object"),
    ARCHIVE_OBJECT("Archive Object"),
    SET_RELATION("Set Relation");

    private final String value;

    AutomationActionType(String value) {
      this.value = value;
    }
  }

  public enum TargetEntityType {
    RESOURCE_PARAMETER,
    OBJECT
  }

  public enum ParameterTargetEntityType {
    TASK,
    PROCESS,
    UNMAPPED
  }

  public enum AutomationDateTimeCaptureType {
    START_TIME("start time"),
    END_TIME("end time"),
    CONSTANT("constant"),
    PARAMETER("parameter");
    private final String value;

    AutomationDateTimeCaptureType(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  public enum ConfigurableViewTargetType {
    PROCESS,
    JOB
  }

  public enum CustomViewFilterType {
    CHECKLIST_ID(Collections.singletonList("checklistId")),
    CHECKLIST_NAME(Collections.singletonList("checklistName")),
    CHECKLIST_CODE(Collections.singletonList("checklistCode")),
    JOB_CODE(Collections.singletonList("code")),
    JOB_STATE(Collections.singletonList("state")),
    STARTED_AT(Collections.singletonList("startedAt")),
    ENDED_AT(Collections.singletonList("endedAt")),
    CREATED_AT(Collections.singletonList("createdAt")),
    MODIFIED_AT(Collections.singletonList("modifiedAt")),
    CREATED_BY(Collections.singletonList("createdBy")),
    STARTED_BY(Collections.singletonList("startedBy")),
    MODIFIED_BY(Collections.singletonList("modifiedBy")),
    ENDED_BY(Collections.singletonList("endedBy"));
    private final List<String> value;

    CustomViewFilterType(List<String> value) {
      this.value = value;
    }

    public List<String> getValue() {
      return value;
    }

  }

  public enum JobExcelFilterType {
    EXPECTED_START_DATE(Collections.singletonList("expectedStartDate")),
    EXPECTED_END_DATE(Collections.singletonList("expectedEndDate")),
    FACILITY_ID(Collections.singletonList("facility.id")),
    ORGANISATION_ID(Collections.singletonList("organisation.id")),
    USE_CASE_ID(Collections.singletonList("useCaseId")),
    CHECKLIST_ANCESTOR_ID(Collections.singletonList("checklistAncestorId")),
    CREATED_BY_ID(Collections.singletonList("createdBy.id"));
    private final List<String> value;

    JobExcelFilterType(List<String> value) {
      this.value = value;
    }

    public List<String> getValue() {
      return value;
    }

  }

  public enum ScheduledJobType {
    CREATE_JOB,
    JOB_DELAY_EMAIL_DISPATCH,
    JOB_OVERDUE_EMAIL_DISPATCH
  }

  public enum ScheduledJobGroup {
    JOBS,
    JOB_DELAY,
    JOB_OVERDUE
  }

  public enum InterlockTargetEntityType {
    JOB,
    PROCESS,
    TASK
  }

  public enum InterlockTriggerType {
    TASK_STARTED,
    TASK_COMPLETED
  }

  @Getter
  public enum TaskExecutionType {
    MASTER(0),
    REPEAT(1),
    RECURRING(2);
    private final int value;

    TaskExecutionType(int value) {
      this.value = value;
    }

    public static TaskExecutionType fromValue(int value) {
      for (TaskExecutionType taskExecutionType : values()) {
        if (taskExecutionType.getValue() == value) {
          return taskExecutionType;
        }
      }
      throw new IllegalArgumentException("Invalid TaskExecutionType value: " + value);
    }
  }

  public enum ScheduledTaskType {
    JOB,
    TASK
  }

  public enum ScheduledTaskCondition {
    START,
    COMPLETE
  }

  public enum SelectorType {
    PARAMETER,
    CONSTANT,
    NONE
  }

  public enum OffSetSelectorType {
    PARAMETER,
    CONSTANT,
    NONE
  }

  public enum TaskExecutorLockType {
    EQ,
    NIN
  }

  public enum ParameterRelationValidationType {
    RESOURCE, CRITERIA
  }

  public enum ParameterExceptionApprovalType {
    NONE,
    DEFAULT_FLOW,
    ACCEPT_WITH_REASON_FLOW,
    APPROVER_REVIEWER_FLOW
  }

  public enum ChecklistElementType {
    TASK,
    STAGE,
    PARAMETER
  }

  public enum EffectEntityType {
    PARAMETER,
    TASK,
    EFFECT,
    CONSTANT

  }
  public enum PdfType{
    JOB_AUDIT,
    JOB_REPORT,
    USER_AUDITS,
    JOB_LOGS,
    OBJECT_AUDIT_LOGS,
    PROCESS_TEMPLATE

  }
  public enum ReportType{
    PDF,
    EXCEL
  }

  public enum JobLogType {
    PROCESS_LOGS,
    ASSETS_LOGS
  }
  public enum TaskDisplayMode {
    SEPARATE_TABLES,
    COLUMN_BASED
  }
}
