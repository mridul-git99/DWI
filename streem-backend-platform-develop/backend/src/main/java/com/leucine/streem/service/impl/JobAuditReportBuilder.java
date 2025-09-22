package com.leucine.streem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.*;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.JobAuditParameter;
import com.leucine.streem.model.helper.JobAuditParameterValue;
import com.leucine.streem.service.IPdfReportBuilder;
import com.leucine.streem.util.DateTimeUtils;
import com.leucine.streem.util.JsonUtils;
import com.leucine.streem.util.Utility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Builder for Job Audit Report PDF type
 * Handles the generation of job audit trail reports
 */
@Component
@RequiredArgsConstructor
public class JobAuditReportBuilder implements IPdfReportBuilder {

    private final com.leucine.streem.dto.mapper.IFacilityMapper facilityMapper;
    private final com.leucine.streem.repository.IUserRepository userRepository;

    @Override
    public String buildReport(GeneratedPdfDataDto variables) throws JsonProcessingException {
        StringBuilder reportSection = new StringBuilder();
        ObjectMapper objectMapper = new ObjectMapper();
        PdfBuilderServiceHelpers.HtmlTemplates htmlTemplates = new PdfBuilderServiceHelpers.HtmlTemplates();

        // Process details
        Checklist checklistInfo = variables.getChecklist();
        reportSection.append(htmlTemplates.sectionTitle("Process Details"));
        
        StringBuilder detailTableContent = new StringBuilder();
        detailTableContent.append(htmlTemplates.tableRow("Process ID", checklistInfo.getCode()));
        detailTableContent.append(htmlTemplates.tableRow("Name", checklistInfo.getName()));

        // Process checklist property values efficiently
        List<ChecklistPropertyValue> checklistPropertyValues = variables.getChecklistPropertyValues();
        if (checklistPropertyValues != null) {
            for (ChecklistPropertyValue checklistPropertyValue : checklistPropertyValues) {
                Property property = checklistPropertyValue.getFacilityUseCasePropertyMapping().getProperty();
                if (property != null) {
                    detailTableContent.append(htmlTemplates.tableRow(
                        property.getLabel(),
                        String.valueOf(checklistPropertyValue.getValue())
                    ));
                }
            }
        }
        
        reportSection.append(htmlTemplates.detailPanel(htmlTemplates.detailTable(detailTableContent.toString())));

        // Job details
        Job job = variables.getJob();
        reportSection.append(htmlTemplates.sectionTitle("Job Details"));
        
        StringBuilder jobDetailsContent = new StringBuilder();
        jobDetailsContent.append(htmlTemplates.tableRow("Job ID", job.getCode()));
        jobDetailsContent.append(htmlTemplates.tableRow("State", Utility.toDisplayName(job.getState())));

      String startedAt = "-";
      if (!Utility.isEmpty(job.getStartedAt())) {
          long startedAtTimestamp = job.getStartedAt();
          if (!Utility.isEmpty(variables.getFacility()) && !Utility.isEmpty(variables.getFacility().getTimeZone())) {
              String zoneOffsetString = PdfBuilderServiceHelpers.getZoneOffsetString(variables.getFacility().getTimeZone());
              startedAtTimestamp = DateTimeUtils.addOffSetToTime(startedAtTimestamp, zoneOffsetString);
          }
          String dateTimeFormat = variables.getFacility().getDateTimeFormat();
          startedAt = DateTimeUtils.getFormattedDateTimeOfPattern(startedAtTimestamp, dateTimeFormat);
      }
      jobDetailsContent.append(htmlTemplates.tableRow("Job Started On", startedAt));

        // Job started by - with null checks
        if (!Utility.isEmpty(job.getStartedBy())) {
            jobDetailsContent.append(htmlTemplates.tableRow("Job Started By",
                Utility.getFullNameAndEmployeeId(job.getStartedBy().getFirstName(), job.getStartedBy().getLastName(), job.getStartedBy().getEmployeeId())));
        } else {
            jobDetailsContent.append(htmlTemplates.tableRow("Job Started By", "-"));
        }

      String endedAt = "-";
      if (!Utility.isEmpty(job.getEndedAt())) {
          long endedAtTimestamp = job.getEndedAt();
          if (!Utility.isEmpty(variables.getFacility()) && !Utility.isEmpty(variables.getFacility().getTimeZone())) {
              String zoneOffsetString = PdfBuilderServiceHelpers.getZoneOffsetString(variables.getFacility().getTimeZone());
              endedAtTimestamp = DateTimeUtils.addOffSetToTime(endedAtTimestamp, zoneOffsetString);
          }
          String dateTimeFormat = variables.getFacility().getDateTimeFormat();
          endedAt = DateTimeUtils.getFormattedDateTimeOfPattern(endedAtTimestamp, dateTimeFormat);
      }
      jobDetailsContent.append(htmlTemplates.tableRow("Job Completed On", endedAt));

      // Job completed by - with null checks
        if (!Utility.isEmpty(job.getEndedBy()) && !Utility.isEmpty(job.getEndedAt())) {
            jobDetailsContent.append(htmlTemplates.tableRow(
                "Job Completed By", 
                Utility.getFullNameAndEmployeeId(job.getEndedBy().getFirstName(), job.getEndedBy().getLastName(), job.getEndedBy().getEmployeeId())
            ));
        } else {
            jobDetailsContent.append(htmlTemplates.tableRow("Job Completed By", "-"));
        }

        // Add parameter values if available - with optimized parameter lookup
        List<ParameterValue> parameterValues = variables.getParameterValues();
        List<Parameter> parameters = variables.getParameters();
        if (!Utility.isEmpty(parameters) && !Utility.isEmpty(parameterValues)) {
            // Create a map for faster parameter lookup
            Map<Long, Parameter> parameterMap = parameters.stream()
                .collect(Collectors.toMap(Parameter::getId, p -> p));
                
            for (ParameterValue pv : parameterValues) {
                Parameter param = parameterMap.get(pv.getParameterId());
                if (param == null) continue;
                
                // Skip hidden parameters
                boolean isHidden = param.getData() != null && param.getData().has("hidden") && param.getData().get("hidden").asBoolean();
                if (isHidden) continue;

                String value = String.valueOf(pv.getValue());
                boolean isInstructionType = param.getType().equals(Type.Parameter.INSTRUCTION.toString());
                if (isInstructionType && param.getData() != null && param.getData().has("text")) {
                    value = param.getData().get("text").asText();
                    // For instruction type, don't escape HTML
                    jobDetailsContent.append(htmlTemplates.tableRowWithRawHtml(param.getLabel(), value));
                } else {
                    jobDetailsContent.append(htmlTemplates.tableRow(param.getLabel(), value));
                }
            }
        }


        String duration = (!Utility.isEmpty(job.getStartedAt()) && !Utility.isEmpty(job.getEndedAt()))
            ? DateTimeUtils.timeFormatDuration(variables.getJobPrintDto().getTotalDuration())
            : "-";

        jobDetailsContent.append(htmlTemplates.tableRow("Job Duration", duration));

        reportSection.append(htmlTemplates.detailPanel(htmlTemplates.detailTable(jobDetailsContent.toString())));

        // Job exception details - with null checks
        if (job.getState() == com.leucine.streem.constant.State.Job.COMPLETED_WITH_EXCEPTION && variables.getCweDetails() != null) {
            JobCweDto cweDetails = variables.getCweDetails();
            
            StringBuilder exceptionContent = new StringBuilder();
            exceptionContent.append(htmlTemplates.tableRow("Exception Reason", cweDetails.getReason()));
            exceptionContent.append(htmlTemplates.tableRow("Additional Comments", cweDetails.getComment()));
            
            reportSection.append(htmlTemplates.sectionTitle("Job Exception Details"));
            reportSection.append(htmlTemplates.detailPanel(htmlTemplates.detailTable(exceptionContent.toString())));
        }

        // Stage and task details
        StringBuilder stageTaskContent = new StringBuilder();
        stageTaskContent.append(htmlTemplates.tableRow("Total Stages", String.valueOf(variables.getTotalStages())));
        stageTaskContent.append(htmlTemplates.tableRow("Total Tasks", String.valueOf(variables.getTotalTask())));
        
        reportSection.append(htmlTemplates.sectionTitle("Stage and Task Details"));
        reportSection.append(htmlTemplates.detailPanel(htmlTemplates.detailTable(stageTaskContent.toString())));

        // Add Filters Applied section if filters exist
        String filters = variables.getFilters();
        if (!Utility.isEmpty(filters)) {
            reportSection.append(htmlTemplates.sectionTitle("Filters Applied"));
            
            // Use specialized helper to format JobAudit filters exactly as "End Time" and "Users"
            String formattedFilters = PdfBuilderServiceHelpers.formatJobAuditFiltersForDisplay(filters, variables.getFacility(), userRepository);
            
            // Add to report section if there are any filters
            if (formattedFilters != null && formattedFilters.length() > 0) {
                reportSection.append(htmlTemplates.detailPanel(htmlTemplates.detailTable(formattedFilters)));
            } else {
                // If no valid filters, just show the raw filters string
                reportSection.append(htmlTemplates.detailPanel(htmlTemplates.detailTable(
                    htmlTemplates.tableRow("Applied Filters", filters)
                )));
            }
        }

        // Job annotation details - with null checks
        JobAnnotation jobAnnotation = variables.getJobAnnotation();
        if (jobAnnotation != null) {
            reportSection.append(htmlTemplates.sectionTitle("Job Annotations"));
            
            // Build media list if available
            String mediaHtml = "";
            if (jobAnnotation.getMedias() != null && !jobAnnotation.getMedias().isEmpty()) {
                StringBuilder mediaList = new StringBuilder();
                mediaList.append("<ul>");
                for (JobAnnotationMediaMapping mapping : jobAnnotation.getMedias()) {
                    Media m = mapping.getMedia();
                    if (m != null) {
                        mediaList.append("<li>")
                            .append("<a href=\"").append(m.getRelativePath()).append("\">")
                            .append(m.getName() != null ? m.getName() : m.getRelativePath())
                            .append("</a>")
                            .append("</li>");
                    }
                }
                mediaList.append("</ul>");
                mediaHtml = mediaList.toString();
            }
            
            String annotationBox = htmlTemplates.annotationBox(
                jobAnnotation.getRemarks(), 
                mediaHtml
            );
            
            reportSection.append(htmlTemplates.detailPanel(annotationBox));
        }

        reportSection.append(htmlTemplates.PAGE_BREAK);

        // Audit section - with optimized date processing
        List<JobAudit> jobAudits = variables.getJobAudits();
        FacilityDto facility = variables.getFacility();

        if (jobAudits != null && !jobAudits.isEmpty() && facility != null) {
            String dateFmt = facility.getDateFormat();
            String timeFmt = facility.getTimeFormat();
            String tz = facility.getTimeZone();

            // Process audits in chunks to reduce memory usage
            final int CHUNK_SIZE = 100;
            Map<String, List<JobAudit>> auditsByDate = new LinkedHashMap<>();
            
            // Ensure audits are sorted by triggeredAt in descending order before grouping
            jobAudits.sort(Comparator.comparing(JobAudit::getId).reversed());
            
            // Group audits by date for more efficient processing
            for (JobAudit audit : jobAudits) {
                if (audit == null || audit.getTriggeredAt() == null) continue;
                
                long zoned = DateTimeUtils.convertUTCEpochToZoneEpoch(audit.getTriggeredAt(), tz);
                String date = DateTimeUtils.getFormattedDatePattern(zoned, dateFmt);
                
                auditsByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(audit);
            }
            
            // Process each date in insertion order (no sorting needed as data comes pre-sorted)
            for (String date : auditsByDate.keySet()) {
                List<JobAudit> auditsForDate = auditsByDate.get(date);
                
                auditsForDate.sort(Comparator.comparingLong(JobAudit::getId).reversed());
                
                StringBuilder dayItems = new StringBuilder();
                int dayCount = 0;
                
                // Process audits in chunks
                for (int i = 0; i < auditsForDate.size(); i += CHUNK_SIZE) {
                    int end = Math.min(i + CHUNK_SIZE, auditsForDate.size());
                    List<JobAudit> chunk = auditsForDate.subList(i, end);
                    
                    for (JobAudit audit : chunk) {
                        if (audit.getDetails() == null) continue;
                        
                        long zoned = DateTimeUtils.convertUTCEpochToZoneEpoch(audit.getTriggeredAt(), tz);
                        String time = DateTimeUtils.getFormattedDateTimeOfPattern(zoned, timeFmt);
                        String details = audit.getDetails();
                        if (details != null && details.contains("{{{0}}}")) {
                            String value = extractAuditParameterDisplayValue(audit, facility);
                            details = details.replace("{{{0}}}", value);
                        }
                        dayItems.append(htmlTemplates.activityItem(time, details));
                        dayCount++;
                    }
                }
                
                // Add the day section to the report
                reportSection.append(htmlTemplates.daySection(date, dayCount, dayItems.toString()));
            }
        }

        return reportSection.toString();
    }


  private String extractAuditParameterDisplayValue(JobAudit audit, Object facility) throws JsonProcessingException {
    if (Utility.isEmpty(audit.getParameters())) return "-";

    Map<String, JobAuditParameterValue> paramMap = JsonUtils.readValue(audit.getParameters().toString(), new TypeReference<>() {});

    JobAuditParameterValue paramValue = paramMap.get("0");
    if (Utility.isEmpty(paramValue.getValue())) {
      return "-";
    }

    String valueText = paramValue.getValue().toString();
    if ("-".equals(valueText)) {
      return "-";
    }
    long epoch = Long.parseLong(valueText);

    Facility facilityEntity = null;
    if (facility instanceof Facility) {
      facilityEntity = (Facility) facility;
    } else if (facility instanceof FacilityDto) {
      facilityEntity = facilityMapper.toEntity((FacilityDto) facility);
    } else {
      return valueText;
    }

    return switch (paramValue.getType()) {
      case DATE_TIME -> DateTimeUtils.getFormattedDateTimeForFacility(epoch, facilityEntity);
      case DATE -> DateTimeUtils.getFormattedDateForFacility(epoch, facilityEntity);
      default -> valueText;
    };
  }

  @Override
    public Type.PdfType getSupportedReportType() {
        return Type.PdfType.JOB_AUDIT;
    }
}
