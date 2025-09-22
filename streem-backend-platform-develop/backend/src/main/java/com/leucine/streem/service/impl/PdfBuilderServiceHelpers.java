package com.leucine.streem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leucine.streem.collections.JobLogMediaData;
import com.leucine.streem.collections.JobLogResource;
import com.leucine.streem.constant.State;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.*;
import com.leucine.streem.model.JobAudit;
import com.leucine.streem.model.helper.search.SearchCriteria;
import com.leucine.streem.model.helper.search.SearchFilter;
import com.leucine.streem.service.HtmlTemplateEngine;
import com.leucine.streem.util.DateTimeUtils;
import com.leucine.streem.util.JsonUtils;
import com.leucine.streem.util.Utility;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.util.HtmlUtils;

import static com.leucine.streem.constant.State.ParameterException.*;
import static com.leucine.streem.service.HtmlTemplateEngine.*;
import static java.util.Locale.ROOT;

import javax.persistence.criteria.Predicate;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Helper methods for PdfBuilderService
 */
public class PdfBuilderServiceHelpers {

    /**
     * Maps filter operator codes to user-friendly display names.
     * @param op The operator code (e.g., "EQ", "NE", "GT", etc.)
     * @return The user-friendly display name.
     */
    public static String getOperatorDisplayName(String op) {
        if (op == null) return "=";
        switch (op.toUpperCase()) {
            case "EQ": return "Equals";
            case "NE": return "Not Equals";
            case "GT": return "Greater Than";
            case "GTE": case "GOE": return "Greater Than or Equal";
            case "LT": return "Less Than";
            case "LTE": case "LOE": return "Less Than or Equal";
            case "LIKE": return "Contains";
            case "IN": return "In";
            case "NIN": return "Not In";
            case "BETWEEN": return "Between";
            default: return op;
        }
    }
  
  // Configuration constants
  private static final int RECURRING_TASK_COLUMN_LIMIT = 3;
  
  // Selection indicator constants
  private static final String SELECTED_TICK = "•";  // Bullet point - proven to work with iText PDF
  private static final String SELECTED_CHECKBOX = "[ " + SELECTED_TICK + " ]";
  private static final String UNSELECTED_CHECKBOX = "[ ]";
  private static final String SELECTED_RADIO = "(" + SELECTED_TICK + ")";
  private static final String UNSELECTED_RADIO = "( )";

  /**
   * Contains common HTML templates used across different PDF builder methods
   */
  public static class HtmlTemplates {
    // Section titles
    public static final String SECTION_TITLE_START = "<h4 class=\"section-title\">";
    public static final String SECTION_TITLE_END = "</h4>";
    
    // Detail panels
    public static final String DETAIL_PANEL_START = "<div class=\"detail-panel\">";
    public static final String DETAIL_PANEL_END = "</div>";
    
    // Detail tables
    public static final String DETAIL_TABLE_START = "<table class=\"detail-table\">";
    public static final String DETAIL_TABLE_END = "</table>";
    
    // Table rows
    public static final String TABLE_ROW_START = "<tr>";
    public static final String TABLE_ROW_END = "</tr>";
    public static final String TABLE_HEADER_START = "<th>";
    public static final String TABLE_HEADER_END = "</th>";
    public static final String TABLE_DATA_START = "<td>";
    public static final String TABLE_DATA_END = "</td>";
    
    // Common table row template
    public static final String TABLE_ROW_TEMPLATE = "<tr><th>%s</th><td>%s</td></tr>";
    
    // Page break
    public static final String PAGE_BREAK = "<div class=\"page-break\"></div>";
    
    // Annotation box
    public static final String ANNOTATION_BOX_START = "<div class=\"annotation-box\">";
    public static final String ANNOTATION_BOX_END = "</div>";
    
    // Stage header
    public static final String STAGE_HEADER_START = "<div class=\"stage-header\">";
    public static final String STAGE_HEADER_END = "</div>";
    public static final String STAGE_NUMBER_START = "<h3 class=\"stage-number\">Stage ";
    public static final String STAGE_NUMBER_END = "</h3>";
    public static final String STAGE_NAME_START = "<div class=\"stage-name\">";
    public static final String STAGE_NAME_END = "</div>";
    public static final String STAGE_SEPARATOR = "<div class=\"stage-separator\"></div>";
    
    // Parameter table
    public static final String PARAMETER_TABLE_START = "<table class=\"parameter-table\">";
    public static final String PARAMETER_TABLE_END = "</table>";
    public static final String PARAMETER_TABLE_HEADER = "<thead><tr><th>Attribute</th><th>Values</th><th>Person</th><th>Time</th></tr></thead>";
    
    // Task state
    public static final String TASK_STATE_START = "<p class=\"task-state\">Task State: ";
    public static final String TASK_STATE_END = "</p>";
    
    // Activities
    public static final String DATE_SECTION_START = "<section class=\"date-section\">";
    public static final String DATE_SECTION_END = "</section>";
    public static final String ACTIVITIES_LIST_START = "<ul class=\"activities\">";
    public static final String ACTIVITIES_LIST_END = "</ul>";
    public static final String ACTIVITY_ITEM_TEMPLATE = "<li><span class=\"circle\"></span><span class=\"time\">%s</span><span class=\"details\">%s</span></li>";
    
    // Job logs table
    public static final String JOB_LOG_TABLE_START = "<div class=\"job-log-table\"><table style=\"table-layout: fixed; width: 100%;\">";
    public static final String JOB_LOG_TABLE_END = "</table></div>";
    
    // Stage instructions
    public static final String STAGE_INSTRUCTIONS_START = "<div class=\"stage-instructions\">";
    public static final String STAGE_INSTRUCTIONS_END = "</div>";
    public static final String INSTRUCTION_ITEM_START = "<div class=\"instruction-item\">";
    public static final String INSTRUCTION_ITEM_END = "</div>";
    public static final String INSTRUCTION_LABEL_START = "<div class=\"instruction-label\"><strong>";
    public static final String INSTRUCTION_LABEL_END = "</strong></div>";
    public static final String INSTRUCTION_CONTENT_START = "<div class=\"instruction-content\">";
    public static final String INSTRUCTION_CONTENT_END = "</div>";
    
    // Task pause details
    public static final String TASK_PAUSE_DETAILS_START = "<div class=\"task-pause-details\">";
    public static final String TASK_PAUSE_DETAILS_END = "</div>";
    public static final String TASK_PAUSE_TABLE_START = "<table class=\"task-pause-resume-table\">";
    public static final String TASK_PAUSE_TABLE_END = "</table>";
    public static final String TASK_PAUSE_TABLE_HEADER = "<thead><tr><th>Paused At</th><th>Paused By</th><th>Resumed At</th><th>Resumed By</th><th>Reason For Pause</th></tr></thead>";
    
    // Early start and delayed completion notes
    public static final String EARLY_START_NOTE_START = "<div class=\"early-start-note\">";
    public static final String EARLY_START_NOTE_END = "</div>";
    public static final String DELAYED_COMPLETION_NOTE_START = "<div class=\"delayed-completion-note\">";
    public static final String DELAYED_COMPLETION_NOTE_END = "</div>";
    /**
     * Creates a section title
     * @param title The title text
     * @return Formatted HTML for section title
     */
    public static String sectionTitle(String title) {
        return SECTION_TITLE_START + HtmlUtils.htmlEscape(title) + SECTION_TITLE_END;
    }
    
    /**
     * Creates a detail panel with content
     * @param content The HTML content to include in the panel
     * @return Formatted HTML for detail panel
     */
    public static String detailPanel(String content) {
        return DETAIL_PANEL_START + content + DETAIL_PANEL_END;
    }
    
    /**
     * Creates a detail table with content
     * @param content The HTML content to include in the table
     * @return Formatted HTML for detail table
     */
    public static String detailTable(String content) {
        return DETAIL_TABLE_START + content + DETAIL_TABLE_END;
    }
    
    /**
     * Creates a table row with label and value
     * @param label The label text
     * @param value The value text
     * @return Formatted HTML for table row
     */
    public static String tableRow(String label, String value) {
        return String.format(TABLE_ROW_TEMPLATE, HtmlUtils.htmlEscape(label), HtmlUtils.htmlEscape(value));
    }
    
    /**
     * Creates a table row with label and raw HTML value (not escaped)
     * @param label The label text
     * @param rawHtmlValue The raw HTML value (not escaped)
     * @return Formatted HTML for table row
     */
    public static String tableRowWithRawHtml(String label, String rawHtmlValue) {
        return "<tr><th>" + HtmlUtils.htmlEscape(label) + "</th><td>" + rawHtmlValue + "</td></tr>";
    }
    
    /**
     * Creates a stage header
     * @param stageNumber The stage number
     * @param stageName The stage name
     * @return Formatted HTML for stage header
     */
    public static String stageHeader(String stageNumber, String stageName) {
        return STAGE_HEADER_START +
               STAGE_NUMBER_START + HtmlUtils.htmlEscape(stageNumber) + STAGE_NUMBER_END +
               STAGE_NAME_START + HtmlUtils.htmlEscape(stageName) + STAGE_NAME_END +
               STAGE_SEPARATOR +
               STAGE_HEADER_END;
    }
    
    /**
     * Creates a task state paragraph
     * @param state The task state
     * @return Formatted HTML for task state
     */
    public static String taskState(String state) {
        return TASK_STATE_START + HtmlUtils.htmlEscape(state) + TASK_STATE_END;
    }
    
  /**
   * Gets the zone offset string for a timezone ID
   * @param timezoneId The timezone ID to convert
   * @return The ZoneOffset string
   */
  public static String getZoneOffsetString(String timezoneId) {
      if (timezoneId.startsWith("+") || timezoneId.startsWith("-")) {
        return timezoneId;
      }
      
      ZoneId zoneId = ZoneId.of(timezoneId);
      ZonedDateTime now = ZonedDateTime.now(zoneId);
      return now.getOffset().getId();
  }

  /**
   * Generates a user-friendly time condition message for a task
   * @param task The task DTO containing timer information
   * @return Formatted time condition message or null if no time condition
   */
  public static String getTaskTimeConditionMessage(TaskDto task) {
    if (task == null || !task.isTimed() || task.getTimerOperator() == null) {
      return null;
    }

    try {
      com.leucine.streem.constant.Operator.Timer timerOperator = 
          com.leucine.streem.constant.Operator.Timer.valueOf(task.getTimerOperator());
      
      StringBuilder message = new StringBuilder();
      
      if (timerOperator == com.leucine.streem.constant.Operator.Timer.LESS_THAN) {
        // For LESS_THAN, use maxPeriod
        if (task.getMaxPeriod() != null && task.getMaxPeriod() > 0) {
          message.append("Complete under ").append(DateTimeUtils.timeFormatDuration(task.getMaxPeriod()));
        }
      } else if (timerOperator == com.leucine.streem.constant.Operator.Timer.NOT_LESS_THAN) {
        // For NOT_LESS_THAN, we can have both minPeriod and maxPeriod
        boolean hasMin = task.getMinPeriod() != null && task.getMinPeriod() > 0;
        boolean hasMax = task.getMaxPeriod() != null && task.getMaxPeriod() > 0;
        
        if (hasMin && hasMax) {
          // Both min and max periods - show as range
          message.append("Complete between ")
                 .append(DateTimeUtils.timeFormatDuration(task.getMinPeriod()))
                 .append(" and ")
                 .append(DateTimeUtils.timeFormatDuration(task.getMaxPeriod()));
        } else if (hasMin) {
          // Only min period - do not complete before
          message.append("Do not complete before ").append(DateTimeUtils.timeFormatDuration(task.getMinPeriod()));
        } else if (hasMax) {
          // Only max period - complete within
          message.append("Complete within ").append(DateTimeUtils.timeFormatDuration(task.getMaxPeriod()));
        }
      }
      
      return message.length() > 0 ? message.toString() : null;
    } catch (Exception e) {
      // If there's any error parsing the timer operator, return null
      return null;
    }
  }

  /**
   * Creates a task state with reason for task execution
   * @param taskExecution The task execution DTO
   * @return Formatted HTML for task state with reason
   */
  public static String taskStateWithReason(com.leucine.streem.dto.TaskExecutionDto taskExecution) {
    StringBuilder sb = new StringBuilder(TASK_STATE_START);

    // Handle null state for template generation
    if (taskExecution.getState() != null) {
        sb.append(HtmlUtils.htmlEscape(Utility.toDisplayName(taskExecution.getState())));
    } else {
        sb.append("____________________"); // Empty field for template
    }

    sb.append(TASK_STATE_END);
    
    // Add exception reason if task is completed with exception
    if (taskExecution.getState() == State.TaskExecution.COMPLETED_WITH_EXCEPTION) {
        // Always add the reason line, even if reason is null or empty
        sb.append("<em>Reason: ");
        
        // Check if reason exists, otherwise show a default message
        if (taskExecution.getReason() != null && !taskExecution.getReason().isEmpty()) {
            sb.append(HtmlUtils.htmlEscape(taskExecution.getReason()));
        } else {
            sb.append("No reason provided");
        }
        
        sb.append("</em>");
    }
    
    return sb.toString();
  }
    
    /**
     * Creates an annotation box with remarks and optional media list
     * @param remarks The remarks text
     * @param mediaHtml Optional HTML for media list
     * @return Formatted HTML for annotation box
     */
    public static String annotationBox(String remarks, String mediaHtml) {
        StringBuilder sb = new StringBuilder(ANNOTATION_BOX_START);
        sb.append("<strong>Remarks:</strong>&nbsp;")
          .append(remarks == null ? "-" : HtmlUtils.htmlEscape(remarks));
        
        if (mediaHtml != null && !mediaHtml.isEmpty()) {
            sb.append("<br/><strong>Medias:</strong>").append(mediaHtml);
        }
        
        sb.append(ANNOTATION_BOX_END);
        return sb.toString();
    }
    
    /**
     * Creates a media list for annotations
     * @param mediaLinks List of media links
     * @param mediaNames List of media names (can be null, will use links as fallback)
     * @return Formatted HTML for media list
     */
    public static String mediaList(List<String> mediaLinks, List<String> mediaNames) {
        if (mediaLinks == null || mediaLinks.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder("<ul>");
        for (int i = 0; i < mediaLinks.size(); i++) {
            String link = mediaLinks.get(i);
            String name = (mediaNames != null && i < mediaNames.size() && mediaNames.get(i) != null) ? 
                          mediaNames.get(i) : link;
            
            sb.append("<li><a href=\"")
              .append(link)
              .append("\">")
              .append(HtmlUtils.htmlEscape(name))
              .append("</a></li>");
        }
        sb.append("</ul>");
        return sb.toString();
    }
    
    /**
     * Creates a day section for activities
     * @param date The date string
     * @param count The activity count
     * @param activitiesHtml The HTML content for activities
     * @return Formatted HTML for day section
     */
    public static String daySection(String date, int count, String activitiesHtml) {
        return DATE_SECTION_START + 
               HtmlUtils.htmlEscape(date) + " – " + count + " activities" +
               DATE_SECTION_END + 
               ACTIVITIES_LIST_START + 
               activitiesHtml + 
               ACTIVITIES_LIST_END;
    }
    
    /**
     * Creates an activity item
     * @param time The time string
     * @param details The details text
     * @return Formatted HTML for activity item
     */
    public static String activityItem(String time, String details) {
        return String.format(ACTIVITY_ITEM_TEMPLATE, 
                            HtmlUtils.htmlEscape(time), 
                            HtmlUtils.htmlEscape(details));
    }
    
    /**
     * Creates a parameter table
     * @param content The HTML content for the table body
     * @return Formatted HTML for parameter table
     */
    public static String parameterTable(String content) {
        return PARAMETER_TABLE_START + 
               PARAMETER_TABLE_HEADER + 
               "<tbody>" + 
               content + 
               "</tbody>" + 
               PARAMETER_TABLE_END;
    }
    
    /**
     * Creates a task pause details section
     * @param content The HTML content for the pause details
     * @return Formatted HTML for task pause details
     */
    public static String taskPauseDetails(String content) {
        return TASK_PAUSE_DETAILS_START +
               "<h4>Task Pause Details:</h4>" +
               TASK_PAUSE_TABLE_START +
               TASK_PAUSE_TABLE_HEADER +
               "<tbody>" +
               content +
               "</tbody>" +
               TASK_PAUSE_TABLE_END +
               TASK_PAUSE_DETAILS_END;
    }
    
    /**
     * Creates an early start note
     * @param taskNumber The task number
     * @param reason The reason for early start
     * @return Formatted HTML for early start note
     */
    public static String earlyStartNote(String taskNumber, String reason) {
        return EARLY_START_NOTE_START +
          "E " + HtmlUtils.htmlEscape(taskNumber) + ":Early start for scheduled task: " +
               HtmlUtils.htmlEscape(reason) +
               EARLY_START_NOTE_END;
    }
    
    /**
     * Creates a delayed completion note
     * @param reason The reason for delayed completion
     * @return Formatted HTML for delayed completion note
     */
    public static String delayedCompletionNote(String reason) {
        return DELAYED_COMPLETION_NOTE_START +
               "Delayed completion – " + HtmlUtils.htmlEscape(reason) +
               DELAYED_COMPLETION_NOTE_END;
    }

  }
  /**
   * Creates a JPA Specification for JobAudit based on the provided filters string
   * @param filters JSON string containing filter criteria
   * @return Specification for JobAudit
   */
  public static Specification<JobAudit> createJobAuditSpecification(String filters) throws JsonProcessingException {
    if (Utility.isEmpty(filters)) {
      return null;
    }
    
      // Parse the filters JSON string into a SearchFilter object
      SearchFilter searchFilter = JsonUtils.readValue(filters, SearchFilter.class);
      
      // Create a specification based on the search filter
      return (root, query, criteriaBuilder) -> {
        List<Predicate> predicates = new ArrayList<>();
        
        // Process each search criteria in the filter
        for (SearchCriteria criteria : searchFilter.getFields()) {
          String field = criteria.getField();
          String operation = criteria.getOp();
          Object value = criteria.getValue();
          
          // Skip if field, operation, or value is null
          if (field == null || operation == null || value == null) {
            continue;
          }
          
          // Create predicate based on operation type
          switch (operation) {
            case "EQ":
              predicates.add(criteriaBuilder.equal(root.get(field), value));
              break;
            case "NE":
              predicates.add(criteriaBuilder.notEqual(root.get(field), value));
              break;
            case "GT":
              if (value instanceof Number) {
                predicates.add(criteriaBuilder.gt(root.get(field), (Number) value));
              } else if (value instanceof String) {
                  Long longValue = Long.parseLong((String) value);
                  predicates.add(criteriaBuilder.gt(root.get(field), longValue));

              }
              break;
            case "LT":
              if (value instanceof Number) {
                predicates.add(criteriaBuilder.lt(root.get(field), (Number) value));
              } else if (value instanceof String) {
                  Long longValue = Long.parseLong((String) value);
                  predicates.add(criteriaBuilder.lt(root.get(field), longValue));

              }
              break;
            case "GTE":
            case "GOE":
              if (value instanceof Number) {
                predicates.add(criteriaBuilder.ge(root.get(field), (Number) value));
              } else if (value instanceof String) {
                  Long longValue = Long.parseLong((String) value);
                  predicates.add(criteriaBuilder.ge(root.get(field), longValue));

              }
              break;
            case "LTE":
            case "LOE":
              if (value instanceof Number) {
                predicates.add(criteriaBuilder.le(root.get(field), (Number) value));
              } else if (value instanceof String) {
                  Long longValue = Long.parseLong((String) value);
                  predicates.add(criteriaBuilder.le(root.get(field), longValue));

              }
              break;
            case "LIKE":
              if (value instanceof String) {
                predicates.add(criteriaBuilder.like(
                  criteriaBuilder.lower(root.get(field)),
                  "%" + ((String) value).toLowerCase() + "%"
                ));
              }
              break;
            case "ANY":
              if (criteria.getValues() != null && !criteria.getValues().isEmpty()) {
                predicates.add(root.get(field).in(criteria.getValues()));
              }
              break;
            default:
              break;
          }
        }
        
        // Combine predicates based on the operation (AND/OR)
        if (predicates.isEmpty()) {
          return null;
        } else if ("OR".equalsIgnoreCase(searchFilter.getOp())) {
          return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        } else {
          return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        }
      };

  }

  /**
   * Formats filters for display in the PDF
   * @param filters JSON string containing filter criteria
   * @param facilityDto Facility DTO containing date/time format settings
   * @return Formatted HTML for filters section
   */
  public static String formatFiltersForDisplay(String filters, FacilityDto facilityDto) {
    if (Utility.isEmpty(filters)) {
      return null;
    }
    
    try {
      // Parse the filters JSON string into a SearchFilter object
      SearchFilter searchFilter = JsonUtils.readValue(filters, SearchFilter.class);
      
      StringBuilder filtersContent = new StringBuilder();
      
      // Process each search criteria in the filter
      for (int i = 0; i < searchFilter.getFields().size(); i++) {
        SearchCriteria criteria = searchFilter.getFields().get(i);
        String field = criteria.getField();
        String operation = criteria.getOp();
        Object value = criteria.getValue();
        
        // Skip if field, operation, or value is null
        if (field == null || operation == null || value == null) {
          continue;
        }
        
        // Format field name for display
        String displayField = formatFieldName(field);
        
        // Format operation for display
        String displayOperation = formatOperation(operation);
        
        // Format value for display
        String displayValue = formatValue(value, field, facilityDto);
        
        // Add to filters content
        filtersContent.append(HtmlTemplates.tableRow(
          "Filter " + (i + 1),
          displayField + " " + displayOperation + " " + displayValue
        ));
      }
      
      return filtersContent.toString();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Specialized formatter for JobAuditReport "Filters Applied" section.
   * Emits exactly two rows:
   * - End Time: <formatted time or N/A>
   * - Users: <resolved user names or N/A>
   */
  public static String formatJobAuditFiltersForDisplay(String filters, FacilityDto facilityDto, com.leucine.streem.repository.IUserRepository userRepository) {
    if (Utility.isEmpty(filters)) {
      return null;
    }

    try {
      SearchFilter searchFilter = JsonUtils.readValue(filters, SearchFilter.class);

      String usersDisplay = "N/A";
      String dateRangeDisplay = "N/A";

      // Prefer these fields for time label if present
      java.util.Set<String> highPriorityTimeFields = new java.util.HashSet<>(java.util.Arrays.asList(
        "endedAt", "endTime", "endedOn"
      ));

      String startTime = null;
      String endTime = null;
      boolean startFromHighPriority = false;
      boolean endFromHighPriority = false;

      String chosenTime = null;
      boolean chosenFromHighPriority = false;

      for (SearchCriteria criteria : searchFilter.getFields()) {
        String field = criteria.getField();
        String operation = criteria.getOp();

        if (Utility.isEmpty(field) || Utility.isEmpty(operation)) {
          continue;
        }

        // Handle Users
        if (isUserField(field) && userRepository != null) {
          java.util.List<Object> raw = criteria.getValues();
          if (Utility.isEmpty(raw) && criteria.getValue() != null) {
            raw = java.util.List.of(criteria.getValue());
          }
          if (!Utility.isEmpty(raw)) {
            String resolved = formatUserValues(raw, userRepository);
            if (!Utility.isEmpty(resolved)) {
              usersDisplay = resolved;
            }
          }
          continue;
        }

        // Handle Time/Date
        String lower = field.toLowerCase();
        boolean isHighPriorityField = highPriorityTimeFields.contains(field);
        boolean looksLikeTime = lower.contains("time") || lower.contains("date") || lower.contains("at") || isHighPriorityField;

        if (looksLikeTime) {
          String display = null;
          java.util.List<Object> vals = criteria.getValues();
          if (!Utility.isEmpty(vals)) {
            display = formatValuesListForDisplay(vals, field, facilityDto);
          } else if (!Utility.isEmpty(criteria.getValue())) {
            display = formatValue(criteria.getValue(), field, facilityDto);
          }

          if (!Utility.isEmpty(display)) {
            String op = operation.toUpperCase(ROOT);
            if ("GOE".equals(op) || "GTE".equals(op)) {
              if (Utility.isEmpty(startTime) || (isHighPriorityField && !startFromHighPriority)) {
                startTime = display;
                startFromHighPriority = isHighPriorityField;
              }
            } else if ("LOE".equals(op) || "LTE".equals(op)) {
              if (Utility.isEmpty(endTime) || (isHighPriorityField && !endFromHighPriority)) {
                endTime = display;
                endFromHighPriority = isHighPriorityField;
              }
            }

            // Track any time-like value as fallback, prefer high priority
            if (Utility.isEmpty(chosenTime) || (isHighPriorityField && !chosenFromHighPriority)) {
              chosenTime = display;
              chosenFromHighPriority = isHighPriorityField;
            }
          }
        }
      }

      // Format date/time range for display
      if (!Utility.isEmpty(startTime) && !Utility.isEmpty(endTime)) {
        dateRangeDisplay = startTime + " to " + endTime;
      } else if (!Utility.isEmpty(startTime)) {
        dateRangeDisplay = "From " + startTime;
      } else if (!Utility.isEmpty(endTime)) {
        dateRangeDisplay = "Until " + endTime;
      } else if (!Utility.isEmpty(chosenTime)) {
        dateRangeDisplay = chosenTime;
      }

      StringBuilder filtersContent = new StringBuilder();
      filtersContent.append(HtmlTemplates.tableRow("Date/Time Range", Utility.isEmpty(dateRangeDisplay) ? "N/A" : dateRangeDisplay));
      filtersContent.append(HtmlTemplates.tableRow("Users", Utility.isEmpty(usersDisplay) ? "N/A" : usersDisplay));
      return filtersContent.toString();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Overload that can resolve user IDs to user display names for user-related fields.
   * Supports fields like triggeredBy, createdBy.id, startedBy.id, endedBy.id, modifiedBy.id
   * and operations with multiple values (e.g., ANY).
   */
  public static String formatFiltersForDisplay(String filters, FacilityDto facilityDto, com.leucine.streem.repository.IUserRepository userRepository) {
    if (Utility.isEmpty(filters)) {
      return null;
    }

    try {
      SearchFilter searchFilter = JsonUtils.readValue(filters, SearchFilter.class);
      StringBuilder filtersContent = new StringBuilder();

      for (int i = 0; i < searchFilter.getFields().size(); i++) {
        SearchCriteria criteria = searchFilter.getFields().get(i);
        String field = criteria.getField();
        String operation = criteria.getOp();
        List<Object> values = criteria.getValues();

        if (field == null || operation == null || Utility.isEmpty(values)) {
          continue;
        }

        String displayField = formatFieldName(field);
        String displayOperation = formatOperation(operation);

        String displayValue;
        if (isUserField(field) && userRepository != null) {
          displayValue = formatUserValues(values, userRepository);
        } else {
          displayValue = formatValuesListForDisplay(values, field, facilityDto);
        }

        filtersContent.append(HtmlTemplates.tableRow(
          "Filter " + (i + 1),
          displayField + " " + displayOperation + " " + displayValue
        ));
      }

      return filtersContent.toString();
    } catch (Exception e) {
      return null;
    }
  }

  private static boolean isUserField(String field) {
    if (field == null) return false;
    field = field.trim();
    return "triggeredBy".equals(field)
        || "createdBy.id".equals(field)
        || "startedBy.id".equals(field)
        || "endedBy.id".equals(field)
        || "modifiedBy.id".equals(field);
  }

  private static String formatUserValues(List<Object> values, com.leucine.streem.repository.IUserRepository userRepository) {
    if (values == null || values.isEmpty()) return "";

    java.util.Set<Long> ids = new java.util.HashSet<>();
    for (Object v : values) {
      try {
        ids.add(Long.parseLong(v.toString()));
      } catch (Exception ignored) {}
    }

    if (ids.isEmpty()) {
      return values.stream().map(Object::toString).collect(java.util.stream.Collectors.joining(", "));
    }

    java.util.List<com.leucine.streem.model.User> users = userRepository.findAllByIdInAndArchivedFalse(ids);
    java.util.Map<Long, String> idToName = new java.util.HashMap<>();
    for (com.leucine.streem.model.User u : users) {
      idToName.put(u.getId(), Utility.getFullNameAndEmployeeId(u.getFirstName(), u.getLastName(), u.getEmployeeId()));
    }

    java.util.List<String> parts = new java.util.ArrayList<>();
    for (Object v : values) {
      try {
        long id = Long.parseLong(v.toString());
        parts.add(idToName.getOrDefault(id, String.valueOf(id)));
      } catch (Exception e) {
        parts.add(v.toString());
      }
    }
    return String.join(", ", parts);
  }

  private static String formatValuesListForDisplay(List<Object> values, String field, FacilityDto facilityDto) {
    if (values == null || values.isEmpty()) return "";
    java.util.List<String> parts = new java.util.ArrayList<>();
    for (Object v : values) {
      parts.add(formatValue(v, field, facilityDto));
    }
    return String.join(", ", parts);
  }

  /**
   * Formats a field name for display
   * @param field The field name to format
   * @return Formatted field name
   */
  private static String formatFieldName(String field) {
    // Convert camelCase to Title Case with spaces
    if (field == null) return "";
    
    StringBuilder result = new StringBuilder();
    result.append(Character.toUpperCase(field.charAt(0)));
    
    for (int i = 1; i < field.length(); i++) {
      char c = field.charAt(i);
      if (Character.isUpperCase(c)) {
        result.append(' ').append(c);
      } else {
        result.append(c);
      }
    }
    
    return result.toString();
  }

  /**
   * Formats an operation for display
   * @param operation The operation to format
   * @return Formatted operation
   */
  private static String formatOperation(String operation) {
    if (operation == null) return "";

    return switch (operation) {
      case "EQ" -> "equals";
      case "NE" -> "not equals";
      case "GT" -> "greater than";
      case "LT" -> "less than";
      case "GTE", "GOE" -> "greater than or equal to";
      case "LTE", "LOE" -> "less than or equal to";
      case "LIKE" -> "contains";
      case "ANY" -> "is any of";
      default -> operation;
    };
  }

  /**
   * Formats a value for display
   * @param value The value to format
   * @param field The field name (used to determine formatting)
   * @param facilityDto Facility DTO containing date/time format settings
   * @return Formatted value
   */
  private static String formatValue(Object value, String field, FacilityDto facilityDto) {
    if (value == null) return "";
    
    // Check if this is a date/time field
    if (field.toLowerCase().contains("date") || 
        field.toLowerCase().contains("time") ||
        field.toLowerCase().contains("at")) {
        // Try to parse the value as a long (timestamp)
        long timestamp = Long.parseLong(value.toString());
        
        // Apply timezone offset if available
        if (!Utility.isEmpty(facilityDto) && !Utility.isEmpty(facilityDto.getTimeZone())) {
          String timezoneId = facilityDto.getTimeZone();
          String zoneOffsetString = getZoneOffsetString(timezoneId);
          timestamp = DateTimeUtils.addOffSetToTime(timestamp, zoneOffsetString);
        }
        
        // Format based on whether it's a date or date/time
        if (field.toLowerCase().contains("date") && !field.toLowerCase().contains("time")) {
          // Date only
          String dateFormat = !Utility.isEmpty(facilityDto) && !Utility.isEmpty(facilityDto.getDateFormat()) ?
                             facilityDto.getDateFormat() : DateTimeUtils.DEFAULT_DATE_FORMAT;
          return DateTimeUtils.getFormattedDatePattern(timestamp, dateFormat);
        } else {
          // Date and time
          String dateTimeFormat = !Utility.isEmpty(facilityDto) && !Utility.isEmpty(facilityDto.getDateTimeFormat()) ?
                                 facilityDto.getDateTimeFormat() : DateTimeUtils.DEFAULT_DATE_TIME_FORMAT;
          return DateTimeUtils.getFormattedDateTimeOfPattern(timestamp, dateTimeFormat);
        }
    }
    
    return value.toString();
  }

  /**
   * Gets the parameter value as a formatted string based on the parameter type
   */
  public static String getParameterValueAsString(ParameterDto parameter, ParameterValueDto parameterValue) throws JsonProcessingException {
    return getParameterValueAsString(parameter, parameterValue, null);
  }

  /**
   * Gets the parameter value as a formatted string based on the parameter type with facility formatting
   */
  public static String getParameterValueAsString(ParameterDto parameter, ParameterValueDto parameterValue, FacilityDto facility) throws JsonProcessingException {
    if (parameterValue == null) {
      return "____________________";
    }

    String value = "____________________";

    String parameterType = parameter.getType();

    if (parameterType != null) {
      try {
        Type.Parameter type = Type.Parameter.valueOf(parameterType);
        
        if (type == Type.Parameter.RESOURCE || type == Type.Parameter.MULTI_RESOURCE) {
          value = getResourceParameterValue(parameter, parameterValue);
        } else if (type == Type.Parameter.CHECKLIST || type == Type.Parameter.YES_NO ||
                  type == Type.Parameter.MULTISELECT || type == Type.Parameter.SINGLE_SELECT) {
          value = getSelectParameterValue(parameter, parameterValue);
        } else if (type == Type.Parameter.NUMBER || type == Type.Parameter.CALCULATION  || type == Type.Parameter.SINGLE_LINE || type == Type.Parameter.MULTI_LINE) {
          value = parameterValue.getValue();
        } else if (type == Type.Parameter.MATERIAL || type == Type.Parameter.SIGNATURE || 
                  type == Type.Parameter.FILE_UPLOAD || type == Type.Parameter.MEDIA) {
          // Show file label or name as a clickable link, not raw HTML
          List<MediaDto> medias = parameterValue.getMedias();
          if (!Utility.isEmpty(medias)) {
            StringBuilder fileLinks = new StringBuilder();
            for (int i = 0; i < medias.size(); i++) {
              MediaDto media = medias.get(i);
              String displayName = getMediaDisplayName(media);
              String fileLink = wrapWithMediaProxy(media.getLink());
              if (i > 0) fileLinks.append(", ");
              fileLinks.append("<a href=\"")
                .append(fileLink)
                .append("\">")
                .append(HtmlUtils.htmlEscape(displayName))
                .append("</a>");
            }
            value = fileLinks.toString();
          } else {
            value = "-";
          }
        } else if (type == Type.Parameter.DATE) {
          value = getDateParameterValue(parameterValue, facility);
        } else if (type == Type.Parameter.DATE_TIME) {
          value = getDateTimeParameterValue(parameterValue, facility);
        }
      } catch (Exception e) {
        value = "-";
      }
    }

    if (!Utility.isEmpty(value) && (parameter.isAutoInitialized() && !value.equals("-"))) {
      value += " (Auto)";
    }
    if (Utility.isEmpty(value)) {
      value = "____________________";
    }


    return value;
  }

  /**
   * Gets the value of a resource parameter
   */
  public static String getResourceParameterValue(ParameterDto parameter, ParameterValueDto parameterValue) throws JsonProcessingException {
    if (Utility.isEmpty(parameterValue.getChoices())) {
      return "-";
    }

    List<String> selectedNames = new ArrayList<>();

    ObjectMapper objectMapper = new ObjectMapper();
    List<ResourceParameterChoiceDto> choices = objectMapper.readValue(
      parameterValue.getChoices().toString(),
      objectMapper.getTypeFactory().constructCollectionType(List.class, ResourceParameterChoiceDto.class)
    );

    for (ResourceParameterChoiceDto choice : choices) {
      if (!Utility.isEmpty(choice.getObjectDisplayName())) {
        // Include external ID if available
        if (!Utility.isEmpty(choice.getObjectExternalId())) {
          selectedNames.add(choice.getObjectDisplayName() + " (ID: " + choice.getObjectExternalId() + ")");
        } else {
          selectedNames.add(choice.getObjectDisplayName());
        }
      } else {
        selectedNames.add(choice.getObjectId());
      }
    }

    if (!selectedNames.isEmpty()) {
      return String.join(", ", selectedNames);
    }

    return parameterValue.getChoices().toString();
  }

  /**
   * Process a resource node to extract selected resource names
   */
  public static void processResourceNode(JsonNode resourceNode, JsonNode choicesNode, List<String> selectedNames, ObjectMapper objectMapper) {
    Map<String, Object> resource = objectMapper.convertValue(resourceNode, new TypeReference<Map<String, Object>>() {});
    String resourceId = (String) resource.get("id");

    if (resourceId != null) {
      JsonNode status = choicesNode.get(resourceId);
      if (status != null && !Utility.isEmpty(status.asText()) && State.Selection.SELECTED.name().equals(status.asText())) {
        if (status.isObject() && status.has("objectDisplayName")) {
          selectedNames.add(status.get("objectDisplayName").asText());
        } else if (status.isObject() && status.has("displayName")) {
          selectedNames.add(status.get("displayName").asText());
        } else {
          String displayName = null;
          if (resource.containsKey("objectDisplayName")) {
            displayName = (String) resource.get("objectDisplayName");
          } else if (resource.containsKey("displayName")) {
            displayName = (String) resource.get("displayName");
          } else if (resource.containsKey("name")) {
            displayName = (String) resource.get("name");
          }

          if (displayName != null) {
            selectedNames.add(displayName);
          } else {
            selectedNames.add(resourceId);
          }
        }
      }
    }
  }

  /**
   * Gets the value of a select parameter (single select, multiselect, etc.)
   */
  public static String getSelectParameterValue(ParameterDto parameter, ParameterValueDto parameterValue) {
    if (Utility.isEmpty(parameterValue.getChoices())) {
      return "-";
    }

    JsonNode choicesNode = parameterValue.getChoices();
    List<String> selectedNames = new ArrayList<>();

    try {
      Type.Parameter type = Type.Parameter.valueOf(parameter.getType());
      
      // For SINGLE_SELECT, create simple text-based checkboxes for all options
      if (type == Type.Parameter.SINGLE_SELECT) {
        StringBuilder textBuilder = new StringBuilder();
        
        // Get all options from parameter data
        JsonNode optionsData = parameter.getData();
        if (optionsData != null && optionsData.isArray()) {
          for (JsonNode option : optionsData) {
            String optionId = option.get("id").asText();
            String optionName = option.get("name").asText();
            
            // Check if this option is selected
            boolean isSelected = false;
            if (choicesNode.has(optionId) && 
                State.Selection.SELECTED.name().equals(choicesNode.get(optionId).asText())) {
              isSelected = true;
              selectedNames.add(optionName); // Still collect selected names for other uses
            }
            
            // Add option with radio button format
            if (textBuilder.length() > 0) {
              textBuilder.append("\n");
            }
            textBuilder.append(isSelected ? SELECTED_RADIO : UNSELECTED_RADIO)
              .append(" ").append(optionName);
          }
        }
        
        return textBuilder.toString();
      } 
      // For CHECKLIST, add "[ ] " at the start of each option
      else if (type == Type.Parameter.CHECKLIST) {
        StringBuilder textBuilder = new StringBuilder();
        
        // Get all options from parameter data
        JsonNode optionsData = parameter.getData();
        if (optionsData != null && optionsData.isArray()) {
          for (JsonNode option : optionsData) {
            String optionId = option.get("id").asText();
            String optionName = option.get("name").asText();
            
            // Check if this option is selected
            boolean isSelected = false;
            if (choicesNode.has(optionId) && 
                State.Selection.SELECTED.name().equals(choicesNode.get(optionId).asText())) {
              isSelected = true;
              selectedNames.add(optionName); // Still collect selected names for other uses
            }
            
            // Add option with square bracket checkbox
            if (textBuilder.length() > 0) {
              textBuilder.append("\n");
            }
            textBuilder.append(isSelected ? SELECTED_CHECKBOX : UNSELECTED_CHECKBOX)
              .append(" ").append(optionName);
          }
        }
        
        return textBuilder.toString();
      }
      // For YES_NO, show both options with appropriate selection indicators
      else if (type == Type.Parameter.YES_NO) {
        StringBuilder textBuilder = new StringBuilder();
        
        boolean yesSelected = false;
        boolean noSelected = false;
        String noReason = null;
        String yesOptionName = "Yes";
        String noOptionName = "No";
        
        // Get all options from parameter data and check their selection status
        JsonNode optionsData = parameter.getData();
        if (optionsData != null && optionsData.isArray()) {
          for (JsonNode option : optionsData) {
            String optionId = option.get("id").asText();
            String optionName = option.get("name").asText();
            String optionType = option.get("type").asText();
            
            // Check if this option is selected
            if (choicesNode.has(optionId) && 
                State.Selection.SELECTED.name().equals(choicesNode.get(optionId).asText())) {
              
              if ("yes".equals(optionType)) {
                yesSelected = true;
                yesOptionName = optionName; // Use actual name from data (e.g., "Y")
                selectedNames.add(optionName);
              } else if ("no".equals(optionType)) {
                noSelected = true;
                noOptionName = optionName; // Use actual name from data (e.g., "No")
                selectedNames.add(optionName);
                
                // Extract reason for "No" selection if available
                if (parameterValue.getReason() != null && !parameterValue.getReason().isEmpty()) {
                  noReason = parameterValue.getReason();
                }
              }
            }
          }
        }
        
        // Add Yes option
        textBuilder.append(yesSelected ? SELECTED_RADIO : UNSELECTED_RADIO)
          .append(" ").append(yesOptionName);
        
        // Add No option with reason if available
        textBuilder.append("\n")
          .append(noSelected ? SELECTED_RADIO : UNSELECTED_RADIO)
          .append(" ").append(noOptionName);
        
        // Add reason for No selection if available
        if (noSelected && noReason != null && !noReason.trim().isEmpty()) {
          textBuilder.append(" - ").append(noReason.trim());
        }
        
        return textBuilder.toString();
      }
      // For MULTISELECT, show all options with radio button format
      else if (type == Type.Parameter.MULTISELECT) {
        StringBuilder textBuilder = new StringBuilder();
        
        // Get all options from parameter data
        JsonNode optionsData = parameter.getData();
        if (optionsData != null && optionsData.isArray()) {
          for (JsonNode option : optionsData) {
            String optionId = option.get("id").asText();
            String optionName = option.get("name").asText();
            
            // Check if this option is selected
            boolean isSelected = false;
            if (choicesNode.has(optionId) && 
                State.Selection.SELECTED.name().equals(choicesNode.get(optionId).asText())) {
              isSelected = true;
              selectedNames.add(optionName); // Still collect selected names for other uses
            }
            
            // Add option with radio button format
            if (textBuilder.length() > 0) {
              textBuilder.append("\n");
            }
            textBuilder.append(isSelected ? SELECTED_RADIO : UNSELECTED_RADIO)
              .append(" ").append(optionName);
          }
        }
        
        return textBuilder.toString();
      }
      // For other select types
      else {
        if (!Utility.isEmpty(parameter.getData())) {
          ObjectMapper objectMapper = new ObjectMapper();
          if (parameter.getData().isArray()) {
            for (JsonNode optionNode : parameter.getData()) {
              extractSelectedOption(optionNode, choicesNode, selectedNames, objectMapper);
            }
          } else {
            extractSelectedOption(parameter.getData(), choicesNode, selectedNames, objectMapper);
          }
        }

        if (!selectedNames.isEmpty()) {
          return String.join(", ", selectedNames);
        }

        return choicesNode.toString();
      }
    } catch (Exception e) {
      // Fallback to original implementation if there's an error
      if (!Utility.isEmpty(parameter.getData())) {
        ObjectMapper objectMapper = new ObjectMapper();
        if (parameter.getType().equals("CHECKLIST")) {
          extractChecklistSelectedOptions(parameter.getData(), choicesNode, selectedNames, objectMapper);
        } else {
          if (parameter.getData().isArray()) {
            for (JsonNode optionNode : parameter.getData()) {
              extractSelectedOption(optionNode, choicesNode, selectedNames, objectMapper);
            }
          } else {
            extractSelectedOption(parameter.getData(), choicesNode, selectedNames, objectMapper);
          }
        }
      }

      if (!selectedNames.isEmpty()) {
        return String.join(", ", selectedNames);
      }

      return choicesNode.toString();
    }
  }

  /**
   * Extract selected options from a checklist parameter
   */
  public static void extractChecklistSelectedOptions(JsonNode data, JsonNode choicesNode, List<String> selectedNames, ObjectMapper objectMapper) {
      choicesNode.fieldNames().forEachRemaining(choiceId -> {
        JsonNode status = choicesNode.get(choiceId);
        if (status != null && !Utility.isEmpty(status.asText()) && State.Selection.SELECTED.name().equals(status.asText())) {
          // Find the corresponding option in the data
          if (data.isArray()) {
            for (JsonNode optionNode : data) {
              String id = optionNode.get("id").asText();
              if (id.equals(choiceId)) {
                String name = optionNode.get("name").asText();
                selectedNames.add(name);
                break;
              }
            }
          }
        }
      });
  }

  /**
   * Extract a selected option from an option node
   */
  public static void extractSelectedOption(JsonNode optionNode, JsonNode choicesNode, List<String> selectedNames, ObjectMapper objectMapper) {
      Map<String, Object> option = objectMapper.convertValue(optionNode, new TypeReference<Map<String, Object>>() {});
      String optionId = (String) option.get("id");
      String optionName = (String) option.get("name");

      if (optionId != null && optionName != null) {
        JsonNode status = choicesNode.get(optionId);
        if (status != null && !Utility.isEmpty(status.asText()) && State.Selection.SELECTED.name().equals(status.asText())) {
          selectedNames.add(optionName);
        }
      }

  }


  /**
   * Gets enhanced attribute label for calculation parameters showing variables with values and expression
   */
  public static String getCalculationParameterAttributeLabel(ParameterDto parameter, List<ParameterDto> allTaskParameters) {
    if (!Type.Parameter.CALCULATION.toString().equals(parameter.getType()) || parameter.getData() == null) {
      return parameter.getLabel();
    }

    try {
      ObjectMapper objectMapper = new ObjectMapper();
      com.leucine.streem.model.helper.parameter.CalculationParameter calcParam = 
          objectMapper.convertValue(parameter.getData(), com.leucine.streem.model.helper.parameter.CalculationParameter.class);

      StringBuilder attributeLabel = new StringBuilder();
      attributeLabel.append(parameter.getLabel());

      // Add variables with values using DTO
      if (calcParam.getVariables() != null && !calcParam.getVariables().isEmpty()) {
        StringBuilder variablesText = new StringBuilder();
        
        // Get variable values from parameter response choices if available
        Map<String, String> variableValues = extractVariableValuesFromChoices(parameter);
        
        for (Map.Entry<String, com.leucine.streem.model.helper.parameter.CalculationParameterVariable> entry : calcParam.getVariables().entrySet()) {
          String variableName = entry.getKey();
          com.leucine.streem.model.helper.parameter.CalculationParameterVariable variable = entry.getValue();
          
          // Find the parameter name by ID
          String parameterName = findParameterNameById(variable.getParameterId(), allTaskParameters);
          
          // Get the variable value from choices
          String variableValue = variableValues.get(variable.getParameterId());
          
          if (variablesText.length() > 0) {
            variablesText.append(", ");
          }
          
          // Format: A = Number 1 = 3
          variablesText.append(variableName).append(" = ").append(parameterName);
          if (variableValue != null) {
            variablesText.append(" = ").append(variableValue);
          }
        }
        
        if (variablesText.length() > 0) {
          attributeLabel.append("<br>Variables: ").append(variablesText.toString());
        }
      }

      // Add output (changed from expression) using DTO
      if (!Utility.isEmpty(calcParam.getExpression())) {
        attributeLabel.append("<br>Output: ").append(calcParam.getExpression());
      }

      return attributeLabel.toString();
    } catch (Exception e) {
      // Fallback to simple label if parsing fails
      return parameter.getLabel();
    }
  }

  /**
   * Extracts variable values from calculation parameter choices
   */
  private static Map<String, String> extractVariableValuesFromChoices(ParameterDto parameter) {
    Map<String, String> variableValues = new HashMap<>();
    
    try {
      if (parameter.getResponse() != null && !parameter.getResponse().isEmpty()) {
        ParameterValueDto paramValue = parameter.getResponse().get(0);
        if (paramValue != null && paramValue.getChoices() != null) {
          ObjectMapper objectMapper = new ObjectMapper();
          
          // Parse choices as array of variable value objects
          if (paramValue.getChoices().isArray()) {
            for (JsonNode choiceNode : paramValue.getChoices()) {
              String parameterId = choiceNode.get("parameterId").asText();
              String value = choiceNode.get("value").asText();
              variableValues.put(parameterId, value);
            }
          }
        }
      }
    } catch (Exception e) {
      // If parsing fails, return empty map
    }
    
    return variableValues;
  }

  /**
   * Helper method to find parameter name by ID from a list of parameters
   */
  private static String findParameterNameById(String parameterId, List<ParameterDto> parameters) {
    if (parameters == null || parameterId == null) {
      return "Unknown Parameter";
    }
    
    for (ParameterDto param : parameters) {
      if (parameterId.equals(param.getId())) {
        return param.getLabel();
      }
    }
    
    return "Parameter ID: " + parameterId;
  }

  /**
   * Gets the value of a date parameter with proper formatting
   */
  public static String getDateParameterValue(ParameterValueDto parameterValue, FacilityDto facility) {
    if (Utility.isEmpty(parameterValue.getValue())) {
      return "-";
    }

    try {
      long timestamp = Long.parseLong(parameterValue.getValue());
      
      // Apply timezone offset if facility timezone is available
      if (facility != null && facility.getTimeZone() != null) {
        String zoneOffsetString = getZoneOffsetString(facility.getTimeZone());
        timestamp = DateTimeUtils.addOffSetToTime(timestamp, zoneOffsetString);
      }
      
      // Use facility date format or default
      String dateFormat = (facility != null && facility.getDateFormat() != null) ? 
                         facility.getDateFormat() : DateTimeUtils.DEFAULT_DATE_FORMAT;
      
      return DateTimeUtils.getFormattedDatePattern(timestamp, dateFormat);
    } catch (NumberFormatException e) {
      // If not a valid timestamp, return the original value
      return parameterValue.getValue();
    }
  }

  /**
   * Gets the value of a date-time parameter with proper formatting
   */
  public static String getDateTimeParameterValue(ParameterValueDto parameterValue, FacilityDto facility) {
    if (Utility.isEmpty(parameterValue.getValue())) {
      return "-";
    }

      long timestamp = Long.parseLong(parameterValue.getValue());
      
      // Apply timezone offset if facility timezone is available
      if (facility != null && facility.getTimeZone() != null) {
        String zoneOffsetString = getZoneOffsetString(facility.getTimeZone());
        timestamp = DateTimeUtils.addOffSetToTime(timestamp, zoneOffsetString);
      }
      
      // Use facility datetime format or default
      String dateTimeFormat = (facility != null && facility.getDateTimeFormat() != null) ? 
                             facility.getDateTimeFormat() : DateTimeUtils.DEFAULT_DATE_TIME_FORMAT;
      
      return DateTimeUtils.getFormattedDateTimeOfPattern(timestamp, dateTimeFormat);
  }


  public static String getFileParameterValue(ParameterDto parameter, ParameterValueDto parameterValue) {
    if (!Utility.isEmpty(parameterValue.getMedias())) {
      return formatCweDocuments(parameterValue.getMedias());
    }
    return "____________________";
  }

  /**
   * Wraps a media URL with the media proxy service
   * Transforms URLs like https://api.demo.platform.leucinetech.com/... 
   * to https://demo.platform.leucinetech.com/media?link=...
   * 
   * @param originalUrl The original media URL
   * @return The wrapped proxy URL, or original URL if transformation fails
   */
  private static String wrapWithMediaProxy(String originalUrl) {
    if (originalUrl == null || originalUrl.isEmpty()) {
      return originalUrl;
    }
    
    try {
      URL url = new URL(originalUrl);
      String host = url.getHost();
      
      // Check if this is an API URL that should be proxied
      if (host != null && host.startsWith("api.")) {
        // Remove "api." prefix to get the FQDN
        String fqdn = host.substring(4);
        
        // URL encode the original link for the query parameter
        String encodedUrl = URLEncoder.encode(originalUrl, StandardCharsets.UTF_8);
        
        // Return the proxy URL
        return "https://" + fqdn + "/media?link=" + encodedUrl;
      }
    } catch (Exception e) {
      // If anything goes wrong, return original URL
    }
    
    return originalUrl;
  }

  /**
   * Gets the best display name for a media file
   * Uses the exact same logic as job annotations for consistency
   */
  private static String getMediaDisplayName(MediaDto media) {
    // Try different field priorities for file parameters
    if (!Utility.isEmpty(media.getName())) {
      return media.getName();
    }
    
    if (!Utility.isEmpty(media.getOriginalFilename())) {
      return media.getOriginalFilename();
    }
    
    if (!Utility.isEmpty(media.getDescription())) {
      return media.getDescription();
    }
    
    // Extract filename from URL as last resort
    if (!Utility.isEmpty(media.getLink())) {
      String link = media.getLink();
      int lastSlash = link.lastIndexOf('/');
      if (lastSlash >= 0 && lastSlash < link.length() - 1) {
        String filename = link.substring(lastSlash + 1);
        // Remove query parameters if any
        int queryIndex = filename.indexOf('?');
        if (queryIndex > 0) {
          filename = filename.substring(0, queryIndex);
        }
        return filename;
      }
    }
    
    return "File";
  }

  /**
   * Formats CWE documents as hyperlinks for job exception details
   * @param medias List of MediaDto objects containing document information
   * @return Formatted HTML string with all documents as hyperlinks
   */
  public static String formatCweDocuments(List<MediaDto> medias) {
    if (medias == null || medias.isEmpty()) {
      return "-";
    }
    
    StringBuilder documentsHtml = new StringBuilder();
    
    for (int i = 0; i < medias.size(); i++) {
      MediaDto media = medias.get(i);
      String displayName = getMediaDisplayName(media);
      String fileLink = wrapWithMediaProxy(media.getLink());
      
      if (i > 0) {
        documentsHtml.append(", ");
      }
      
      // Create hyperlink: <a href="link">displayName</a>
      documentsHtml.append("<a href=\"")
              .append(fileLink)
              .append("\">")
              .append(HtmlUtils.htmlEscape(displayName))
              .append("</a>");
    }
    
    return documentsHtml.toString();
  }

  /**
   * Formats job annotation media as hyperlinks for consistent display
   * @param medias List of MediaDto objects containing document information
   * @return Formatted HTML string with all documents as hyperlinks
   */
  public static String formatJobAnnotationMedia(List<MediaDto> medias) {
    if (medias == null || medias.isEmpty()) {
      return "-";
    }
    
    StringBuilder mediaHtml = new StringBuilder();
    
    for (int i = 0; i < medias.size(); i++) {
      MediaDto media = medias.get(i);
      String displayName = getMediaDisplayName(media);
      String fileLink = wrapWithMediaProxy(media.getLink());
      
      if (i > 0) {
        mediaHtml.append(", ");
      }
      
      // Create hyperlink: <a href="link">displayName</a>
      mediaHtml.append("<a href=\"")
              .append(fileLink)
              .append("\">")
              .append(HtmlUtils.htmlEscape(displayName))
              .append("</a>");
    }
    
    return mediaHtml.toString();
  }

  /**
   * Gets the display name for a resource from its identifier value
   * @param identifierValue The identifier value to look for
   * @param resourceParameters The map of resource parameters to search in
   * @param mongoTemplate The MongoTemplate to use for database queries (not used in this implementation)
   * @return The formatted display name and ID, or the identifier value if not found
   */
  public static String getResourceDisplayNameFromIdentifier(String identifierValue, Map<String, JobLogResource> resourceParameters, MongoTemplate mongoTemplate) {
    if (identifierValue == null) {
      return "-";
    }

    if (resourceParameters != null && !resourceParameters.isEmpty()) {
      for (Map.Entry<String, JobLogResource> entry : resourceParameters.entrySet()) {
        JobLogResource resource = entry.getValue();

        if (resource.getChoices() != null && !resource.getChoices().isEmpty()) {
          for (ResourceParameterChoiceDto choice : resource.getChoices()) {
            if (identifierValue.equals(choice.getObjectId())) {
              return choice.getObjectDisplayName() + " (ID: " + choice.getObjectExternalId() + ")";
            }
          }
        }
      }
    }

    return identifierValue;
  }

  /**
   * Overloaded method for backward compatibility
   */
  public static String getResourceDisplayNameFromIdentifier(String identifierValue, Map<String, JobLogResource> resourceParameters) {
    return getResourceDisplayNameFromIdentifier(identifierValue, resourceParameters, null);
  }
  
  /**
   * Format a cell value based on column type, similar to setCellValue in JobLogService
   * 
   * @param columnType The type of column (DATE, DATE_TIME, TEXT, etc.)
   * @param value The value to format
   * @param identifierValue The identifier value (for resource parameters)
   * @param resourceParameters Resource parameters map (for resource parameters)
   * @param resourceParameterChoiceDtoMap Global map of resource parameter choices
   * @param timezoneOffset The timezone offset to apply
   * @param dateTimeFormat The date/time format to use
   * @param dateFormat The date format to use
   * @return The formatted value as a string
   */
  /**
   * Convert a timezone ID (like "Asia/Kolkata") to a ZoneOffset string (like "+05:30")
   * 
   * @param timezoneId The timezone ID to convert
   * @return The ZoneOffset string
   */
  public static String getZoneOffsetString(String timezoneId) {
      if (timezoneId.startsWith("+") || timezoneId.startsWith("-")) {
        return timezoneId;
      }
      
      ZoneId zoneId = ZoneId.of(timezoneId);
      ZonedDateTime now = ZonedDateTime.now(zoneId);
      return now.getOffset().getId();
  }
  
  /**
   * Renders tasks as columns in a single table (for RECURRING tasks)
   * @param stage The stage containing the tasks
   * @param tasks List of related tasks to render
   * @return The formatted HTML for the table
   */
  /**
   * Renders a consolidated pause/resume table for all tasks in a stage that have pause/resume history
   * @param stage The stage containing the tasks
   * @param tasks List of tasks to check for pause/resume history
   * @return The formatted HTML for the consolidated pause/resume table, or empty string if no tasks have pause/resume history
   */
  public static String renderStagePauseResumeTables(StageDto stage,
                                                  List<TaskDto> tasks,
                                                  FacilityDto facilityDto) {
    List<TaskDto> tasksWithPauseResume = new ArrayList<>();
    
    for (TaskDto task : tasks) {
      if (!Utility.isEmpty(task.getTaskExecutions())) {
        boolean hasAnyPauseResumeAudits = task.getTaskExecutions().stream()
          .anyMatch(ex -> !Utility.isEmpty(ex.getTaskPauseResumeAudits()));
        if (hasAnyPauseResumeAudits) {
          tasksWithPauseResume.add(task);
        }
      }
    }
    
    // If no tasks have pause/resume audits, return empty string
    if (tasksWithPauseResume.isEmpty()) {
      return "";
    }
    
    StringBuilder sb = new StringBuilder();
    
    // Create table without any wrapper or heading
    sb.append("<table class=\"task-pause-resume-table\">")
      .append("<thead><tr>")
      .append("<th>Task</th>")
      .append("<th>Paused At</th>")
      .append("<th>Paused By</th>")
      .append("<th>Resumed At</th>")
      .append("<th>Resumed By</th>")
      .append("<th>Reason For Pause</th>")
      .append("</tr></thead><tbody>");
    
    // Add rows for each task with pause/resume audits
    for (TaskDto task : tasksWithPauseResume) {
      if (Utility.isEmpty(task.getTaskExecutions())) {
        continue;
      }
      for (TaskExecutionDto taskExecution : task.getTaskExecutions()) {
        if (Utility.isEmpty(taskExecution.getTaskPauseResumeAudits())) {
          continue;
        }

        String baseTaskNumber = stage.getOrderTree() + "." + task.getOrderTree();
        String taskNumber = baseTaskNumber;

        // Append execution-specific numbering for REPEAT and RECURRING executions
        if (taskExecution.getType() == Type.TaskExecutionType.REPEAT || taskExecution.getType() == Type.TaskExecutionType.RECURRING) {
          if (taskExecution.getOrderTree() != null) {
            taskNumber = baseTaskNumber + "." + (taskExecution.getOrderTree() - 1);
          }
        }

        for (TaskPauseResumeAuditDto audit : taskExecution.getTaskPauseResumeAudits()) {
          String pausedAt = !Utility.isEmpty(audit.getPausedAt()) ?
            formatTimeWithFacility(audit.getPausedAt(), facilityDto) : "-";
          
          String pausedBy = !Utility.isEmpty(audit.getPausedBy()) ?
            Utility.getFullNameAndEmployeeId(audit.getPausedBy().getFirstName(), audit.getPausedBy().getLastName(), audit.getPausedBy().getEmployeeId()) : "-";
          
          String resumedAt = audit.getResumedAt() != null ? 
            formatTimeWithFacility(audit.getResumedAt(), facilityDto) : "-";
          
          String resumedBy = audit.getResumedBy() != null ?
            Utility.getFullNameAndEmployeeId(audit.getResumedBy().getFirstName(), audit.getResumedBy().getLastName(), audit.getResumedBy().getEmployeeId()): "-";

          String reason = !Utility.isEmpty(audit.getReason()) ?
            audit.getReason().toString() : "-";
          
          if (audit.getComment() != null && !audit.getComment().isEmpty()) {
            reason += ": " + audit.getComment();
          }
          
          sb.append("<tr>")
            .append("<td>").append("Task ").append(HtmlUtils.htmlEscape(taskNumber)).append("</td>")
            .append("<td>").append(HtmlUtils.htmlEscape(pausedAt)).append("</td>")
            .append("<td>").append(HtmlUtils.htmlEscape(pausedBy)).append("</td>")
            .append("<td>").append(HtmlUtils.htmlEscape(resumedAt)).append("</td>")
            .append("<td>").append(HtmlUtils.htmlEscape(resumedBy)).append("</td>")
            .append("<td>").append(HtmlUtils.htmlEscape(reason)).append("</td>")
            .append("</tr>");
        }
      }
    }
    
    sb.append("</tbody></table>");
    
    return sb.toString();
  }

    // Helper to format a timestamp according to facility timezone and format.
    public static String formatTimeWithFacility(Long timestamp, FacilityDto facilityDto) {
        if (timestamp == null) return "-";
        String zoneOffsetString = null;
        String dateTimeFormat = DateTimeUtils.DEFAULT_DATE_TIME_FORMAT;
        if (facilityDto != null) {
            if (facilityDto.getTimeZone() != null) {
                zoneOffsetString = getZoneOffsetString(facilityDto.getTimeZone());
            }
            if (facilityDto.getDateTimeFormat() != null) {
                dateTimeFormat = facilityDto.getDateTimeFormat();
            }
        }
        long adjusted = (zoneOffsetString != null)
            ? DateTimeUtils.addOffSetToTime(timestamp, zoneOffsetString)
            : timestamp;
        return DateTimeUtils.getFormattedDateTimeOfPattern(adjusted, dateTimeFormat);
    }

    public static void renderCorrectionTableWithColumn(StringBuilder sb, ParameterDto param, ParameterValueDto paramValue, int columnIndex, FacilityDto facilityDto) throws JsonProcessingException {
        CorrectionDto correction = paramValue.getCorrection();
        if (correction == null) return;
        
        // Create a header for the correction table with column identification
        sb.append("<h5 style=\"margin-top: 15px; margin-bottom: 5px;\">Correction Details of ")
          .append(HtmlUtils.htmlEscape(param.getLabel()))
          .append(" (C").append(columnIndex).append(")")
          .append("</h5>");
        
        // Start the correction table
        sb.append("<table class=\"parameter-table\" style=\"margin-bottom: 20px;\">")
          .append("<thead><tr>")
          .append("<th>Attribute</th><th>Values</th><th>Details</th>")
          .append("</tr></thead><tbody>");
        
        // Add correction status
        sb.append("<tr>")
          .append("<td>Correction Status</td>")
          .append("<td>").append(HtmlUtils.htmlEscape(correction.getStatus() != null ? correction.getStatus() : "-")).append("</td>")
          .append("<td>-</td>")
          .append("</tr>");
        
    // Add corrected value
    String oldValue = correction.getOldValue() != null ? correction.getOldValue() : "-";
    String newValue = correction.getNewValue() != null ? correction.getNewValue() : "-";
    // Format old/new values for date/datetime/resource parameters
    if (param.getType() != null) {
      Type.Parameter type = Type.Parameter.valueOf(param.getType());
      if ((type == Type.Parameter.RESOURCE || type == Type.Parameter.MULTI_RESOURCE)) {
        // Use oldChoices/newChoices for resource correction display
        List<ResourceParameterChoiceDto> oldResourceChoices = Collections.emptyList();
        List<ResourceParameterChoiceDto> newResourceChoices = Collections.emptyList();
        if (correction.getOldChoices() != null) {
          oldResourceChoices = JsonUtils.readValue(
            correction.getOldChoices().toString(),
            new TypeReference<List<ResourceParameterChoiceDto>>() {}
          );
        }
        if (correction.getNewChoices() != null) {
          newResourceChoices = JsonUtils.readValue(
            correction.getNewChoices().toString(),
            new TypeReference<List<ResourceParameterChoiceDto>>() {}
          );
        }
        oldValue = formatResourceChoicesForCorrection(oldResourceChoices);
        newValue = formatResourceChoicesForCorrection(newResourceChoices);
      } else if (type == Type.Parameter.MEDIA || type == Type.Parameter.FILE_UPLOAD || type == Type.Parameter.SIGNATURE) {
        List<MediaDto> oldMedias = correction.getOldMedias();
        List<MediaDto> newMedias = correction.getNewMedias();

        if (Utility.isEmpty(oldMedias)  && !Utility.isEmpty(correction.getOldChoices())) {
          oldMedias = JsonUtils.readValue(
            correction.getOldChoices().toString(),
            new TypeReference<List<MediaDto>>() {}
          );
        }
        if (Utility.isEmpty(newMedias) && !Utility.isEmpty(correction.getNewChoices())) {
          newMedias = JsonUtils.readValue(
            correction.getNewChoices().toString(),
            new TypeReference<List<MediaDto>>() {}
          );
        }

        oldValue = formatJobAnnotationMedia(oldMedias);
        newValue = formatJobAnnotationMedia(newMedias);
      } else if ((type == Type.Parameter.DATE) && !"-".equals(oldValue)) {
        ParameterValueDto temp = new ParameterValueDto();
        temp.setValue(oldValue);
        oldValue = getDateParameterValue(temp, facilityDto);
      } else if (type == Type.Parameter.DATE_TIME && !"-".equals(oldValue)) {
        ParameterValueDto temp = new ParameterValueDto();
        temp.setValue(oldValue);
        oldValue = getDateTimeParameterValue(temp, facilityDto);
      }
      if (type == Type.Parameter.DATE && !"-".equals(newValue)) {
        ParameterValueDto temp = new ParameterValueDto();
        temp.setValue(newValue);
        newValue = getDateParameterValue(temp, facilityDto);
      } else if (type == Type.Parameter.DATE_TIME && !"-".equals(newValue)) {
        ParameterValueDto temp = new ParameterValueDto();
        temp.setValue(newValue);
        newValue = getDateTimeParameterValue(temp, facilityDto);
      }
    }

    // Build details for each row
    String lastUpdatedInfo = "";
    if (correction.getCreatedAt() != null && correction.getCreatedBy() != null) {
      lastUpdatedInfo = "Last updated by " +
        correction.getCreatedBy().getFirstName() + " " +
        correction.getCreatedBy().getLastName() + " (ID: " +
        correction.getCreatedBy().getEmployeeId() + ") at " +
        formatTimeWithFacility(correction.getCreatedAt(), facilityDto) +
        " from " + oldValue + ".";
    }

    String initiatorInfo = "";
    if (correction.getCreatedAt() != null && correction.getCreatedBy() != null) {
      initiatorInfo = "Initiated at " +
        formatTimeWithFacility(correction.getCreatedAt(), facilityDto) +
        " by " +
        correction.getCreatedBy().getFirstName() + " " +
        correction.getCreatedBy().getLastName() + " (ID: " +
        correction.getCreatedBy().getEmployeeId() + ").";
    }

    String correctorInfo = "";
    if (correction.getCorrector() != null && !correction.getCorrector().isEmpty()) {
      for (com.leucine.streem.dto.CorrectorDto corrector : correction.getCorrector()) {
        if (corrector.getUser() != null) {
          correctorInfo = "Corrected at ";
          if (corrector.getModifiedAt() != null) {
            correctorInfo += formatTimeWithFacility(corrector.getModifiedAt(), facilityDto) + " ";
          }
          correctorInfo += "by " +
            corrector.getUser().getFirstName() + " " +
            corrector.getUser().getLastName() + " (ID: " +
            corrector.getUser().getEmployeeId() + ").";
          break; // Just show the first corrector
        }
      }
    }

    String reviewerInfo = "";
    if (correction.getReviewer() != null && !correction.getReviewer().isEmpty()) {
      for (com.leucine.streem.dto.ReviewerDto reviewer : correction.getReviewer()) {
        if (reviewer.getUser() != null) {
          reviewerInfo = "Reviewed at ";
          if (reviewer.getModifiedAt() != null) {
            reviewerInfo += formatTimeWithFacility(reviewer.getModifiedAt(), facilityDto) + " ";
          }
          reviewerInfo += "by " +
            reviewer.getUser().getFirstName() + " " +
            reviewer.getUser().getLastName() + " (ID: " +
            reviewer.getUser().getEmployeeId() + ")";
          break; // Just show the first reviewer
        }
      }
    }
    if (Utility.isEmpty(reviewerInfo)  || reviewerInfo.trim().isEmpty()) {
      reviewerInfo = "____________________";
    }

    String newValueCell = (!Utility.isEmpty(newValue) && newValue.contains("<a href="))
      ? newValue
      : HtmlUtils.htmlEscape(newValue);
    sb.append("<tr>")
      .append("<td>").append(HtmlUtils.htmlEscape(param.getLabel())).append(" - Corrected Value</td>")
      .append("<td>").append(newValueCell).append("</td>")
      .append("<td>").append(HtmlUtils.htmlEscape(lastUpdatedInfo.trim())).append("</td>")
      .append("</tr>");

    sb.append("<tr>")
      .append("<td>Initiator Remarks</td>")
      .append("<td>").append(HtmlUtils.htmlEscape(!Utility.isEmpty(correction.getInitiatorsReason()) ? correction.getInitiatorsReason() : "____________________")).append("</td>")
      .append("<td>").append(HtmlUtils.htmlEscape(initiatorInfo.trim().isEmpty() ? "____________________" : initiatorInfo.trim())).append("</td>")
      .append("</tr>");

    String status = !Utility.isEmpty(correction.getStatus()) ? correction.getStatus() : "";
    if (!State.Correction.INITIATED.toString().equalsIgnoreCase(status)) {
      sb.append("<tr>")
        .append("<td>Corrector Remarks</td>")
        .append("<td>").append(HtmlUtils.htmlEscape(!Utility.isEmpty(correction.getCorrectorsReason()) ? correction.getCorrectorsReason() : "____________________")).append("</td>")
        .append("<td>").append(HtmlUtils.htmlEscape(correctorInfo.trim().isEmpty() ? "____________________" : correctorInfo.trim())).append("</td>")
        .append("</tr>");
    } else {
      sb.append("<tr>")
        .append("<td>Corrector Remarks</td>")
        .append("<td>____________________</td>")
        .append("<td>____________________</td>")
        .append("</tr>");
    }

    // Reviewer Remarks row (always show)
    if (State.Correction.ACCEPTED.toString().equalsIgnoreCase(status) ||
        State.Correction.REJECTED.toString().equalsIgnoreCase(status) ||
        State.Correction.RECALLED.toString().equalsIgnoreCase(status)) {
      sb.append("<tr>")
        .append("<td>Reviewer Remarks</td>")
        .append("<td>").append(HtmlUtils.htmlEscape(correction.getReviewersReason() != null ? correction.getReviewersReason() : "____________________")).append("</td>")
        .append("<td>").append(HtmlUtils.htmlEscape(reviewerInfo.trim().isEmpty() ? "____________________" : reviewerInfo.trim())).append("</td>")
        .append("</tr>");
    } else {
      sb.append("<tr>")
        .append("<td>Reviewer Remarks</td>")
        .append("<td>____________________</td>")
        .append("<td>____________________</td>")
        .append("</tr>");
    }
        
        // Close the table
        sb.append("</tbody></table>");
    }

  /**
   * Renders a correction table for a parameter (legacy method without column identification)
   * @param sb StringBuilder to append to
   * @param param The parameter with correction
   * @param paramValue The parameter value containing the correction
   */
  public static void renderCorrectionTable(StringBuilder sb, com.leucine.streem.dto.ParameterDto param, com.leucine.streem.dto.ParameterValueDto paramValue, FacilityDto facilityDto) throws JsonProcessingException {
    com.leucine.streem.dto.CorrectionDto correction = paramValue.getCorrection();
    if (correction == null) return;
    
    // Create a header for the correction table
    sb.append("<h5 style=\"margin-top: 15px; margin-bottom: 5px;\">Correction Details of ")
      .append(HtmlUtils.htmlEscape(param.getLabel()))
      .append("</h5>");
    
    // Start the correction table
    sb.append("<table class=\"parameter-table\" style=\"margin-bottom: 20px;\">")
      .append("<thead><tr>")
      .append("<th>Attribute</th><th>Values</th><th>Details</th>")
      .append("</tr></thead><tbody>");
    
    // Add correction status
    sb.append("<tr>")
      .append("<td>Correction Status</td>")
      .append("<td>").append(HtmlUtils.htmlEscape(correction.getStatus() != null ? correction.getStatus() : "-")).append("</td>")
      .append("<td>-</td>")
      .append("</tr>");
    
    // Add corrected value
    String oldValue = correction.getOldValue() != null ? correction.getOldValue() : "-";
    String newValue = correction.getNewValue() != null ? correction.getNewValue() : "-";
    if (param.getType() != null) {
      Type.Parameter type = Type.Parameter.valueOf(param.getType());
      if ((type == Type.Parameter.RESOURCE || type == Type.Parameter.MULTI_RESOURCE)) {
        List<ResourceParameterChoiceDto> oldResourceChoices = Collections.emptyList();
        List<ResourceParameterChoiceDto> newResourceChoices = Collections.emptyList();
        if (!Utility.isEmpty(correction.getOldChoices())) {
          oldResourceChoices = JsonUtils.readValue(correction.getOldChoices().toString(), new TypeReference<List<ResourceParameterChoiceDto>>() {});
        }
        if (!Utility.isEmpty(correction.getNewChoices())) {
          newResourceChoices = JsonUtils.readValue(correction.getNewChoices().toString(), new TypeReference<List<ResourceParameterChoiceDto>>() {});
        }
        oldValue = formatResourceChoicesForCorrection(oldResourceChoices);
        newValue = formatResourceChoicesForCorrection(newResourceChoices);
      } else if (type == Type.Parameter.MEDIA || type == Type.Parameter.FILE_UPLOAD || type == Type.Parameter.SIGNATURE) {
        List<MediaDto> oldMedias = correction.getOldMedias();
        List<MediaDto> newMedias = correction.getNewMedias();

        if (Utility.isEmpty(oldMedias) && !Utility.isEmpty(correction.getOldChoices())) {
          oldMedias = JsonUtils.readValue(
            correction.getOldChoices().toString(),
            new TypeReference<List<MediaDto>>() {}
          );
        }
        if (Utility.isEmpty(newMedias) && !Utility.isEmpty(correction.getNewChoices())) {
          newMedias = JsonUtils.readValue(
            correction.getNewChoices().toString(),
            new TypeReference<List<MediaDto>>() {}
          );
        }

        oldValue = formatJobAnnotationMedia(oldMedias);
        newValue = formatJobAnnotationMedia(newMedias);
      } else if ((type == Type.Parameter.DATE) && !"-".equals(oldValue)) {
        ParameterValueDto temp = new ParameterValueDto();
        temp.setValue(oldValue);
        oldValue = getDateParameterValue(temp, facilityDto);
      } else if (type == Type.Parameter.DATE_TIME && !"-".equals(oldValue)) {
        ParameterValueDto temp = new ParameterValueDto();
        temp.setValue(oldValue);
        oldValue = getDateTimeParameterValue(temp, facilityDto);
      }
      if (type == Type.Parameter.DATE && !"-".equals(newValue)) {
        ParameterValueDto temp = new ParameterValueDto();
        temp.setValue(newValue);
        newValue = getDateParameterValue(temp, facilityDto);
      } else if (type == Type.Parameter.DATE_TIME && !"-".equals(newValue)) {
        ParameterValueDto temp = new ParameterValueDto();
        temp.setValue(newValue);
        newValue = getDateTimeParameterValue(temp, facilityDto);
      }
    }


    // Build details for each row
    String lastUpdatedInfo = "";
    if (correction.getCreatedAt() != null && correction.getCreatedBy() != null) {
      lastUpdatedInfo = "Last updated by " +
        correction.getCreatedBy().getFirstName() + " " +
        correction.getCreatedBy().getLastName() + " (ID: " +
        correction.getCreatedBy().getEmployeeId() + ") at " +
        formatTimeWithFacility(correction.getCreatedAt(), facilityDto) +
        " from " + oldValue + ".";
    }

    String initiatorInfo = "";
    if (correction.getCreatedAt() != null && correction.getCreatedBy() != null) {
      initiatorInfo = "Initiated at " +
        formatTimeWithFacility(correction.getCreatedAt(), facilityDto) +
        " by " +
        correction.getCreatedBy().getFirstName() + " " +
        correction.getCreatedBy().getLastName() + " (ID: " +
        correction.getCreatedBy().getEmployeeId() + ").";
    }

    String correctorInfo = "";
    if (correction.getCorrector() != null && !correction.getCorrector().isEmpty()) {
      for (com.leucine.streem.dto.CorrectorDto corrector : correction.getCorrector()) {
        if (corrector.getUser() != null) {
          correctorInfo = "Corrected at ";
          if (corrector.getModifiedAt() != null) {
            correctorInfo += formatTimeWithFacility(corrector.getModifiedAt(), facilityDto) + " ";
          }
          correctorInfo += "by " +
            corrector.getUser().getFirstName() + " " +
            corrector.getUser().getLastName() + " (ID: " +
            corrector.getUser().getEmployeeId() + ").";
          break; // Just show the first corrector
        }
      }
    }

    String reviewerInfo = "";
    if (correction.getReviewer() != null && !correction.getReviewer().isEmpty()) {
      for (com.leucine.streem.dto.ReviewerDto reviewer : correction.getReviewer()) {
        if (reviewer.getUser() != null) {
          reviewerInfo = "Reviewed at ";
          if (reviewer.getModifiedAt() != null) {
            reviewerInfo += formatTimeWithFacility(reviewer.getModifiedAt(), facilityDto) + " ";
          }
          reviewerInfo += "by " +
            reviewer.getUser().getFirstName() + " " +
            reviewer.getUser().getLastName() + " (ID: " +
            reviewer.getUser().getEmployeeId() + ")";
          break; // Just show the first reviewer
        }
      }
    }

    String newValueCell2 = (!Utility.isEmpty(newValue) && newValue.contains("<a href="))
      ? newValue
      : HtmlUtils.htmlEscape(newValue);
    sb.append("<tr>")
      .append("<td>").append(HtmlUtils.htmlEscape(param.getLabel())).append(" - Corrected Value</td>")
      .append("<td>").append(newValueCell2).append("</td>")
      .append("<td>").append(HtmlUtils.htmlEscape(lastUpdatedInfo.trim())).append("</td>")
      .append("</tr>");

    // Initiator Remarks row
    sb.append("<tr>")
      .append("<td>Initiator Remarks</td>")
      .append("<td>").append(HtmlUtils.htmlEscape(!Utility.isEmpty(correction.getInitiatorsReason() ) ? correction.getInitiatorsReason() : "____________________")).append("</td>")
      .append("<td>").append(HtmlUtils.htmlEscape(initiatorInfo.trim().isEmpty() ? "____________________" : initiatorInfo.trim())).append("</td>")
      .append("</tr>");

    // Corrector Remarks row (always show)
    String status = !Utility.isEmpty(correction.getStatus() ) ? correction.getStatus() : "";
    if (!State.Correction.INITIATED.toString().equalsIgnoreCase(status)) {
      sb.append("<tr>")
        .append("<td>Corrector Remarks</td>")
        .append("<td>").append(HtmlUtils.htmlEscape(!Utility.isEmpty(correction.getCorrectorsReason() ) ? correction.getCorrectorsReason() : "____________________")).append("</td>")
        .append("<td>").append(HtmlUtils.htmlEscape(correctorInfo.trim().isEmpty() ? "____________________" : correctorInfo.trim())).append("</td>")
        .append("</tr>");
    } else {
      sb.append("<tr>")
        .append("<td>Corrector Remarks</td>")
        .append("<td>____________________</td>")
        .append("<td>____________________</td>")
        .append("</tr>");
    }

    // Reviewer Remarks row (always show)
    if (State.Correction.ACCEPTED.toString().equalsIgnoreCase(status)) {
      sb.append("<tr>")
        .append("<td>Reviewer Remarks</td>")
        .append("<td>").append(HtmlUtils.htmlEscape(correction.getReviewersReason() != null ? correction.getReviewersReason() : "____________________")).append("</td>")
        .append("<td>").append(HtmlUtils.htmlEscape(reviewerInfo.trim().isEmpty() ? "____________________" : reviewerInfo.trim())).append("</td>")
        .append("</tr>");
    } else {
      sb.append("<tr>")
        .append("<td>Reviewer Remarks</td>")
        .append("<td>____________________</td>")
        .append("<td>____________________</td>")
        .append("</tr>");
    }
    
    // Close the table
    sb.append("</tbody></table>");
  }

  /**
   * Helper to format resource choices for correction display
   */
  public static String formatResourceChoicesForCorrection(List<ResourceParameterChoiceDto> choices) {
    if (choices == null || choices.isEmpty()) return "-";
    List<String> names = new ArrayList<>();
    for (ResourceParameterChoiceDto choice : choices) {
      if (!Utility.isEmpty(choice.getObjectDisplayName())) {
        if (!Utility.isEmpty(choice.getObjectExternalId())) {
          names.add(choice.getObjectDisplayName() + " (ID: " + choice.getObjectExternalId() + ")");
        } else {
          names.add(choice.getObjectDisplayName());
        }
      } else if (!Utility.isEmpty(choice.getObjectId())) {
        names.add(choice.getObjectId());
      }
    }
    return names.isEmpty() ? "-" : String.join(", ", names);
  }

  public static String formatMediaChoicesForCorrection(JsonNode choicesNode) {
    if (Utility.isEmpty(choicesNode)) {
      return "-";
    }
    List<String> parts = new ArrayList<>();
      if (choicesNode.isArray()) {
        List<MediaDto> mediaList = JsonUtils.convertValue(choicesNode, new TypeReference<List<MediaDto>>() {});
        for (MediaDto media : mediaList) {
          String name = null;
          if (!Utility.isEmpty(media.getName())) {
            name = media.getName();
          } else if (!Utility.isEmpty(media.getOriginalFilename())) {
            name = media.getOriginalFilename();
          } else if (!Utility.isEmpty(media.getDescription())) {
            name = media.getDescription();
          } else if (!Utility.isEmpty(media.getId())) {
            name = "File " + media.getId();
          }

          if (!Utility.isEmpty(name)) {
            parts.add(name);
          }
        }
      }
    return parts.isEmpty() ? "-" : String.join(", ", parts);
  }
  /**
   * Helper to format select-type choices (SINGLE_SELECT, MULTISELECT, CHECKLIST, YES_NO) for correction display
   */
  public static String formatSelectChoicesForCorrection(JsonNode choicesNode, JsonNode parameterData) {
    if (choicesNode == null || !choicesNode.isObject() || parameterData == null) return "-";
    List<String> selectedNames = new ArrayList<>();
    // Build a map of id -> name from parameterData
    Map<String, String> idToName = new HashMap<>();
    if (parameterData.isArray()) {
      for (JsonNode option : parameterData) {
        String id = option.has("id") ? option.get("id").asText() : null;
        String name = option.has("name") ? option.get("name").asText() : null;
        if (id != null && name != null) idToName.put(id, name);
      }
    }
    Iterator<String> fieldNames = choicesNode.fieldNames();
    while (fieldNames.hasNext()) {
      String optionId = fieldNames.next();
      String state = choicesNode.get(optionId).asText();
      if ("SELECTED".equalsIgnoreCase(state)) {
        String displayName = idToName.getOrDefault(optionId, optionId);
        selectedNames.add(displayName);
      }
    }
    return selectedNames.isEmpty() ? "-" : String.join(", ", selectedNames);
  }

  public static String renderTasksAsColumns(StageDto stage,
                                           List<TaskDto> tasks,
                                           FacilityDto facilityDto) throws JsonProcessingException {
    if (tasks.isEmpty()) {
        return "";
    }

    StringBuilder sb = new StringBuilder();

    // Get the master task
    TaskDto masterTask = tasks.get(0);

    // Gather all executions (MASTER + RECURRING), and sort by orderTree and startedAt for correct order
    List<TaskExecutionDto> allExecs = masterTask.getTaskExecutions().stream()
        .filter(te -> te.getType() == com.leucine.streem.constant.Type.TaskExecutionType.MASTER
            || te.getType() == Type.TaskExecutionType.RECURRING)
        .sorted(Comparator
            .comparingInt((TaskExecutionDto te) -> te.getOrderTree() != null ? te.getOrderTree() : 0)
            .thenComparing(te -> te.getStartedAt() != null ? te.getStartedAt() : 0L))
        .toList();

    int totalExecutions = allExecs.size();
    int limit = RECURRING_TASK_COLUMN_LIMIT;

    // Split executions into chunks of size 'limit'
    for (int chunkStart = 0; chunkStart < totalExecutions; chunkStart += limit) {
        int chunkEnd = Math.min(chunkStart + limit, totalExecutions);
        List<TaskExecutionDto> execs = allExecs.subList(chunkStart, chunkEnd);

        String baseNo = stage.getOrderTree() + "." + masterTask.getOrderTree();

        // Add task header (add chunk info if more than one chunk)
        sb.append("<h4>Task ").append(baseNo);

        sb.append(" – ").append(HtmlUtils.htmlEscape(masterTask.getName())).append("</h4>");

        // Check if any parameter has a correction and add "Error Correction is Enabled" message if needed
        boolean hasCorrection = false;
        if (masterTask.getParameters() != null) {
            for (ParameterDto param : masterTask.getParameters()) {
                if (param.getResponse() != null && !param.getResponse().isEmpty()) {
                    for (ParameterValueDto resp : param.getResponse()) {
                        if (resp != null && resp.getCorrection() != null) {
                            hasCorrection = true;
                            break;
                        }
                    }
                    if (hasCorrection) break;
                }
            }
        }

        if (hasCorrection) {
            sb.append("<p style=\"font-weight: bold; margin: 5px 0;\">Error Correction is Enabled</p>");
        }

        // Check if we have recurring tasks - if so, skip the redundant state information above the table
        boolean hasRecurringTasks = execs.stream()
            .anyMatch(ex -> ex.getType() == Type.TaskExecutionType.RECURRING);

        // Only add task state and start/end info for non-recurring tasks
        if (!hasRecurringTasks) {
            // Add task state with exception reason if applicable
            sb.append(HtmlTemplates.taskStateWithReason(masterTask.getTaskExecutions().get(0)));

            // Add start and end information as a sentence under Task State for each execution
            for (int i = 0; i < execs.size(); i++) {
                TaskExecutionDto ex = execs.get(i);
                String taskNumber = (i == 0) ? baseNo : baseNo + "." + (chunkStart + i);

                String startInfo = "_";
                if (ex.getStartedAt() != null && ex.getStartedBy() != null) {
                    startInfo = "Task " + taskNumber + " started on " +
                            formatTimeWithFacility(ex.getStartedAt(), facilityDto) +
                            " by " + ex.getStartedBy().getFirstName() + " " +
                            ex.getStartedBy().getLastName() +
                            " (ID: " + ex.getStartedBy().getEmployeeId() + ")";
                }

                String endInfo = "_";
                if (ex.getEndedAt() != null && ex.getEndedBy() != null) {
                    endInfo = "Task " + taskNumber + " completed on " +
                            formatTimeWithFacility(ex.getEndedAt(), facilityDto) +
                            " by " + ex.getEndedBy().getFirstName() + " " +
                            ex.getEndedBy().getLastName() +
                            " (ID: " + ex.getEndedBy().getEmployeeId() + ")";
                }

                sb.append("<p style=\"margin: 5px 0;\">").append(startInfo).append("</p>");
                sb.append("<p style=\"margin: 5px 0;\">").append(endInfo).append("</p>");
            }
        }

        // Add time condition message if available
        String timeConditionMessage = HtmlTemplates.getTaskTimeConditionMessage(masterTask);
        if (timeConditionMessage != null) {
            sb.append("<p style=\"font-weight: bold; margin: 5px 0;\">")
                .append(HtmlUtils.htmlEscape(timeConditionMessage))
                .append("</p>");
        }

        // Create table header
        sb.append(HtmlTemplates.PARAMETER_TABLE_START)
            .append("<thead>")
            .append(HtmlTemplates.TABLE_ROW_START)
            .append(HtmlTemplates.TABLE_HEADER_START).append("Attributes").append(HtmlTemplates.TABLE_HEADER_END);

        // Add column headers for each execution
        for (int i = 0; i < execs.size(); i++) {
            int execIndex = chunkStart + i;
            String hdr = (execIndex == 0)
                ? "Task " + baseNo                               // MASTER
                : "Task " + baseNo + "." + execIndex;            // RECURRING i
            sb.append(HtmlTemplates.TABLE_HEADER_START)
                .append(HtmlUtils.htmlEscape(hdr))
                .append(HtmlTemplates.TABLE_HEADER_END);
        }
        sb.append(HtmlTemplates.TABLE_ROW_END)
        .append("</thead>");

        // Start table body
        sb.append("<tbody>");

        // Add task state row as the first row
        sb.append(HtmlTemplates.TABLE_ROW_START)
            .append(HtmlTemplates.TABLE_DATA_START)
            .append("<strong>Task State</strong>")
            .append(HtmlTemplates.TABLE_DATA_END);

        // Add task state for each execution
        for (int i = 0; i < execs.size(); i++) {
            com.leucine.streem.dto.TaskExecutionDto ex = execs.get(i);
            int execIndex = chunkStart + i;
            String taskNumber = (execIndex == 0) ? baseNo : baseNo + "." + execIndex;

            StringBuilder stateInfo = new StringBuilder();
            stateInfo.append("<strong>").append(HtmlUtils.htmlEscape(Utility.toDisplayName(ex.getState()))).append("</strong>");

            // Add start time if available
            if (ex.getStartedAt() != null) {
                stateInfo.append("<br>Started: ")
                        .append(formatTimeWithFacility(ex.getStartedAt(), facilityDto));
            }

            // Add end time if available
            if (ex.getEndedAt() != null) {
                stateInfo.append("<br>Ended: ")
                        .append(formatTimeWithFacility(ex.getEndedAt(), facilityDto));
            }

            // Add exception reason if task is completed with exception
            if (ex.getState() == State.TaskExecution.COMPLETED_WITH_EXCEPTION) {
                stateInfo.append("<br><em>Reason: ");
                if (ex.getReason() != null && ex.getReason().isEmpty()) {
                    stateInfo.append(HtmlUtils.htmlEscape(ex.getReason()));
                } else {
                    stateInfo.append("No reason provided");
                }
                stateInfo.append("</em>");
            }

            sb.append(HtmlTemplates.TABLE_DATA_START)
                .append(stateInfo.toString())
                .append(HtmlTemplates.TABLE_DATA_END);
        }
        sb.append(HtmlTemplates.TABLE_ROW_END);

        // Parameter rows
        boolean hasVisibleParameterRows = false;
        StringBuilder parameterRowsContent = new StringBuilder();

        if (masterTask.getParameters() != null) {
            // Keep label order stable
            List<ParameterDto> visibleParams = masterTask.getParameters().stream()
                .filter(p -> !p.getType().equals(com.leucine.streem.constant.Type.Parameter.INSTRUCTION.toString()))
                .filter(p -> !(p.getData()!=null && p.getData().path("hidden").asBoolean(false)))
                .toList();

            for (ParameterDto param : visibleParams) {
                // Check if ALL parameter values are hidden - only then skip the entire parameter
                boolean allParameterValuesHidden = true;
                boolean hasAnyParameterValue = false;

              for (TaskExecutionDto exec : execs) {
                ParameterValueDto pv = null;
                if (!Utility.isEmpty(param.getResponse())) {
                  for (ParameterValueDto resp : param.getResponse()) {
                    if (!Utility.isEmpty(resp) && !Utility.isEmpty(resp.getTaskExecutionId())
                      && resp.getTaskExecutionId().equals(exec.getId())) {
                      pv = resp;
                      break;
                    }
                  }
                }

                if (pv != null) {
                  hasAnyParameterValue = true;
                  if (!pv.isHidden()) {
                    allParameterValuesHidden = false;
                    break;
                  }
                }
              }

                // Skip parameter only if ALL parameter values are hidden (and at least one parameter value exists)
                if (hasAnyParameterValue && allParameterValuesHidden) {
                    continue; // Skip this parameter entirely - don't start the row at all
                }

                // We have a visible parameter row
                hasVisibleParameterRows = true;

                // Start the parameter row only if we're going to show it
                parameterRowsContent.append(HtmlTemplates.TABLE_ROW_START)
                    .append(HtmlTemplates.TABLE_DATA_START);

                // Check if any parameter value has a correction and add superscript with correction index
                StringBuilder correctionIndexes = new StringBuilder();
                if (param.getResponse() != null && !param.getResponse().isEmpty()) {
                    int correctionCount = 1;
                    for (com.leucine.streem.dto.ParameterValueDto resp : param.getResponse()) {
                        if (resp != null && resp.getCorrection() != null) {
                            if (correctionIndexes.length() > 0) {
                                correctionIndexes.append(", ");
                            }
                            correctionIndexes.append("C").append(correctionCount);
                            correctionCount++;
                        }
                    }
                }

                // Add superscript with correction indexes to parameter label if it has corrections
                if (correctionIndexes.length() > 0) {
                    parameterRowsContent.append(HtmlUtils.htmlEscape(param.getLabel()))
                    .append("<sup style=\"font-weight: bold;\">(").append(correctionIndexes.toString()).append(")</sup>");
                } else {
                    parameterRowsContent.append(HtmlUtils.htmlEscape(param.getLabel()));
                }

                parameterRowsContent.append(HtmlTemplates.TABLE_DATA_END);

                for (int col = 0; col < execs.size(); col++) {
                    int execIndex = chunkStart + col;
                    // Find the parameter value for this execution by matching taskExecutionId
                    ParameterValueDto pv = null;
                    if (param.getResponse() != null) {
                        for (ParameterValueDto resp : param.getResponse()) {
                            if (resp != null && resp.getTaskExecutionId() != null
                                && resp.getTaskExecutionId().equals(execs.get(col).getId())) {
                                pv = resp;
                                break;
                            }
                        }
                    }

                    String value = "-";
                    String user  = "-";
                    String time  = "-";

                    // Handle hidden parameter values per cell
                    if (pv != null && pv.isHidden()) {
                        // Show empty cell for hidden parameter value
                        parameterRowsContent.append(HtmlTemplates.TABLE_DATA_START)
                            .append("-").append("<br>")
                            .append(HtmlTemplates.TABLE_DATA_END);
                        continue;
                    }

                    if (pv != null) {
                        // Check if parameter has a correction
                        boolean paramValueHasCorrection = pv.getCorrection() != null;

                        // If parameter has a correction, use the corrected value ONLY if status is ACCEPTED or CORRECTED
                        if (paramValueHasCorrection) {
                            CorrectionDto correction = pv.getCorrection();
                            String correctionStatus = !Utility.isEmpty(correction.getStatus()) ? correction.getStatus().trim() : "";
                            String paramTypeStr = param.getType() != null ? param.getType().trim() : "";
                            boolean isFinalCorrection = correctionStatus.equalsIgnoreCase(State.Correction.ACCEPTED.toString()) || correctionStatus.equalsIgnoreCase(State.Correction.CORRECTED.toString());
                            Type.Parameter type = Type.Parameter.valueOf(paramTypeStr);
                            if (isFinalCorrection && type != null && (type == Type.Parameter.RESOURCE || type == Type.Parameter.MULTI_RESOURCE)) {
                                // Show corrected resource value
                                List<ResourceParameterChoiceDto> newResourceChoices = Collections.emptyList();
                                if (correction.getNewChoices() != null) {
                                    newResourceChoices = JsonUtils.readValue(
                                        correction.getNewChoices().toString(),
                                        new com.fasterxml.jackson.core.type.TypeReference<List<ResourceParameterChoiceDto>>() {}
                                    );
                                }
                                value = formatResourceChoicesForCorrection(newResourceChoices);
                            } else if (isFinalCorrection && type != null && (
                                    type == Type.Parameter.SINGLE_SELECT ||
                                    type == Type.Parameter.MULTISELECT ||
                                    type == Type.Parameter.CHECKLIST ||
                                    type == Type.Parameter.YES_NO
                                )) {
                                JsonNode newChoicesNode = null;
                                if (correction.getNewChoices() != null) {
                                    ObjectMapper om = new ObjectMapper();
                                    newChoicesNode = om.readTree(correction.getNewChoices().toString());
                                }
                                value = formatSelectChoicesForCorrection(newChoicesNode, param.getData());
                            } else if (type == Type.Parameter.MEDIA || type == Type.Parameter.FILE_UPLOAD || type == Type.Parameter.SIGNATURE) {
                                if (isFinalCorrection) {
                                    value = formatMediaChoicesForCorrection(correction.getNewChoices());
                                } else {
                                    value = formatMediaChoicesForCorrection(correction.getOldChoices());
                                }
                                if (Utility.isEmpty(value) || "-".equals(value)) {
                                    value = getParameterValueAsString(param, pv);
                                }
                            } else if (isFinalCorrection) {
                                value = correction.getNewValue();
                            } else {
                                value = correction.getOldValue();
                            }
                        } else {
                            value = getParameterValueAsString(param, pv);
                        }

                        // Store original value before adding verification info
                        String originalValue = value;

                        // Add verification information using common method
                        value = addVerificationInfo(value, pv, facilityDto);

                        if (pv.getState() != State.ParameterExecution.NOT_STARTED && !Utility.isEmpty(pv.getAudit().getModifiedBy())) {
                            user = Utility.getFullNameAndEmployeeId(
                                pv.getAudit().getModifiedBy().getFirstName(),
                                pv.getAudit().getModifiedBy().getLastName(),
                                pv.getAudit().getModifiedBy().getEmployeeId());
                            time = formatTimeWithFacility(pv.getAudit().getModifiedAt(), facilityDto);
                        }
                    }

                    // Check for any HTML tags that indicate raw HTML content (verification info, line breaks, etc.)
                    boolean hasHtmlContent = (!Utility.isEmpty(value)) && (value.contains("<br>") || value.contains("<hr>") || value.contains("<b>") || value.contains("<a href="));

                    if (hasHtmlContent) {
                        // Don't escape HTML - render verification info and other HTML content properly
                        parameterRowsContent.append(HtmlTemplates.TABLE_DATA_START)
                            .append(value).append("<br>")
                            .append(HtmlUtils.htmlEscape(user)).append("<br>")
                            .append(HtmlUtils.htmlEscape(time))
                            .append(HtmlTemplates.TABLE_DATA_END);
                    } else {
                        // Escape HTML for plain text values
                        parameterRowsContent.append(HtmlTemplates.TABLE_DATA_START)
                            .append(HtmlUtils.htmlEscape(value)).append("<br>")
                            .append(HtmlUtils.htmlEscape(user)).append("<br>")
                            .append(HtmlUtils.htmlEscape(time))
                            .append(HtmlTemplates.TABLE_DATA_END);
                    }
                }
                parameterRowsContent.append(HtmlTemplates.TABLE_ROW_END);
            }
        }

        // Only add parameter rows if we have visible parameters
        if (hasVisibleParameterRows) {
            sb.append(parameterRowsContent.toString());
        }

        // Close table
        sb.append("</tbody>").append(HtmlTemplates.PARAMETER_TABLE_END);

        // Add early start / delayed completion messages (unified for master and recurring)
        for (int col = 0; col < execs.size(); col++) {
            TaskExecutionDto ex = execs.get(col);
            int absoluteExecIndex = chunkStart + col;
            String taskNumber = (absoluteExecIndex == 0) ? baseNo : baseNo + "." + absoluteExecIndex;

            if (ex.getType() == Type.TaskExecutionType.MASTER) {
                String earlyStartReason = null;
                String delayedCompletionReason = null;

                // Prefer scheduled fields; fallback to recurring fields if necessary
                if (!Utility.isEmpty(ex.getSchedulePrematureStartReason())) {
                    earlyStartReason = ex.getSchedulePrematureStartReason();
                } else if (!Utility.isEmpty(ex.getRecurringPrematureStartReason())) {
                    earlyStartReason = ex.getRecurringPrematureStartReason();
                }

                if (!Utility.isEmpty(ex.getScheduleOverdueCompletionReason())) {
                    delayedCompletionReason = ex.getScheduleOverdueCompletionReason();
                } else if (!Utility.isEmpty(ex.getRecurringOverdueCompletionReason())) {
                    delayedCompletionReason = ex.getRecurringOverdueCompletionReason();
                }

                if (earlyStartReason != null || delayedCompletionReason != null) {
                    sb.append("<div style=\"margin: 10px 0;\">")
                      .append("<b>E ").append(taskNumber).append("</b><br>");
                    if (earlyStartReason != null) {
                        sb.append("Early start for scheduled task: ")
                          .append(HtmlUtils.htmlEscape(earlyStartReason));
                        if (delayedCompletionReason != null) {
                            sb.append(", ");
                        }
                    }
                    if (delayedCompletionReason != null) {
                        sb.append("Delayed completion for scheduled task: ")
                          .append(HtmlUtils.htmlEscape(delayedCompletionReason));
                    }
                    sb.append("</div>");
                }
            } else if (ex.getType() == Type.TaskExecutionType.RECURRING) {
                if (!Utility.isEmpty(ex.getRecurringPrematureStartReason())) {
                    sb.append("<div style=\"margin: 10px 0;\">")
                      .append("<b>E ").append(taskNumber).append("</b><br>")
                      .append("Early start for recurring task: ")
                      .append(HtmlUtils.htmlEscape(ex.getRecurringPrematureStartReason()))
                      .append("</div>");
                }
                if (!Utility.isEmpty(ex.getRecurringOverdueCompletionReason())) {
                    sb.append("<div style=\"margin: 10px 0;\">")
                      .append("<b>E ").append(taskNumber).append("</b><br>")
                      .append("Delayed completion for recurring task: ")
                      .append(HtmlUtils.htmlEscape(ex.getRecurringOverdueCompletionReason()))
                      .append("</div>");
                }
            }
        }

        // Check for parameters with corrections and render correction tables if found
        // Only render correction tables for REPEAT tasks (columns 1+), not MASTER tasks (column 0)
        Set<String> correctionTablesRendered = new java.util.HashSet<>();

        if (masterTask.getParameters() != null) {
            for (ParameterDto param : masterTask.getParameters()) {
                if (param.getResponse() != null && !param.getResponse().isEmpty()) {
                    for (int col = 0; col < execs.size(); col++) {
                        int execIndex = chunkStart + col;
                        ParameterValueDto pv = (param.getResponse().size() > execIndex) ?
                            param.getResponse().get(execIndex) : null;

                        // Only render correction table if correction exists for this specific task execution
                        String correctionKey = param.getId() + "_" + execIndex;
                        if (
                            pv != null &&
                            pv.getCorrection() != null &&
                            pv.getTaskExecutionId() != null &&
                            pv.getTaskExecutionId().equals(execs.get(col).getId()) &&
                            !correctionTablesRendered.contains(correctionKey)
                        ) {
                            renderCorrectionTableWithColumn(sb, param, pv, execIndex, null);
                            correctionTablesRendered.add(correctionKey);
                        }
                    }
                }
            }
        }

        // Add spacing between tables if there are more chunks
        if (chunkEnd < totalExecutions) {
            sb.append("<div style=\"margin: 24px 0;\"></div>");
        }
    }

    return sb.toString();
  }
  
  /**
   * Format job log cell value based on column type using consistent logic
   * This ensures PDF and Excel outputs use the same formatting rules
   * 
   * @param columnType The type of column (DATE, DATE_TIME, TEXT, etc.)
   * @param value The value to format
   * @param identifierValue The identifier value (for resource parameters)
   * @param resourceParameters Resource parameters map (for resource parameters)
   * @param resourceParameterChoiceDtoMap Global map of resource parameter choices
   * @param timezoneId The timezone ID to convert to offset
   * @param dateTimeFormat The date/time format to use
   * @param dateFormat The date format to use
   * @param mediaData List of media data for FILE type columns
   * @return The formatted value as a string
   */
  public static String formatJobLogCellValue(Type.JobLogColumnType columnType, String value, String identifierValue,
                                           Map<String, JobLogResource> resourceParameters,
                                           Map<String, ResourceParameterChoiceDto> resourceParameterChoiceDtoMap,
                                           String timezoneId, String dateTimeFormat, String dateFormat,
                                           List<JobLogMediaData> mediaData) {
    
    String zoneOffsetString = getZoneOffsetString(timezoneId);
    
    switch (columnType) {
      case TEXT:
        // Handle resource parameters specially
        if (identifierValue != null && !identifierValue.isEmpty() && resourceParameters != null) {
          return formatResourceParameterValue(identifierValue, value, resourceParameterChoiceDtoMap, resourceParameters);
        } else {
          // Check if value is JSON for select parameters or resources
          // Try to detect parameter type from the value structure for better formatting
          String formattedValue = formatSelectOrResourceParameterValue(detectParameterType(value), value, null);
          return formattedValue != null ? formattedValue : (value != null ? value : "");
        }
        
      case DATE:
        if (!Utility.isEmpty(value)) {
          try {
            long timestamp = Long.parseLong(value);
            long dateValueWithOffset = DateTimeUtils.addOffSetToTime(timestamp, zoneOffsetString);
            return DateTimeUtils.getFormattedDatePattern(dateValueWithOffset, dateFormat);
          } catch (NumberFormatException e) {
            // Not a valid timestamp, return as-is
            return value;
          }
        }
        return "";
        
      case DATE_TIME:
        if (!Utility.isEmpty(value)) {
          try {
            long timestamp = Long.parseLong(value);
            long dateValueWithOffset = DateTimeUtils.addOffSetToTime(timestamp, zoneOffsetString);
            return DateTimeUtils.getFormattedDateTimeOfPattern(dateValueWithOffset, dateTimeFormat);
          } catch (NumberFormatException e) {
            // Not a valid timestamp, return as-is
            return value;
          }
        }
        return "";
        
      case FILE:
        if (mediaData != null && !mediaData.isEmpty()) {
          StringBuilder linksBuilder = new StringBuilder();
          for (JobLogMediaData media : mediaData) {
            if (linksBuilder.length() > 0) {
              linksBuilder.append(", ");
            }
            linksBuilder.append("<a href=\"")
              .append(media.getLink())
              .append("\">")
              .append(media.getName() != null ? media.getName() : "File")
              .append("</a>");
          }
          return linksBuilder.toString();
        }
        return "";
        
      default:
        return value != null ? value : "";
    }
  }

  /**
   * Format resource parameter values for display
   * 
   * @param identifierValue Comma-separated identifier values
   * @param value Comma-separated display values (fallback)
   * @param resourceParameterChoiceDtoMap Global map of resource parameter choices
   * @param resourceParameters Resource parameters map
   * @return Formatted resource parameter value string
   */
  public static String formatResourceParameterValue(String identifierValue, String value,
                                                   Map<String, ResourceParameterChoiceDto> resourceParameterChoiceDtoMap,
                                                   Map<String, JobLogResource> resourceParameters) {
    String[] identifierValueArr = identifierValue.split(",");
    
    StringBuilder valueBuilder = new StringBuilder();
    for (int i = 0; i < identifierValueArr.length; i++) {
      if (!Utility.isEmpty(identifierValueArr[i])) {
        String trimmedId = identifierValueArr[i].trim();
        
        ResourceParameterChoiceDto choice = resourceParameterChoiceDtoMap != null ?
                                          resourceParameterChoiceDtoMap.get(trimmedId) : null;
        String displayName;
        
        if (choice != null) {
          displayName = choice.getObjectDisplayName() + " (ID: " + choice.getObjectExternalId() + ")";
        } else {
          displayName = getResourceDisplayNameFromIdentifier(trimmedId, resourceParameters);
        }
        
        if (valueBuilder.length() > 0) {
          valueBuilder.append(", ");
        }
        valueBuilder.append(displayName);
      }
    }
    
    return valueBuilder.length() > 0 ? valueBuilder.toString() : (value != null ? value : "");
  }

  /**
   * Adds verification information to parameter value string for all task types (MASTER, REPEAT, RECURRING)
   * @param value The original parameter value
   * @param parameterValue The parameter value DTO containing verification info
   * @param facilityDto The facility DTO for date/time formatting
   * @return The value string with verification information appended
   */
  public static String addVerificationInfo(String value, ParameterValueDto parameterValue, FacilityDto facilityDto) {
    if (parameterValue != null && parameterValue.getParameterVerifications() != null &&
        !parameterValue.getParameterVerifications().isEmpty()) {

        for (ParameterVerificationDto verification : parameterValue.getParameterVerifications()) {
            if (State.ParameterVerification.ACCEPTED.toString().equals(verification.getVerificationStatus())) {

                String verificationTime = "-";
                if (verification.getModifiedAt() != null) {
                    try {
                        long timestamp = Long.parseLong(verification.getModifiedAt());
                        verificationTime = formatTimeWithFacility(timestamp, facilityDto);
                    } catch (NumberFormatException e) {
                        verificationTime = verification.getModifiedAt();
                    }
                }

                String verifierInfo = verification.getModifiedBy().getFirstName() + " " +
                                    verification.getModifiedBy().getLastName() +
                                    " (ID: " + verification.getModifiedBy().getEmployeeId() + ")";

                if (Type.VerificationType.PEER.toString().equals(verification.getVerificationType())) {
                    if (verification.isBulk()) {
                        value += "<hr><b>Reviewed By</b> at " + verificationTime + " by " + verifierInfo + ". Signed for Peer verification";
                    } else {
                        value += "<hr><b>Peer verified</b> at " + verificationTime + " by " + verifierInfo;
                    }
                } else if (Type.VerificationType.SELF.toString().equals(verification.getVerificationType())) {
                    value += "<hr><b>Self verified</b> at " + verificationTime + " by " + verifierInfo;
                }
            } else if (State.ParameterVerification.PENDING.toString().equals(verification.getVerificationStatus())) {
                // Handle pending verification states
                String verificationTime = "-";
                if (verification.getCreatedAt() != null) {
                    try {
                        long timestamp = Long.parseLong(verification.getCreatedAt());
                        verificationTime = formatTimeWithFacility(timestamp, facilityDto);
                    } catch (NumberFormatException e) {
                        verificationTime = verification.getCreatedAt();
                    }
                }

                if (Type.VerificationType.PEER.toString().equals(verification.getVerificationType())) {
                    value += "<hr><b>Peer verification initiated</b> at " + verificationTime;
                } else if (Type.VerificationType.SELF.toString().equals(verification.getVerificationType())) {
                    value += "<hr><b>Self verification initiated</b> at " + verificationTime;
                }
            }
        }
    }
    
    // Add parameter state information for pending approvals
    value = addParameterStateInfo(value, parameterValue, facilityDto);
    
    // Add exception information
    value = addExceptionInfo(value, parameterValue, facilityDto);
    
    return value;
  }

  /**
   * Adds parameter state information for pending states
   * @param value The original parameter value
   * @param parameterValue The parameter value DTO containing state info
   * @return The value string with state information appended
   */
  public static String addParameterStateInfo(String value, ParameterValueDto parameterValue, FacilityDto facilityDto) {
    if (parameterValue != null && parameterValue.getState() != null) {
        if (parameterValue.getState() == State.ParameterExecution.PENDING_FOR_APPROVAL) {
            // Check if exception data exists - if so, let addExceptionInfo handle it
            if (parameterValue.getException() != null && !parameterValue.getException().isEmpty()) {
                // Exception data exists, skip parameter state info to avoid duplication
                return value;
            }
            
            // No exception data, show parameter state info
            String stateTime = "-";
            String initiatorInfo = "-";
            
            if (parameterValue.getAudit() != null) {
                if (parameterValue.getAudit().getModifiedAt() != null) {
                    stateTime = formatTimeWithFacility(parameterValue.getAudit().getModifiedAt(), facilityDto);
                }
                if (parameterValue.getAudit().getModifiedBy() != null) {
                    initiatorInfo = parameterValue.getAudit().getModifiedBy().getFirstName() + " " +
                                  parameterValue.getAudit().getModifiedBy().getLastName() +
                                  " (ID: " + parameterValue.getAudit().getModifiedBy().getEmployeeId() + ")";
                }
            }
            
            value += "<hr><b>Initiated with exception</b> by " + initiatorInfo + " at " + stateTime;
        }
    }
    return value;
  }

  /**
   * Adds exception information to parameter value string for all task types (MASTER, REPEAT, RECURRING)
   * @param value The original parameter value
   * @param parameterValue The parameter value DTO containing exception info
   * @return The value string with exception information appended
   */
  public static String addExceptionInfo(String value, ParameterValueDto parameterValue, FacilityDto facilityDto) {
    if (parameterValue != null && parameterValue.getException() != null &&
        !parameterValue.getException().isEmpty()) {

        for (ParameterExceptionDto exception : parameterValue.getException()) {
            State.ParameterException status = State.ParameterException.valueOf(exception.getStatus());
            
            if (status != null) {
                String exceptionTime = "-";
                if (exception.getCreatedAt() != null) {
                    try {
                        exceptionTime = formatTimeWithFacility(exception.getCreatedAt(), facilityDto);
                    } catch (Exception e) {
                        exceptionTime = exception.getCreatedAt().toString();
                    }
                }

                String initiatorInfo = "-";
                if (exception.getCreatedBy() != null) {
                    initiatorInfo = exception.getCreatedBy().getFirstName() + " " +
                                  exception.getCreatedBy().getLastName() +
                                  " (ID: " + exception.getCreatedBy().getEmployeeId() + ")";
                }

                switch (status) {
                    case INITIATED:
                        String initiatorReason = exception.getInitiatorsReason() != null ? exception.getInitiatorsReason() : "";
                        value += "<hr><b>Initiated exception</b> '" + initiatorReason + "' by " + initiatorInfo + " at " + exceptionTime;
                        break;

                    case ACCEPTED:
                        String reviewerInfo = getReviewerInfo(exception);
                        String reviewerTime = getReviewerTime(exception, facilityDto);
                        String reviewerReason = exception.getReviewersReason() != null ? exception.getReviewersReason() : "";
                        String acceptedReason = exception.getInitiatorsReason() != null ? exception.getInitiatorsReason() : "";
                        
                        value += "<hr><b>Exception</b> '" + acceptedReason + "' <b>Approved</b> by " + reviewerInfo +
                                " on " + reviewerTime + " stating reason '" + reviewerReason + "'";
                        break;
                        
                    case REJECTED:
                        String rejectorInfo = getReviewerInfo(exception);
                        String rejectorTime = getReviewerTime(exception, facilityDto);
                        String rejectorReason = exception.getReviewersReason() != null ? exception.getReviewersReason() : "";
                        String rejectedReason = exception.getInitiatorsReason() != null ? exception.getInitiatorsReason() : "";
                        
                        value += "<hr><b>Exception</b> '" + rejectedReason + "' <b>Rejected</b> by " + rejectorInfo +
                                " on " + rejectorTime + " stating reason '" + rejectorReason + "'";
                        break;

                    case AUTO_ACCEPTED:
                      if(!Utility.isEmpty(exception.getInitiatorsReason())) {
                        String reasonedEntryReason = exception.getInitiatorsReason();
                        value += "<hr><b>Executed with exception</b> '" + reasonedEntryReason + "' by " + initiatorInfo + " at " + exceptionTime;
                      } else {
                        String reasonedEntryReason = exception.getReason();
                        value += "<hr><b>Executed with exception</b> '" + reasonedEntryReason + "' by " + initiatorInfo + " at " + exceptionTime;
                      }
                        break;
                }
            }
        }
    }
    return value;
  }

  /**
   * Gets reviewer information from exception reviewers list
   * @param exception The parameter exception DTO
   * @return Formatted reviewer information
   */
  private static String getReviewerInfo(ParameterExceptionDto exception) {
    if (exception.getReviewer() != null && !exception.getReviewer().isEmpty()) {
        // Find the reviewer who performed the action
        for (ParameterExceptionReviewerDto reviewer : exception.getReviewer()) {
            if (reviewer.isActionPerformed() && reviewer.getUser() != null) {
                return reviewer.getUser().getFirstName() + " " +
                       reviewer.getUser().getLastName() +
                       ", ID: " + reviewer.getUser().getEmployeeId();
            }
        }
    }
    return "-";
  }

  /**
   * Gets reviewer action time from exception reviewers list
   * @param exception The parameter exception DTO
   * @return Formatted reviewer time
   */
  private static String getReviewerTime(ParameterExceptionDto exception, FacilityDto facilityDto) {
    if (exception.getReviewer() != null && !exception.getReviewer().isEmpty()) {
        // Find the reviewer who performed the action
        for (ParameterExceptionReviewerDto reviewer : exception.getReviewer()) {
            if (reviewer.isActionPerformed() && reviewer.getModifiedAt() != null) {
                try {
                    return formatTimeWithFacility(reviewer.getModifiedAt(), facilityDto);
                } catch (Exception e) {
                    return reviewer.getModifiedAt().toString();
                }
            }
        }
    }
    return "-";
  }

  /**
   * Adds template verification information based on parameter verification type for template generation
   * @param value The original parameter value
   * @param parameter The parameter DTO containing verification type configuration
   * @return The value string with template verification lines appended
   */
  public static String addTemplateVerificationInfo(String value, ParameterDto parameter) {
    if (parameter != null && parameter.getVerificationType() != null) {
        Type.VerificationType verificationType = parameter.getVerificationType();

        if (verificationType == Type.VerificationType.PEER || verificationType == Type.VerificationType.BOTH) {
            value += "<hr><b>Peer verified</b> __________________________________";
        }

        if (verificationType == Type.VerificationType.SELF || verificationType == Type.VerificationType.BOTH) {
            value += "<hr><b>Self verified</b> ___________________________________";
        }
    }
    return value;
  }


  /**
   * Formats parameter values based on parameter type with proper ID to name mapping
   * @param parameterType The parameter type from Type.Parameter enum
   * @param value The raw JSON value string
   * @param parameterDefinitionData Optional parameter definition data for ID to name mapping
   * @return Formatted display names or null if not a recognized format
   */
  public static String formatSelectOrResourceParameterValue(Type.Parameter parameterType, String value, JsonNode parameterDefinitionData) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      
      // Handle based on parameter type if available
      if (parameterType != null) {
        switch (parameterType) {
          case SINGLE_SELECT:
          case MULTISELECT:
          case YES_NO:
          case CHECKLIST:
            return formatSelectParameterValue(value, parameterDefinitionData, objectMapper);
          
          case RESOURCE:
          case MULTI_RESOURCE:
            return formatResourceParameterValue(value, objectMapper);
          
          default:
            // For other types, return null to use original value
            return null;
        }
      }
      
      // Fallback to JSON structure detection if parameter type is not available
      // Try to parse as choices object (for SINGLE_SELECT, MULTI_SELECT)
      if (value.trim().startsWith("{") && value.trim().endsWith("}")) {
        return formatSelectParameterValue(value, parameterDefinitionData, objectMapper);
      }
      
      // Try to parse as resource array (for RESOURCE parameters)
      if (value.trim().startsWith("[") && value.trim().endsWith("]")) {
        return formatResourceParameterValue(value, objectMapper);
      }
      
    } catch (Exception e) {
      // If JSON parsing fails, return null to use original value
      return null;
    }
    
    // Not a recognized format
    return null;
  }

  /**
   * Formats select parameter values (SINGLE_SELECT, MULTISELECT, YES_NO, CHECKLIST)
   */
  private static String formatSelectParameterValue(String value, JsonNode parameterDefinitionData, ObjectMapper objectMapper) throws Exception {
    JsonNode choicesNode = objectMapper.readTree(value);
    
    // Check if it looks like a choices object with ID keys and SELECTED/NOT_SELECTED values
    if (choicesNode.isObject() && choicesNode.size() > 0) {
      List<String> selectedNames = new ArrayList<>();
      
      // Create a mapping from ID to name if parameter definition data is available
      Map<String, String> idToNameMap = new HashMap<>();
      if (parameterDefinitionData != null && parameterDefinitionData.isArray()) {
        for (JsonNode optionNode : parameterDefinitionData) {
          String id = optionNode.path("id").asText(null);
          String name = optionNode.path("name").asText(null);
          if (id != null && name != null) {
            idToNameMap.put(id, name);
          }
        }
      }
      
      // Iterate through the choices object
      choicesNode.fieldNames().forEachRemaining(choiceId -> {
        JsonNode status = choicesNode.get(choiceId);
        if (status != null && "SELECTED".equals(status.asText())) {
          // Try to get the display name from the mapping, fallback to ID
          String displayName = idToNameMap.get(choiceId);
          if (displayName != null && !displayName.isEmpty()) {
            selectedNames.add(displayName);
          } else {
            // Fallback to ID if no mapping available
            selectedNames.add(choiceId);
          }
        }
      });
      
      if (!selectedNames.isEmpty()) {
        return String.join(", ", selectedNames);
      }
    }
    
    return null;
  }

  /**
   * Formats resource parameter values (RESOURCE, MULTI_RESOURCE)
   */
  private static String formatResourceParameterValue(String value, ObjectMapper objectMapper) throws Exception {
    JsonNode arrayNode = objectMapper.readTree(value);
    
    if (arrayNode.isArray() && arrayNode.size() > 0) {
      List<String> resourceNames = new ArrayList<>();
      
      for (JsonNode resourceNode : arrayNode) {
        if (resourceNode.isObject()) {
          // Extract objectDisplayName and objectExternalId
          String displayName = resourceNode.path("objectDisplayName").asText(null);
          String externalId = resourceNode.path("objectExternalId").asText(null);
          
          if (displayName != null && !displayName.isEmpty()) {
            if (externalId != null && !externalId.isEmpty()) {
              resourceNames.add(displayName + " (ID: " + externalId + ")");
            } else {
              resourceNames.add(displayName);
            }
          } else {
            // Fallback to objectId if displayName is not available
            String objectId = resourceNode.path("objectId").asText(null);
            if (objectId != null) {
              resourceNames.add(objectId);
            }
          }
        }
      }
      
      if (!resourceNames.isEmpty()) {
        return String.join(", ", resourceNames);
      }
    }
    
    return null;
  }

  /**
   * Detects parameter type from the JSON value structure
   * This helps improve formatting when parameter type is not explicitly available
   * @param value The JSON value string to analyze
   * @return The detected parameter type or null if cannot be determined
   */
  private static Type.Parameter detectParameterType(String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      
      // Try to parse as JSON object (choices format for select parameters)
      if (value.trim().startsWith("{") && value.trim().endsWith("}")) {
        JsonNode choicesNode = objectMapper.readTree(value);
        
        // Check if it looks like a choices object with ID keys and SELECTED/NOT_SELECTED values
        if (choicesNode.isObject() && choicesNode.size() > 0) {
          // Check if all values are SELECTED/NOT_SELECTED (indicates select parameter)
          boolean hasSelectValues = false;
          for (JsonNode statusNode : choicesNode) {
            String status = statusNode.asText();
            if ("SELECTED".equals(status) || "NOT_SELECTED".equals(status)) {
              hasSelectValues = true;
              break;
            }
          }
          
          if (hasSelectValues) {
            // Could be any select type, but we'll default to SINGLE_SELECT
            // The actual formatting logic will handle multiple selections appropriately
            return Type.Parameter.SINGLE_SELECT;
          }
        }
      }
      
      // Try to parse as JSON array (resource format)
      if (value.trim().startsWith("[") && value.trim().endsWith("]")) {
        JsonNode arrayNode = objectMapper.readTree(value);
        
        if (arrayNode.isArray() && arrayNode.size() > 0) {
          // Check if array elements have resource-like structure
          for (JsonNode resourceNode : arrayNode) {
            if (resourceNode.isObject() && 
                (resourceNode.has("objectDisplayName") || 
                 resourceNode.has("objectExternalId") || 
                 resourceNode.has("objectId"))) {
              return Type.Parameter.RESOURCE;
            }
          }
        }
      }
      
    } catch (Exception e) {
      // If JSON parsing fails, return null
      return null;
    }
    
    // Cannot determine parameter type from value structure
    return null;
  }

}
