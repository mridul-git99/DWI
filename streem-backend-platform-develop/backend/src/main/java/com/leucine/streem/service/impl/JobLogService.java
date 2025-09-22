package com.leucine.streem.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leucine.streem.ObjectTypeCustomView;
import com.leucine.streem.collections.*;
import com.leucine.streem.collections.helper.MongoFilter;
import com.leucine.streem.collections.partial.PartialEntityObject;
import com.leucine.streem.config.MediaConfig;
import com.leucine.streem.constant.JobLogMisc;
import com.leucine.streem.constant.State;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.FacilityDto;
import com.leucine.streem.dto.GeneratedPdfDataDto;
import com.leucine.streem.dto.ResourceParameterChoiceDto;
import com.leucine.streem.dto.UserAuditDto;
import com.leucine.streem.dto.mapper.ISearchFilterMapper;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.ExceptionType;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.model.Property;
import com.leucine.streem.model.Relation;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.JobLogColumn;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.model.helper.parameter.*;
import com.leucine.streem.model.helper.search.SearchCriteria;
import com.leucine.streem.model.helper.search.SearchFilter;
import com.leucine.streem.model.helper.search.SearchOperator;
import com.leucine.streem.repository.*;
import com.leucine.streem.service.ICustomViewService;
import com.leucine.streem.service.IFacilityService;
import com.leucine.streem.service.IJobLogService;
import com.leucine.streem.util.DateTimeUtils;
import com.leucine.streem.util.JsonUtils;
import com.leucine.streem.util.PdfGeneratorUtil;
import com.leucine.streem.util.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.leucine.streem.constant.JobLogMisc.*;
import static com.leucine.streem.constant.Type.CustomViewFilterType.values;
import static com.leucine.streem.util.WorkbookUtils.getXSSFFont;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobLogService implements IJobLogService {
  private static final String NUMBER_PART_OF_CALCULATION = "[-+]?\\d*\\.\\d+|\\d+";
  private final IJobLogRepository logItemRepository;
  private final IParameterRepository parameterRepository;
  private final IParameterValueRepository parameterValueRepository;
  private final MediaConfig mediaConfig;
  private final MongoTemplate mongoTemplate;
  private final ICustomViewService customViewService;
  private final ObjectMapper objectMapper;
  private final IFacilityService facilityService;
  private final IJobRepository jobRepository;
  private final IFacilityRepository facilityRepository;
  private final IEntityObjectRepository entityObjectRepository;
  private final PdfGeneratorUtil pdfGeneratorUtil;
  private final IUserRepository userRepository;
  private final IChecklistRepository checklistRepository;
  private final IObjectTypeCustomViewRepository objectTypeCustomViewRepository;

  private static final Pattern NUMBER_PATTERN = Pattern.compile(NUMBER_PART_OF_CALCULATION);


  @Override
  public Page<JobLog> findAllJobLogs(String filters, String customViewId, Pageable pageable) throws ResourceNotFoundException {
    Query query = MongoFilter.buildQuery(filters);
    long count = mongoTemplate.count(query, JobLog.class);
    query.with(pageable);
    List<JobLog> jobLogList = mongoTemplate.find(query, JobLog.class);
//    if (customViewId != null) {
//      CustomView customView = customViewService.getCustomViewById(customViewId.toString());
//      List<CustomViewColumn> columns = customView.getColumns();
//      Set<String> uniqueColumnName = columns.stream().map(customViewColumn -> customViewColumn.getId() + customViewColumn.getTriggerType())
//        .collect(Collectors.toSet());
//      for (JobLog jobLog : jobLogList) {
//        List<JobLogData> logs = jobLog.getLogs();
//        List<JobLogData> filteredLogs = logs.stream().filter(jobLogData -> ((uniqueColumnName.contains(jobLogData.getEntityId() + jobLogData.getTriggerType()))
//            || jobLogData.getTriggerType().equals(Type.JobLogTriggerType.RESOURCE)))
//          .collect(Collectors.toList());
//        jobLog.setLogs(filteredLogs);
//      }
//    }
    return PageableExecutionUtils.getPage(jobLogList, pageable, () -> count);
  }

  @Override
  public JobLog findByJobId(String jobId) {
    return logItemRepository.findById(jobId).get();
  }

  @Override
  public void createJobLog(String jobId, String jobCode, State.Job jobState, Long jobCreatedAt, UserAuditDto jobCreatedBy, String checklistId, String checklistName,
                           String checklistCode, String facilityId, PrincipalUser principalUser) {
    log.info("[createJobLog] Request to create a new job log record, jobId: {}, jobCode: {}, checklistId: {}, facilityId: {}", jobId, jobCode, checklistId, facilityId);
    JobLog logItem = new JobLog();
    logItem.setId(jobId);
    logItem.setCreatedBy(jobCreatedBy);
    logItem.setModifiedBy(jobCreatedBy);
    logItem.setCode(jobCode);
    logItem.setState(jobState);
    logItem.setChecklistCode(checklistCode);
    logItem.setChecklistName(checklistName);
    logItem.setFacilityId(facilityId);
    logItem.setChecklistId(checklistId);
    logItem.setCreatedAt(jobCreatedAt);
    logItem.setModifiedAt(jobCreatedAt);
    var jobLogColumns = new ArrayList<JobLogData>();

    String fullName = Utility.getFullNameAndEmployeeIdFromPrincipalUser(principalUser);
    String createdAt = String.valueOf(jobCreatedAt);
    JobLogData jobIdLog = getJobLogData(JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_ID, JobLogMisc.JOB, null, jobCode, jobCode);
    JobLogData checklistIdLog = getJobLogData(JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.CHK_ID, JobLogMisc.PROCESS, null, checklistCode, checklistCode);
    JobLogData checklistNameLog = getJobLogData(JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.CHK_NAME, JobLogMisc.PROCESS, null, checklistName, checklistName);
    JobLogData jobStateLog = getJobLogData(JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_STATE, JobLogMisc.JOB, null, jobState.getDisplayName(), jobState.name());
    JobLogData jobCreatedAtLog = getJobLogData(JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_CREATED_AT, JobLogMisc.JOB, null, createdAt, createdAt);
    JobLogData jobCreatedByLog = getJobLogData(JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_CREATED_BY, JobLogMisc.JOB, null, fullName, principalUser.getIdAsString());
    JobLogData jobModifiedByLog = getJobLogData(JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_MODIFIED_BY, JobLogMisc.JOB, null, fullName, principalUser.getIdAsString());
    JobLogData jobModifiedAtLog = getJobLogData(JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_MODIFIED_AT, JobLogMisc.JOB, null, createdAt, createdAt);
    JobLogData jobCweReasonLog = getJobLogData(JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_CWE_REASON, JobLogMisc.CJWE_REASON, null, "", "");
    JobLogData jobCweCommentLog = getJobLogData(JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_CWE_COMMENT, JobLogMisc.CJWE_COMMENT, null, "", "");
    JobLogData jobCweFileLog = getJobLogData(JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_CWE_FILE, JobLogMisc.CJWE_FILE, null, "", "");
    jobLogColumns.add(jobIdLog);
    jobLogColumns.add(checklistIdLog);
    jobLogColumns.add(checklistNameLog);
    jobLogColumns.add(jobStateLog);
    jobLogColumns.add(jobCreatedAtLog);
    jobLogColumns.add(jobCreatedByLog);
    jobLogColumns.add(jobModifiedByLog);
    jobLogColumns.add(jobModifiedAtLog);
    jobLogColumns.add(jobCweReasonLog);
    jobLogColumns.add(jobCweCommentLog);
    jobLogColumns.add(jobCweFileLog);

    try {
      logItem.setLogs(jobLogColumns);
      logItemRepository.save(logItem);
    } catch (Exception ex) {
      log.error("[createJobLog] error creating job log record", ex);
    }
  }

  @Override
  public List<JobLogColumn> getJobLogColumnForChecklist(Checklist checklist) {
    List<JobLogColumn> jobLogColumns = new ArrayList<>();

    jobLogColumns.add(getJobLogColumn(JobLog.COMMON_COLUMN_ID, Type.JobLogColumnType.DATE_TIME, Type.JobLogTriggerType.JOB_START_TIME, JobLogMisc.JOB));
    jobLogColumns.add(getJobLogColumn(JobLog.COMMON_COLUMN_ID, Type.JobLogColumnType.DATE_TIME, Type.JobLogTriggerType.JOB_CREATED_AT, JobLogMisc.JOB));
    jobLogColumns.add(getJobLogColumn(JobLog.COMMON_COLUMN_ID, Type.JobLogColumnType.TEXT, Type.JobLogTriggerType.JOB_CREATED_BY, JobLogMisc.JOB));
    jobLogColumns.add(getJobLogColumn(JobLog.COMMON_COLUMN_ID, Type.JobLogColumnType.TEXT, Type.JobLogTriggerType.JOB_MODIFIED_BY, JobLogMisc.JOB));
    jobLogColumns.add(getJobLogColumn(JobLog.COMMON_COLUMN_ID, Type.JobLogColumnType.TEXT, Type.JobLogTriggerType.JOB_ID, JobLogMisc.JOB));
    jobLogColumns.add(getJobLogColumn(JobLog.COMMON_COLUMN_ID, Type.JobLogColumnType.TEXT, Type.JobLogTriggerType.JOB_STATE, JobLogMisc.JOB));
    jobLogColumns.add(getJobLogColumn(JobLog.COMMON_COLUMN_ID, Type.JobLogColumnType.TEXT, Type.JobLogTriggerType.CHK_ID, JobLogMisc.PROCESS));
    jobLogColumns.add(getJobLogColumn(JobLog.COMMON_COLUMN_ID, Type.JobLogColumnType.TEXT, Type.JobLogTriggerType.CHK_NAME, JobLogMisc.PROCESS));
    jobLogColumns.add(getJobLogColumn(JobLog.COMMON_COLUMN_ID, Type.JobLogColumnType.TEXT, Type.JobLogTriggerType.ANNOTATION_REMARK, ANNOTATION_REMARK));
    jobLogColumns.add(getJobLogColumn(JobLog.COMMON_COLUMN_ID, Type.JobLogColumnType.FILE, Type.JobLogTriggerType.ANNOTATION_MEDIA, ANNOTATION_MEDIA));
    jobLogColumns.add(getJobLogColumn(JobLog.COMMON_COLUMN_ID, Type.JobLogColumnType.DATE_TIME, Type.JobLogTriggerType.JOB_MODIFIED_AT, JobLogMisc.JOB));
    jobLogColumns.add(getJobLogColumn(JobLog.COMMON_COLUMN_ID, Type.JobLogColumnType.TEXT, Type.JobLogTriggerType.JOB_STARTED_BY, JobLogMisc.JOB));
    jobLogColumns.add(getJobLogColumn(JobLog.COMMON_COLUMN_ID, Type.JobLogColumnType.TEXT, Type.JobLogTriggerType.JOB_CWE_REASON, JobLogMisc.CJWE_REASON));
    jobLogColumns.add(getJobLogColumn(JobLog.COMMON_COLUMN_ID, Type.JobLogColumnType.TEXT, Type.JobLogTriggerType.JOB_CWE_COMMENT, JobLogMisc.CJWE_COMMENT));
    jobLogColumns.add(getJobLogColumn(JobLog.COMMON_COLUMN_ID, Type.JobLogColumnType.FILE, Type.JobLogTriggerType.JOB_CWE_FILE, JobLogMisc.CJWE_FILE));

    var processParameters = parameterRepository.getParametersByChecklistIdAndTargetEntityType(checklist.getId(), Type.ParameterTargetEntityType.PROCESS);
    for (Parameter parameter : processParameters) {
      switch (parameter.getType()) {
        case YES_NO, MULTISELECT, SINGLE_SELECT, MULTI_LINE, SINGLE_LINE, NUMBER ->
          jobLogColumns.add(getJobLogColumn(parameter.getIdAsString(), Type.JobLogColumnType.TEXT, Type.JobLogTriggerType.PROCESS_PARAMETER_VALUE, parameter.getLabel()));
        case DATE ->
          jobLogColumns.add(getJobLogColumn(parameter.getIdAsString(), Type.JobLogColumnType.DATE, Type.JobLogTriggerType.PROCESS_PARAMETER_VALUE, parameter.getLabel()));
        case DATE_TIME ->
          jobLogColumns.add(getJobLogColumn(parameter.getIdAsString(), Type.JobLogColumnType.DATE_TIME, Type.JobLogTriggerType.PROCESS_PARAMETER_VALUE, parameter.getLabel()));
        case MEDIA, FILE_UPLOAD, SIGNATURE ->
          jobLogColumns.add(getJobLogColumn(parameter.getIdAsString(), Type.JobLogColumnType.FILE, Type.JobLogTriggerType.PROCESS_PARAMETER_VALUE, parameter.getLabel()));
        case RESOURCE, MULTI_RESOURCE ->
          jobLogColumns.add(getJobLogColumn(parameter.getIdAsString(), Type.JobLogColumnType.TEXT, Type.JobLogTriggerType.RESOURCE_PARAMETER, parameter.getLabel()));
      }
    }

    for (Stage stage : checklist.getStages()) {
      for (Task task : stage.getTasks()) {
        jobLogColumns.add(getJobLogColumn(task.getIdAsString(), Type.JobLogColumnType.DATE_TIME, Type.JobLogTriggerType.TSK_START_TIME, task.getName()));
        jobLogColumns.add(getJobLogColumn(task.getIdAsString(), Type.JobLogColumnType.TEXT, Type.JobLogTriggerType.TSK_STARTED_BY, task.getName()));

        for (Parameter parameter : task.getParameters()) {
          if (parameter.getVerificationType() != Type.VerificationType.NONE) {
            switch (parameter.getVerificationType()) {
              case SELF -> {
                jobLogColumns.add(getJobLogColumn(parameter.getIdAsString(), Type.JobLogColumnType.TEXT, Type.JobLogTriggerType.PARAMETER_SELF_VERIFIED_BY, parameter.getLabel()));
                jobLogColumns.add(getJobLogColumn(parameter.getIdAsString(), Type.JobLogColumnType.DATE_TIME, Type.JobLogTriggerType.PARAMETER_SELF_VERIFIED_AT, parameter.getLabel()));
              }
              case PEER -> {
                jobLogColumns.add(getJobLogColumn(parameter.getIdAsString(), Type.JobLogColumnType.TEXT, Type.JobLogTriggerType.PARAMETER_PEER_VERIFIED_BY, parameter.getLabel()));
                jobLogColumns.add(getJobLogColumn(parameter.getIdAsString(), Type.JobLogColumnType.DATE_TIME, Type.JobLogTriggerType.PARAMETER_PEER_VERIFIED_AT, parameter.getLabel()));
                jobLogColumns.add(getJobLogColumn(parameter.getIdAsString(), Type.JobLogColumnType.TEXT, Type.JobLogTriggerType.PARAMETER_PEER_STATUS, parameter.getLabel()));
              }
              case BOTH -> {
                jobLogColumns.add(getJobLogColumn(parameter.getIdAsString(), Type.JobLogColumnType.TEXT, Type.JobLogTriggerType.PARAMETER_SELF_VERIFIED_BY, parameter.getLabel()));
                jobLogColumns.add(getJobLogColumn(parameter.getIdAsString(), Type.JobLogColumnType.DATE_TIME, Type.JobLogTriggerType.PARAMETER_SELF_VERIFIED_AT, parameter.getLabel()));
                jobLogColumns.add(getJobLogColumn(parameter.getIdAsString(), Type.JobLogColumnType.TEXT, Type.JobLogTriggerType.PARAMETER_PEER_VERIFIED_BY, parameter.getLabel()));
                jobLogColumns.add(getJobLogColumn(parameter.getIdAsString(), Type.JobLogColumnType.DATE_TIME, Type.JobLogTriggerType.PARAMETER_PEER_VERIFIED_AT, parameter.getLabel()));
                jobLogColumns.add(getJobLogColumn(parameter.getIdAsString(), Type.JobLogColumnType.TEXT, Type.JobLogTriggerType.PARAMETER_PEER_STATUS, parameter.getLabel()));
              }
            }
          }
          switch (parameter.getType()) {
            case YES_NO, MULTISELECT, SINGLE_SELECT, SINGLE_LINE, MULTI_LINE, SHOULD_BE, NUMBER, CALCULATION ->
              jobLogColumns.add(getJobLogColumn(parameter.getIdAsString(), Type.JobLogColumnType.TEXT, Type.JobLogTriggerType.PARAMETER_VALUE, parameter.getLabel()));
            case DATE ->
              jobLogColumns.add(getJobLogColumn(parameter.getIdAsString(), Type.JobLogColumnType.DATE, Type.JobLogTriggerType.PARAMETER_VALUE, parameter.getLabel()));
            case DATE_TIME ->
              jobLogColumns.add(getJobLogColumn(parameter.getIdAsString(), Type.JobLogColumnType.DATE_TIME, Type.JobLogTriggerType.PARAMETER_VALUE, parameter.getLabel()));
            case MEDIA, FILE_UPLOAD, SIGNATURE ->
              jobLogColumns.add(getJobLogColumn(parameter.getIdAsString(), Type.JobLogColumnType.FILE, Type.JobLogTriggerType.PARAMETER_VALUE, parameter.getLabel()));
            case RESOURCE, MULTI_RESOURCE ->
              jobLogColumns.add(getJobLogColumn(parameter.getIdAsString(), Type.JobLogColumnType.TEXT, Type.JobLogTriggerType.RESOURCE_PARAMETER, parameter.getLabel()));
          }
        }
        jobLogColumns.add(getJobLogColumn(task.getIdAsString(), Type.JobLogColumnType.DATE_TIME, Type.JobLogTriggerType.TSK_END_TIME, task.getName()));
        jobLogColumns.add(getJobLogColumn(task.getIdAsString(), Type.JobLogColumnType.TEXT, Type.JobLogTriggerType.TSK_ENDED_BY, task.getName()));
      }
    }

    for (Relation relation : checklist.getRelations()) {
      jobLogColumns.add(getJobLogColumn(relation.getIdAsString(), Type.JobLogColumnType.TEXT, Type.JobLogTriggerType.RELATION_VALUE, relation.getDisplayName()));
    }

    jobLogColumns.add(getJobLogColumn(JobLog.COMMON_COLUMN_ID, Type.JobLogColumnType.DATE_TIME, Type.JobLogTriggerType.JOB_END_TIME, JobLogMisc.JOB));
    jobLogColumns.add(getJobLogColumn(JobLog.COMMON_COLUMN_ID, Type.JobLogColumnType.TEXT, Type.JobLogTriggerType.JOB_ENDED_BY, JobLogMisc.JOB));

    return jobLogColumns;
  }

  @Override
  public void recordJobLogTrigger(String jobId, String entityId, Type.JobLogTriggerType jobLogTriggerType, String label, List<JobLogMediaData> medias,
                                  String value, String identifierValue, UserAuditDto modifiedBy) {
    recordJobLogTrigger(jobId, entityId, jobLogTriggerType, label, medias, value, identifierValue, null, modifiedBy);
  }

  @Override
  public void recordJobLogTrigger(String jobId, String entityId, Type.JobLogTriggerType jobLogTriggerType, String label, List<JobLogMediaData> medias, String value, String identifierValue, Type.Parameter parameterType, UserAuditDto userAuditDto) {
    recordJobLogTrigger(jobId, entityId, jobLogTriggerType, label, medias, value, identifierValue, parameterType, userAuditDto, null);
  }

  @Override
  //TODO: Removed @Async annotation move this to single mongo query upsert
  public void recordJobLogTrigger(String jobId, String entityId, Type.JobLogTriggerType jobLogTriggerType, String label, List<JobLogMediaData> medias,
                                  String value, String identifierValue, Type.Parameter parameterType, UserAuditDto modifiedBy, List<String> choices) {
    String displayName = getDisplayNameByLabelAndTriggerType(label, jobLogTriggerType);

    JobLogData jobLogData = new JobLogData();
    jobLogData.setMedias(medias);
    jobLogData.setValue(value);
    jobLogData.setIdentifierValue(identifierValue);
    jobLogData.setEntityId(entityId);
    jobLogData.setTriggerType(jobLogTriggerType);
    jobLogData.setDisplayName(displayName);

    setOrUpdateLogItem(jobId, jobLogTriggerType, parameterType, jobLogData, modifiedBy, choices);
  }

  @Override
  public void recordJobLogResource(String jobId, String parameterId, String parameterLabel, String objectTypeId, String objectTypeDisplayName, List<ResourceParameterChoiceDto> choices) {
    var jobLog = logItemRepository.findById(jobId).get();

    var jobLogDataToCreateOrUpdate = new JobLogData();
    boolean jobLogRecordFound = false;
    for (JobLogData jobLogData : jobLog.getLogs()) {
      if (jobLogData.getEntityId().equals(objectTypeId) && jobLogData.getTriggerType().equals(Type.JobLogTriggerType.RESOURCE)) {
        jobLogRecordFound = true;
        jobLogDataToCreateOrUpdate = jobLogData;
        jobLogData.setMedias(null);
        jobLogData.setValue(null);
        jobLogData.setIdentifierValue(null);
      }
    }

    jobLogDataToCreateOrUpdate.setEntityId(objectTypeId);
    jobLogDataToCreateOrUpdate.setTriggerType(Type.JobLogTriggerType.RESOURCE);
    jobLogDataToCreateOrUpdate.setDisplayName(objectTypeDisplayName);

    JobLogResource jobLogResource = new JobLogResource();
    jobLogResource.setDisplayName(parameterLabel);
    jobLogResource.setChoices(choices);
    jobLogDataToCreateOrUpdate.getResourceParameters().put(parameterId, jobLogResource);

    if (!jobLogRecordFound) {
      jobLog.getLogs().add(jobLogDataToCreateOrUpdate);
    }
    logItemRepository.save(jobLog);
  }

  @Override
  public void updateJobState(String jobId, PrincipalUser principalUser) {
    var jobLog = logItemRepository.findById(jobId).get();
    State.Job jobState = jobRepository.getStateByJobId(Long.valueOf(jobId));
    for (JobLogData jobLogData : jobLog.getLogs()) {
      if (jobLogData.getTriggerType().equals(Type.JobLogTriggerType.JOB_STATE)) {
        jobLogData.setValue(jobState.name());
        jobLogData.setIdentifierValue(jobState.name());
      }
      if (jobLogData.getTriggerType().equals(Type.JobLogTriggerType.JOB_MODIFIED_BY)) {
        String fullName = Utility.getFullNameAndEmployeeIdFromPrincipalUser(principalUser);
        jobLogData.setValue(fullName);
        jobLogData.setIdentifierValue(fullName);
      }
      if (jobLogData.getTriggerType().equals(Type.JobLogTriggerType.JOB_MODIFIED_AT)) {
        jobLogData.setValue(String.valueOf(DateTimeUtils.now()));
        jobLogData.setIdentifierValue(String.valueOf(DateTimeUtils.now()));
      }
    }
    jobLog.setState(jobState);
    logItemRepository.save(jobLog);
  }

  @Override
  public void updateJobLog(Long jobId, Long parameterId, Type.Parameter parameterType, String parameterValueReason,
                           String label, Type.JobLogTriggerType triggerType, UserAuditDto userAuditDto) throws IOException {
    Parameter parameter = parameterRepository.findById(parameterId).get();
    ParameterValue parameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, parameter.getId());
    switch (parameterType) {
      case MULTI_LINE, SINGLE_LINE, SHOULD_BE, NUMBER, DATE, DATE_TIME -> {
        recordJobLogTrigger(String.valueOf(jobId), String.valueOf(parameter.getId()), triggerType, label, null, parameterValue.getValue(), parameterValue.getValue(), parameterType, userAuditDto);
      }
      case CALCULATION -> {
        CalculationParameter calculationParameter = JsonUtils.readValue(parameter.getData().toString(), CalculationParameter.class);
        String uom = calculationParameter.getUom() != null ? calculationParameter.getUom() : "";
        String valueWithUOM = parameterValue.getValue() + Utility.SPACE + uom;
        recordJobLogTrigger(String.valueOf(jobId), String.valueOf(parameter.getId()), triggerType, label, null, valueWithUOM, valueWithUOM, parameterType, userAuditDto);
      }
      case CHECKLIST, MULTISELECT, SINGLE_SELECT -> {
        List<ChoiceParameterBase> parameters = JsonUtils.jsonToCollectionType(parameter.getData().toString(), List.class, ChoiceParameterBase.class);
        Map<String, String> optionsNameMap = parameters.stream().collect(
          Collectors.toMap(ChoiceParameterBase::getId, ChoiceParameterBase::getName));
        JsonNode oldChoices = parameterValue.getChoices();
        Map<String, String> result = objectMapper.convertValue(oldChoices, new TypeReference<>() {
        });
        List<String> selectedItems = new ArrayList<>();
        List<String> selectedIdentifierItems = new ArrayList<>();

        for (ChoiceParameterBase choiceParameter : parameters) {
          if (!Utility.isEmpty(result) && result.containsKey(choiceParameter.getId())) {
            String state = result.get(choiceParameter.getId());
            if (!Utility.isEmpty(state) && State.Selection.SELECTED.name().equals(state)) {
              selectedItems.add(optionsNameMap.get(choiceParameter.getId()));
              selectedIdentifierItems.add(choiceParameter.getId());
            }
          }
        }
        String value = String.join(",", selectedItems);
        String identifierValue = String.join(",", selectedIdentifierItems);

        recordJobLogTrigger(String.valueOf(jobId), String.valueOf(parameter.getId()), triggerType, label, null, value, identifierValue, parameterType, userAuditDto, selectedIdentifierItems);
      }
      case YES_NO -> {
        List<YesNoParameter> parameters = JsonUtils.jsonToCollectionType(parameter.getData().toString(), List.class, YesNoParameter.class);
        Map<String, String> optionsNameMap = parameters.stream().collect(
          Collectors.toMap(ChoiceParameterBase::getId, ChoiceParameterBase::getName));
        ParameterValue activityChoiceValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, parameter.getId());
        JsonNode oldChoices = activityChoiceValue.getChoices();
        Map<String, String> result = objectMapper.convertValue(oldChoices, new TypeReference<>() {
        });
        List<String> selectedItems = new ArrayList<>();
        List<String> selectedIdentifierItems = new ArrayList<>();

        String reason = "";
        for (YesNoParameter yesNoParameter : parameters) {
          if (!Utility.isEmpty(result) && result.containsKey(yesNoParameter.getId())) {
            String state = result.get(yesNoParameter.getId());
            if (!Utility.isEmpty(state) && State.Selection.SELECTED.name().equals(state)) {
              selectedItems.add(optionsNameMap.get(yesNoParameter.getId()));
              selectedIdentifierItems.add(yesNoParameter.getId());

              if ("no".equals(yesNoParameter.getType())) {
                reason = parameterValueReason;
              } else {
                reason = "";
              }
            }
          }
        }
        String value = String.join(",", selectedItems);
        String identifierValue = String.join(",", selectedIdentifierItems);

        if (!Utility.isEmpty(reason)) {
          value = String.join(",", value, reason);
        }
        recordJobLogTrigger(String.valueOf(jobId), String.valueOf(parameter.getId()), triggerType, label, null, value, identifierValue, parameterType, userAuditDto, selectedIdentifierItems);
      }
      case MEDIA, FILE_UPLOAD, SIGNATURE -> {
        List<ParameterValueMediaMapping> medias = parameterValue.getMedias();
        List<JobLogMediaData> jobLogMedias = new ArrayList<>();
        for (ParameterValueMediaMapping parameterValueMediaMapping : medias) {
          if (!parameterValueMediaMapping.isArchived()) {
            Media parameterValueMedia = parameterValueMediaMapping.getMedia();
            JobLogMediaData jobLogMediaData = new JobLogMediaData();

            jobLogMediaData.setName(Type.Parameter.SIGNATURE.equals(parameterType) ? "Signature" : parameterValueMedia.getName());
            jobLogMediaData.setType(parameterValueMedia.getType());
            jobLogMediaData.setDescription(parameterValueMedia.getDescription());
            String link = mediaConfig.getCdn() + java.io.File.separator + parameterValueMedia.getRelativePath() + java.io.File.separator + parameterValueMedia.getFilename();
            jobLogMediaData.setLink(link);
            jobLogMedias.add(jobLogMediaData);
          }
        }

        recordJobLogTrigger(String.valueOf(jobId), String.valueOf(parameter.getId()), triggerType, label, jobLogMedias, null, null, parameterType, userAuditDto);
      }
      case RESOURCE, MULTI_RESOURCE -> {
        ResourceParameter resourceParameter = JsonUtils.readValue(parameter.getData().toString(), ResourceParameter.class);
        List<ResourceParameterChoiceDto> choices = !Utility.isEmpty(parameterValue.getChoices())
          ? JsonUtils.jsonToCollectionType(parameterValue.getChoices(), List.class, ResourceParameterChoiceDto.class)
          : new ArrayList<>();
        List<String> selectedItems = new ArrayList<>();
        List<String> selectedIdentifierItems = new ArrayList<>();
        for (ResourceParameterChoiceDto choice : choices) {
          selectedIdentifierItems.add(choice.getObjectId());
          selectedItems.add(choice.getObjectExternalId());
        }
        String value = String.join(",", selectedItems);
        String identifierValue = String.join(",", selectedIdentifierItems);
        recordJobLogTrigger(String.valueOf(jobId), String.valueOf(parameter.getId()), Type.JobLogTriggerType.RESOURCE_PARAMETER,
          label, null, value, identifierValue, parameterType, userAuditDto, selectedIdentifierItems);
        recordJobLogResource(String.valueOf(jobId), String.valueOf(parameter.getId()), parameter.getLabel(),
          resourceParameter.getObjectTypeId().toString(), resourceParameter.getObjectTypeDisplayName(), choices);
      }
    }
  }

  private JobLogColumn getJobLogColumn(String entityId, Type.JobLogColumnType jobLogColumnType, Type.JobLogTriggerType triggerType, String label) {
    var jobLogColumn = new JobLogColumn();
    jobLogColumn.setId(entityId);
    jobLogColumn.setType(jobLogColumnType);
    jobLogColumn.setTriggerType(triggerType);
    jobLogColumn.setDisplayName(getDisplayNameByLabelAndTriggerType(label, triggerType));
    return jobLogColumn;
  }

  private JobLogData getJobLogData(String entityId, Type.JobLogTriggerType jobLogTriggerType, String label, List<JobLogMediaData> medias,
                                   String value, String identifierValue) {
    String displayName = getDisplayNameByLabelAndTriggerType(label, jobLogTriggerType);

    JobLogData jobLogData = new JobLogData();
    jobLogData.setMedias(medias);
    jobLogData.setValue(value);
    jobLogData.setIdentifierValue(identifierValue);
    jobLogData.setEntityId(entityId);
    jobLogData.setTriggerType(jobLogTriggerType);
    jobLogData.setDisplayName(displayName);
    return jobLogData;
  }

  private String getDisplayNameByLabelAndTriggerType(String label, Type.JobLogTriggerType jobLogTriggerType) {
    StringBuilder builder = new StringBuilder(label);
    switch (jobLogTriggerType) {
      case CHK_ID, JOB_ID -> builder.append(Utility.SPACE).append(JobLogMisc.ID_SUFFIX);
      case JOB_STATE -> builder.append(Utility.SPACE).append(JobLogMisc.STATE_SUFFIX);
      case CHK_NAME -> builder.append(Utility.SPACE).append(JobLogMisc.NAME_SUFFIX);
      case JOB_CREATED_AT -> builder.append(Utility.SPACE).append(JobLogMisc.CREATED_AT_SUFFIX);
      case JOB_CREATED_BY -> builder.append(Utility.SPACE).append(JobLogMisc.CREATED_BY_SUFFIX);
      case JOB_MODIFIED_BY -> builder.append(Utility.SPACE).append(JobLogMisc.MODIFIED_BY_SUFFIX);
      case JOB_MODIFIED_AT -> builder.append(Utility.SPACE).append(JobLogMisc.MODIFIED_AT_SUFFIX);
      case JOB_START_TIME, TSK_START_TIME -> builder.append(Utility.SPACE).append(JobLogMisc.START_TIME_SUFFIX);
      case JOB_END_TIME, TSK_END_TIME -> builder.append(Utility.SPACE).append(JobLogMisc.END_TIME_SUFFIX);
      case JOB_STARTED_BY, TSK_STARTED_BY -> builder.append(Utility.SPACE).append(JobLogMisc.STARTED_BY_SUFFIX);
      case JOB_ENDED_BY, TSK_ENDED_BY -> builder.append(Utility.SPACE).append(JobLogMisc.ENDED_BY_SUFFIX);
      case PARAMETER_PEER_VERIFIED_AT, PARAMETER_PEER_VERIFIED_BY, PARAMETER_SELF_VERIFIED_AT,
           PARAMETER_SELF_VERIFIED_BY, PARAMETER_PEER_STATUS ->
        builder = new StringBuilder().append(getParameterVerificationCommonName(jobLogTriggerType))
          .append(Utility.SPACE)
          .append(Utility.HYPHEN)
          .append(Utility.SPACE)
          .append(label);
    }
    return builder.toString();
  }

  private String getParameterVerificationCommonName(Type.JobLogTriggerType jobLogTriggerType) {
    switch (jobLogTriggerType) {
      case PARAMETER_PEER_VERIFIED_AT, PARAMETER_PEER_VERIFIED_BY -> {
        return "Peer Verification";
      }
      case PARAMETER_SELF_VERIFIED_AT, PARAMETER_SELF_VERIFIED_BY -> {
        return "Self Verification";
      }
      case PARAMETER_PEER_STATUS -> {
        return "Peer Status";
      }
      default -> {
        return "";
      }
    }
  }

  private void setOrUpdateLogItem(String jobId, Type.JobLogTriggerType jobLogTriggerType, Type.Parameter parameterType, JobLogData jobLogDataToCreateOrUpdate, UserAuditDto modifiedBy, List<String> choices) {
    var jobLog = logItemRepository.findById(jobId).get();
    Long time = DateTimeUtils.now();
    jobLog.setModifiedAt(time);
    jobLog.setModifiedBy(modifiedBy);
    if (Type.JobLogTriggerType.JOB_ENDED_BY.equals(jobLogTriggerType)) {
      jobLog.setEndedBy(modifiedBy);
      jobLog.setEndedAt(time);
    }
    if (Type.JobLogTriggerType.JOB_STARTED_BY.equals(jobLogTriggerType)) {
      jobLog.setStartedBy(modifiedBy);
      jobLog.setStartedAt(time);
    }
    var logEntryExists = false;

    for (JobLogData jobLogData : jobLog.getLogs()) {
      if (jobLogDataToCreateOrUpdate.getEntityId().equals(jobLogData.getEntityId()) && jobLogDataToCreateOrUpdate.getTriggerType().equals(jobLogData.getTriggerType())) {
        logEntryExists = true;

        jobLogData.setValue(jobLogDataToCreateOrUpdate.getValue());
        jobLogData.setIdentifierValue(jobLogDataToCreateOrUpdate.getIdentifierValue());
        jobLogData.setEntityId(jobLogDataToCreateOrUpdate.getEntityId());
        jobLogData.setDisplayName(jobLogDataToCreateOrUpdate.getDisplayName());
        jobLogData.setTriggerType(jobLogDataToCreateOrUpdate.getTriggerType());
        jobLogData.setMedias(jobLogDataToCreateOrUpdate.getMedias());
      }
    }

    if (!logEntryExists) {
      jobLog.getLogs().add(jobLogDataToCreateOrUpdate);
    }

    Map<String, Object> parameterValues = new HashMap<>();


    Map<String, List<JobLogData>> parameterVerificationLogs = new HashMap<>();
    JobLogData logEntry = jobLog.getLogs().stream().filter(log -> log.getEntityId().equals(jobLogDataToCreateOrUpdate.getEntityId()) && log.getTriggerType().equals(jobLogTriggerType)).findFirst().orElse(null);
    if (logEntry != null) {
      Set<Type.JobLogTriggerType> types = new HashSet<>();
      types.add(Type.JobLogTriggerType.PROCESS_PARAMETER_VALUE);
      types.add(Type.JobLogTriggerType.PARAMETER_VALUE);
      types.add(Type.JobLogTriggerType.RESOURCE_PARAMETER);
      if (types.contains(logEntry.getTriggerType())) {

        Object value = null;
        switch (parameterType) {
          case CALCULATION -> {
            if (!Utility.isEmpty(logEntry.getIdentifierValue())) {
              Double numberPart = extractNumberFromString(logEntry.getIdentifierValue());
              if (!Utility.isEmpty(numberPart)) {
                value = Double.parseDouble(numberPart.toString());
              }
            }
          }
          case NUMBER, SHOULD_BE -> {
            if (!Utility.isEmpty(logEntry.getIdentifierValue())) {
              value = Double.parseDouble(logEntry.getIdentifierValue());
            }
          }
          case DATE, DATE_TIME -> {
            if (!Utility.isEmpty(logEntry.getIdentifierValue())) {
              value = Long.parseLong(logEntry.getIdentifierValue());
            }
          }
          case MULTI_LINE, SINGLE_LINE -> {
            value = logEntry.getIdentifierValue();
          }
          case CHECKLIST, MULTISELECT, SINGLE_SELECT, YES_NO, RESOURCE, MULTI_RESOURCE -> {
            value = choices;
          }
        }
        parameterValues.put(logEntry.getEntityId(), value);
      }
    }

    List<JobLogData> verificationLogs = jobLog.getLogs().stream().filter(log -> log.getEntityId().equals(jobLogDataToCreateOrUpdate.getEntityId()) && Type.VERIFICATION_TRIGGER_TYPES.contains(log.getTriggerType())).toList();

    for (JobLogData verificationLog : verificationLogs) {
      String parameterId = verificationLog.getEntityId();
      if (parameterVerificationLogs.containsKey(parameterId)) {
        parameterVerificationLogs.get(parameterId).add(verificationLog);
      } else {
        List<JobLogData> jobLogDataList = new ArrayList<>();
        jobLogDataList.add(verificationLog);
        parameterVerificationLogs.put(parameterId, jobLogDataList);
      }
    }

    Map<String, Object> updatedParamMap = jobLog.getParameterValues();
    if (updatedParamMap == null) {
      updatedParamMap = new HashMap<>();
    }
    updatedParamMap.putAll(parameterValues);
    jobLog.setParameterValues(updatedParamMap);
    jobLog.setVerifications(getAllVerifications(parameterVerificationLogs));
    logItemRepository.save(jobLog);
  }

  private static Double extractNumberFromString(String s) {
    Matcher matcher = NUMBER_PATTERN.matcher(s);
    if (matcher.find()) {
      return Double.parseDouble(matcher.group());
    } else {
      return null;
    }
  }

  /**
   * {
   * "parameterId": {
   * <p>
   * "self": {
   * "verifiedAt": "1623345600000",
   * "verifiedBy": "John Doe"
   * },
   * "peer": {
   * "verifiedAt": "1623345600000",
   * "verifiedBy": "John Doe"
   * }
   * }
   *
   * @param parameterVerificationLogs
   * @return
   */
  private Map<String, Object> getAllVerifications(Map<String, List<JobLogData>> parameterVerificationLogs) {
    Map<String, Object> verifications = new HashMap<>();
    for (String parameterId : parameterVerificationLogs.keySet()) {
      List<JobLogData> jobLogDataList = parameterVerificationLogs.get(parameterId);

      Map<String, String> selfVerificationValues = new HashMap<>();
      Map<String, String> peerVerificationValues = new HashMap<>();
      Map<String, Object> peerVerificationStatus = new HashMap<>();
      for (JobLogData jobLogData : jobLogDataList) {
        if (jobLogData.getTriggerType().equals(Type.JobLogTriggerType.PARAMETER_SELF_VERIFIED_AT)) {
          selfVerificationValues.put(getParameterVerificationTriggerType(Type.JobLogTriggerType.PARAMETER_SELF_VERIFIED_AT), jobLogData.getIdentifierValue());
        } else if (jobLogData.getTriggerType().equals(Type.JobLogTriggerType.PARAMETER_SELF_VERIFIED_BY)) {
          selfVerificationValues.put(getParameterVerificationTriggerType(Type.JobLogTriggerType.PARAMETER_SELF_VERIFIED_BY), jobLogData.getIdentifierValue());
        } else if (jobLogData.getTriggerType().equals(Type.JobLogTriggerType.PARAMETER_PEER_VERIFIED_AT)) {
          peerVerificationValues.put(getParameterVerificationTriggerType(Type.JobLogTriggerType.PARAMETER_PEER_VERIFIED_AT), jobLogData.getIdentifierValue());
        } else if (jobLogData.getTriggerType().equals(Type.JobLogTriggerType.PARAMETER_PEER_VERIFIED_BY)) {
          peerVerificationValues.put(getParameterVerificationTriggerType(Type.JobLogTriggerType.PARAMETER_PEER_VERIFIED_BY), jobLogData.getIdentifierValue());
        } else if (jobLogData.getTriggerType() == Type.JobLogTriggerType.PARAMETER_PEER_STATUS) {
          peerVerificationStatus.put(getParameterVerificationTriggerType(Type.JobLogTriggerType.PARAMETER_PEER_STATUS), jobLogData.getIdentifierValue());
        }
      }
      verifications.put(parameterId, Map.of("self", selfVerificationValues, "peer", peerVerificationValues, "status", peerVerificationStatus));
    }
    return verifications;
  }

  private String getParameterVerificationTriggerType(Type.JobLogTriggerType triggerType) {
    switch (triggerType) {
      case PARAMETER_SELF_VERIFIED_AT, PARAMETER_PEER_VERIFIED_AT -> {
        return "verifiedAt";
      }
      case PARAMETER_SELF_VERIFIED_BY, PARAMETER_PEER_VERIFIED_BY -> {
        return "verifiedBy";
      }
      case PARAMETER_PEER_STATUS -> {
        return "status";
      }
      default -> {
        return "";
      }
    }
  }

  /* TODO
  Check if we can use FileOutputStream as using ByteArrayOutputStream the whole file will be created in memory causing performance issue for large data.
  Also, need to add data in chunks (X records) and in parallel stream the file
   */
  @Override
  public ByteArrayInputStream createJobLogExcel(String customViewId, String filters) throws ResourceNotFoundException, IOException {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
         Workbook wb = new XSSFWorkbook()
    ) {
      CustomView customView = customViewService.getCustomViewById(customViewId);
      Facility facility = facilityRepository.getReferenceById(Long.valueOf(customView.getFacilityId()));
      String facilityTimeZone = facility.getTimeZone();
      ZoneId zoneId = ZoneId.of(facilityTimeZone);
      ZonedDateTime now = ZonedDateTime.now(zoneId);
      String timezoneOffset = now.getOffset().getId();
      var searchFilter = ISearchFilterMapper.toSearchCriteria(customView.getFilters());
      Sort sort = Sort.by(Sort.Direction.DESC, "id");
      Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, sort);
      List<JobLog> jobLogs = findAllJobLogs(objectMapper.writeValueAsString(searchFilter), null, pageable).getContent();

      FacilityDto facilityDto = getFacilityDetailsFromJaas(Long.valueOf(customView.getFacilityId()));

      Map<String, String> checklistNameAndCodeMapping = new HashMap<>();
      createDataSheet(wb, customView, jobLogs, checklistNameAndCodeMapping, timezoneOffset, facilityDto.getDateTimeFormat(), facilityDto.getDateFormat());
      createDetailsSheet(wb, customView.getFilters(), checklistNameAndCodeMapping, timezoneOffset);
      wb.write(outputStream);
      return new ByteArrayInputStream(outputStream.toByteArray());
    }
  }

  private FacilityDto getFacilityDetailsFromJaas(Long facilityId) {
    return facilityService.getFacility(facilityId);
  }


  public void createDataSheet(Workbook wb, CustomView customView, List<JobLog> jobLogs, Map<String, String> checklistNameAndCodeMapping,
                              String timezoneOffset, String dateTimeFormat, String dateFormat) {
    // Step 1: Create a new sheet in the workbook
    wb.createSheet(DATA_SHEET);

    // Step 2: Initialize some variables and styles
    CreationHelper createHelper = wb.getCreationHelper();
    Sheet dataSheet = wb.getSheet(DATA_SHEET);
    CellStyle headerStyle = wb.createCellStyle();
    XSSFFont font = getXSSFFont((XSSFWorkbook) wb, ARIAL_FONT_NAME, FONT_HEIGHT);
    headerStyle.setFont(font);

    // Step 3: Check if the list of jobLogs is empty, and if so, return early
    if (Utility.isEmpty(jobLogs)) {
      return;
    }

    // Step 4: Filter and sort the customViewColumns, excluding certain trigger types
    // On UI verification columns like  PARAMETER_PEER_VERIFIED_AT and PARAMETER_PEER_VERIFIED_BY are merged into 1 and
    // displayed as "Perfomed at {formatDateTime(peerVerifiedAt)}, by{' '}{value}. We are replicating the same here.
    List<CustomViewColumn> customViewColumns = customView.getColumns().stream()
      .filter(customViewColumn -> {
        Type.JobLogTriggerType triggerType = Type.JobLogTriggerType.valueOf(customViewColumn.getTriggerType());
        return (triggerType != Type.JobLogTriggerType.PARAMETER_PEER_VERIFIED_AT)
          && (triggerType != Type.JobLogTriggerType.PARAMETER_SELF_VERIFIED_AT)
          && (triggerType != Type.JobLogTriggerType.PARAMETER_PEER_STATUS);
      })
      .sorted(Comparator.comparing(CustomViewColumn::getOrderTree, Comparator.nullsLast(Integer::compareTo)))
      .toList();

    // Step 5: Set orderTree for each customViewColumn, preventing Excel cell value shifts
    for (int i = 0; i < customViewColumns.size(); i++) {
      customViewColumns.get(i).setOrderTree(i + 1);
    }

    // Step 6: Create a mapping of jobLogViewColumn using their unique identifier
    final Map<String, CustomViewColumn> jobLogViewColumnMap = customViewColumns.stream()
      .collect(Collectors.toMap(jobLogViewColumn -> jobLogViewColumn.getId() + jobLogViewColumn.getTriggerType(), Function.identity()));

    // Step 7: Create the header row in the dataSheet
    Row headerDetailsRow = dataSheet.createRow(0);
    final Set<String> uniqueLogs = new HashSet<>();
    for (int i = 0; i < customViewColumns.size(); i++) {
      Cell jobLogColumn = headerDetailsRow.createCell(i);
      var triggerType = Type.JobLogTriggerType.valueOf(customViewColumns.get(i).getTriggerType());
      jobLogColumn.setCellValue(customViewColumns.get(i).getDisplayName());
      jobLogColumn.setCellStyle(headerStyle);
      uniqueLogs.add(customViewColumns.get(i).getId() + triggerType);
    }

    // Step 8: Loop through each jobLog and process their data
    for (int i = 0; i < jobLogs.size(); i++) {
      JobLog jobLog = jobLogs.get(i);

      // Step 9: Create a mapping of verificationJobLogData using their unique identifier
      Map<String, JobLogData> verificationJobLogDataMap = jobLog.getLogs().stream()
        .filter(jobLogData -> Type.VERIFICATION_TRIGGER_TYPES.contains(jobLogData.getTriggerType()))
        .collect(Collectors.toMap(
          jobLogData -> jobLogData.getEntityId() + jobLogData.getTriggerType(),
          jobLogData -> jobLogData,
          (existing, replacement) -> existing
        ));

      // Step 10: Process and format verification jobLogData entries
      jobLog.getLogs().stream()
        .filter(jobLogData -> Type.VERIFICATION_TRIGGER_TYPES.contains(jobLogData.getTriggerType()))
        .forEach(jobLogData -> {
          if (jobLogData.getTriggerType() == Type.JobLogTriggerType.PARAMETER_PEER_VERIFIED_BY) {
            String peerVerifiedAtValue = verificationJobLogDataMap.get(jobLogData.getEntityId() + Type.JobLogTriggerType.PARAMETER_PEER_VERIFIED_AT).getValue();
            String formattedDateValue = DateTimeUtils.getFormattedDateTimeOfPattern(DateTimeUtils.addOffSetToTime(Long.parseLong(peerVerifiedAtValue), timezoneOffset), dateTimeFormat);
            String peerStatus = verificationJobLogDataMap.get(jobLogData.getEntityId() + Type.JobLogTriggerType.PARAMETER_PEER_STATUS).getValue();
            String peerStatusValue = peerStatus.equals("ACCEPTED") ? "Approved" : peerStatus.equals("REJECTED") ? "Rejected" : null;
            String formattedVerifiedByValue = String.format("%s at %s, by %s", peerStatusValue, formattedDateValue, jobLogData.getValue());
            jobLogData.setValue(formattedVerifiedByValue);
          } else if (jobLogData.getTriggerType() == Type.JobLogTriggerType.PARAMETER_SELF_VERIFIED_BY) {
            String selfVerifiedAtValue = verificationJobLogDataMap.get(jobLogData.getEntityId() + Type.JobLogTriggerType.PARAMETER_SELF_VERIFIED_AT).getValue();
            String formattedDateValue = DateTimeUtils.getFormattedDateTimeOfPattern(DateTimeUtils.addOffSetToTime(Long.parseLong(selfVerifiedAtValue), timezoneOffset), dateTimeFormat);
            String formattedVerifiedByValue = String.format("Performed at %s, by %s", formattedDateValue, jobLogData.getValue());
            jobLogData.setValue(formattedVerifiedByValue);
          }
        });

      // Step 11: Create a mapping of requiredJobLogData using their unique identifier
      Map<String, JobLogData> requiredJobLogData = jobLog.getLogs().stream()
        .filter(jobLogData -> uniqueLogs.contains(jobLogData.getEntityId() + jobLogData.getTriggerType()))
        .collect(Collectors.toMap(jobLogData -> jobLogData.getEntityId() + jobLogData.getTriggerType(), Function.identity()));

      // Step 12: Create a mapping of resourceParameterChoiceDto
      Map<String, ResourceParameterChoiceDto> resourceParameterChoiceDtoMap = jobLog.getLogs().stream()
        .filter(jobLogData -> jobLogData.getTriggerType() == Type.JobLogTriggerType.RESOURCE)
        .flatMap(jobLogData -> jobLogData.getResourceParameters().values().stream())
        .flatMap(jobLogResource -> jobLogResource.getChoices().stream())
        .collect(Collectors.toMap(
          ResourceParameterChoiceDto::getObjectId,
          resourceParameterChoiceDto -> resourceParameterChoiceDto,
          (existingChoice, newChoice) -> newChoice
        ));

      // Step 13: Add checklist information to the mapping
      checklistNameAndCodeMapping.put(jobLog.getChecklistCode(), jobLog.getChecklistName());

      // Step 14: Create a new row for the current jobLog and fill in the data
      Row logsRow = dataSheet.createRow(i + 1);
      for (String logKey : jobLogViewColumnMap.keySet()) {
        CustomViewColumn mappedCustomViewColumn = jobLogViewColumnMap.get(logKey);
        JobLogData mappedJobLogData = requiredJobLogData.get(logKey);
        Cell logsCell = logsRow.createCell(mappedCustomViewColumn.getOrderTree() - 1);

        if (mappedJobLogData != null) {
          // Step 15: Set the cell value based on the data
          setCellValue(logsCell, mappedCustomViewColumn.getType(), mappedJobLogData, createHelper, wb, resourceParameterChoiceDtoMap, timezoneOffset, dateTimeFormat, dateFormat);
        }
      }
    }
  }

  private static void setCellValue(Cell logsCell, String type, JobLogData mappedJobLogData, CreationHelper createHelper, Workbook wb, Map<String, ResourceParameterChoiceDto> resourceParameterChoiceDtoMap,
                                   String timezoneOffset, String dateTimeFormat, String dateFormat) {
    var jobLogColumnType = Type.JobLogColumnType.valueOf(type);
    switch (jobLogColumnType) {
      case TEXT -> {
        if (Objects.requireNonNull(mappedJobLogData.getTriggerType()) == Type.JobLogTriggerType.RESOURCE) {
          String resourceValue = mappedJobLogData.getResourceParameters().values().stream()
            .map(jobLogResource -> jobLogResource.getDisplayName() + ":" + jobLogResource.getChoices()
              .stream()
              .map(choice -> choice.getObjectDisplayName() + " (ID: " + choice.getObjectExternalId() + ")")
              .collect(Collectors.joining(","))
            ).collect(Collectors.joining());
          logsCell.setCellValue(resourceValue);
        } else if (Objects.requireNonNull(mappedJobLogData.getTriggerType()) == Type.JobLogTriggerType.RESOURCE_PARAMETER) {
          String[] identifierValueArr = mappedJobLogData.getIdentifierValue().split(",");
          String[] valueArr = mappedJobLogData.getValue().split(",");

          StringJoiner resourceJoiner = new StringJoiner(", ");
          for (int i = 0; i < identifierValueArr.length; i++) {
            // Adding this because one can unselect all the resources and the data is represented as '[]'
            if (!Utility.isEmpty(identifierValueArr[i])) {
              resourceJoiner.add(resourceParameterChoiceDtoMap.get(identifierValueArr[i]).getObjectDisplayName() + " (ID: " + valueArr[i] + (")"));
            }
          }
          String resourceValue = resourceJoiner.toString();
          logsCell.setCellValue(resourceValue);
        } else {
          logsCell.setCellValue(mappedJobLogData.getValue());
        }
      }
      case DATE -> {
        if (!Utility.isEmpty(mappedJobLogData.getValue())) {
          long dateValueWithOffSet = DateTimeUtils.addOffSetToTime(Long.parseLong(mappedJobLogData.getValue()), timezoneOffset);
          String formattedDate = DateTimeUtils.getFormattedDatePattern(dateValueWithOffSet, dateFormat);
          logsCell.setCellValue(formattedDate);
        }
      }
      case DATE_TIME -> {
        if (!Utility.isEmpty(mappedJobLogData.getValue())) {
          long dateValueWithOffSet = DateTimeUtils.addOffSetToTime(Long.parseLong(mappedJobLogData.getValue()), timezoneOffset);
          String formattedDate = DateTimeUtils.getFormattedDateTimeOfPattern(dateValueWithOffSet, dateTimeFormat);
          logsCell.setCellValue(formattedDate);
        }
      }
      case FILE -> {
        if (!Utility.isEmpty(mappedJobLogData.getMedias())) {
          var mediaDataList = mappedJobLogData.getMedias();
          StringJoiner mediaNamesJoiner = new StringJoiner(", ");
          for (var mediaData : mediaDataList) {
            mediaNamesJoiner.add(mediaData.getName() + "(" + mediaData.getLink() + ")");
          }
          logsCell.setCellValue(mediaNamesJoiner.toString());
        }
      }
    }
  }

  public void createDetailsSheet(Workbook wb, List<CustomViewFilter> filters, Map<String, String> checklistNameAndCodeMapping, String timezoneOffset) throws IOException {
    wb.createSheet(DETAILS_SHEET);
    Sheet detailsSheet = wb.getSheet(DETAILS_SHEET);

    CellStyle headerStyle = wb.createCellStyle();
    XSSFFont font = getXSSFFont((XSSFWorkbook) wb, ARIAL_FONT_NAME, FONT_HEIGHT);
    headerStyle.setFont(font);

    Row processDetailsRow = detailsSheet.createRow(0);
    Cell processCellHeader = processDetailsRow.createCell(0);
    processCellHeader.setCellValue(PROCESS_CELL);
    processCellHeader.setCellStyle(headerStyle);

    int cellCounter = 1;
    for (Map.Entry<String, String> entry : checklistNameAndCodeMapping.entrySet()) {
      Cell processCellValue = processDetailsRow.createCell(cellCounter);
      var processValue = String.format("%s (ID: %s), ", entry.getValue(), entry.getKey());
      processCellValue.setCellValue(processValue);
      cellCounter++;
    }
    Row filterHeaderRow = detailsSheet.createRow(1);
    Cell filterHeaderCell = filterHeaderRow.createCell(0);
    filterHeaderCell.setCellValue(FILTERS_CELL);
    filterHeaderCell.setCellStyle(headerStyle);


    if (!Utility.isEmpty(filters)) {
      int filterIndex = 0;
      for (int i = 0; i < filters.size(); i++) {
        if(filters.get(i).getKey().equals("checklistId")) {
          continue;
        }
        
        Row filterIndexRow = detailsSheet.createRow(checklistNameAndCodeMapping.size() + filterIndex + 2);
        Cell filterIndexCell = filterIndexRow.createCell(0);
        filterIndexCell.setCellValue("Filter " + (filterIndex + 1));
        filterIndex++;
        filterIndexCell.setCellStyle(headerStyle);
        CustomViewFilter filter = filters.get(i);
        String filterDisplayName = filter.getKey(), comparisonOperator = getComparisonOperatorValue(filter.getConstraint());
        List<String> filterValues = filter.getValue().stream()
          .map(Object::toString)
          .collect(Collectors.toList());
        String filterKeyValue = filter.getKey();
        if (!filterKeyValue.contains("parameterValues")) {
          var filterKey = Arrays.stream(values()).filter(f -> f.getValue().contains(filter.getKey())).findFirst().get();
          filterValues = switch (filterKey) {
            case ENDED_AT, CREATED_AT, MODIFIED_AT, STARTED_AT ->
              filterValues = filterValues.stream().map(value -> DateTimeUtils.getFormattedDateTime(DateTimeUtils.addOffSetToTime(Long.parseLong(value), timezoneOffset))).collect(Collectors.toList());
            case JOB_STATE ->
              filterValues = filterValues.stream().map(value -> State.Job.valueOf(value).getDisplayName()).collect(Collectors.toList());
            default -> filterValues;
          };
        }else {
          String parameterId = filterKeyValue.substring(filterKeyValue.indexOf("parameterValues.") + "parameterValues.".length());
          Parameter parameter = parameterRepository.findById(Long.parseLong(parameterId)).get();
          filterDisplayName = parameter.getLabel();
          getFilterValues (parameter, filterValues, timezoneOffset);
        }
        var filterFormat = String.format("Where %s %s %s", filterDisplayName, comparisonOperator, filterValues);
        Cell filterValueCell = filterIndexRow.createCell(1);
        filterValueCell.setCellValue(filterFormat);
      }
    }
  }

  private void getFilterValues(Parameter parameter, List<String> filterValues, String timezoneOffset) throws IOException {
    switch (parameter.getType()) {
      case YES_NO -> {
        List<YesNoParameter> choices = JsonUtils.jsonToCollectionType(parameter.getData().toString(), List.class, YesNoParameter.class);
        Map<Object, String> optionsNameMap = choices.stream()
          .collect(Collectors.toMap(YesNoParameter::getId, YesNoParameter::getName));
        replaceFilterValues(filterValues, optionsNameMap);
      }

      case SINGLE_SELECT, MULTISELECT -> {
        List<MultiSelectParameter> choices = JsonUtils.jsonToCollectionType(parameter.getData().toString(), List.class, MultiSelectParameter.class);
        Map<Object, String> optionsNameMap = choices.stream()
          .collect(Collectors.toMap(MultiSelectParameter::getId, MultiSelectParameter::getName));
        replaceFilterValues(filterValues, optionsNameMap);
      }

      case RESOURCE, MULTI_RESOURCE -> {
        ResourceParameter choices = JsonUtils.readValue(parameter.getData().toString(), ResourceParameter.class);
        List<PartialEntityObject> partialEntities = entityObjectRepository.findPartialByIds(choices.getCollection(), filterValues);
        Map<Object, String> optionsNameMap = partialEntities.stream()
          .collect(Collectors.toMap(PartialEntityObject::getId, PartialEntityObject::getDisplayName));
        replaceFilterValues(filterValues, optionsNameMap);
      }

      case DATE, DATE_TIME -> {
        filterValues.replaceAll(value -> {
          try {
            return DateTimeUtils.getFormattedDateTime(DateTimeUtils.addOffSetToTime(Long.parseLong(value), timezoneOffset));
          } catch (NumberFormatException e) {
            return "Invalid Date";
          }
        });
      }
      default -> {
      }
    }
  }

  private void replaceFilterValues(List<String> filterValues, Map<Object, String> optionsNameMap) {
    filterValues.replaceAll(id -> optionsNameMap.entrySet().stream()
      .filter(entry -> entry.getKey().toString().equals(id))
      .map(Map.Entry::getValue)
      .findAny()
      .orElse("Unknown Choice"));
  }

  private String getComparisonOperatorValue(String operator) {
    return SearchOperator.valueOf(operator).getOperator();
  }

  @Override
  public byte[] createJobLogPdf(String customViewId, String filters, Type.JobLogType jobLogType, String pdfMetaData) throws IOException, ResourceNotFoundException {
    log.info("[createJobLogPdf] Request to create job log PDF, jobLogType: {}, customViewId: {}", jobLogType, customViewId);

    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());
    
    // Create GeneratedPdfDataDto with common fields
    GeneratedPdfDataDto pdfData = new GeneratedPdfDataDto();
    pdfData.setGeneratedOn(DateTimeUtils.now());
    pdfData.setUserFullName(principalUser.getFirstName() + " " + principalUser.getLastName());
    pdfData.setUserId(principalUser.getEmployeeId());


    if (jobLogType == Type.JobLogType.ASSETS_LOGS) {
      return handleObjectJobLogs(pdfData, customViewId, filters, pdfMetaData);
    } else if (jobLogType == Type.JobLogType.PROCESS_LOGS) {
      return handleProcessJobLogs(pdfData, customViewId, filters);
    }

    throw new IllegalArgumentException("Unsupported job log type: " + jobLogType);
  }


  private byte[] handleObjectJobLogs(GeneratedPdfDataDto pdfData, String customViewId, String filters, String pdfMetaData) throws IOException, ResourceNotFoundException {
    log.info("[handleObjectJobLogs] Generating object job logs PDF, customViewId: {}, pdfMetaData: {}", customViewId, pdfMetaData);
    
    // Set the job log type for generic PDF generation
    pdfData.setJobLogType(Type.JobLogType.ASSETS_LOGS);
    
    // Parse pdfMetaData if provided
    if (pdfMetaData != null && !pdfMetaData.trim().isEmpty()) {
      try {
        JsonNode metaDataNode = objectMapper.readTree(pdfMetaData);
        
        // Extract object metadata from pdfMetaData
        String objectTypeDisplayName = metaDataNode.has("objectTypeDisplayName") ? metaDataNode.get("objectTypeDisplayName").asText() : null;
        String objectDisplayName = metaDataNode.has("objectDisplayName") ? metaDataNode.get("objectDisplayName").asText() : null;
        String objectExternalId = metaDataNode.has("objectExternalId") ? metaDataNode.get("objectExternalId").asText() : null;
        
        // Set object metadata directly from pdfMetaData
        pdfData.setObjectTypeDisplayName(objectTypeDisplayName);
        pdfData.setObjectDisplayName(objectDisplayName);
        pdfData.setObjectExternalId(objectExternalId);
        
        log.info("[handleObjectJobLogs] Using metadata from pdfMetaData: objectType={}, objectName={}, objectId={}", 
                 objectTypeDisplayName, objectDisplayName, objectExternalId);
      } catch (Exception e) {
        log.warn("[handleObjectJobLogs] Failed to parse pdfMetaData, proceeding without object metadata: {}", e.getMessage());
      }
    }
    
    // Get job logs using existing findAllJobLogs with filters
    Sort sort = Sort.by(Sort.Direction.DESC, "id");
    Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, sort);
    List<JobLog> jobLogs = findAllJobLogs(filters != null ? filters : "{}", null, pageable).getContent();
    
    // Get columns based on customViewId
    List<CustomViewColumn> customViewColumns;
    String customViewName;
    
    if ("0".equals(customViewId)) {
      // Default columns for object job logs
      customViewColumns = getDefaultObjectJobLogColumns();
      customViewName = "Job Logs";
      
      // Extract facility ID from filters and set facility info for default view
      String facilityId = extractFacilityIdFromFilters(filters);
      if (facilityId != null) {
        try {
          Facility facility = facilityRepository.getReferenceById(Long.valueOf(facilityId));
          FacilityDto facilityDto = getFacilityDetailsFromJaas(Long.valueOf(facilityId));
          pdfData.setFacility(facilityDto);
          setTimezoneAndDateFormats(pdfData, facility);
          log.info("[handleObjectJobLogs] Set facility info from filters for default view, facilityId: {}", facilityId);
        } catch (Exception e) {
          log.warn("[handleObjectJobLogs] Failed to set facility info from filters: {}", e.getMessage());
        }
      }
    } else {
      // Custom view columns
      ObjectTypeCustomView objectTypeCustomView = objectTypeCustomViewRepository.findById(customViewId)
          .orElseThrow(() -> new ResourceNotFoundException(customViewId, ErrorCode.CUSTOM_VIEW_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
      
      customViewColumns = objectTypeCustomView.getColumns().stream()
          .sorted(Comparator.comparing(CustomViewColumn::getOrderTree, Comparator.nullsLast(Integer::compareTo))).toList();
      customViewName = objectTypeCustomView.getLabel();
      
      // Get facility info from custom view
      Facility facility = facilityRepository.getReferenceById(Long.valueOf(objectTypeCustomView.getFacilityId()));
      FacilityDto facilityDto = getFacilityDetailsFromJaas(Long.valueOf(objectTypeCustomView.getFacilityId()));
      pdfData.setFacility(facilityDto);
      setTimezoneAndDateFormats(pdfData, facility);
    }
    
    // Set data for PDF generation
    pdfData.setJobLogs(jobLogs);
    pdfData.setColumnsList(customViewColumns);
    pdfData.setFilters(filters != null ? filters : "{}");
    pdfData.setCustomViewName(customViewName);
    
    log.info("[handleObjectJobLogs] Generating PDF for object job logs, customViewId: {}, jobLogsCount: {}", 
             customViewId, jobLogs != null ? jobLogs.size() : 0);

    return pdfGeneratorUtil.generatePdf(Type.PdfType.JOB_LOGS, pdfData);
  }

  private byte[] handleProcessJobLogs(GeneratedPdfDataDto pdfData, String customViewId, String filters) throws IOException, ResourceNotFoundException {
    log.info("[handleProcessJobLogs] Generating process job logs PDF for customViewId: {}", customViewId);
    
    // Set the job log type for generic PDF generation
    pdfData.setJobLogType(Type.JobLogType.PROCESS_LOGS);
    
    // Get custom view
    CustomView customView = customViewService.getCustomViewById(customViewId);
    
    // Get facility and timezone information
    Facility facility = facilityRepository.getReferenceById(Long.valueOf(customView.getFacilityId()));
    FacilityDto facilityDto = getFacilityDetailsFromJaas(Long.valueOf(customView.getFacilityId()));
    pdfData.setFacility(facilityDto);
    
    // Set timezone and date formats
    setTimezoneAndDateFormats(pdfData, facility);
    
    // Set filters
    pdfData.setFilters(filters);
    
    // Get job logsi will get 
    var searchFilter = ISearchFilterMapper.toSearchCriteria(customView.getFilters());
    Sort sort = Sort.by(Sort.Direction.DESC, "id");
    Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, sort);
    List<JobLog> jobLogs = findAllJobLogs(objectMapper.writeValueAsString(searchFilter), null, pageable).getContent();
    
    // Get columns
    List<CustomViewColumn> customViewColumns = customView.getColumns().stream()
        .sorted(Comparator.comparing(CustomViewColumn::getOrderTree, Comparator.nullsLast(Integer::compareTo))).toList();

    // Get checklist info
    Checklist checklistInfo = checklistRepository.findById(Long.valueOf(customView.getProcessId()))
        .orElseThrow(() -> new ResourceNotFoundException(customView.getProcessId(), ErrorCode.PROCESS_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    
    User principalUserEntity = userRepository.getReferenceById(
        ((PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId());
    
    List<ChecklistPropertyValue> checklistPropertiesValue = new ArrayList<>(checklistInfo.getChecklistPropertyValues());
    for (ChecklistPropertyValue checklistPropertyValue : checklistPropertiesValue) {
        Property property = checklistPropertyValue.getFacilityUseCasePropertyMapping().getProperty();
        if (!property.isArchived()) {
            checklistInfo.addProperty(checklistPropertyValue.getFacilityUseCasePropertyMapping(), checklistPropertyValue.getValue(), principalUserEntity);
        }
    }

    // Set data for PDF generation
    pdfData.setJobLogs(jobLogs);
    pdfData.setColumnsList(customViewColumns);
    pdfData.setChecklist(checklistInfo);
    pdfData.setCustomViewName(customView.getLabel());
    pdfData.setProcessName(checklistInfo.getName());
    
    // Add the custom view to the list
    if (pdfData.getCustomViews() == null) {
        pdfData.setCustomViews(new ArrayList<>());
    }
    pdfData.getCustomViews().add(customView);

    log.info("[handleProcessJobLogs] Generating PDF for process job logs, customViewId: {}, jobLogsCount: {}", 
             customViewId, jobLogs != null ? jobLogs.size() : 0);

    return pdfGeneratorUtil.generatePdf(Type.PdfType.JOB_LOGS, pdfData);
  }

  private void setTimezoneAndDateFormats(GeneratedPdfDataDto pdfData, Facility facility) {
    String facilityTimeZone = facility.getTimeZone();
    ZoneId zoneId = ZoneId.of(facilityTimeZone);
    ZonedDateTime now = ZonedDateTime.now(zoneId);
    String timezoneOffset = now.getOffset().getId();
    pdfData.setTimezoneOffset(timezoneOffset);
    pdfData.setDateFormat(pdfData.getFacility().getDateFormat());
    pdfData.setDateTimeFormat(pdfData.getFacility().getDateTimeFormat());
  }

  private String extractFacilityIdFromFilters(String filters) {
    if (filters == null || filters.trim().isEmpty() || "{}".equals(filters.trim())) {
      return null;
    }
    
    try {
      // Parse the filters JSON using existing SearchFilter structure
      SearchFilter searchFilter = objectMapper.readValue(filters, SearchFilter.class);
      
      if (searchFilter.getFields() != null) {
        for (SearchCriteria criteria : searchFilter.getFields()) {
          if ("facilityId".equals(criteria.getField()) && criteria.getValue() != null) {
            return criteria.getValue().toString();
          }
        }
      }
      
      // Fallback: try to parse as direct JSON and look for facilityId
      JsonNode filtersNode = objectMapper.readTree(filters);
      if (filtersNode.has("facilityId")) {
        return filtersNode.get("facilityId").asText();
      }
      
      // Check if it's in a nested structure
      if (filtersNode.has("fields")) {
        JsonNode fieldsNode = filtersNode.get("fields");
        if (fieldsNode.isArray()) {
          for (JsonNode field : fieldsNode) {
            if (field.has("field") && "facilityId".equals(field.get("field").asText()) && field.has("values")) {
              JsonNode valuesNode = field.get("values");
              if (valuesNode.isArray() && valuesNode.size() > 0) {
                return valuesNode.get(0).asText();
              }
            }
          }
        }
      }
      
    } catch (Exception e) {
      log.warn("[extractFacilityIdFromFilters] Failed to parse filters for facilityId: {}", e.getMessage());
    }
    
    return null;
  }


  private List<CustomViewColumn> getDefaultObjectJobLogColumns() {
    List<CustomViewColumn> columns = new ArrayList<>();
    int orderTree = 1;
    
    // Add default job columns (matching frontend's objectJobLogColumns)
    columns.add(createCustomViewColumn("-1", Type.JobLogTriggerType.JOB_ID.name(), "TEXT", "Job Id", orderTree++));
    columns.add(createCustomViewColumn("-1", Type.JobLogTriggerType.JOB_STATE.name(), "TEXT", "Job State", orderTree++));
    columns.add(createCustomViewColumn("-1", Type.JobLogTriggerType.JOB_START_TIME.name(), "DATE_TIME", "Job Started At", orderTree++));
    columns.add(createCustomViewColumn("-1", Type.JobLogTriggerType.JOB_STARTED_BY.name(), "TEXT", "Job Started By", orderTree++));
    columns.add(createCustomViewColumn("-1", Type.JobLogTriggerType.JOB_CREATED_AT.name(), "DATE_TIME", "Job Created At", orderTree++));
    columns.add(createCustomViewColumn("-1", Type.JobLogTriggerType.JOB_CREATED_BY.name(), "TEXT", "Job Created By", orderTree++));
    columns.add(createCustomViewColumn("-1", Type.JobLogTriggerType.CHK_ID.name(), "TEXT", "Process Id", orderTree++));
    columns.add(createCustomViewColumn("-1", Type.JobLogTriggerType.CHK_NAME.name(), "TEXT", "Process Name", orderTree++));
    columns.add(createCustomViewColumn("-1", Type.JobLogTriggerType.ANNOTATION_REMARK.name(), "TEXT", "Annotation Remark", orderTree++));
    columns.add(createCustomViewColumn("-1", Type.JobLogTriggerType.ANNOTATION_MEDIA.name(), "FILE", "Annotation Media", orderTree++));
    columns.add(createCustomViewColumn("-1", Type.JobLogTriggerType.JOB_END_TIME.name(), "DATE_TIME", "Job Ended At", orderTree++));
    columns.add(createCustomViewColumn("-1", Type.JobLogTriggerType.JOB_ENDED_BY.name(), "TEXT", "Job Ended By", orderTree++));
    
    return columns;
  }

  private CustomViewColumn createCustomViewColumn(String id, String triggerType, String type, String displayName, int orderTree) {
    CustomViewColumn column = new CustomViewColumn();
    column.setId(id);
    column.setTriggerType(triggerType);
    column.setType(type);
    column.setDisplayName(displayName);
    column.setOrderTree(orderTree);
    return column;
  }


}
