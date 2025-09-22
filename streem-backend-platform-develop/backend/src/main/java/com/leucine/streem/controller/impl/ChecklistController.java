package com.leucine.streem.controller.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leucine.streem.constant.State;
import com.leucine.streem.constant.Type;
import com.leucine.streem.controller.IChecklistController;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.projection.ChecklistCollaboratorView;
import com.leucine.streem.dto.projection.ChecklistView;
import com.leucine.streem.dto.projection.TaskAssigneeView;
import com.leucine.streem.dto.request.*;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.service.*;
import com.leucine.streem.util.DateTimeUtils;
import com.leucine.streem.util.IdGenerator;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class ChecklistController implements IChecklistController {
  private final IChecklistService checklistService;
  private final IChecklistCollaboratorService checklistCollaboratorService;
  private final IChecklistRevisionService checklistRevisionService;
  private final IChecklistTrainedUserService checklistTrainedUserService;
  private final IParameterService parameterService;
  private final ObjectMapper objectMapper;
  private final IActionService actionService;
  private final IimportExportChecklistService importExportChecklistService;
  private final IPdfReportBuilderFactory pdfReportBuilderFactory;
  private final IPdfBuilderService pdfBuilderService;

  @Autowired
  public ChecklistController(IChecklistService checklistService, IChecklistCollaboratorService checklistCollaboratorService,
                             IChecklistRevisionService checklistRevisionService, IChecklistTrainedUserService checklistTrainedUserService, 
                             IParameterService parameterService, ObjectMapper objectMapper, IActionService actionService, 
                             IimportExportChecklistService importExportChecklistService, IPdfReportBuilderFactory pdfReportBuilderFactory,
                             IPdfBuilderService pdfBuilderService) {
    this.checklistService = checklistService;
    this.checklistCollaboratorService = checklistCollaboratorService;
    this.checklistRevisionService = checklistRevisionService;
    this.checklistTrainedUserService = checklistTrainedUserService;
    this.parameterService = parameterService;
    this.objectMapper = objectMapper;
    this.actionService = actionService;
    this.importExportChecklistService = importExportChecklistService;
    this.pdfReportBuilderFactory = pdfReportBuilderFactory;
    this.pdfBuilderService = pdfBuilderService;
  }

  @Override
  public Response<Page<ChecklistPartialDto>> getAll(String filters, Pageable pageable) {
    return Response.builder().data(checklistService.getAllChecklist(filters, pageable)).build();
  }

  @Override
  public Response<ChecklistDto> getChecklist(Long checklistId) throws ResourceNotFoundException {
    return Response.builder().data(checklistService.getChecklistById(checklistId)).build();
  }

  @Override
  public Response<ChecklistInfoDto> getChecklistInfo(Long checklistId) throws ResourceNotFoundException {
    return Response.builder().data(checklistService.getChecklistInfoById(checklistId)).build();
  }

  @Override
  public Response<ChecklistDto> createChecklist(CreateChecklistRequest createChecklistRequest) throws StreemException, ResourceNotFoundException {
    return Response.builder().data(checklistService.createChecklist(createChecklistRequest)).build();
  }

  @Override
  public Response<BasicDto> archiveChecklist(Long checklistId, ArchiveChecklistRequest archiveChecklistRequest) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(checklistService.archiveChecklist(checklistId, archiveChecklistRequest.getReason())).build();
  }

  @Override
  public Response<List<ParameterInfoDto>> configureProcessParameters(Long checklistId, MapJobParameterRequest mapJobParameterRequest) throws StreemException, ResourceNotFoundException {
    return Response.builder().data(checklistService.configureProcessParameters(checklistId, mapJobParameterRequest)).build();
  }

  @Override
  public Response<BasicDto> validateChecklistArchival(Long checklistId) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(checklistService.validateChecklistArchival(checklistId)).build();
  }

  @Override
  public Response<BasicDto> unarchiveChecklist(Long checklistId, UnarchiveChecklistRequest unarchiveChecklistRequest) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(checklistService.unarchiveChecklist(checklistId, unarchiveChecklistRequest.getReason())).build();
  }

  @Override
  public Response<BasicDto> validateChecklist(Long checklistId) throws ResourceNotFoundException, IOException, StreemException {
    return Response.builder().data(checklistService.validateChecklist(checklistId)).build();
  }

  @Override
  public Response<BasicDto> updateChecklist(Long checklistId, ChecklistUpdateRequest checklistUpdateRequest) throws StreemException, ResourceNotFoundException {
    return Response.builder().data(checklistService.updateChecklist(checklistId, checklistUpdateRequest)).build();
  }

  @Override
  public Response<ChecklistBasicDto> reviewerAssignments(Long checklistId, ChecklistCollaboratorAssignmentRequest checklistCollaboratorAssignmentRequest) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(checklistCollaboratorService.assignments(checklistId, checklistCollaboratorAssignmentRequest)).build();
  }

  @Override
  public Response<List<ChecklistCollaboratorView>> getAllAuthors(Long checklistId) {
    return Response.builder().data(checklistCollaboratorService.getAllAuthors(checklistId)).build();
  }

  @Override
  public Response<List<ChecklistCollaboratorView>> getAllReviewers(Long checklistId) {
    return Response.builder().data(checklistCollaboratorService.getAllReviewers(checklistId)).build();
  }

  @Override
  public Response<List<ChecklistCollaboratorView>> getAllSignOffUsers(Long checklistId) {
    return Response.builder().data(checklistCollaboratorService.getAllSignOffUsers(checklistId)).build();
  }

  @Override
  public Response<List<ChecklistCollaboratorView>> getAllCollaborators(Long checklistId, State.ChecklistCollaboratorPhaseType phaseType) {
    return Response.builder().data(checklistCollaboratorService.getAllCollaborators(checklistId, phaseType)).build();
  }

  @Override
  public Response<ChecklistBasicDto> submitForReview(Long checklistId) throws ResourceNotFoundException, StreemException, JsonProcessingException {
    return Response.builder().data(checklistCollaboratorService.submitForReview(checklistId)).build();
  }

  @Override
  public Response<ChecklistReviewDto> startReview(Long checklistId) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(checklistCollaboratorService.startReview(checklistId)).build();
  }

  @Override
  public Response<ChecklistCommentDto> commentedOk(Long checklistId) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(checklistCollaboratorService.commentedOk(checklistId)).build();
  }

  @Override
  public Response<ChecklistCommentDto> commentedChanges(Long checklistId, CommentAddRequest commentAddRequest) throws ResourceNotFoundException,
    StreemException {
    return Response.builder().data(checklistCollaboratorService.commentedChanges(checklistId, commentAddRequest)).build();
  }

  @Override
  public Response<ChecklistReviewDto> submitBack(Long checklistId) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(checklistCollaboratorService.submitBack(checklistId)).build();
  }

  @Override
  public Response<ChecklistBasicDto> initiateSignOff(Long checklistId) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(checklistCollaboratorService.initiateSignOff(checklistId)).build();
  }

  @Override
  public Response<ChecklistReviewDto> signOffOrderTree(Long checklistId, SignOffOrderTreeRequest signOffOrderTreeRequest) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(checklistCollaboratorService.signOffOrderTree(checklistId, signOffOrderTreeRequest)).build();
  }

  @Override
  public Response<ChecklistReviewDto> signOff(Long checklistId) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(checklistCollaboratorService.signOff(checklistId)).build();
  }

  @Override
  public Response<ChecklistBasicDto> publish(Long checklistId) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(checklistCollaboratorService.publish(checklistId)).build();
  }

  @Override
  public Response<List<CollaboratorCommentDto>> getComments(Long checklistId, Long reviewerId, ChecklistCollaboratorAssignmentRequest checklistCollaboratorAssignmentRequest) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(checklistCollaboratorService.getComments(checklistId, reviewerId)).build();
  }

  @Override
  public Response<ChecklistDto> createChecklistRevision(Long checklistId) throws ResourceNotFoundException, StreemException, IOException {
    return Response.builder().data(checklistRevisionService.createChecklistRevision(checklistId)).build();
  }


  @Override
  public Response<BasicDto> validateIfCurrentUserCanReviseChecklist(@PathVariable Long checklistId) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(checklistRevisionService.validateIfCurrentUserCanReviseChecklist(checklistId)).build();
  }


  @Override
  public Response<Page<ParameterInfoDto>> getParametersByTargetEntityType(Long checklistId, String filters, Pageable pageable) {
    return Response.builder().data(parameterService.getAllParameters(checklistId, filters, pageable)).build();
  }

  @Override
  public Response<List<TaskAssigneeView>> getAssignmentList(Long checklistId, TaskAssigneeDto taskAssigneeDto) throws ResourceNotFoundException {
    return Response.builder().data(checklistService.getTaskAssignmentDetails(checklistId, taskAssigneeDto.isUsers(), taskAssigneeDto.isUserGroups(), taskAssigneeDto.getTask())).build();
  }

  @Override
  public Response<Page<TrainedUsersDto>> getTrainedUsers(Long checklistId, Boolean isUser, Boolean isUserGroup, String query, Pageable pageable) throws StreemException {
    return Response.builder().data(checklistTrainedUserService.getTrainedUsers(checklistId, isUser, isUserGroup, query, pageable)).build();
  }

  @Override
  public Response<BasicDto> bulkAssignment(Long checklistId, ChecklistTaskAssignmentRequest assignmentRequest, boolean notify) throws StreemException, ResourceNotFoundException {
    return Response.builder().data(checklistService.bulkAssignDefaultUsers(checklistId, assignmentRequest, notify)).build();
  }

  @Override
  public Response<List<FacilityDto>> getAllFacilitiesByChecklistId(Long checklistId) throws ResourceNotFoundException {
    return Response.builder().data((checklistService.getFacilityChecklistMapping(checklistId))).build();
  }

  @Override
  public Response<BasicDto> addFacilitiesToChecklist(Long checklistId, ChecklistFacilityAssignmentRequest checklistFacilityAssignmentRequest) throws ResourceNotFoundException {
    return Response.builder().data(checklistService.bulkAssignmentFacilityIds(checklistId, checklistFacilityAssignmentRequest)).build();
  }

  @Override
  public Response<BasicDto> reconfigureJobLogColumns(Long checklistId) throws ResourceNotFoundException {
    return Response.builder().data(checklistService.reconfigureJobLogColumns(checklistId)).build();
  }

  @Override
  public Response<BasicDto> importChecklists(Long useCaseId, MultipartFile file) throws StreemException, ResourceNotFoundException, IOException {
    return Response.builder().data(importExportChecklistService.importChecklists(useCaseId, file)).build();
  }

  @Override
  public void exportChecklists(List<Long> ids, HttpServletResponse httpServletResponse) throws IOException, ResourceNotFoundException, StreemException {
    httpServletResponse.setContentType("application/zip");
    httpServletResponse.setHeader("Content-Disposition", "attachment; filename=download.zip");
    httpServletResponse.setStatus(HttpServletResponse.SC_OK);
    importExportChecklistService.populateMissingMediasDetails(ids);
    var data = importExportChecklistService.exportChecklists(ids);
    List<MultipartFile> medias = importExportChecklistService.getAllMediaMultiPart(ids);

    try (var zipOutPutStream = new ZipOutputStream(httpServletResponse.getOutputStream())) {
      ZipEntry processData = new ZipEntry(IdGenerator.getInstance().generateUnique() + ".json");
      processData.setTime(System.currentTimeMillis());
      try {
        var processInputStream = new ByteArrayInputStream(objectMapper.writeValueAsBytes(data));
        zipOutPutStream.putNextEntry(processData);
        StreamUtils.copy(processInputStream, zipOutPutStream);
        zipOutPutStream.closeEntry();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      for (MultipartFile multipartFile : medias) {
        ZipEntry media = new ZipEntry(multipartFile.getName());
        media.setTime(System.currentTimeMillis());
        try {
          var mediaInputStream = new ByteArrayInputStream(multipartFile.getBytes());
          zipOutPutStream.putNextEntry(media);
          StreamUtils.copy(mediaInputStream, zipOutPutStream);
          zipOutPutStream.closeEntry();
        } catch (IOException e) {
          throw new StreemException("Unable to export process");
        }
      }

      zipOutPutStream.finish();
    } catch (IOException ex) {
      throw new StreemException("Unable to export process");
    }
  }

  @Override
  public Response<IChecklistElementDto> copyChecklistElement(Long checklistId, CopyChecklistElementRequest copyChecklistRequest) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(checklistService.copyChecklistElement(checklistId, copyChecklistRequest)).build();
  }

  @Override
  public Response<Page<TrainedUsersDto>> getTrainedUsersWithAssignedTasks(Long checklistId, String query) throws StreemException {
    return Response.builder().data(checklistTrainedUserService.getAllTrainedUserTaskMapping(checklistId, query)).build();
  }

  @Override
  public Response<Page<TrainedUsersDto>> getUnTrainedUsers(Long checklistId, Boolean isUser, Boolean isUserGroup, String query, Pageable pageable) {
    return Response.builder().data(checklistTrainedUserService.getUnTrainedUsers(checklistId, isUser, isUserGroup, query, pageable)).build();
  }

  @Override
  public Response<BasicDto> mapTrainedUsers(Long checklistId, TrainedUserMappingRequest trainedUserMappingRequest) throws StreemException {
    return Response.builder().data(checklistTrainedUserService.mapTrainedUsers(checklistId, trainedUserMappingRequest)).build();
  }

  @Override
  public Response<BasicDto> validateIfCurrentUserCanRecallChecklist(@PathVariable Long checklistId) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(checklistService.validateIfCurrentUserCanRecallChecklist(checklistId)).build();
  }

  @Override
  public Response<ChecklistReviewDto> recall(Long checklistId, RecallProcessDto recallProcessDto) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(checklistService.recallChecklist(checklistId, recallProcessDto)).build();
  }

  @Override
  public Response<Page<ChecklistView>> getAllByResource(String objectTypeId, String objectId, Long useCaseId, boolean archived, String name, Pageable pageable) throws JsonProcessingException {
    return Response.builder().data(checklistService.getAllByResource(objectTypeId, objectId, useCaseId, archived, name, pageable)).build();
  }

  @Override
  public Response<ChecklistBasicDto> customPublishChecklist(Long checklistId) throws ResourceNotFoundException, StreemException, JsonProcessingException {
    return Response.builder().data(checklistService.customPublishChecklist(checklistId)).build();
  }

  @Override
  public Response<Page<ActionDto>> getActionByChecklistId(Long checklistId, Pageable pageable) {
    return Response.builder().data(actionService.getActions(checklistId, pageable)).build();
  }

  @Override
  public Response<JobLogsColumnDto> getJobLogColumns(Long checklistId) throws ResourceNotFoundException, IOException {
    return Response.builder().data(checklistService.getJobLogColumns(checklistId)).build();
  }

  @Override
  public ResponseEntity<byte[]> getProcessTemplatePdf(Long checklistId) throws ResourceNotFoundException, IOException {
    // Generate PDF using service
    byte[] pdfBytes = checklistService.generateProcessTemplatePdf(checklistId);
    
    String filename = UUID.randomUUID().toString() + ".pdf";
    
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("attachment", filename);
    headers.setContentLength(pdfBytes.length);
    
    return ResponseEntity.ok()
        .headers(headers)
        .body(pdfBytes);
  }
}
