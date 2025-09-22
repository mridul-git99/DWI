package com.leucine.streem.dto;

import com.leucine.streem.constant.TaskPauseReason;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskPauseResumeAuditDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -108793525624189945L;

  private String id;
  private Long pausedAt;
  private Long resumedAt;
  private String taskExecutionId;
  private TaskPauseReason reason;
  private String comment;
  private UserAuditDto pausedBy;
  private UserAuditDto resumedBy;
}
