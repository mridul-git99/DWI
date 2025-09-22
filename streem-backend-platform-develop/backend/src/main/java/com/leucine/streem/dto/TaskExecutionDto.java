package com.leucine.streem.dto;

import com.leucine.streem.constant.State;
import com.leucine.streem.constant.Type;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecutionDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -108793525629189945L;

  private String id;
  private Long period;
  private Integer orderTree;
  private String correctionReason;
  private boolean correctionEnabled;
  private String reason;
  private List<TaskExecutionAssigneeDto> assignees;
  private List<TaskExecutionAssigneeDto> userGroupAssignees;
  private UserAuditDto startedBy;
  private Long startedAt;
  private Long endedAt;
  private UserAuditDto endedBy;
  private State.TaskExecution state;
  private Type.TaskExecutionType type;
  private PartialAuditDto audit;
  private Set<Long> hide;
  private Set<Long> show;
  private UserAuditDto correctedBy;
  private Long correctedAt;
  private Long duration;
  private List<TaskPauseReasonOrComment> pauseReasons = new ArrayList<>();
  private boolean continueRecurrence;
  private Long recurringExpectedStartedAt;
  private Long recurringExpectedDueAt;
  private String recurringPrematureStartReason;
  private String recurringOverdueCompletionReason;
  private Long schedulingExpectedStartedAt;
  private Long schedulingExpectedDueAt;
  private String scheduleOverdueCompletionReason;
  private String schedulePrematureStartReason;
  private Set<String> scheduledTaskExecutionIds = new HashSet<>();
  private List<AutomationResponseDto> executedAutomations;
  private String recurredTaskExecutionId;
  private List<TaskPauseResumeAuditDto> taskPauseResumeAudits = new ArrayList<>();
}
