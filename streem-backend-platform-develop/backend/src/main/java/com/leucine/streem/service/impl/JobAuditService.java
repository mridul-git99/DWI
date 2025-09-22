package com.leucine.streem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leucine.streem.collections.EntityObject;
import com.leucine.streem.collections.ObjectType;
import com.leucine.streem.collections.partial.PartialEntityObject;
import com.leucine.streem.constant.*;
import com.leucine.streem.constant.Action;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.mapper.IJobAuditMapper;
import com.leucine.streem.dto.projection.ChecklistView;
import com.leucine.streem.dto.request.*;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.ExceptionType;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.JobAuditParameter;
import com.leucine.streem.model.helper.JobAuditParameterValue;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.model.helper.SpecificationBuilder;
import com.leucine.streem.model.helper.parameter.ChoiceParameterBase;
import com.leucine.streem.model.helper.parameter.MediaParameterBase;
import com.leucine.streem.model.helper.parameter.ResourceParameter;
import com.leucine.streem.model.helper.parameter.YesNoParameter;
import com.leucine.streem.model.helper.search.SearchCriteria;
import com.leucine.streem.repository.*;
import com.leucine.streem.service.IJobAuditService;
import com.leucine.streem.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.SetUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.persistence.EntityNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobAuditService implements IJobAuditService {
  public static final String CREATE_JOB = "{0} {1} (ID:{2}) created the Job";
  public static final String START_JOB = "{0} {1} (ID:{2}) started the Job";
  public static final String START_JOB_EARLY = "{0} {1} (ID:{2}) started the job earlier than the scheduled start time of {3}.";
  public static final String COMPLETED_JOB = "{0} {1} (ID:{2}) completed the Job";
  public static final String COMPLETED_JOB_WITH_EXCEPTION = "{0} {1} (ID:{2}) completed the job with exception stating reason \"{3}\"";
  public static final String PRINT_JOB = "{0} {1} (ID:{2}) downloaded a PDF of the Job";

  public static final String PRINT_JOB_ACTIVITY = "{0} {1} (ID:{2}) downloaded Job activity PDF of the Job";

  public static final String PRINT_JOB_REPORT = "{0} {1} (ID:{2}) downloaded a PDF of the Job Summary Report";

  public static final String START_TASK = "{0} {1} (ID:{2}) started the task \"{3}\" as Task{4} of the stage \"{5}\"";
  public static final String START_TASK_PREMATURE = "{0} {1} (ID:{2}) started the task \"{3}\" as Task{4} prematurely stating reason \"{5}\" of the stage \"{6}\"";
  public static final String COMPLETE_TASK = "{0} {1} (ID:{2}) completed the task \"{3}\" as Task{4} {5} {6} {7} of the stage \"{8}\"";
  public static final String SKIP_TASK = "{0} {1} (ID:{2}) skipped the task \"{3}\" as Task{4} stating reason \"{5}\" of the stage \"{6}\"";
  public static final String COMPLETED_TASK_WITH_EXCEPTION = "{0} {1} (ID:{2}) completed the task \"{3}\" as Task{4} with exception {5} {6} {7} of the stage \"{8}\"";

  public static final String ENABLED_TASK_FOR_CORRECTION = "{0} {1} (ID:{2}) enabled the task \"{3}\" as Task{4} for correction stating reason \"{5}\" of the stage \"{6}\"";
  public static final String CANCEL_CORRECTION = "{0} {1} (ID:{2}) cancelled error correction on task \"{3}\" as Task{4} of the stage \"{5}\"";
  public static final String COMPLETE_CORRECTION = "{0} {1} (ID:{2}) completed error correction for task \"{3}\" as Task{4} of the stage \"{5}\"";
  public static final String ASSIGNED_USERS_TO_TASKS = "{0} {1} (ID:{2}) Assigned User(s) to Task(s) in the Job";
  public static final String ASSIGNED_USER_GROUPS_TO_TASKS = "{0} {1} (ID:{2}) Assigned User Group(s) to Task(s) in the Job";
  public static final String UNASSIGNED_USERS_FROM_TASKS = "{0} {1} (ID:{2}) Unassigned User(s) from Task(s) in the Job";
  public static final String SIGNED_OFF_TASKS = "{0} {1} (ID:{2}) signed off on their completed Tasks";
  public static final String UNCHANGED_PARAMETER_CORRECTION = "{0} {1} (ID:{2}) made no correction changes with Remark \"{3}\" and submitted a Correction (ID:{4}) on parameter \"{5}\" in Task{6} \"{7}\" of the stage \"{8}\"";


  public static final String CHOICE_PARAMETER_CHECKED = "{0} {1} (ID:{2}) checked \"{3}\" for parameter \"{4}\" in Task{5} \"{6}\" of the stage \"{7}\"";
  public static final String CHOICE_PARAMETER_CORRECTION_CHECKED = "{0} {1} (ID:{2}) checked \"{3}\" when correcting for parameter \"{4}\" in Task{5} \"{6}\" of the stage \"{7}\"";
  public static final String CHOICE_PARAMETER_UNCHECKED = "{0} {1} (ID:{2}) unchecked \"{3}\" for parameter \"{4}\" in Task{5} \"{6}\" of the stage \"{7}\"";
  public static final String CHOICE_PARAMETER_CORRECTION_UNCHECKED = "{0} {1} (ID:{2}) unchecked \"{3}\" when correcting for parameter \"{4}\" in Task{5} \"{6}\" of the stage \"{7}\"";

  public static final String CHOICE_PARAMETER_SELECTED = "{0} {1} (ID:{2}) selected \"{3}\" for parameter \"{4}\" in Task{5} \"{6}\" of the stage \"{7}\"";
  public static final String CHOICE_PARAMETER_CORRECTION_SELECTED = "{0} {1} (ID:{2}) selected \"{3}\" with Remark \"{4}\" and submitted a Correction (ID:{5}) on parameter \"{6}\" in Task{7} \"{8}\" of the stage \"{9}\"";
  public static final String CHOICE_PARAMETER_DESELECTED = "{0} {1} (ID:{2}) deselected \"{3}\" the parameter \"{4}\" in Task{5} \"{6}\" of the stage \"{7}\"";
  public static final String CHOICE_PARAMETER_CORRECTION_DESELECTED = "{0} {1} (ID:{2}) deselected \"{3}\" with Remark \"{4}\" and submitted a Correction (ID:{5}) on parameter \"{6}\" in Task{7} \"{8}\" of the stage \"{9}\"";

  public static final String CHOICE_PARAMETER_SELECTED_CJF = "{0} {1} (ID:{2}) selected \"{3}\" for parameter \"{4}\" in Create Job Form (CJF) Parameter";
  public static final String CHOICE_PARAMETER_DESELECTED_CJF = "{0} {1} (ID:{2}) deselected \"{3}\" the parameter \"{4}\" in Create Job Form (CJF) Parameter";

  public static final String YES_NO_PARAMETER = "{0} {1} (ID:{2}) selected \"{3}\" for parameter \"{4}\" in Task{5} \"{6}\" of the stage \"{7}\"";
  public static final String YES_NO_PARAMETER_WITH_REASON = "{0} {1} (ID:{2}) selected \"{3}\" stating reason \"{4}\" for parameter \"{5}\" in Task{6} \"{7}\" of the stage \"{8}\"";
  public static final String YES_NO_PARAMETER_CORRECTION = "{0} {1} (ID:{2}) provided \"{3}\" with Remark \"{4}\" and submitted a Correction (ID:{5}) on parameter \"{6}\" in Task{7} \"{8}\" of the stage \"{9}\"";
  public static final String YES_NO_PARAMETER_CORRECTION_WITH_REASON = "{0} {1} (ID:{2}) provided \"{3}\" with Remark \"{4}\" and submitted a Correction (ID:{5}) on parameter \"{6}\" in Task{7} \"{8}\" of the stage \"{9}\"";
  public static final String TEXT_BOX_PARAMETER = "{0} {1} (ID:{2}) updated text input to \"{3}\" for parameter \"{4}\" in Task{5} \"{6}\" of the stage \"{7}\"";
  public static final String TEXT_BOX_PARAMETER_ON_CORRECTION = "{0} {1} (ID:{2}) provided \"{3}\" with remark \"{4}\" and submitted a Correction (ID:{5}) on parameter \"{6}\" in Task{7} \"{8}\" of the stage \"{9}\"";
  public static final String SHOULD_BE_PARAMETER = "{0} {1} (ID:{2}) updated \"{3}\" from \"{4}\" to \"{5}\" in Task{6} \"{7}\" of the stage \"{8}\"";
  public static final String SHOULD_BE_PARAMETER_INITIAL = "{0} {1} (ID:{2}) provided \"{3}\" for \"{4}\" in Task{5} \"{6}\" of the stage \"{7}\"";
  public static final String SHOULD_BE_PARAMETER_WITH_REASON = "{0} {1} (ID:{2}) updated \"{3}\" from \"{4}\" to \"{5}\" stating reason \"{6}\" in Task{7} \"{8}\" of the stage \"{9}\"";
  public static final String SHOULD_BE_PARAMETER_INITIAL_WITH_REASON = "{0} {1} (ID:{2}) provided \"{3}\" for \"{4}\" stating reason \"{5}\" in Task{6} \"{7}\" of the stage \"{8}\"";

  public static final String TEXT_BOX_PARAMETER_CJF = "{0} {1} (ID:{2}) updated text input to \"{3}\" for parameter \"{4}\" in Create Job Form (CJF) Parameter";

  public static final String SHOULD_BE_PARAMETER_CORRECTION = "{0} {1} (ID:{2}) provided a value of \"{3}\" with Remark \"{4}\" and submitted a Correction (ID:{5}) on parameter \"{6}\" in Task{7} \"{8}\" of the stage \"{9}\"";
  public static final String SHOULD_BE_PARAMETER_CORRECTION_INITIAL = "{0} {1} (ID:{2}) provided a value of \"{3}\" with Remark \"{4}\" and submitted a Correction (ID:{5}) on parameter \"{6}\" in Task{7} \"{8}\" of the stage \"{9}\"";
  public static final String SHOULD_BE_PARAMETER_CORRECTION_WITH_REASON = "{0} {1} (ID:{2}) provided a value of \"{3}\" with Remark \"{4}\" and submitted a Correction (ID:{5}) on parameter \"{6}\" in Task{7} \"{8}\" of the stage \"{9}\"";
  public static final String SHOULD_BE_PARAMETER_CORRECTION_INITIAL_WITH_REASON = "{0} {1} (ID:{2}) provided a value of \"{3}\" with Remark \"{4}\" and submitted a Correction (ID:{5}) on parameter \"{6}\" in Task{7} \"{8}\" of the stage \"{9}\"";

  public static final String NUMBER_PARAMETER = "{0} {1} (ID:{2}) updated the value of \"{3}\" to \"{4}\" in Task{5} \"{6}\" of the stage \"{7}\"";
  public static final String NUMBER_PARAMETER_INITIAL = "{0} {1} (ID:{2}) provided a value of \"{3}\" for \"{4}\" in Task{5} \"{6}\" of the stage \"{7}\"";
  public static final String NUMBER_PARAMETER_CORRECTION = "{0} {1} (ID:{2}) provided \"{3}\" with Remark \"{4}\" and submitted a Correction (ID:{5}) on parameter \"{6}\" in Task{7} \"{8}\" of the stage \"{9}\"";
  public static final String NUMBER_PARAMETER_CORRECTION_INITIAL = "{0} {1} (ID:{2}) provided \"{3}\" with Remark \"{4}\" and submitted a Correction (ID:{5}) on parameter \"{6}\" in Task{7} \"{8}\" of the stage \"{9}\"";

  public static final String NUMBER_PARAMETER_INITIAL_CJF = "{0} {1} (ID:{2}) provided a value \"{3}\" for parameter \"{4}\" in Create Job Form (CJF) Parameter";

  // TODO hack to get the date parameter to work, "ABC" is replaced with 0 this needs to be changed, below 4 audits
  public static final String DATE_PARAMETER = "{0} {1} (ID:{2}) updated \"{{{ABC}}}\" for \"{3}\" in Task{4} \"{5}\" of the stage \"{6}\"";
  public static final String DATE_PARAMETER_INITIAL = "{0} {1} (ID:{2}) set \"{{{ABC}}}\" for \"{3}\" in Task{4} \"{5}\" of the stage \"{6}\"";
  public static final String DATE_PARAMETER_CORRECTION = "{0} {1} (ID:{2}) provided \"{{{ABC}}}\" with Remark \"{3}\" and submitted a Correction (ID:{4}) on parameter \"{5}\" in Task{6} \"{7}\" of the stage \"{8}\"";
  public static final String DATE_PARAMETER_CORRECTION_INITIAL = "{0} {1} (ID:{2}) provided \"{{{ABC}}}\" with Remark \"{3}\" and submitted a Correction (ID:{4}) on parameter \"{5}\" in Task{6} \"{7}\" of the stage \"{8}\"";

  public static final String DATE_PARAMETER_CJF = "{0} {1} (ID:{2}) updated \"{{{ABC}}}\" for paramter \"{3}\" in Create Job Form (CJF) Parameter";
  public static final String DATE_PARAMETER_INITIAL_CJF = "{0} {1} (ID:{2}) set \"{{{ABC}}}\" for paramter \"{3}\" in Create Job Form (CJF) Parameter";

  public static final String CALCULATION_PARAMETER = "{0} {1} (ID:{2}) updated the calculated value of \"{3}\" to \"{4}\" for Task{5} \"{6}\" of the stage \"{7}\"";
  public static final String CALCULATION_PARAMETER_INITIAL = "{0} {1} (ID:{2}) calculated the value for \"{3}\" as \"{4}\" for Task{5} \"{6}\" of the stage \"{7}\"";

  public static final String CALCULATION_PARAMETER_CORRECTION = "{0} {1} (ID:{2}) updated the calculated value to \"{3}\" with Remark \"{4}\" and submitted a Correction (ID:{5}) on parameter \"{6}\" in Task{7} \"{8}\" of the stage \"{9}\"";
  public static final String CALCULATION_PARAMETER_CORRECTION_INITIAL = "{0} {1} (ID:{2}) calculated the value to \"{3}\" with Remark \"{4}\" and submitted a Correction (ID:{5}) on parameter \"{6}\" in Task{7} \"{8}\" of the stage \"{9}\"";

  public static final String RESOURCE_PARAMETER = "{0} {1} (ID:{2}) updated selection for \"{3}\" to \"{4}\" for Task{5} \"{6}\" of the stage \"{7}\"";
  public static final String RESOURCE_PARAMETER_INITIAL = "{0} {1} (ID:{2}) selected \"{3}\" for \"{4}\" for Task{5} \"{6}\" of the stage \"{7}\"";
  public static final String RESOURCE_PARAMETER_DESELECTION = "{0} {1} (ID:{2}) deselected \"{3}\" for \"{4}\" for Task{5} \"{6}\" of the stage \"{7}\"";
  public static final String RESOURCE_PARAMETER_CORRECTION = "{0} {1} (ID:{2}) updated selection to \"{3}\" with Remark \"{4}\" and submitted a Correction (ID: {5}) on parameter \"{6}\" in Task{7} \"{9}\" of the stage \"{9}\"";
  public static final String RESOURCE_PARAMETER_CORRECTION_INITIAL = "{0} {1} (ID:{2}) selected \"{3}\" with Remark \"{4}\" and submitted a Correction (ID: {5}) on parameter \"{6}\" in Task{7} \"{8}\" of the stage \"{9}\"";
  public static final String RESOURCE_PARAMETER_CORRECTION_INITIAL_DESELECTED = "{0} {1} (ID:{2}) deselected \"{3}\" with Remark \"{4}\" and submitted a Correction (ID: {5}) on parameter \"{6}\" in Task{7} \"{8}\" of the stage \"{9}\"";

  public static final String RESOURCE_PARAMETER_INITIAL_CJF = "{0} {1} (ID:{2}) selected \"{3}\" for parameter \"{4}\" for Create Job Form (CJF) Parameter";
  public static final String RESOURCE_PARAMETER_DESELECTION_CJF = "{0} {1} (ID:{2}) deselected \"{3}\" for parameter \"{4}\" for Create Job Form (CJF) Parameter";

  public static final String APPROVE_PARAMETER = "{0} {1} (ID:{2}) approved the provided values for \"{3}\" in Task{4} \"{5}\" of the stage \"{6}\"";
  public static final String REJECT_PARAMETER = "{0} {1} (ID:{2}) rejected the provided values for \"{3}\" in Task{4} \"{5}\" of the stage \"{6}\"";

  public static final String SIGNATURE_PARAMETER = "{0} {1} (ID:{2}) updated the signature for parameter \"{3}\" in Task{4} \"{5}\" of the stage \"{6}\"";
  public static final String SIGNATURE_PARAMETER_CORRECTION = "{0} {1} (ID:{2}) updated the signature on correcting the parameter \"{3}\" in Task{4} \"{5}\" of the stage \"{6}\"";
  public static final String MEDIA_PARAMETER = "{0} {1} (ID:{2}) uploaded file {3} for parameter \"{4}\" in Task{5} \"{6}\" of the stage \"{7}\"";
  public static final String MEDIA_PARAMETER_UNCHANGED_CORRECTION = "{0} {1} (ID:{2}) made no correction changes with Remark \"{3}\" on correcting the parameter \"{4}\" in Task{5} \"{6}\" of the stage \"{7}\"";
  public static final String MEDIA_PARAMETER_CORRECTION = "{0} {1} (ID:{2}) uploaded file {3} with Remark \"{4}\" on correcting the parameter \"{5}\" in Task{6} \"{7}\" of the stage \"{8}\"";
  public static final String MEDIA_PARAMETER_ARCHIVED = "{0} {1} (ID:{2}) archived file {3} for parameter \"{4}\" in Task{5} \"{6}\" of the stage \"{7}\"";
  public static final String MEDIA_PARAMETER_ARCHIVED_CORRECTION = "{0} {1} (ID:{2}) archived file {3} on correcting the parameter \"{4}\" in Task{5} \"{6}\" of the stage \"{7}\"";
  public static final String MEDIA_NAME_UPDATED = "{0} {1} (ID:{2}) updated file name from \"{3}\" to \"{4}\" for parameter \"{5}\" in Task{6} \"{7}\" of the stage \"{8}\"";
  public static final String MEDIA_DESCRIPTION_UPDATED = "{0} {1} (ID:{2}) updated description for file \"{3}\" from \"{4}\" to \"{5}\" for parameter \"{6}\" in Task{7} \"{8}\" of the stage \"{9}\"";
  public static final String MEDIA_NAME_AND_DESCRIPTION_UPDATED = "{0} {1} (ID:{2}) updated file name from \"{3}\" to \"{4}\" and description from \"{5}\" to \"{6}\" for parameter \"{7}\" in Task{8} \"{9}\" of the stage \"{10}\"";
  public static final String FILE_UPLOAD_PARAMETER_ARCHIVED = "{0} {1} (ID:{2}) stated reason \"{3}\" to archive file {4} for parameter \"{5}\" in Task{6} \"{7}\" of the stage \"{8}\"";
  public static final String FILE_UPLOAD_PARAMETER_ARCHIVED_CORRECTION = "{0} {1} (ID:{2}) stated reason \"{3}\" to archive file {4} on correcting the parameter \"{5}\" in Task{6} \"{7}\" of the stage \"{8}\"";
  public static final String INITIATE_SELF_VERIFICATION = "{0} {1} (ID:{2}) initiated self verification for the parameter \"{3}\" in Task{4} \"{5}\" of the stage \"{6}\"";
  public static final String INITIATE_SELF_VERIFICATION_CORRECTION = "{0} {1} (ID:{2}) initiated self verification for the parameter \"{3}\" on correcting the Task{4} \"{5}\" of the stage \"{6}\"";
  public static final String APPROVE_SELF_VERIFICATION = "{0} {1} (ID:{2}) approved self verification for the parameter \"{3}\" in Task{4} \"{5}\" of the stage \"{6}\"";
  public static final String APPROVE_SELF_VERIFICATION_CORRECTION = "{0} {1} (ID:{2}) approved self verification for the parameter \"{3}\" on correcting the Task{4} \"{5}\" of the stage \"{6}\"";
  public static final String RECALL_SELF_VERIFICATION = "{0} {1} (ID:{2}) cancel self verification for the parameter \"{3}\" in Task{4} \"{5}\" of the stage \"{6}\"";
  public static final String RECALL_SELF_VERIFICATION_CORRECTION = "{0} {1} (ID:{2}) cancel self verification for the parameter \"{3}\" on correcting the Task{4} \"{5}\" of the stage \"{6}\"";
  public static final String SUBMITTED_FOR_PEER_VERIFICATION = "{0} {1} (ID:{2}) requested  peer verification from \"{7} {8} (ID:{9})\" for the parameter \"{3}\" in Task{4} \"{5}\" of the stage \"{6}\"";
  public static final String SUBMITTED_FOR_PEER_VERIFICATION_CORRECTION = "{0} {1} (ID:{2}) requested  peer verification for the parameter \"{3}\" in Task{4} \"{5}\" of the stage \"{6}\"";
  public static final String RECALL_PEER_VERIFICATION = "{0} {1} (ID:{2}) requesting recall peer verification for the parameter \"{3}\" in Task{4} \"{5}\" of the stage \"{6}\"";
  public static final String RECALL_PEER_VERIFICATION_CORRECTION = "{0} {1} (ID:{2}) requesting recall peer verification for the parameter \"{3}\" on correcting the Task{4} \"{5}\" of the stage \"{6}\"";
  public static final String ACCEPT_PEER_VERIFICATION = "{0} {1} (ID:{2}) approved peer verification for the parameter \"{3}\" in Task{4} \"{5}\" of the stage \"{6}\"";
  public static final String ACCEPT_PEER_VERIFICATION_CORRECTION = "{0} {1} (ID:{2}) approved peer verification for the parameter \"{3}\" on correcting the Task{4} \"{5}\" of the stage \"{6}\"";
  public static final String REJECT_PEER_VERIFICATION = "{0} {1} (ID:{2}) rejected peer verification for the parameter \"{3}\" in Task{4} \"{5}\" of the stage \"{6}\" stating reason \"{7}\"";
  public static final String REJECT_PEER_VERIFICATION_CORRECTION = "{0} {1} (ID:{2}) rejected peer verification for the parameter \"{3}\" on correcting the Task{4} \"{5}\" of the stage \"{6}\" stating reason \"{7}\"";
  public static final String ACCEPT_SAME_SESSION_VERIFICATION = "{0} {1} (ID:{2}) initiated same session verification and {3} {4} (ID:{5}) approved peer verification for the parameter \"{6}\" in Task{7} \"{8}\" of the stage \"{9}\"";
  public static final String ACCEPT_SAME_SESSION_VERIFICATION_CORRECTION = "{0} {1} (ID:{2}) initiated same session verification and {3} {4} (ID:{5}) approved peer verification for the parameter \"{6}\" on correcting the Task{7} \"{8}\" of the stage \"{9}\"";
  public static final String REJECT_SAME_SESSION_VERIFICATION = "{0} {1} (ID:{2}) initiated same session verification and {3} {4} (ID:{5}) rejected peer verification for the parameter \"{6}\" in Task{7} \"{8}\" of the stage \"{9}\" stating reason \"{10}\"";
  public static final String REJECT_SAME_SESSION_VERIFICATION_CORRECTION = "{0} {1} (ID:{2}) initiated same session verification and {3} {4} (ID:{5}) rejected peer verification for the parameter \"{6}\" on correcting the Task{7} \"{8}\" of the stage \"{9}\" stating reason \"{10}\"";
  private static final String PAUSE_TASK = "{0} {1} (ID:{2}) stated reason \"{3}\" to pause the task \"{4}\" of the stage \"{5}\"";
  private static final String RESUME_TASK = "{0} {1} (ID:{2}) resumed the task \"{3}\" of the stage \"{4}\"";
  private static final String REPEAT_TASK = "{0} {1} (ID:{2}) repeated task \"{3}\" of Task{4} as Task{5} of the stage \"{6}\"";
  private static final String RECURRENCE_TASK = "{0} {1} (ID:{2}) recurred task \"{3}\" of Task{4} as Task{5} of the stage \"{6}\"";
  private static final String END_RECURRENCE = "{0} {1} (ID:{2}) recurring task ended for task \"{3}\" of Task{4} of stage \"{5}\"";
  private static final String REMOVE_TASK = "{0} {1} (ID:{2}) removed task \"Task {3}\" of Task{4} of stage \"{5}\"";
  private static final String SCHEDULE_AT_START_TASK = "{0} {1} (ID:{2}) scheduled task \"{3}\" as Task{4} of the stage \"{5}\" on starting task \"{6}\" as Task{7} of stage \"{8}\"";
  private static final String SCHEDULE_AT_COMPLETE_TASK = "{0} {1} (ID:{2}) scheduled task \"{3}\" as Task{4} of the stage \"{5}\" on completing task \"{6}\" as Task{7} of stage \"{8}\"";
  private static final String SCHEDULE_AT_START_JOB = "{0} {1} (ID:{2}) scheduled task \"{3}\" as Task{4} of the stage \"{5}\" on starting job";

  private static final String CREATE_VARIATION = "{0} {1} (ID:{2}) created a planned variation with the name \"{3}\" and number \"{4}\" on parameter \"{5}\" of Task {6} \"{7}\"";
  private static final String DELETE_VARIATION = "{0} {1} (ID:{2}) removed a planned variation with the name \"{3}\" and number \"{4}\" on parameter \"{5}\" of Task {6} \"{7}\" stating reason \"{8}\"";
  public static final String JOB_ANNOTATION_WITH_MEDIA = "{0} {1} (ID:{2}) annotated the Job by providing the following remark: {3} and uploading the following documents: {4}";
  public static final String JOB_ANNOTATION_WITHOUT_MEDIA = "{0} {1} (ID:{2}) annotated the Job by providing the following remark: {3}";
  public static final String JOB_ANNOTATION_DELETE = "{0} {1} (ID:{2}) deleted the Job annotation and remark stating reason \"{3}\"";
  private static final String INCREASE_PROPERTY_AUTOMATION = "{0} {1} (ID:{2}) executed Task Automation \"{3}\": \"{4}\" of \"{5}\" in {6} was increased by {7} in the Task{8} \"{9}\" of the Stage \"{10}\"";
  private static final String SKIP_INCREASE_PROPERTY_AUTOMATION = "{0} {1} (ID:{2}) skipped to execute Task Automation \"{3}\": \"{4}\" of \"{5}\" in {6} was skipped to be increased by \"{7}\" in {8}, due to missing resources in the Task{9} \"{10}\" of the Stage \"{11}\"";
  private static final String DECREASE_PROPERTY_AUTOMATION = "{0} {1} (ID:{2}) executed Task Automation \"{3}\": \"{4}\" of \"{5}\" in {6} was decreased by {7} in the Task{8} \"{9}\" of the Stage \"{10}\"";
  private static final String SKIP_DECREASE_PROPERTY_AUTOMATION = "{0} {1} (ID:{2}) skipped to execute Task Automation \"{3}\": \"{4}\" of \"{5}\" in {6} was skipped to be decreased by \"{7}\" in {8}, due to missing resources in the Task{9} \"{10}\" of the Stage \"{11}\"";
  private static final String SET_PROPERTY_AUTOMATION = "{0} {1} (ID:{2}) executed Task Automation \"{3}\": \"{4}\" of \"{5}\" in {6} was set to \"{7}\" in the Task{8} \"{9}\" of the Stage \"{10}\"";
  private static final String SKIP_SET_PROPERTY_AUTOMATION = "{0} {1} (ID:{2}) skipped to execute Task Automation \"{3}\": \"{4}\" of \"{5}\" in {6} was skipped to be set, due to missing resources in the Task{7} \"{8}\" of the Stage \"{9}\"";
  private static final String SET_RELATION_AUTOMATION = "{0} {1} (ID:{2}) executed Task Automation \"{3}\": \"{4}\" of \"{5}\" (ID:{6}) selected as \"{7}\" in {8} was set as \"{9}\" selected in the \"{10}\" in {11} in Task{12} \"{13}\" of the Stage \"{14}\"";
  private static final String SKIP_SET_RELATION_AUTOMATION = "{0} {1} (ID:{2}) skipped to execute Task Automation \"{3}\": \"{4}\" of \"{5}\" in {6} was skipped to be set as the resource selected in the \"{7}\" parameter in {8} due to missing resources in Task{9} \"{10}\" of the Stage \"{11}\"";
  private static final String CREATE_OBJECT_AUTOMATION = "{0} {1} (ID:{2}) executed Task Automation \"{3}\": New object, {4} (ID:{5}) of Object Type: {6} was created in the Task{8} \"{7}\" of the Stage \"{9}\"";
  private static final String ARCHIVE_OBJECT_AUTOMATION = "{0} {1} (ID:{2}) executed Task Automation \"{3}\": {4} (ID:{5}) of Object Type: {6} in the {7} was archived in the Task{8} \"{9}\" of the Stage \"{10}\"";
  private static final String SKIP_ARCHIVE_OBJECT_AUTOMATION = "{0} {1} (ID:{2}) skipped to execute Task Automation \"{3}\": {4} of Object Type: {5} in the {6} was skipped to be archived in the Task{7} \"{8}\" of the Stage \"{9}\"";
  private static final String TASK_LOCATION_AND_NAME = "Task{0} \"{1}\"";
  private static final String PROCESS_PARAMETER = "Process Parameter";
  private static final String A_CONSTANT = "a constant";
  private static final String UNASSIGNED_USER_GROUPS_FROM_TASKS = "{0} {1} (ID:{2}) Unassigned User Group(s) from Task(s) in the Job";
  public static final String CORRECTION_INITIATED = "{0} {1} (ID:{2}) initiated a Correction (ID:{3}) on Parameter \"{4}\" with Description \"{5}\" for the Task{6} \"{7}\" of the stage \"{8}\"";
  public static final String CORRECTION_APPROVED = "{0} {1} (ID:{2}) approved with Remark \"{3}\" the Correction (ID:{4}) on Parameter \"{5}\" for the Task{6} \"{7}\" of the stage \"{8}\"";
  public static final String CORRECTION_REJECTED = "{0} {1} (ID:{2}) rejected with Remark \"{3}\" the Correction (ID:{4}) on Parameter \"{5}\" for the Task{6} \"{7}\" of the stage \"{8}\"";
  private static final String LOCKED_USER_TO_EXECUTE_TASK = "{0} {1} (ID:{2}) locked {3} {4} (ID:{5}) to execute the Task \"{6}\" as Task{7} of the Stage \"{8}\" and unassigned all previously assigned user(s) and user group(s) from the Task.";
  private static final String CORRECTION_ENABLED = "{0} {1} (ID:{2}) enabled the Task{3} \"{4}\" of the stage \"{5}\" for correction";
  private static final String CORRECTION_DISABLED = "{0} {1} (ID:{2}) disabled the Task{3} \"{4}\" of the stage \"{5}\" for correction";
  private static final String PARAMETER_EXCEPTION_INITIATED = "{0} {1} (ID:{2}) initiated a Exception (ID:{3}) request stating exception reason \"{4}\" for the Parameter \"{5}\" with value as \"{6}\" in Task{7} \"{8}\" of the stage \"{9}\"";
  private static final String DATE_DATE_TIME_PARAMETER_EXCEPTION_INITIATED = "{0} {1} (ID:{2}) initiated a Exception (ID:{3}) request stating exception reason \"{4}\" for the Parameter \"{5}\" with value as \"{{{ABC}}}\" in Task{6} \"{7}\" of the stage \"{8}\"";
  private static final String PARAMETER_EXCEPTION_INITIATED_CJF = "{0} {1} (ID:{2}) initiated a Exception (ID:{3}) request stating exception reason \"{4}\" for the Parameter \"{5}\" with value as \"{6}\" in Create Job Form (CJF)";
  private static final String DATE__DATE_TIME_PARAMETER_EXCEPTION_INITIATED_CJF = "{0} {1} (ID:{2}) initiated a Exception (ID:{3}) request stating exception reason \"{4}\" for the Parameter \"{5}\" with value as \"{{{ABC}}}\" in Create Job Form (CJF)";
  public static final String PARAMETER_EXCEPTION_APPROVED = "{0} {1} (ID:{2}) approved the Exception (ID:{3}) request stating exception reason \"{4}\" for the Parameter \"{5}\" in Task{6} \"{7}\" of the stage \"{8}\"";
  public static final String PARAMETER_EXCEPTION_APPROVED_CJF = "{0} {1} (ID:{2}) approved the Exception (ID:{3}) request stating exception reason \"{4}\" for the Parameter \"{5}\" in Create Job Form (CJF)";
  public static final String PARAMETER_EXCEPTION_REJECTED = "{0} {1} (ID:{2}) rejected the Exception (ID:{3}) request stating exception reason \"{4}\" for the Parameter \"{5}\" in Task{6} \"{7}\" of the stage \"{8}\"";
  public static final String PARAMETER_EXCEPTION_REJECTED_CJF = "{0} {1} (ID:{2}) rejected the Exception (ID:{3}) request stating exception reason \"{4}\" for the Parameter \"{5}\" in Create Job Form (CJF)";
  public static final String PARAMETER_EXCEPTION_AUTO_APPROVED = "{0} {1} (ID:{2}) updated the value of \"{3}\" to \"{4}\" in Task{5} \"{6}\" of the stage \"{7}\" stating exception reason \"{8}\"";
  public static final String DATE_PARAMETER_EXCEPTION_AUTO_APPROVED = "{0} {1} (ID:{2}) updated the value of \"{3}\" to \"{{{ABC}}}\" in Task{4} \"{5}\" of the stage \"{6}\" stating exception reason \"{7}\"";
  public static final String PARAMETER_EXCEPTION_AUTO_APPROVED_CJF = "{0} {1} (ID:{2}) updated the value of \"{3}\" to \"{4}\" in Create Job Form (CJF) stating exception reason \"{5}\"";
  public static final String DATE_PARAMETER_EXCEPTION_AUTO_APPROVED_CJF = "{0} {1} (ID:{2}) updated the value of \"{3}\" to \"{{{ABC}}}\" in Create Job Form (CJF) stating exception reason \"{4}\"";
  private static final String VERIFICATION_CHECKED = "{0} {1} (ID:{2}) checked \"I Confirm\" the verification for the Parameter \"{3}\" in Task{4} \"{5}\" of the stage \"{6}\"";
  public static final String INITIATE_BULK_SELF_VERIFICATION = "{0} {1} (ID:{2}) initiated bulk self verification for the parameter(s) of the task \"{3}\" as Task{4} of the stage \"{5}\"";
  public static final String BULK_SELF_VERIFICATION_PARAMETER_EXAMINED = "{0} {1} (ID:{2}) examined the parameter \"{3}\" in Task{4} \"{5}\" of the stage \"{6}\"";
  public static final String APPROVE_BULK_SELF_VERIFICATION = "{0} {1} (ID:{2}) approved bulk self verification for the parameter(s) of the task \"{3}\" as Task{4} of the stage \"{5}\"";
  public static final String INITIATE_BULK_PEER_VERIFICATION = "{0} {1} (ID:{2}) Requested bulk peer verification for the parameter(s) of the task \"{3}\" as Task{4} of the stage \"{5}\"";
  public static final String BULK_PEER_VERIFICATION_PARAMETER_REVIEWED = "{0} {1} (ID:{2}) reviewed  the parameter \"{3}\" in Task{4} \"{5}\" of the stage \"{6}\"";
  public static final String BULK_SAME_SESSION_VERIFICATION_PARAMETERS_REVIEWED = "{0} {1} (ID:{2}) initiated bulk same session verification and {3} {4} (ID:{5}) reviewed  the parameter \"{6}\" in Task{7} \"{8}\" of the stage \"{9}\"";
  public static final String APPROVE_BULK_PEER_VERIFICATION = "{0} {1} (ID:{2}) approved bulk peer verification for the parameter(s) of the task \"{3}\" as Task{4} of the stage \"{5}\"";
  public static final String USER_ARCHIVAL_UNASSIGNMENT = "{0} {1} (ID:{2}) unassigned {3} {4} (ID:{5}) from non completed tasks in this job due to user archival by {6} {7} (ID:{8}) stating reason \"{9}\"";
  public static final String CORRECTION_RECALLED = "{0} {1} (ID:{2}) recalled a Correction (ID:{3}) on Parameter \"{4}\" with reason \"{5}\" for the Task{6} \"{7}\" of the stage \"{8}\"";
  public static final String APPROVE_BULK_SAME_SESSION_PEER_VERIFICATION = "{0} {1} (ID:{2}) initiated bulk same session verification and {3} {4} (ID:{5}) approved bulk peer verification for the parameter(s) of the task \"{6}\" as Task{7} of the stage \"{8}\"";

  private final IJobAuditRepository jobAuditRepository;
  private final IJobAuditMapper jobAuditMapper;
  private final IStageRepository stageRepository;
  private final ITaskRepository taskRepository;
  private final ObjectMapper objectMapper;
  private final IParameterRepository parameterRepository;
  private final IParameterValueRepository parameterValueRepository;
  private final IVariationRepository variationRepository;
  private final ITaskExecutionRepository taskExecutionRepository;
  private final ICorrectionRepository correctionRepository;
  private final ICorrectionMediaMappingRepository correctionMediaMappingRepository;
  private final IObjectTypeRepository objectTypeRepository;
  private final IEntityObjectRepository entityObjectRepository;
  private final IJobRepository jobRepository;
  private final PdfGeneratorUtil pdfGeneratorUtil;
  private final IFacilityRepository facilityRepository;
  private final IChecklistRepository checklistRepository;
  private final IJobAnnotationRepository jobAnnotationRepository;
  private final IUserRepository userRepository;


  @Override
  public Page<JobAuditDto> getAuditsByJobId(Long jobId, String filters, Pageable pageable) {
    List<Object> values = new ArrayList<>();
    values.add(jobId);
    SearchCriteria mandatorySearchCriteria = new SearchCriteria()
      .setField("jobId")
      .setOp(Operator.Search.EQ.toString())
      .setValues(values);
    Specification<JobAudit> specification = SpecificationBuilder.createSpecification(filters, Collections.singletonList(mandatorySearchCriteria));

    Pageable updatedPageable = Utility.appendSortByIdDesc(pageable);

    Page<JobAudit> jobAudits = jobAuditRepository.findAll(specification, updatedPageable);
    List<JobAuditDto> jobAuditDtoList = jobAuditMapper.toDto(jobAudits.getContent());
    return new PageImpl<>(jobAuditDtoList, pageable, jobAudits.getTotalElements());
  }

  @Override
  public void createJob(String jobId, PrincipalUser principalUser) {
    String details = formatMessage(CREATE_JOB, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId());
    jobAuditRepository.save(getInfoAudit(details, null, Long.parseLong(jobId), null, null, principalUser));
  }

  @Override
  public void startJob(JobInfoDto jobDto, PrincipalUser principalUser) {
    String details = formatMessage(START_JOB, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId());
    jobAuditRepository.save(getInfoAudit(details, null, Long.parseLong(jobDto.getId()), null, null, principalUser));
  }

  public void startJobEarly(JobInfoDto jobDto, PrincipalUser principalUser, String expectedStartStr) {
    String details = formatMessage(START_JOB_EARLY,principalUser.getFirstName(),principalUser.getLastName(),principalUser.getEmployeeId(),expectedStartStr);
    jobAuditRepository.save(getInfoAudit(details, null, Long.parseLong(jobDto.getId()), null, null, principalUser));
  }

  @Override
  public void completeJob(JobInfoDto jobDto, PrincipalUser principalUser) {
    String details = formatMessage(COMPLETED_JOB, principalUser.getFirstName(),
      principalUser.getLastName(), principalUser.getEmployeeId());
    jobAuditRepository.save(getInfoAudit(details, null, Long.parseLong(jobDto.getId()), null, null, principalUser));
  }

  @Override
  public void completeJobWithException(Long jobId, JobCweDetailRequest jobCweDetailRequest, PrincipalUser principalUser) {
    String details = formatMessage(COMPLETED_JOB_WITH_EXCEPTION, principalUser.getFirstName(),
      principalUser.getLastName(), principalUser.getEmployeeId(), jobCweDetailRequest.getReason().get());
    jobAuditRepository.save(getInfoAudit(details, null, jobId, null, null, principalUser));
  }

  @Override
  public void printJob(JobPrintDto jobPrintDto, PrincipalUser principalUser) {
    String details = formatMessage(PRINT_JOB, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId());
    jobAuditRepository.save(getInfoAudit(details, null, Long.parseLong(jobPrintDto.getId()), null, null, principalUser));
  }

  @Override
  public void scheduleTask(Long jobId, Long taskId, PrincipalUser principalUser, boolean isScheduledAtTaskComplete, boolean isScheduledAtStartJob, Set<String> taskScheduledIds) {
    Task schedluedTriggerdByTask = null;
    Stage scheduledTriggeredByStage = null;
    TaskExecution triggertingTaskExecution;
    String taskLocation = "";
    if (!Utility.isEmpty(taskId)) {
      schedluedTriggerdByTask = taskRepository.findById(taskId).get();
      scheduledTriggeredByStage = stageRepository.findByTaskId(taskId);
      triggertingTaskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(taskId, jobId);
      taskLocation = getTaskLocation(scheduledTriggeredByStage.getOrderTree(), schedluedTriggerdByTask.getOrderTree(), triggertingTaskExecution.getOrderTree());
    }
    for (String taskScheduledId : taskScheduledIds) {
      TaskExecution scheduledTaskExecution = taskExecutionRepository.getReferenceById(Long.valueOf(taskScheduledId));
      Task scheduledTask = taskRepository.findById(scheduledTaskExecution.getTaskId()).get();
      Stage scheduledTaskStage = stageRepository.getReferenceById(scheduledTask.getStageId());

      String details;
      String scheduledTaskLocation = getTaskLocation(scheduledTaskStage.getOrderTree(), scheduledTask.getOrderTree(), scheduledTaskExecution.getOrderTree());
      if (!isScheduledAtStartJob) {

        if (isScheduledAtTaskComplete) {
          details = formatMessage(SCHEDULE_AT_COMPLETE_TASK, principalUser.getFirstName(),
            principalUser.getLastName(), principalUser.getEmployeeId(), scheduledTask.getName(),
            scheduledTaskLocation, scheduledTaskStage.getName(), schedluedTriggerdByTask.getName(), taskLocation, scheduledTriggeredByStage.getName());
        } else {
          details = formatMessage(SCHEDULE_AT_START_TASK, principalUser.getFirstName(),
            principalUser.getLastName(), principalUser.getEmployeeId(), scheduledTask.getName(),
            scheduledTaskLocation, scheduledTaskStage.getName(), schedluedTriggerdByTask.getName(), taskLocation, scheduledTriggeredByStage.getName());
        }
      } else {
        details = formatMessage(SCHEDULE_AT_START_JOB, principalUser.getFirstName(),
          principalUser.getLastName(), principalUser.getEmployeeId(), scheduledTask.getName(),
          scheduledTaskLocation, scheduledTaskStage.getName());
      }
      jobAuditRepository.save(getInfoAudit(details, null, jobId, scheduledTaskStage.getId(), taskId, principalUser));
    }
  }

  @Override
  public void handleSoloTaskLock(Task task, Long jobId, PrincipalUser principalUser, PrincipalUser systemUser) {
    Stage stage = stageRepository.findByTaskId(task.getId());
    TaskExecution taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    String currentTaskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    String details = formatMessage(LOCKED_USER_TO_EXECUTE_TASK, systemUser.getFirstName(), systemUser.getLastName(), systemUser.getEmployeeId(),
      principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), task.getName(), currentTaskLocation, task.getStage().getName());
    jobAuditRepository.save(getInfoAudit(details, null, jobId, null, null, principalUser));
  }


  @Override
  public void increaseOrDecreasePropertyAutomation(Long taskId, Long jobId, Type.AutomationActionType automationActionType, String valueToUpdate, Parameter referencedParameter, Parameter parameter, AutomationActionForResourceParameterDto resourceParameterAction, PrincipalUser principalUser, String displayName) {

    Task task = taskRepository.findById(taskId).get();
    Stage stage = stageRepository.findByTaskId(taskId);
    TaskExecution taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(taskId, jobId);
    String currentTaskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String referencedParameterTaskLocationAndName = getTaskLocationAndNameOfParameter(referencedParameter, jobId);

    String parameterValueStatement = "";
    String actionVerb = automationActionType.equals(Type.AutomationActionType.INCREASE_PROPERTY) ? "increasing" : "decreasing";
    String valueStatement = "\"" + valueToUpdate + "\"";

    if (resourceParameterAction.getSelector() == Type.SelectorType.PARAMETER) {
      String parameterTaskLocationAndNameOrConstant = getTaskLocationAndNameOfParameter(parameter, jobId);
      String targetLabel = parameter.getLabel();
      parameterValueStatement = MessageFormat.format("\"{0}\" in {1} {2} the value by {3}", targetLabel, parameterTaskLocationAndNameOrConstant, actionVerb, valueStatement);
    } else if (resourceParameterAction.getSelector() == Type.SelectorType.CONSTANT) {
      parameterValueStatement = A_CONSTANT + " " + valueStatement;
    }

    String message = automationActionType.equals(Type.AutomationActionType.INCREASE_PROPERTY) ? INCREASE_PROPERTY_AUTOMATION : DECREASE_PROPERTY_AUTOMATION;
    String details = formatMessage(message, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
      displayName, resourceParameterAction.getPropertyDisplayName(),
      referencedParameter.getLabel(), referencedParameterTaskLocationAndName,
      parameterValueStatement, currentTaskLocation,
      task.getName(), stage.getName());
    jobAuditRepository.save(getInfoAudit(details, null, jobId, stage.getId(), taskId, principalUser));
  }

  @Override
  public void skipIncreaseOrDecreaseAutomation(Long taskId, Long jobId, Type.AutomationActionType automationActionType, Parameter referencedParameter, Parameter parameter, AutomationActionForResourceParameterDto resourceParameterAction, PrincipalUser principalUser, String displayName) {
    Task task = taskRepository.findById(taskId).get();
    Stage stage = stageRepository.findByTaskId(taskId);
    TaskExecution taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(taskId, jobId);
    String currentTaskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String referencedParameterTaskLocationAndName = getTaskLocationAndNameOfParameter(referencedParameter, jobId);
    String parameterTaskLocationAndNameOrConstant = "";
    String targetLabel = "-";
    if (resourceParameterAction.getSelector() == Type.SelectorType.PARAMETER) {
      parameterTaskLocationAndNameOrConstant = getTaskLocationAndNameOfParameter(parameter, jobId);
      targetLabel = parameter.getLabel();
    } else if (resourceParameterAction.getSelector() == Type.SelectorType.CONSTANT) {
      targetLabel = resourceParameterAction.getValue();
      parameterTaskLocationAndNameOrConstant = A_CONSTANT;
    }

    String message = automationActionType.equals(Type.AutomationActionType.INCREASE_PROPERTY) ? SKIP_INCREASE_PROPERTY_AUTOMATION : SKIP_DECREASE_PROPERTY_AUTOMATION;
    String details = formatMessage(message, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
      displayName, resourceParameterAction.getPropertyDisplayName(),
      referencedParameter.getLabel(), referencedParameterTaskLocationAndName,
      targetLabel, parameterTaskLocationAndNameOrConstant, currentTaskLocation, task.getName(), stage.getName());
    jobAuditRepository.save(getInfoAudit(details, null, jobId, stage.getId(), taskId, principalUser));
  }

  @Override
  public void setPropertyAutomation(Long dateTimeValue, Long taskId, Long jobId, AutomationActionDateTimeDto automationActionDateTimeDto, AutomationActionSetPropertyDto automationSetProperty, Parameter parameter, PrincipalUser principalUser, String displayName) {
    Task task = taskRepository.findById(taskId).get();
    Stage stage = stageRepository.findByTaskId(taskId);
    TaskExecution taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(taskId, jobId);
    String currentTaskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String parameterTaskLocationAndName = getTaskLocationAndNameOfParameter(parameter, jobId);

    String details = "";
    JobAuditParameter jobAuditParameter = null;
    if (automationSetProperty != null) {
      String value = "";
      if (automationSetProperty.getPropertyInputType() == CollectionMisc.PropertyType.NUMBER) {
        if (automationSetProperty.getSelector() == Type.SelectorType.CONSTANT) {
          value = automationSetProperty.getValue();
        } else if (automationSetProperty.getSelector() == Type.SelectorType.PARAMETER) {
          ParameterValue parameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, Long.valueOf(automationSetProperty.getParameterId()));
          value = parameterValue.getValue();
        }
      } else if (automationSetProperty.getPropertyInputType() == CollectionMisc.PropertyType.SINGLE_SELECT) {
        if (automationSetProperty.getSelector() == Type.SelectorType.CONSTANT) {
          value = automationSetProperty.getChoices().get(0).getDisplayName();
        } else if (automationSetProperty.getSelector() == Type.SelectorType.PARAMETER) {
          ParameterValue parameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, Long.valueOf(automationSetProperty.getParameterId()));
          Map<String, String> choices = JsonUtils.convertValue(parameterValue.getChoices(), new TypeReference<Map<String, String>>() {
          });
          String selectedOptionId = choices.entrySet().stream().filter(entry -> entry.getValue().equals("SELECTED")).findFirst().get().getKey();
          String objectTypeId = parameterValue.getParameter().getMetadata().get("objectTypeId").asText();
          String propertyId = parameterValue.getParameter().getMetadata().get("propertyId").asText();
          ObjectType objectType = objectTypeRepository.findById(objectTypeId).get();
          value = objectType.getProperties().stream().filter(property -> property.getId().toString().equals(propertyId)).findFirst().get()
            .getOptions()
            .stream()
            .filter(option -> option.getId().toString().equals(selectedOptionId))
            .findFirst().get()
            .getDisplayName();
        }
      }
      details = formatMessage(SET_PROPERTY_AUTOMATION, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
        displayName, automationSetProperty.getPropertyDisplayName(),
        parameter.getLabel(), parameterTaskLocationAndName,
        value, currentTaskLocation,
        task.getName(), stage.getName());
    } else if (automationActionDateTimeDto != null) {

      String dateAuditValue = "";
      String dateValue = "";
      if (automationActionDateTimeDto.getSelector() == Type.SelectorType.CONSTANT) {
        CollectionMisc.DateUnit offSetDateUnit = automationActionDateTimeDto.getOffsetDateUnit();
        dateAuditValue = "{{{ABC}}}";
        jobAuditParameter = new JobAuditParameter();
        Map<Integer, JobAuditParameterValue> jobAuditParameterValueMap = new HashMap<>();
        JobAuditParameterValue jobAuditParameterValue = new JobAuditParameterValue();
        if (offSetDateUnit == CollectionMisc.DateUnit.DAYS || offSetDateUnit == CollectionMisc.DateUnit.MONTHS || offSetDateUnit == CollectionMisc.DateUnit.YEARS) {
          jobAuditParameterValue.setType(Type.Parameter.DATE);
        } else {
          jobAuditParameterValue.setType(Type.Parameter.DATE_TIME);
        }
        jobAuditParameterValue.setValue(dateTimeValue);
        jobAuditParameterValueMap.put(0, jobAuditParameterValue);
        jobAuditParameter.setParameters(jobAuditParameterValueMap);
      } else if (automationActionDateTimeDto.getSelector() == Type.SelectorType.PARAMETER) {
        ParameterValue parameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, Long.valueOf(automationActionDateTimeDto.getParameterId()));
        dateValue = parameterValue.getValue();
        dateAuditValue = "{{{ABC}}}";
        jobAuditParameter = new JobAuditParameter();
        Map<Integer, JobAuditParameterValue> jobAuditParameterValueMap = new HashMap<>();
        JobAuditParameterValue jobAuditParameterValue = new JobAuditParameterValue();
        jobAuditParameterValue.setType(parameterValue.getParameter().getType());
        jobAuditParameterValue.setValue(dateValue);
        jobAuditParameterValueMap.put(0, jobAuditParameterValue);
        jobAuditParameter.setParameters(jobAuditParameterValueMap);
      }

      details = formatMessage(SET_PROPERTY_AUTOMATION, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
        displayName, automationActionDateTimeDto.getPropertyDisplayName(),
        parameter.getLabel(), parameterTaskLocationAndName,
        dateAuditValue, currentTaskLocation, task.getName(), stage.getName());
      details = details.replace("{{{ABC}}}", "{{{0}}}");
    }

    jobAuditRepository.save(getInfoAudit(details, jobAuditParameter, jobId, stage.getId(), taskId, principalUser));
  }

  @Override
  public void skipSetPropertyAutomation(Long taskId, Long jobId, AutomationActionDateTimeDto automationActionDateTimeDto, AutomationActionSetPropertyDto automationSetProperty, Parameter parameter, PrincipalUser principalUser, String displayName) {
    Task task = taskRepository.findById(taskId).get();
    Stage stage = stageRepository.findByTaskId(taskId);
    TaskExecution taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(taskId, jobId);
    String currentTaskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String parameterTaskLocationAndName = getTaskLocationAndNameOfParameter(parameter, jobId);

    String details = "";
    if (automationSetProperty != null) {
      details = formatMessage(SKIP_SET_PROPERTY_AUTOMATION, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
        displayName, automationSetProperty.getPropertyDisplayName(),
        parameter.getLabel(), parameterTaskLocationAndName, currentTaskLocation,
        task.getName(), stage.getName());
    } else if (automationActionDateTimeDto != null) {
      details = formatMessage(SKIP_SET_PROPERTY_AUTOMATION, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
        displayName, automationActionDateTimeDto.getPropertyDisplayName(),
        parameter.getLabel(), parameterTaskLocationAndName,
        currentTaskLocation, task.getName(), stage.getName());
    }
    jobAuditRepository.save(getInfoAudit(details, null, jobId, stage.getId(), taskId, principalUser));
  }


  @Override
  public void setRelationAutomation(Long taskId, Long jobId, AutomationActionMappedRelationDto automationActionMappedRelationDto, EntityObject entityObject, String resourceParameterChoices, Parameter referencedParameter, Parameter parameter, PrincipalUser principalUser, String displayName) {
    Task task = taskRepository.findById(taskId).get();
    Stage stage = stageRepository.findByTaskId(taskId);
    TaskExecution taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(taskId, jobId);
    String currentTaskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());


    String referencedParameterTaskLocationAndName = getTaskLocationAndNameOfParameter(referencedParameter, jobId);
    String parameterTaskLocationAndName = getTaskLocationAndNameOfParameter(parameter, jobId);

    String details = formatMessage(SET_RELATION_AUTOMATION, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
      displayName, automationActionMappedRelationDto.getRelationDisplayName(), entityObject.getDisplayName(), entityObject.getExternalId(),
      referencedParameter.getLabel(), referencedParameterTaskLocationAndName,
      resourceParameterChoices, parameter.getLabel(), parameterTaskLocationAndName, currentTaskLocation, task.getName(),
      stage.getName());
    jobAuditRepository.save(getInfoAudit(details, null, jobId, stage.getId(), taskId, principalUser));
  }

  @Override
  public void skipSetRelationAutomation(Long taskId, Long jobId, AutomationActionMappedRelationDto automationActionMappedRelationDto, Map<String, List<PartialEntityObject>> partialEntityObjectMap, Parameter referencedParameter, Parameter parameter, PrincipalUser principalUser, String displayName) {
    // Fetch task, stage, and locations
    Task task = taskRepository.findById(taskId).get();
    Stage stage = stageRepository.findByTaskId(taskId);
    TaskExecution taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(taskId, jobId);
    String currentTaskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String referencedParameterTaskLocationAndName = getTaskLocationAndNameOfParameter(referencedParameter, jobId);
    String parameterTaskLocationAndName = getTaskLocationAndNameOfParameter(parameter, jobId);

    String details = formatMessage(SKIP_SET_RELATION_AUTOMATION, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
      displayName, automationActionMappedRelationDto.getRelationDisplayName(), referencedParameter.getLabel(),
      referencedParameterTaskLocationAndName, parameter.getLabel(), parameterTaskLocationAndName,
      currentTaskLocation, task.getName(), stage.getName());
    jobAuditRepository.save(getInfoAudit(details, null, jobId, stage.getId(), taskId, principalUser));
  }

  @Override
  public void createObjectAutomation(Long taskId, Long jobId, EntityObject entityObject, AutomationObjectCreationActionDto automationObjectCreationActionDto, PrincipalUser principalUser, String displayName) {
    Task task = taskRepository.findById(taskId).get();
    Stage stage = stageRepository.findByTaskId(taskId);
    TaskExecution taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(taskId, jobId);
    String currentTaskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String details = formatMessage(CREATE_OBJECT_AUTOMATION, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
      displayName, entityObject.getDisplayName(), entityObject.getExternalId(),
      entityObject.getObjectType().getDisplayName(),
      task.getName(), currentTaskLocation, stage.getName());

    jobAuditRepository.save(getInfoAudit(details, null, jobId, stage.getId(), taskId, principalUser));
  }

  @Override
  public void archiveObjectAutomation(Long taskId, Long jobId, ResourceParameterChoiceDto resourceParameterChoiceDto, Parameter referencedParameter, AutomationActionArchiveObjectDto automationActionArchiveObjectDto, PrincipalUser principalUser, String displayName) throws JsonProcessingException {
    Task task = taskRepository.findById(taskId).get();
    Stage stage = stageRepository.findByTaskId(taskId);
    TaskExecution taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(taskId, jobId);
    String currentTaskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String referencedParameterTaskLocationAndName = getTaskLocationAndNameOfParameter(referencedParameter, jobId);

    ResourceParameter resourceParameter = JsonUtils.readValue(referencedParameter.getData().toString(), ResourceParameter.class);
    String objectTypeDisplayName = resourceParameter.getObjectTypeDisplayName();

    String details = formatMessage(ARCHIVE_OBJECT_AUTOMATION, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
      displayName, referencedParameter.getLabel(),
      resourceParameterChoiceDto.getObjectExternalId(), objectTypeDisplayName,
      referencedParameterTaskLocationAndName, currentTaskLocation, task.getName(), stage.getName());
    jobAuditRepository.save(getInfoAudit(details, null, jobId, stage.getId(), taskId, principalUser));
  }

  @Override
  public void skipArchiveObjectAutomation(Long taskId, Long jobId, Parameter referencedParameter, AutomationActionArchiveObjectDto automationActionArchiveObjectDto, PrincipalUser principalUser, String displayName) throws JsonProcessingException {
    Task task = taskRepository.findById(taskId).get();
    Stage stage = stageRepository.findByTaskId(taskId);
    TaskExecution taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(taskId, jobId);
    String currentTaskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String referencedParameterTaskLocationAndName = getTaskLocationAndNameOfParameter(referencedParameter, jobId);

    ResourceParameter resourceParameter = JsonUtils.readValue(referencedParameter.getData().toString(), ResourceParameter.class);
    String objectTypeDisplayName = resourceParameter.getObjectTypeDisplayName();

    String details = formatMessage(SKIP_ARCHIVE_OBJECT_AUTOMATION, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
      displayName, referencedParameter.getLabel(),
      objectTypeDisplayName, referencedParameterTaskLocationAndName, currentTaskLocation,
      task.getName(), stage.getName());
    jobAuditRepository.save(getInfoAudit(details, null, jobId, stage.getId(), taskId, principalUser));
  }


  @Override
  public void removeTask(Long taskId, Long jobId, Integer orderTree, PrincipalUser principalUser) {
    Task task = taskRepository.findById(taskId).get();
    Stage stage = stageRepository.findByTaskId(taskId);
    TaskExecution taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    String removedTaskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree() + 1);
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    String details = formatMessage(REMOVE_TASK, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), removedTaskLocation, taskLocation, stage.getName());
    jobAuditRepository.save(getInfoAudit(details, null, jobId, stage.getId(), taskId, principalUser));
  }

  @Override
  public void printJobActivity(JobPrintDto jobPrintDto, PrincipalUser principalUser) {
    String details = formatMessage(PRINT_JOB_ACTIVITY, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId());
    jobAuditRepository.save(getInfoAudit(details, null, Long.parseLong(jobPrintDto.getId()), null, null, principalUser));
  }

  @Override
  public void deleteVariation(DeleteVariationRequest deleteVariationRequest, PrincipalUser principalUser) {
    Parameter parameter = parameterRepository.getReferenceById(deleteVariationRequest.getParameterId());
    Task task = taskRepository.getReferenceById(parameter.getTaskId());
    Stage stage = stageRepository.getReferenceById(task.getStageId());
    Variation variation = variationRepository.getReferenceById(deleteVariationRequest.getVariationId());
    TaskExecution taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(parameter.getTaskId(), deleteVariationRequest.getJobId());
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String details = formatMessage(DELETE_VARIATION, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
      variation.getName(), variation.getVariationNumber(), parameter.getLabel(), taskLocation, task.getName(), deleteVariationRequest.getReason());
    jobAuditRepository.save(getInfoAudit(details, null, variation.getJobId(), stage.getId(), task.getId(), principalUser));

  }

  @Override
  public void createVariation(CreateVariationRequest createVariationRequest, PrincipalUser principalUser) {
    ParameterValue parameterValue = parameterValueRepository.getMasterTaskParameterValue(createVariationRequest.getParameterId(), createVariationRequest.getJobId());
    Parameter parameter = parameterValue.getParameter();
    Task task = taskRepository.getReferenceById(parameter.getTaskId());
    Stage stage = stageRepository.getReferenceById(task.getStageId());
    TaskExecution taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(parameter.getTaskId(), createVariationRequest.getJobId());
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    String details = formatMessage(CREATE_VARIATION, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
      createVariationRequest.getName(), createVariationRequest.getVariationNumber(), parameter.getLabel(), taskLocation, task.getName());
    jobAuditRepository.save(getInfoAudit(details, null, parameterValue.getJobId(), stage.getId(), task.getId(), principalUser));
  }

  @Override
  public void saveJobAnnotation(JobAnnotationDto jobAnnotationDto, PrincipalUser principalUser) {
    String details;
    if (Utility.isEmpty(jobAnnotationDto.getMedias())) {
      details = formatMessage(JOB_ANNOTATION_WITHOUT_MEDIA, principalUser.getFirstName(), principalUser.getLastName(),
        principalUser.getEmployeeId(), jobAnnotationDto.getRemarks());
    } else {
      details = formatMessage(JOB_ANNOTATION_WITH_MEDIA, principalUser.getFirstName(), principalUser.getLastName(),
        principalUser.getEmployeeId(), jobAnnotationDto.getRemarks(),
        jobAnnotationDto.getMedias().stream().map(MediaDto::getOriginalFilename).toList().toString());
    }
    jobAuditRepository.save(getInfoAudit(details, null, jobAnnotationDto.getJobId(), null, null, principalUser));
  }

  @Override
  public void deleteJobAnnotation(Long jobId, String reason, PrincipalUser principalUser) {
    String details;
    details = formatMessage(JOB_ANNOTATION_DELETE, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), reason);
    jobAuditRepository.save(getInfoAudit(details, null, jobId, null, null, principalUser));
  }

  @Override
  public void printJobReport(JobReportDto jobReportDto, PrincipalUser principalUser) {
    String details = formatMessage(PRINT_JOB_REPORT, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId());
    jobAuditRepository.save(getInfoAudit(details, null, Long.parseLong(jobReportDto.getId()), null, null, principalUser));
  }

  @Override
  public void recurrenceTask(Long jobId, Long previousTaskRecurrenceId, Long latestTaskRecurrenceId, Long taskId, boolean continueRecurrence, PrincipalUser principalUser) {
    Task task = taskRepository.findById(taskId).get();
    Stage stage = stageRepository.findByTaskId(taskId);
    TaskExecution previousTaskRecurrence = taskExecutionRepository.getReferenceById(previousTaskRecurrenceId);
    TaskExecution latestTaskRecurrence;
    String latestTaskLocation = null;
    if (!Utility.isEmpty(latestTaskRecurrenceId)) {
      latestTaskRecurrence = taskExecutionRepository.getReferenceById(latestTaskRecurrenceId);
      latestTaskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), latestTaskRecurrence.getOrderTree());
    }
    String previousTaskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), previousTaskRecurrence.getOrderTree());

    String details;
    if (continueRecurrence) {
      details = formatMessage(RECURRENCE_TASK, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), task.getName(), previousTaskLocation,
        latestTaskLocation, stage.getName());

    } else {
      details = formatMessage(END_RECURRENCE, principalUser.getFirstName(), principalUser.getLastName(),
        principalUser.getEmployeeId(), task.getName(), latestTaskLocation, stage.getName());
    }
    jobAuditRepository.save(getInfoAudit(details, null, jobId, stage.getId(), taskId, principalUser));
  }

  @Override
  public void repeatTask(TaskRepeatRequest taskRepeatRequest, PrincipalUser principalUser, Long previousTaskExecutionId, Long latestTaskExecutionId) {
    Task task = taskRepository.findById(taskRepeatRequest.getTaskId()).get();
    Stage stage = stageRepository.findByTaskId(taskRepeatRequest.getTaskId());
    TaskExecution previousTaskExecution = taskExecutionRepository.getReferenceById(previousTaskExecutionId);
    TaskExecution latestTaskExecution = taskExecutionRepository.getReferenceById(latestTaskExecutionId);

    String previousTaskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), previousTaskExecution.getOrderTree());
    String latestTaskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), latestTaskExecution.getOrderTree());

    String details;
    details = formatMessage(REPEAT_TASK, principalUser.getFirstName(),
      principalUser.getLastName(), principalUser.getEmployeeId(), task.getName(), previousTaskLocation, latestTaskLocation, stage.getName());


    jobAuditRepository.save(getInfoAudit(details, null, taskRepeatRequest.getJobId(), stage.getId(), taskRepeatRequest.getTaskId(), principalUser));
  }

  @Override
  public void startTask(Long jobId, Long taskId, TaskExecutionRequest taskExecutionRequest, PrincipalUser principalUser) {
    Task task = taskRepository.findById(taskId).get();
    Stage stage = stageRepository.findByTaskId(taskId);
    TaskExecution taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(taskId, jobId);
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    String details;
    if (!Utility.isEmpty(taskExecutionRequest.getRecurringPrematureStartReason())) {
      details = formatMessage(START_TASK_PREMATURE, principalUser.getFirstName(),
        principalUser.getLastName(), principalUser.getEmployeeId(), task.getName(), taskLocation, taskExecutionRequest.getRecurringPrematureStartReason(), stage.getName());
    } else if (!Utility.isEmpty(taskExecutionRequest.getSchedulePrematureStartReason())) {
      details = formatMessage(START_TASK_PREMATURE, principalUser.getFirstName(),
        principalUser.getLastName(), principalUser.getEmployeeId(), task.getName(), taskLocation, taskExecutionRequest.getSchedulePrematureStartReason(), stage.getName());
    } else {
      details = formatMessage(START_TASK, principalUser.getFirstName(),
        principalUser.getLastName(), principalUser.getEmployeeId(), task.getName(), taskLocation, stage.getName());
    }
    jobAuditRepository.save(getInfoAudit(details, null, jobId, stage.getId(), taskId, principalUser));
  }

  @Override
  public void initiateSelfVerification(Long jobId, VerificationBase verificationBase, boolean isVerifiedForCorrection, PrincipalUser principalUser) {
    String details;
    Stage stage;
    Task task;
    TaskExecution taskExecution;

    if (isVerifiedForCorrection) {
      TempParameterVerification tempParameterVerification = (TempParameterVerification) verificationBase;
      task = tempParameterVerification.getTempParameterValue().getParameter().getTask();
      stage = stageRepository.findByTaskId(task.getId());
      taskExecution = taskExecutionRepository.findTaskExecutionEnabledForCorrection(task.getId(), jobId);
      String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
      details = formatMessage(INITIATE_SELF_VERIFICATION_CORRECTION, tempParameterVerification.getCreatedBy().getFirstName(),
        tempParameterVerification.getCreatedBy().getLastName(), tempParameterVerification.getCreatedBy().getEmployeeId(),
        tempParameterVerification.getTempParameterValue().getParameter().getLabel(), taskLocation, task.getName(), stage.getName());
    } else {
      ParameterVerification parameterVerification = (ParameterVerification) verificationBase;
      task = parameterVerification.getParameterValue().getParameter().getTask();
      stage = stageRepository.findByTaskId(task.getId());
      taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
      String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
      details = formatMessage(INITIATE_SELF_VERIFICATION, parameterVerification.getCreatedBy().getFirstName(),
        parameterVerification.getCreatedBy().getLastName(), parameterVerification.getCreatedBy().getEmployeeId(),
        parameterVerification.getParameterValue().getParameter().getLabel(), taskLocation, task.getName(), stage.getName());
    }
    jobAuditRepository.save(getInfoAudit(details, null, jobId, stage.getId(), task.getId(), principalUser));

  }

  @Override
  public void completeSelfVerification(Long jobId, VerificationBase verificationBase, boolean isVerifiedForCorrection, PrincipalUser principalUser) {
    Task task;
    Stage stage;
    String details;
    TaskExecution taskExecution;
    if (isVerifiedForCorrection) {
      TempParameterVerification tempParameterVerification = (TempParameterVerification) verificationBase;
      task = tempParameterVerification.getTempParameterValue().getParameter().getTask();
      stage = stageRepository.findByTaskId(task.getId());
      taskExecution = taskExecutionRepository.findTaskExecutionEnabledForCorrection(task.getId(), jobId);
      String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
      details = formatMessage(APPROVE_SELF_VERIFICATION_CORRECTION, tempParameterVerification.getModifiedBy().getFirstName(), tempParameterVerification.getModifiedBy().getLastName(), tempParameterVerification.getModifiedBy().getEmployeeId(),
        tempParameterVerification.getTempParameterValue().getParameter().getLabel(), taskLocation, task.getName(), stage.getName());
    } else {
      ParameterVerification parameterVerification = (ParameterVerification) verificationBase;
      task = parameterVerification.getParameterValue().getParameter().getTask();
      stage = stageRepository.findByTaskId(task.getId());
      taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
      String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
      details = formatMessage(APPROVE_SELF_VERIFICATION, parameterVerification.getModifiedBy().getFirstName(), parameterVerification.getModifiedBy().getLastName(), parameterVerification.getModifiedBy().getEmployeeId(),
        parameterVerification.getParameterValue().getParameter().getLabel(), taskLocation, task.getName(), stage.getName());
    }
    jobAuditRepository.save(getInfoAudit(details, null, jobId, stage.getId(), task.getId(), principalUser));
  }

  @Override
  public void recallVerification(Long jobId, VerificationBase verificationBase, boolean isVerifiedForCorrection, PrincipalUser principalUser) {
    ParameterVerification parameterVerification = null;
    TempParameterVerification tempParameterVerification = null;
    Task task;
    TaskExecution taskExecution;
    Stage stage;
    if (isVerifiedForCorrection) {
      tempParameterVerification = (TempParameterVerification) verificationBase;
      task = ((TempParameterVerification) verificationBase).getTempParameterValue().getParameter().getTask();
      stage = stageRepository.findByTaskId(task.getId());
      taskExecution = taskExecutionRepository.findTaskExecutionEnabledForCorrection(task.getId(), jobId);
    } else {
      parameterVerification = (ParameterVerification) verificationBase;
      task = ((ParameterVerification) (verificationBase)).getParameterValue().getParameter().getTask();
      stage = stageRepository.findByTaskId(task.getId());
      taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    }

    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());


    Type.VerificationType verificationType = isVerifiedForCorrection ? tempParameterVerification.getVerificationType() : parameterVerification.getVerificationType();
    if (verificationType.equals(Type.VerificationType.PEER)) {
      String details;
      if (isVerifiedForCorrection) {
        details = formatMessage(RECALL_PEER_VERIFICATION_CORRECTION, tempParameterVerification.getModifiedBy().getFirstName(), tempParameterVerification.getModifiedBy().getLastName(), tempParameterVerification.getModifiedBy().getEmployeeId(),
          tempParameterVerification.getTempParameterValue().getParameter().getLabel(), taskLocation, task.getName(), stage.getName());
      } else {
        details = formatMessage(RECALL_PEER_VERIFICATION, parameterVerification.getModifiedBy().getFirstName(), parameterVerification.getModifiedBy().getLastName(), parameterVerification.getModifiedBy().getEmployeeId(),
          parameterVerification.getParameterValue().getParameter().getLabel(), taskLocation, task.getName(), stage.getName());
      }
      jobAuditRepository.save(getInfoAudit(details, null, jobId, stage.getId(), task.getId(), principalUser));
    } else {
      String details;
      if (isVerifiedForCorrection) {
        details = formatMessage(RECALL_SELF_VERIFICATION_CORRECTION, tempParameterVerification.getModifiedBy().getFirstName(), tempParameterVerification.getModifiedBy().getLastName(), tempParameterVerification.getModifiedBy().getEmployeeId(),
          tempParameterVerification.getTempParameterValue().getParameter().getLabel(), taskLocation, task.getName(), stage.getName());
      } else {
        details = formatMessage(RECALL_SELF_VERIFICATION, parameterVerification.getModifiedBy().getFirstName(), parameterVerification.getModifiedBy().getLastName(), parameterVerification.getModifiedBy().getEmployeeId(),
          parameterVerification.getParameterValue().getParameter().getLabel(), taskLocation, task.getName(), stage.getName());
      }
      jobAuditRepository.save(getInfoAudit(details, null, jobId, stage.getId(), task.getId(), principalUser));
    }
  }

  @Override
  public void sendForPeerVerification(Long jobId, VerificationBase verificationBase, boolean isVerifiedForCorrection, PrincipalUser principalUser) {
    TempParameterVerification tempParameterVerification = null;
    ParameterVerification parameterVerification = null;
    Task task = null;
    Stage stage;
    TaskExecution taskExecution;

    if (isVerifiedForCorrection) {
      tempParameterVerification = (TempParameterVerification) verificationBase;
      task = tempParameterVerification.getTempParameterValue().getParameter().getTask();
      stage = stageRepository.findByTaskId(task.getId());
      taskExecution = taskExecutionRepository.findTaskExecutionEnabledForCorrection(task.getId(), jobId);
    } else {
      parameterVerification = (ParameterVerification) verificationBase;
      task = parameterVerification.getParameterValue().getParameter().getTask();
      stage = stageRepository.findByTaskId(task.getId());
      taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    }
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String details = "";
    if (isVerifiedForCorrection) {
      details = formatMessage(SUBMITTED_FOR_PEER_VERIFICATION_CORRECTION, tempParameterVerification.getModifiedBy().getFirstName(), tempParameterVerification.getModifiedBy().getLastName(), tempParameterVerification.getModifiedBy().getEmployeeId(),
        tempParameterVerification.getTempParameterValue().getParameter().getLabel(), taskLocation, task.getName(), stage.getName());
    } else {
      details = formatMessage(SUBMITTED_FOR_PEER_VERIFICATION, parameterVerification.getModifiedBy().getFirstName(), parameterVerification.getModifiedBy().getLastName(), parameterVerification.getModifiedBy().getEmployeeId(),
        parameterVerification.getParameterValue().getParameter().getLabel(), taskLocation, task.getName(), stage.getName(), parameterVerification.getUser().getFirstName(),
        parameterVerification.getUser().getLastName(), parameterVerification.getUser().getEmployeeId());
    }
    jobAuditRepository.save(getInfoAudit(details, null, jobId, stage.getId(), task.getId(), principalUser));
  }

  @Override
  public void acceptPeerVerification(Long jobId, VerificationBase verificationBase, boolean isVerifiedForCorrection, PrincipalUser principalUser) {
    TempParameterVerification tempParameterVerification = null;
    ParameterVerification parameterVerification = null;
    Task task = null;
    Stage stage;
    TaskExecution taskExecution;

    if (isVerifiedForCorrection) {
      tempParameterVerification = (TempParameterVerification) verificationBase;
      task = tempParameterVerification.getTempParameterValue().getParameter().getTask();
      stage = stageRepository.findByTaskId(task.getId());
      taskExecution = taskExecutionRepository.findTaskExecutionEnabledForCorrection(task.getId(), jobId);
    } else {
      parameterVerification = (ParameterVerification) verificationBase;
      task = parameterVerification.getParameterValue().getParameter().getTask();
      stage = stageRepository.findByTaskId(task.getId());
      taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    }
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String details = "";
    if (isVerifiedForCorrection) {
      details = formatMessage(ACCEPT_PEER_VERIFICATION_CORRECTION, tempParameterVerification.getModifiedBy().getFirstName(), tempParameterVerification.getModifiedBy().getLastName(), tempParameterVerification.getModifiedBy().getEmployeeId(),
        tempParameterVerification.getTempParameterValue().getParameter().getLabel(), taskLocation, task.getName(), stage.getName());
    } else {
      details = formatMessage(ACCEPT_PEER_VERIFICATION, parameterVerification.getModifiedBy().getFirstName(), parameterVerification.getModifiedBy().getLastName(), parameterVerification.getModifiedBy().getEmployeeId(),
        parameterVerification.getParameterValue().getParameter().getLabel(), taskLocation, task.getName(), stage.getName());
    }
    jobAuditRepository.save(getInfoAudit(details, null, jobId, stage.getId(), task.getId(), principalUser));
  }


  @Override
  public void resumeTask(Long taskId, TaskPauseOrResumeRequest taskPauseOrResumeRequest, PrincipalUser principalUser) {
    Task task = taskRepository.findById(taskId).get();
    Stage stage = stageRepository.findByTaskId(taskId);
    String details = formatMessage(RESUME_TASK, principalUser.getFirstName(),
      principalUser.getLastName(), principalUser.getEmployeeId(), task.getName(), stage.getName());
    jobAuditRepository.save(getInfoAudit(details, null, taskPauseOrResumeRequest.jobId(), stage.getId(), taskId, principalUser));
  }

  @Override
  public void pauseTask(Long taskId, TaskPauseOrResumeRequest taskPauseOrResumeRequest, PrincipalUser principalUser) {
    Task task = taskRepository.findById(taskId).get();
    Stage stage = stageRepository.findByTaskId(taskId);
    // In case of "OTHER" we are using comment field to store the pause reason, in other cases we are using the reason field
    String pauseReason = Utility.isEmpty(taskPauseOrResumeRequest.comment()) ? taskPauseOrResumeRequest.reason().getText() : taskPauseOrResumeRequest.comment();
    String details = formatMessage(PAUSE_TASK, principalUser.getFirstName(),
      principalUser.getLastName(), principalUser.getEmployeeId(), pauseReason, task.getName(), stage.getName());
    jobAuditRepository.save(getInfoAudit(details, null, taskPauseOrResumeRequest.jobId(), stage.getId(), taskId, principalUser));
  }

  @Override
  public void rejectPeerVerification(Long jobId, VerificationBase verificationBase, boolean isVerifiedForCorrection, PrincipalUser principalUser) {
    TempParameterVerification tempParameterVerification = null;
    ParameterVerification parameterVerification = null;
    Task task;
    TaskExecution taskExecution;
    Stage stage;

    if (isVerifiedForCorrection) {
      tempParameterVerification = (TempParameterVerification) verificationBase;
      task = tempParameterVerification.getTempParameterValue().getParameter().getTask();
      stage = stageRepository.findByTaskId(task.getId());
      taskExecution = taskExecutionRepository.findTaskExecutionEnabledForCorrection(task.getId(), jobId);
    } else {
      parameterVerification = (ParameterVerification) verificationBase;
      task = parameterVerification.getParameterValue().getParameter().getTask();
      stage = stageRepository.findByTaskId(task.getId());
      taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    }

    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String details = "";
    if (isVerifiedForCorrection) {
      details = formatMessage(REJECT_PEER_VERIFICATION_CORRECTION, tempParameterVerification.getModifiedBy().getFirstName(), tempParameterVerification.getModifiedBy().getLastName(), tempParameterVerification.getModifiedBy().getEmployeeId(),
        tempParameterVerification.getTempParameterValue().getParameter().getLabel(), taskLocation, task.getName(), stage.getName(), tempParameterVerification.getComments());
    } else {
      details = formatMessage(REJECT_PEER_VERIFICATION, parameterVerification.getModifiedBy().getFirstName(), parameterVerification.getModifiedBy().getLastName(), parameterVerification.getModifiedBy().getEmployeeId(),
        parameterVerification.getParameterValue().getParameter().getLabel(), taskLocation, task.getName(), stage.getName(), parameterVerification.getComments());
    }
    jobAuditRepository.save(getInfoAudit(details, null, jobId, stage.getId(), task.getId(), principalUser));
  }

  @Override
  public void acceptSameSessionVerification(Long jobId, VerificationBase verificationBase, boolean isVerifiedForCorrection, PrincipalUser verifierUser, PrincipalUser initiatorUser) {
    TempParameterVerification tempParameterVerification = null;
    ParameterVerification parameterVerification = null;
    Task task = null;
    Stage stage;
    TaskExecution taskExecution;

    if (isVerifiedForCorrection) {
      tempParameterVerification = (TempParameterVerification) verificationBase;
      task = tempParameterVerification.getTempParameterValue().getParameter().getTask();
      stage = stageRepository.findByTaskId(task.getId());
      taskExecution = taskExecutionRepository.findTaskExecutionEnabledForCorrection(task.getId(), jobId);
    } else {
      parameterVerification = (ParameterVerification) verificationBase;
      task = parameterVerification.getParameterValue().getParameter().getTask();
      stage = stageRepository.findByTaskId(task.getId());
      taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    }
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String details = "";
    if (isVerifiedForCorrection) {
      details = formatMessage(ACCEPT_SAME_SESSION_VERIFICATION_CORRECTION,
        initiatorUser.getFirstName(), initiatorUser.getLastName(), initiatorUser.getEmployeeId(),
        verifierUser.getFirstName(), verifierUser.getLastName(), verifierUser.getEmployeeId(),
        tempParameterVerification.getTempParameterValue().getParameter().getLabel(), taskLocation, task.getName(), stage.getName());
    } else {
      details = formatMessage(ACCEPT_SAME_SESSION_VERIFICATION,
        initiatorUser.getFirstName(), initiatorUser.getLastName(), initiatorUser.getEmployeeId(),
        verifierUser.getFirstName(), verifierUser.getLastName(), verifierUser.getEmployeeId(),
        parameterVerification.getParameterValue().getParameter().getLabel(), taskLocation, task.getName(), stage.getName());
    }
    jobAuditRepository.save(getInfoAudit(details, null, jobId, stage.getId(), task.getId(), verifierUser));
  }

  @Override
  public void rejectSameSessionVerification(Long jobId, VerificationBase verificationBase, boolean isVerifiedForCorrection, PrincipalUser verifierUser, PrincipalUser initiatorUser) {
    TempParameterVerification tempParameterVerification = null;
    ParameterVerification parameterVerification = null;
    Task task = null;
    Stage stage;
    TaskExecution taskExecution;

    if (isVerifiedForCorrection) {
      tempParameterVerification = (TempParameterVerification) verificationBase;
      task = tempParameterVerification.getTempParameterValue().getParameter().getTask();
      stage = stageRepository.findByTaskId(task.getId());
      taskExecution = taskExecutionRepository.findTaskExecutionEnabledForCorrection(task.getId(), jobId);
    } else {
      parameterVerification = (ParameterVerification) verificationBase;
      task = parameterVerification.getParameterValue().getParameter().getTask();
      stage = stageRepository.findByTaskId(task.getId());
      taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    }
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String details = "";
    if (isVerifiedForCorrection) {
      details = formatMessage(REJECT_SAME_SESSION_VERIFICATION_CORRECTION,
        initiatorUser.getFirstName(), initiatorUser.getLastName(), initiatorUser.getEmployeeId(),
        verifierUser.getFirstName(), verifierUser.getLastName(), verifierUser.getEmployeeId(),
        tempParameterVerification.getTempParameterValue().getParameter().getLabel(), taskLocation, task.getName(), stage.getName(), tempParameterVerification.getComments());
    } else {
      details = formatMessage(REJECT_SAME_SESSION_VERIFICATION,
        initiatorUser.getFirstName(), initiatorUser.getLastName(), initiatorUser.getEmployeeId(),
        verifierUser.getFirstName(), verifierUser.getLastName(), verifierUser.getEmployeeId(),
        parameterVerification.getParameterValue().getParameter().getLabel(), taskLocation, task.getName(), stage.getName(), parameterVerification.getComments());
    }
    jobAuditRepository.save(getInfoAudit(details, null, jobId, stage.getId(), task.getId(), verifierUser));
  }

  @Override
  public void completeTask(Long jobId, Long taskId, TaskCompletionRequest taskCompletionRequest, Integer orderTree, PrincipalUser principalUser) {
    Task task = taskRepository.findById(taskId).get();
    Stage stage = stageRepository.findByTaskId(taskId);
    TaskExecution taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(taskId, jobId);

    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String details = "", reason = "", recurrenceOverDueDetails = "", scheduleOverDueDetails = "";
    boolean isTaskTimerEnabled = task.isTimed();
    if (isTaskTimerEnabled) {
      if (!Utility.isEmpty(task.getMinPeriod()) && isTaskEndedEarly(taskExecution, task)) {
        reason = "early due to \"" + taskCompletionRequest.getReason() + "\"";
      }
      if (!Utility.isEmpty(task.getMaxPeriod()) && !Utility.isNullOrZero(task.getMaxPeriod()) && isTaskEndedDelayed(taskExecution, task)) {
        reason = "delay due to \"" + taskCompletionRequest.getReason() + "\"";
      }
    }
    if (!Utility.isEmpty(taskCompletionRequest.getRecurringOverdueCompletionReason())) {
      recurrenceOverDueDetails = ", stating recurring overdue reason \"" + taskCompletionRequest.getRecurringOverdueCompletionReason() + "\"";
    }
    if (!Utility.isEmpty(taskCompletionRequest.getScheduleOverdueCompletionReason())) {
      scheduleOverDueDetails = ", stating scheduled overdue reason \"" + taskCompletionRequest.getScheduleOverdueCompletionReason() + "\"";
    }

    details = formatMessage(COMPLETE_TASK, principalUser.getFirstName(),
      principalUser.getLastName(), principalUser.getEmployeeId(), task.getName(), taskLocation,
      reason, recurrenceOverDueDetails, scheduleOverDueDetails, stage.getName());


    JobAudit jobAudit = getInfoAudit(details, null, jobId, stage.getId(), taskId, principalUser);
    jobAuditRepository.save(jobAudit);
  }

  private boolean isTaskEndedDelayed(TaskExecution taskExecution, Task task) {
    long totalTime = taskExecution.getEndedAt() - taskExecution.getStartedAt();
    if (Operator.Timer.NOT_LESS_THAN.equals(Operator.Timer.valueOf(task.getTimerOperator()))) {
      return task.getMaxPeriod() < totalTime;
    } else if (Operator.Timer.LESS_THAN.equals(Operator.Timer.valueOf(task.getTimerOperator()))) {
      return task.getMaxPeriod() < totalTime;
    }
    return false;
  }


  private boolean isTaskEndedEarly(TaskExecution taskExecution, Task task) {
    long totalTime = taskExecution.getEndedAt() - taskExecution.getStartedAt();

    if (Operator.Timer.NOT_LESS_THAN.equals(Operator.Timer.valueOf(task.getTimerOperator()))) {
      return task.getMinPeriod() > totalTime;
    }
    return false;
  }


  @Override
  public void completeTaskWithException(Long jobId, Long taskId, TaskCompletionRequest taskCompletionRequest, Integer orderTree, PrincipalUser principalUser) {
    Task task = taskRepository.findById(taskId).get();
    Stage stage = stageRepository.findByTaskId(taskId);
    TaskExecution taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(taskId, jobId);
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String details = "", reason = "", recurrenceOverDueDetails = "", scheduleOverDueDetails = "";
    if (!Utility.isEmpty(taskCompletionRequest.getReason())) {
      reason = "stating reason \"" + taskCompletionRequest.getReason() + "\"";
    }
    if (!Utility.isEmpty(taskCompletionRequest.getRecurringOverdueCompletionReason())) {
      recurrenceOverDueDetails = ", stating recurring overdue reason \"" + taskCompletionRequest.getRecurringOverdueCompletionReason() + "\"";
    }
    if (!Utility.isEmpty(taskCompletionRequest.getScheduleOverdueCompletionReason())) {
      scheduleOverDueDetails = ", stating scheduled overdue reason \"" + taskCompletionRequest.getScheduleOverdueCompletionReason() + "\"";
    }

    details = formatMessage(COMPLETED_TASK_WITH_EXCEPTION, principalUser.getFirstName(),
      principalUser.getLastName(), principalUser.getEmployeeId(), task.getName(), taskLocation,
      reason, recurrenceOverDueDetails, scheduleOverDueDetails, stage.getName());

    JobAudit jobAudit = getInfoAudit(details, null, jobId, stage.getId(), taskId, principalUser);
    jobAuditRepository.save(jobAudit);
  }

  @Override
  public void skipTask(Long jobId, Long taskId, TaskExecutionRequest taskExecutionRequest, PrincipalUser principalUser) {
    Task task = taskRepository.findById(taskId).get();
    Stage stage = stageRepository.findByTaskId(taskId);
    TaskExecution taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(taskId, jobId);
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    String details = formatMessage(SKIP_TASK, principalUser.getFirstName(),
      principalUser.getLastName(), principalUser.getEmployeeId(), task.getName(), taskLocation, taskExecutionRequest.getReason(), stage.getName());
    JobAudit jobAudit = getInfoAudit(details, null, jobId, stage.getId(), taskId, principalUser);
    jobAuditRepository.save(jobAudit);
  }

  @Override
  public void enableTaskForCorrection(Long jobId, Long taskExecutionId, TaskExecutionRequest taskExecutionRequest, PrincipalUser principalUser) {
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);
    Task task = taskExecution.getTask();
    Stage stage = stageRepository.findByTaskId(task.getId());
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String details = formatMessage(ENABLED_TASK_FOR_CORRECTION, principalUser.getFirstName(),
      principalUser.getLastName(), principalUser.getEmployeeId(), task.getName(), taskLocation, taskExecutionRequest.getCorrectionReason(), stage.getName());
    JobAudit jobAudit = getInfoAudit(details, null, jobId, stage.getId(), task.getId(), principalUser);
    jobAuditRepository.save(jobAudit);
  }

  @Override
  public void cancelCorrection(Long jobId, Long taskExecutionId, TaskExecutionRequest taskExecutionRequest, PrincipalUser principalUser) {
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);
    Task task = taskExecution.getTask();
    Stage stage = stageRepository.findByTaskId(task.getId());
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String details = formatMessage(CANCEL_CORRECTION, principalUser.getFirstName(),
      principalUser.getLastName(), principalUser.getEmployeeId(), task.getName(), taskLocation, stage.getName());
    JobAudit jobAudit = getInfoAudit(details, null, jobId, stage.getId(), task.getId(), principalUser);
    jobAuditRepository.save(jobAudit);
  }

  @Override
  public void completeCorrection(Long jobId, Long taskExecutionId, TaskExecutionRequest taskExecutionRequest, PrincipalUser principalUser) {
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);
    Task task = taskExecution.getTask();
    Stage stage = stageRepository.findByTaskId(task.getId());
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String details = formatMessage(COMPLETE_CORRECTION, principalUser.getFirstName(),
      principalUser.getLastName(), principalUser.getEmployeeId(), task.getName(), taskLocation, stage.getName());
    JobAudit jobAudit = getInfoAudit(details, null, jobId, stage.getId(), task.getId(), principalUser);
    jobAuditRepository.save(jobAudit);
  }

  @Override
  public void enableCorrection(Long jobId, Long taskExecutionId, PrincipalUser principalUser) {
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);
    Task task = taskExecution.getTask();
    Stage stage = stageRepository.findByTaskId(task.getId());
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String details = formatMessage(CORRECTION_ENABLED, principalUser.getFirstName(),
      principalUser.getLastName(), principalUser.getEmployeeId(), taskLocation, task.getName(), stage.getName());
    JobAudit jobAudit = getInfoAudit(details, null, jobId, stage.getId(), task.getId(), principalUser);
    jobAuditRepository.save(jobAudit);
  }

  @Override
  public void disableCorrection(Long jobId, Long taskExecutionId, PrincipalUser principalUser) {
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);
    Task task = taskExecution.getTask();
    Stage stage = stageRepository.findByTaskId(task.getId());
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String details = formatMessage(CORRECTION_DISABLED, principalUser.getFirstName(),
      principalUser.getLastName(), principalUser.getEmployeeId(), taskLocation, task.getName(), stage.getName());
    JobAudit jobAudit = getInfoAudit(details, null, jobId, stage.getId(), task.getId(), principalUser);
    jobAuditRepository.save(jobAudit);
  }

  @Override
  public void initiateCorrection(Long jobId, Long taskExecutionId, Long parameterId, PrincipalUser principalUser, Correction savedCorrection, String initiatorReason) {
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);
    Parameter parameter = parameterRepository.getReferenceById(parameterId);
    Task task = taskExecution.getTask();
    Stage stage = stageRepository.findByTaskId(task.getId());
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String details = formatMessage(CORRECTION_INITIATED, principalUser.getFirstName(),
      principalUser.getLastName(), principalUser.getEmployeeId(), savedCorrection.getCode(), parameter.getLabel(), initiatorReason, taskLocation, task.getName(), stage.getName());
    JobAudit jobAudit = getInfoAudit(details, null, jobId, stage.getId(), task.getId(), principalUser);
    jobAuditRepository.save(jobAudit);
  }

  public void approveCorrection(Long jobId, Long taskExecutionId, Long parameterId, PrincipalUser principalUser, Correction savedCorrection, String reviewerReason) {
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);
    Parameter parameter = parameterRepository.getReferenceById(parameterId);
    Task task = taskExecution.getTask();
    Stage stage = stageRepository.findByTaskId(task.getId());
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String details = formatMessage(CORRECTION_APPROVED, principalUser.getFirstName(),
      principalUser.getLastName(), principalUser.getEmployeeId(), reviewerReason, savedCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
    JobAudit jobAudit = getInfoAudit(details, null, jobId, stage.getId(), task.getId(), principalUser);
    jobAuditRepository.save(jobAudit);
  }

  public void rejectCorrection(Long jobId, Long taskExecutionId, Long parameterId, PrincipalUser principalUser, Correction savedCorrection, String reviewerReason) {
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);
    Parameter parameter = parameterRepository.getReferenceById(parameterId);
    Task task = taskExecution.getTask();
    Stage stage = stageRepository.findByTaskId(task.getId());
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String details = formatMessage(CORRECTION_REJECTED, principalUser.getFirstName(),
      principalUser.getLastName(), principalUser.getEmployeeId(), reviewerReason, savedCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
    JobAudit jobAudit = getInfoAudit(details, null, jobId, stage.getId(), task.getId(), principalUser);
    jobAuditRepository.save(jobAudit);
  }

  @Override
  public void initiateParameterException(Long jobId, Long taskExecutionId, Long parameterId, PrincipalUser principalUser, ParameterException parameterException, String initiatorReason, String exceptionDeviatedValue) throws JsonProcessingException {
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);
    Parameter parameter = parameterRepository.getReferenceById(parameterId);
    boolean isDateOrDateTimeParameter = parameter.getType() == Type.Parameter.DATE || parameter.getType() == Type.Parameter.DATE_TIME;
    Task task = taskExecution.getTask();
    Stage stage = stageRepository.findByTaskId(task.getId());
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    JsonNode choice = null;
    if (!Utility.isEmpty(parameterException.getChoices())) {
      choice = parameterException.getChoices();
    }
    String value = exceptionDeviatedValue == null ? getValueFromChoices(choice) : exceptionDeviatedValue;
    JobAudit jobAudit = null;
    String details = null;
    if (isDateOrDateTimeParameter) {
      JobAuditParameter jobAuditParameter = new JobAuditParameter();
      Map<Integer, JobAuditParameterValue> jobAuditParameterValueMap = new HashMap<>();
      JobAuditParameterValue jobAuditParameterValue = new JobAuditParameterValue();
      jobAuditParameterValue.setType(parameter.getType());
      jobAuditParameterValue.setValue(value);
      jobAuditParameterValueMap.put(0, jobAuditParameterValue);
      jobAuditParameter.setParameters(jobAuditParameterValueMap);

      details = formatMessage(DATE_DATE_TIME_PARAMETER_EXCEPTION_INITIATED,
        principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), parameterException.getCode(), initiatorReason, parameter.getLabel(), taskLocation, task.getName(), stage.getName());
      details = details.replace("{{{ABC}}}", "{{{0}}}");
      jobAudit = getInfoAudit(details, jobAuditParameter, jobId, stage.getId(), task.getId(), principalUser);

    } else {
      details = formatMessage(PARAMETER_EXCEPTION_INITIATED, principalUser.getFirstName(),
        principalUser.getLastName(), principalUser.getEmployeeId(), parameterException.getCode(), initiatorReason, parameter.getLabel(), value, taskLocation, task.getName(), stage.getName());
      jobAudit = getInfoAudit(details, null, jobId, stage.getId(), task.getId(), principalUser);
    }

    jobAuditRepository.save(jobAudit);
  }

  public void initiateCjfParameterException(Long jobId, Long parameterId, PrincipalUser principalUser, ParameterException parameterException, String initiatorReason, String exceptionDeviatedValue) throws JsonProcessingException {
    Parameter parameter = parameterRepository.getReferenceById(parameterId);
    boolean isDateOrDateTimeParameter = parameter.getType() == Type.Parameter.DATE || parameter.getType() == Type.Parameter.DATE_TIME;
    JsonNode choice = null;
    if (!Utility.isEmpty(parameterException.getChoices())) {
      choice = parameterException.getChoices();
    }
    String value = exceptionDeviatedValue == null ? getValueFromChoices(choice) : exceptionDeviatedValue;

    String details = null;
    if (isDateOrDateTimeParameter) {
      JobAuditParameter jobAuditParameter = new JobAuditParameter();
      Map<Integer, JobAuditParameterValue> jobAuditParameterValueMap = new HashMap<>();
      JobAuditParameterValue jobAuditParameterValue = new JobAuditParameterValue();
      jobAuditParameterValue.setType(parameter.getType());
      jobAuditParameterValue.setValue(value);
      jobAuditParameterValueMap.put(0, jobAuditParameterValue);
      jobAuditParameter.setParameters(jobAuditParameterValueMap);

      details = formatMessage(DATE__DATE_TIME_PARAMETER_EXCEPTION_INITIATED_CJF,
        principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), parameterException.getCode(), initiatorReason, parameter.getLabel());
      details = details.replace("{{{ABC}}}", "{{{0}}}");
      saveAudit(details, jobAuditParameter, jobId, null, null, principalUser, true);

    } else {
      details = formatMessage(PARAMETER_EXCEPTION_INITIATED_CJF, principalUser.getFirstName(),
        principalUser.getLastName(), principalUser.getEmployeeId(), parameterException.getCode(), initiatorReason, parameter.getLabel(), value);
      JobAudit jobAudit = getInfoAudit(details, null, jobId, principalUser);
      jobAuditRepository.save(jobAudit);
    }
  }

  public void approveParameterException(Long jobId, Long taskExecutionId, Long parameterId, PrincipalUser principalUser, ParameterException parameterException, String reviewerReason) {
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);
    Parameter parameter = parameterRepository.getReferenceById(parameterId);
    Task task = taskExecution.getTask();
    Stage stage = stageRepository.findByTaskId(task.getId());
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String details = formatMessage(PARAMETER_EXCEPTION_APPROVED, principalUser.getFirstName(),
      principalUser.getLastName(), principalUser.getEmployeeId(), parameterException.getCode(), reviewerReason, parameter.getLabel(), taskLocation, task.getName(), stage.getName());
    JobAudit jobAudit = getInfoAudit(details, null, jobId, stage.getId(), task.getId(), principalUser);
    jobAuditRepository.save(jobAudit);
  }

  public void approveCjfParameterException(Long jobId, Long parameterId, PrincipalUser principalUser, ParameterException parameterException, String reviewerReason) {
    Parameter parameter = parameterRepository.getReferenceById(parameterId);
    String details = formatMessage(PARAMETER_EXCEPTION_APPROVED_CJF, principalUser.getFirstName(),
      principalUser.getLastName(), principalUser.getEmployeeId(), parameterException.getCode(), reviewerReason, parameter.getLabel());
    JobAudit jobAudit = getInfoAudit(details, null, jobId, principalUser);
    jobAuditRepository.save(jobAudit);
  }

  public void rejectParameterException(Long jobId, Long taskExecutionId, Long parameterId, PrincipalUser principalUser, ParameterException parameterException, String reviewerReason) {
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);
    Parameter parameter = parameterRepository.getReferenceById(parameterId);
    Task task = taskExecution.getTask();
    Stage stage = stageRepository.findByTaskId(task.getId());
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String details = formatMessage(PARAMETER_EXCEPTION_REJECTED, principalUser.getFirstName(),
      principalUser.getLastName(), principalUser.getEmployeeId(), parameterException.getCode(), reviewerReason, parameter.getLabel(), taskLocation, task.getName(), stage.getName());
    JobAudit jobAudit = getInfoAudit(details, null, jobId, stage.getId(), task.getId(), principalUser);
    jobAuditRepository.save(jobAudit);
  }

  public void rejectCjfParameterException(Long jobId, Long parameterId, PrincipalUser principalUser, ParameterException parameterException, String reviewerReason) {
    Parameter parameter = parameterRepository.getReferenceById(parameterId);
    String details = formatMessage(PARAMETER_EXCEPTION_REJECTED_CJF, principalUser.getFirstName(),
      principalUser.getLastName(), principalUser.getEmployeeId(), parameterException.getCode(), reviewerReason, parameter.getLabel());
    JobAudit jobAudit = getInfoAudit(details, null, jobId, principalUser);
    jobAuditRepository.save(jobAudit);
  }

  @Override
  public void checkVerification(Long parameterExecutionId, Long checkedAt, PrincipalUser principalUser) {
    ParameterValue parameterValue = parameterValueRepository.getReferenceById(parameterExecutionId);
    Parameter parameter = parameterValue.getParameter();
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(parameterValue.getTaskExecutionId());
    Task task = taskRepository.getReferenceById(parameter.getTaskId());
    Stage stage = stageRepository.findByTaskId(task.getId());
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    String details = formatMessage(VERIFICATION_CHECKED, principalUser.getFirstName(),
      principalUser.getLastName(), principalUser.getEmployeeId(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
    JobAudit jobAudit = getInfoAudit(checkedAt, details, null, taskExecution.getJobId(), stage.getId(), task.getId(), principalUser);
    jobAuditRepository.save(jobAudit);
  }

  public void autoAcceptParameterException(Long jobId, Long taskExecutionId, Long parameterId, PrincipalUser principalUser, ParameterException parameterException, String reason) throws JsonProcessingException {
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);
    Parameter parameter = parameterRepository.getReferenceById(parameterId);
    boolean isDateOrDateTimeParameter = parameter.getType() == Type.Parameter.DATE || parameter.getType() == Type.Parameter.DATE_TIME;
    Task task = taskExecution.getTask();
    JsonNode choices = null;
    String value;
    if (!Utility.isEmpty(parameterException.getChoices())) {
      choices = parameterException.getChoices();
      value = getValueFromChoices(choices);
    } else {
      value = parameterException.getValue();
    }
    Stage stage = stageRepository.findByTaskId(task.getId());
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    String details = null;
    JobAudit jobAudit = null;
    if (isDateOrDateTimeParameter) {
      JobAuditParameter jobAuditParameter = new JobAuditParameter();
      Map<Integer, JobAuditParameterValue> jobAuditParameterValueMap = new HashMap<>();
      JobAuditParameterValue jobAuditParameterValue = new JobAuditParameterValue();
      jobAuditParameterValue.setType(parameter.getType());
      jobAuditParameterValue.setValue(value);
      jobAuditParameterValueMap.put(0, jobAuditParameterValue);
      jobAuditParameter.setParameters(jobAuditParameterValueMap);

      details = formatMessage(DATE_PARAMETER_EXCEPTION_AUTO_APPROVED,
        principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), parameter.getLabel(), taskLocation, task.getName(), stage.getName(), reason);
      details = details.replace("{{{ABC}}}", "{{{0}}}");
      jobAudit = getInfoAudit(details, jobAuditParameter, jobId, stage.getId(), task.getId(), principalUser);

    } else {
      details = formatMessage(PARAMETER_EXCEPTION_AUTO_APPROVED, principalUser.getFirstName(),
        principalUser.getLastName(), principalUser.getEmployeeId(), parameter.getLabel(), value, taskLocation, task.getName(), stage.getName(), reason);
      jobAudit = getInfoAudit(details, null, jobId, stage.getId(), task.getId(), principalUser);
    }
    jobAuditRepository.save(jobAudit);
  }

  public void autoAcceptCjfParameterException(Long jobId, Long parameterId, PrincipalUser principalUser, ParameterException parameterException, String reason) throws JsonProcessingException {
    Parameter parameter = parameterRepository.getReferenceById(parameterId);
    boolean isDateOrDateTimeParameter = parameter.getType() == Type.Parameter.DATE || parameter.getType() == Type.Parameter.DATE_TIME;
    JsonNode choices = null;
    String value;
    if (!Utility.isEmpty(parameterException.getChoices())) {
      choices = parameterException.getChoices();
      value = getValueFromChoices(choices);
    } else {
      value = parameterException.getValue();
    }

    if (isDateOrDateTimeParameter) {
      JobAuditParameter jobAuditParameter = new JobAuditParameter();
      Map<Integer, JobAuditParameterValue> jobAuditParameterValueMap = new HashMap<>();
      JobAuditParameterValue jobAuditParameterValue = new JobAuditParameterValue();
      jobAuditParameterValue.setType(parameter.getType());
      jobAuditParameterValue.setValue(value);
      jobAuditParameterValueMap.put(0, jobAuditParameterValue);
      jobAuditParameter.setParameters(jobAuditParameterValueMap);

      String details = formatMessage(DATE_PARAMETER_EXCEPTION_AUTO_APPROVED_CJF, principalUser.getFirstName(),
        principalUser.getLastName(), principalUser.getEmployeeId(), parameter.getLabel(), reason);
      details = details.replace("{{{ABC}}}", "{{{0}}}");
      saveAudit(details, jobAuditParameter, jobId, null, null, principalUser, true);
    } else {
      String details = formatMessage(PARAMETER_EXCEPTION_AUTO_APPROVED_CJF, principalUser.getFirstName(),
        principalUser.getLastName(), principalUser.getEmployeeId(), parameter.getLabel(), value, reason);
      JobAudit jobAudit = getInfoAudit(details, null, jobId, principalUser);
      jobAuditRepository.save(jobAudit);
    }
  }

  @Override
  public void bulkAssignUsersToJob(Long jobId, boolean areUsersAssigned, boolean areUsersUnassigned, boolean areUserGroupsAssigned, boolean areUserGroupsUnAssigned, PrincipalUser principalUser) {
    String details;
    JobAudit jobAudit;

    if (areUsersAssigned) {
      details = formatMessage(ASSIGNED_USERS_TO_TASKS, principalUser.getFirstName(),
        principalUser.getLastName(), principalUser.getEmployeeId());
      jobAudit = getInfoAudit(details, null, jobId, null, null, principalUser);
      jobAuditRepository.save(jobAudit);
    }

    if (areUserGroupsAssigned) {
      details = formatMessage(ASSIGNED_USER_GROUPS_TO_TASKS, principalUser.getFirstName(),
        principalUser.getLastName(), principalUser.getEmployeeId());
      jobAudit = getInfoAudit(details, null, jobId, null, null, principalUser);
      jobAuditRepository.save(jobAudit);
    }

    if (areUsersUnassigned) {
      details = formatMessage(UNASSIGNED_USERS_FROM_TASKS, principalUser.getFirstName(),
        principalUser.getLastName(), principalUser.getEmployeeId());
      jobAudit = getInfoAudit(details, null, jobId, null, null, principalUser);
      jobAuditRepository.save(jobAudit);
    }

    if (areUserGroupsUnAssigned) {
      details = formatMessage(UNASSIGNED_USER_GROUPS_FROM_TASKS, principalUser.getFirstName(),
        principalUser.getLastName(), principalUser.getEmployeeId());
      jobAudit = getInfoAudit(details, null, jobId, null, null, principalUser);
      jobAuditRepository.save(jobAudit);
    }
  }

//  @Async //TODO Removing temporarily because this needs to be call after the transaction is completed

  @Override
  public <T extends BaseParameterValueDto> void executedParameter(Long jobId, Long parameterValueId, Long parameterId, @Nullable T oldValue, List<MediaDto> oldMedias, Type.Parameter parameterType,
                                                                  boolean isExecutedForCorrection, String reason, String correctorRemark, PrincipalUser principalUser, ParameterExecuteRequest parameterExecuteRequest, Boolean isCJF) throws IOException, StreemException {
    Task task = null;
    Stage stage = null;
    if (!isCJF) {
      task = taskRepository.findByParameterId(parameterId);
      stage = stageRepository.findByTaskId(task.getId());
    }
    Parameter parameter = parameterRepository.findById(parameterId).get();
    ParameterValue parameterValue = parameterValueRepository.findById(parameterValueId).get();
    List<ParameterValueMediaMapping> parameterValueMedias = parameterValue.getMedias();
    Correction latestCorrection = correctionRepository.getLatestCorrectionByParameterValueId(parameterValueId);
    List<CorrectionMediaMapping> correctionValueMedias = new ArrayList<>();
    if (isExecutedForCorrection) {
      correctionValueMedias = new ArrayList<>(correctionMediaMappingRepository.findAllByCorrectionId(latestCorrection.getId()));
    }


    switch (parameterType) {
      case CHECKLIST:
        saveChecklistAudit(jobId, stage, task, parameter, parameterValue.getChoices(), oldValue, isExecutedForCorrection, principalUser);
        break;
      case SINGLE_SELECT:
        saveSingleSelectAudit(jobId, stage, task, parameter, isExecutedForCorrection ? latestCorrection.getNewChoices() : parameterValue.getChoices(), oldValue, isExecutedForCorrection, principalUser, latestCorrection, isCJF);
        break;
      case MULTISELECT:
        saveMultiSelectAudit(jobId, stage, task, parameter, isExecutedForCorrection ? latestCorrection.getNewChoices() : parameterValue.getChoices(), oldValue, isExecutedForCorrection, principalUser, latestCorrection, isCJF);
        break;
      case MULTI_LINE:
      case SINGLE_LINE:
        saveTextParameterAudit(jobId, stage, task, parameter, parameterValue.getValue(), isExecutedForCorrection, principalUser, latestCorrection, isCJF);
        break;
      case SHOULD_BE:
        saveShouldBeParameter(jobId, stage, task, parameter, isExecutedForCorrection ? latestCorrection.getNewValue() : parameterValue.getValue(), oldValue, isExecutedForCorrection, reason, principalUser, latestCorrection);
        break;
      case SIGNATURE:
        saveSignatureAudit(jobId, stage, task, parameter, isExecutedForCorrection, principalUser, latestCorrection);
        break;
      case YES_NO:
        saveYesNoAudit(jobId, stage, task, parameter, isExecutedForCorrection ? latestCorrection.getNewChoices() : parameterValue.getChoices(), isExecutedForCorrection, reason, principalUser, latestCorrection);
        break;
      case MEDIA:
        MediaParameterBase mediaParameterBase = isExecutedForCorrection ? null : (MediaParameterBase) JsonUtils.readValue(parameterExecuteRequest.getParameter().getData().toString(), ParameterUtils.getClassForParameter(Type.Parameter.MEDIA));
        saveMediaAudit(jobId, stage, task, parameter, parameterValueMedias, correctionValueMedias, oldMedias, isExecutedForCorrection, principalUser, mediaParameterBase, correctorRemark);
        break;
      case FILE_UPLOAD:
        MediaParameterBase fileUploadParameterBase = isExecutedForCorrection ? null : (MediaParameterBase) JsonUtils.readValue(parameterExecuteRequest.getParameter().getData().toString(), ParameterUtils.getClassForParameter(Type.Parameter.MEDIA));
        saveFileUploadAudit(jobId, stage, task, parameter, parameterValueMedias, correctionValueMedias, oldMedias, isExecutedForCorrection, principalUser, fileUploadParameterBase, correctorRemark);
        break;
      case NUMBER:
        saveNumberParameter(jobId, stage, task, parameter, parameterValue.getValue(), oldValue, isExecutedForCorrection, principalUser, latestCorrection, isCJF);
        break;
      case DATE:
        saveDateParameter(jobId, stage, task, parameter, isExecutedForCorrection ? latestCorrection.getNewValue() : parameterValue.getValue(), oldValue, isExecutedForCorrection, principalUser, latestCorrection, isCJF);
        break;
      case DATE_TIME:
        saveDateTimeParameter(jobId, stage, task, parameter, isExecutedForCorrection ? latestCorrection.getNewValue() : parameterValue.getValue(), oldValue, isExecutedForCorrection, principalUser, latestCorrection, isCJF);
        break;
      case CALCULATION:
        saveCalculationParameter(jobId, stage, task, parameter, parameterValue.getValue(), oldValue, isExecutedForCorrection, parameterValue.getState(), principalUser, latestCorrection);
        break;
      case RESOURCE:
        saveResourceParameter(jobId, stage, task, parameter, isExecutedForCorrection ? latestCorrection.getNewChoices().toString() : parameterValue.getChoices().toString(), oldValue, isExecutedForCorrection, principalUser, latestCorrection, isCJF);
        break;
      case MULTI_RESOURCE:
        saveMultiResourceParameter(jobId, stage, task, parameter, isExecutedForCorrection ? latestCorrection.getNewChoices().toString() : parameterValue.getChoices().toString(), oldValue, isExecutedForCorrection, principalUser, latestCorrection, isCJF);
        break;
    }
  }

  @Override
  public void signedOffTasks(TaskSignOffRequest taskSignOffRequest, PrincipalUser principalUser) {
    String details = formatMessage(SIGNED_OFF_TASKS, principalUser.getFirstName(),
      principalUser.getLastName(), principalUser.getEmployeeId());
    JobAudit jobAudit = getInfoAudit(details, null, taskSignOffRequest.getJobId(), null, null, principalUser);
    jobAuditRepository.save(jobAudit);
  }

  @Override
  public void approveParameter(Long jobId, ParameterDto parameterDto, Long parameterId, PrincipalUser principalUser) {
    Task task = taskRepository.findByParameterId(parameterId);
    Stage stage = stageRepository.findByTaskId(task.getId());

    TaskExecution taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    try {
      String parameter = getAuditParameter(parameterDto);
      String details = formatMessage(APPROVE_PARAMETER, principalUser.getFirstName(),
        principalUser.getLastName(), principalUser.getEmployeeId(), parameter, taskLocation, task.getName(), stage.getName());
      JobAudit jobAudit = getInfoAudit(details, null, jobId, stage.getId(), task.getId(), principalUser);
      jobAuditRepository.save(jobAudit);
    } catch (Exception ex) {
      log.error("[approveParameter] error saving audit", ex);
    }
  }

  @Override
  public void rejectParameter(Long jobId, ParameterDto parameterDto, Long parameterId, PrincipalUser principalUser) {
    Task task = taskRepository.findByParameterId(parameterId);
    Stage stage = stageRepository.findByTaskId(task.getId());

    TaskExecution taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    try {
      String parameter = getAuditParameter(parameterDto);
      String details = formatMessage(REJECT_PARAMETER, principalUser.getFirstName(),
        principalUser.getLastName(), principalUser.getEmployeeId(), parameter, taskLocation, task.getName(), stage.getName());
      JobAudit jobAudit = getInfoAudit(details, null, jobId, stage.getId(), task.getId(), principalUser);
      jobAuditRepository.save(jobAudit);
    } catch (Exception exception) {
      log.error("[rejectParameter] error saving audit", exception);
    }
  }

  @Override
  public void recallCorrection(Long jobId, Long taskExecutionId, Long parameterId, PrincipalUser principalUser, Correction savedCorrection, String recalledReason) {
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);
    Parameter parameter = parameterRepository.getReferenceById(parameterId);
    Task task = taskExecution.getTask();
    Stage stage = stageRepository.findByTaskId(task.getId());
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    String details = formatMessage(CORRECTION_RECALLED, principalUser.getFirstName(),
      principalUser.getLastName(), principalUser.getEmployeeId(), savedCorrection.getCode(), parameter.getLabel(), recalledReason, taskLocation, task.getName(), stage.getName());
    JobAudit jobAudit = getInfoAudit(details, null, jobId, stage.getId(), task.getId(), principalUser);
    jobAuditRepository.save(jobAudit);

  }

  @Override
  public void userUnassignmentLogsForJobs(Set<Long> jobIds, String reason, PrincipalUser systemUser, PrincipalUser archivedUser, PrincipalUser archivedByUser) {
    List<JobAudit> jobAuditsList = new ArrayList<>();

    for (Long jobId : jobIds) {
      String details;
      JobAudit jobAudit;
      details = formatMessage(USER_ARCHIVAL_UNASSIGNMENT, systemUser.getFirstName(),
        systemUser.getLastName(), systemUser.getEmployeeId(), archivedUser.getFirstName(), archivedUser.getLastName(), archivedUser.getEmployeeId(), archivedByUser.getFirstName(), archivedByUser.getLastName(), archivedByUser.getEmployeeId(), reason);
      jobAudit = getInfoAudit(details, null, jobId, null, null, archivedByUser);
      jobAuditsList.add(jobAudit);
    }
    jobAuditRepository.saveAll(jobAuditsList);
  }

  private void saveAudit(String details, JobAuditParameter jobAuditParameter, Long jobId, Long stageId, Long taskId, PrincipalUser principalUser, Boolean isCJF) {
    if (!details.isEmpty()) {
      JobAudit jobAudit;
      if (!isCJF) {
        jobAudit = getInfoAudit(details, jobAuditParameter, jobId, stageId, taskId, Action.Audit.EXECUTE_PARAMETER, principalUser);
      } else {
        jobAudit = getInfoAudit(details, jobAuditParameter, jobId, principalUser);
      }
      jobAuditRepository.save(jobAudit);
    }
  }

  private String getAuditParameter(ParameterDto parameterDto) throws JsonProcessingException {
    switch (Type.Parameter.valueOf(parameterDto.getType())) {
      case SHOULD_BE:
        return parameterDto.getLabel();
      case YES_NO:
        YesNoParameter yesNoParameter = JsonUtils.readValue(parameterDto.getData().toString(), YesNoParameter.class);
        return yesNoParameter.getName();
      default:
        throw new IllegalArgumentException("Incorrect parameter type");
    }
  }

//TODO facility id probably needs to be the selected facility or
// do we need facility id to be saved ?

  private JobAudit getInfoAudit(String details, JobAuditParameter jobAuditParameter, Long jobId, Long stageId, Long taskId, PrincipalUser principalUser) {
    if (jobAuditParameter == null) {
      jobAuditParameter = new JobAuditParameter();
    }
    // TODO Workaround need to fix this
    JsonNode jobParameters = JsonUtils.valueToNode(jobAuditParameter.getParameters());
    return new JobAudit()
      .setDetails(details)
      .setParameters(jobParameters)
      .setTriggeredAt(DateTimeUtils.now())
      .setTriggeredBy(principalUser.getId())
      .setJobId(jobId)
      .setStageId(stageId)
      .setTaskId(taskId)
      .setAction(Action.Audit.EXECUTE_JOB)
      .setOrganisationsId(principalUser.getOrganisationId());

  }

  private JobAudit getInfoAudit(Long checkedAt, String details, JobAuditParameter jobAuditParameter, Long jobId, Long stageId, Long taskId, PrincipalUser principalUser) {
    if (jobAuditParameter == null) {
      jobAuditParameter = new JobAuditParameter();
    }
    // TODO Workaround need to fix this
    JsonNode jobParameters = JsonUtils.valueToNode(jobAuditParameter.getParameters());
    return new JobAudit()
      .setDetails(details)
      .setParameters(jobParameters)
      .setTriggeredAt(checkedAt)
      .setTriggeredBy(principalUser.getId())
      .setJobId(jobId)
      .setStageId(stageId)
      .setTaskId(taskId)
      .setAction(Action.Audit.EXECUTE_JOB)
      .setOrganisationsId(principalUser.getOrganisationId());

  }

  private JobAudit getInfoAudit(String details, JobAuditParameter jobAuditParameter, Long jobId, Long stageId, Long taskId, Action.Audit audit, PrincipalUser principalUser) {
    if (jobAuditParameter == null) {
      jobAuditParameter = new JobAuditParameter();
    }
    // TODO Workaround need to fix this
    JsonNode jobParameters = JsonUtils.valueToNode(jobAuditParameter.getParameters());
    return new JobAudit()
      .setDetails(details)
      .setParameters(jobParameters)
      .setTriggeredAt(DateTimeUtils.now())
      .setTriggeredBy(principalUser.getId())
      .setJobId(jobId)
      .setStageId(stageId)
      .setTaskId(taskId)
      .setAction(audit)
      .setOrganisationsId(principalUser.getOrganisationId());
  }

  private JobAudit getInfoAudit(String details, JobAuditParameter jobAuditParameter, Long jobId, PrincipalUser principalUser) {
    if (jobAuditParameter == null) {
      jobAuditParameter = new JobAuditParameter();
    }
    JsonNode jobParameters = JsonUtils.valueToNode(jobAuditParameter.getParameters());
    return new JobAudit()
      .setDetails(details)
      .setParameters(jobParameters)
      .setTriggeredAt(DateTimeUtils.now())
      .setTriggeredBy(principalUser.getId())
      .setJobId(jobId)
      .setAction(Action.Audit.EXECUTE_PARAMETER)
      .setOrganisationsId(principalUser.getOrganisationId());
  }
//TODO facility id probably needs to be the selected facility or
// do we need facility id to be saved ?

  private JobAudit getInfoAudit(String details, Long jobId, Long stageId, Long taskId, User principalUser) {
    return new JobAudit()
      .setDetails(details)
      .setTriggeredAt(DateTimeUtils.now())
      .setTriggeredAt(DateTimeUtils.now())
      .setTriggeredBy(principalUser.getId())
      .setJobId(jobId)
      .setStageId(stageId)
      .setTaskId(taskId)
      .setAction(Action.Audit.EXECUTE_JOB)
      .setOrganisationsId(principalUser.getOrganisationId());
  }

  private <T extends BaseParameterValueDto> void saveChecklistAudit(Long jobId, Stage stage, Task task, Parameter parameter, JsonNode newData,
                                                                    @Nullable T oldValue, boolean isExecutedForCorrection,
                                                                    PrincipalUser principalUser) throws IOException {
    TaskExecution taskExecution;
    if (!isExecutedForCorrection) {
      taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    } else {
      taskExecution = taskExecutionRepository.findTaskExecutionEnabledForCorrection(task.getId(), jobId);
    }

    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    String details = "";
    List<ChoiceParameterBase> parameters = JsonUtils.jsonToCollectionType(parameter.getData(), List.class, ChoiceParameterBase.class);
    Map<String, ChoiceParameterBase> choicesDetailsMap =
      parameters.stream().collect(Collectors.toMap(ChoiceParameterBase::getId, p -> p));
    if (!Utility.isEmpty(oldValue) && !Utility.isEmpty(oldValue.getChoices())) {
      JsonNode oldChoices = oldValue.getChoices();
      Map<String, String> result = objectMapper.convertValue(oldChoices, new TypeReference<>() {
      });
      Map<String, String> newChoicesMap = objectMapper.convertValue(newData, new TypeReference<>() {
      });
      for (Map.Entry<String, String> newChoice : newChoicesMap.entrySet()) {
        String state = result.get(newChoice.getKey());
        if (!state.equals(newChoice.getValue())) {
          if (!State.Selection.SELECTED.name().equals(state)) {
            details = formatMessage(isExecutedForCorrection ? CHOICE_PARAMETER_CORRECTION_CHECKED : CHOICE_PARAMETER_CHECKED,
              principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
              choicesDetailsMap.get(newChoice.getKey()).getName(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
          } else {
            details = formatMessage(isExecutedForCorrection ? CHOICE_PARAMETER_CORRECTION_UNCHECKED : CHOICE_PARAMETER_UNCHECKED,
              principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
              choicesDetailsMap.get(newChoice.getKey()).getName(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
          }
          break;
        }
      }
    } else {
      Map<String, String> newChoicesMap = objectMapper.convertValue(newData, new TypeReference<>() {
      });
      for (Map.Entry<String, String> newChoice : newChoicesMap.entrySet()) {
        if (State.Selection.SELECTED.name().equals(newChoice.getValue())) {
          details = formatMessage(isExecutedForCorrection ? CHOICE_PARAMETER_CORRECTION_CHECKED : CHOICE_PARAMETER_CHECKED,
            principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
            choicesDetailsMap.get(newChoice.getKey()).getName(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
          break;
        }
      }
    }
    saveAudit(details, null, jobId, stage.getId(), task.getId(), principalUser, false);
  }

  private <T extends BaseParameterValueDto> void saveSingleSelectAudit(Long jobId, Stage stage, Task task, Parameter parameter, JsonNode newData,
                                                                       @Nullable T oldValue, boolean isExecutedForCorrection,
                                                                       PrincipalUser principalUser, Correction latestCorrection, Boolean isCJF) throws IOException, StreemException {
    TaskExecution taskExecution;
    if (isCJF) {
      taskExecution = null;
    } else if (!isExecutedForCorrection) {
      taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    } else {
      taskExecution = taskExecutionRepository.findTaskExecutionEnabledForCorrection(task.getId(), jobId);
    }
    String taskLocation = "";
    if (!isCJF) {
      taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    }
    List<ChoiceParameterBase> parameters = JsonUtils.jsonToCollectionType(parameter.getData(), List.class, ChoiceParameterBase.class);
    Map<String, ChoiceParameterBase> choicesDetailsMap =
      parameters.stream().collect(Collectors.toMap(ChoiceParameterBase::getId, p -> p));
    String selectedItemDetails = "";
    String deselectedItemDetails = "";

    if (!Utility.isEmpty(oldValue) && !Utility.isEmpty(oldValue.getChoices())) {
      JsonNode oldChoices = oldValue.getChoices();
      Map<String, String> result = objectMapper.convertValue(oldChoices, new TypeReference<>() {
      });
      Map<String, String> newChoicesMap = objectMapper.convertValue(newData, new TypeReference<>() {
      });

      if (Utility.isEmpty(newData)) {
        saveUnchangedCorrectionParameter(isCJF, jobId, stage, task, parameter, principalUser, latestCorrection, taskLocation);
      } else {
        var unchangedChoicesCount = 0;
        for (Map.Entry<String, String> newChoice : newChoicesMap.entrySet()) {
          String state = result.getOrDefault(newChoice.getKey(), State.Selection.NOT_SELECTED.toString());

          String choiceName = null;
          if (choicesDetailsMap.get(newChoice.getKey()) != null && choicesDetailsMap.get(newChoice.getKey()).getName() != null) {
            choiceName = choicesDetailsMap.get(newChoice.getKey()).getName();
          } else {
            ValidationUtils.invalidate(parameter.getId(), ErrorCode.SELECT_PARAMETERS_AUTOINITIALIZED_COULD_NOT_BE_EXECUTED);
          }
          if (!state.equals(newChoice.getValue())) {
            if (!State.Selection.SELECTED.name().equals(state)) {
              if (isExecutedForCorrection) {
                selectedItemDetails = formatMessage(CHOICE_PARAMETER_CORRECTION_SELECTED,
                  principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
                  choiceName, latestCorrection.getCorrectorsReason(), latestCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
              } else if (isCJF) {
                selectedItemDetails = formatMessage(CHOICE_PARAMETER_SELECTED_CJF,
                  principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
                  choiceName, parameter.getLabel());
              } else {
                selectedItemDetails = formatMessage(CHOICE_PARAMETER_SELECTED,
                  principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
                  choiceName, parameter.getLabel(), taskLocation, task.getName(), stage.getName());
              }
            } else {
              if (isExecutedForCorrection) {
                deselectedItemDetails = formatMessage(CHOICE_PARAMETER_CORRECTION_DESELECTED,
                  principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
                  choiceName, latestCorrection.getCorrectorsReason(), latestCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
              } else if (isCJF) {
                deselectedItemDetails = formatMessage(CHOICE_PARAMETER_DESELECTED_CJF,
                  principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
                  choiceName, parameter.getLabel());
              } else {
                deselectedItemDetails = formatMessage(CHOICE_PARAMETER_DESELECTED,
                  principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
                  choiceName, parameter.getLabel(), taskLocation, task.getName(), stage.getName());
              }
            }
          } else {
            unchangedChoicesCount++;
          }
        }
        if (unchangedChoicesCount == newChoicesMap.size()) {
          saveUnchangedCorrectionParameter(isCJF, jobId, stage, task, parameter, principalUser, latestCorrection, taskLocation);
        }
      }
      if (isCJF) {
        saveAudit(deselectedItemDetails, null, jobId, null, null, principalUser, isCJF);
        saveAudit(selectedItemDetails, null, jobId, null, null, principalUser, isCJF);
      } else {
        saveAudit(deselectedItemDetails, null, jobId, stage.getId(), task.getId(), principalUser, false);
        saveAudit(selectedItemDetails, null, jobId, stage.getId(), task.getId(), principalUser, false);
      }

    } else {
      Map<String, String> newChoicesMap = objectMapper.convertValue(newData, new TypeReference<>() {
      });
      for (Map.Entry<String, String> newChoice : newChoicesMap.entrySet()) {

        String choiceName = null;
        if (choicesDetailsMap.get(newChoice.getKey()) != null && choicesDetailsMap.get(newChoice.getKey()).getName() != null) {
          choiceName = choicesDetailsMap.get(newChoice.getKey()).getName();
        } else {
          ValidationUtils.invalidate(parameter.getId(), ErrorCode.SELECT_PARAMETERS_AUTOINITIALIZED_COULD_NOT_BE_EXECUTED);
        }

        if (State.Selection.SELECTED.name().equals(newChoice.getValue())) {
          if (isExecutedForCorrection) {
            selectedItemDetails = formatMessage(CHOICE_PARAMETER_CORRECTION_SELECTED,
              principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
              choiceName, latestCorrection.getCorrectorsReason(), latestCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
          } else if (isCJF) {
            selectedItemDetails = formatMessage(CHOICE_PARAMETER_SELECTED_CJF,
              principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
              choicesDetailsMap.get(newChoice.getKey()).getName(), parameter.getLabel());
          } else {
            selectedItemDetails = formatMessage(CHOICE_PARAMETER_SELECTED,
              principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
              choiceName, parameter.getLabel(), taskLocation, task.getName(), stage.getName());
          }
          if (isCJF) {
            saveAudit(selectedItemDetails, null, jobId, null, null, principalUser, isCJF);
          } else {
            saveAudit(selectedItemDetails, null, jobId, stage.getId(), task.getId(), principalUser, false);
          }
        }
      }
    }
  }

  private <T extends BaseParameterValueDto> void saveMultiSelectAudit(Long jobId, Stage stage, Task task, Parameter parameter, JsonNode newData,
                                                                      @Nullable T oldValue, boolean isExecutedForCorrection,
                                                                      PrincipalUser principalUser, Correction latestCorrection, Boolean isCJF) throws IOException {
    TaskExecution taskExecution;
    if (isCJF) {
      taskExecution = null;
    } else if (!isExecutedForCorrection) {
      taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    } else {
      taskExecution = taskExecutionRepository.findTaskExecutionEnabledForCorrection(task.getId(), jobId);
    }
    String taskLocation = "";
    if (!isCJF) {
      taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    }
    List<ChoiceParameterBase> parameters = JsonUtils.jsonToCollectionType(parameter.getData(), List.class, ChoiceParameterBase.class);
    Map<String, ChoiceParameterBase> choicesDetailsMap =
      parameters.stream().collect(Collectors.toMap(ChoiceParameterBase::getId, p -> p));
    String details = "";

    if (!Utility.isEmpty(oldValue) && !Utility.isEmpty(oldValue.getChoices())) {
      JsonNode oldChoices = oldValue.getChoices();
      Map<String, String> oldChoicesMap = objectMapper.convertValue(oldChoices, new TypeReference<>() {
      });
      Map<String, String> newChoicesMap = objectMapper.convertValue(newData, new TypeReference<>() {
      });
      if (Utility.isEmpty(newData)) {
        saveUnchangedCorrectionParameter(isCJF, jobId, stage, task, parameter, principalUser, latestCorrection, taskLocation);
      } else {
        if (!Utility.isEmpty(newChoicesMap)) {
          var unchangedChoicesCount = 0;
          for (Map.Entry<String, String> newChoice : newChoicesMap.entrySet()) {
            String state = oldChoicesMap.get(newChoice.getKey());
            if (!state.equals(newChoice.getValue())) {
              if (!State.Selection.SELECTED.name().equals(state)) {
                if (isExecutedForCorrection) {
                  details = formatMessage(CHOICE_PARAMETER_CORRECTION_SELECTED,
                    principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
                    choicesDetailsMap.get(newChoice.getKey()).getName(), latestCorrection.getCorrectorsReason(), latestCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
                } else if (isCJF) {
                  details = formatMessage(CHOICE_PARAMETER_SELECTED_CJF,
                    principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
                    choicesDetailsMap.get(newChoice.getKey()).getName(), parameter.getLabel());
                } else {
                  details = formatMessage(CHOICE_PARAMETER_SELECTED,
                    principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
                    choicesDetailsMap.get(newChoice.getKey()).getName(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
                }
                if (isCJF) {
                  saveAudit(details, null, jobId, null, null, principalUser, isCJF);
                } else {
                  saveAudit(details, null, jobId, stage.getId(), task.getId(), principalUser, false);
                }
              } else {
                if (isExecutedForCorrection) {
                  details = formatMessage(CHOICE_PARAMETER_CORRECTION_DESELECTED, principalUser.getFirstName(),
                    principalUser.getLastName(), principalUser.getEmployeeId(), choicesDetailsMap.get(newChoice.getKey()).getName(), latestCorrection.getCorrectorsReason(), latestCorrection.getCode(),
                    parameter.getLabel(), taskLocation, task.getName(), stage.getName());
                } else if (isCJF) {
                  details = formatMessage(CHOICE_PARAMETER_DESELECTED_CJF,
                    principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
                    choicesDetailsMap.get(newChoice.getKey()).getName(), parameter.getLabel());
                } else {
                  details = formatMessage(CHOICE_PARAMETER_DESELECTED, principalUser.getFirstName(),
                    principalUser.getLastName(), principalUser.getEmployeeId(), choicesDetailsMap.get(newChoice.getKey()).getName(),
                    parameter.getLabel(), taskLocation, task.getName(), stage.getName());
                }
                if (isCJF) {
                  saveAudit(details, null, jobId, null, null, principalUser, isCJF);
                } else {
                  saveAudit(details, null, jobId, stage.getId(), task.getId(), principalUser, false);
                }
              }
            } else {
              unchangedChoicesCount++;
            }
          }
          if (unchangedChoicesCount == newChoicesMap.size()) {
            saveUnchangedCorrectionParameter(isCJF, jobId, stage, task, parameter, principalUser, latestCorrection, taskLocation);
          }
        }
      }

    } else {
      Map<String, String> newChoicesMap = objectMapper.convertValue(newData, new TypeReference<>() {
      });
      for (Map.Entry<String, String> newChoice : newChoicesMap.entrySet()) {
        if (State.Selection.SELECTED.name().equals(newChoice.getValue())) {
          if (isExecutedForCorrection) {
            details = formatMessage(CHOICE_PARAMETER_CORRECTION_SELECTED,
              principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
              choicesDetailsMap.get(newChoice.getKey()).getName(), latestCorrection.getCorrectorsReason(), latestCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
          } else if (isCJF) {
            details = formatMessage(CHOICE_PARAMETER_SELECTED_CJF,
              principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
              choicesDetailsMap.get(newChoice.getKey()).getName(), parameter.getLabel());
          } else {
            details = formatMessage(CHOICE_PARAMETER_SELECTED,
              principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
              choicesDetailsMap.get(newChoice.getKey()).getName(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
          }
          if (isCJF) {
            saveAudit(details, null, jobId, null, null, principalUser, isCJF);
          } else {
            saveAudit(details, null, jobId, stage.getId(), task.getId(), principalUser, false);
          }
        }
      }
    }
  }

  private void saveUnchangedCorrectionParameter(boolean isCJF, Long jobId, Stage stage, Task task, Parameter parameter, PrincipalUser principalUser, Correction latestCorrection, String taskLocation) {
    if (!Utility.isEmpty(latestCorrection)) {
      String details = formatMessage(UNCHANGED_PARAMETER_CORRECTION,
        principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
        latestCorrection.getCorrectorsReason(), latestCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
      if (isCJF) {
        saveAudit(details, null, jobId, null, null, principalUser, isCJF);
      } else {
        saveAudit(details, null, jobId, stage.getId(), task.getId(), principalUser, false);
      }
    }
  }

  private void saveTextParameterAudit(Long jobId, Stage stage, Task task, Parameter parameter, String newValue,
                                      boolean isExecutedForCorrection, PrincipalUser principalUser, Correction latestCorrection, Boolean isCJF) {
    TaskExecution taskExecution;
    if (isCJF) {
      taskExecution = null;
    } else if (!isExecutedForCorrection) {
      taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    } else {
      taskExecution = taskExecutionRepository.findTaskExecutionEnabledForCorrection(task.getId(), jobId);
    }
    String taskLocation = "";
    if (!isCJF) {
      taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    }
    String details;
    if (isExecutedForCorrection) {
      if (Utility.isEmpty(latestCorrection.getNewValue())) {
        details = formatMessage(UNCHANGED_PARAMETER_CORRECTION,
          principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
          latestCorrection.getCorrectorsReason(), latestCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());

      } else {
        details = formatMessage(TEXT_BOX_PARAMETER_ON_CORRECTION,
          principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
          latestCorrection.getNewValue(), latestCorrection.getCorrectorsReason(), latestCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
      }
    } else if (isCJF) {
      details = formatMessage(TEXT_BOX_PARAMETER_CJF,
        principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
        newValue, parameter.getLabel());
    } else {
      details = formatMessage(TEXT_BOX_PARAMETER,
        principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
        newValue, parameter.getLabel(), taskLocation, task.getName(), stage.getName());
    }
    if (isCJF) {
      saveAudit(details, null, jobId, null, null, principalUser, isCJF);
    } else {
      saveAudit(details, null, jobId, stage.getId(), task.getId(), principalUser, false);
    }

  }

  private <T extends BaseParameterValueDto> void saveShouldBeParameter(Long jobId, Stage stage, Task task, Parameter parameter, String newValue,
                                                                       @Nullable T oldValue, boolean isExecutedForCorrection, String reason,
                                                                       PrincipalUser principalUser, Correction latestCorrection) {
    TaskExecution taskExecution;
    if (!isExecutedForCorrection) {
      taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    } else {
      taskExecution = taskExecutionRepository.findTaskExecutionEnabledForCorrection(task.getId(), jobId);
    }
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    String details = "";
    if (Utility.isEmpty(reason)) {
      if (null != oldValue && null != oldValue.getValue()) {
        if (isExecutedForCorrection) {
          details = formatMessage(SHOULD_BE_PARAMETER_CORRECTION,
            principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
            newValue, latestCorrection.getCorrectorsReason(), latestCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
        } else {
          details = formatMessage(SHOULD_BE_PARAMETER,
            principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
            parameter.getLabel(), oldValue.getValue(), newValue, taskLocation, task.getName(), stage.getName());
        }
      } else {
        if (isExecutedForCorrection) {
          details = formatMessage(SHOULD_BE_PARAMETER_CORRECTION_INITIAL,
            principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
            newValue, latestCorrection.getCorrectorsReason(), latestCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
        } else {
          details = formatMessage(SHOULD_BE_PARAMETER_INITIAL,
            principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
            newValue, parameter.getLabel(), taskLocation, task.getName(), stage.getName());
        }
      }
    } else {
      if (null != oldValue && null != oldValue.getValue()) {
        if (isExecutedForCorrection) {
          details = formatMessage(SHOULD_BE_PARAMETER_CORRECTION_WITH_REASON,
            principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
            newValue, latestCorrection.getCorrectorsReason(), latestCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
        } else {
          details = formatMessage(SHOULD_BE_PARAMETER_WITH_REASON,
            principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
            parameter.getLabel(), oldValue.getValue(), newValue, reason, taskLocation, task.getName(), stage.getName());
        }
      } else {
        if (isExecutedForCorrection) {
          details = formatMessage(SHOULD_BE_PARAMETER_CORRECTION_INITIAL_WITH_REASON,
            principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
            newValue, latestCorrection.getCorrectorsReason(), latestCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
        } else {
          details = formatMessage(SHOULD_BE_PARAMETER_INITIAL_WITH_REASON,
            principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
            newValue, parameter.getLabel(), reason, taskLocation, task.getName(), stage.getName());
        }
      }
    }
    saveAudit(details, null, jobId, stage.getId(), task.getId(), principalUser, false);
  }

  private void saveSignatureAudit(Long jobId, Stage stage, Task task, Parameter parameter, boolean isExecutedForCorrection, PrincipalUser principalUser, Correction latestCorrection) {
    TaskExecution taskExecution;
    if (!isExecutedForCorrection) {
      taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    } else {
      taskExecution = taskExecutionRepository.findTaskExecutionEnabledForCorrection(task.getId(), jobId);
    }
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    String details = formatMessage(isExecutedForCorrection ? SIGNATURE_PARAMETER_CORRECTION : SIGNATURE_PARAMETER,
      principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
      parameter.getLabel(), taskLocation, task.getName(), stage.getName());
    saveAudit(details, null, jobId, stage.getId(), task.getId(), principalUser, false);
  }

  private void saveYesNoAudit(Long jobId, Stage stage, Task task, Parameter parameter, JsonNode newData,
                              boolean isExecutedForCorrection, String reason, PrincipalUser principalUser, Correction latestCorrection) throws IOException {
    TaskExecution taskExecution;
    if (!isExecutedForCorrection) {
      taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    } else {
      taskExecution = taskExecutionRepository.findTaskExecutionEnabledForCorrection(task.getId(), jobId);
    }
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    String details = "";
    List<ChoiceParameterBase> parameters = JsonUtils.jsonToCollectionType(parameter.getData(), List.class, ChoiceParameterBase.class);
    Map<String, ChoiceParameterBase> choicesDetailsMap =
      parameters.stream().collect(Collectors.toMap(ChoiceParameterBase::getId, p -> p));
    Map<String, String> newChoicesMap = objectMapper.convertValue(newData, new TypeReference<>() {
    });
    if (Utility.isEmpty(newData)) {
      saveUnchangedCorrectionParameter(false, jobId, stage, task, parameter, principalUser, latestCorrection, taskLocation);
    } else {
      for (Map.Entry<String, String> newChoice : newChoicesMap.entrySet()) {
        if (State.Selection.SELECTED.name().equals(newChoice.getValue())) {
          if (Utility.isEmpty(reason)) {
            if (isExecutedForCorrection) {
              details = formatMessage(YES_NO_PARAMETER_CORRECTION,
                principalUser.getFirstName(), principalUser.getLastName(),
                principalUser.getEmployeeId(), choicesDetailsMap.get(newChoice.getKey()).getName(), latestCorrection.getCorrectorsReason(), latestCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
            } else {
              details = formatMessage(YES_NO_PARAMETER,
                principalUser.getFirstName(), principalUser.getLastName(),
                principalUser.getEmployeeId(), choicesDetailsMap.get(newChoice.getKey()).getName(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
            }
          } else {
            if (isExecutedForCorrection) {
              details = formatMessage(YES_NO_PARAMETER_CORRECTION_WITH_REASON,
                principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
                choicesDetailsMap.get(newChoice.getKey()).getName(), latestCorrection.getCorrectorsReason(), latestCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
            } else {
              details = formatMessage(YES_NO_PARAMETER_WITH_REASON,
                principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
                choicesDetailsMap.get(newChoice.getKey()).getName(), reason, parameter.getLabel(), taskLocation, task.getName(), stage.getName());
            }
          }
          break;
        }
      }
      saveAudit(details, null, jobId, stage.getId(), task.getId(), principalUser, false);
    }
  }

  private void saveMediaAudit(Long jobId, Stage stage, Task task, Parameter parameter, List<ParameterValueMediaMapping> mediaValues,
                              List<CorrectionMediaMapping> correctionMedias, List<MediaDto> oldMedias, boolean isExecutedForCorrection, PrincipalUser principalUser, MediaParameterBase executedMediaParameterBase, String correctorRemark) {

    TaskExecution taskExecution;
    if (!isExecutedForCorrection) {
      taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    } else {
      taskExecution = taskExecutionRepository.findTaskExecutionEnabledForCorrection(task.getId(), jobId);
    }
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    StringBuilder medias = new StringBuilder();
    Map<Long, MediaDto> mediaMap = oldMedias.stream()
      .collect(Collectors.toMap(media -> Long.valueOf(media.getId()), media -> media));

    List<Media> newMedias = new ArrayList<>();
    Set<Long> archivedMediaIds = new HashSet<>();
    Set<Long> mediaIdsToBeArchived = new HashSet<>();
    if (isExecutedForCorrection) {
      for (CorrectionMediaMapping mediaMapping : correctionMedias) {
        if (!mediaMapping.isOldMedia()) {
          newMedias.add(mediaMapping.getMedia());
        } else if (mediaMapping.isArchived()) {
          archivedMediaIds.add(mediaMapping.getMedia().getId());
        }
      }
    } else {
      for (ParameterValueMediaMapping mediaMapping : mediaValues) {
        newMedias.add(mediaMapping.getMedia());
        if (mediaMapping.isArchived()) {
          archivedMediaIds.add(mediaMapping.getMedia().getId());
        }
      }
      if (!Utility.isEmpty(executedMediaParameterBase)) {
        for (ExecuteMediaPrameterRequest mediaBase : executedMediaParameterBase.getMedias()) {
          mediaIdsToBeArchived.add(Long.valueOf(mediaBase.getMediaId()));
        }
      }
    }

    String details = "";

    if (isExecutedForCorrection && Utility.isEmpty(newMedias)) {
      details = formatMessage(MEDIA_PARAMETER_UNCHANGED_CORRECTION,
        principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), correctorRemark, parameter.getLabel(), taskLocation, task.getName(), stage.getName());
      saveAudit(details, null, jobId, stage.getId(), task.getId(), principalUser, false);
    } else {
      for (Media media : newMedias) {
        if (!mediaMap.containsKey(media.getId())) {
          medias.append("Name: ").append(media.getName());
          if (!Utility.isEmpty(media.getDescription())) {
            medias.append(" ").append("(Description: ").append(media.getDescription()).append(")");
          }
          details = isExecutedForCorrection
            ?
            formatMessage(MEDIA_PARAMETER_CORRECTION,
              principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
              medias.toString(), correctorRemark, parameter.getLabel(), taskLocation, task.getName(), stage.getName())
            :
            formatMessage(MEDIA_PARAMETER,
              principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
              medias.toString(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
          saveAudit(details, null, jobId, stage.getId(), task.getId(), principalUser, false);

        } else if (archivedMediaIds.contains(media.getId())) {
          medias.append("Name: ").append(media.getName());
          if (!Utility.isEmpty(media.getDescription())) {
            medias.append(" ").append("(Description: ").append(media.getDescription()).append(")");
          }
          Long thisMedia = media.getId();
          if (mediaIdsToBeArchived.contains(thisMedia)) {
            details = formatMessage(isExecutedForCorrection ? MEDIA_PARAMETER_ARCHIVED_CORRECTION : MEDIA_PARAMETER_ARCHIVED,
              principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
              medias.toString(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
            saveAudit(details, null, jobId, stage.getId(), task.getId(), principalUser, false);
          }
        } else {
          // Check for property changes and create specific audit entries
          MediaDto oldMedia = mediaMap.get(media.getId());
          boolean nameChanged = !Objects.equals(media.getName(), oldMedia.getName());
          boolean descriptionChanged = !Objects.equals(media.getDescription(), oldMedia.getDescription());
          
          if (nameChanged && descriptionChanged) {
            String bothUpdateDetails = formatMessage(MEDIA_NAME_AND_DESCRIPTION_UPDATED,
              principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
              oldMedia.getName(), media.getName(),
              oldMedia.getDescription() != null ? oldMedia.getDescription() : "", 
              media.getDescription() != null ? media.getDescription() : "",
              parameter.getLabel(), taskLocation, task.getName(), stage.getName());
            saveAudit(bothUpdateDetails, null, jobId, stage.getId(), task.getId(), principalUser, false);
            
          } else if (nameChanged) {
            String nameUpdateDetails = formatMessage(MEDIA_NAME_UPDATED,
              principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
              oldMedia.getName(), media.getName(),
              parameter.getLabel(), taskLocation, task.getName(), stage.getName());
            saveAudit(nameUpdateDetails, null, jobId, stage.getId(), task.getId(), principalUser, false);
            
          } else if (descriptionChanged) {
            String descUpdateDetails = formatMessage(MEDIA_DESCRIPTION_UPDATED,
              principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
              media.getName(), 
              oldMedia.getDescription() != null ? oldMedia.getDescription() : "", 
              media.getDescription() != null ? media.getDescription() : "",
              parameter.getLabel(), taskLocation, task.getName(), stage.getName());
            saveAudit(descUpdateDetails, null, jobId, stage.getId(), task.getId(), principalUser, false);
          }
        }
        medias = new StringBuilder();
      }
    }
  }

  private void saveFileUploadAudit(Long jobId, Stage stage, Task task, Parameter parameter, List<ParameterValueMediaMapping> mediaValues,
                                   List<CorrectionMediaMapping> correctionMedias, List<MediaDto> oldMedias, boolean isExecutedForCorrection, PrincipalUser principalUser, MediaParameterBase executedMediaParameterBase, String correctorRemark) {
    TaskExecution taskExecution;
    if (!isExecutedForCorrection) {
      taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    } else {
      taskExecution = taskExecutionRepository.findTaskExecutionEnabledForCorrection(task.getId(), jobId);
    }
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    StringBuilder medias = new StringBuilder();
    Map<Long, MediaDto> mediaMap = oldMedias.stream()
      .collect(Collectors.toMap(media -> Long.valueOf(media.getId()), media -> media));

    List<Media> newMedias = new ArrayList<>();
    Set<Long> archivedMediaIds = new HashSet<>();
    Map<Long, String> mediaIdsToBeArchived = new HashMap<>();
    if (isExecutedForCorrection) {
      for (CorrectionMediaMapping mediaMapping : correctionMedias) {
        if (!mediaMapping.isOldMedia()) {
          newMedias.add(mediaMapping.getMedia());
        } else if (mediaMapping.isArchived()) {
          archivedMediaIds.add(mediaMapping.getMedia().getId());
        }
      }
    } else {
      for (ParameterValueMediaMapping mediaMapping : mediaValues) {
        newMedias.add(mediaMapping.getMedia());
        if (mediaMapping.isArchived()) {
          archivedMediaIds.add(mediaMapping.getMedia().getId());
        }
      }
      if (!Utility.isEmpty(executedMediaParameterBase)) {
        for (ExecuteMediaPrameterRequest mediaBase : executedMediaParameterBase.getMedias()) {
          mediaIdsToBeArchived.put(Long.valueOf(mediaBase.getMediaId()), mediaBase.getReason());
        }
      }
    }

    String auditDetails = "";

    if (isExecutedForCorrection && Utility.isEmpty(newMedias)) {
      auditDetails = formatMessage(MEDIA_PARAMETER_UNCHANGED_CORRECTION,
        principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), correctorRemark, parameter.getLabel(), taskLocation, task.getName(), stage.getName());
      saveAudit(auditDetails, null, jobId, stage.getId(), task.getId(), principalUser, false);
    } else {

      for (Media media : newMedias) {
        if (!mediaMap.containsKey(media.getId())) {
          medias.append("Name: ").append(media.getName());
          if (!Utility.isEmpty(media.getDescription())) {
            medias.append(" ").append("(Description: ").append(media.getDescription()).append(")");
          }
          auditDetails = isExecutedForCorrection
            ?
            formatMessage(MEDIA_PARAMETER_CORRECTION,
              principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
              medias.toString(), correctorRemark, parameter.getLabel(), taskLocation, task.getName(), stage.getName())
            :
            formatMessage(MEDIA_PARAMETER,
              principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
              medias.toString(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());

          saveAudit(auditDetails, null, jobId, stage.getId(), task.getId(), principalUser, false);
        } else if (archivedMediaIds.contains(media.getId())) {
          medias.append("Name: ").append(media.getName());
          if (!Utility.isEmpty(media.getDescription())) {
            medias.append(" ").append("(Description: ").append(media.getDescription()).append(")");
          }
          Long thisMedia = media.getId();
          String reason = mediaIdsToBeArchived.getOrDefault(thisMedia, "");
          if (!reason.equals("")) {
            String details = formatMessage(isExecutedForCorrection ? FILE_UPLOAD_PARAMETER_ARCHIVED_CORRECTION : FILE_UPLOAD_PARAMETER_ARCHIVED,
              principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), reason,
              medias.toString(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
            saveAudit(details, null, jobId, stage.getId(), task.getId(), principalUser, false);
          }
        }
        medias = new StringBuilder();
      }
    }
  }

  private String getUploadedMediaAuditMessage(Media media) {
    StringBuilder mediaMessage = new StringBuilder();
    mediaMessage.append("Name: ").append(media.getName());
    if (!Utility.isEmpty(media.getDescription())) {
      mediaMessage.append(" ").append("(Description: ").append(media.getDescription()).append(")");
    }
    mediaMessage.append(",");
    return mediaMessage.toString();
  }

  private String formatMessage(String pattern, String... replacements) {
    for (int i = 0; i < replacements.length; i++) {
      if (replacements[i] != null) {
        pattern = pattern.replace("{" + i + "}", replacements[i]);
      } else {
        pattern = pattern.replace("{" + i + "}", "");
      }
    }

    return pattern;
  }

  private <T extends BaseParameterValueDto> void saveDateParameter(Long jobId, Stage stage, Task task, Parameter parameter, String newValue,
                                                                   @Nullable T oldValue, boolean isExecutedForCorrection,
                                                                   PrincipalUser principalUser, Correction latestCorrection, Boolean isCJF) {
    saveDateOrDateTimeAudit(jobId, stage, task, parameter, oldValue, isExecutedForCorrection, principalUser, !Utility.isEmpty(newValue) ? Long.parseLong(newValue) : null, latestCorrection, isCJF);
  }

  private <T extends BaseParameterValueDto> void saveDateOrDateTimeAudit(Long jobId, Stage stage, Task task, Parameter parameter, T oldValue, boolean isExecutedForCorrection, PrincipalUser principalUser, Long date, Correction latestCorrection, Boolean isCJF) {
    TaskExecution taskExecution;
    if (isCJF) {
      taskExecution = null;
    } else if (!isExecutedForCorrection) {
      taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    } else {
      taskExecution = taskExecutionRepository.findTaskExecutionEnabledForCorrection(task.getId(), jobId);
    }
    String taskLocation = "";
    if (!isCJF) {
      taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    }
    String details;
    JobAuditParameter jobAuditParameter = new JobAuditParameter();
    Map<Integer, JobAuditParameterValue> jobAuditParameterValueMap = new HashMap<>();
    JobAuditParameterValue jobAuditParameterValue = new JobAuditParameterValue();
    jobAuditParameterValue.setType(parameter.getType());
    jobAuditParameterValue.setValue(date);
    jobAuditParameterValueMap.put(0, jobAuditParameterValue);
    jobAuditParameter.setParameters(jobAuditParameterValueMap);
    if (!Utility.isEmpty(oldValue) && !Utility.isEmpty(oldValue.getValue())) {
      if (isExecutedForCorrection) {
        if (Utility.isEmpty(latestCorrection.getNewValue())) {
          details = formatMessage(UNCHANGED_PARAMETER_CORRECTION,
            principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
            latestCorrection.getCorrectorsReason(), latestCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
        } else {
          details = formatMessage(DATE_PARAMETER_CORRECTION,
            principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), latestCorrection.getCorrectorsReason(), latestCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
        }
      } else if (isCJF) {
        details = formatMessage(DATE_PARAMETER_CJF,
          principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), parameter.getLabel());
      } else {
        details = formatMessage(DATE_PARAMETER,
          principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
      }
      details = details.replace("{{{ABC}}}", "{{{0}}}");
    } else {
      if (isExecutedForCorrection) {
        if (Utility.isEmpty(latestCorrection.getNewValue())) {
          details = formatMessage(UNCHANGED_PARAMETER_CORRECTION,
            principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
            latestCorrection.getCorrectorsReason(), latestCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
        } else {
          details = formatMessage(DATE_PARAMETER_CORRECTION_INITIAL,
            principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), latestCorrection.getCorrectorsReason(), latestCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
        }
      } else if (isCJF) {
        details = formatMessage(DATE_PARAMETER_INITIAL_CJF,
          principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
          parameter.getLabel());
      } else {
        details = formatMessage(DATE_PARAMETER_INITIAL,
          principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
      }
      details = details.replace("{{{ABC}}}", "{{{0}}}");
    }
    if (isCJF) {
      saveAudit(details, jobAuditParameter, jobId, null, null, principalUser, isCJF);
    } else {
      saveAudit(details, jobAuditParameter, jobId, stage.getId(), task.getId(), principalUser, false);
    }
  }

  private <T extends BaseParameterValueDto> void saveDateTimeParameter(Long jobId, Stage stage, Task task, Parameter parameter, String newValue,
                                                                       @Nullable T oldValue, boolean isExecutedForCorrection,
                                                                       PrincipalUser principalUser, Correction latestCorrection, Boolean isCJF) {
    saveDateOrDateTimeAudit(jobId, stage, task, parameter, oldValue, isExecutedForCorrection, principalUser,
      Utility.isEmpty(newValue) ? null : Long.parseLong(newValue), latestCorrection, isCJF);
  }

  private <T extends BaseParameterValueDto> void saveNumberParameter(Long jobId, Stage stage, Task task, Parameter parameter, String newValue,
                                                                     @Nullable T oldValue, boolean isExecutedForCorrection,
                                                                     PrincipalUser principalUser, Correction latestCorrection, boolean isCJF) {
    TaskExecution taskExecution;
    if (isCJF) {
      taskExecution = null;
    } else if (!isExecutedForCorrection) {
      taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    } else {
      taskExecution = taskExecutionRepository.findTaskExecutionEnabledForCorrection(task.getId(), jobId);
    }
    String taskLocation = "";
    if (!isCJF) {
      taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    }
    String details = "";
    if (null != oldValue && null != oldValue.getValue()) {
      if (isCJF) {
        details = formatMessage(NUMBER_PARAMETER_INITIAL_CJF,
          principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
          newValue, parameter.getLabel());
      } else if (isExecutedForCorrection) {
        if (Utility.isEmpty(latestCorrection.getNewValue())) {
          details = formatMessage(UNCHANGED_PARAMETER_CORRECTION,
            principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
            latestCorrection.getCorrectorsReason(), latestCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
        } else {
          details = formatMessage(NUMBER_PARAMETER_CORRECTION,
            principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
            latestCorrection.getNewValue(), latestCorrection.getCorrectorsReason(), latestCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
        }
      } else {
        details = formatMessage(NUMBER_PARAMETER,
          principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
          parameter.getLabel(), newValue, taskLocation, task.getName(), stage.getName());
      }
    } else {
      if (isExecutedForCorrection) {
        details = formatMessage(NUMBER_PARAMETER_CORRECTION_INITIAL,
          principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
          latestCorrection.getNewValue(), latestCorrection.getCorrectorsReason(), latestCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
      } else if (isCJF) {
        details = formatMessage(NUMBER_PARAMETER_INITIAL_CJF,
          principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
          newValue, parameter.getLabel());
      } else {
        details = formatMessage(NUMBER_PARAMETER_INITIAL,
          principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
          newValue, parameter.getLabel(), taskLocation, task.getName(), stage.getName());
      }
    }
    if (isCJF) {
      saveAudit(details, null, jobId, null, null, principalUser, isCJF);
    } else {
      saveAudit(details, null, jobId, stage.getId(), task.getId(), principalUser, false);
    }

  }

  private <T extends BaseParameterValueDto> void saveCalculationParameter(Long jobId, Stage stage, Task task, Parameter parameter, String updatedValue,
                                                                          @Nullable T oldValue, boolean isExecutedForCorrection, State.ParameterExecution state, PrincipalUser principalUser, Correction latestCorrection) {
    TaskExecution taskExecution;
    if (!isExecutedForCorrection) {
      taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    } else {
      taskExecution = taskExecutionRepository.findTaskExecutionEnabledForCorrection(task.getId(), jobId);
    }
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    if (state == State.ParameterExecution.EXECUTED) {
      String details = "";
      if (null != oldValue && null != oldValue.getValue()) {
        if (isExecutedForCorrection) {
          details = formatMessage(CALCULATION_PARAMETER_CORRECTION,
            principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
            updatedValue, latestCorrection.getCorrectorsReason(), latestCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
        } else {
          details = formatMessage(CALCULATION_PARAMETER,
            principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
            parameter.getLabel(), updatedValue, taskLocation, task.getName(), stage.getName());
        }
      } else {
        if (isExecutedForCorrection) {
          details = formatMessage(CALCULATION_PARAMETER_CORRECTION_INITIAL,
            principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
            updatedValue, latestCorrection.getCorrectorsReason(), latestCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
        } else {
          details = formatMessage(CALCULATION_PARAMETER_INITIAL,
            principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
            parameter.getLabel(), updatedValue, taskLocation, task.getName(), stage.getName());
        }
      }
      saveAudit(details, null, jobId, stage.getId(), task.getId(), principalUser, false);
    }
  }

  private <T extends BaseParameterValueDto> void saveResourceParameter(Long jobId, Stage stage, Task task, Parameter parameter, String newData,
                                                                       @Nullable T oldValue, boolean isExecutedForCorrection,
                                                                       PrincipalUser principalUser, Correction latestCorrection, Boolean isCJF) throws JsonProcessingException {
    TaskExecution taskExecution;
    if (isCJF) {
      taskExecution = null;
    } else if (!isExecutedForCorrection) {
      taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    } else {
      taskExecution = taskExecutionRepository.findTaskExecutionEnabledForCorrection(task.getId(), jobId);
    }
    String taskLocation = "";
    if (!isCJF) {
      taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    }
    String details = "";
    List<ResourceParameterChoiceDto> choices = JsonUtils.readValue(newData, new TypeReference<>() {
    });
    if (!Utility.isEmpty(choices)) {
      var choice = choices.get(0);
      String value = choice.getObjectDisplayName() + "(ID: " + choice.getObjectExternalId() + ")";

      if (null != oldValue && null != oldValue.getValue()) {
        if (isExecutedForCorrection) {
          details = formatMessage(RESOURCE_PARAMETER_CORRECTION,
            principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), value, latestCorrection.getCorrectorsReason(), latestCorrection.getCode(),
            parameter.getLabel(), taskLocation, task.getName(), stage.getName());
        } else {
          details = formatMessage(RESOURCE_PARAMETER,
            principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), value, latestCorrection.getCorrectorsReason(), latestCorrection.getCode(),
            parameter.getLabel(), taskLocation, task.getName(), stage.getName());
        }
      } else {

        if (isExecutedForCorrection) {
          details = formatMessage(RESOURCE_PARAMETER_CORRECTION_INITIAL,
            principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
            value, latestCorrection.getCorrectorsReason(), latestCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
        } else if (isCJF) {
          details = formatMessage(RESOURCE_PARAMETER_INITIAL_CJF,
            principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
            value, parameter.getLabel());
        } else {
          details = formatMessage(RESOURCE_PARAMETER_INITIAL,
            principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
            value, parameter.getLabel(), taskLocation, task.getName(), stage.getName());
        }
      }
    } else {
      if (!Utility.isEmpty(oldValue.getChoices())) {
        choices = JsonUtils.readValue(oldValue.getChoices().toString(), new TypeReference<>() {
        });
      }
      if (!Utility.isEmpty(choices)) {
        ResourceParameterChoiceDto resourceParameterChoiceDto = choices.get(0);
        String value = resourceParameterChoiceDto.getObjectDisplayName() + "(ID: " + resourceParameterChoiceDto.getObjectExternalId() + ")";
        if (isExecutedForCorrection) {
          if (newData.equals("null")) {
            details = formatMessage(UNCHANGED_PARAMETER_CORRECTION,
              principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
              latestCorrection.getCorrectorsReason(), latestCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
          } else {
            details = formatMessage(RESOURCE_PARAMETER_CORRECTION_INITIAL_DESELECTED,
              principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
              value, latestCorrection.getCorrectorsReason(), latestCorrection.getCode(), parameter.getLabel(), taskLocation, task.getName(), stage.getName());
          }
        } else if(isCJF){
          details = formatMessage(RESOURCE_PARAMETER_DESELECTION_CJF,
            principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
            value, parameter.getLabel());
        } else {
          details = formatMessage(RESOURCE_PARAMETER_DESELECTION,
            principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
            value, parameter.getLabel(), taskLocation, task.getName(), stage.getName());
        }
      }
    }
    if (isCJF) {
      saveAudit(details, null, jobId, null, null, principalUser, isCJF);
    } else {
      saveAudit(details, null, jobId, stage.getId(), task.getId(), principalUser, false);
    }
  }


  private <T extends BaseParameterValueDto> void saveMultiResourceParameter(Long jobId, Stage stage, Task task, Parameter parameter, String newData,
                                                                            @Nullable T oldValue, boolean isExecutedForCorrection,
                                                                            PrincipalUser principalUser, Correction latestCorrection, Boolean isCJF) throws IOException {
    TaskExecution taskExecution;
    if (isCJF) {
      taskExecution = null;
    } else if (!isExecutedForCorrection) {
      taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    } else {
      taskExecution = taskExecutionRepository.findTaskExecutionEnabledForCorrection(task.getId(), jobId);
    }
    String taskLocation = "";
    if (!isCJF) {
      taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    }
    String details = "";
    List<ResourceParameterChoiceDto> choices = JsonUtils.readValue(newData, new TypeReference<>() {
    });
    List<ResourceParameterChoiceDto> oldChoices = new ArrayList<>();
    if (null != oldValue && !Utility.isEmpty(oldValue.getChoices())) {
      oldChoices = JsonUtils.readValue(oldValue.getChoices().toString(), new TypeReference<>() {
      });
    }
    Map<String, ResourceParameterChoiceDto> newChoicesMap = new HashMap<>();
    if (newData.equals("null")) {
      saveUnchangedCorrectionParameter(false, jobId, stage, task, parameter, principalUser, latestCorrection, taskLocation);
      return;
    } else {
      newChoicesMap = choices.stream().collect(Collectors.toMap(ResourceParameterChoiceDto::getObjectId, Function.identity()));
    }
    Map<String, ResourceParameterChoiceDto> oldChoicesMap = oldChoices.stream().collect(Collectors.toMap(ResourceParameterChoiceDto::getObjectId, Function.identity()));

    Set<String> oldChoicesIds = oldChoices.stream().map(ResourceParameterChoiceDto::getObjectId).collect(Collectors.toSet());
    Set<String> newChoiceIds = choices.stream().map(ResourceParameterChoiceDto::getObjectId).collect(Collectors.toSet());

    Set<String> newSelections = SetUtils.difference(newChoiceIds, oldChoicesIds);
    Set<String> deselections = SetUtils.difference(oldChoicesIds, newChoiceIds);

    for (String id : newSelections) {
      ResourceParameterChoiceDto choice = newChoicesMap.get(id);
      String value = choice.getObjectDisplayName() + "(ID: " + choice.getObjectExternalId() + ")";
      if (isExecutedForCorrection) {
        details = formatMessage(RESOURCE_PARAMETER_CORRECTION_INITIAL,
          principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
          value, parameter.getLabel(), taskLocation, task.getName(), stage.getName());
      } else if (isCJF) {
        details = formatMessage(RESOURCE_PARAMETER_INITIAL_CJF,
          principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
          value, parameter.getLabel());
      } else {
        details = formatMessage(RESOURCE_PARAMETER_INITIAL,
          principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
          value, parameter.getLabel(), taskLocation, task.getName(), stage.getName());
      }
      if (isCJF) {
        saveAudit(details, null, jobId, null, null, principalUser, isCJF);
      } else {
        saveAudit(details, null, jobId, stage.getId(), task.getId(), principalUser, false);
      }
    }

    for (String id : deselections) {
      ResourceParameterChoiceDto choice = oldChoicesMap.get(id);
      String value = choice.getObjectDisplayName() + "(ID: " + choice.getObjectExternalId() + ")";
      if (isExecutedForCorrection) {
        details = formatMessage(RESOURCE_PARAMETER_CORRECTION_INITIAL_DESELECTED,
          principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
          value, parameter.getLabel(), taskLocation, task.getName(), stage.getName());
      } else if (isCJF) {
        details = formatMessage(RESOURCE_PARAMETER_DESELECTION_CJF,
          principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
          value, parameter.getLabel());
      } else {
        details = formatMessage(RESOURCE_PARAMETER_DESELECTION,
          principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(),
          value, parameter.getLabel(), taskLocation, task.getName(), stage.getName());
      }

      if (isCJF) {
        saveAudit(details, null, jobId, null, null, principalUser, isCJF);
      } else {
        saveAudit(details, null, jobId, stage.getId(), task.getId(), principalUser, false);
      }
    }

  }

  private String getTaskLocation(Integer stageOrderTree, Integer taskOrderTree, Integer taskExecutionOrderTree) {
    if (taskExecutionOrderTree != 1) {
      return stageOrderTree + "." + taskOrderTree + "." + (taskExecutionOrderTree - 1);
    } else {
      return stageOrderTree + "." + taskOrderTree;
    }
  }

  private String getTaskLocationAndNameOfParameter(Parameter parameter, Long jobId) {
    String taskLocationAndName = "";
    if (parameter.getTargetEntityType() == Type.ParameterTargetEntityType.TASK) {
      Task parameterTask = parameter.getTask();
      Stage parameterTaskStage = parameterTask.getStage();
      TaskExecution parameterTaskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(parameterTask.getId(), jobId);
      String parameterTaskLocation = getTaskLocation(parameterTaskStage.getOrderTree(), parameterTask.getOrderTree(), parameterTaskExecution.getOrderTree());
      taskLocationAndName = formatMessage(TASK_LOCATION_AND_NAME, parameterTaskLocation, parameterTask.getName());
    } else if (parameter.getTargetEntityType() == Type.ParameterTargetEntityType.PROCESS) {
      taskLocationAndName = PROCESS_PARAMETER;
    }
    return taskLocationAndName;
  }

  public void initiateBulkSelfVerification(Long jobId, Long checkedAt, Task task, PrincipalUser principalUser) {
    String details;
    Stage stage;
    TaskExecution taskExecution;

    taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    stage = stageRepository.findByTaskId(task.getId());
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    details = formatMessage(INITIATE_BULK_SELF_VERIFICATION, principalUser.getFirstName(),
      principalUser.getLastName(), principalUser.getEmployeeId(), task.getName(),
      taskLocation, stage.getName());

    jobAuditRepository.save(getInfoAudit(details, null, jobId, stage.getId(), task.getId(), principalUser));
  }

  public void logBulkSelfParameterExamination(Long parameterExecutionId, Long checkedAt, PrincipalUser principalUser) {
    ParameterValue parameterValue = parameterValueRepository.getReferenceById(parameterExecutionId);
    Parameter parameter = parameterValue.getParameter();
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(parameterValue.getTaskExecutionId());
    Task task = taskRepository.getReferenceById(parameter.getTaskId());
    Stage stage = stageRepository.findByTaskId(task.getId());
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    String details = formatMessage(BULK_SELF_VERIFICATION_PARAMETER_EXAMINED,
      principalUser.getFirstName(),
      principalUser.getLastName(),
      principalUser.getEmployeeId(),
      parameter.getLabel(),
      taskLocation,
      task.getName(),
      stage.getName(),
      checkedAt.toString()
    );
    JobAudit jobAudit = getInfoAudit(details, null, taskExecution.getJobId(), stage.getId(), task.getId(), principalUser);
    jobAuditRepository.save(jobAudit);
  }


  public void approveBulkSelfVerification(Long jobId, Task task, Long checkedAt, PrincipalUser principalUser) {
    String details;
    Stage stage;
    TaskExecution taskExecution;

    taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    stage = stageRepository.findByTaskId(task.getId());
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    details = formatMessage(APPROVE_BULK_SELF_VERIFICATION,
      principalUser.getFirstName(),
      principalUser.getLastName(),
      principalUser.getEmployeeId(),
      task.getName(),
      taskLocation,
      stage.getName(),
      checkedAt.toString()
    );
    jobAuditRepository.save(getInfoAudit(details, null, jobId, stage.getId(), task.getId(), principalUser));
  }

  public void initiateBulkPeerVerification(Long jobId, Task task, PrincipalUser principalUser) {
    String details;
    Stage stage;
    TaskExecution taskExecution;

    taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    stage = stageRepository.findByTaskId(task.getId());
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    details = formatMessage(INITIATE_BULK_PEER_VERIFICATION,
      principalUser.getFirstName(),
      principalUser.getLastName(),
      principalUser.getEmployeeId(),
      task.getName(),
      taskLocation,
      stage.getName());

    jobAuditRepository.save(getInfoAudit(details, null, jobId, stage.getId(), task.getId(), principalUser));
  }

  public void logBulkPeerParameterExamination(Long parameterExecutionId, Long checkedAt, PrincipalUser principalUser) {
    ParameterValue parameterValue = parameterValueRepository.getReferenceById(parameterExecutionId);
    Parameter parameter = parameterValue.getParameter();
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(parameterValue.getTaskExecutionId());
    Task task = taskRepository.getReferenceById(parameter.getTaskId());
    Stage stage = stageRepository.findByTaskId(task.getId());
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    String details = formatMessage(BULK_PEER_VERIFICATION_PARAMETER_REVIEWED,
      principalUser.getFirstName(),
      principalUser.getLastName(),
      principalUser.getEmployeeId(),
      parameter.getLabel(),
      taskLocation,
      task.getName(),
      stage.getName());
    JobAudit jobAudit = getInfoAudit(checkedAt, details, null, taskExecution.getJobId(), stage.getId(), task.getId(), principalUser);
    jobAuditRepository.save(jobAudit);
  }

  public void logBulkSameSessionParameterReviewed(Long parameterExecutionId, Long checkedAt, PrincipalUser initiatorUser, PrincipalUser reviewerUser) {
    ParameterValue parameterValue = parameterValueRepository.getReferenceById(parameterExecutionId);
    Parameter parameter = parameterValue.getParameter();
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(parameterValue.getTaskExecutionId());
    Task task = taskRepository.getReferenceById(parameter.getTaskId());
    Stage stage = stageRepository.findByTaskId(task.getId());
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    String details = formatMessage(BULK_SAME_SESSION_VERIFICATION_PARAMETERS_REVIEWED,
      initiatorUser.getFirstName(),
      initiatorUser.getLastName(),
      initiatorUser.getEmployeeId(),
      reviewerUser.getFirstName(),
      reviewerUser.getLastName(),
      reviewerUser.getEmployeeId(),
      parameter.getLabel(),
      taskLocation,
      task.getName(),
      stage.getName());
    JobAudit jobAudit = getInfoAudit(checkedAt, details, null, taskExecution.getJobId(), stage.getId(), task.getId(), reviewerUser);
    jobAuditRepository.save(jobAudit);
  }

  public void acceptBulkPeerVerification(Long jobId, Task task, PrincipalUser principalUser) {
    String details;
    Stage stage;
    TaskExecution taskExecution;

    taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    stage = stageRepository.findByTaskId(task.getId());
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());
    details = formatMessage(APPROVE_BULK_PEER_VERIFICATION,
      principalUser.getFirstName(),
      principalUser.getLastName(),
      principalUser.getEmployeeId(),
      task.getName(),
      taskLocation,
      stage.getName());

    jobAuditRepository.save(getInfoAudit(details, null, jobId, stage.getId(), task.getId(), principalUser));
  }

  public void acceptBulkSameSessionPeerVerification(Long jobId, Task task, PrincipalUser verifierUser, PrincipalUser initiatorUser) {
    String details;
    Stage stage;
    TaskExecution taskExecution;

    taskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(task.getId(), jobId);
    stage = stageRepository.findByTaskId(task.getId());
    String taskLocation = getTaskLocation(stage.getOrderTree(), task.getOrderTree(), taskExecution.getOrderTree());

    details = formatMessage(APPROVE_BULK_SAME_SESSION_PEER_VERIFICATION,
      initiatorUser.getFirstName(), initiatorUser.getLastName(), initiatorUser.getEmployeeId(),
      verifierUser.getFirstName(), verifierUser.getLastName(), verifierUser.getEmployeeId(),
      task.getName(), taskLocation, stage.getName());

    jobAuditRepository.save(getInfoAudit(details, null, jobId, stage.getId(), task.getId(), verifierUser));
  }


  private String getValueFromChoices(JsonNode choices) throws JsonProcessingException {
    List<ResourceParameterChoiceDto> allChoices = JsonUtils.readValue(choices.toString(), new TypeReference<>() {
    });
    StringBuilder value = new StringBuilder();
    for (int i = 0; i < allChoices.size(); i++) {
      ResourceParameterChoiceDto choice = allChoices.get(i);
      value.append(choice.getObjectDisplayName())
        .append(" (ID: ")
        .append(choice.getObjectExternalId())
        .append(")");
      if (i < choices.size() - 1) {
        value.append(", ");
      }
    }
    return value.toString();
  }


}
