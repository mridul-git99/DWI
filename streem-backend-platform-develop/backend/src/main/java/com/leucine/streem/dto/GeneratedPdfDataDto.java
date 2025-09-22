package com.leucine.streem.dto;

import com.leucine.streem.collections.CustomView;
import com.leucine.streem.collections.CustomViewColumn;
import com.leucine.streem.collections.EntityObject;
import com.leucine.streem.collections.JobLog;
import com.leucine.streem.collections.changelogs.ChangeLogDataDto;
import com.leucine.streem.collections.changelogs.EntityObjectChangeLog;
import com.leucine.streem.constant.Type;
import com.leucine.streem.model.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedPdfDataDto {
  private Long generatedOn;
  private String userFullName;
  private String userId;
  private String timezoneOffset;
  private String dateFormat;
  private String dateTimeFormat;
  private String filters;
  
  private FacilityDto facility;
  private Job job;
  private Checklist checklist;
  private JobAnnotation jobAnnotation;

  private List<EntityObjectChangeLog> changeLogs = new ArrayList<>();
  private EntityObject objectType;
  private List<AuditDto> audits = new ArrayList<>();
  private List<ChecklistPropertyValue> checklistPropertyValues = new ArrayList<>();
  private List<JobLog> jobLogs = new ArrayList<>();
  private List<CustomViewColumn> columnsList = new ArrayList<>();
  private List<CustomView> customViews = new ArrayList<>();
  private List<JobAudit> jobAudits = new ArrayList<>();
  private List<Parameter> parameters = new ArrayList<>();
  private List<ParameterValue> parameterValues = new ArrayList<>();
  private ChecklistJobDto checklistJobDto;
  private ChecklistDto checklistDto;
  private Long totalStages;
  private Long totalTask;
  private JobCweDto cweDetails;
  private Object customView;
  private JobPrintDto jobPrintDto;
  private Object columns;
  
  // Fields for job logs PDF report
  private String customViewName;
  private String processName;
  
  // Fields for object job logs PDF report
  private String objectId;
  private String objectDisplayName;
  private String objectExternalId;
  private String objectTypeDisplayName;
  
  // Field to distinguish between different job log types for generic PDF generation
  private Type.JobLogType jobLogType;
}
