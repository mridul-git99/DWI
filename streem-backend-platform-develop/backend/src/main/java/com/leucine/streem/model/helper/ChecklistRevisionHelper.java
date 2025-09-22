package com.leucine.streem.model.helper;

import com.leucine.streem.dto.request.InterlockDto;
import com.leucine.streem.model.Parameter;
import com.leucine.streem.model.Task;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChecklistRevisionHelper {
  Map<Long, Task> existingParameterRevisedTaskMap = new HashMap<>();
  Map<Long, Parameter> revisedParameters = new HashMap<>();

  List<Long> existingCalculationParameterIdList = new ArrayList<>();
  List<Task> taskHavingAutomations = new ArrayList<>();
  List<Parameter> existingParameterHavingAutoInitialize = new ArrayList<>();
  List<Parameter> existingParameterHavingRules = new ArrayList<>();
  List<Parameter> existingResourceParameters = new ArrayList<>();
  List<Parameter> existingParameterHavingValidations = new ArrayList<>();
  List<Parameter> existingParameterHavingParameterizedLeastCount = new ArrayList<>();

  Map<Long, Task> oldTaskIdAndNewTaskMapping = new HashMap<>();
  Map<Long, Long> revisedParametersOldAndNewIdMap = new HashMap<>();
  Map<Long, Long> oldStageIdNewStageIdMapping = new HashMap<>();
  Map<Task, InterlockDto> OldTaskAndOldInterlockMap = new HashMap<>();
  Map<Long, Long> oldActionIdToNewActionIdMap = new HashMap<>();
  Map<Long, Long> oldEffectIdToNewEffectIdMap = new HashMap<>();
}
