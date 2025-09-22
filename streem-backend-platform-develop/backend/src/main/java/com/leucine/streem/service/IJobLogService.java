package com.leucine.streem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.collections.CustomViewColumn;
import com.leucine.streem.collections.JobLog;
import com.leucine.streem.collections.JobLogMediaData;
import com.leucine.streem.constant.State;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.ResourceParameterChoiceDto;
import com.leucine.streem.dto.UserAuditDto;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.model.Checklist;
import com.leucine.streem.model.helper.JobLogColumn;
import com.leucine.streem.model.helper.PrincipalUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

public interface IJobLogService {
  Page<JobLog> findAllJobLogs(String filters, String customViewId, Pageable pageable) throws ResourceNotFoundException;

  JobLog findByJobId(String jobId);

  void createJobLog(String jobId, String jobCode, State.Job jobState, Long jobCreatedAt, UserAuditDto jobCreatedBy, String checklistId,
                    String checklistName, String checklistCode, String facilityId, PrincipalUser principalUser);

  List<JobLogColumn> getJobLogColumnForChecklist(Checklist checklist);

  //TODO: Removed @Async annotation move this to single mongo query upsert
  void recordJobLogTrigger(String jobId, String entityId, Type.JobLogTriggerType jobLogTriggerType, String label, List<JobLogMediaData> medias,
                           String value, String identifierValue, UserAuditDto modifiedBy);

  void recordJobLogTrigger(String jobId, String entityId, Type.JobLogTriggerType jobLogTriggerType, String label, List<JobLogMediaData> medias, String value, String identifierValue, Type.Parameter parameterType, UserAuditDto userAuditDto);

  void recordJobLogTrigger(String jobId, String entityId, Type.JobLogTriggerType jobLogTriggerType, String label, List<JobLogMediaData> medias, String value, String identifierValue, Type.Parameter parameterType, UserAuditDto userAuditDto, List<String> choices);

  void recordJobLogResource(String jobId, String parameterId, String label, String objectTypeId, String objectTypeDisplayName, List<ResourceParameterChoiceDto> choices);

  void updateJobState(String jobId, PrincipalUser principalUser);

  void updateJobLog(Long jobId, Long parameterId, Type.Parameter parameterType, String parameterValueReason, String label, Type.JobLogTriggerType triggerType, UserAuditDto userAuditDto) throws IOException;

  ByteArrayInputStream createJobLogExcel(String customViewId, String filters) throws IOException, ResourceNotFoundException;

  byte[] createJobLogPdf(String customViewId, String filters, Type.JobLogType jobLogType, String pdfMetaData) throws IOException, ResourceNotFoundException;
}
