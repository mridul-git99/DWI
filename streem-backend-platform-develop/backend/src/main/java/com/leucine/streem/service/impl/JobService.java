package com.leucine.streem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leucine.streem.collections.JobLog;
import com.leucine.streem.collections.JobLogMediaData;
import com.leucine.streem.constant.*;
import com.leucine.streem.constant.Action;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.mapper.*;
import com.leucine.streem.dto.projection.*;
import com.leucine.streem.dto.request.CreateJobRequest;
import com.leucine.streem.dto.request.JobCweDetailRequest;
import com.leucine.streem.dto.request.TaskExecutionAssignmentRequest;
import com.leucine.streem.dto.request.UpdateJobRequest;
import com.leucine.streem.dto.response.Error;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.*;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.BaseEntity;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.model.helper.SpecificationBuilder;
import com.leucine.streem.model.helper.parameter.ResourceParameter;
import com.leucine.streem.model.helper.parameter.ShouldBeParameter;
import com.leucine.streem.model.helper.search.SearchCriteria;
import com.leucine.streem.repository.*;
import com.leucine.streem.service.*;
import com.leucine.streem.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.leucine.streem.dto.request.MediaRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService implements IJobService {
  private static final String JOB_ASSIGNEE_PATH = "taskExecutions.assignees.user.id";

  private final IParameterValueRepository parameterValueRepository;
  private final IJobRepository jobRepository;
  private final IJobMapper jobMapper;
  private final ITaskExecutionRepository taskExecutionRepository;
  private final IUserRepository userRepository;
  private final IJobCweService jobCweService;
  private final ITaskExecutionAssigneeRepository taskExecutionAssigneeRepository;
  private final IParameterRepository parameterRepository;
  private final ITempParameterValueRepository tempParameterValueRepository;
  private final IJobAuditService jobAuditService;
  private final IStageReportService stageReportService;
  private final IStageRepository stageRepository;
  private final IStageMapper stageMapper;
  private final ITaskMapper taskMapper;
  private final ITaskExecutionService taskExecutionService;
  private final IUserMapper userMapper;
  private final JobLogService jobLogService;
  private final IFacilityRepository facilityRepository;
  private final IParameterMapper parameterMapper;
  private final IParameterVerificationService parameterVerificationService;
  private final ITaskExecutionTimerService taskExecutionTimerService;
  private final ICreateJobService createJobService;
  private final IJobAssignmentService jobAssignmentService;
  private final ITaskSchedulesRepository taskSchedulesRepository;
  private final IJobAnnotationMapper jobAnnotationMapper;
  private final IJobAnnotationRepository jobAnnotationRepository;
  private final IJobNotificationEmailDispatchService jobDelayEmailDispatchService;
  private final EntityManager entityManager;
  private final IChecklistRepository checklistRepository;
  private final ITaskRepository taskRepository;
  private final IParameterVerificationRepository parameterVerificationRepository;
  private final IParameterExceptionRepository parameterExceptionRepository;
  private final ITaskExecutionTimerRepository taskExecutionTimerRepository;
  private final IMediaRepository mediaRepository;
  private final IJobAnnotationService jobAnnotationService;
  private final IJobAuditRepository jobAuditRepository;
  private final IJobExcelService jobExcelService;

  private static final int SORT_OVERDUE_UNASSIGNED_OR_ASSIGNED = 1;
  private static final int SORT_PENDING_START_UNASSIGNED_OR_ASSIGNED = 2;
  private static final int SORT_PENDING_START_SCHEDULED_FOR_TODAY = 3;
  private static final int SORT_OVERDUE_IN_PROGRESS = 4;
  private static final int SORT_ONGOING = 5;
  private static final int SORT_UNSCHEDULED_ONGOING = 6;
  private static final int SORT_SCHEDULED_FOR_TODAY = 7;
  private static final int SORT_UNSCHEDULED = 8;
  private static final int SORT_COMPLETED_WITH_EXCEPTION = 9;
  private static final int SORT_COMPLETED = 10;
  private static final int SORT_OTHERWISE = 11;

  private final ZoneId zoneId = ZoneId.systemDefault();
  private final ITrainedUserRepository trainedUserRepository;
  private final ICorrectionMapper correctionMapper;
  private final IReviewerMapper reviewerMapper;
  private final ICorrectorMapper correctorMapper;
  private final IReviewerRepository reviewerRepository;
  private final ICorrectorRepository correctorRepository;
  private final ICorrectionRepository correctionRepository;
  private final ICorrectionMediaMappingRepository correctionMediaMappingRepository;
  private final INotificationService notificationService;
  private final ParameterExecutionValidationService parameterExecutionValidationService;
  private final PdfGeneratorUtil pdfGeneratorUtil;

  //TODO add stats object if required, applies everywhere in job execution
  //TODO add corrections in response
  @Override
  public JobDto getJobById(Long jobId) throws ResourceNotFoundException, JsonProcessingException {
    log.info("[getJobById] Request to get job, jobId: {}", jobId);
    Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException(jobId, ErrorCode.JOB_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    List<ParameterValue> parameterValues = parameterValueRepository.findAllByJobId(jobId);
    List<TaskExecution> taskExecutionList = taskExecutionRepository.readByJobId(jobId, job.getChecklistId());
    Map<Long, List<TaskPauseReasonOrComment>> pauseCommentsOrReason = taskExecutionTimerService.calculateDurationAndReturnReasonsOrComments(taskExecutionList);
    Map<Long, List<ParameterValue>> taskParameterValuesMap = new HashMap<>();
    Map<Long, List<ParameterValue>> jobParameterValuesMap = new HashMap<>();
    Set<Parameter> jobParameters = new HashSet<>();
    for (ParameterValue av : parameterValues) {
      var parameter = av.getParameter();
      if (Type.ParameterTargetEntityType.TASK.equals(av.getParameter().getTargetEntityType())) {
        taskParameterValuesMap.computeIfAbsent(parameter.getId(), k -> new ArrayList<>());
        taskParameterValuesMap.get(parameter.getId()).add(av);
      } else {
        jobParameters.add(parameter);
        jobParameterValuesMap.computeIfAbsent(parameter.getId(), k -> new ArrayList<>());
        jobParameterValuesMap.get(parameter.getId()).add(av);
      }
    }
    Map<Long, List<TaskExecution>> taskIdTaskExecutionListMap = new HashMap<>();
    Map<Long, TaskExecution> taskExecutionMap = new HashMap<>();
    for (TaskExecution taskExecution : taskExecutionList) {
      taskIdTaskExecutionListMap.computeIfAbsent(taskExecution.getTask().getId(), k -> new ArrayList<>());
      taskIdTaskExecutionListMap.get(taskExecution.getTask().getId()).add(taskExecution);
      taskExecutionMap.put(taskExecution.getId(), taskExecution);
    }
    List<TempParameterValue> tempParameterValues = tempParameterValueRepository.readByJobId(jobId);

    Map<Long, List<TempParameterValue>> tempParameterValueMap = tempParameterValues.stream().collect(Collectors.groupingBy(av -> av.getParameter().getId(), Collectors.toList()));
    Map<Long, List<TempParameterVerification>> tempParameterVerificationMap = parameterVerificationService.getTempParameterVerificationsDataForAJob(jobId);
    Map<Long, List<ParameterVerification>> parameterVerificationMap = parameterVerificationService.getParameterVerificationsDataForAJob(jobId);

    JobDto jobDto = jobMapper.toDto(job, taskParameterValuesMap, taskExecutionMap, tempParameterValueMap, pauseCommentsOrReason, parameterVerificationMap, tempParameterVerificationMap);
    List<ParameterDto> parameterDtos = parameterMapper.toDto(jobParameters, jobParameterValuesMap, taskExecutionMap, null, new HashMap<>(), new HashMap<>(), new HashMap<>());
    jobDto.setParameterValues(parameterDtos);
    return jobDto;
  }

  @Override
  @Transactional(readOnly = true)
  public ByteArrayInputStream generateJobsExcel(String filters, String objectId) throws IOException, ResourceNotFoundException {
    log.info("[generateJobsExcel] Request to generate jobs Excel, filters: {}, objectId: {}", filters, objectId);

    return jobExcelService.generateJobsExcel(filters, objectId);
  }

  @Override
  public Page<JobPartialDto> getAllJobs(String objectId, String filters, Pageable pageable) {
    log.info("[getAllJobs] Request to get all jobs, filters: {}, pageable: {}", filters, pageable);

    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    SearchCriteria organisationSearchCriteria = (new SearchCriteria()).setField(Job.ORGANISATION_ID).setOp(Operator.Search.EQ.toString()).setValues(Collections.singletonList(principalUser.getOrganisationId()));
    SearchCriteria facilitySearchCriteria = null;
    Long currentFacilityId = principalUser.getCurrentFacilityId();
    if (currentFacilityId != null && !currentFacilityId.equals(Misc.ALL_FACILITY_ID)) {
      facilitySearchCriteria =
        (new SearchCriteria()).setField(Job.FACILITY_ID).setOp(Operator.Search.EQ.toString()).setValues(Collections.singletonList(currentFacilityId));
    }

    /*--Fetch JobsIds wrt Specification--*/
    Specification<Job> specification;

    if (!Utility.isEmpty(objectId)) {
      Set<Long> jobIds = getJobIdsHavingObjectInChoicesForAllParameters(objectId);
      SearchCriteria jobIdsCriteria = (new SearchCriteria()).setField(Job.ID).setOp(Operator.Search.ANY.toString()).setValues(new ArrayList<>(jobIds));
      if (Utility.isEmpty(jobIds)) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
      }
      specification = SpecificationBuilder.createSpecification(filters, Arrays.asList(organisationSearchCriteria, facilitySearchCriteria, jobIdsCriteria));
    } else {
      specification = SpecificationBuilder.createSpecification(filters, Arrays.asList(organisationSearchCriteria, facilitySearchCriteria));
    }
    Page<Job> jobPage = jobRepository.findAll(specification, pageable);

    List<Job> jobs = jobPage.getContent();
    Set<Long> ids = jobs.stream().map(BaseEntity::getId).collect(Collectors.toSet());

    Map<Long, Set<ParameterValue>> jobParameterValueMap = parameterValueRepository.findAllByJobIdAndTargetEntityType(ids, Type.ParameterTargetEntityType.PROCESS).stream()
      .collect(Collectors.groupingBy(ParameterValue::getJobId, Collectors.toSet()));
    jobs.forEach(job -> job.setParameterValues(jobParameterValueMap.getOrDefault(job.getId(), new HashSet<>())));
    List<Long> processParameterIds = getProcessParameterIds(jobs);
    List<JobPartialDto> jobDtoList = jobMapper.jobToJobPartialDto(jobs, getPendingOnMeTasks(ids), processParameterIds);

    return new PageImpl<>(jobDtoList, pageable, jobPage.getTotalElements());
  }


  @Override
  public Page<JobPartialDto> getAllJobsCount(String objectId, String filters, Pageable pageable) {
    log.info("[getAllJobs] Request to get all jobs count, filters: {}, pageable: {}", filters, pageable);

    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    SearchCriteria organisationSearchCriteria = (new SearchCriteria()).setField(Job.ORGANISATION_ID).setOp(Operator.Search.EQ.toString()).setValues(Collections.singletonList(principalUser.getOrganisationId()));
    SearchCriteria facilitySearchCriteria = null;
    Long currentFacilityId = principalUser.getCurrentFacilityId();
    if (currentFacilityId != null && !currentFacilityId.equals(Misc.ALL_FACILITY_ID)) {
      facilitySearchCriteria =
        (new SearchCriteria()).setField(Job.FACILITY_ID).setOp(Operator.Search.EQ.toString()).setValues(Collections.singletonList(currentFacilityId));
    }

    /*--Fetch JobsIds wrt Specification--*/
    Specification<Job> specification;

    if (!Utility.isEmpty(objectId)) {
      Set<Long> jobIds = getJobIdsHavingObjectInChoicesForAllParameters(objectId);
      SearchCriteria jobIdsCriteria = (new SearchCriteria()).setField(Job.ID).setOp(Operator.Search.ANY.toString()).setValues(new ArrayList<>(jobIds));
      if (Utility.isEmpty(jobIds)) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
      }
      specification = SpecificationBuilder.createSpecification(filters, Arrays.asList(organisationSearchCriteria, facilitySearchCriteria, jobIdsCriteria));
    } else {
      specification = SpecificationBuilder.createSpecification(filters, Arrays.asList(organisationSearchCriteria, facilitySearchCriteria));
    }

    Page<Job> jobPage = jobRepository.findAll(specification, pageable);

    return new PageImpl<>(Collections.emptyList(), pageable, jobPage.getTotalElements());
  }

  @Override
  public Page<JobPartialDto> getJobsAssignedToMe(String objectId, String filters, Pageable pageable, Boolean showPendingOnly) {
    log.info("[getJobsAssignedToMe] Request to get jobs assigned to logged in user, filters: {}, pageable: {}", filters, pageable);

    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    SearchCriteria organisationSearchCriteria = (new SearchCriteria()).setField(Job.ORGANISATION_ID).setOp(Operator.Search.EQ.toString()).setValues(Collections.singletonList(principalUser.getOrganisationId()));
    SearchCriteria facilitySearchCriteria = null;
    Long currentFacilityId = principalUser.getCurrentFacilityId();
    if (currentFacilityId != null && !currentFacilityId.equals(Misc.ALL_FACILITY_ID)) {
      facilitySearchCriteria =
        (new SearchCriteria()).setField(Job.FACILITY_ID).setOp(Operator.Search.EQ.toString()).setValues(Collections.singletonList(currentFacilityId));
    }

    /*--Fetch JobsIds wrt Specification--*/
    Specification<Job> specification;

    if (!Utility.isEmpty(objectId)) {
      Set<Long> jobIds = getJobIdsHavingObjectInChoicesForAllParameters(objectId);
      SearchCriteria jobIdsCriteria = (new SearchCriteria()).setField(Job.ID).setOp(Operator.Search.ANY.toString()).setValues(new ArrayList<>(jobIds));
      if (Utility.isEmpty(jobIds)) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
      }
      specification = SpecificationBuilder.createSpecification(filters, Arrays.asList(organisationSearchCriteria, facilitySearchCriteria, jobIdsCriteria));
    } else {
      specification = SpecificationBuilder.createSpecification(filters, Arrays.asList(organisationSearchCriteria, facilitySearchCriteria));
    }

    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Tuple> criteriaQuery = cb.createTupleQuery();
    Root<Job> root = criteriaQuery.from(Job.class);
    Predicate combinedPredicate = cb.conjunction();

    if (showPendingOnly) {
      combinedPredicate = cb.and(combinedPredicate, addPendingTasksAssignedToCurrentUserPredicate(cb, root));
    } else {
      combinedPredicate = cb.and(combinedPredicate, addCurrentUserPredicate(cb, root));
    }
    if (specification != null) {
      combinedPredicate = cb.and(combinedPredicate, specification.toPredicate(root, criteriaQuery, cb));
    }
    criteriaQuery.select(cb.tuple(root)).where(combinedPredicate);

    if (showPendingOnly) {
      applyCustomSortPendingOnMe(cb, criteriaQuery, root);
    } else {
      applyCustomSort(cb, criteriaQuery, root);
    }

    TypedQuery<Tuple> query = entityManager.createQuery(criteriaQuery);
    List<Tuple> tuples = query.setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize()).getResultList();
    List<Job> jobs = tuples.stream().map(tuple -> tuple.get(2, Job.class)).toList();
    long total = totalCount(cb, combinedPredicate);

    Set<Long> ids = jobs.stream().map(BaseEntity::getId).collect(Collectors.toSet());
    List<Long> processParameterIds = getProcessParameterIds(jobs);
    List<JobPartialDto> jobDtoList = jobMapper.jobToJobPartialDto(jobs, getPendingOnMeTasks(ids), processParameterIds);

    return new PageImpl<>(jobDtoList, pageable, total);
  }

  @Override
  public Page<JobAutoSuggestDto> getJobsAssignedToMeAutoSuggest(String objectId, String filters, Pageable pageable, Boolean showPendingOnly) {
    log.info("[getJobsAssignedToMeAutoSuggest] Request to get jobs assigned to logged in user, filters: {}, pageable: {}", filters, pageable);

    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    SearchCriteria organisationSearchCriteria = (new SearchCriteria()).setField(Job.ORGANISATION_ID).setOp(Operator.Search.EQ.toString()).setValues(Collections.singletonList(principalUser.getOrganisationId()));
    SearchCriteria facilitySearchCriteria = null;
    Long currentFacilityId = principalUser.getCurrentFacilityId();
    if (currentFacilityId != null && !currentFacilityId.equals(Misc.ALL_FACILITY_ID)) {
      facilitySearchCriteria =
        (new SearchCriteria()).setField(Job.FACILITY_ID).setOp(Operator.Search.EQ.toString()).setValues(Collections.singletonList(currentFacilityId));
    }

    /*--Fetch JobsIds wrt Specification--*/
    Specification<Job> specification;

    if (!Utility.isEmpty(objectId)) {
      Set<Long> jobIds = getJobIdsHavingObjectInChoicesForAllParameters(objectId);
      SearchCriteria jobIdsCriteria = (new SearchCriteria()).setField(Job.ID).setOp(Operator.Search.ANY.toString()).setValues(new ArrayList<>(jobIds));
      if (Utility.isEmpty(jobIds)) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
      }
      specification = SpecificationBuilder.createSpecification(filters, Arrays.asList(organisationSearchCriteria, facilitySearchCriteria, jobIdsCriteria));
    } else {
      specification = SpecificationBuilder.createSpecification(filters, Arrays.asList(organisationSearchCriteria, facilitySearchCriteria));
    }

    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Tuple> criteriaQuery = cb.createTupleQuery();
    Root<Job> root = criteriaQuery.from(Job.class);
    Predicate combinedPredicate = cb.conjunction();

    if (showPendingOnly) {
      combinedPredicate = cb.and(combinedPredicate, addPendingTasksAssignedToCurrentUserPredicate(cb, root));
    } else {
      combinedPredicate = cb.and(combinedPredicate, addCurrentUserPredicate(cb, root));
    }
    if (specification != null) {
      combinedPredicate = cb.and(combinedPredicate, specification.toPredicate(root, criteriaQuery, cb));
    }
    criteriaQuery.select(cb.tuple(root)).where(combinedPredicate);

    TypedQuery<Tuple> query = entityManager.createQuery(criteriaQuery);
    List<Tuple> tuples = query.setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize()).getResultList();
    List<Job> jobs = tuples.stream().map(tuple -> tuple.get(0, Job.class)).toList();
    long total = totalCount(cb, combinedPredicate);

    List<JobAutoSuggestDto> jobAutoSuggestDtoList = jobMapper.jobToJobAutoSuggestDto(jobs);
    return new PageImpl<>(jobAutoSuggestDtoList, pageable, total);

  }

  @Override
  public CountDto getJobsAssignedToMeCount(String objectId, String filters, Boolean showPendingOnly) {
    log.info("[getJobsAssignedToMeCount] Request to get jobs count assigned to logged in user, filters: {}", filters);

    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    SearchCriteria organisationSearchCriteria = (new SearchCriteria()).setField(Job.ORGANISATION_ID).setOp(Operator.Search.EQ.toString()).setValues(Collections.singletonList(principalUser.getOrganisationId()));
    SearchCriteria facilitySearchCriteria = null;
    Long currentFacilityId = principalUser.getCurrentFacilityId();
    if (!Misc.ALL_FACILITY_ID.equals(currentFacilityId)) {
      facilitySearchCriteria =
        (new SearchCriteria()).setField(Job.FACILITY_ID).setOp(Operator.Search.EQ.toString()).setValues(Collections.singletonList(currentFacilityId));
    }

    Specification<Job> specification;

    if (!Utility.isEmpty(objectId)) {
      Set<Long> jobIds = getJobIdsHavingObjectInChoicesForAllParameters(objectId);
      SearchCriteria jobIdsCriteria = (new SearchCriteria()).setField(Job.ID).setOp(Operator.Search.ANY.toString()).setValues(new ArrayList<>(jobIds));
      if (Utility.isEmpty(jobIds)) {
        CountDto countDto = new CountDto();
        countDto.setCount(String.valueOf(0));
        return countDto;
      }
      specification = SpecificationBuilder.createSpecification(filters, Arrays.asList(organisationSearchCriteria, facilitySearchCriteria, jobIdsCriteria));
    } else {
      specification = SpecificationBuilder.createSpecification(filters, Arrays.asList(organisationSearchCriteria, facilitySearchCriteria));
    }

    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Tuple> criteriaQuery = cb.createTupleQuery();
    Root<Job> root = criteriaQuery.from(Job.class);
    Predicate combinedPredicate = cb.conjunction();

    if (showPendingOnly) {
      combinedPredicate = cb.and(combinedPredicate, addPendingTasksAssignedToCurrentUserPredicate(cb, root));
    } else {
      combinedPredicate = cb.and(combinedPredicate, addCurrentUserPredicate(cb, root));
    }
    if (specification != null) {
      combinedPredicate = cb.and(combinedPredicate, specification.toPredicate(root, criteriaQuery, cb));
    }
    criteriaQuery.select(cb.tuple(root)).where(combinedPredicate);
    TypedQuery<Tuple> query = entityManager.createQuery(criteriaQuery);
    long jobsCount = totalCount(cb, combinedPredicate);

    CountDto countDto = new CountDto();
    countDto.setCount(String.valueOf(jobsCount));
    return countDto;
  }

  @Override
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
  public JobDto createJob(CreateJobRequest createJobRequest, Boolean validateUserRole) throws StreemException, IOException, ResourceNotFoundException, MultiStatusException {
    log.info("[createJob] Request to create a job, createJobRequest: {}", createJobRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Long currentFacilityId = principalUser.getCurrentFacilityId();
    Facility facility = facilityRepository.getReferenceById(currentFacilityId);
    if (Objects.equals(principalUser.getCurrentFacilityId(), null)) {
      ValidationUtils.invalidate(principalUser.getId(), ErrorCode.JOB_CANNOT_BE_CREATED_FROM_ALL_FACILITY);
    }

    if (validateUserRole) {
      boolean isOperator = principalUser.getRoles().stream()
        .anyMatch(obj -> Objects.equals(obj.getName(), "OPERATOR"));

      boolean isDefaultUser = trainedUserRepository.verifyUserIsAssignedToTheChecklist(createJobRequest.getChecklistId(), principalUser.getId());

      if (isOperator && !isDefaultUser) {
        ValidationUtils.invalidate(createJobRequest.getChecklistId(), ErrorCode.USER_NOT_ALLOWED_TO_CREATE_JOB);
      }
    }
    return createJobService.createJob(createJobRequest, principalUser, facility, false, null, null);
  }

  @Override
  public BasicDto updateJob(Long jobId, UpdateJobRequest updateJobRequest) throws ResourceNotFoundException, StreemException {
    log.info("[updateJob] Request to update job, jobId: {}, updateJobRequest: {}", jobId, updateJobRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());

    Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException(jobId, ErrorCode.JOB_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    validateJobState(jobId, Action.Job.UPDATE, job.getState());

    if (!Utility.isNull(updateJobRequest.getExpectedStartDate())) {
      job.setExpectedStartDate(updateJobRequest.getExpectedStartDate());
      //TODO: remove dely events if present any - check if job StartTime can be changed
      jobDelayEmailDispatchService.addJobDelayEmailDispatchEvent(job);
    }

    if (!Utility.isNull(updateJobRequest.getExpectedEndDate())) {
      job.setExpectedEndDate(updateJobRequest.getExpectedEndDate());
      jobDelayEmailDispatchService.addJobOverDueEmailDispatchEvent(job);
    }

    if (DateTimeUtils.isDateAfter(job.getExpectedStartDate(), job.getExpectedEndDate())) {
      ValidationUtils.invalidate(jobId, ErrorCode.JOB_EXPECTED_START_DATE_CANNOT_BE_AFTER_EXPECTED_END_DATE);
    }

    if (DateTimeUtils.isDateInPast(job.getExpectedStartDate())) {
      ValidationUtils.invalidate(jobId, ErrorCode.JOB_EXPECTED_START_DATE_CANNOT_BE_A_PAST_DATE);
    }

    if (DateTimeUtils.isDateInPast(job.getExpectedEndDate())) {
      ValidationUtils.invalidate(jobId, ErrorCode.JOB_EXPECTED_END_DATE_CANNOT_BE_A_PAST_DATE);
    }

    job.setModifiedAt(DateTimeUtils.now());
    job.setModifiedBy(principalUserEntity);
    jobRepository.save(job);

    jobLogService.updateJobState(job.getIdAsString(), principalUser);
    BasicDto basicDto = new BasicDto();
    basicDto.setMessage("success");
    return basicDto;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JobInfoDto startJob(Long jobId) throws ResourceNotFoundException, StreemException, IOException, ParameterExecutionException {
    log.info("[startJob] Request to start job, jobId: {}", jobId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());
    Facility facility = facilityRepository.getReferenceById(principalUser.getCurrentFacilityId());
    String facilityTimeZone = facility.getTimeZone();
    Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException(jobId, ErrorCode.JOB_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    validateIfUserIsAssignedToExecuteJob(jobId, principalUser.getId());
    validateJobState(jobId, Action.Job.START, job.getState());

    job.setStartedAt(DateTimeUtils.now());
    job.setStartedBy(principalUserEntity);
    job.setState(State.Job.IN_PROGRESS);
    job.setModifiedBy(principalUserEntity);
    JobInfoDto jobDto = jobMapper.toJobInfoDto(jobRepository.save(job), principalUser);

    List<ParameterValue> cjfParameterValues = parameterValueRepository.findAllByJobIdAndTargetEntityType(Collections.singleton(jobId), Type.ParameterTargetEntityType.PROCESS);
    // todo only cjf
    List<Error> errorList = new ArrayList<>();
    //TODO: remove n+1 queries for getting cjf parameters
    for (ParameterValue parameterValue : cjfParameterValues) {
      if (!parameterValue.isHidden() && parameterValue.getState() != State.ParameterExecution.EXECUTED) {
        try {
          Parameter parameter = parameterRepository.getReferenceById(parameterValue.getParameterId());
          JsonNode validations = parameter.getValidations();
          if (!Utility.isEmpty(validations)) {
            List<ParameterValidationDto> parameterValidationDtoList = JsonUtils.jsonToCollectionType(validations, List.class, ParameterValidationDto.class);

            if (!(Utility.isEmpty(parameterValidationDtoList) && parameter.getType() == Type.Parameter.NUMBER) && !parameterValue.getState().equals(State.ParameterExecution.NOT_STARTED)) {
              parameterExecutionValidationService.validateNumberParameterValidations(jobId, parameterValue.getId(), parameterValue.getParameterId(), validations, parameterValue.getValue());
            }
            if ((!Utility.isEmpty(parameterValidationDtoList) && parameter.getType() == Type.Parameter.RESOURCE || parameter.getType() == Type.Parameter.MULTI_RESOURCE) && !parameterValue.getState().equals(State.ParameterExecution.NOT_STARTED)) {
              List<ResourceParameterChoiceDto> choices = JsonUtils.jsonToCollectionType(parameterValue.getChoices(), List.class, ResourceParameterChoiceDto.class);


              ResourceParameter resourceParameter = JsonUtils.readValue(parameter.getData().toString(), ResourceParameter.class);

              for (ResourceParameterChoiceDto resourceParameterChoiceDto : choices){
                parameterExecutionValidationService.validateParameterValueChoice(resourceParameterChoiceDto.getObjectId(), resourceParameter.getObjectTypeExternalId(), validations, parameter.getIdAsString(), jobId, false);
              }
            }
            if (!(Utility.isEmpty(parameterValidationDtoList) && (parameter.getType() == Type.Parameter.DATE || parameter.getType() == Type.Parameter.DATE_TIME)) && !parameterValue.getState().equals(State.ParameterExecution.NOT_STARTED)) {
              boolean isDateTimeParameter = parameter.getType() == Type.Parameter.DATE_TIME;
              parameterExecutionValidationService.validateDateAndDateTimeParameterValidations(jobId, validations, parameterValue.getValue(), isDateTimeParameter, facilityTimeZone, parameter.getId());
            }

          }
        } catch (ParameterExecutionException e) {
          errorList.addAll(e.getErrorList());
        }
      }
    }

    if (!Utility.isEmpty(errorList)) {
      throw new ParameterExecutionException(errorList);
    }

    List<TaskExecution> taskExecutionsWithJobSchedule = taskExecutionRepository.findAllTaskExecutionsWithJobSchedule(job.getId());

    for (TaskExecution taskExecution : taskExecutionsWithJobSchedule) {
      TaskSchedules taskSchedules = taskSchedulesRepository.getReferenceById(taskExecution.getTask().getTaskSchedulesId());
      taskExecution.setScheduled(true);
      taskExecution.setSchedulingExpectedStartedAt(job.getStartedAt() + taskSchedules.getStartDateInterval());
      taskExecution.setSchedulingExpectedDueAt(job.getStartedAt() + taskSchedules.getStartDateInterval() + taskSchedules.getDueDateInterval());
      taskExecution.setModifiedBy(principalUserEntity);
      taskExecution.setModifiedAt(DateTimeUtils.now());
    }

    taskExecutionRepository.saveAll(taskExecutionsWithJobSchedule);

    jobDto.setScheduledTaskExecutionIds(taskExecutionsWithJobSchedule.stream().map(BaseEntity::getId).map(String::valueOf).collect(Collectors.toSet()));

    if (!Utility.isEmpty(job.getExpectedStartDate()) && job.getStartedAt() < job.getExpectedStartDate()) {
      String expectedStartAt = DateTimeUtils.getFormattedDateTimeForFacility(job.getExpectedStartDate(), facility);
      jobAuditService.startJobEarly(jobDto, principalUser, expectedStartAt);
    } else {
      jobAuditService.startJob(jobDto, principalUser);
    }
    Set<String> taskScheduledIds = taskExecutionsWithJobSchedule.stream().map(taskExecution -> taskExecution.getIdAsString()).collect(Collectors.toSet());
    if (!Utility.isEmpty(taskScheduledIds)) {
      jobAuditService.scheduleTask(jobId, null, principalUser, true, true, taskScheduledIds);
    }
    UserAuditDto userAuditDto = userMapper.toUserAuditDto(principalUserEntity);
    jobLogService.recordJobLogTrigger(job.getIdAsString(), JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_STARTED_BY, JobLogMisc.JOB, null,
      Utility.getFullNameAndEmployeeIdFromPrincipalUser(principalUser), principalUser.getIdAsString(), userAuditDto);
    jobLogService.recordJobLogTrigger(job.getIdAsString(), JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_START_TIME, JobLogMisc.JOB, null,
      String.valueOf(job.getStartedAt()), String.valueOf(job.getStartedAt()), userAuditDto);
    jobLogService.updateJobState(String.valueOf(jobId), principalUser);
    Set<Long> preRequisiteTaskIds = job.getTaskExecutions().stream().map(TaskExecution::getTaskId).collect(Collectors.toSet());
    for (Long preRequisiteTaskId : preRequisiteTaskIds) {
      notificationService.notifyIfAllPrerequisiteTasksCompleted(preRequisiteTaskId, jobId, principalUser.getOrganisationId());
    }

    return jobDto;
  }

  @Override
  public JobInfoDto completeJob(Long jobId) throws ResourceNotFoundException, StreemException {
    log.info("[completeJob] Request to complete job, jobId: {}", jobId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException(jobId, ErrorCode.JOB_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    validateIfUserIsAssignedToExecuteJob(jobId, principalUser.getId());

    JobInfoDto jobDto = completeJob(job, principalUser, principalUserEntity);

    if (job.isScheduled()) {
      try {
        createJobService.createScheduledJob(job.getSchedulerId(), job.getExpectedStartDate());
      } catch (Exception ex) {
        log.error("[completeJobWithException] error creating a scheduled job", ex);
      }
    }
    return jobDto;
  }

  @Transactional(rollbackFor = Exception.class)
  public JobInfoDto completeJob(Job job, PrincipalUser principalUser, User principalUserEntity) throws StreemException {
    validateJobState(job.getId(), Action.Job.COMPLETE, job.getState());
    validateMandatoryParametersIncomplete(job.getId());
    validateIfTasksBelongToCompletedStates(job.getId());
    job.setEndedAt(DateTimeUtils.now());
    job.setEndedBy(principalUserEntity);
    job.setModifiedBy(principalUserEntity);
    job.setState(State.Job.COMPLETED);
    validateIfUserIsAssignedToExecuteJob(job.getId(), principalUser.getId());
    JobInfoDto jobDto = jobMapper.toJobInfoDto(jobRepository.save(job), principalUser);

    stageReportService.unregisterStagesForJob(job.getId());
    jobAuditService.completeJob(jobDto, principalUser);
    jobLogService.recordJobLogTrigger(job.getIdAsString(), JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_ENDED_BY, JobLogMisc.JOB, null,
      Utility.getFullNameAndEmployeeIdFromPrincipalUser(principalUser), principalUser.getIdAsString(), userMapper.toUserAuditDto(principalUserEntity));
    jobLogService.recordJobLogTrigger(job.getIdAsString(), JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_END_TIME, JobLogMisc.JOB, null,
      String.valueOf(job.getEndedAt()), String.valueOf(job.getEndedAt()), userMapper.toUserAuditDto(principalUserEntity));
    jobLogService.updateJobState(String.valueOf(job.getId()), principalUser);

    return jobDto;
  }

  @Override
  public JobInfoDto completeJobWithException(Long jobId, JobCweDetailRequest jobCweDetailRequest) throws ResourceNotFoundException, StreemException {
    log.info("[completeJobWithException] Request to complete job with exception, jobId: {}, jobCweDetailRequest: {}", jobId, jobCweDetailRequest);
    if (Utility.isEmpty(jobCweDetailRequest.getComment())) {
      ValidationUtils.invalidate(jobId, ErrorCode.COMMENT_TO_COMPLETE_JOB_WITH_EXCEPTION_CANNOT_BE_EMPTY);
    }

    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException(jobId, ErrorCode.JOB_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    JobInfoDto jobInfoDto = completeJobWithException(job, principalUserEntity, jobCweDetailRequest, principalUser);

    if (job.isScheduled()) {
      try {
        createJobService.createScheduledJob(job.getSchedulerId(), job.getExpectedStartDate());
      } catch (Exception ex) {
        log.error("[completeJobWithException] error creating a scheduled job", ex);
      }
    }

    return jobInfoDto;
  }

  // TODO this is a workaround, some strange behaviour transactional instance isn't getting passed to createScheduledJob
  // so we are creating a new transactional instance here and created a separate method
  @Transactional(rollbackFor = Exception.class)
  public JobInfoDto completeJobWithException(Job job, User principalUserEntity, JobCweDetailRequest jobCweDetailRequest, PrincipalUser principalUser) throws StreemException, ResourceNotFoundException {
    validateJobState(job.getId(), Action.Job.COMPLETE_WITH_EXCEPTION, job.getState());
    validateIfTasksBelongToCompletedStates(job.getId());
    String exceptionReason = jobCweDetailRequest.getReason().get();
    String comment = jobCweDetailRequest.getComment();
    jobCweService.createJobCweDetail(jobCweDetailRequest, job, principalUserEntity);

    job.setState(State.Job.COMPLETED_WITH_EXCEPTION);
    job.setEndedAt(DateTimeUtils.now());
    job.setEndedBy(principalUserEntity);
    job.setModifiedBy(principalUserEntity);

    JobInfoDto jobInfoDto = jobMapper.toJobInfoDto(jobRepository.save(job), principalUser);

    stageReportService.unregisterStagesForJob(job.getId());
    jobAuditService.completeJobWithException(job.getId(), jobCweDetailRequest, principalUser);

    UserAuditDto userAuditDto = userMapper.toUserAuditDto(principalUserEntity);
    jobLogService.recordJobLogTrigger(job.getIdAsString(), JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_ENDED_BY, JobLogMisc.JOB, null,
      Utility.getFullNameAndEmployeeIdFromPrincipalUser(principalUser), principalUser.getIdAsString(), userAuditDto);
    jobLogService.recordJobLogTrigger(job.getIdAsString(), JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_END_TIME, JobLogMisc.JOB, null,
      String.valueOf(job.getEndedAt()), String.valueOf(job.getEndedAt()), userAuditDto);
    jobLogService.recordJobLogTrigger(job.getIdAsString(), JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_CWE_REASON, JobLogMisc.JOB, null, exceptionReason, null, userAuditDto);
    jobLogService.recordJobLogTrigger(job.getIdAsString(), JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_CWE_COMMENT, JobLogMisc.JOB, null, comment, null, userAuditDto);
    if (!Utility.isEmpty(jobCweDetailRequest.getMedias())) {
      List<Long> mediaIds = jobCweDetailRequest.getMedias().stream()
        .map(MediaRequest::getMediaId)
        .collect(Collectors.toList());
      List<Media> medias = mediaRepository.findAllById(mediaIds);
      List<JobLogMediaData> jobLogMedias = jobAnnotationService.getJobLogMediaData(medias);
      jobLogService.recordJobLogTrigger(job.getIdAsString(), JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_CWE_FILE, JobLogMisc.JOB, jobLogMedias, null, null, userAuditDto);
    }
    jobLogService.updateJobState(String.valueOf(job.getId()), principalUser);

    return jobInfoDto;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public BasicDto bulkAssign(Long jobId, TaskExecutionAssignmentRequest taskExecutionAssignmentRequest, boolean notify) throws ResourceNotFoundException, StreemException, MultiStatusException {
    log.info("[bulkAssign] Request to bulk assign tasks, jobId: {}, taskExecutionAssignmentRequest: {}, notify: {}", jobId, taskExecutionAssignmentRequest, notify);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    jobAssignmentService.assignUsers(jobId, taskExecutionAssignmentRequest, notify, principalUser);
    Set<Long> allAssignedIds = taskExecutionAssignmentRequest.getAssignedUserIds();
    Set<Long> allUnassignedIds = taskExecutionAssignmentRequest.getUnassignedUserIds();
    Set<Long> allAssignedUserGroupIds = taskExecutionAssignmentRequest.getAssignedUserGroupIds();
    Set<Long> allUnassignedUserGroupIds = taskExecutionAssignmentRequest.getUnassignedUserGroupIds();

    Long checklistId = jobRepository.getChecklistIdByJobId(jobId);

    if (taskExecutionAssignmentRequest.isAllUsersSelected()) {
      allAssignedIds.addAll(trainedUserRepository.findAllUserIdsByChecklistId(checklistId));
    }

    if (taskExecutionAssignmentRequest.isAllUserGroupsSelected()) {
      allAssignedUserGroupIds.addAll(trainedUserRepository.findAllUserGroupIdsByChecklistId(checklistId));
    }

    if (!allAssignedIds.isEmpty() || !allUnassignedIds.isEmpty() || !allAssignedUserGroupIds.isEmpty() || !allUnassignedUserGroupIds.isEmpty()) {
      jobAuditService.bulkAssignUsersToJob(jobId, !allAssignedIds.isEmpty(), !allUnassignedIds.isEmpty(), !allAssignedUserGroupIds.isEmpty(), !allUnassignedUserGroupIds.isEmpty(), principalUser);
    }
    BasicDto basicDto = new BasicDto();
    basicDto.setMessage("success");
    return basicDto;
  }

  @Override
  public List<TaskExecutionAssigneeDetailsView> getAssignees(Long jobId) {
    return taskExecutionAssigneeRepository.findByJobId(jobId, taskExecutionRepository.getTaskExecutionCountByJobId(jobId));
  }

  @Override
  public JobReportDto getJobReport(Long jobId) throws ResourceNotFoundException, JsonProcessingException {
    Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException(jobId, ErrorCode.JOB_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    JobReportDto jobReportDto = jobMapper.toJobReportDto(job);

    if (State.Job.COMPLETED_WITH_EXCEPTION.equals(job.getState())) {
      jobReportDto.setCweDetails(jobCweService.getJobCweDetail(jobId));
    }

    List<Stage> stages = stageRepository.findStagesByJobIdAndAllTaskExecutionStateIn(jobId, State.TASK_COMPLETED_STATES);
    List<StageReportDto> stageReportDtos = new ArrayList<>();

    Map<Long, StageReportDto> stageReportDtoMap = new HashMap<>();
    for (Stage stage : stages) {
      StageReportDto stageReportDto = stageMapper.toStageReportDto(stage);
      stageReportDtoMap.put(stage.getId(), stageReportDto);
      stageReportDtos.add(stageReportDto);
    }

    Set<Long> stageIds = stages.stream().map(BaseEntity::getId).collect(Collectors.toSet());

    List<TaskExecution> taskExecutions = taskExecutionRepository.findByJobIdAndStageIdIn(jobId, stageIds);
    Map<Long, TaskReportDto> taskReportDtoMap = new HashMap<>();
    Map<Long, TaskExecution> taskExecutionMap = new HashMap<>();

    List<Long> taskIds = taskExecutions.stream().map(te -> te.getTask().getId()).collect(Collectors.toList());
    List<ParameterValue> shouldBeParameterExecutions = parameterValueRepository.findByJobIdAndTaskIdParameterTypeIn(jobId, taskIds, Collections.singletonList(Type.Parameter.SHOULD_BE));
    Map<Long, List<ParameterValue>> taskIdShouldBeParameterValueMap = shouldBeParameterExecutions.stream()
      .collect(Collectors.groupingBy(av -> av.getParameter().getTask().getId(), Collectors.mapping(av -> av, Collectors.toList())));

    List<ParameterValue> yesNoParameterExecutions = parameterValueRepository.findByJobIdAndTaskIdParameterTypeIn(jobId, taskIds, Collections.singletonList(Type.Parameter.YES_NO));
    List<ParameterValue> noParameterExecutions = yesNoParameterExecutions.stream()
      .filter(parameterValue -> !Utility.isEmpty(parameterValue.getReason())).toList();
    Map<Long, List<ParameterValue>> taskIdNoParameterValueMap = noParameterExecutions.stream()
      .collect(Collectors.groupingBy(av -> av.getParameter().getTask().getId(), Collectors.mapping(av -> av, Collectors.toList())));

    long totalDuration = 0;
    int totalTasks = 0;
    //calculated throughout all the tasks
    int totalExceptionsInJob = 0;

    long minStartedAtForStage = Long.MAX_VALUE;
    long maxEndedAtForStage = Long.MIN_VALUE;

    Long totalStageDuration = null;
    Long currentStageId = null;
    Set<AssigneeSignOffDto> assignees = new HashSet<>();
    Map<String, Long> recentSignOffDetails = new HashMap<>();
    for (TaskExecution taskExecution : taskExecutions) {
      taskExecutionMap.put(taskExecution.getId(), taskExecution);
      var taskExecutionUserMappings = taskExecution.getAssignees();
      for (var taskExecutionUserMapping : taskExecutionUserMappings) {
        setAssignees(assignees, recentSignOffDetails, taskExecutionUserMapping);
      }

      Task task = taskExecution.getTask();
      Stage stage = task.getStage();
      TaskReportDto taskReportDto = null;

      if (currentStageId == null) {
        currentStageId = stage.getId();
      } else if (!stage.getId().equals(currentStageId)) {
        if (totalStageDuration == null) {
          totalStageDuration = maxEndedAtForStage - minStartedAtForStage;
        } else {
          totalStageDuration += (maxEndedAtForStage - minStartedAtForStage);
        }
        stageReportDtoMap.get(currentStageId).setTotalDuration(maxEndedAtForStage - minStartedAtForStage);
        stageReportDtoMap.get(currentStageId).setAverageTaskCompletionDuration(totalDuration / totalTasks);
        currentStageId = stage.getId();
        totalDuration = 0;
        totalTasks = 0;
        minStartedAtForStage = Long.MAX_VALUE;
        maxEndedAtForStage = Long.MIN_VALUE;
      }
      totalTasks++;
      totalDuration += (taskExecution.getEndedAt() - taskExecution.getStartedAt());

      maxEndedAtForStage = maxEndedAtForStage < taskExecution.getEndedAt() ? taskExecution.getEndedAt() : maxEndedAtForStage;
      minStartedAtForStage = minStartedAtForStage > taskExecution.getStartedAt() ? taskExecution.getStartedAt() : minStartedAtForStage;

      UserAuditDto userAuditDto = userMapper.toUserAuditDto(taskExecution.getModifiedBy());
      UserAuditDto correctedByUserAuditDto = userMapper.toUserAuditDto(taskExecution.getCorrectedBy());

      if (State.TASK_EXECUTION_EXCEPTION_STATE.contains(taskExecution.getState()) || taskExecutionService.isInvalidTimedTaskCompletedState(task, taskExecution.getStartedAt(),
        taskExecution.getEndedAt()) || taskExecution.getCorrectionReason() != null) {
        if (taskReportDtoMap.containsKey(task.getId())) {
          taskReportDto = taskReportDtoMap.get(task.getId());
        } else {
          taskReportDto = taskMapper.toTaskReportDto(task);
          taskReportDtoMap.put(task.getId(), taskReportDto);
          stageReportDtoMap.get(stage.getId()).getTasks().add(taskReportDto);
        }
        totalExceptionsInJob++;
        stageReportDtoMap.get(currentStageId).setTotalTaskExceptions(stageReportDtoMap.get(stage.getId()).getTotalTaskExceptions() + 1);
      }

      if (State.TaskExecution.COMPLETED_WITH_EXCEPTION.equals(taskExecution.getState())) {
        taskReportDto.getExceptions().add(createTaskExceptionDtoForTask(taskExecution, userAuditDto, Type.TaskException.COMPLETED_WITH_EXCEPTION, null));
      } else if (State.TaskExecution.SKIPPED.equals(taskExecution.getState())) {
        taskReportDto.getExceptions().add(createTaskExceptionDtoForTask(taskExecution, userAuditDto, Type.TaskException.SKIPPED, null));
      } else if (taskExecution.getCorrectionReason() != null) {
        taskReportDto.getExceptions().add(createTaskExceptionDtoForTask(taskExecution, correctedByUserAuditDto, Type.TaskException.ERROR_CORRECTION, null));
      } else if (taskExecutionService.isInvalidTimedTaskCompletedState(task, taskExecution.getStartedAt(), taskExecution.getEndedAt())) {
        userAuditDto = userMapper.toUserAuditDto(taskExecution.getStartedBy());
        TaskTimerDto taskTimerDto = new TaskTimerDto()
          .setEndedAt(taskExecution.getEndedAt())
          .setStartedAt(taskExecution.getStartedAt())
          .setTimerOperator(task.getTimerOperator())
          .setMinPeriod(task.getMinPeriod())
          .setMaxPeriod(task.getMaxPeriod());

        taskReportDto.getExceptions().add(createTaskExceptionDtoForTask(taskExecution, userAuditDto, Type.TaskException.DURATION_EXCEPTION, taskTimerDto));
      }

      if (taskIdShouldBeParameterValueMap.containsKey(task.getId())) {
        for (ParameterValue parameterValue : taskIdShouldBeParameterValueMap.get(task.getId())) {
          boolean parameterDeviationFound = false;
          TaskExceptionDto taskExceptionDto = null;

          userAuditDto = userMapper.toUserAuditDto(parameterValue.getModifiedBy());

          if (!Utility.isEmpty(parameterValue.getValue())) {
            ShouldBeParameter shouldBeParameter = JsonUtils.readValue(parameterValue.getParameter().getData().toString(), ShouldBeParameter.class);
            Operator.Parameter operator = Operator.Parameter.valueOf(shouldBeParameter.getOperator());

            double lowerValue = Utility.isEmpty(shouldBeParameter.getLowerValue()) ? 0 : Double.parseDouble(shouldBeParameter.getLowerValue());
            double upperValue = Utility.isEmpty(shouldBeParameter.getUpperValue()) ? 0 : Double.parseDouble(shouldBeParameter.getUpperValue());
            double value = Utility.isEmpty(shouldBeParameter.getValue()) ? 0 : Double.parseDouble(shouldBeParameter.getValue());
            double userInput = Double.parseDouble(parameterValue.getValue());

            switch (operator) {
              case BETWEEN:
                if (userInput < lowerValue || userInput > upperValue) {
                  parameterDeviationFound = true;
                  taskExceptionDto = createTaskExceptionDtoForParameterDeviation(parameterValue, userAuditDto, userInput);
                }
                break;
              case EQUAL_TO:
                if (userInput != value) {
                  parameterDeviationFound = true;
                  taskExceptionDto = createTaskExceptionDtoForParameterDeviation(parameterValue, userAuditDto, userInput);
                }
                break;
              case LESS_THAN:
                if (userInput >= value) {
                  parameterDeviationFound = true;
                  taskExceptionDto = createTaskExceptionDtoForParameterDeviation(parameterValue, userAuditDto, userInput);
                }
                break;
              case LESS_THAN_EQUAL_TO:
                if (userInput > value) {
                  parameterDeviationFound = true;
                  taskExceptionDto = createTaskExceptionDtoForParameterDeviation(parameterValue, userAuditDto, userInput);
                }
                break;
              case MORE_THAN:
                if (userInput <= value) {
                  parameterDeviationFound = true;
                  taskExceptionDto = createTaskExceptionDtoForParameterDeviation(parameterValue, userAuditDto, userInput);
                }
                break;
              case MORE_THAN_EQUAL_TO:
                if (userInput < value) {
                  parameterDeviationFound = true;
                  taskExceptionDto = createTaskExceptionDtoForParameterDeviation(parameterValue, userAuditDto, userInput);
                }
                break;
            }
          }

          if (parameterDeviationFound) {
            if (taskReportDtoMap.containsKey(task.getId())) {
              taskReportDto = taskReportDtoMap.get(task.getId());
            } else {
              taskReportDto = taskMapper.toTaskReportDto(task);
              taskReportDtoMap.put(task.getId(), taskReportDto);
              stageReportDtoMap.get(stage.getId()).getTasks().add(taskReportDto);
            }
            totalExceptionsInJob++;
            stageReportDtoMap.get(stage.getId()).setTotalTaskExceptions(stageReportDtoMap.get(stage.getId()).getTotalTaskExceptions() + 1);
            taskReportDto.getExceptions().add(taskExceptionDto);
          }
        }
      }
      if (taskIdNoParameterValueMap.containsKey(task.getId())) {
        for (ParameterValue parameterValue : taskIdNoParameterValueMap.get(task.getId())) {
          userAuditDto = userMapper.toUserAuditDto(parameterValue.getModifiedBy());
          TaskExceptionDto taskExceptionDto = createTaskExceptionDtoForYesNoType(parameterValue, userAuditDto);
          if (taskReportDtoMap.containsKey(task.getId())) {
            taskReportDto = taskReportDtoMap.get(task.getId());
          } else {
            taskReportDto = taskMapper.toTaskReportDto(task);
            taskReportDtoMap.put(task.getId(), taskReportDto);
            stageReportDtoMap.get(stage.getId()).getTasks().add(taskReportDto);
          }
          totalExceptionsInJob++;
          stageReportDtoMap.get(stage.getId()).setTotalTaskExceptions(stageReportDtoMap.get(stage.getId()).getTotalTaskExceptions() + 1);
          taskReportDto.getExceptions().add(taskExceptionDto);
        }
      }
    }

    var assigneeSignOffDtos = getAssigneeSignOffDtos(assignees, recentSignOffDetails);
    jobReportDto.setAssignees(assigneeSignOffDtos);

    long totalStages = 0L;
    long taskCount = 0L;
    for (var stage : job.getChecklist().getStages()) {
      totalStages++;
      taskCount = taskCount + stage.getTasks().size();
    }
    jobReportDto.setTotalStages(totalStages);
    jobReportDto.setTotalTask(taskCount);

    if (currentStageId != null) {
      if (totalStageDuration == null) {
        totalStageDuration = maxEndedAtForStage - minStartedAtForStage;
      } else {
        totalStageDuration += (maxEndedAtForStage - minStartedAtForStage);
      }
      stageReportDtoMap.get(currentStageId).setTotalDuration(maxEndedAtForStage - minStartedAtForStage);
      stageReportDtoMap.get(currentStageId).setAverageTaskCompletionDuration(totalDuration / totalTasks);
    }

    if (State.JOB_COMPLETED_STATES.contains(job.getState())) {
      UserAuditDto userAuditDto = userMapper.toUserAuditDto(job.getModifiedBy());
      jobReportDto.setCompletedBy(userAuditDto);
      jobReportDto.setEndedBy(userAuditDto);

      if (job.getStartedAt() != null && job.getEndedAt() != null) {
        jobReportDto.setTotalDuration(job.getEndedAt() - job.getStartedAt());
      }
    }

    List<ParameterValue> jobParameterValues = parameterValueRepository.findByJobIdAndParameterTargetEntityTypeIn(jobId, Collections.singletonList(Type.ParameterTargetEntityType.PROCESS));
    Map<Long, List<ParameterValue>> jobParameterValuesMap = new HashMap<>();
    Set<Parameter> jobParameters = new HashSet<>();
    for (ParameterValue av : jobParameterValues) {
      var parameter = av.getParameter();
      jobParameters.add(parameter);
      jobParameterValuesMap.computeIfAbsent(parameter.getId(), k -> new ArrayList<>()).add(av);
    }


    List<ParameterDto> parameterDtos = parameterMapper.toDto(jobParameters, jobParameterValuesMap, taskExecutionMap, null, new HashMap<>(), null, new HashMap<>());
    jobReportDto.setParameterValues(parameterDtos);

    UserAuditDto userAuditDto = userMapper.toUserAuditDto(job.getCreatedBy());
    jobReportDto.setCreatedBy(userAuditDto);

    jobReportDto.setTotalStageDuration(totalStageDuration);
    jobReportDto.setTotalAssignees(taskExecutionAssigneeRepository.getJobAssigneesCount(jobId));
    jobReportDto.setTotalTaskExceptions(totalExceptionsInJob);
    jobReportDto.setStages(stageReportDtos);
    return jobReportDto;
  }

  @Override
  public JobInformationDto getJobInformation(Long jobId) throws ResourceNotFoundException {
    Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException(jobId, ErrorCode.JOB_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    boolean isCJFExceptionPending = parameterExceptionRepository.isCJFExceptionPendingOnUser(jobId);
    boolean isExceptionRejectedOnCjf = parameterExceptionRepository.isExceptionRejectedOnCjf(jobId);
    Set<Long> jobIds = new HashSet<>();
    jobIds.add(job.getId());
    List<Long> processParameterIds = getProcessParameterIds(Stream.of(job).collect(Collectors.toList()));
    JobInformationDto jobInformationDto = jobMapper.toJobInformationDto(job, getPendingOnMeTasks(jobIds), getEngagedUsersOfJob(jobIds), processParameterIds);
    List<JobAnnotationDto> jobAnnotationDtoList = jobAnnotationMapper.toDto(jobAnnotationRepository.findByJobId(jobId));
    jobInformationDto.setStartedBy(userMapper.toDto(job.getStartedBy()));
    jobInformationDto.setCreatedBy(userMapper.toDto(job.getCreatedBy()));
    jobInformationDto.setEndedBy(userMapper.toDto(job.getEndedBy()));
    jobInformationDto.setJobAnnotationDto(jobAnnotationDtoList);
    jobInformationDto.setShowCJFExceptionBanner(isCJFExceptionPending);
    jobInformationDto.setForceCWE(isExceptionRejectedOnCjf);
    return jobInformationDto;
  }

  private List<AssigneeSignOffDto> getAssigneeSignOffDtos(Set<AssigneeSignOffDto> assignees, Map<String, Long> recentSignOffDetails) {
    return assignees.stream().sorted(Comparator.comparing(u -> u.getFirstName() + u.getLastName()))
      .map(a -> {
        Long value = recentSignOffDetails.get(a.getId());
        a.setRecentSignOffAt(value);
        return a;
      }).toList();
  }

  private void setAssignees(Set<AssigneeSignOffDto> assignees, Map<String, Long> recentSignOffDetails, TaskExecutionUserMapping taskExecutionUserMapping) {
    var assigneeSignOffDto = userMapper.toAssigneeSignOffDto(taskExecutionUserMapping.getUser());
    if (!Utility.isEmpty(assigneeSignOffDto)) {
      assignees.add(assigneeSignOffDto);
    }
    if (taskExecutionUserMapping.getState().equals(State.TaskExecutionAssignee.SIGNED_OFF)) {
      Long value = recentSignOffDetails.get(assigneeSignOffDto.getId());
      if (value == null || value < taskExecutionUserMapping.getModifiedAt()) {
        assigneeSignOffDto.setRecentSignOffAt(taskExecutionUserMapping.getModifiedAt());
        recentSignOffDetails.put(assigneeSignOffDto.getId(), taskExecutionUserMapping.getModifiedAt());
      }
    } else {
      if (!Utility.isEmpty(assigneeSignOffDto) && !recentSignOffDetails.containsKey(assigneeSignOffDto.getId())) {
        recentSignOffDetails.put(assigneeSignOffDto.getId(), null);
      }
    }
  }

  @Override
  public JobReportDto printJobReport(Long jobId) throws ResourceNotFoundException, JsonProcessingException {
    var principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    var jobReportDto = getJobReport(jobId);
    jobAuditService.printJobReport(jobReportDto, principalUser);
    return jobReportDto;
  }

  private TaskExceptionDto createTaskExceptionDtoForTask(TaskExecution taskExecution, UserAuditDto userAuditDto,
                                                         Type.TaskException taskExceptionType, TaskTimerDto timer) {
    TaskExceptionDto taskExceptionDto = new TaskExceptionDto();
    if (taskExceptionType.equals(Type.TaskException.ERROR_CORRECTION)) {
      taskExceptionDto.setRemark(taskExecution.getCorrectionReason());
    } else {
      taskExceptionDto.setRemark(taskExecution.getReason());

    }
    taskExceptionDto.setType(taskExceptionType.name());
    taskExceptionDto.setInitiator(userAuditDto);
    taskExceptionDto.setTimer(timer);
    return taskExceptionDto;
  }

  @Override
  public byte[] printJob(Long jobId) throws ResourceNotFoundException, IOException {
    log.info("[printJob] Request to print job, jobId: {}", jobId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    JobPrintDto jobPrintDto = getJobData(jobId);
    jobAuditService.printJob(jobPrintDto, principalUser);
    return generateJobPdf(jobPrintDto);
  }

  public List<CorrectionPrintDto> printJobCorrections(Long jobId) throws ResourceNotFoundException, JsonProcessingException {
    log.info("[printJob] Request to print job, jobId: {}", jobId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    Job job = jobRepository.findById(jobId)
      .orElseThrow(() -> new ResourceNotFoundException(jobId, ErrorCode.JOB_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Set<ParameterValueView> parameterValues = parameterValueRepository.findAllParameterValueDataByJobId(jobId);
    // Get the map of parameterValueId to list of CorrectionDto objects
    Map<Long, List<CorrectionDto>> correctionDtoMap = getCorrectionDtoMap(parameterValues);

    List<CorrectionPrintDto> correctionPrintDtoList = new ArrayList<>();

    for (Map.Entry<Long, List<CorrectionDto>> entry : correctionDtoMap.entrySet()) {
      Long parameterValueId = entry.getKey();
      List<CorrectionDto> corrections = entry.getValue();

      CorrectionPrintDto correctionPrintDto = new CorrectionPrintDto();
      correctionPrintDto.setParameterExecutionId(String.valueOf(parameterValueId));
      correctionPrintDto.setCorrections(corrections);
      correctionPrintDtoList.add(correctionPrintDto);
    }

    return correctionPrintDtoList;
  }


  @Override
  public byte[] printJobActivity(Long jobId, String filters) throws ResourceNotFoundException, IOException {
    log.info("[printJobActivity] Request to print job activity, jobId: {}, filters: {}", jobId, filters);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    // Record the audit entry
    JobPrintDto jobPrintDto = getJobData(jobId);
    jobAuditService.printJobActivity(jobPrintDto, principalUser);

    // Get facility information
    Facility facility = facilityRepository.findById(principalUser.getCurrentFacilityId())
      .orElseThrow(() -> new ResourceNotFoundException(
        principalUser.getCurrentFacilityId(), ErrorCode.FACILITY_NOT_FOUND,
        ExceptionType.ENTITY_NOT_FOUND));

    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setId(facility.getId().toString());
    facilityDto.setName(facility.getName());
    facilityDto.setTimeZone(facility.getTimeZone());
    facilityDto.setDateFormat(facility.getDateFormat());
    facilityDto.setTimeFormat(facility.getTimeFormat());
    facilityDto.setDateTimeFormat(facility.getDateTimeFormat());

    Job job = jobRepository.findById(jobId)
      .orElseThrow(() -> new ResourceNotFoundException(
        jobId, ErrorCode.JOB_NOT_FOUND,
        ExceptionType.ENTITY_NOT_FOUND));

    Checklist checklist = checklistRepository.findById(job.getChecklistId())
      .orElseThrow(() -> new ResourceNotFoundException(
        job.getChecklistId(), ErrorCode.PROCESS_NOT_FOUND,
        ExceptionType.ENTITY_NOT_FOUND));

    List<ChecklistPropertyValue> checklistPropertyValues =
      new ArrayList<>(checklist.getChecklistPropertyValues());

    // Apply filters to job audits if provided
    List<JobAudit> jobAudits;
    if (!Utility.isEmpty(filters)) {
      // Create specification from filters using helper method
      Specification<JobAudit> specification = PdfBuilderServiceHelpers.createJobAuditSpecification(filters);

      // Apply specification to get filtered audits
      if (specification != null) {
        jobAudits = jobAuditRepository.findAll(
          Specification.where(specification)
            .and((root, query, cb) -> cb.equal(root.get("jobId"), jobId))
        );

        // Sort the audits by triggeredAt in descending order (newest first)
        jobAudits.sort(Comparator.comparing(JobAudit::getTriggeredAt).reversed());
      } else {
        // If specification creation failed, fall back to getting all audits
        jobAudits = jobAuditRepository.findByJobIdOrderByTriggeredAtDesc(jobId);
      }
    } else {
      // If no filters, get all audits for the job
      jobAudits = jobAuditRepository.findByJobIdOrderByTriggeredAtDesc(jobId);
    }

    JobAnnotation jobAnnotation =
      jobAnnotationRepository.findLatestByJobId(jobId);

    GeneratedPdfDataDto pdfData = new GeneratedPdfDataDto();
    pdfData.setGeneratedOn(DateTimeUtils.now());
    pdfData.setUserFullName(Utility.getFullNameAndEmployeeId(principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId()));
    pdfData.setUserId(principalUser.getEmployeeId());
    pdfData.setTimezoneOffset(facility.getTimeZone());
    pdfData.setDateFormat(facility.getDateFormat());
    pdfData.setDateTimeFormat(facility.getDateTimeFormat());
    pdfData.setFilters(filters); // Pass filters to PDF data

    pdfData.setFacility(facilityDto);
    pdfData.setJob(job);
    pdfData.setChecklist(checklist);
    pdfData.setJobAudits(jobAudits);
    pdfData.setChecklistPropertyValues(checklistPropertyValues);
    pdfData.setJobAnnotation(jobAnnotation);
    pdfData.setTotalStages(jobPrintDto.getTotalStages());
    pdfData.setTotalTask(jobPrintDto.getTotalTask());

    if (job.getState() == State.Job.COMPLETED_WITH_EXCEPTION) {
      pdfData.setCweDetails(jobPrintDto.getCweDetails());
    }

    pdfData.setJobPrintDto(jobPrintDto);

    return pdfGeneratorUtil.generatePdf(Type.PdfType.JOB_AUDIT, pdfData);
  }


  private TaskExceptionDto createTaskExceptionDtoForParameterDeviation(ParameterValue parameterValue, UserAuditDto userAuditDto, double userInput) {
    TaskExceptionDto taskExceptionDto = new TaskExceptionDto();
    taskExceptionDto.setRemark(parameterValue.getReason());
    taskExceptionDto.setInitiator(userAuditDto);
    taskExceptionDto.setType(Type.TaskException.PARAMETER_DEVIATION.name());

    ParameterDeviationDto parameterDeviationDto = new ParameterDeviationDto();
    parameterDeviationDto.setParameter(parameterValue.getParameter().getData());
    parameterDeviationDto.setUserInput(userInput);

    taskExceptionDto.setParameterDeviation(parameterDeviationDto);

    return taskExceptionDto;
  }

  private TaskExceptionDto createTaskExceptionDtoForYesNoType(ParameterValue parameterValue, UserAuditDto userAuditDto) {
    TaskExceptionDto taskExceptionDto = new TaskExceptionDto();
    taskExceptionDto.setRemark(parameterValue.getReason());
    taskExceptionDto.setInitiator(userAuditDto);
    taskExceptionDto.setType(Type.TaskException.YES_NO.name());
    return taskExceptionDto;
  }

  //TODO State Management ?
  private void validateJobState(Long jobId, Action.Job action, State.Job state) throws StreemException {
    switch (action) {
      case START:
        if (State.Job.BLOCKED.equals(state)) {
          ValidationUtils.invalidate(jobId, ErrorCode.JOB_IS_BLOCKED);
        }
        if (State.Job.UNASSIGNED.equals(state)) {
          ValidationUtils.invalidate(jobId, ErrorCode.UNASSIGNED_JOB_CANNOT_BE_STARTED);
        }
        if (State.Job.IN_PROGRESS.equals(state)) {
          ValidationUtils.invalidate(jobId, ErrorCode.JOB_ALREADY_STARTED);
        }
        if (State.Job.COMPLETED_WITH_EXCEPTION.equals(state) || State.Job.COMPLETED.equals(state)) {
          ValidationUtils.invalidate(jobId, ErrorCode.JOB_ALREADY_COMPLETED);
        }
        break;
      case COMPLETE:
        if (State.Job.BLOCKED.equals(state)) {
          ValidationUtils.invalidate(jobId, ErrorCode.JOB_IS_BLOCKED);
        }
        if (State.Job.COMPLETED.equals(state) || State.Job.COMPLETED_WITH_EXCEPTION.equals(state)) {
          ValidationUtils.invalidate(jobId, ErrorCode.JOB_ALREADY_COMPLETED);
        }
        if (!State.Job.IN_PROGRESS.equals(state)) {
          ValidationUtils.invalidate(jobId, ErrorCode.JOB_IS_NOT_IN_PROGRESS);
        }
        break;
      case COMPLETE_WITH_EXCEPTION:
        if (State.Job.COMPLETED.equals(state) || State.Job.COMPLETED_WITH_EXCEPTION.equals(state)) {
          ValidationUtils.invalidate(jobId, ErrorCode.JOB_ALREADY_COMPLETED);
        }
        break;
      case BLOCKED:
        if (State.Job.COMPLETED.equals(state) || State.Job.COMPLETED_WITH_EXCEPTION.equals(state)) {
          ValidationUtils.invalidate(jobId, ErrorCode.JOB_ALREADY_COMPLETED);
        }

        if (!State.Job.IN_PROGRESS.equals(state)) {
          ValidationUtils.invalidate(jobId, ErrorCode.JOB_IS_NOT_IN_PROGRESS);
        }

        break;
      case IN_PROGRESS:
        if (State.Job.IN_PROGRESS.equals(state)) {
          ValidationUtils.invalidate(jobId, ErrorCode.JOB_ALREADY_STARTED);
        }
        if (State.Job.COMPLETED.equals(state) || State.Job.COMPLETED_WITH_EXCEPTION.equals(state)) {
          ValidationUtils.invalidate(jobId, ErrorCode.JOB_ALREADY_COMPLETED);
        }
        if (!State.Job.BLOCKED.equals(state)) {
          ValidationUtils.invalidate(jobId, ErrorCode.JOB_IS_NOT_IN_BLOCKED);
        }
        break;
      case ASSIGN, UPDATE:
        if (State.JOB_COMPLETED_STATES.contains(state)) {
          ValidationUtils.invalidate(jobId, ErrorCode.JOB_ALREADY_COMPLETED);
        }
        break;
    }
  }

  /**
   * function checks if user is assigned to any of the tasks in the job
   */
  public void validateIfUserIsAssignedToExecuteJob(Long jobId, Long userId) throws StreemException {
    if (!taskExecutionAssigneeRepository.isUserAssignedToAnyTask(jobId, userId)) {
      ValidationUtils.invalidate(jobId, ErrorCode.USER_NOT_ASSIGNED_TO_EXECUTE_JOB);
    }
  }

  /**
   * function checks if all the mandatory parameters are completed.
   * Exclusion - Skipped and Completed Tasks
   *
   * @param jobId
   * @throws StreemException
   */
  private void validateMandatoryParametersIncomplete(Long jobId) throws StreemException {
    List<Error> errorList = new ArrayList<>();
    List<IncompleteParameterView> incompleteParameters = parameterValueRepository.findIncompleteParametersByJobId(jobId);
    if (!Utility.isEmpty(incompleteParameters)) {
      setIncompleteParameters(incompleteParameters, errorList);
      ValidationUtils.invalidate(ErrorMessage.MANDATORY_PARAMETERS_NOT_COMPLETED, errorList);
    }
  }

  private void setIncompleteParameters(List<IncompleteParameterView> incompleteParameterViews, List<Error> errors) {
    Set<Long> taskIds = new HashSet<>();
    Set<Long> parameterIds = new HashSet<>();

    for (IncompleteParameterView incompleteParameterView : incompleteParameterViews) {
      if (!taskIds.contains(incompleteParameterView.getTaskExecutionId())) {
        taskIds.add(incompleteParameterView.getTaskExecutionId());
        ValidationUtils.addError(incompleteParameterView.getTaskExecutionId(), errors, ErrorCode.TASK_INCOMPLETE);
      }
      if (!parameterIds.contains(incompleteParameterView.getParameterValueId())) {
        parameterIds.add(incompleteParameterView.getParameterValueId());
        ValidationUtils.addError(incompleteParameterView.getParameterValueId(), errors, ErrorCode.PARAMETER_INCOMPLETE);
      }
    }
  }

  private void validateIfTasksBelongToCompletedStates(Long jobId) throws StreemException {
    List<Long> nonCompletedTaskIds = taskExecutionRepository.findNonCompletedTaskIdsByJobId(jobId);
    List<Long> taskIdsEnabledForCorrection = taskExecutionRepository.findEnabledForCorrectionTaskIdsByJobId(jobId);
    List<Error> errorList = new ArrayList<>();
    if (!Utility.isEmpty(nonCompletedTaskIds)) {
      for (Long taskId : nonCompletedTaskIds) {
        ValidationUtils.addError(taskId, errorList, ErrorCode.TASK_IN_PROGRESS);
      }
    }
    if (!Utility.isEmpty(taskIdsEnabledForCorrection)) {
      for (Long taskId : taskIdsEnabledForCorrection) {
        ValidationUtils.addError(taskId, errorList, ErrorCode.TASK_ENABLED_FOR_CORRECTION);
      }
    }
    if (!Utility.isEmpty(errorList)) {
      ValidationUtils.invalidate(ErrorMessage.TASKS_NOT_IN_COMPLETED_STATE, errorList);
    }
  }

  @Override
  public JobStateDto getJobState(Long jobId) throws ResourceNotFoundException {
    log.info("[getJobState] Request to get job state, jobId: {}", jobId);
    Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException(jobId, ErrorCode.JOB_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    return jobMapper.toJobStateDto(job);
  }

  @Override
  public Page<JobPartialDto> getAllByResource(String objectId, String filters, Pageable pageable) {
    log.info("[getAllByResource] Request to find all jobs by resource, objectTypeId: {}, filters: {}, pageable: {}", objectId, filters, pageable);

    // TODO URGENT this needs to be optimized
    Set<Long> jobIds = getJobIdsHavingObjectInChoicesForAllParameters(objectId);

    if (!Utility.isEmpty(jobIds)) {
      PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
      SearchCriteria organisationSearchCriteria = (new SearchCriteria()).setField(Job.ORGANISATION_ID).setOp(Operator.Search.EQ.toString()).setValues(Collections.singletonList(principalUser.getOrganisationId()));
      SearchCriteria jobIdsCriteria = (new SearchCriteria()).setField(Job.ID).setOp(Operator.Search.ANY.toString()).setValues(new ArrayList<>(jobIds));

      SearchCriteria facilitySearchCriteria = null;
      Long currentFacilityId = principalUser.getCurrentFacilityId();
      if (currentFacilityId != null && !currentFacilityId.equals(Misc.ALL_FACILITY_ID)) {
        facilitySearchCriteria =
          (new SearchCriteria()).setField(Job.FACILITY_ID).setOp(Operator.Search.EQ.toString()).setValues(Collections.singletonList(currentFacilityId));
      }

      /*--Fetch JobsIds wrt Specification--*/
      Specification<Job> specification = SpecificationBuilder.createSpecification(filters, Arrays.asList(organisationSearchCriteria, facilitySearchCriteria, jobIdsCriteria));
      Page<Job> jobPage = jobRepository.findAll(specification, pageable);

      Set<Long> ids = jobPage.getContent()
        .stream().map(BaseEntity::getId).collect(Collectors.toSet());
      List<Job> jobs = jobRepository.findAllByIdIn(ids);
      Map<Long, Set<ParameterValue>> jobParameterValueMap = parameterValueRepository.findAllByJobIdAndTargetEntityType(ids, Type.ParameterTargetEntityType.PROCESS).stream().collect(Collectors.groupingBy(ParameterValue::getJobId, Collectors.toSet()));
      jobs.forEach(job -> job.setParameterValues(jobParameterValueMap.getOrDefault(job.getId(), new HashSet<>())));
      List<Long> processParameterIds = getProcessParameterIds(jobs);
      List<JobPartialDto> jobDtoList = jobMapper.jobToJobPartialDto(jobs, getPendingOnMeTasks(ids), processParameterIds);

      return new PageImpl<>(jobDtoList, pageable, jobPage.getTotalElements());
    } else {
      return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }
  }

  private Map<String, List<JobAssigneeView>> getJobAssignees(Set<Long> jobIds) {
    List<JobAssigneeView> jobAssignees = taskExecutionAssigneeRepository.getJobAssignees(jobIds);
    return jobAssignees.stream().collect(Collectors.groupingBy(JobAssigneeView::getJobId));
  }

  private Map<String, List<TaskPendingOnMeView>> getPendingOnMeTasks(Set<Long> jobIds) {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Long principalUserId = principalUser.getId();
    List<TaskPendingOnMeView> taskPendingOnMeViewList = taskExecutionRepository.getPendingTasksOfUserForJobs(jobIds, principalUserId, Misc.JOB_PENDING_STATES);
    return taskPendingOnMeViewList.stream().filter(task -> task.getJobId() != null)
      .collect(Collectors.groupingBy(TaskPendingOnMeView::getJobId));
  }

  private List<EngagedUserView> getEngagedUsersOfJob(Set<Long> jobIds) {
    return taskExecutionRepository.getEngagedUsersForJob(jobIds);
  }

  @Override
  @Transactional(readOnly = true)
  public StageDetailsDto getStageData(Long jobId, Long stageId) throws ResourceNotFoundException, JsonProcessingException {
    Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException(jobId, ErrorCode.JOB_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Stage stage = stageRepository.findById(stageId).orElseThrow(() -> new ResourceNotFoundException(stageId, ErrorCode.STAGE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    StageDetailsDto stageDetailsDto = new StageDetailsDto();
    stageDetailsDto.setJobId(job.getIdAsString());
    stageDetailsDto.setJobState(job.getState());

    List<ParameterValue> parameterValues = parameterValueRepository.readByJobIdAndStageId(jobId, stageId);
    List<TaskExecution> taskExecutions = taskExecutionRepository.readByJobIdAndStageIdOrderByOrderTree(jobId, stageId);
    Map<Long, List<TaskPauseReasonOrComment>> pauseCommentsOrReason = taskExecutionTimerService.calculateDurationAndReturnReasonsOrComments(taskExecutions);


    Map<Long, List<ParameterValue>> parameterValueMap =
      parameterValues.stream()
        .collect(Collectors.groupingBy(
          av -> av.getParameter().getId(),
          Collectors.toList()
        ));

    Map<Long, List<TaskExecution>> taskIdTaskExecutionListMap = new HashMap<>();
    Map<Long, TaskExecution> taskExecutionMap = new HashMap<>();
    for (TaskExecution taskExecution : taskExecutions) {
      taskIdTaskExecutionListMap.computeIfAbsent(taskExecution.getTask().getId(), k -> new ArrayList<>());
      taskIdTaskExecutionListMap.get(taskExecution.getTask().getId()).add(taskExecution);
      taskExecutionMap.put(taskExecution.getId(), taskExecution);
    }
    List<TempParameterValue> tempParameterValues = tempParameterValueRepository.readByJobIdAndStageId(jobId, stageId);
    Map<Long, List<TempParameterValue>> tempParameterValueMap =
      tempParameterValues.stream()
        .collect(Collectors.groupingBy(
          av -> av.getParameter().getId(),
          Collectors.toList()
        ));


    Map<Long, List<TempParameterVerification>> tempParameterVerificationPeerAndSelf = parameterVerificationService.getTempParameterVerificationsDataForAJob(jobId);
    Map<Long, List<ParameterVerification>> parameterVerificationPeerAndSelf = parameterVerificationService.getParameterVerificationsDataForAJob(jobId);
    stageDetailsDto.setStage(stageMapper.toDto(stage, parameterValueMap, taskExecutionMap, tempParameterValueMap, pauseCommentsOrReason, parameterVerificationPeerAndSelf, tempParameterVerificationPeerAndSelf));

    return stageDetailsDto;
  }

  @Override
  public boolean isJobExistsBySchedulerIdAndDateGreaterThanOrEqualToExpectedStartDate(Long schedulerId, Long epochDateTime) {
    return jobRepository.isJobExistsBySchedulerIdAndDateGreaterThanOrEqualToExpectedStartDate(schedulerId, epochDateTime);
  }

  @Override
  public TaskDetailsDto getTaskData(Long jobId, Long taskExecutionId) throws ResourceNotFoundException {
    State.Job jobState = jobRepository.getStateByJobId(jobId);
    TaskDetailsDto taskDetailsDto = new TaskDetailsDto();
    taskDetailsDto.setJobState(jobState);
    taskDetailsDto.setJobId(jobId.toString());

    List<ParameterValue> parameterValues = parameterValueRepository.findAllByTaskExecutionId(taskExecutionId);
    TaskExecution taskExecution = taskExecutionRepository.findById(taskExecutionId)
      .orElseThrow(() -> new ResourceNotFoundException(taskExecutionId, ErrorCode.TASK_EXECUTION_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    Map<Long, List<TaskPauseReasonOrComment>> pauseCommentsOrReason = taskExecutionTimerService.calculateDurationAndReturnReasonsOrComments(List.of(taskExecution));


    Map<Long, List<ParameterValue>> parameterValueMap =
      parameterValues.stream()
        .collect(Collectors.groupingBy(
          av -> av.getParameter().getId(),
          Collectors.toList()
        ));

    Map<Long, List<TaskExecution>> taskIdTaskExecutionListMap = new HashMap<>();
    Map<Long, TaskExecution> taskExecutionMap = new HashMap<>();

    taskIdTaskExecutionListMap.computeIfAbsent(taskExecution.getTaskId(), k -> new ArrayList<>());
    taskIdTaskExecutionListMap.get(taskExecution.getTaskId()).add(taskExecution);
    taskExecutionMap.put(taskExecution.getId(), taskExecution);

    List<TempParameterValue> tempParameterValues = tempParameterValueRepository.findAllByTaskExecutionId(taskExecutionId);

    Map<Long, List<TempParameterValue>> tempParameterValueMap =
      tempParameterValues.stream()
        .collect(Collectors.groupingBy(
          av -> av.getParameter().getId(),
          Collectors.toList()
        ));


    Map<Long, List<TempParameterVerification>> tempParameterVerificationPeerAndSelf = parameterVerificationService.getTempParameterVerificationsDataForAJob(jobId);
    Map<Long, List<ParameterVerification>> parameterVerificationPeerAndSelf = parameterVerificationService.getParameterVerificationsDataForAJob(jobId);
    taskDetailsDto.setTask(taskMapper.toDto(taskExecution.getTask(), parameterValueMap, taskExecutionMap, tempParameterValueMap, pauseCommentsOrReason, parameterVerificationPeerAndSelf, tempParameterVerificationPeerAndSelf));
    taskDetailsDto.setHidden(parameterValues.stream().allMatch(ParameterValueBase::isHidden));
    return taskDetailsDto;

  }

  @Override
  @Transactional(readOnly = true)
  public JobLiteDto getJobLiteById(Long jobId) throws ResourceNotFoundException {
    log.info("[getJobLiteById] Request to get job, jobId: {}", jobId);

    // Fetch the job and related data
    Job job = jobRepository.findById(jobId)
      .orElseThrow(() -> new ResourceNotFoundException(jobId, ErrorCode.JOB_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    List<ParameterValue> parameterValues = parameterValueRepository.findAllByJobIdAndTargetEntityType(Set.of(jobId), Type.ParameterTargetEntityType.PROCESS);
    List<TaskExecution> taskExecutionList = taskExecutionRepository.readByJobId(jobId, job.getChecklistId());

    // Create mappings for parameter values and task executions
    Map<Long, List<ParameterValue>> jobParameterValuesMap = new HashMap<>();
    Set<Parameter> jobParameters = new HashSet<>();

    for (ParameterValue av : parameterValues) {
      var parameter = av.getParameter();
      if (Type.ParameterTargetEntityType.PROCESS.equals(parameter.getTargetEntityType())) {
        jobParameters.add(parameter);
        jobParameterValuesMap.computeIfAbsent(parameter.getId(), k -> new ArrayList<>()).add(av);
      }
    }

    Map<Long, TaskExecution> taskExecutionMap = taskExecutionList.stream()
      .collect(Collectors.toMap(TaskExecution::getId, taskExecution -> taskExecution));

    // Initialize DTOs and other variables
    JobLiteDto jobLiteDto = new JobLiteDto();
    ChecklistJobLiteDto checklistJobLiteDto = new ChecklistJobLiteDto();
    List<StageLiteDto> stageLiteDtoList = new ArrayList<>();
    Map<Long, List<TaskLiteDto>> taskLiteListByStageId = new HashMap<>();
    Map<Long, List<TaskExecutionLiteDto>> taskExecutionLiteListByTaskId = new HashMap<>();

    // Fetch checklist, stages, and task executions
    ChecklistJobLiteView checklistJobLiteView = checklistRepository.getChecklistJobLiteDtoById(job.getChecklistId());
    List<StageLiteView> stageLiteViewList = stageRepository.getStagesByChecklistIdOrdered(job.getChecklistId());
    List<TaskExecutionLiteView> taskExecutionLiteViewList = taskExecutionRepository.getTaskExecutionsLiteByJobId(jobId);
    boolean isCorrectionPending = correctionRepository.isCorrectionPending(jobId, principalUser.getId());
    boolean isVerificationPending = parameterVerificationRepository.isVerificationPendingOnUser(jobId, principalUser.getId());
    boolean isExceptionPending = parameterExceptionRepository.isExceptionPendingOnUser(jobId, principalUser.getId());
    boolean isCJFExceptionPending = parameterExceptionRepository.isCJFExceptionPendingOnUser(jobId);
    boolean isExceptionRejectedOnCjf = parameterExceptionRepository.isExceptionRejectedOnCjf(jobId);

    Set<Long> stageIdsSet = stageLiteViewList.stream()
      .map(stage -> Long.parseLong(stage.getId()))
      .collect(Collectors.toSet());

    List<TaskLiteView> taskLiteViewList = new ArrayList<>();
    if (!Utility.isEmpty(stageIdsSet)) {
      taskLiteViewList = taskRepository.findTaskLiteInfoByStageIds(stageIdsSet);
    }

    // Map task executions by task ID
    for (TaskExecutionLiteView taskExecutionLiteView : taskExecutionLiteViewList) {
      TaskExecutionLiteDto taskExecutionLiteDto = new TaskExecutionLiteDto();
      taskExecutionLiteDto.setId(taskExecutionLiteView.getId());
      taskExecutionLiteDto.setType(taskExecutionLiteView.getType());
      taskExecutionLiteDto.setOrderTree(taskExecutionLiteView.getOrderTree());
      taskExecutionLiteDto.setState(taskExecutionLiteView.getState());
      taskExecutionLiteDto.setHidden(taskExecutionLiteView.getHidden());

      Long taskId = Long.parseLong(taskExecutionLiteView.getTaskId());
      taskExecutionLiteListByTaskId.computeIfAbsent(taskId, k -> new ArrayList<>()).add(taskExecutionLiteDto);
    }

    // Map tasks by stage ID and set task executions
    for (TaskLiteView taskLiteView : taskLiteViewList) {
      TaskLiteDto taskLiteDto = new TaskLiteDto();
      taskLiteDto.setId(taskLiteView.getId());
      taskLiteDto.setOrderTree(taskLiteView.getOrderTree());
      taskLiteDto.setName(taskLiteView.getName());

      Long taskId = Long.parseLong(taskLiteView.getId());
      List<TaskExecutionLiteDto> taskExecutionListForTask = taskExecutionLiteListByTaskId.getOrDefault(taskId, new ArrayList<>());
      taskLiteDto.setTaskExecutions(taskExecutionListForTask);
      taskLiteDto.setHidden(areAllTaskExecutionsHidden(taskExecutionListForTask));
      Long stageId = Long.parseLong(taskLiteView.getStageId());
      taskLiteListByStageId.computeIfAbsent(stageId, k -> new ArrayList<>()).add(taskLiteDto);
    }

    // Create StageLiteDto and set tasks for each stage
    for (StageLiteView stageLiteView : stageLiteViewList) {
      StageLiteDto stageLiteDto = new StageLiteDto();
      stageLiteDto.setId(stageLiteView.getId());
      stageLiteDto.setName(stageLiteView.getName());
      stageLiteDto.setOrderTree(stageLiteView.getOrderTree());

      Long stageId = Long.parseLong(stageLiteView.getId());
      List<TaskLiteDto> taskLiteListForStage = taskLiteListByStageId.getOrDefault(stageId, new ArrayList<>());
      stageLiteDto.setTasks(taskLiteListForStage);
      stageLiteDto.setHidden(areAllTasksHidden(taskLiteListForStage));
      stageLiteDtoList.add(stageLiteDto);
    }

    // Set the checklist details
    checklistJobLiteDto.setId(checklistJobLiteView.getId());
    checklistJobLiteDto.setName(checklistJobLiteView.getName());
    checklistJobLiteDto.setCode(checklistJobLiteView.getCode());
    checklistJobLiteDto.setStages(stageLiteDtoList);

    // Set the job details
    jobLiteDto.setId(String.valueOf(job.getId()));
    jobLiteDto.setCode(job.getCode());
    jobLiteDto.setState(job.getState());
    jobLiteDto.setSchedulerId(job.getSchedulerId());
    jobLiteDto.setExpectedStartDate(job.getExpectedStartDate());
    jobLiteDto.setExpectedEndDate(job.getExpectedEndDate());
    jobLiteDto.setStartedAt(job.getStartedAt());
    jobLiteDto.setEndedAt(job.getEndedAt());
    jobLiteDto.setChecklist(checklistJobLiteDto);
    jobLiteDto.setShowCorrectionBanner(isCorrectionPending);
    jobLiteDto.setShowVerificationBanner(isVerificationPending);
    jobLiteDto.setShowExceptionBanner(isExceptionPending);
    jobLiteDto.setShowCJFExceptionBanner(isCJFExceptionPending);
    jobLiteDto.setForceCwe(isExceptionRejectedOnCjf);

    // Convert parameter values to DTOs
    List<ParameterDto> parameterDtos = parameterMapper.toDto(jobParameters, jobParameterValuesMap, taskExecutionMap, null, new HashMap<>(), new HashMap<>(), new HashMap<>());
    if (!Utility.isEmpty(parameterDtos)) {
      parameterDtos.sort(Comparator.comparingInt(ParameterDto::getOrderTree));
    }
    jobLiteDto.setParameterValues(parameterDtos);

    return jobLiteDto;
  }

  @Override
  public JobAssigneeDto isCurrentUserAssignedToJob(Long jobId) {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return new JobAssigneeDto(taskExecutionAssigneeRepository.isUserAssignedToAnyTask(jobId, principalUser.getId()));
  }

  @Override
  //TODO: support pagination
  public List<JobAssigneeView> getAllJobAssignees(Long jobId, String query, List<String> roles, Pageable pageable) {
    if (Utility.isEmpty(roles)) {
      return taskExecutionAssigneeRepository.getAllJobAssigneesUsersAndUserGroups(jobId, query);
    } else {
      return taskExecutionAssigneeRepository.getAllJobAssigneesUsersAndUserGroupsByRoles(jobId, query, roles);
    }
  }

  @Override
  public Page<PendingForApprovalStatusDto> getPendingForApprovalParameters(String processName, String parameterName, String objectId, String jobId, String userId, String useCaseId, boolean showAllException,Long requestedBy, Pageable pageable) {
    log.info("[getPendingForApprovalParameters] Request to get all pending for approval parameters, processName: {}, parameterName: {}", processName, parameterName);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    long facilityId = principalUser.getCurrentFacilityId();
    objectId = Utility.isEmpty(objectId) ? null : objectId;
    Page<PendingForApprovalStatusView> pendingParameters = jobRepository.getAllPendingForApprovalParameters(
      facilityId,
      "%" + parameterName + "%",
      "%" + processName + "%",
      objectId,
      jobId,
      userId,
      Long.valueOf(useCaseId),
      showAllException,
      requestedBy,
      pageable
    );

    Set<Long> userIds = pendingParameters
      .stream()
      .map(PendingForApprovalStatusView::getExceptionInitiatedBy)
      .collect(Collectors.toSet());

    Map<Long, UserAuditDto> userAuditDtoMap = userRepository.findAllByIdIn(userIds)
      .stream()
      .collect(Collectors.toMap(
        User::getId,
        userMapper::toUserAuditDto));


    return pendingParameters.map(view -> {
      UserAuditDto userAuditDto = userAuditDtoMap.get(view.getExceptionInitiatedBy());
      return new PendingForApprovalStatusDto(
        view.getParameterValueId(),
        view.getJobId(),
        view.getParameterName(),
        view.getTaskName(),
        view.getProcessName(),
        view.getModifiedAt(),
        view.getJobCode(),
        view.getStageId(),
        view.getTaskId(),
        view.getCreatedAt(),
        view.getTaskExecutionId(),
        view.getParameterId(),
        userAuditDto,
        view.getRulesId()
        );
    });
  }

  private boolean areAllTaskExecutionsHidden(List<TaskExecutionLiteDto> taskExecutionListForTask) {
    if (taskExecutionListForTask == null || taskExecutionListForTask.isEmpty()) {
      return false;
    }
    for (TaskExecutionLiteDto taskExecutionLiteDto : taskExecutionListForTask) {
      if (taskExecutionLiteDto == null || !taskExecutionLiteDto.isHidden()) {
        return false;
      }
    }
    return true;
  }

  private boolean areAllTasksHidden(List<TaskLiteDto> taskLiteListForStage) {
    if (taskLiteListForStage == null || taskLiteListForStage.isEmpty()) {
      return false; // Consider no tasks as not hidden
    }
    for (TaskLiteDto taskLiteDto : taskLiteListForStage) {
      if (taskLiteDto == null || !taskLiteDto.isHidden()) {
        return false;
      }
    }
    return true;
  }


  private void recordJobLogForRelations(JobDto jobDto, UserAuditDto userBasicInfoDto) {
    for (RelationValueDto relationValueDto : jobDto.getRelations()) {
      if (!Utility.isEmpty(relationValueDto.getTargets())) {
        var targets = relationValueDto.getTargets();
        StringBuilder value = new StringBuilder(targets.get(0).getExternalId());
        for (int i = 1; i < targets.size(); i++) {
          value.append(", ").append(targets.get(i).getExternalId());
        }
        jobLogService.recordJobLogTrigger(jobDto.getId(), relationValueDto.getId(), Type.JobLogTriggerType.RELATION_VALUE, relationValueDto.getDisplayName(), null, value.toString(), value.toString(), userBasicInfoDto);
      }
    }
  }

  private Set<Long> getJobIdsHavingObjectInChoicesForProcessParameters(String objectId) {
    String jsonChoices = String.format("""
      [
          {
              "objectId": "%s"
          }
      ]
      """, objectId);
    return parameterValueRepository.getJobIdsByTargetEntityTypeAndObjectInChoices(Type.ParameterTargetEntityType.PROCESS.name(), jsonChoices);
  }

  private Set<Long> getJobIdsHavingObjectInChoicesForAllParameters(String objectId) {
    String jsonChoices = String.format("""
      [
          {
              "objectId": "%s"
          }
      ]
      """, objectId);
    return parameterValueRepository.getJobIdsByObjectInChoices(jsonChoices);
  }

  private JobPrintDto getJobData(Long jobId) throws ResourceNotFoundException, JsonProcessingException {
    Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException(jobId, ErrorCode.JOB_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    List<ParameterValue> parameterValues = parameterValueRepository.findAllByJobId(jobId);
    List<TaskExecution> taskExecutions = taskExecutionRepository.readByJobId(jobId, job.getChecklistId());

    Map<Long, List<TaskExecution>> taskIdTaskExecutionListMap = new HashMap<>();
    Map<Long, TaskExecution> taskExecutionMap = new HashMap<>();
    List<TaskPauseReasonOrComment> pausedReason = new ArrayList<>();
    List<TaskPauseResumeAuditView> taskPauseResumeAuditList = new ArrayList<>();
    Map<Long, List<TaskPauseReasonOrComment>> pauseReasonOrCommentMap = new HashMap<>();
    Map<Long, List<TaskPauseResumeAuditDto>> taskPauseResumeAuditDtoMap = new HashMap<>();

    for (TaskExecution taskExecution : taskExecutions) {
      taskIdTaskExecutionListMap.computeIfAbsent(taskExecution.getTaskId(), k -> new ArrayList<>());
      taskIdTaskExecutionListMap.get(taskExecution.getTask().getId()).add(taskExecution);
      taskExecutionMap.put(taskExecution.getId(), taskExecution);
      pausedReason = taskExecutionTimerService.calculateDurationAndReturnReasonsOrComments(List.of(taskExecution)).get(taskExecution.getId());
      taskPauseResumeAuditList = taskExecutionRepository.getTaskPauseResumeAuditDtoByTaskExecutionId(taskExecution.getId());
      pauseReasonOrCommentMap.put(taskExecution.getId(), pausedReason);

      //Convert TaskPauseResumeAuditDto
      List<TaskPauseResumeAuditDto> taskPauseResumeAuditDtoList  = convertToTaskPauseResumeDto(taskPauseResumeAuditList);
      taskPauseResumeAuditDtoMap.put(taskExecution.getId(), taskPauseResumeAuditDtoList);
    }

    Map<Long, List<ParameterValue>> taskParameterValuesMap = new HashMap<>();
    Map<Long, List<ParameterValue>> jobParameterValuesMap = new HashMap<>();
    Set<Parameter> jobParameters = new HashSet<>();
    for (ParameterValue av : parameterValues) {
      var parameter = av.getParameter();
      if (Type.ParameterTargetEntityType.TASK.equals(av.getParameter().getTargetEntityType())) {
        taskParameterValuesMap.computeIfAbsent(parameter.getId(), k -> new ArrayList<>()).add(av);
      } else {
        jobParameters.add(parameter);
        jobParameterValuesMap.computeIfAbsent(parameter.getId(), k -> new ArrayList<>()).add(av);
      }
    }

    List<TempParameterValue> tempParameterValues = tempParameterValueRepository.readByJobId(jobId);
    Map<Long, List<TempParameterValue>> tempParameterValueMap = tempParameterValues.stream()
      .collect(Collectors.groupingBy(
        av -> av.getParameter().getId(),
        Collectors.toList()
      ));

    Map<Long, List<ParameterVerification>> parameterVerificationMap = parameterVerificationService.getParameterVerificationsDataForAJob(jobId);
    Map<Long, List<TempParameterVerification>> tempParameterVerificationMap = parameterVerificationService.getTempParameterVerificationsDataForAJob(jobId);


    JobPrintDto jobPrintDto = jobMapper.toJobPrintDto(job,
      taskParameterValuesMap,
      taskExecutionMap,
      tempParameterValueMap,
      new HashMap<>(),
      parameterVerificationMap,
      tempParameterVerificationMap);


    jobPrintDto.getChecklist().getStages().stream()
      .flatMap(stageDto -> stageDto.getTasks().stream())
      .flatMap(taskDto -> taskDto.getTaskExecutions().stream())
      .forEach(taskExecutionDto ->
        taskExecutionDto.setTaskPauseResumeAudits(
          taskPauseResumeAuditDtoMap.get(Long.valueOf(taskExecutionDto.getId()))));  // Set audits for each taskExecutionDto

    Set<AssigneeSignOffDto> assignees = new HashSet<>();
    Map<String, Long> recentSignOffDetails = new HashMap<>();
    for (TaskExecution taskExecution : taskExecutions) {
      Set<TaskExecutionUserMapping> taskExecutionUserMappings = taskExecution.getAssignees();
      for (TaskExecutionUserMapping taskExecutionUserMapping : taskExecutionUserMappings) {
        setAssignees(assignees, recentSignOffDetails, taskExecutionUserMapping);
      }
    }
    var assigneeSignOffDtos = getAssigneeSignOffDtos(assignees, recentSignOffDetails);
    jobPrintDto.setAssignees(assigneeSignOffDtos);

    if (State.Job.COMPLETED_WITH_EXCEPTION.equals(job.getState())) {
      jobPrintDto.setCweDetails(jobCweService.getJobCweDetail(jobId));
    }

    if (State.JOB_COMPLETED_STATES.contains(job.getState()) && job.getStartedAt() != null && job.getEndedAt() != null) {
      jobPrintDto.setTotalDuration(job.getEndedAt() - job.getStartedAt());
    }
    long totalStages = 0L;
    long totalTasks = 0L;
    for (Stage stage : job.getChecklist().getStages()) {
      totalStages++;
      totalTasks = totalTasks + stage.getTasks().size();
    }
    jobPrintDto.setTotalStages(totalStages);
    jobPrintDto.setTotalTask(totalTasks);

    List<ParameterDto> parameterDtos = parameterMapper.toDto(jobParameters, jobParameterValuesMap, taskExecutionMap, null, new HashMap<>(), new HashMap<>(), new HashMap<>());
    jobPrintDto.setParameterValues(parameterDtos);

    List<JobAnnotationDto> jobAnnotationDtoList = jobAnnotationMapper.toDto(jobAnnotationRepository.findByJobId(jobId));
    jobPrintDto.setJobAnnotationDto(jobAnnotationDtoList);

    return jobPrintDto;
  }

  private void applyCustomSortPendingOnMe(CriteriaBuilder cb, CriteriaQuery<Tuple> query, Root<Job> root) {
    long now = Instant.now().atZone(zoneId).toInstant().getEpochSecond();

    Expression<Boolean> isOverdueUnassignedOrAssigned = createOverdueUnassignedOrAssignedExpression(cb, root, now);
    Expression<Boolean> isPendingStartUnassignedOrAssigned = createPendingStartUnassignedOrAssignedExpression(cb, root, now);
    Expression<Boolean> isOverdueInProgress = createOverdueInProgressExpression(cb, root, now);
    Expression<Boolean> isOngoing = cb.equal(root.get("state"), State.Job.IN_PROGRESS);
    Expression<Boolean> isUnscheduled = cb.isNull(root.get("expectedStartDate"));
    Expression<Boolean> isScheduledForToday = createScheduledForTodayExpression(cb, root);

    Expression<Integer> sortCase = cb.<Integer>selectCase()
      .when(cb.and(isOverdueUnassignedOrAssigned, isPendingStartUnassignedOrAssigned), cb.literal(SORT_OVERDUE_UNASSIGNED_OR_ASSIGNED))
      .when(isPendingStartUnassignedOrAssigned, cb.literal(SORT_PENDING_START_UNASSIGNED_OR_ASSIGNED))
      .when(isOverdueInProgress, cb.literal(SORT_OVERDUE_IN_PROGRESS))
      .when(isOngoing, cb.literal(SORT_ONGOING))
      .when(cb.and(isUnscheduled, isOngoing), cb.literal(SORT_UNSCHEDULED_ONGOING))
      .when(isScheduledForToday, cb.literal(SORT_SCHEDULED_FOR_TODAY))
      .otherwise(cb.literal(SORT_OTHERWISE));

    Expression<Long> secondarySortCase = cb.<Long>selectCase()
      .when(cb.equal(root.get("state"), State.Job.IN_PROGRESS), root.get("startedAt"))
      .otherwise(root.get("createdAt"));

    List<Order> orders = new ArrayList<>();
    orders.add(cb.asc(cb.literal(1)));
    orders.add(cb.desc(cb.literal(2)));
    query.multiselect(sortCase, secondarySortCase, root);
    query.orderBy(orders);
  }

  private void applyCustomSort(CriteriaBuilder cb, CriteriaQuery<Tuple> query, Root<Job> root) {
    long now = Instant.now().atZone(zoneId).toInstant().getEpochSecond();

    Expression<Boolean> isOverdueUnassignedOrAssigned = createOverdueUnassignedOrAssignedExpression(cb, root, now);
    Expression<Boolean> isPendingStartUnassignedOrAssigned = createPendingStartUnassignedOrAssignedExpression(cb, root, now);
    Expression<Boolean> isPendingStartScheduledForToday = cb.and(
      createPendingStartUnassignedOrAssignedExpression(cb, root, now),
      createScheduledForTodayExpression(cb, root)
    );
    Expression<Boolean> isOverdueInProgress = createOverdueInProgressExpression(cb, root, now);
    Expression<Boolean> isOngoing = cb.equal(root.get("state"), State.Job.IN_PROGRESS);
    Expression<Boolean> isUnscheduled = cb.and(
      cb.isNull(root.get("expectedStartDate")),
      cb.notEqual(root.get("state"), State.Job.COMPLETED),
      cb.notEqual(root.get("state"), State.Job.COMPLETED_WITH_EXCEPTION));
    Expression<Boolean> isScheduledForToday = createScheduledForTodayExpression(cb, root);
    Expression<Boolean> isCompletedWithException = cb.equal(root.get("state"), State.Job.COMPLETED_WITH_EXCEPTION);
    Expression<Boolean> isCompleted = cb.equal(root.get("state"), State.Job.COMPLETED);

    Expression<Integer> sortCase = cb.<Integer>selectCase()
      .when(cb.and(isOverdueUnassignedOrAssigned, isPendingStartUnassignedOrAssigned), cb.literal(SORT_OVERDUE_UNASSIGNED_OR_ASSIGNED))
      .when(isPendingStartUnassignedOrAssigned, cb.literal(SORT_PENDING_START_UNASSIGNED_OR_ASSIGNED))
      .when(isPendingStartScheduledForToday, cb.literal(SORT_PENDING_START_SCHEDULED_FOR_TODAY))
      .when(isOverdueInProgress, cb.literal(SORT_OVERDUE_IN_PROGRESS))
      .when(isOngoing, cb.literal(SORT_ONGOING))
      .when(cb.and(isUnscheduled, isOngoing), cb.literal(SORT_UNSCHEDULED_ONGOING))
      .when(isScheduledForToday, cb.literal(SORT_SCHEDULED_FOR_TODAY))
      .when(isUnscheduled, cb.literal(SORT_UNSCHEDULED))
      .when(isCompletedWithException, cb.literal(SORT_COMPLETED_WITH_EXCEPTION))
      .when(isCompleted, cb.literal(SORT_COMPLETED))
      .otherwise(cb.literal(SORT_OTHERWISE));

    Expression<Long> secondarySortCase = cb.<Long>selectCase()
      .when(cb.equal(root.get("state"), State.Job.IN_PROGRESS), root.get("startedAt"))
      .when(cb.equal(root.get("state"), State.Job.COMPLETED), root.get("endedAt"))
      .when(cb.equal(root.get("state"), State.Job.COMPLETED_WITH_EXCEPTION), root.get("endedAt"))
      .otherwise(root.get("createdAt"));

    List<Order> orders = new ArrayList<>();
    orders.add(cb.asc(cb.literal(1)));
    orders.add(cb.desc(cb.literal(2)));
    query.multiselect(sortCase, secondarySortCase, root);
    query.orderBy(orders);
  }

  private Expression<Boolean> createOverdueUnassignedOrAssignedExpression(CriteriaBuilder cb, Root<Job> root, long now) {
    return cb.and(
      cb.lessThan(root.get("expectedEndDate"), now),
      cb.or(
        cb.equal(root.get("state"), State.Job.UNASSIGNED),
        cb.equal(root.get("state"), State.Job.ASSIGNED)
      )
    );
  }

  private Expression<Boolean> createPendingStartUnassignedOrAssignedExpression(CriteriaBuilder cb, Root<Job> root, long now) {
    return cb.and(
      cb.lessThan(root.get("expectedStartDate"), now),
      cb.or(
        cb.equal(root.get("state"), State.Job.UNASSIGNED),
        cb.equal(root.get("state"), State.Job.ASSIGNED)
      )
    );
  }

  private Expression<Boolean> createOverdueInProgressExpression(CriteriaBuilder cb, Root<Job> root, long now) {
    return cb.and(
      cb.lessThan(root.get("expectedEndDate"), now),
      cb.equal(root.get("state"), State.Job.IN_PROGRESS)
    );
  }

  private Expression<Boolean> createScheduledForTodayExpression(CriteriaBuilder cb, Root<Job> root) {
    LocalDate today = LocalDate.now(zoneId);
    long todayStart = today.atStartOfDay(zoneId).toInstant().getEpochSecond();
    long tomorrowStart = today.plusDays(1).atStartOfDay(zoneId).toInstant().getEpochSecond();
    return cb.and(
      cb.greaterThanOrEqualTo(root.get("expectedStartDate"), todayStart),
      cb.lessThan(root.get("expectedStartDate"), tomorrowStart)
    );
  }

  private Long totalCount(CriteriaBuilder cb, Predicate... predicates) {
    CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
    Root<Job> countRoot = countQuery.from(Job.class);
    Join<Job, TaskExecution> jobToTaskExecution = countRoot.join("taskExecutions", JoinType.INNER);
    Join<TaskExecution, TaskExecutionUserMapping> taskExecutionToUserMapping = jobToTaskExecution.join("assignees", JoinType.INNER);
    Join<TaskExecutionUserMapping, UserGroup> userGroupJoin = taskExecutionToUserMapping.join("userGroup", JoinType.LEFT);
    Join<UserGroup, UserGroupMember> userGroupMemberJoin = userGroupJoin.join("userGroupMembers", JoinType.LEFT);
    Join<TaskExecution, ParameterValue> taskExecutionToParameterValue = jobToTaskExecution.join("parameterValues", JoinType.LEFT);
    countQuery.select(cb.countDistinct(countRoot)).where(predicates);
    return entityManager.createQuery(countQuery).getSingleResult();
  }

  private static Predicate addPendingTasksAssignedToCurrentUserPredicate(CriteriaBuilder cb, Root<Job> root) {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Long currentUserId = principalUser.getId();

    Set<State.TaskExecution> pendingStates = Misc.TASK_EXECUTION_PENDING_STATES_ENUMS;
    Set<State.Job> pendingJobStates = Misc.JOB_PENDING_STATES_ENUMS;
    Predicate jobIsPending = root.get("state").in(pendingJobStates);
    Join<Job, TaskExecution> jobToTaskExecution = root.join("taskExecutions", JoinType.INNER);
    Predicate taskExecutionIsPending = jobToTaskExecution.get("state").in(pendingStates);
    Join<TaskExecution, TaskExecutionUserMapping> taskExecutionToUserMapping = jobToTaskExecution.join("assignees", JoinType.LEFT);
    Join<TaskExecutionUserMapping, UserGroup> userGroupJoin = taskExecutionToUserMapping.join("userGroup", JoinType.LEFT);
    Join<UserGroup, UserGroupMember> userGroupMemberJoin = userGroupJoin.join("userGroupMembers", JoinType.LEFT);

    Predicate taskAssignedToCurrentUser = cb.or(
      cb.equal(taskExecutionToUserMapping.get("usersId"), currentUserId),
      cb.equal(userGroupMemberJoin.get("usersId"), currentUserId)
    );
    Join<TaskExecution, ParameterValue> taskExecutionToParameterValue = jobToTaskExecution.join("parameterValues", JoinType.LEFT);
    Predicate parameterValueNotHidden = cb.isFalse(taskExecutionToParameterValue.get("hidden"));

    return cb.and(taskExecutionIsPending, taskAssignedToCurrentUser, jobIsPending, parameterValueNotHidden);
  }

  private static Predicate addCurrentUserPredicate(CriteriaBuilder cb, Root<Job> root) {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Long currentUserId = principalUser.getId();
    Join<Job, TaskExecution> jobToTaskExecution = root.join("taskExecutions", JoinType.INNER);
    Join<TaskExecution, TaskExecutionUserMapping> taskExecutionToUserMapping = jobToTaskExecution.join("assignees", JoinType.LEFT);
    Join<TaskExecutionUserMapping, UserGroup> userGroupJoin = taskExecutionToUserMapping.join("userGroup", JoinType.LEFT);
    Join<UserGroup, UserGroupMember> userGroupMemberJoin = userGroupJoin.join("userGroupMembers", JoinType.LEFT);

    Predicate taskAssignedToCurrentUser = cb.or(
      cb.equal(taskExecutionToUserMapping.get("usersId"), currentUserId),
      cb.equal(userGroupMemberJoin.get("usersId"), currentUserId)
    );
    return taskAssignedToCurrentUser;
  }

  private List<Long> getProcessParameterIds(List<Job> jobs) {
    Set<Long> checklistIds = jobs.stream()
      .map(Job::getChecklistId)
      .collect(Collectors.toSet());

    return parameterRepository.getParameterTargetEntityTypeByParameterIds(checklistIds);
  }

  private Map<Long, List<CorrectionDto>> getCorrectionDtoMap(Set<ParameterValueView> parameterValues) {
    Map<Long, List<CorrectionListViewProjection>> corrections = new HashMap<>();
    Map<Long, List<Media>> oldCorrectionIdMediaListMap = new HashMap<>();
    Map<Long, List<Media>> newCorrectionIdMediaListMap = new HashMap<>();

    for (ParameterValueView parameterValue : parameterValues) {
      if (parameterValue.getHasCorrections()) {
        Type.Parameter parameterType = parameterValue.getType();
        List<CorrectionListViewProjection> correctionList = correctionRepository.getAllCorrectionsByParameterValueId(parameterValue.getId());
        corrections.put(parameterValue.getId(), correctionList);

        for (CorrectionListViewProjection correction : correctionList) {
          Long correctionId = Long.parseLong(correction.getId());

          // Fetch old media mappings
          List<CorrectionMediaMapping> oldMediaMappingList;
          if (parameterType == Type.Parameter.SIGNATURE) {
            oldMediaMappingList = correctionMediaMappingRepository.findByCorrectionIdAndIsOldMediaAndArchived(correctionId, true, true);
          } else {
            oldMediaMappingList = correctionMediaMappingRepository.findByCorrectionIdAndIsOldMedia(correctionId, true);
          }
          List<Media> oldMediaList = oldMediaMappingList.stream()
            .map(CorrectionMediaMapping::getMedia)
            .collect(Collectors.toList());
          oldCorrectionIdMediaListMap.put(correctionId, oldMediaList);

          // Fetch new media mappings
          List<CorrectionMediaMapping> newMediaMappingList;
          if (parameterType == Type.Parameter.SIGNATURE) {
            newMediaMappingList = correctionMediaMappingRepository.findByCorrectionIdAndIsOldMediaAndArchived(correctionId, false, false);
          } else {
            newMediaMappingList = correctionMediaMappingRepository.findByCorrectionIdAndArchived(correctionId, false);
          }
          List<Media> newMediaList = newMediaMappingList.stream()
            .map(CorrectionMediaMapping::getMedia)
            .collect(Collectors.toList());
          newCorrectionIdMediaListMap.put(correctionId, newMediaList);
        }
      }
    }

    Map<Long, List<CorrectionDto>> correctionDtoMap = new HashMap<>();
    for (Map.Entry<Long, List<CorrectionListViewProjection>> entryMap : corrections.entrySet()) {
      Long parameterValueId = entryMap.getKey();
      List<CorrectionListViewProjection> correctionList = entryMap.getValue();

      List<CorrectionDto> correctionDtos = new ArrayList<>();
      for (CorrectionListViewProjection correction : correctionList) {
        Long correctionId = Long.parseLong(correction.getId());

        List<Corrector> correctorList = correctorRepository.findByCorrectionId(correctionId);
        Map<Long, List<CorrectorDto>> correctorIdMap = correctorList.stream()
          .collect(Collectors.groupingBy(Corrector::getCorrectionId, Collectors.mapping(correctorMapper::toDto, Collectors.toList())));

        List<Reviewer> reviewerList = reviewerRepository.findByCorrectionId(correctionId);
        Map<Long, List<ReviewerDto>> reviewerIdMap = reviewerList.stream()
          .collect(Collectors.groupingBy(Reviewer::getCorrectionId, Collectors.mapping(reviewerMapper::toDto, Collectors.toList())));

        CorrectionDto correctionDto = correctionMapper.toDto(correction, correctorIdMap, reviewerIdMap,
          oldCorrectionIdMediaListMap.get(correctionId), newCorrectionIdMediaListMap.get(correctionId));
        correctionDtos.add(correctionDto);
      }
      correctionDtoMap.put(parameterValueId, correctionDtos);
    }
    return correctionDtoMap;
  }

  private List<TaskPauseResumeAuditDto> convertToTaskPauseResumeDto(List<TaskPauseResumeAuditView> taskPauseResumeAuditList) {
    return taskPauseResumeAuditList.stream()
      .map(view -> {
        // Creating the pausedBy UserAuditDto object
        UserAuditDto pausedBy = new UserAuditDto(
          view.getPausedByUserId(),
          view.getPausedByEmployeeId(),
          view.getPausedByFirstName(),
          view.getPausedByLastName()
        );

        // Creating the resumedBy UserAuditDto object if resumedAt is not null
        UserAuditDto resumedBy = view.getResumedAt() != null ? new UserAuditDto(
          view.getResumedByUserId(),
          view.getResumedByEmployeeId(),
          view.getResumedByFirstName(),
          view.getResumedByLastName()
        ) : null;

        // Creating the TaskPauseResumeAuditDto object
        return new TaskPauseResumeAuditDto(
          view.getId(),
          view.getPausedAt(),
          view.getResumedAt(),
          view.getTaskExecutionId(),
          view.getReason(),
          view.getComment(),
          pausedBy,
          resumedBy
        );
      })
      .collect(Collectors.toList()); // Collecting the DTO list
  }

  private byte[] generateJobPdf(JobPrintDto jobPrintDto) throws IOException, ResourceNotFoundException {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    
    // Get facility information
    Facility facility = facilityRepository.findById(principalUser.getCurrentFacilityId())
      .orElseThrow(() -> new ResourceNotFoundException(
        principalUser.getCurrentFacilityId(), ErrorCode.FACILITY_NOT_FOUND,
        ExceptionType.ENTITY_NOT_FOUND));

    // Create facility DTO
    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setId(facility.getId().toString());
    facilityDto.setName(facility.getName());
    facilityDto.setTimeZone(facility.getTimeZone());
    facilityDto.setDateFormat(facility.getDateFormat());
    facilityDto.setTimeFormat(facility.getTimeFormat());
    facilityDto.setDateTimeFormat(facility.getDateTimeFormat());

    // Create PDF data with facility information
    GeneratedPdfDataDto pdfDataDto = new GeneratedPdfDataDto();
    pdfDataDto.setGeneratedOn(DateTimeUtils.now());
    pdfDataDto.setUserFullName(Utility.getFullNameAndEmployeeId(principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId()));
    pdfDataDto.setUserId(principalUser.getEmployeeId());
    pdfDataDto.setTimezoneOffset(facility.getTimeZone());
    pdfDataDto.setDateFormat(facility.getDateFormat());
    pdfDataDto.setDateTimeFormat(facility.getDateTimeFormat());
    pdfDataDto.setFacility(facilityDto);
    pdfDataDto.setJobPrintDto(jobPrintDto);
    
    return pdfGeneratorUtil.generatePdf(Type.PdfType.JOB_REPORT, pdfDataDto);
  }

}
