package com.leucine.streem.dto;

import com.leucine.streem.constant.State;
import com.leucine.streem.constant.Type;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecutionLiteDto  implements Serializable {
  private static final long serialVersionUID = -109793525629189945L;

  private String id;
  private Integer orderTree;
  private State.TaskExecution state;
  private Type.TaskExecutionType type;
  private boolean hidden;
}
