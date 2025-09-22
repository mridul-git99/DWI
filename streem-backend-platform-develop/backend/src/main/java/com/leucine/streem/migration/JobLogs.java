package com.leucine.streem.migration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leucine.streem.collections.JobLog;
import com.leucine.streem.collections.JobLogData;
import com.leucine.streem.collections.JobLogMediaData;
import com.leucine.streem.collections.JobLogResource;
import com.leucine.streem.config.MediaConfig;
import com.leucine.streem.constant.State;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.ResourceParameterChoiceDto;
import com.leucine.streem.dto.mapper.IUserMapper;
import com.leucine.streem.dto.projection.JobLogMigrationChecklistView;
import com.leucine.streem.dto.projection.JobLogMigrationParameterValueMediaMapping;
import com.leucine.streem.dto.projection.JobLogTaskExecutionView;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.model.helper.parameter.CalculationParameter;
import com.leucine.streem.model.helper.parameter.ChoiceParameterBase;
import com.leucine.streem.model.helper.parameter.ResourceParameter;
import com.leucine.streem.repository.*;
import com.leucine.streem.service.IChecklistService;
import com.leucine.streem.util.JsonUtils;
import com.leucine.streem.util.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.leucine.streem.constant.CollectionName.JOB_LOGS;

@Component
@RequiredArgsConstructor
@Slf4j
/*
 * This migration updates:
 * - Reconfigured job_log_columns
 * - Re-generated the jobLogs collection
 * - Re-configures the customViews
 */
public class JobLogs {
  private final IJobRepository jobRepository;
  private final IParameterValueRepository parameterValueRepository;
  private final IJobAnnotationRepository jobAnnotationRepository;
  private final IParameterRepository parameterRepository;
  private final IChecklistService checklistService;
  private final ObjectMapper objectMapper;
  private final MediaConfig mediaConfig;
  private final ITaskExecutionRepository taskExecutionRepository;
  private final MongoTemplate mongoTemplate;
  private final IUserMapper userMapper;
  private final IChecklistRepository checklistRepository;
  private final IParameterValueMediaRepository parameterValueMediaRepository;
  private final IParameterVerificationRepository parameterVerificationRepository;


  private void updateJobLogs(List<Job> jobList, Long checklistId) throws IOException {
    Set<JobLog> jobLogs = new HashSet<>();
    Set<Long> jobIds = jobList.stream().map(Job::getId).collect(Collectors.toSet());
    Map<Long, Parameter> parameterMap = parameterRepository.findByChecklistIdAndArchived(checklistId, false).stream().collect(Collectors.toMap(Parameter::getId, parameter -> parameter));

    Map<Long, List<JobLogTaskExecutionView>> taskExecutionMap = taskExecutionRepository.findTaskExecutionDetailsByJobId(jobIds).stream().collect(Collectors.groupingBy(JobLogTaskExecutionView::getJobId));


    JobLogMigrationChecklistView jobLogMigrationChecklistView = checklistRepository.findChecklistInfoById(checklistId);
    for (Job job : jobList) {
      log.info("[updateJobLogs] creating job log record for job id: {}", job.getId());
      var jobLogRecord = new JobLog();
      List<JobLogData> logs = new ArrayList<>();
      jobLogRecord.setLogs(logs);
      jobLogRecord.setId(job.getIdAsString());
      jobLogRecord.setFacilityId(job.getFacilityId().toString());
      jobLogRecord.setChecklistId(jobLogMigrationChecklistView.getId().toString());
      jobLogRecord.setChecklistName(jobLogMigrationChecklistView.getName());
      jobLogRecord.setChecklistCode(jobLogMigrationChecklistView.getCode());
      jobLogRecord.setCode(job.getCode());
      jobLogRecord.setState(job.getState());
      jobLogRecord.setStartedAt(job.getStartedAt() == null ? null : job.getStartedAt());
      jobLogRecord.setEndedAt(job.getEndedAt() == null ? null : job.getEndedAt());
      jobLogRecord.setCreatedBy(userMapper.toUserAuditDto(job.getCreatedBy()));
      jobLogRecord.setStartedBy(job.getStartedBy() == null ? null : userMapper.toUserAuditDto(job.getStartedBy()));
      jobLogRecord.setEndedBy(job.getEndedBy() == null ? null : userMapper.toUserAuditDto(job.getEndedBy()));
      jobLogRecord.setCreatedAt(job.getCreatedAt());
      jobLogRecord.setModifiedAt(job.getModifiedAt());

      var jobIdLog = new JobLogData(JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_ID, "Job Id", job.getCode(), job.getCode(), null, null);
      var jobStateLog = new JobLogData(JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_STATE, "Job State", job.getState().name(), job.getState().name(), null, null);

      var chkNameLog = new JobLogData(JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.CHK_NAME, "Checklist Name", jobLogMigrationChecklistView.getName(), jobLogMigrationChecklistView.getName(), null, null);
      var chkIdLog = new JobLogData(JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.CHK_ID, "Checklist Id", jobLogMigrationChecklistView.getCode(), jobLogMigrationChecklistView.getCode(), null, null);
      User jobStartedByUser = job.getStartedBy();
      PrincipalUser jobStartedByPrincipalUser = userMapper.toPrincipalUser(jobStartedByUser);
      String fullName;
      if (jobStartedByUser != null) {
        fullName = Utility.getFullNameAndEmployeeIdFromPrincipalUser(jobStartedByPrincipalUser);
        var jobStartedByLog = new JobLogData(JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_STARTED_BY, "Job Started By", fullName, jobStartedByPrincipalUser.getIdAsString(), null, null);
        logs.add(jobStartedByLog);
      }
      if (job.getStartedAt() != null) {
        var jobStartedTimeLog = new JobLogData(JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_START_TIME, "Job Started At", job.getStartedAt().toString(), job.getStartedAt().toString(), null, null);
        logs.add(jobStartedTimeLog);
      }

      var jobCreatedAtTimeLog = new JobLogData(JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_CREATED_AT, "Job Created At", job.getCreatedAt().toString(), job.getCreatedAt().toString(), null, null);
      logs.add(jobCreatedAtTimeLog);
      fullName = Utility.getFullNameAndEmployeeIdFromPrincipalUser(userMapper.toPrincipalUser(job.getCreatedBy()));
      var jobStartedByLog = new JobLogData(JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_CREATED_BY, "Job Created By", fullName, job.getCreatedBy().getIdAsString(), null, null);
      logs.add(jobStartedByLog);

      fullName = Utility.getFullNameAndEmployeeIdFromPrincipalUser(userMapper.toPrincipalUser(job.getModifiedBy()));
      var jobModifiedByLog = new JobLogData(JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_MODIFIED_BY, "Job Modified By", fullName, job.getModifiedBy().getIdAsString(), null, null);
      logs.add(jobModifiedByLog);

      var jobModifiedAtLog = new JobLogData(JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_MODIFIED_AT, "Job Modified At", job.getModifiedAt().toString(), job.getModifiedAt().toString(), null, null);
      logs.add(jobModifiedAtLog);

      if (job.getEndedAt() != null && job.getEndedBy() != null) {
        var jobEndedTimeLog = new JobLogData(JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_END_TIME, "Job Ended At", job.getEndedAt().toString(), job.getEndedAt().toString(), null, null);
        logs.add(jobEndedTimeLog);
        var jobEndedByUserFullName = Utility.getFullNameAndEmployeeIdFromPrincipalUser(userMapper.toPrincipalUser(job.getEndedBy()));
        var jobEndedByLog = new JobLogData(JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.JOB_ENDED_BY, "Job Ended By", jobEndedByUserFullName, job.getEndedBy().getIdAsString(), null, null);
        logs.add(jobEndedByLog);
      }
      logs.add(jobIdLog);
      logs.add(jobStateLog);
      logs.add(chkNameLog);
      logs.add(chkIdLog);

      List<ParameterValue> parameterValueList = parameterValueRepository.findAllByJobId(job.getId());
      // TODO Fix job logs

      Map<String, Object> parameterValues = new HashMap<>();
      for (var parameterValue : parameterValueList) {
        var parameter = parameterMap.get(parameterValue.getParameterId());
        var triggerType = parameter.getTargetEntityType().equals(Type.ParameterTargetEntityType.PROCESS) ? Type.JobLogTriggerType.PROCESS_PARAMETER_VALUE : Type.JobLogTriggerType.PARAMETER_VALUE;
        switch (parameter.getType()) {
          case MULTI_LINE, SINGLE_LINE, SHOULD_BE -> {
            String value;
            if (parameterValue.getValue() != null) {
              value = parameterValue.getValue();
              logs.add(new JobLogData(parameter.getId().toString(), triggerType, parameter.getLabel(), value, value, null, null));
              parameterValues.put(parameter.getIdAsString(), parameterValue.getValue());
            }
          }

          case DATE, DATE_TIME -> {
            long value;
            if (!Utility.isEmpty(parameterValue.getValue())) {
              value = Long.parseLong(parameterValue.getValue());
              logs.add(new JobLogData(parameter.getId().toString(), triggerType, parameter.getLabel(), String.valueOf(value), String.valueOf(value), null, null));
              parameterValues.put(parameter.getIdAsString(), value);
            }
          }
          case NUMBER -> {
            double value;
            if (!Utility.isEmpty(parameterValue.getValue())) {
              value = Double.parseDouble(parameterValue.getValue());
              logs.add(new JobLogData(parameter.getId().toString(), triggerType, parameter.getLabel(), Double.toString(value), String.valueOf(value), null, null));
              parameterValues.put(parameter.getIdAsString(), value);
            }
          }

          case CALCULATION -> {
            CalculationParameter calculationParameter = JsonUtils.readValue(parameter.getData().toString(), CalculationParameter.class);
            String valueWithUOM = "";
            if (!Utility.isEmpty(parameterValue.getValue())) {
              String uom = calculationParameter.getUom() != null ? calculationParameter.getUom() : "";
              valueWithUOM = parameterValue.getValue() + Utility.SPACE + uom;
              parameterValues.put(parameter.getIdAsString(), Double.valueOf(parameterValue.getValue()));
            }
            logs.add(new JobLogData(parameter.getIdAsString(), triggerType, parameter.getLabel(), valueWithUOM, valueWithUOM, null, null));
          }

          case CHECKLIST, MULTISELECT, SINGLE_SELECT, YES_NO -> {
            List<ChoiceParameterBase> activities = JsonUtils.jsonToCollectionType(parameter.getData().toString(), List.class, ChoiceParameterBase.class);
            Map<String, String> optionsNameMap = activities.stream().collect(
              Collectors.toMap(ChoiceParameterBase::getId, ChoiceParameterBase::getName));
            JsonNode oldChoices = parameterValue.getChoices();
            if (oldChoices != null) {
              List<String> selectedItems = new ArrayList<>();
              List<String> selectedIdentifierItems = new ArrayList<>();
              Map<String, String> result = objectMapper.convertValue(oldChoices, new TypeReference<>() {
              });
              for (ChoiceParameterBase choiceParameter : activities) {
                String state = result.get(choiceParameter.getId());
                if (State.Selection.SELECTED.name().equals(state)) {
                  selectedItems.add(optionsNameMap.get(choiceParameter.getId()));
                  selectedIdentifierItems.add(choiceParameter.getId());
                }
              }
              String value = String.join(",", selectedItems);
              String identifierValue = String.join(",", selectedIdentifierItems);
              parameterValues.put(parameter.getIdAsString(), identifierValue);
              logs.add(new JobLogData(parameter.getIdAsString(), triggerType, parameter.getLabel(), value, identifierValue, null, null));
            }
          }
          case MEDIA, SIGNATURE, FILE_UPLOAD -> {

            List<JobLogMigrationParameterValueMediaMapping> parameterValueMediaMappings = parameterValueMediaRepository.findMediaByParameterValueId(parameterValue.getId());
            List<JobLogMediaData> jobLogMedias = new ArrayList<>();
            for (JobLogMigrationParameterValueMediaMapping parameterValueMedia : parameterValueMediaMappings) {
              JobLogMediaData jobLogMediaData = new JobLogMediaData();

              jobLogMediaData.setName(Type.Parameter.SIGNATURE.equals(parameter.getType()) ? "Signature" : parameterValueMedia.getName());
              jobLogMediaData.setType(parameterValueMedia.getType());
              jobLogMediaData.setDescription(parameterValueMedia.getDescription());
              String link = mediaConfig.getCdn() + java.io.File.separator + parameterValueMedia.getRelativePath() + java.io.File.separator + parameterValueMedia.getFilename();
              jobLogMediaData.setLink(link);
              jobLogMedias.add(jobLogMediaData);
            }
            parameterValues.put(parameter.getIdAsString(), null);
            logs.add(new JobLogData(parameter.getIdAsString(), triggerType, parameter.getLabel(), null, null, jobLogMedias, null));
          }
          case RESOURCE, MULTI_RESOURCE -> {
            ResourceParameter resourceParameter = JsonUtils.readValue(parameter.getData().toString(), ResourceParameter.class);
            List<ResourceParameterChoiceDto> choices = JsonUtils.jsonToCollectionType(parameterValue.getChoices(), List.class, ResourceParameterChoiceDto.class);
            List<String> selectedItems = new ArrayList<>();
            List<String> selectedIdentifierItems = new ArrayList<>();
            if (choices != null) {
              for (ResourceParameterChoiceDto choice : choices) {
                selectedIdentifierItems.add(choice.getObjectId());
                selectedItems.add(choice.getObjectExternalId());
              }
              String value = String.join(",", selectedItems);
              String identifierValue = String.join(",", selectedIdentifierItems);
              parameterValues.put(parameter.getIdAsString(), identifierValue);
              logs.add(new JobLogData(parameter.getIdAsString(), Type.JobLogTriggerType.RESOURCE_PARAMETER, parameter.getLabel(), value, identifierValue, null, null));

              // JOB LOG CUSTOM VIEW - ENTITY TYPE - RESOURCE
              var jobLogDataToCreateOrUpdate = new JobLogData();
              boolean jobLogRecordFound = false;
              if (!Utility.isEmpty(jobLogRecord) && !Utility.isEmpty(jobLogRecord.getLogs())) {
                for (JobLogData jobLogData : jobLogRecord.getLogs()) {
                  if (jobLogData.getEntityId().equals(resourceParameter.getObjectTypeId().toString()) && jobLogData.getTriggerType().equals(Type.JobLogTriggerType.RESOURCE)) {
                    jobLogRecordFound = true;
                    jobLogDataToCreateOrUpdate = jobLogData;
                    jobLogData.setMedias(null);
                    jobLogData.setValue(null);
                    jobLogData.setIdentifierValue(null);
                  }
                }
              }

              jobLogDataToCreateOrUpdate.setEntityId(resourceParameter.getObjectTypeId().toString());
              jobLogDataToCreateOrUpdate.setTriggerType(Type.JobLogTriggerType.RESOURCE);
              jobLogDataToCreateOrUpdate.setDisplayName(resourceParameter.getObjectTypeDisplayName());

              var iterator = jobLogDataToCreateOrUpdate.getResourceParameters().entrySet().iterator();
              List<String> objectIds = new ArrayList<>();
              while (iterator.hasNext()) {
                Map.Entry<String, JobLogResource> mapElement = iterator.next();
                var jobLogResource = mapElement.getValue();
                for (ResourceParameterChoiceDto resourceParameterChoiceDto : jobLogResource.getChoices()) {
                  objectIds.add(resourceParameterChoiceDto.getObjectId());
                }
              }
              parameterValues.put(resourceParameter.getObjectTypeId().toString(), objectIds);

              JobLogResource jobLogResource = new JobLogResource();
              jobLogResource.setDisplayName(parameter.getLabel());
              jobLogResource.setChoices(choices);
              jobLogDataToCreateOrUpdate.getResourceParameters().put(parameter.getIdAsString(), jobLogResource);

              if (!jobLogRecordFound) {
                jobLogRecord.getLogs().add(jobLogDataToCreateOrUpdate);
              }
            }
          }
        }
        if (parameter.getVerificationType() != Type.VerificationType.NONE &&
          (parameterValue.getState() == State.ParameterExecution.EXECUTED || parameterValue.getTaskExecution().isCorrectionEnabled())) {
          JobLogData selfVerificationLogDoneAt = new JobLogData();
          JobLogData selfVerificationLogDoneBy = new JobLogData();
          JobLogData peerVerificationLogDoneAt = new JobLogData();
          JobLogData peerVerificationLogDoneBy = new JobLogData();
          JobLogData peerVerificationLogStatus = new JobLogData();
          List<ParameterVerification> parameterVerificationList = parameterVerificationRepository.findLatestSelfAndPeerVerificationOfParameterValueId(parameterValue.getId());
          ParameterVerification selfAcceptedParameterVerification = null;
          ParameterVerification peerAcceptedParameterVerification = null;

          for (ParameterVerification parameterVerification: parameterVerificationList) {
            if (parameterVerification.getVerificationStatus() == State.ParameterVerification.ACCEPTED) {
              if (parameterVerification.getVerificationType() == Type.VerificationType.SELF) {
                selfAcceptedParameterVerification = parameterVerification;
              } else if (parameterVerification.getVerificationType() == Type.VerificationType.PEER) {
                peerAcceptedParameterVerification = parameterVerification;
              }
            }
          }

          StringBuilder displayName = new StringBuilder();
          if (selfAcceptedParameterVerification != null) {
            displayName.append(getParameterVerificationCommonName(Type.JobLogTriggerType.PARAMETER_SELF_VERIFIED_AT))
              .append(Utility.SPACE)
              .append(Utility.HYPHEN)
              .append(Utility.SPACE)
              .append(parameter.getLabel());
            selfVerificationLogDoneAt.setDisplayName(displayName.toString());
            selfVerificationLogDoneAt.setTriggerType(Type.JobLogTriggerType.PARAMETER_SELF_VERIFIED_AT);
            selfVerificationLogDoneAt.setValue(selfAcceptedParameterVerification.getModifiedAt().toString());
            selfVerificationLogDoneAt.setIdentifierValue(selfAcceptedParameterVerification.getModifiedAt().toString());

            selfVerificationLogDoneAt.setEntityId(parameter.getIdAsString());

            String selfVerificationDoneBy = Utility.getFullNameAndEmployeeIdFromPrincipalUser(userMapper.toPrincipalUser(selfAcceptedParameterVerification.getModifiedBy()));
            selfVerificationLogDoneBy.setDisplayName(getParameterVerificationCommonName(Type.JobLogTriggerType.PARAMETER_SELF_VERIFIED_BY));
            selfVerificationLogDoneBy.setTriggerType(Type.JobLogTriggerType.PARAMETER_SELF_VERIFIED_BY);
            selfVerificationLogDoneBy.setValue(selfVerificationDoneBy);
            selfVerificationLogDoneBy.setIdentifierValue(selfAcceptedParameterVerification.getModifiedBy().getIdAsString());
            selfVerificationLogDoneBy.setEntityId(parameter.getIdAsString());

            logs.add(selfVerificationLogDoneAt);
            logs.add(selfVerificationLogDoneBy);

          }

          if (peerAcceptedParameterVerification != null) {
            displayName = new StringBuilder();
            displayName.append(getParameterVerificationCommonName(Type.JobLogTriggerType.PARAMETER_PEER_VERIFIED_AT))
              .append(Utility.SPACE)
              .append(Utility.HYPHEN)
              .append(Utility.SPACE)
              .append(parameter.getLabel());
            peerVerificationLogDoneAt.setDisplayName(displayName.toString());
            peerVerificationLogDoneAt.setTriggerType(Type.JobLogTriggerType.PARAMETER_PEER_VERIFIED_AT);
            peerVerificationLogDoneAt.setValue(peerAcceptedParameterVerification.getModifiedAt().toString());
            peerVerificationLogDoneAt.setIdentifierValue(peerAcceptedParameterVerification.getModifiedAt().toString());
            peerVerificationLogDoneAt.setEntityId(parameter.getIdAsString());

            String peerVerificationDoneBy = Utility.getFullNameAndEmployeeIdFromPrincipalUser(userMapper.toPrincipalUser(peerAcceptedParameterVerification.getModifiedBy()));
            peerVerificationLogDoneBy.setDisplayName(getParameterVerificationCommonName(Type.JobLogTriggerType.PARAMETER_PEER_VERIFIED_BY));
            peerVerificationLogDoneBy.setTriggerType(Type.JobLogTriggerType.PARAMETER_PEER_VERIFIED_BY);
            peerVerificationLogDoneBy.setValue(peerVerificationDoneBy);
            peerVerificationLogDoneBy.setIdentifierValue(peerAcceptedParameterVerification.getModifiedBy().getIdAsString());
            peerVerificationLogDoneBy.setEntityId(parameter.getIdAsString());

            String peerVerificationStatus = peerAcceptedParameterVerification.getVerificationStatus().name();
            peerVerificationLogStatus.setDisplayName(getParameterVerificationCommonName(Type.JobLogTriggerType.PARAMETER_PEER_STATUS));
            peerVerificationLogStatus.setTriggerType(Type.JobLogTriggerType.PARAMETER_PEER_STATUS);
            peerVerificationLogStatus.setValue(peerVerificationStatus);
            peerVerificationLogStatus.setIdentifierValue(peerVerificationStatus);
            peerVerificationLogStatus.setEntityId(parameter.getIdAsString());

            logs.add(peerVerificationLogDoneAt);
            logs.add(peerVerificationLogDoneBy);
            logs.add(peerVerificationLogStatus);
          }


        }
      }
      jobLogRecord.setParameterValues(parameterValues);

      List<JobAnnotation> jobAnnotations = jobAnnotationRepository.findByJobId(job.getId());
      for (JobAnnotation jobAnnotation : jobAnnotations) {
        if (!Utility.isEmpty(jobAnnotation.getRemarks())) {
          var jobAnnotationLog = new JobLogData(JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.ANNOTATION_REMARK, "Job Annotation", jobAnnotation.getRemarks(), jobAnnotation.getRemarks(), null, null);
          logs.add(jobAnnotationLog);
        }
        if (!Utility.isEmpty(jobAnnotation.getMedias())) {
          List<JobLogMediaData> jobLogMedias = new ArrayList<>();
          for (var media : jobAnnotation.getMedias()) {
            JobLogMediaData jobLogMedia = new JobLogMediaData();
            jobLogMedia.setName(media.getMedia().getName());
            jobLogMedia.setType(media.getMedia().getType());
            jobLogMedia.setLink(mediaConfig.getCdn() + File.separator + media.getMedia().getRelativePath() + File.separator + media.getMedia().getFilename());
            jobLogMedia.setDescription(media.getMedia().getDescription());
            jobLogMedias.add(jobLogMedia);
          }

          var jobAnnotationMediaLog = new JobLogData(JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.ANNOTATION_MEDIA, "Job Annotation Media", null, null, jobLogMedias, null);
          logs.add(jobAnnotationMediaLog);
        }
      }

      List<JobLogTaskExecutionView> taskExecutions = taskExecutionMap.get(job.getId());

      for (var taskExecution : taskExecutions) {

        if (taskExecution.getStartedAt() != null && taskExecution.getTaskStartedByFirstName() != null) {
          var taskStartedAt = new JobLogData(taskExecution.getTaskId().toString(), Type.JobLogTriggerType.TSK_START_TIME, taskExecution.getName() + " " + "Started At", taskExecution.getStartedAt().toString(), taskExecution.getStartedAt().toString(), null, null);
          var fullNameTaskStartedBy = Utility.getFullNameAndEmployeeId(taskExecution.getTaskStartedByFirstName(), taskExecution.getTaskStartedByLastName(), taskExecution.getTaskStartedByEmployeeId());
          var taskStartedBy = new JobLogData(taskExecution.getTaskId().toString(), Type.JobLogTriggerType.TSK_STARTED_BY, taskExecution.getName() + " " + "Started By", fullNameTaskStartedBy, taskExecution.getTaskStartedById(), null, null);
          logs.add(taskStartedAt);
          logs.add(taskStartedBy);
        }
        if (taskExecution.getEndedAt() != null && taskExecution.getTaskModifiedByFirstName() != null) {
          var taskEndedAt = new JobLogData(taskExecution.getTaskId().toString(), Type.JobLogTriggerType.TSK_END_TIME, taskExecution.getName() + " " + "Ended At", taskExecution.getEndedAt().toString(), taskExecution.getEndedAt().toString(), null, null);
          var fullNameTaskEndedBy = Utility.getFullNameAndEmployeeId(taskExecution.getTaskModifiedByFirstName(), taskExecution.getTaskModifiedByLastName(), taskExecution.getTaskModifiedByEmployeeId());
          var taskEndedBy = new JobLogData(taskExecution.getTaskId().toString(), Type.JobLogTriggerType.TSK_ENDED_BY, taskExecution.getName() + " " + "Ended By", fullNameTaskEndedBy, taskExecution.getTaskModifiedById(), null, null);
          logs.add(taskEndedAt);
          logs.add(taskEndedBy);
        }
      }
      jobLogs.add(jobLogRecord);
    }
    bulkSave(jobLogs);
  }

  private void bulkSave(Set<JobLog> jobLogs) {
    if (!jobLogs.isEmpty()) {
      mongoTemplate.insertAll(jobLogs);
    }
  }

  private void jobLogs() throws IOException, ResourceNotFoundException {
    mongoTemplate.remove(new Query(), JOB_LOGS);
    List<Long> checklistIds = checklistRepository.findByStateInOrderByStateDesc(Set.of(State.Checklist.PUBLISHED, State.Checklist.DEPRECATED));
    for (Long checklistId : checklistIds) {
      log.info("[jobLogs] processing job logs for checklist: {}", checklistId);
      try {
        checklistService.reconfigureJobLogColumns(checklistId);
        List<Job> jobList = jobRepository.findAllByChecklistId(checklistId);
        updateJobLogs(jobList, checklistId);
      } catch (ResourceNotFoundException | IOException e) {
        log.error("Error while migrating job logs for checklist id: " + checklistId, e);
        throw e;
      }
    }
  }

  private String getParameterVerificationCommonName(Type.JobLogTriggerType jobLogTriggerType) {
    switch (jobLogTriggerType) {
      case PARAMETER_PEER_VERIFIED_AT, PARAMETER_PEER_VERIFIED_BY -> {
        return "Peer Verification";
      }
      case PARAMETER_SELF_VERIFIED_AT, PARAMETER_SELF_VERIFIED_BY -> {
        return "Self Verification";
      }
      default -> {
        return "";
      }
    }
  }


  public BasicDto execute() throws Exception {
    jobLogs();
    return (new BasicDto()).setMessage("Migration Successful");
  }
}
