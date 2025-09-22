package com.leucine.streem.dto.mapper;

import com.leucine.streem.constant.Misc;
import com.leucine.streem.constant.State;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.mapper.helper.IBaseMapper;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.util.Utility;
import org.mapstruct.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Mapper(uses = {IChecklistMapper.class})
public interface IJobMapper extends IBaseMapper<JobDto, Job> {

  @Override
  JobDto toDto(Job job);

  JobDto toDto(Job job, @Context Map<Long, List<ParameterValue>> parameterValueMap,
               @Context Map<Long, TaskExecution> taskExecutionMap,
               @Context Map<Long, List<TempParameterValue>> tempParameterValueMap,
               @Context Map<Long, List<TaskPauseReasonOrComment>> pauseReasonOrCommentMap,
               @Context Map<Long, List<ParameterVerification>> parameterVerificationMapPeerAndSelf,
               @Context Map<Long, List<TempParameterVerification>> tempParameterVerificationMapPeerAndSelf);

  JobInfoDto toJobInfoDto(Job job, @Context PrincipalUser principalUser);

  @Mapping(source = "parameterValues", target = "parameterValues", qualifiedByName = "toParameterValuesDto")
  JobInformationDto toJobInformationDto(Job job, @Context Map<String, List<TaskPendingOnMeView>> pendingOnMeTasks, @Context List<EngagedUserView> engagedUsers, @Context List<Long> parameterIds);

  JobStateDto toJobStateDto(Job job);

  @Mapping(source = "relationValues", target = "relations", qualifiedByName = "toRelationValuesDto")
  @Mapping(source = "parameterValues", target = "parameterValues", qualifiedByName = "toParameterValuesDto")
  JobPartialDto jobToJobPartialDto(Job job, @Context Map<String, List<TaskPendingOnMeView>> taskPendingOnMeMap, @Context List<Long> parameterIds);

  List<JobPartialDto> jobToJobPartialDto(List<Job> jobs, @Context Map<String, List<TaskPendingOnMeView>> taskPendingOnMeMap, @Context List<Long> parameterIds);

  @Mapping(source = "checklist.name", target = "name")
  JobAutoSuggestDto jobToJobAutoSuggestDto(Job job);

  List<JobAutoSuggestDto> jobToJobAutoSuggestDto(List<Job> jobs);

  @AfterMapping
  default void setPendingOnMeTasks(Job job, @MappingTarget JobPartialDto jobPartialDto, @Context Map<String, List<TaskPendingOnMeView>> taskPendingOnMeMap) {
    List<TaskPendingOnMeView> taskPendingOnMeViewList = taskPendingOnMeMap.getOrDefault(job.getIdAsString(), new ArrayList<>());
    List<TaskPendingOnMeView> pendingOnMeViewList = taskPendingOnMeViewList.stream()
      .filter(taskPendingOnMeView -> Misc.TASK_EXECUTION_PENDING_STATES.contains(taskPendingOnMeView.getTaskExecutionState())).toList();
    List<StagePendingDto> stagePendingDtos = buildStageTaskExecutionStructure(pendingOnMeViewList);
    if (!Utility.isEmpty(stagePendingDtos)) {
      StagePendingDto stagePendingDto = stagePendingDtos.get(0);
      if (!Utility.isEmpty(stagePendingDto.getTasks())) {
        TaskPendingDto taskPendingDto = stagePendingDto.getTasks().get(0);
        if (!Utility.isEmpty(taskPendingDto.getTaskExecutions())) {
          TaskExecutionPendingDto taskExecutionPendingDto = taskPendingDto.getTaskExecutions().get(0);
          jobPartialDto.setFirstPendingTaskId(taskExecutionPendingDto.getId());
        }
      }
    }
    jobPartialDto.setPendingTasksCount(pendingOnMeViewList.size());
    jobPartialDto.setTotalTasksCount(taskPendingOnMeViewList.size());
  }

  @AfterMapping
  default void setPendingOnMeTasks(Job job, @MappingTarget JobInformationDto jobInformationDto, @Context Map<String, List<TaskPendingOnMeView>> taskPendingOnMeMap) {
    List<TaskPendingOnMeView> taskPendingOnMeViewList = taskPendingOnMeMap.getOrDefault(job.getIdAsString(), new ArrayList<>());
    List<TaskPendingOnMeView> pendingOnMeViewList = taskPendingOnMeViewList.stream()
      .filter(taskPendingOnMeView -> Misc.TASK_EXECUTION_PENDING_STATES.contains(taskPendingOnMeView.getTaskExecutionState())).toList();
    List<StagePendingDto> stagePendingDtos = buildStageTaskExecutionStructure(pendingOnMeViewList);
    jobInformationDto.setPendingOnMeTasks(stagePendingDtos);
  }

  @AfterMapping
  default void setEngagedUsers(Job job, @MappingTarget JobInformationDto jobInformationDto, @Context List<EngagedUserView> engagedUserViews) {
    List<TaskExecutionEngagedUserDto> taskExecutionEngagedUserDtos = getEngagedUsers(engagedUserViews);
    jobInformationDto.setEngagedUsers(taskExecutionEngagedUserDtos);
  }

  @AfterMapping
  default void setCompletedTasks(Job job, @MappingTarget JobDto jobDto, @Context Map<Long, ParameterValue> parameterValueMap,
                                 @Context Map<Long, List<TaskExecution>> taskExecutionMap, @Context Map<Long, TempParameterValue> tempParameterValueMap) {
    jobDto.setTotalTasks(job.getTaskExecutions().size());
    jobDto.setCompletedTasks(getCompletedTasks(job));
  }

  @AfterMapping
  default void setAudit(Job job, @MappingTarget JobInfoDto jobDto, @Context PrincipalUser principalUser) {
    jobDto.setAudit(IAuditMapper.createAuditDtoFromPrincipalUser(principalUser, job.getModifiedAt()));
  }

  default long getCompletedTasks(Job job) {
    return job.getTaskExecutions().stream()
      .filter(taskExecution -> (taskExecution.getState().equals(State.TaskExecution.COMPLETED)
        || taskExecution.getState().equals(State.TaskExecution.SKIPPED)
        || taskExecution.getState().equals(State.TaskExecution.COMPLETED_WITH_EXCEPTION))).count();
  }

  @Mapping(source = "relationValues", target = "relations", qualifiedByName = "toRelationValuesDto")
  JobReportDto toJobReportDto(Job job);

  @Mapping(source = "relationValues", target = "relations", qualifiedByName = "toRelationValuesDto")
  JobPrintDto toJobPrintDto(Job job,
                            @Context Map<Long, List<ParameterValue>> parameterValueMap,
                            @Context Map<Long, TaskExecution> taskExecutionMap,
                            @Context Map<Long, List<TempParameterValue>> tempParameterValueMap,
                            @Context Map<Long, List<TaskPauseReasonOrComment>> pauseReasonOrCommentMap,
                            @Context Map<Long, List<ParameterVerification>> parameterVerificationMapPeerAndSelf,
                            @Context Map<Long, List<TempParameterVerification>> tempParameterVerificationMapPeerAndSelf);

  @Named("toRelationValuesDto")
  static List<RelationValueDto> toRelationsDto(Set<RelationValue> relations) {
    //TODO: create a IRelationValueMapper and use toDto method here rather then setting values
    var relationValuesMap = relations.stream()
      .map(r -> RelationValueDto.builder()
        .id(r.getRelationId().toString())
        .externalId(r.getObjectTypeExternalId())
        .displayName(r.getObjectTypeDisplayName())
        .targets(new ArrayList<>(Collections.singletonList(
          RelationValueTargetDto.builder()
            .id(r.getIdAsString())
            .collection(r.getCollection())
            .displayName(r.getDisplayName())
            .externalId(r.getExternalId())
            .build())))
        .build())
      .collect(toMap(RelationValueDto::getExternalId,
        Function.identity(),
        (r1, r2) -> {
          r1.getTargets().addAll(r2.getTargets());
          return r1;
        }
      ));
    return new ArrayList<>(relationValuesMap.values());
  }

  // TODO seems of no use, check and remove it
  @Named("toParameterValuesDto")
  static List<ParameterDto> toParameterValuesDto(Set<ParameterValue> parameterValues, @Context List<Long> processParameterIds) {

    Map<Long, List<ParameterValue>> parameterValuesMap = parameterValues.stream()
      .filter(pv -> processParameterIds.contains(pv.getParameterId()))
      .collect(Collectors.groupingBy(ParameterValue::getParameterId));

    List<ParameterDto> parameterDtos = new ArrayList<>();
    for (Long paramId : processParameterIds) {
      List<ParameterValue> valuesForId = parameterValuesMap.getOrDefault(paramId, Collections.emptyList());
      for (ParameterValue parameterValue : valuesForId) {
        Parameter parameter = parameterValue.getParameter();
        ParameterDto parameterDto = new ParameterDto();
        ParameterValueDto parameterValueDto = new ParameterValueDto();
        parameterValueDto.setId(parameterValue.getIdAsString());
        parameterValueDto.setChoices(parameterValue.getChoices());
        parameterValueDto.setValue(parameterValue.getValue());
        parameterValueDto.setReason(parameterValue.getReason());
        parameterValueDto.setHidden(parameterValue.isHidden());
        parameterValueDto.setState(parameterValue.getState());
        parameterValueDto.setAudit(IAuditMapper.createAuditDto(parameterValue.getModifiedBy(), parameterValue.getModifiedAt()));
        parameterDto.setId(parameter.getIdAsString());
        parameterDto.setDescription(parameter.getDescription());
        parameterDto.setAutoInitialize(parameter.getAutoInitialize());
        parameterDto.setAutoInitialized(parameter.isAutoInitialized());
        parameterDto.setMandatory(parameter.isMandatory());
        parameterDto.setLabel(parameter.getLabel());
        parameterDto.setType(parameter.getType().toString());
        parameterDto.setTargetEntityType(parameter.getTargetEntityType());
        parameterDto.setOrderTree(parameter.getOrderTree());
        parameterDto.setData(parameter.getData());
        parameterDto.setMetadata(parameter.getMetadata());
        parameterValueDto.setHasActiveException(parameterValue.isHasActiveException());
        List<ParameterValueDto> responses = new ArrayList<>();
        responses.add(parameterValueDto);
        parameterDto.setResponse(responses);
        parameterDtos.add(parameterDto);
      }
    }
    return parameterDtos;
  }
  static List<StagePendingDto> buildStageTaskExecutionStructure(List<TaskPendingOnMeView> projections) {
    Map<String, StagePendingDto> stages = new LinkedHashMap<>();

    projections.forEach(projection -> {
      StagePendingDto stage = stages.computeIfAbsent(projection.getStageId(),
        id -> new StagePendingDto(id, projection.getStageName(), Integer.valueOf(projection.getStageOrderTree()), new ArrayList<>()));

      TaskPendingDto task = stage.getTasks().stream()
        .filter(t -> t.getId().equals(projection.getTaskId()))
        .findFirst()
        .orElseGet(() -> {
          TaskPendingDto newTask = new TaskPendingDto(projection.getTaskId(), projection.getTaskName(), Integer.valueOf(projection.getTaskOrderTree()), new ArrayList<>());
          stage.getTasks().add(newTask);
          return newTask;
        });

      TaskExecutionPendingDto taskExecution = new TaskExecutionPendingDto(projection.getTaskExecutionId(), Integer.valueOf(projection.getTaskExecutionOrderTree()));
      task.getTaskExecutions().add(taskExecution);
    });

    stages.values().forEach(stage -> stage.getTasks().forEach(task ->
      task.getTaskExecutions().sort(Comparator.comparingInt(TaskExecutionPendingDto::getSortOrder))));
    stages.values().forEach(stage -> stage.getTasks().sort(Comparator.comparingInt(TaskPendingDto::getOrderTree)));
    List<StagePendingDto> sortedStages = new ArrayList<>(stages.values());
    sortedStages.sort(Comparator.comparingInt(StagePendingDto::getOrderTree));
    return sortedStages;
  }

  static List<TaskExecutionEngagedUserDto> getEngagedUsers(List<EngagedUserView> projections) {
    return projections.stream()
      .filter(EngagedUserView::getActionPerformed)
      .map(projection -> new TaskExecutionEngagedUserDto(projection.getUserId(), projection.getEmployeeId(), projection.getFirstName(), projection.getLastName(), projection.getActionPerformed())).distinct().collect(Collectors.toList());
  }
}
