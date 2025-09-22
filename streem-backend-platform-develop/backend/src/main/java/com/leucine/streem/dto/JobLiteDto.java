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
public class JobLiteDto implements Serializable {
  private static final long serialVersionUID = 1305472892372612136L;


  private String id;
  private String code;
  private State.Job state;
  private ChecklistJobLiteDto checklist;
  private Long schedulerId;
  private Long expectedStartDate;
  private Long expectedEndDate;
  private List<ParameterDto> parameterValues;
  private Long startedAt;
  private Long endedAt;
  private boolean showVerificationBanner;
  private boolean showCorrectionBanner;
  private boolean showExceptionBanner;
  private boolean showCJFExceptionBanner;
  private boolean forceCwe;
}
