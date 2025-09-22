package com.leucine.streem.service;

import com.leucine.streem.dto.ParameterVerificationDto;
import com.leucine.streem.dto.ParameterVerificationListViewDto;
import com.leucine.streem.dto.projection.UserGroupView;
import com.leucine.streem.dto.request.ParameterVerificationRequest;
import com.leucine.streem.dto.request.PeerAssignRequest;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ParameterExecutionException;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.ParameterVerification;
import com.leucine.streem.model.TempParameterVerification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public interface IParameterVerificationService {
  ParameterVerificationDto initiateSelfVerification(Long parameterExecutionId, boolean isBulk) throws ResourceNotFoundException, StreemException, IOException;

  ParameterVerificationDto acceptSelfVerification(Long parameterExecutionId, boolean isBulk, Long checkedAt) throws ResourceNotFoundException, StreemException, IOException;

  List<ParameterVerificationDto> sendForPeerVerification(Long parameterExecutionId, PeerAssignRequest peerAssignRequest, boolean isBulk) throws ResourceNotFoundException, StreemException, IOException;

  ParameterVerificationDto recallSelfVerification(Long parameterExecutionId) throws ResourceNotFoundException, StreemException;

  ParameterVerificationDto recallPeerVerification(Long parameterExecutionId) throws ResourceNotFoundException, StreemException;

  ParameterVerificationDto rejectPeerVerification(Long parameterExecutionId, ParameterVerificationRequest parameterVerificationRequest) throws ResourceNotFoundException, StreemException;

  ParameterVerificationDto acceptPeerVerification(Long parameterExecutionId, boolean isBulk, Long checkedAt,ParameterVerificationRequest parameterVerificationRequest) throws ResourceNotFoundException, StreemException, IOException;

  Page<ParameterVerificationListViewDto> getAllVerifications(String status, Long jobId, Long requestedTo, Long requestedBy, String parameterName, String processName, String objectId,Long useCaseId, Pageable pageable) throws ResourceNotFoundException, StreemException;

  Response<Object> getAssignees(Long jobId, String filters);

  Map<Long, List<ParameterVerification>> getParameterVerificationsDataForAJob(Long jobId);

  Map<Long, List<TempParameterVerification>> getTempParameterVerificationsDataForAJob(Long jobId);

  List<UserGroupView> getUserGroupAssignees(Long jobId, String query);


}
