package com.leucine.streem.controller.impl;

import com.leucine.streem.controller.IParameterVerificationController;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.projection.UserGroupView;
import com.leucine.streem.dto.request.ParameterVerificationRequest;
import com.leucine.streem.dto.request.PeerAssignRequest;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ParameterExecutionException;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.handler.IParameterBulkVerification;
import com.leucine.streem.service.IParameterVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class ParameterVerificationController implements IParameterVerificationController {

  private final IParameterVerificationService parameterVerificationService;
  private final IParameterBulkVerification bulkParameterVerificationHandler;

  @Autowired
  public ParameterVerificationController(IParameterVerificationService parameterVerificationService, IParameterBulkVerification bulkParameterVerificationHandler) {
    this.parameterVerificationService = parameterVerificationService;
    this.bulkParameterVerificationHandler = bulkParameterVerificationHandler;
  }


  @Override
  public Response<ParameterVerificationDto> initiateSelfVerification(Long parameterExecutionId) throws ResourceNotFoundException, StreemException, IOException {
    return Response.builder().data(parameterVerificationService.initiateSelfVerification(parameterExecutionId, false)).build();
  }

  @Override
  public Response<ParameterVerificationDto> acceptSelfVerification(Long parameterExecutionId, Long checkedAt) throws ResourceNotFoundException, StreemException, IOException {
    return Response.builder().data(parameterVerificationService.acceptSelfVerification(parameterExecutionId, false, checkedAt)).build();
  }

  @Override
  public Response<List<ParameterVerificationDto>> sendForPeerVerification(Long parameterExecutionId, PeerAssignRequest peerAssignRequest) throws ResourceNotFoundException, StreemException, IOException {
    return Response.builder().data(parameterVerificationService.sendForPeerVerification(parameterExecutionId, peerAssignRequest, false)).build();
  }

  @Override
  public Response<ParameterVerificationDto> recallPeerVerification(Long parameterExecutionId) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(parameterVerificationService.recallPeerVerification(parameterExecutionId)).build();
  }

  @Override
  public Response<ParameterVerificationDto> recallSelfVerification(Long parameterExecutionId) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(parameterVerificationService.recallSelfVerification(parameterExecutionId)).build();
  }

  @Override
  public Response<ParameterVerificationDto> rejectPeerVerification(Long parameterExecutionId, ParameterVerificationRequest parameterVerificationRequest) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(parameterVerificationService.rejectPeerVerification(parameterExecutionId, parameterVerificationRequest)).build();
  }

  @Override
  public Response<ParameterVerificationDto> acceptPeerVerification(Long parameterExecutionId,Long checkedAt, ParameterVerificationRequest parameterVerificationRequest) throws ResourceNotFoundException, StreemException, IOException, ParameterExecutionException {
    return Response.builder().data(parameterVerificationService.acceptPeerVerification(parameterExecutionId, false, checkedAt, parameterVerificationRequest)).build();
  }

  @Override
  public Response<Object> getAssignees(Long jobId, String filters) {
    return parameterVerificationService.getAssignees(jobId, filters);
  }

  @Override
  public Response<Page<ParameterVerificationListViewDto>> getUserAssignedAndRequestedVerifications(String status, Long jobId, Long requestedTo, Long requestedBy, String parameterName, String processName, String objectId, Long useCaseId, Pageable pageable) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(parameterVerificationService.getAllVerifications(status, jobId, requestedTo, requestedBy, parameterName, processName, objectId, useCaseId, pageable)).build();
  }


  @Override
  public Response<List<ParameterVerificationDto>> bulkPeerVerification(BulkPeerVerificationRequest bulkPeerVerificationRequest) throws ResourceNotFoundException, StreemException, IOException, ParameterExecutionException {
    return Response.builder().data(bulkParameterVerificationHandler.bulkPeerVerification(bulkPeerVerificationRequest)).build();
  }

  @Override
  public Response<List<ParameterVerificationDto>> bulkPeerAssign(List<BulkPeerAssigneesRequest> bulkPeerAssigneeRequest) throws ResourceNotFoundException, StreemException, IOException {
    return Response.builder().data(bulkParameterVerificationHandler.bulkPeerAssign(bulkPeerAssigneeRequest)).build();
  }

  @Override
  public Response<List<ParameterVerificationDto>> bulkSelfVerification(BulkSelfVerificationRequest bulkSelfVerificationRequest) throws ResourceNotFoundException, StreemException, IOException {
    return Response.builder().data(bulkParameterVerificationHandler.bulkSelfVerification(bulkSelfVerificationRequest)).build();
  }

  @Override
  public Response<List<UserGroupView>> getGroupAssignees(Long jobId, String query) {
    return Response.builder().data(parameterVerificationService.getUserGroupAssignees(jobId, query)).build();
  }
}
