package com.leucine.streem.controller.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.collections.partial.PartialEntityObject;
import com.leucine.streem.constant.State;
import com.leucine.streem.constant.Type;
import com.leucine.streem.controller.IParameterExecutionController;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.request.*;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ParameterExecutionException;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.service.IParameterExecutionHandler;
import com.leucine.streem.service.IParameterExecutionService;
import com.leucine.streem.util.lock.ParameterValueKey;
import com.leucine.streem.util.lock.ParameterValueLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class ParameterExecutionController implements IParameterExecutionController {
  private final IParameterExecutionService parameterService;
  private final IParameterExecutionHandler parameterExecutionHandler;

  @Autowired
  public ParameterExecutionController(IParameterExecutionService parameterService, IParameterExecutionHandler parameterExecutionHandler) {
    this.parameterService = parameterService;
    this.parameterExecutionHandler = parameterExecutionHandler;
  }

  @Override
  public Response<List<ParameterDto>> executeParameters(List<BulkParameterExecuteRequest> bulkParameterExecuteRequests) throws StreemException, IOException, ResourceNotFoundException {
    return Response.builder().data(parameterExecutionHandler.executeParameters(bulkParameterExecuteRequests)).build();
  }

  @Override
  public Response<ParameterDto> executeParameter(Long parameterExecutionId, ParameterExecuteRequest parameterExecuteRequest) throws IOException, StreemException, ResourceNotFoundException {
    return Response.builder().data(executeParameterRequest(parameterExecuteRequest.getJobId(), parameterExecutionId, parameterExecuteRequest)).build();
  }

  @Override
  public Response<TempParameterDto> fixError(Long parameterExecutionId, ParameterExecuteRequest parameterExecuteRequest) throws IOException, StreemException, ResourceNotFoundException {
    return Response.builder().data(executeTempParameterRequest(parameterExecutionId, parameterExecuteRequest)).build();
  }

  @Override
  public Response<ParameterDto> rejectParameter(Long parameterExecutionId, ParameterStateChangeRequest parameterStateChangeRequest) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(parameterService.rejectParameter(parameterExecutionId, parameterStateChangeRequest)).build();
  }

  @Override
  public Response<ParameterDto> approveParameter(Long parameterExecutionId, ParameterStateChangeRequest parameterStateChangeRequest) throws StreemException, IOException, ResourceNotFoundException, ParameterExecutionException {
    Response<ParameterDto> response = Response.builder().data(parameterService.approveParameter(parameterExecutionId, parameterStateChangeRequest)).build();
    State.ParameterExecution state = response.getData().getResponse().get(response.getData().getResponse().size() - 1).getState();
    if (state == State.ParameterExecution.EXECUTED) {
      ParameterExecuteRequest parameterExecuteRequest = new ParameterExecuteRequest();
      parameterExecuteRequest.setJobId(parameterStateChangeRequest.getJobId());
      ParameterRequest parameterRequest = new ParameterRequest();
      parameterRequest.setData(response.getData().getData());
      parameterRequest.setId(Long.valueOf(response.getData().getId()));
      parameterRequest.setLabel(response.getData().getLabel());
      parameterExecuteRequest.setParameter(parameterRequest);
      parameterExecuteRequest.setReferencedParameterId(Long.valueOf(response.getData().getId()));
      parameterExecutionHandler.executeParameter(parameterStateChangeRequest.getJobId(), parameterExecutionId, parameterExecuteRequest, Type.JobLogTriggerType.PARAMETER_VALUE, true, false, false);
    }
    return response;
  }

  @Override
  public Response<Page<PartialEntityObject>> getAllFilteredEntityObjects(Long parameterExecutionId, String query, String shortCode, Pageable pageable) throws IOException, ResourceNotFoundException {
    return Response.builder().data(parameterExecutionHandler.getAllFilteredEntityObjects(parameterExecutionId, query,shortCode, pageable)).build();
  }

  @Override
  public Response<List<VariationDto>> getAllVariationsOfParameterExecution(Long parameterExecutionId) {
    return Response.builder().data(parameterService.getAllVariationsOfParameterExecution(parameterExecutionId)).build();
  }

  @Override
  public Response<List<ParameterPartialDto>> getParameterPartialData(Long jobId, ParameterPartialRequest parameterPartialRequest) throws JsonProcessingException {
    return Response.builder().data(parameterService.getParameterPartialData(jobId,parameterPartialRequest)).build();
  }

  @Override
  public Response<List<ParameterPartialDto>> getParameterPartialDataForMaster(Long jobId, ParameterPartialRequest parameterPartialRequest) throws JsonProcessingException {
    return Response.builder().data(parameterService.getParameterPartialDataForMaster(jobId,parameterPartialRequest)).build();
  }

  @Override
  public Response<BasicDto> deleteVariation(DeleteVariationRequest deleteVariationRequest) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(parameterService.deleteVariation(deleteVariationRequest)).build();
  }

  @Override
  public Response<Page<VariationDto>> getAllVariationsOfJob(Long jobId, String parameterName, Pageable pageable) throws ResourceNotFoundException {
    return Response.builder().data(parameterService.getAllVariationsOfJob(jobId, parameterName, pageable)).build();
  }

  @Override
  public Response<Page<ParameterDto>> getAllParametersAvailableForVariations(Long jobId, String filters, String parameterName, Pageable pageable) {
    return Response.builder().data(parameterService.getAllAllowedParametersForVariations(jobId, filters, parameterName, pageable)).build();
  }

  @Override
  public Response<BasicDto> addVariations(CreateVariationRequest createVariationRequest) throws ResourceNotFoundException, StreemException, JsonProcessingException {
    return Response.builder().data(parameterService.createVariations(createVariationRequest)).build();
  }

  @Override
  public Response<RuleHideShowDto> executeTemporary(ParameterTemporaryExecuteRequest parameterTemporaryExecuteRequest) throws IOException {
    return Response.builder().data(parameterService.tempExecuteRules(parameterTemporaryExecuteRequest.parameterValues(), parameterTemporaryExecuteRequest.checklistId())).build();
  }

  @Override
  public Response<List<ParameterDto>> getParameterExecutionByParameterIdAndJobId(Long jobId,String filters) throws ResourceNotFoundException {
    return Response.builder().data(parameterService.getParameterExecutionByParameterIdAndJobId(jobId,filters)).build();
  }

  private ParameterDto executeParameterRequest(Long jobId, Long parameterExecutionId, ParameterExecuteRequest executeRequest) throws StreemException, IOException, ResourceNotFoundException {
    log.info("[executeParameterRequest] Request to execute parameter, jobId: {}, parameterExecuteRequest: {}", jobId, executeRequest);
    String key = jobId + "_" + parameterExecutionId;
    ParameterValueKey parameterValueKey = new ParameterValueKey(key);
    final boolean[] newlyCreated = {false};
    Lock lock = ParameterValueLock.lock.get(parameterValueKey, k -> {
      newlyCreated[0] = true;
      return new ReentrantLock(true);
    });
    log.info("[executeParameterRequest] Lock {} for jobId: {}, parameterExecutionId: {}, parameterExecuteRequest: {}", newlyCreated[0] ? "not present" : "already present", jobId, parameterExecutionId, executeRequest);
    try {
      if (lock.tryLock(1, TimeUnit.MINUTES)) {// Attempt to acquire the lock with a timeout
        return parameterExecutionHandler.executeParameter(executeRequest.getJobId(), parameterExecutionId, executeRequest, Type.JobLogTriggerType.PARAMETER_VALUE, false, false, false);
      } else {
        log.warn("[executeParameter] Could not acquire lock within the timeout for jobId: {}, parameterExecutionId: {}, parameterExecuteRequest: {}", jobId, parameterExecutionId, executeRequest);
        // Handle the case where the lock was not acquired within the timeout
        return null;
      }
    } catch (InterruptedException | ParameterExecutionException e) {
      log.error("[executeParameter] Error while executing parameter, jobId: {}, parameterExecutionId: {}, parameterExecuteRequest: {}", jobId, parameterExecutionId, executeRequest, e);
      return null;
    } finally {
      lock.unlock();
    }
  }

  private TempParameterDto executeTempParameterRequest(Long parameterExecutionId, ParameterExecuteRequest executeRequest) throws IOException, StreemException, ResourceNotFoundException {
    log.info("[executeTempParameterRequest] Request to execute parameter, parameterExecuteRequest: {}", executeRequest);
    String key = executeRequest.getJobId() + "_" + parameterExecutionId;
    ParameterValueKey parameterValueKey = new ParameterValueKey(key);
    final boolean[] newlyCreated = {false};
    Lock lock = ParameterValueLock.lock.get(parameterValueKey, k -> {
      newlyCreated[0] = true;
      return new ReentrantLock(true);
    });
    log.info("[executeTempParameterRequest] Lock {} for jobId: {}, parameterExecutionId: {}, parameterExecuteRequest: {}", newlyCreated[0] ? "not present" : "already present", executeRequest.getJobId(), parameterExecutionId, executeRequest);
    try {
      if (lock.tryLock(1, TimeUnit.MINUTES)) { // Attempt to acquire the lock with a timeout
        return parameterExecutionHandler.executeParameterForError(executeRequest);
      } else {
        log.warn("[executeParameter] Could not acquire lock within the timeout  temp-parameterExecuteRequest: {}", executeRequest);
        throw new StreemException("Could not acquire lock within the timeout");
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ParameterExecutionException e) {
      throw new RuntimeException(e);
    } finally {
      lock.unlock();
    }
    // Dont return anything if the lock was not acquired
  }


}
