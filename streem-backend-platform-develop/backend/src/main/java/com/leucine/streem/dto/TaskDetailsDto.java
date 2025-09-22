package com.leucine.streem.dto;

import com.leucine.streem.constant.State;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class TaskDetailsDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -6714070537687841563L;

  private String jobId;
  private State.Job jobState;
  private TaskDto task;
  private boolean isHidden;
}
