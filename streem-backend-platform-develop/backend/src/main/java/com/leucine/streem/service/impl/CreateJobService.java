package com.leucine.streem.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.leucine.streem.constant.State;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.JobDto;
import com.leucine.streem.dto.ParameterDetailsDto;
import com.leucine.streem.dto.ParameterDto;
import com.leucine.streem.dto.RuleHideShowDto;
import com.leucine.streem.dto.mapper.IUserMapper;
import com.leucine.streem.dto.projection.TaskDetailsView;
import com.leucine.streem.dto.projection.TrainedUsersView;
import com.leucine.streem.dto.request.BulkTaskExecutionAssignmentRequest;
import com.leucine.streem.dto.request.CreateJobRequest;
import com.leucine.streem.dto.request.ParameterExecuteRequest;
import com.leucine.streem.dto.request.TaskExecutionAssignmentRequest;
import com.leucine.streem.dto.response.Error;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.*;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.BaseEntity;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.repository.*;
import com.leucine.streem.repository.impl.ParameterValueRepositoryImpl;
import com.leucine.streem.repository.impl.TaskExecutionRepositoryImpl;
import com.leucine.streem.service.*;
import com.leucine.streem.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.model.Recur;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.leucine.streem.constant.State.ALLOWED_CREATE_JOB_FORM_PARAMETERS;
import static com.leucine.streem.constant.Type.ParameterExceptionApprovalType.DEFAULT_FLOW;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateJobService implements ICreateJobService {
  private final ICodeService codeService;
  private final ITrainedUserTaskMappingRepository trainedUserTaskMapping;
  private final IChecklistRepository checklistRepository;
  private final IFacilityRepository facilityRepository;
  private final IJobAuditService jobAuditService;
  private final IJobAssignmentService jobAssignmentService;
  private final JobLogService jobLogService;
  private final IJobRepository jobRepository;
  private final IParameterExecutionService parameterExecutionService;
  private final IParameterRepository parameterRepository;
  private final ISchedulerRepository schedulerRepository;
  private final IStageReportService stageReportService;
  private final IUserMapper userMapper;
  private final IUserRepository userRepository;
  private final IJobNotificationEmailDispatchService jobDelayDispatchService;
  private final IParameterExecutionHandler parameterExecutionHandler;
  private final ITaskExecutionRepository taskExecutionRepository;
  private final TaskExecutionRepositoryImpl taskExecutionRepositoryImpl;
  private final ParameterValueRepositoryImpl parameterValueRepositoryImpl;
  private final JdbcTemplate jdbcTemplate;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JobDto createJob(CreateJobRequest createJobRequest, PrincipalUser principalUser, Facility facility, boolean isScheduled,
                          Scheduler scheduler, Long nextExpectedStartDate) throws ResourceNotFoundException, StreemException, IOException, MultiStatusException {
    Checklist checklist = checklistRepository
      .findById(createJobRequest.getChecklistId())
      .orElseThrow(() -> new ResourceNotFoundException(createJobRequest.getChecklistId(), ErrorCode.PROCESS_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    return createJob(checklist, createJobRequest, principalUser, facility, isScheduled, scheduler, nextExpectedStartDate);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void createScheduledJob(Long schedulerId, Long dateTime) throws ResourceNotFoundException, StreemException, IOException, MultiStatusException {
    log.info("[createScheduledJob] request to create a scheduled job, schedulerId: {}, dateTime: {}", schedulerId, dateTime);

    Scheduler scheduler = schedulerRepository.findById(schedulerId)
      .orElseThrow(() -> new ResourceNotFoundException(schedulerId, ErrorCode.SCHEDULER_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    Checklist checklist = checklistRepository
      .findById(scheduler.getChecklistId())
      .orElseThrow(() -> new ResourceNotFoundException(scheduler.getChecklistId(), ErrorCode.PROCESS_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));


    if (!State.Scheduler.DEPRECATED.equals(scheduler.getState()) && !scheduler.isArchived() && !checklist.isArchived()) {
      Map<Long, ParameterExecuteRequest> parameterValues = JsonUtils.convertValue(scheduler.getData().get(Scheduler.PARAMETER_VALUES), new TypeReference<>() {
      });

      Facility facility = facilityRepository.findById(scheduler.getFacilityId()).get();
      User user = userRepository.findById(User.SYSTEM_USER_ID).get();
      PrincipalUser principalUser = userMapper.toPrincipalUser(user);

      CreateJobRequest createJobRequest = new CreateJobRequest();
      createJobRequest.setChecklistId(scheduler.getChecklistId());
      createJobRequest.setParameterValues(parameterValues);

      Long expectedStartDateTime;
      Recur recurrence = RecurrenceRuleUtils.parseRecurrenceRuleExpression(scheduler.getRecurrenceRule());
      LocalDateTime localDateTime = DateTimeUtils.getLocalDateTime(dateTime);
      ZoneId zoneId = ZoneId.systemDefault();
      Date date = Date.from(localDateTime.atZone(zoneId).toInstant());
      Date nextDate = RecurrenceRuleUtils.getNextEventAfter(recurrence, scheduler.getRecurrenceRule(), date);
      if (null != nextDate) {
        expectedStartDateTime = DateTimeUtils.getEpochTime(nextDate);
      } else {
        expectedStartDateTime = null;
      }

      if (null != expectedStartDateTime) {
        var jobAlreadyCreated = jobRepository.isJobExistsBySchedulerIdAndDateGreaterThanOrEqualToExpectedStartDate(scheduler.getId(), expectedStartDateTime);

        if (jobAlreadyCreated) {
          log.info("[createScheduledJob] skipping job creation since job was already created for this date, scheduler: {}", scheduler);
        } else {
          createJob(checklist, createJobRequest, principalUser, facility, true, scheduler, expectedStartDateTime);
        }
      }
    }
  }

  private JobDto createJob(Checklist checklist, CreateJobRequest createJobRequest, PrincipalUser principalUser, Facility facility, boolean isScheduled,
                           Scheduler scheduler, Long nextExpectedStartDate) throws StreemException, IOException, ResourceNotFoundException, MultiStatusException {


    User principalUserEntity = userRepository.findById(principalUser.getId()).get();

    if (!State.Checklist.PUBLISHED.equals(checklist.getState())) {
      ValidationUtils.invalidate(createJobRequest.getChecklistId(), ErrorCode.PROCESS_NOT_PUBLISHED);
    }


// Generate a job ID
    Long jobId = IdGenerator.getInstance().nextId();

// Collect all task executions and parameter values first
    List<TaskExecution> allTaskExecutions = new ArrayList<>();
    List<ParameterValue> allParameterValues = new ArrayList<>();

// Create a job object for reference only
    Job job = new Job();
    job.setId(jobId);
    job.setState(State.Job.UNASSIGNED);

    log.info("Creating job with ID: {}", jobId);

// Prepare all task executions and parameter values
    for (Stage stage : checklist.getStages()) {
      for (Task task : stage.getTasks()) {
        TaskExecution taskExecution = createTaskExecution(job, task, Type.TaskExecutionType.MASTER, principalUserEntity);
        allTaskExecutions.add(taskExecution);

        for (Parameter parameter : task.getParameters()) {
          ParameterValue parameterValue = createParameterValue(job, taskExecution, parameter, principalUserEntity);
          allParameterValues.add(parameterValue);
        }
      }
    }

    String code = codeService.getCode(Type.EntityType.JOB, facility.getOrganisation().getId());

    Long now = DateTimeUtils.now();
    Long expectedEndDate = null;

    if (isScheduled) {
      job.setScheduler(scheduler);
      job.setScheduled(true);
      Integer interval = scheduler.getDueDateInterval();
      job.setExpectedStartDate(nextExpectedStartDate);
      LocalDateTime endDate = DateTimeUtils.getLocalDateTime(nextExpectedStartDate).plusSeconds(interval);
      expectedEndDate = DateTimeUtils.getEpochTime(endDate);
      job.setExpectedEndDate(DateTimeUtils.getEpochTime(endDate));
    }
    long afterScheduleSetup = System.currentTimeMillis();

// Use JDBC to directly insert the job
    String jobSql = "INSERT INTO jobs (id, code, state, checklists_id, facilities_id, organisations_id, " +
      "use_cases_id, created_by, modified_by, created_at, modified_at, checklist_ancestor_id, " +
      "is_scheduled, expected_start_date, expected_end_date) " +
      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    int rowsAffected = jdbcTemplate.update(jobSql,
      jobId,                                     // id
      code,                                      // code
      State.Job.ASSIGNED.name(),                 // state
      checklist.getId(),                         // checklists_id
      facility.getId(),                          // facilities_id
      checklist.getOrganisation().getId(),       // organisations_id
      checklist.getUseCase().getId(),            // use_cases_id
      principalUserEntity.getId(),               // created_by
      principalUserEntity.getId(),               // modified_by
      now,                                       // created_at
      now,                                       // modified_at
      checklist.getVersion().getAncestor(),      // checklist_ancestor_id
      isScheduled,                               // is_scheduled
      isScheduled ? nextExpectedStartDate : null, // expected_start_date
      expectedEndDate                           // expected_end_date
    );


    if (rowsAffected != 1) {
      ValidationUtils.invalidate(jobId, ErrorCode.ERROR_CREATING_A_SCHEDULER);
    }

// Insert scheduler mapping if scheduled
    long beforeSchedulerUpdate = System.currentTimeMillis();
    if (isScheduled && scheduler != null) {
      String schedulerSql = "UPDATE jobs SET schedulers_id = ? WHERE id = ?";
      jdbcTemplate.update(schedulerSql, scheduler.getId(), jobId);
    }
    long afterSchedulerUpdate = System.currentTimeMillis();

// Rather than loading the entire job entity, we'll use the job object we've already created
// and complete populating its properties for use in the rest of the method
    job.setCode(code);
    job.setChecklist(checklist);
    job.setFacility(facility);
    job.setOrganisation(checklist.getOrganisation());
    job.setUseCase(checklist.getUseCase());
    job.setUseCaseId(checklist.getUseCase().getId());
    job.setCreatedBy(principalUserEntity);
    job.setModifiedBy(principalUserEntity);
    job.setChecklistAncestorId(checklist.getVersion().getAncestor());
    job.setCreatedAt(now);
    job.setModifiedAt(now);
    job.setState(State.Job.ASSIGNED);

    if (isScheduled) {
      job.setScheduler(scheduler);
      job.setScheduled(true);
      job.setExpectedStartDate(nextExpectedStartDate);
      job.setExpectedEndDate(expectedEndDate);
    }

    // Perform bulk inserts
    taskExecutionRepositoryImpl.bulkInsertTaskExecutions(allTaskExecutions);

    parameterValueRepositoryImpl.bulkInsertParameterValues(allParameterValues);

    if (isScheduled) {
      jobDelayDispatchService.addJobDelayEmailDispatchEvent(job);
      jobDelayDispatchService.addJobOverDueEmailDispatchEvent(job);
    }

    // Execute rules temporarily, create job parameter values and validate if all mandatory parameters are provided
    Map<Long, ParameterExecuteRequest> parameterValues = createJobRequest.getParameterValues();
    Map<Long, ParameterValue> jobParameterIdParameterValueMap = new HashMap<>();
    List<Parameter> parameters = parameterRepository.getParametersByChecklistIdAndTargetEntityType(checklist.getId(), Type.ParameterTargetEntityType.PROCESS);
    List<ParameterDto> parameterDtos = new ArrayList<>();

    RuleHideShowDto tempRuleHideShow = parameterExecutionService.tempExecuteRules(createJobRequest.getParameterValues(), checklist.getId());

    Set<String> hideParameterSet = tempRuleHideShow.getHide();
    List<ParameterValue> jobParameterValueList = new ArrayList<>();

    // Process job-level parameters
    for (Parameter parameter : parameters) {
      ParameterValue jobParameterValue = createParameterValue(job, null, parameter, principalUserEntity);
      jobParameterIdParameterValueMap.put(parameter.getId(), jobParameterValue);
      jobParameterValueList.add(jobParameterValue);

      if (!parameterValues.containsKey(parameter.getId()) && parameter.isMandatory() && !hideParameterSet.contains(parameter.getId().toString())) {
        ValidationUtils.invalidate(parameter.getId(), ErrorCode.MANDATORY_PARAMETER_VALUES_NOT_PROVIDED);
      }
    }

    // Bulk insert job parameter values
    parameterValueRepositoryImpl.bulkInsertParameterValues(jobParameterValueList);

    // Create job log
    jobLogService.createJobLog(job.getIdAsString(), job.getCode(), job.getState(), job.getCreatedAt(), userMapper.toUserAuditDto(principalUser), checklist.getIdAsString(),
      checklist.getName(), checklist.getCode(), facility.getIdAsString(), principalUser);

    List<Error> hardErrors = new ArrayList<>();
    List<Error> softErrors = new ArrayList<>();

    // Process parameter execution requests
    int parameterCount = 0;
    for (Parameter parameter : parameters) {
      if (!parameter.isAutoInitialized() && parameterValues.containsKey(parameter.getId())) {
        parameterCount++;

        ParameterExecuteRequest parameterExecuteRequest = parameterValues.get(parameter.getId());
        parameterExecuteRequest.setJobId(job.getId());
        ParameterValue parameterValue = jobParameterIdParameterValueMap.get(parameter.getId());
        try {
          ParameterDto parameterDto = parameterExecutionHandler.executeParameter(job.getId(), parameterValue.getId(), parameterExecuteRequest, Type.JobLogTriggerType.PROCESS_PARAMETER_VALUE, false, true, isScheduled);
          log.info("ParameterDto state: {}", parameterDto.getResponse().get(0).getState());
          if (parameter.isMandatory() && !Utility.isEmpty(parameterDto) && !ALLOWED_CREATE_JOB_FORM_PARAMETERS.contains(parameterDto.getResponse().get(0).getState())) {
            ValidationUtils.invalidate(parameter.getId(), ErrorCode.MANDATORY_PARAMETER_VALUES_NOT_PROVIDED);
          }
          parameterDtos.add(parameterDto);
        } catch (ParameterExecutionException parameterExecutionException) {
          List<ParameterDetailsDto> parameterDetailsDtoList = parameterExecutionException.getErrorList().stream()
            .flatMap(error -> Stream.of((ParameterDetailsDto)(error.getErrorInfo())))
            .toList();

          for (ParameterDetailsDto parameterDetailsDto : parameterDetailsDtoList) {
            if (parameterDetailsDto.getExceptionApprovalType() == DEFAULT_FLOW) {
              hardErrors.addAll(parameterExecutionException.getErrorList());
            } else {
              softErrors.addAll(parameterExecutionException.getErrorList());
            }
          }
        }

      }
    }

    if (!Utility.isEmpty(hardErrors)) {
      StreemException exception = new StreemException("Error during createJob", hardErrors);
      throw exception;
    }

    JobDto jobDto = new JobDto();
    if (!isScheduled) {
      jobDto.setId(job.getIdAsString());
      jobDto.setCode(job.getCode());
      jobDto.setParameterValues(parameterDtos);
      jobDto.setSoftErrors(softErrors);
    }
    stageReportService.registerStagesForJob(checklist.getId(), job.getId());

    jobAuditService.createJob(job.getIdAsString(), principalUser);

    // Assign default users
    assignDefaultUsersToJob(checklist, job, facility, principalUser);

    jobLogService.updateJobState(job.getIdAsString(), principalUser);

    return jobDto;
  }

  /**
   * Creates a task execution object with necessary associations.
   */
  private TaskExecution createTaskExecution(Job job, Task task, Type.TaskExecutionType type, User principalUserEntity) {
    TaskExecution taskExecution = new TaskExecution();
    taskExecution.setId(IdGenerator.getInstance().nextId());
    taskExecution.setOrderTree(1);
    taskExecution.setType(type);
    taskExecution.setJob(job);
    taskExecution.setJobId(job.getId());
    taskExecution.setTask(task);
    taskExecution.setCreatedBy(principalUserEntity);
    taskExecution.setModifiedBy(principalUserEntity);
    taskExecution.setState(State.TaskExecution.NOT_STARTED);
    taskExecution.setContinueRecurrence(task.isEnableRecurrence());
    return taskExecution;
  }

  /**
   * Creates a parameter value object with necessary associations.
   */
  private ParameterValue createParameterValue(Job job, TaskExecution taskExecution, Parameter parameter, User principalUserEntity) {
    ParameterValue parameterValue = new ParameterValue();
    parameterValue.setId(IdGenerator.getInstance().nextId());
    parameterValue.setJob(job);
    parameterValue.setJobId(job.getId());
    parameterValue.setTaskExecution(taskExecution);
    parameterValue.setParameter(parameter);
    parameterValue.setHidden(parameter.isHidden());
    parameterValue.setState(State.ParameterExecution.NOT_STARTED);
    parameterValue.setCreatedBy(principalUserEntity);
    return parameterValue;
  }

//  private void createTaskExecutions(Job job, Stage stage, User principalUserEntity) {
//    for (Task task : stage.getTasks()) {
//      TaskExecution taskExecution = createTaskExecution(job, task, Type.TaskExecutionType.MASTER, principalUserEntity);
//      job.addTaskExecution(taskExecution);
//      createParameterValues(job, taskExecution, task, principalUserEntity);
//    }
//  }
//
//  private void createParameterValues(Job job, TaskExecution taskExecution, Task task, User principalUserEntity) {
//    for (Parameter parameter : task.getParameters()) {
//      ParameterValue parameterValue = createParameterValue(job, taskExecution, parameter, principalUserEntity);
//      job.addParameterValue(parameterValue);
//    }
//  }
//
//  private TaskExecution createTaskExecution(Job job, Task task, Type.TaskExecutionType type, User principalUserEntity) {
//    TaskExecution taskExecution = new TaskExecution();
//    taskExecution.setId(IdGenerator.getInstance().nextId());
//    taskExecution.setOrderTree(1);
//    taskExecution.setType(type);
//    taskExecution.setJob(job);
//    taskExecution.setTask(task);
//    taskExecution.setCreatedBy(principalUserEntity);
//    taskExecution.setModifiedBy(principalUserEntity);
//    taskExecution.setState(State.TaskExecution.NOT_STARTED);
//    taskExecution.setContinueRecurrence(task.isEnableRecurrence());
//    return taskExecution;
//  }
//
//  private ParameterValue createParameterValue(Job job, TaskExecution taskExecution, Parameter parameter, User principalUserEntity) {
//    ParameterValue parameterValue = new ParameterValue();
//    parameterValue.setJob(job);
//    parameterValue.setTaskExecution(taskExecution);
//    parameterValue.setParameter(parameter);
//    parameterValue.setHidden(parameter.isHidden());
//    parameterValue.setState(State.ParameterExecution.NOT_STARTED);
//    parameterValue.setCreatedBy(principalUserEntity);
//    parameterValue.setTaskExecution(taskExecution);
//    return parameterValue;
//  }

  private void assignDefaultUsersToJob(Checklist checklist, Job job, Facility facility, PrincipalUser principalUser) throws StreemException, MultiStatusException, ResourceNotFoundException {
    Set<Long> trainedUserIds = trainedUserTaskMapping.findUserIdsByChecklistIdAndFacilityId(checklist.getId(), facility.getId());
    Set<Long> trainedUserGroupIds = trainedUserTaskMapping.findUserGroupIdsByChecklistIdAndFacilityId(checklist.getId(), facility.getId());

    Set<Long> allAssignedUserIds = new HashSet<>();
    Set<Long> allUnassignedUserIds = new HashSet<>();
    Set<Long> allAssignedUserGroupIds = new HashSet<>();
    Set<Long> allUnassignedUserGroupIds = new HashSet<>();

    var jobId = job.getId();

    if (!Utility.isEmpty(trainedUserIds)) {
      //TODO: optimise
      Set<TrainedUsersView> trainedUserIdAndTaskIdView = trainedUserTaskMapping.findTaskIdsByChecklistIdAndUserIdAndFacilityId(checklist.getId(), trainedUserIds, facility.getId());
      Map<String, Set<String>> trainedUserIdAndTaskIdMap = trainedUserIdAndTaskIdView.stream()
        .collect(Collectors.groupingBy(TrainedUsersView::getUserId, Collectors.mapping(TrainedUsersView::getTaskId, Collectors.toSet())));

      Map<String, Set<String>> taskIdAndTrainedUserMap = trainedUserIdAndTaskIdView.stream()
        .collect(Collectors.groupingBy(TrainedUsersView::getTaskId, Collectors.mapping(TrainedUsersView::getUserId, Collectors.toSet())));

      BulkTaskExecutionAssignmentRequest bulkAssignmentRequest = new BulkTaskExecutionAssignmentRequest(getDefaultUserTaskDetails(taskIdAndTrainedUserMap.keySet(), job), trainedUserIdAndTaskIdMap, taskIdAndTrainedUserMap);
      jobAssignmentService.assignUsersDuringCreateJob(jobId, bulkAssignmentRequest, false, principalUser);
      allAssignedUserIds.addAll(trainedUserIds);


//      if (!allAssignedUserIds.isEmpty() || !allUnassignedUserIds.isEmpty()) {
//        try {
//          // Adding the sleep so that triggerAt field of create job and assign user audit has the difference
//          Thread.sleep(1000L);
//        } catch (InterruptedException ignore) {
//        }
//      }
    }

    if (!Utility.isEmpty(trainedUserGroupIds)) {
      for (var userGroupId : trainedUserGroupIds) {
        var taskIds = trainedUserTaskMapping.findTaskIdsByChecklistIdAndUserGroupIdAndFacilityId(checklist.getId(), userGroupId, facility.getId());
        var assignedUserGroup = new HashSet<Long>();
        assignedUserGroup.add(userGroupId);

        var bulkAssignmentRequest = new TaskExecutionAssignmentRequest(getDefaultUserTaskExecutionId(taskIds, job), new HashSet<>(), new HashSet<>(), assignedUserGroup, new HashSet<>(), false, false);
        jobAssignmentService.assignUsers(jobId, bulkAssignmentRequest, false, principalUser);
        allAssignedUserGroupIds.addAll(bulkAssignmentRequest.getAssignedUserGroupIds());
        allUnassignedUserGroupIds.addAll(bulkAssignmentRequest.getUnassignedUserGroupIds());
      }
    }
      if (!allAssignedUserIds.isEmpty() || !allUnassignedUserIds.isEmpty() || !allAssignedUserGroupIds.isEmpty() || !allUnassignedUserGroupIds.isEmpty()) {
        User user = userRepository.findById(User.SYSTEM_USER_ID).get();
        PrincipalUser systemPrincipalUser = userMapper.toPrincipalUser(user);
        try {
          // Adding the sleep so that triggerAt field of create job and assign user audit has the difference
          Thread.sleep(1000L);
        } catch (InterruptedException ignore) {
        }
        jobAuditService.bulkAssignUsersToJob(job.getId(), !allAssignedUserIds.isEmpty(), !allUnassignedUserIds.isEmpty(), !allAssignedUserGroupIds.isEmpty(), !allUnassignedUserGroupIds.isEmpty(), systemPrincipalUser);
      }


  }

  private Set<TaskDetailsView> getDefaultUserTaskDetails(Set<String> taskIds, Job job) {
    return taskExecutionRepository.findAllByJobIdAndTaskIdIn(job.getId(), taskIds);
  }

  private Set<Long> getDefaultUserTaskExecutionId(Set<String> taskIds, Job job) {
    return taskExecutionRepository.findAllByJobIdAndTaskIdIn(job.getId(), taskIds).stream()
      .map(TaskDetailsView::getTaskExecutionId)
      .collect(Collectors.toSet());
  }
}
