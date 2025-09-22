package com.leucine.streem.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImportChecklistRequest {
  private String id;
  private String name;
  private List<ImportStageRequest> stageRequests;
  private List<ImportParameterRequest> parameterRequests;
  private List<ImportActionRequest> actionRequests;
  private String description;
  private String colorCode;
}
