package com.leucine.streem.handler;

import com.leucine.streem.dto.BulkPeerAssigneesRequest;
import com.leucine.streem.dto.BulkPeerVerificationRequest;
import com.leucine.streem.dto.BulkSelfVerificationRequest;
import com.leucine.streem.dto.ParameterVerificationDto;
import com.leucine.streem.exception.ParameterExecutionException;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;

import java.io.IOException;
import java.util.List;

public interface IParameterBulkVerification {
  List<ParameterVerificationDto> bulkSelfVerification(BulkSelfVerificationRequest bulkSelfVerificationRequest) throws ResourceNotFoundException, StreemException, IOException;

  List<ParameterVerificationDto> bulkPeerVerification(BulkPeerVerificationRequest bulkPeerVerificationRequest) throws StreemException, IOException, ResourceNotFoundException, ParameterExecutionException;

  List<ParameterVerificationDto> bulkPeerAssign(List<BulkPeerAssigneesRequest> bulkPeerAssigneesRequest) throws StreemException, IOException, ResourceNotFoundException;

}
