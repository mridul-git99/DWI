package com.leucine.streem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.collections.EntityObject;
import com.leucine.streem.collections.partial.PartialEntityObject;
import com.leucine.streem.constant.State;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.mapper.IUserMapper;
import com.leucine.streem.dto.projection.IdView;
import com.leucine.streem.dto.request.ParameterExecuteRequest;
import com.leucine.streem.dto.response.Error;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.ParameterExecutionException;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.Parameter;
import com.leucine.streem.model.ParameterValue;
import com.leucine.streem.model.ParameterValueBase;
import com.leucine.streem.model.User;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.model.helper.parameter.ResourceParameter;
import com.leucine.streem.repository.*;
import com.leucine.streem.service.IEntityObjectService;
import com.leucine.streem.service.IParameterAutoInitializeService;
import com.leucine.streem.service.IParameterExecutionHandler;
import com.leucine.streem.service.IParameterExecutionService;
import com.leucine.streem.util.JsonUtils;
import com.leucine.streem.util.Utility;
import com.leucine.streem.util.ValidationUtils;
import com.leucine.streem.util.graph.ParameterNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
@Slf4j
public class ParameterExecutionHandler implements IParameterExecutionHandler {
  private final IParameterExecutionService parameterExecutionService;
  private final IParameterAutoInitializeService parameterAutoInitializeService;
  private final IAutoInitializedParameterRepository autoInitializedParameterRepository;
  private final IParameterValueRepository parameterValueRepository;
  private final IParameterRepository parameterRepository;
  private final IUserRepository userRepository;
  private final IUserMapper userMapper;

  //TODO: remove entity manager.
  // This is heavy.
  // To fix it In parameter execution service just update the object and return it.
  // After that bulk update the object in the database.
  private final EntityManager em;
  private final IEntityObjectService entityObjectService;
  private final RulesExecutionService rulesExecutionService;

  @Override
  @Transactional(rollbackFor = Exception.class, noRollbackFor = ParameterExecutionException.class)
  public ParameterDto executeParameter(Long jobId, Long parameterExecutionId, ParameterExecuteRequest parameterExecuteRequest, Type.JobLogTriggerType jobLogTriggerType, boolean ignoreRootExecution, boolean isCreateJobRequest, boolean isScheduled) throws StreemException, IOException, ResourceNotFoundException {
    log.info("[executeParameter] jobId: {}, parameterExecutionId: {}", jobId, parameterExecutionId);
    PrincipalUser principalUser;
    if (isScheduled) {
      User user = userRepository.findById(User.SYSTEM_USER_ID).get();
      principalUser = userMapper.toPrincipalUser(user);
    } else {
      principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
    ParameterDto referencedParameterDto = new ParameterDto();
    Queue<Long> parameterIdsToBeExecutedOrAutoInitialised = new LinkedList<>();
    parameterIdsToBeExecutedOrAutoInitialised.add(parameterExecuteRequest.getParameter().getId());

    Set<Long> executedParameterIds = new HashSet<>();
    List<Error> parameterExecutionSoftErrors = new ArrayList<>();

    if (ignoreRootExecution) {
      ParameterValue parameterValue = parameterValueRepository.getReferenceById(parameterExecutionId);
      if (parameterValue.getState() == State.ParameterExecution.EXECUTED) {
        Parameter parameter = parameterRepository.getReferenceById(parameterExecuteRequest.getParameter().getId());
        rulesExecutionService.updateRules(jobId, parameter, parameterValue);
      }
      em.flush();
      em.clear();
    }

    while (!parameterIdsToBeExecutedOrAutoInitialised.isEmpty()) {
      Long currentParameterId = parameterIdsToBeExecutedOrAutoInitialised.poll();
      executedParameterIds.add(currentParameterId);

      Set<Long> showParameterExecutionIds = new HashSet<>();

      ParameterNode referencedParameterNode = buildTree(currentParameterId, jobId, executedParameterIds);

      referencedParameterDto = findAndAutoInitializeParameters(referencedParameterNode, jobId, showParameterExecutionIds, parameterExecuteRequest, referencedParameterDto, principalUser, jobLogTriggerType, ignoreRootExecution, isCreateJobRequest, parameterExecutionSoftErrors, isScheduled);


      Set<Long> eligibleParameterIds = parameterValueRepository.findParametersEligibleForAutoInitialization(jobId, showParameterExecutionIds, executedParameterIds);

      parameterIdsToBeExecutedOrAutoInitialised.addAll(eligibleParameterIds);
      if (!Utility.isEmpty(parameterExecutionSoftErrors) && Utility.isEmpty(referencedParameterDto)) {
        ParameterDetailsDto currentParameterDetailsDto = parameterExecutionSoftErrors.stream()
          .flatMap(error -> Stream.of((ParameterDetailsDto) (error.getErrorInfo())))
          .filter(parameterDetailsDto -> parameterDetailsDto.getParameterId().equals(currentParameterId.toString()))
          .findFirst()
          .orElse(null);

        if (!Utility.isEmpty(currentParameterDetailsDto)) {
          referencedParameterDto = currentParameterDetailsDto.getCurrentParameterDto();
          parameterExecutionSoftErrors.forEach(error -> {
            ParameterDetailsDto parameterDetailsDto = (ParameterDetailsDto) error.getErrorInfo();
            parameterDetailsDto.setCurrentParameterDto(null);
          });
          referencedParameterDto.setSoftErrors(parameterExecutionSoftErrors);
        }
      }
    }

    return referencedParameterDto;

  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public List<ParameterDto> executeParameters(List<BulkParameterExecuteRequest> bulkParameterExecuteRequests) throws StreemException, IOException, ResourceNotFoundException {
    List<ParameterDto> parameterDtos = new ArrayList<>();
    for (BulkParameterExecuteRequest bulkParameterExecuteRequest : bulkParameterExecuteRequests) {

      ParameterDto parameterDto = executeParameter(bulkParameterExecuteRequest.getParameterExecuteRequest().getJobId(), bulkParameterExecuteRequest.getParameterExecuteRequest().getParameter().getId(), bulkParameterExecuteRequest.getParameterExecuteRequest(), Type.JobLogTriggerType.PROCESS_PARAMETER_VALUE, false, false, false);
      parameterDtos.add(parameterDto);
    }
    return parameterDtos;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TempParameterDto executeParameterForError(ParameterExecuteRequest parameterExecuteRequest) throws IOException, StreemException, ResourceNotFoundException, ParameterExecutionException {
    log.info("[executeParameterForError] parameterExecuteRequest: {}", parameterExecuteRequest);
    boolean isParameterUsedInAutoInitialisation = parameterRepository.isParameterUsedInAutoInitialization(parameterExecuteRequest.getParameter().getId());
    if (isParameterUsedInAutoInitialisation) {
      ValidationUtils.invalidate(parameterExecuteRequest.getParameter().getId(), ErrorCode.PARAMETER_USED_IN_AUTO_INITIALIZE);
    }
    return parameterExecutionService.executeParameterForError(parameterExecuteRequest);
  }

  // We do not form the tree for hidden parameters
  // and it's children because if the parent is hidden, their child won't be initialized at all.
  private ParameterNode buildTree(Long referencedParameterId, Long jobId, Set<Long> executedParameterIds) {
    Map<Long, ParameterNode> nodeMap = new HashMap<>();
    Set<Long> visited = new HashSet<>();
    Queue<Long> queue = new LinkedList<>();
    queue.offer(referencedParameterId);
    visited.add(referencedParameterId);

    while (!queue.isEmpty()) {
      Long currentId = queue.poll();
      List<Long> autoInitializedParameters = autoInitializedParameterRepository.findAllEligibleParameterIdsToAutoInitializeByReferencedParameterId(currentId, executedParameterIds, jobId);

      mapParameterToNode(currentId, nodeMap);

      for (Long autoInitializedParameterId : autoInitializedParameters) {
        mapParameterToNode(autoInitializedParameterId, nodeMap);
        nodeMap.get(currentId).getNeighbors().add(nodeMap.get(autoInitializedParameterId));
        if (visited.add(autoInitializedParameterId)) {
          queue.offer(autoInitializedParameterId);
        }
      }

    }
    return nodeMap.get(referencedParameterId);
  }

  private void mapParameterToNode(Long currentId, Map<Long, ParameterNode> nodeMap) {
    log.info("[mapParameterToNode] currentId: {}", currentId);
    nodeMap.putIfAbsent(currentId, new ParameterNode(
      currentId
    ));
  }

  private ParameterDto findAndAutoInitializeParameters(ParameterNode referencedParameterNode, Long jobId, Set<Long> showParameterIds, ParameterExecuteRequest parameterExecuteRequest, ParameterDto referencedParameterDto, PrincipalUser principalUser, Type.JobLogTriggerType jobLogTriggerType, boolean ignoreRootExecution, boolean isCreateJobRequest, List<Error> parameterExecutionSoftErrors, boolean isScheduled) throws IOException, ResourceNotFoundException, StreemException {
    log.info("[findAndAutoInitializeParameters] referencedParameterNode: {}, jobId: {}", referencedParameterNode, jobId);
    Long rootParameterId = parameterExecuteRequest.getParameter().getId();
    if (!Utility.isEmpty(referencedParameterNode)) {
      Queue<ParameterNode> parameterNodeQueue = new LinkedList<>();
      parameterNodeQueue.offer(referencedParameterNode);

      // This is set because if a parameter doesn't have any parent node then it's referencedId is itself
      if (Utility.isEmpty(parameterExecuteRequest.getReferencedParameterId())) {
        parameterExecuteRequest.setReferencedParameterId(referencedParameterNode.getId());
      }

      // List of long because a calculation parameter can be auto initialized by 2 or more calculation parameters forming a cyclic structure.
      Map<Long, Set<Long>> autoInitializeParameterAndReferencedParameterMap = new HashMap<>();

      while (!parameterNodeQueue.isEmpty()) {
        ParameterNode currentParameterNode = parameterNodeQueue.poll();
        if (!(currentParameterNode.getId().equals(rootParameterId) && ignoreRootExecution)) {
          // During starting of a task we also send the referenced parameter of a parameter in parameter execute request.
          // So we need to check if the current parameter is the referenced parameter or not.

          Set<Long> referencedParameterIds;

          if (!Utility.isEmpty(autoInitializeParameterAndReferencedParameterMap.get(currentParameterNode.getId()))) {
            referencedParameterIds = autoInitializeParameterAndReferencedParameterMap.get(currentParameterNode.getId());
          } else {
            // This is the case where due to show rules we don't have autoInitializeParameterAndReferencedParameterMap for the currentParameterNode.
            // If we get referenced parameter ids then it is an auto initialized parameter which will be calculated by the referenced parameter.
            referencedParameterIds = autoInitializedParameterRepository.getReferencedParameterIdByAutoInitializedParameterId(currentParameterNode.getId()).stream()
              .map(IdView::getId)
              .collect(Collectors.toSet());

            // If we don't have referenced parameter ids then it is a non auto-initialize parameter Hence it's referenced parameter id is itself.
            if (Utility.isEmpty(referencedParameterIds)) {
              referencedParameterIds = Set.of(parameterExecuteRequest.getReferencedParameterId());
            }
          }

          for (Long referencedParameter : referencedParameterIds) {
            log.info("[findAndAutoInitializeParameters] currentParameterNode: {}, referencedParameter: {}", currentParameterNode, referencedParameter);
            ParameterExecuteRequest autoInitializeExecuteRequest = parameterAutoInitializeService.getParameterExecuteRequestForParameterToAutoInitialize(jobId, currentParameterNode.getId(), false, referencedParameter);
            ParameterExecuteRequest executeRequest = Utility.isEmpty(autoInitializeExecuteRequest) ? parameterExecuteRequest : autoInitializeExecuteRequest;

            boolean isAutoInitialized = !Utility.isEmpty(autoInitializeExecuteRequest);

            boolean isResourceParameter = parameterRepository.existsByIdAndType(executeRequest.getParameter().getId(), Type.Parameter.RESOURCE);
            if (isResourceParameter) {
              ResourceParameter resourceParameter = JsonUtils.readValue(executeRequest.getParameter().getData().toString(), ResourceParameter.class);
              List<ResourceParameterChoiceDto> parameterChoices = JsonUtils.jsonToCollectionType(resourceParameter.getChoices(), List.class, ResourceParameterChoiceDto.class);
              List<ResourceParameterChoiceDto> parameterChoicesWIthUpdatedName = new ArrayList<>();
              if (!Utility.isEmpty(resourceParameter.getCollection())) {
                for (ResourceParameterChoiceDto choiceDto : parameterChoices) {
                  EntityObject updatedEntityObject = entityObjectService.findById(resourceParameter.getCollection(), choiceDto.getObjectId());
                  choiceDto.setObjectExternalId(updatedEntityObject.getExternalId());
                  choiceDto.setObjectDisplayName(updatedEntityObject.getDisplayName());
                  parameterChoicesWIthUpdatedName.add(choiceDto);
                }
                resourceParameter.setChoices(parameterChoicesWIthUpdatedName);
                JsonNode resourceParameterNode = JsonUtils.valueToNode(resourceParameter);
                executeRequest.getParameter().setData(resourceParameterNode);
              }
            }

            ParameterDto currentParameterDto = null;
            List<Error> parameterExecutionHardErrors = new ArrayList<>();


            try {
              currentParameterDto = parameterExecutionService.executeParameter(jobId, executeRequest, isAutoInitialized, jobLogTriggerType, principalUser, isCreateJobRequest, isScheduled);
            } catch (ParameterExecutionException e) {
              for (Error error : e.getErrorList()) {
                ParameterDetailsDto parameterDetailsDto = (ParameterDetailsDto) error.getErrorInfo();
                if (parameterDetailsDto.getExceptionApprovalType() == Type.ParameterExceptionApprovalType.DEFAULT_FLOW) {
                  parameterExecutionHardErrors.add(error);
                } else {
                  parameterExecutionSoftErrors.add(error);
                }
              }

              if (!Utility.isEmpty(parameterExecutionHardErrors)) {
                throw new StreemException(parameterExecutionHardErrors);
              }

            }
            em.flush();
            em.clear();
            if (!Utility.isEmpty(currentParameterDto) && !Utility.isEmpty(currentParameterDto.getShow())) {
              showParameterIds.addAll(currentParameterDto.getShow().stream().map(Long::parseLong).collect(Collectors.toSet()));
            }

            if (Objects.equals(parameterExecuteRequest.getParameter().getId(), currentParameterNode.getId())) {
              referencedParameterDto = currentParameterDto;
            }
          }
        }


        // Order of execution of parameters should be based on taskOrderTree and parameter orderTree. If it's a CJF parameter then taskOrderTree will be null in this case it should be executed according to parameter orderTree.
        for (ParameterNode neighbor : currentParameterNode.getNeighbors()) {
          parameterNodeQueue.offer(neighbor);
          autoInitializeParameterAndReferencedParameterMap.computeIfAbsent(neighbor.getId(), k -> new HashSet<>()).add(currentParameterNode.getId());

        }

      }
    }
    return referencedParameterDto;
  }

  @Override
  public Page<PartialEntityObject> getAllFilteredEntityObjects(Long parameterExecutionId, String query, String shortCode, Pageable pageable) throws IOException, ResourceNotFoundException {
    return parameterExecutionService.getAllFilteredEntityObjects(parameterExecutionId, query, shortCode, pageable, false);
  }
}
