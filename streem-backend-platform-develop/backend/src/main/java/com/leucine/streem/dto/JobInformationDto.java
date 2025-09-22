package com.leucine.streem.dto;

import com.leucine.streem.constant.State;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobInformationDto implements Serializable {
  private static final long serialVersionUID = 8665246750593307900L;

  private String id;
  private String code;
  private State.Job state;
  private PartialAuditDto audit;
  private List<ParameterDto> parameterValues;
  private ChecklistPartialDto checklist;
  private List<JobAnnotationDto> jobAnnotationDto;
  private List<StagePendingDto> pendingOnMeTasks;
  private Long createdAt;
  private List<TaskExecutionEngagedUserDto> engagedUsers;
  private Long startedAt;
  private UserDto startedBy;
  private UserDto createdBy;
  private Long endedAt;
  private UserDto endedBy;
  private boolean forceCWE;
  private boolean showCJFExceptionBanner;
}
