package com.leucine.streem.dto.request;

import com.leucine.streem.dto.projection.TaskDetailsView;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
@AllArgsConstructor
public class BulkTaskExecutionAssignmentRequest {
  private Set<TaskDetailsView> taskDetails;
  private Map<String, Set<String>> trainedUserIdAndTaskIdMap;
  private Map<String, Set<String>> taskIdAndTrainedUserMap;
}
