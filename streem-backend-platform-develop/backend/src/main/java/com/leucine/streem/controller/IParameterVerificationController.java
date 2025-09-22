package com.leucine.streem.controller;

import com.leucine.streem.dto.*;
import com.leucine.streem.dto.projection.UserGroupView;
import com.leucine.streem.dto.request.ParameterVerificationRequest;
import com.leucine.streem.dto.request.PeerAssignRequest;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ParameterExecutionException;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/v1/parameter-verifications")
public interface IParameterVerificationController {

  @PostMapping("/parameter-executions/{parameterExecutionId}/self/verify")
  Response<ParameterVerificationDto> initiateSelfVerification(@PathVariable Long parameterExecutionId) throws ResourceNotFoundException, StreemException, IOException;

  @PatchMapping("/parameter-executions/{parameterExecutionId}/self/accept")
  Response<ParameterVerificationDto> acceptSelfVerification(@PathVariable Long parameterExecutionId, @RequestParam(value = "checkedAt", required = false) Long checkedAt) throws Exception;

  @PostMapping("/parameter-executions/{parameterExecutionId}/peer/assign")
  Response<List<ParameterVerificationDto>> sendForPeerVerification(@PathVariable Long parameterExecutionId,
                                                                   @RequestBody PeerAssignRequest peerAssignRequest) throws ResourceNotFoundException, StreemException, IOException;

  @PatchMapping("/parameter-executions/{parameterExecutionId}/peer/recall")
  Response<ParameterVerificationDto> recallPeerVerification(@PathVariable Long parameterExecutionId) throws ResourceNotFoundException, StreemException;

  @PatchMapping("/parameter-executions/{parameterExecutionId}/self/recall")
  Response<ParameterVerificationDto> recallSelfVerification(@PathVariable Long parameterExecutionId) throws ResourceNotFoundException, StreemException;

  @PatchMapping("/parameter-executions/{parameterExecutionId}/peer/reject")
  Response<ParameterVerificationDto> rejectPeerVerification(@PathVariable Long parameterExecutionId,
                                                            @RequestBody ParameterVerificationRequest parameterVerificationRequest) throws ResourceNotFoundException, StreemException;

  @PatchMapping("/parameter-executions/{parameterExecutionId}/peer/accept")
  Response<ParameterVerificationDto> acceptPeerVerification(@PathVariable Long parameterExecutionId,
                                                            @RequestParam(value = "checkedAt", required = false) Long checkedAt,
                                                            @RequestBody(required = false) ParameterVerificationRequest parameterVerificationRequest) throws Exception;

  @GetMapping
  Response<Page<ParameterVerificationListViewDto>> getUserAssignedAndRequestedVerifications(@RequestParam(required = false) String status, @RequestParam(required = false) Long jobId, @RequestParam(required = false) Long requestedTo, @RequestParam(required = false) Long requestedBy, @RequestParam(required = false) String parameterName, @RequestParam(required = false) String processName, @RequestParam(required = false) String objectId, @RequestParam(required = false) Long useCaseId, Pageable pageable) throws ResourceNotFoundException, StreemException;

  @GetMapping("/jobs/{jobId}/assignees")
  Response<Object> getAssignees(@PathVariable Long jobId, @RequestParam(value = "filters", required = false) String filters);

  @GetMapping("/jobs/{jobId}/group/assignees")
  Response<List<UserGroupView>> getGroupAssignees(@PathVariable Long jobId, @RequestParam(value = "query", required = false) String query);

  @PatchMapping("/parameter-executions/bulk/self")
  Response<List<ParameterVerificationDto>> bulkSelfVerification(@RequestBody BulkSelfVerificationRequest bulkSelfVerificationRequest) throws Exception;

  @PatchMapping("/parameter-executions/bulk/peer")
  Response<List<ParameterVerificationDto>> bulkPeerVerification(@RequestBody BulkPeerVerificationRequest bulkPeerVerificationRequest) throws Exception;

  @PatchMapping("/parameter-executions/bulk/peer/assign")
  Response<List<ParameterVerificationDto>> bulkPeerAssign(@RequestBody List<BulkPeerAssigneesRequest> bulkPeerAssigneeRequest) throws ResourceNotFoundException, StreemException, IOException;

}
