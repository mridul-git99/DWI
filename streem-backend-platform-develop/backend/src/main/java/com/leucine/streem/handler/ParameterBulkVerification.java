package com.leucine.streem.handler;

import com.leucine.streem.dto.*;
import com.leucine.streem.dto.request.ParameterVerificationRequest;
import com.leucine.streem.exception.ParameterExecutionException;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.ParameterValue;
import com.leucine.streem.model.TaskExecution;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.repository.IParameterValueRepository;
import com.leucine.streem.service.IParameterVerificationService;
import com.leucine.streem.service.impl.JobAuditService;
import com.leucine.streem.service.impl.JwtService;
import com.leucine.streem.util.DateTimeUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class ParameterBulkVerification implements IParameterBulkVerification {
  private final JobAuditService jobAuditService;
  private final IParameterVerificationService parameterVerificationService;
  private final IParameterValueRepository parameterValueRepository;
  private final JwtService jwtService;
  @Override
  public List<ParameterVerificationDto> bulkPeerAssign(List<BulkPeerAssigneesRequest> bulkPeerAssigneesRequest) throws StreemException, IOException, ResourceNotFoundException {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    List<ParameterVerificationDto> parameterVerifications = new ArrayList<>();

    Long parameterExecutionId = bulkPeerAssigneesRequest.stream().findFirst().get().getParameterExecutionId();
    ParameterValue parameterValue = parameterValueRepository.getReferenceById(parameterExecutionId);
    TaskExecution taskExecution = parameterValue.getTaskExecution();
    Long jobId = taskExecution.getJobId();
    jobAuditService.initiateBulkPeerVerification(jobId, taskExecution.getTask(), principalUser);

    for (BulkPeerAssigneesRequest peerAssignRequest : bulkPeerAssigneesRequest) {
      parameterVerifications.addAll(parameterVerificationService.sendForPeerVerification(peerAssignRequest.getParameterExecutionId(),peerAssignRequest.getPeerAssignees(),true));
    }

    return parameterVerifications;
  }

  @Override
  public List<ParameterVerificationDto> bulkPeerVerification(BulkPeerVerificationRequest bulkPeerVerificationRequest) throws StreemException, IOException, ResourceNotFoundException {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    List<ParameterVerificationDto> parameterVerifications = new ArrayList<>();
    Long parameterExecutionId = bulkPeerVerificationRequest.getPeerVerify().stream().findFirst().get().getParameterExecutionId();
    ParameterValue parameterValue = parameterValueRepository.getReferenceById(parameterExecutionId);
    TaskExecution taskExecution = parameterValue.getTaskExecution();
    Long jobId = taskExecution.getJobId();

    boolean bulkIsSameSession = Boolean.TRUE.equals(bulkPeerVerificationRequest.getSameSession());
    PrincipalUser initiatorUser = null;
    if (bulkIsSameSession) {
      initiatorUser = jwtService.parseAndValidate(bulkPeerVerificationRequest.getInitiatorJwtToken());
    }
    for(VerifyRequest peerVerifyRequest : bulkPeerVerificationRequest.getPeerVerify()){
      Long checkedAt = (peerVerifyRequest.getCheckedAt() == null) ? DateTimeUtils.now() : peerVerifyRequest.getCheckedAt();
      if (bulkIsSameSession && initiatorUser != null) {
        jobAuditService.logBulkSameSessionParameterReviewed(peerVerifyRequest.getParameterExecutionId(), checkedAt, initiatorUser, principalUser);
      } else {
        jobAuditService.logBulkPeerParameterExamination(peerVerifyRequest.getParameterExecutionId(), checkedAt, principalUser);
      }

      ParameterVerificationRequest parameterVerificationRequest = new ParameterVerificationRequest();
      parameterVerificationRequest.setSameSession(bulkIsSameSession);
      if (bulkIsSameSession) {
        parameterVerificationRequest.setInitiatorJwtToken(bulkPeerVerificationRequest.getInitiatorJwtToken());
      }

      parameterVerifications.add(parameterVerificationService.acceptPeerVerification(peerVerifyRequest.getParameterExecutionId(),true,peerVerifyRequest.getCheckedAt(),parameterVerificationRequest));
    };

    if(bulkIsSameSession && initiatorUser != null){
      jobAuditService.acceptBulkSameSessionPeerVerification(jobId, taskExecution.getTask(), principalUser, initiatorUser);
    }
    else {
      jobAuditService.acceptBulkPeerVerification(jobId, taskExecution.getTask(), principalUser);
    }
    return parameterVerifications;
  }

  @Override
  @Transactional(rollbackOn = Exception.class)
  public List<ParameterVerificationDto> bulkSelfVerification(BulkSelfVerificationRequest bulkSelfVerificationRequest) throws StreemException, IOException, ResourceNotFoundException {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    List<ParameterVerificationDto> parameterVerifications = new ArrayList<>();
    for (VerifyRequest selfVerifyRequest : bulkSelfVerificationRequest.getSelfVerify()) {
      jobAuditService.checkVerification(selfVerifyRequest.getParameterExecutionId(),selfVerifyRequest.getCheckedAt(),principalUser);
    }

    ParameterValue parameterValue = parameterValueRepository.getReferenceById(bulkSelfVerificationRequest.getSelfVerify().stream().findFirst().get().getParameterExecutionId());
    TaskExecution taskExecution = parameterValue.getTaskExecution();
    Long jobId = taskExecution.getJobId();
    jobAuditService.initiateBulkSelfVerification(jobId, bulkSelfVerificationRequest.getSelfVerify().stream().findFirst().get().getCheckedAt(), taskExecution.getTask(), principalUser);
    for (VerifyRequest selfVerifyRequest : bulkSelfVerificationRequest.getSelfVerify()) {
      parameterVerificationService.initiateSelfVerification(selfVerifyRequest.getParameterExecutionId(), true);
      jobAuditService.logBulkSelfParameterExamination(selfVerifyRequest.getParameterExecutionId(), selfVerifyRequest.getCheckedAt(), principalUser);
      parameterVerifications.add(parameterVerificationService.acceptSelfVerification(selfVerifyRequest.getParameterExecutionId(), true, selfVerifyRequest.getCheckedAt()));
    }
    jobAuditService.approveBulkSelfVerification(jobId, taskExecution.getTask(), bulkSelfVerificationRequest.getSelfVerify().stream().findFirst().get().getCheckedAt(), principalUser);
    return parameterVerifications;
  }
}
