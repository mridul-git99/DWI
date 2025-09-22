package com.leucine.streem.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.constant.State;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.projection.ChecklistCollaboratorView;
import com.leucine.streem.dto.projection.ChecklistView;
import com.leucine.streem.dto.projection.TaskAssigneeView;
import com.leucine.streem.dto.request.*;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/v1/checklists")
public interface IChecklistController {

  @GetMapping
  @ResponseBody
  Response<Page<ChecklistPartialDto>> getAll(@RequestParam(name = "filters", defaultValue = "") String filters, Pageable pageable);

  @GetMapping("/{checklistId}")
  @ResponseBody
  Response<ChecklistDto> getChecklist(@PathVariable Long checklistId) throws ResourceNotFoundException;

  @GetMapping("/{checklistId}/info")
  @ResponseBody
  Response<ChecklistInfoDto> getChecklistInfo(@PathVariable Long checklistId) throws ResourceNotFoundException;

  @PostMapping
  @ResponseBody
  Response<ChecklistDto> createChecklist(@RequestBody CreateChecklistRequest createChecklistRequest) throws StreemException, ResourceNotFoundException;

  @PatchMapping("/{checklistId}/archive")
  @ResponseBody
  Response<BasicDto> archiveChecklist(@PathVariable Long checklistId, @RequestBody ArchiveChecklistRequest archiveChecklistRequest) throws ResourceNotFoundException, StreemException;

  @GetMapping("/{checklistId}/archive/validate")
  @ResponseBody
  Response<BasicDto> validateChecklistArchival(@PathVariable Long checklistId) throws ResourceNotFoundException, StreemException;

  @PatchMapping("/{checklistId}/unarchive")
  @ResponseBody
  Response<BasicDto> unarchiveChecklist(@PathVariable Long checklistId, @RequestBody UnarchiveChecklistRequest unarchiveChecklistRequest) throws ResourceNotFoundException, StreemException;

  @GetMapping("/{checklistId}/validate")
  @ResponseBody
  Response<BasicDto> validateChecklist(@PathVariable Long checklistId) throws ResourceNotFoundException, IOException, StreemException;

  @PatchMapping("/{checklistId}")
  @ResponseBody
  Response<BasicDto> updateChecklist(@PathVariable Long checklistId, @RequestBody ChecklistUpdateRequest checklistUpdateRequest) throws StreemException, ResourceNotFoundException;

  @PatchMapping("/{checklistId}/reviewers/assignments")
  Response<ChecklistBasicDto> reviewerAssignments(@PathVariable Long checklistId, @RequestBody ChecklistCollaboratorAssignmentRequest checklistCollaboratorAssignmentRequest) throws ResourceNotFoundException, StreemException;

  @PatchMapping("/{checklistId}/review/submit")
  Response<ChecklistBasicDto> submitForReview(@PathVariable Long checklistId) throws ResourceNotFoundException, StreemException, JsonProcessingException;

  @GetMapping("/{checklistId}/authors")
  Response<List<ChecklistCollaboratorView>> getAllAuthors(@PathVariable Long checklistId);

  @GetMapping("/{checklistId}/reviewers")
  Response<List<ChecklistCollaboratorView>> getAllReviewers(@PathVariable Long checklistId);

  @GetMapping("/{checklistId}/sign-off-users")
  Response<List<ChecklistCollaboratorView>> getAllSignOffUsers(@PathVariable Long checklistId);

  @GetMapping("/{checklistId}/collaborators/{phaseType}")
  Response<List<ChecklistCollaboratorView>> getAllCollaborators(@PathVariable Long checklistId, @PathVariable State.ChecklistCollaboratorPhaseType phaseType);

  @PatchMapping("/{checklistId}/review/start")
  Response<ChecklistReviewDto> startReview(@PathVariable Long checklistId) throws ResourceNotFoundException, StreemException;

  @PatchMapping("/{checklistId}/review/ok")
  Response<ChecklistCommentDto> commentedOk(@PathVariable Long checklistId) throws ResourceNotFoundException,
    StreemException;

  @PatchMapping("/{checklistId}/review/changes")
  Response<ChecklistCommentDto> commentedChanges(@PathVariable Long checklistId, @RequestBody CommentAddRequest commentAddRequest) throws ResourceNotFoundException, StreemException;

  @PatchMapping("/{checklistId}/review/submit-back")
  Response<ChecklistReviewDto> submitBack(@PathVariable Long checklistId) throws ResourceNotFoundException, StreemException;

  @PatchMapping("/{checklistId}/review/sign-off/initiate")
  Response<ChecklistBasicDto> initiateSignOff(@PathVariable Long checklistId) throws ResourceNotFoundException, StreemException;

  @PostMapping("/{checklistId}/review/sign-off/order")
  Response<ChecklistReviewDto> signOffOrderTree(@PathVariable Long checklistId, @RequestBody SignOffOrderTreeRequest signOffOrderTreeRequest) throws ResourceNotFoundException, StreemException;

  @PatchMapping("/{checklistId}/review/sign-off")
  Response<ChecklistReviewDto> signOff(@PathVariable Long checklistId) throws ResourceNotFoundException, StreemException;


  @PatchMapping("/{checklistId}/publish")
  Response<ChecklistBasicDto> publish(@PathVariable Long checklistId) throws ResourceNotFoundException, StreemException;

  @GetMapping("/{checklistId}/reviewer/{reviewerId}/comment")
  Response<List<CollaboratorCommentDto>> getComments(@PathVariable Long checklistId, @PathVariable Long reviewerId, @RequestBody ChecklistCollaboratorAssignmentRequest checklistCollaboratorAssignmentRequest) throws ResourceNotFoundException, StreemException;

  @PostMapping("/{checklistId}/revision")
  @ResponseBody
  Response<ChecklistDto> createChecklistRevision(@PathVariable Long checklistId) throws ResourceNotFoundException, StreemException, IOException;

  @PatchMapping("/{checklistId}/revision/validate")
  Response<BasicDto> validateIfCurrentUserCanReviseChecklist(@PathVariable Long checklistId) throws ResourceNotFoundException, StreemException;

  @GetMapping("/{checklistId}/parameters")
  Response<Page<ParameterInfoDto>> getParametersByTargetEntityType(@PathVariable Long checklistId, @RequestParam(name = "filters", defaultValue = "") String filters, Pageable pageable);

  @PatchMapping("/{checklistId}/parameters/map")
  Response<List<ParameterInfoDto>> configureProcessParameters(@PathVariable Long checklistId, @RequestBody MapJobParameterRequest mapJobParameterRequest) throws StreemException, ResourceNotFoundException;

  @GetMapping("/{checklistId}/trained")
  Response<Page<TrainedUsersDto>> getTrainedUsers(@PathVariable Long checklistId, @RequestParam(value = "users", required = false) Boolean isUser, @RequestParam(value = "userGroups", required = false) Boolean isUserGroup, @RequestParam(value = "query", required = false) String query, Pageable pageable) throws StreemException;

  @PostMapping("/{checklistId}/task/assignment")
  Response<List<TaskAssigneeView>> getAssignmentList(@PathVariable Long checklistId, @RequestBody TaskAssigneeDto taskAssigneeDto) throws ResourceNotFoundException;

  @PatchMapping("/{checklistId}/users/assignment")
  Response<BasicDto> bulkAssignment(@PathVariable Long checklistId, @RequestBody ChecklistTaskAssignmentRequest assignmentRequest,
                                    @RequestParam(required = false, defaultValue = "false") boolean notify) throws StreemException, ResourceNotFoundException;

  @GetMapping("/{checklistId}/facility/share")
  Response<List<FacilityDto>> getAllFacilitiesByChecklistId(@PathVariable Long checklistId) throws ResourceNotFoundException;

  @PatchMapping("/{checklistId}/facility/share")
  Response<BasicDto> addFacilitiesToChecklist(@PathVariable Long checklistId, @RequestBody ChecklistFacilityAssignmentRequest checklistFacilityAssignmentRequest
  ) throws ResourceNotFoundException;

  @PatchMapping("/{checklistId}/job-log-columns/re-configure")
  @ResponseBody
  Response<BasicDto> reconfigureJobLogColumns(@PathVariable Long checklistId) throws ResourceNotFoundException;

  @PostMapping("/import")
  @ResponseBody
  Response<BasicDto> importChecklists(@RequestParam(name = "useCaseId") Long useCaseId, @RequestParam("file") MultipartFile file) throws StreemException, ResourceNotFoundException, IOException;

  @GetMapping("/export")
  @ResponseBody
  void exportChecklists(@RequestParam(required = false) List<Long> ids, HttpServletResponse response) throws IOException, ResourceNotFoundException, StreemException;

  @GetMapping("/by/resource/{objectTypeId}")
  Response<Page<ChecklistView>> getAllByResource(@PathVariable("objectTypeId") String objectTypeId, @RequestParam("objectId") String objectId, @RequestParam("useCaseId") Long useCaseId, @RequestParam(value = "archived", required = false, defaultValue = "false") boolean archived, @RequestParam(value = "name", required = false, defaultValue = "") String name, Pageable pageable) throws JsonProcessingException;

  @PostMapping("/{checklistId}/recall")
  Response<ChecklistReviewDto> recall(@PathVariable Long checklistId, @RequestBody RecallProcessDto recallProcessDto) throws ResourceNotFoundException, StreemException;

  @PatchMapping("/{checklistId}/recall/validate")
  Response<BasicDto> validateIfCurrentUserCanRecallChecklist(@PathVariable Long checklistId) throws ResourceNotFoundException, StreemException;

  @PatchMapping("/{checklistId}/trained-users/map")
  Response<BasicDto> mapTrainedUsers(@PathVariable Long checklistId, @RequestBody TrainedUserMappingRequest trainedUserMappingRequest) throws ResourceNotFoundException, StreemException;

  @GetMapping("/{checklistId}/untrained")
  Response<Page<TrainedUsersDto>> getUnTrainedUsers(@PathVariable Long checklistId, @RequestParam(value = "users", required = false) Boolean isUser, @RequestParam(value = "userGroups", required = false) Boolean isUserGroup, @RequestParam(value = "query", required = false) String query, Pageable pageable) throws StreemException;

  @GetMapping("/{checklistId}/trained-users/assigned-tasks")
  Response<Page<TrainedUsersDto>> getTrainedUsersWithAssignedTasks(@PathVariable Long checklistId, @RequestParam(value = "query", required = false) String query) throws StreemException;

  @PostMapping("/{checklistId}/copy")
  Response<IChecklistElementDto> copyChecklistElement(@PathVariable Long checklistId, @RequestBody CopyChecklistElementRequest copyChecklistRequest) throws ResourceNotFoundException, StreemException;

  @PostMapping("/{checklistId}/custom-publish")
  Response<ChecklistBasicDto> customPublishChecklist(@PathVariable Long checklistId) throws ResourceNotFoundException, StreemException, JsonProcessingException;

  @GetMapping("/{checklistId}/job-log-columns")
  Response<JobLogsColumnDto> getJobLogColumns(@PathVariable Long checklistId) throws ResourceNotFoundException, IOException;


  @GetMapping("/{checklistId}/actions")
  Response<Page<ActionDto>> getActionByChecklistId(@PathVariable Long checklistId, Pageable pageable);

  @GetMapping("/{checklistId}/pdf")
  @ResponseBody
  ResponseEntity<byte[]> getProcessTemplatePdf(@PathVariable Long checklistId) throws ResourceNotFoundException, IOException;

}
