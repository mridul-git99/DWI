package com.leucine.streem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.*;
import com.leucine.streem.service.HtmlTemplateEngine;
import com.leucine.streem.service.IPdfReportBuilder;
import com.leucine.streem.util.Utility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.List;

import static com.leucine.streem.service.CssClasses.*;

/**
 * Service implementation that generates process template PDFs for checklist definitions.
 *
 * <p>This service creates standalone PDF reports for checklist templates, displaying process
 * information, property values, and checklist state regardless of the checklist's current state.
 * Unlike JobReportBuilder which handles job execution reports, this service focuses specifically
 * on checklist template generation with proper display of all checklist states.</p>
 *
 * <h3>Business Value</h3>
 * <ul>
 *   <li><strong>Process Documentation</strong>: Generates printable process templates for manual
 *       execution and training purposes</li>
 *   <li><strong>Template Generation</strong>: Creates empty templates with proper formatting for
 *       field completion during manual processes</li>
 *   <li><strong>State-Independent Display</strong>: Shows property values and checklist information
 *       regardless of checklist state (BEING_BUILT, PUBLISHED, etc.)</li>
 * </ul>
 *
 * @businessValue Provides independent checklist template PDF generation
 * @compliance Process documentation and template generation
 * @userPersona Process managers, operators, quality personnel
 * @usageScenario Checklist template generation for documentation and manual processes
 * @author Leucine Team
 * @version 1.0
 * @since 1.0
 * @see IPdfReportBuilder
 * @see ChecklistDto
 * @see GeneratedPdfDataDto
 */
@Component
@RequiredArgsConstructor
public class ChecklistReportBuilder implements IPdfReportBuilder {

    private final JobReportBuilder jobReportBuilder; // For selective method reuse

    @Override
    public String buildReport(GeneratedPdfDataDto variables) throws JsonProcessingException {
        StringBuilder reportSection = new StringBuilder();
        ChecklistDto checklist = variables.getChecklistDto();
        
        // Build Process Details with checklist state and property values
        buildProcessDetails(reportSection, checklist);
        
        // Build Job Details with template fields
        buildJobDetails(reportSection, checklist, variables);
        
        // Build Stage and Task count details
        buildStageTaskDetails(reportSection, checklist);
        
        // Build stages sections with template-specific rendering
        buildStagesSections(reportSection, checklist, variables);
        
        return reportSection.toString();
    }

    @Override
    public Type.PdfType getSupportedReportType() {
        return Type.PdfType.PROCESS_TEMPLATE;
    }

    /**
     * Builds the Process Details section with checklist state and property values
     */
    private void buildProcessDetails(StringBuilder sb, ChecklistDto checklist) {
        StringBuilder processDetailsContent = new StringBuilder();
        
        // Basic process information
        processDetailsContent.append(HtmlTemplateEngine.tableRow("Process ID", checklist.getCode()));
        processDetailsContent.append(HtmlTemplateEngine.tableRow("Name", checklist.getName()));
        
        // Add Process State - show checklist state regardless of what it is
        processDetailsContent.append(HtmlTemplateEngine.tableRow("Process State", Utility.toDisplayName(checklist.getState())));

        // Add all property values regardless of checklist state
        List<PropertyValueDto> checklistPropertyValues = checklist.getProperties();
        if (checklistPropertyValues != null) {
            for (PropertyValueDto checklistPropertyValue : checklistPropertyValues) {
                String propertyValue = checklistPropertyValue.getValue() != null ? 
                    String.valueOf(checklistPropertyValue.getValue()) : "____________________";
                processDetailsContent.append(HtmlTemplateEngine.tableRow(
                    checklistPropertyValue.getLabel(),
                    propertyValue
                ));
            }
        }
        
        sb.append(HtmlTemplateEngine.completeSection("Process Details", 
            HtmlTemplateEngine.table(processDetailsContent.toString(), DETAIL_TABLE)));
    }

    /**
     * Builds the Job Details section with template fields - same structure as JobReportBuilder
     */
    private void buildJobDetails(StringBuilder sb, ChecklistDto checklist, GeneratedPdfDataDto variables) throws JsonProcessingException {
        StringBuilder jobDetailsContent = new StringBuilder();
        
        // Exact same structure as JobReportBuilder but with underscores for template
        jobDetailsContent.append(HtmlTemplateEngine.tableRow("Job ID", "____________________"));
        jobDetailsContent.append(HtmlTemplateEngine.tableRow("State", "____________________"));
        jobDetailsContent.append(HtmlTemplateEngine.tableRow("Job Created By", "____________________"));
        jobDetailsContent.append(HtmlTemplateEngine.tableRow("Job Completed By", "____________________"));

        // CJF Parameters - same loop structure as JobReportBuilder
        if (checklist.getParameters() != null) {
            for (ParameterDto p : checklist.getParameters()) {

                // Generate template value instead of actual parameter value
                String templateValue = generateTemplateValue(p);
                boolean isInstructionType = p.getType().equals(Type.Parameter.INSTRUCTION.toString());
                
                if (isInstructionType) {
                    String value = p.getData().get("text").asText();
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    jobDetailsContent.append(HtmlTemplateEngine.tableRowWithRawHtml(p.getLabel(), value));
                } else if (!Utility.isEmpty(templateValue)&&(templateValue.contains("\n") || templateValue.contains("<br>"))) {
                    String formattedValue = templateValue.replace("\n", "<br>");
                    jobDetailsContent.append(HtmlTemplateEngine.tableRowWithRawHtml(p.getLabel(), formattedValue));
                } else {
                    jobDetailsContent.append(HtmlTemplateEngine.tableRow(p.getLabel(), templateValue));
                }
            }
        }

        // Job timing - same fields as JobReportBuilder but with underscores
        jobDetailsContent.append(HtmlTemplateEngine.tableRow("Job Started On", "_______________________"));
        jobDetailsContent.append(HtmlTemplateEngine.tableRow("Job Completed On", "_______________________"));
        jobDetailsContent.append(HtmlTemplateEngine.tableRow("Job Duration", "____________________"));

        sb.append(HtmlTemplateEngine.completeSection("Job Details", 
            HtmlTemplateEngine.table(jobDetailsContent.toString(), DETAIL_TABLE)));
    }

    /**
     * Builds the Stage and Task count details section
     */
    private void buildStageTaskDetails(StringBuilder sb, ChecklistDto checklist) {
        // Simple total counts
        long totalStages = checklist.getStages() != null ? checklist.getStages().size() : 0;
        long totalTasks = checklist.getStages() != null ? 
            checklist.getStages().stream()
                .mapToLong(stage -> stage.getTasks() != null ? stage.getTasks().size() : 0)
                .sum() : 0;
        
        StringBuilder stageTaskDetailsContent = new StringBuilder();
        stageTaskDetailsContent.append(HtmlTemplateEngine.tableRow("Total Stages", String.valueOf(totalStages)));
        stageTaskDetailsContent.append(HtmlTemplateEngine.tableRow("Total Tasks", String.valueOf(totalTasks)));
        
        sb.append(HtmlTemplateEngine.completeSection("Stage and Task Details", 
            HtmlTemplateEngine.table(stageTaskDetailsContent.toString(), DETAIL_TABLE)));
        
        sb.append(HtmlTemplateEngine.pageBreak());
    }

    /**
     * Builds the stages sections with template-specific rendering
     */
    private void buildStagesSections(StringBuilder sb, ChecklistDto checklist, GeneratedPdfDataDto variables) throws JsonProcessingException {
        if (checklist.getStages() == null) {
            return;
        }

        sb.append("<div class=\"stage-details-section\">");

        for (StageDto stage : checklist.getStages()) {
            buildStageSection(sb, stage, variables);
        }
        
        sb.append("</div>");
    }

    /**
     * Builds a single stage section for template display
     */
    private void buildStageSection(StringBuilder sb, StageDto stage, GeneratedPdfDataDto variables) throws JsonProcessingException {
        sb.append(HtmlTemplateEngine.stageHeader(String.valueOf(stage.getOrderTree()), stage.getName()));

        // Build stage instructions
        buildStageInstructions(sb, stage);

        // Build tasks for this stage
        if (stage.getTasks() != null) {
            for (TaskDto task : stage.getTasks()) {
                buildTaskSection(sb, stage, task);
            }
        }

        // Only add page break if this is not the last stage
        ChecklistDto checklist = variables.getChecklistDto();
        List<StageDto> stages = checklist.getStages();
        int currentStageIndex = stages.indexOf(stage);
        if (currentStageIndex < stages.size() - 1) {
            sb.append(HtmlTemplateEngine.pageBreak());
        }
    }

    /**
     * Builds stage instructions section
     */
    private void buildStageInstructions(StringBuilder sb, StageDto stage) {
        List<ParameterDto> stageInstructions = new ArrayList<>();
        
        // Collect all instruction parameters from all tasks in this stage
        if (stage.getTasks() != null) {
            for (TaskDto task : stage.getTasks()) {
                if (task.getParameters() != null) {
                    for (ParameterDto p : task.getParameters()) {
                        if (p.getType().equals(Type.Parameter.INSTRUCTION.toString())) {
                            stageInstructions.add(p);
                        }
                    }
                }
            }
        }

        // Display all instruction parameters at the stage level
        if (!stageInstructions.isEmpty()) {
            sb.append("<div class=\"stage-instructions\">");
            sb.append("<h4>Stage Instructions</h4>");

            for (ParameterDto p : stageInstructions) {
                String value = p.getData().get("text").asText();
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }

                // Find the task that contains this parameter to get the task number
                String taskNumber = "";
                if (stage.getTasks() != null) {
                    for (TaskDto task : stage.getTasks()) {
                        if (task.getParameters() != null) {
                            for (ParameterDto taskParam : task.getParameters()) {
                                if (taskParam.getId().equals(p.getId())) {
                                    taskNumber = stage.getOrderTree() + "." + task.getOrderTree();
                                    break;
                                }
                            }
                            if (!taskNumber.isEmpty()) {
                                break;
                            }
                        }
                    }
                }

                sb.append("<div class=\"instruction-item\">")
                  .append("<div class=\"instruction-label\"><strong>");

                // Add task number prefix in the same format as JobReportBuilder
                if (!taskNumber.isEmpty()) {
                    sb.append("<span class=\"task-number\">[TASK ")
                      .append(HtmlUtils.htmlEscape(taskNumber))
                      .append("]</span>");
                }

                sb.append(HtmlUtils.htmlEscape(p.getLabel()))
                  .append("</strong></div>")
                  .append("<div class=\"instruction-content\">").append(value).append("</div>")
                  .append("</div>");
            }

            sb.append("</div>");
        }
    }

    /**
     * Builds a single task section for template display
     */
    private void buildTaskSection(StringBuilder sb, StageDto stage, TaskDto task) throws JsonProcessingException {
        String taskNo = stage.getOrderTree() + "." + task.getOrderTree();

        sb.append("<h4>Task ")
          .append(taskNo)
          .append(" – ").append(HtmlUtils.htmlEscape(task.getName()))
          .append("</h4>");

        // Template-specific task state (empty for manual completion)
        sb.append("<p style=\"margin: 5px 0;\">Task State: ____________________</p>");
        sb.append("<p style=\"margin: 5px 0;\">Start: _____________________________________________</p>");
        sb.append("<p style=\"margin: 5px 0;\">Complete: _________________________________________</p>");

        // Build parameter table
        sb.append("<table class=\"parameter-table\">")
          .append("<thead><tr>")
          .append("<th>Attribute</th><th>Values</th><th>Person</th><th>Time</th>")
          .append("</tr></thead><tbody>");

        if (task.getParameters() != null) {
            for (ParameterDto p : task.getParameters()) {
                // Skip instruction parameters (already shown in stage instructions)
                if (p.getType().equals(Type.Parameter.INSTRUCTION.toString())) {
                    continue;
                }

                // Generate template value based on parameter type
                String templateValue = generateTemplateValue(p);

                // Get enhanced attribute label for calculation parameters
                String attributeLabel;
                if (Type.Parameter.CALCULATION.toString().equals(p.getType())) {
                    attributeLabel = PdfBuilderServiceHelpers.getCalculationParameterAttributeLabel(p, task.getParameters());
                } else {
                    attributeLabel = p.getLabel();
                }

                sb.append("<tr><td>");
                
                // For calculation parameters, the label might contain HTML
                if (attributeLabel.contains("<br>")) {
                    sb.append(attributeLabel);
                } else {
                    sb.append(HtmlUtils.htmlEscape(attributeLabel));
                }
                
                sb.append("</td><td>").append(templateValue).append("</td>")
                  .append("<td>____________________</td>")
                  .append("<td>____________________</td></tr>");
            }
        }

        sb.append("</tbody></table>");
    }

    /**
     * Generates template value based on parameter type using hybrid approach
     * Uses template-specific logic for select types and existing helper methods for others
     */
    private String generateTemplateValue(ParameterDto parameter) throws JsonProcessingException {
        String parameterType = parameter.getType();

        // Handle select types with template-specific logic to show all options
        if ("SINGLE_SELECT".equals(parameterType) || "MULTI_SELECT".equals(parameterType) ||
            "CHECKLIST".equals(parameterType) || "YES_NO".equals(parameterType)) {
            String value = generateTemplateValueForSelectTypes(parameter);
            return PdfBuilderServiceHelpers.addTemplateVerificationInfo(value, parameter);
        }

        // Use existing logic for other types (DATE, NUMBER, TEXT, etc.)
        ParameterValueDto emptyValue = new ParameterValueDto();
        String value = PdfBuilderServiceHelpers.getParameterValueAsString(parameter, emptyValue, null);
        return PdfBuilderServiceHelpers.addTemplateVerificationInfo(value, parameter);
    }

    /**
     * Generates template values for select-type parameters showing all available options
     */
    private String generateTemplateValueForSelectTypes(ParameterDto parameter) {
        // Use the original helper method for all select types including YES_NO
        return generateChecklistOptions(parameter);
    }

    /**
     * Generates checkbox options for SELECT and CHECKLIST parameters
     */
    private String generateChecklistOptions(ParameterDto parameter) {
        if (parameter.getData() == null) {
            return "____________________";
        }
        
        StringBuilder options = new StringBuilder();

        // Handle direct array structure (most common case)
        if (parameter.getData().isArray()) {
            parameter.getData().forEach(option -> {
                String optionName = option.get("name").asText();
                options.append("☐ ").append(optionName).append("<br>");
            });
        }
        // Handle wrapped structure (fallback)
        else if (parameter.getData().has("options")) {
            parameter.getData().get("options").forEach(option -> {
                String optionName = option.get("name").asText();
                options.append("☐ ").append(optionName).append("<br>");
            });
        }
        else {
            return "____________________";
        }
        
        return options.toString();
    }
}
