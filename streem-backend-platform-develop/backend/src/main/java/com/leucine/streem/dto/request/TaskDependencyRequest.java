package com.leucine.streem.dto.request;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Data
@Builder
@Jacksonized
public class TaskDependencyRequest {
  private List<Long> prerequisiteTaskIds;
}
