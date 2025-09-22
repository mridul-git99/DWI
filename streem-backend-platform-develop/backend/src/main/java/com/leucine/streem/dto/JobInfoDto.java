package com.leucine.streem.dto;

import com.leucine.streem.constant.State;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobInfoDto implements Serializable {
  private static final long serialVersionUID = 8665246750593307900L;

  private String id;
  private String code;
  private State.Job state;
  private PartialAuditDto audit;
  private Set<String> scheduledTaskExecutionIds = new HashSet<>();
}
