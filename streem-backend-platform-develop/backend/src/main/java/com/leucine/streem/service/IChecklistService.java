package com.leucine.streem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.constant.State;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.projection.ChecklistView;
import com.leucine.streem.dto.projection.TaskAssigneeView;
import com.leucine.streem.dto.projection.TrainedUsersView;
import com.leucine.streem.dto.request.*;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.Checklist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface IChecklistService {
  Page<ChecklistPartialDto> getAllChecklist(String filters, Pageable pageable);

  ChecklistDto getChecklistById(Long checklistId) throws ResourceNotFoundException;

  ChecklistInfoDto getChecklistInfoById(Long checklistId) throws ResourceNotFoundException;

  ChecklistDto createChecklist(CreateChecklistRequest createChecklistRequest) throws StreemException, ResourceNotFoundException;

  BasicDto archiveChecklist(Long checklistId, String reason) throws ResourceNotFoundException, StreemException;

  BasicDto validateChecklistArchival(Long checklistId) throws ResourceNotFoundException, StreemException;

  BasicDto unarchiveChecklist(Long checklistId, String reason) throws ResourceNotFoundException, StreemException;

  BasicDto validateChecklist(Long checklistId) throws ResourceNotFoundException, IOException, StreemException;

  BasicDto updateChecklist(Long checklistId, ChecklistUpdateRequest checklistUpdateRequest) throws ResourceNotFoundException, StreemException;

  Checklist findById(Long checklistId) throws ResourceNotFoundException;

  Checklist findByTaskId(Long taskId) throws ResourceNotFoundException;

  void validateChecklistModificationState(Long checklistId, State.Checklist state) throws StreemException;

  void validateIfUserIsAuthorForPrototype(Long checklistId, Long userId) throws StreemException;

  BasicDto bulkAssignDefaultUsers(Long checklistId, ChecklistTaskAssignmentRequest checklistTaskAssignmentRequest, boolean notify) throws ResourceNotFoundException, StreemException;

  List<TrainedUsersView> getTrainedUsersOfFacility(Long checklistId, Long facilityId);

  List<TaskAssigneeView> getTaskAssignmentDetails(Long checklistId, boolean isUser, boolean isUserGroup, Set<Long> taskAssignedIdsDto) throws ResourceNotFoundException;

  List<FacilityDto> getFacilityChecklistMapping(Long checklistId) throws ResourceNotFoundException;

  BasicDto bulkAssignmentFacilityIds(Long checklistId, ChecklistFacilityAssignmentRequest checklistFacilityAssignmentRequest) throws ResourceNotFoundException;

  List<ParameterInfoDto> configureProcessParameters(Long checklistId, MapJobParameterRequest mapJobParameterRequest) throws ResourceNotFoundException, StreemException;

  BasicDto reconfigureJobLogColumns(Long checklistId) throws ResourceNotFoundException;

  BasicDto validateIfCurrentUserCanRecallChecklist(Long checklistId) throws ResourceNotFoundException, StreemException;

  ChecklistReviewDto recallChecklist(Long checklistId, RecallProcessDto recallProcessDto) throws ResourceNotFoundException, StreemException;

  Page<ChecklistView> getAllByResource(String objectTypeId, String objectId, Long useCaseId, boolean archived, String name, Pageable pageable) throws JsonProcessingException;

  IChecklistElementDto copyChecklistElement(Long checklistId, CopyChecklistElementRequest copyChecklistRequest) throws ResourceNotFoundException, StreemException;

  ChecklistBasicDto customPublishChecklist(Long checklistId) throws ResourceNotFoundException, StreemException, JsonProcessingException;

  JobLogsColumnDto getJobLogColumns(Long checklistId) throws ResourceNotFoundException, IOException;

  byte[] generateProcessTemplatePdf(Long checklistId) throws ResourceNotFoundException, IOException;

}
