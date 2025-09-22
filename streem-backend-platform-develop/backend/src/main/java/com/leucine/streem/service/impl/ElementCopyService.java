package com.leucine.streem.service.impl;

import com.leucine.streem.constant.State;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.mapper.IParameterMapper;
import com.leucine.streem.dto.mapper.IStageMapper;
import com.leucine.streem.dto.mapper.ITaskMapper;
import com.leucine.streem.dto.response.Error;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.ExceptionType;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.model.helper.parameter.LeastCount;
import com.leucine.streem.model.helper.parameter.NumberParameter;
import com.leucine.streem.model.helper.parameter.ResourceParameter;
import com.leucine.streem.model.helper.search.Selector;
import com.leucine.streem.repository.*;
import com.leucine.streem.service.IElementCopyService;
import com.leucine.streem.util.*;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class ElementCopyService implements IElementCopyService {
  private final IParameterRepository parameterRepository;
  private final IUserRepository userRepository;
  private final ITaskRepository taskRepository;
  private final IStageRepository stageRepository;
  private final IStageMapper stageMapper;
  private final IParameterMapper parameterMapper;
  private final ITaskMapper taskMapper;
  private final IChecklistRepository checklistRepository;

  @Override
  public IChecklistElementDto copyChecklistElements(Long checklistId, CopyChecklistElementRequest copyChecklistRequest) throws ResourceNotFoundException, StreemException {
    log.info("[copyChecklistElements] Copying checklist element: {}, of type: {}, for checklist: {} ", copyChecklistRequest.getElementId(), copyChecklistRequest.getType(), checklistId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());
    validateIfChecklistCanBeModified(checklistId);

    switch (copyChecklistRequest.getType()) {
      case PARAMETER -> {
        return copyAndReturnParameter(copyChecklistRequest, principalUserEntity);
      }
      case TASK -> {
        return copyAndReturnTask(copyChecklistRequest, principalUserEntity);
      }
      case STAGE -> {
        return copyAndReturnStage(checklistId, copyChecklistRequest, principalUserEntity);
      }
      default ->
        throw new ResourceNotFoundException(copyChecklistRequest.getType().name(), ErrorCode.INCORRECT_ELEMENT_COPY_TYPE, ExceptionType.BAD_REQUEST);
    }
  }

  private void validateIfChecklistCanBeModified(Long checklistId) throws StreemException {
    Checklist checklist = checklistRepository.getReferenceById(checklistId);
    List<Error> errorList = new ArrayList<>();
    if (checklist.isArchived()) {
      ValidationUtils.addError(checklistId, errorList, ErrorCode.PROCESS_ALREADY_ARCHIVED);
    }
    if (!State.CHECKLIST_EDIT_STATES.contains(checklist.getState())) {
      ValidationUtils.addError(checklistId, errorList, ErrorCode.PROCESS_CANNOT_BE_MODFIFIED);
    }
    if (!Utility.isEmpty(errorList)) {
      ValidationUtils.invalidate(checklistId.toString(), errorList);
    }
  }

  private IChecklistElementDto copyAndReturnStage(Long checklistId, CopyChecklistElementRequest copyChecklistRequest, User principalUserEntity) {
    Stage stage = copyStage(copyChecklistRequest.getElementId(), principalUserEntity);
    Long newElementId = stage.getId();
    stageRepository.increaseOrderTreeByOneAfterStage(checklistId, stage.getOrderTree(), newElementId);
    return convertStageToDto(newElementId);
  }

  private TaskDto copyAndReturnTask(CopyChecklistElementRequest copyChecklistRequest, User principalUserEntity) {
    List<Task> taskList = copyTask(Set.of(copyChecklistRequest.getElementId()), principalUserEntity);
    Long newElementId = taskList.get(0).getId();
    Task task = taskList.get(0);
    taskRepository.increaseOrderTreeByOneAfterTask(task.getStage().getId(), task.getOrderTree(), newElementId);
    return taskMapper.toDto(taskRepository.getReferenceById(newElementId));
  }

  private ParameterDto copyAndReturnParameter(CopyChecklistElementRequest copyChecklistRequest, User principalUserEntity) {
    List<Parameter> parameterList = copyParameter(Set.of(copyChecklistRequest.getElementId()), new HashMap<>(), principalUserEntity);
    saveParameter(parameterList);
    Long newElementId = parameterList.get(0).getId();
    parameterRepository.increaseOrderTreeByOneAfterParameter(parameterList.get(0).getTask().getId(), parameterList.get(0).getOrderTree(), newElementId);
    return parameterMapper.toDto(parameterList.get(0));
  }

  private StageDto convertStageToDto(Long stageId) {
    Stage stage = stageRepository.getReferenceById(stageId);
    return stageMapper.toDto(stage);
  }

  private Stage copyStage(Long stageId, User principalUserEntity) {
    Stage referencedStage = stageRepository.getReferenceById(stageId);
    Stage stage = createStageCopy(principalUserEntity, referencedStage);
    Stage savedStage = stageRepository.save(stage);
    Set<Long> taskIds = taskRepository.getAllTaskIdsByStageId(referencedStage.getId());
    List<Task> copiedTaskList = copyTask(taskIds, principalUserEntity);
    copiedTaskList.forEach(task -> task.setStage(savedStage));
    saveTask(copiedTaskList);
    return savedStage;
  }

  private Stage createStageCopy(User principalUserEntity, Stage referencedStage) {
    Stage stage = new Stage();
    stage.setId(IdGenerator.getInstance().nextId());
    stage.setName(referencedStage.getName());
    stage.setOrderTree(referencedStage.getOrderTree() + 1);
    stage.setChecklist(referencedStage.getChecklist());
    stage.setArchived(false);
    stage.setCreatedAt(DateTimeUtils.now());
    stage.setCreatedBy(principalUserEntity);
    stage.setModifiedAt(DateTimeUtils.now());
    stage.setModifiedBy(principalUserEntity);
    return stage;
  }

  private List<Task> copyTask(Set<Long> taskIds, User principalUserEntity) {
    List<Task> referencedTaskList = taskRepository.findAllById(taskIds);
    final Map<Long, Task> oldTaskIdNewTaskMap = new HashMap<>();
    List<Task> taskList = referencedTaskList.stream()
      .filter(task -> !task.isArchived())
      .map(task -> {
        Task copiedTask = createTaskCopy(task, principalUserEntity);
        oldTaskIdNewTaskMap.put(task.getId(), copiedTask);
        return copiedTask;
      })
      .toList();

    List<Parameter> updatedParameterList = referencedTaskList.stream()
      .flatMap(task -> {
        Set<Long> parameterIds = parameterRepository.findParameterIdsByTaskId(task.getId());
        Map<Long, Long> oldAndNewParameterIdMap = new HashMap<>();
        return copyParameter(parameterIds, oldAndNewParameterIdMap, principalUserEntity).stream()
          .peek(parameter -> copyParameterRelativeInfo(task, parameter, oldTaskIdNewTaskMap, oldAndNewParameterIdMap));
      })
      .collect(Collectors.toList());


    List<Task> savedTasks = saveTask(taskList);
    saveParameter(updatedParameterList);
    return savedTasks;
  }

  @SneakyThrows
  private void copyParameterRelativeInfo(Task referencedTask, Parameter parameter, Map<Long, Task> oldTaskIdNewTaskMap, Map<Long, Long> oldAndNewParameterIdMap) {
    parameter.setTask(oldTaskIdNewTaskMap.get(referencedTask.getId()));

    Set<Long> oldParameterIds = new HashSet<>(oldAndNewParameterIdMap.keySet());

    if (parameter.getType() == Type.Parameter.NUMBER) {
      if (!Utility.isEmpty(parameter.getData())) {
        NumberParameter numberParameter = JsonUtils.readValue(parameter.getData().toString(), NumberParameter.class);
        if (!Utility.isEmpty(numberParameter.getLeastCount())) {
          LeastCount leastCount = JsonUtils.readValue(numberParameter.getLeastCount().toString(), LeastCount.class);
          long leastCountParameterId = getRelativeReferencedParameterId(oldParameterIds, leastCount.getReferencedParameterId(), oldAndNewParameterIdMap);
          leastCount.setReferencedParameterId(Long.toString(leastCountParameterId));
          numberParameter.setLeastCount(leastCount);
        }
        parameter.setData(JsonUtils.valueToNode(numberParameter));
      }
    }

    if (parameter.getType() == Type.Parameter.RESOURCE) {
      ResourceParameter resourceParameter = JsonUtils.readValue(JsonUtils.writeValueAsString(parameter.getData()), ResourceParameter.class);
      if (!Utility.isEmpty(resourceParameter.getPropertyFilters())) {
        resourceParameter.getPropertyFilters().getFields()
          .forEach(resourceParameterFilterField -> {
            if (resourceParameterFilterField.getSelector() == Selector.PARAMETER) {
              long referencedParameterId = getRelativeReferencedParameterId(oldParameterIds, resourceParameterFilterField.getReferencedParameterId(), oldAndNewParameterIdMap);
              resourceParameterFilterField.setReferencedParameterId(Long.toString(referencedParameterId));
            }
          });
      }
      parameter.setData(JsonUtils.valueToNode(resourceParameter));
    }

    if (!Utility.isEmpty(parameter.getAutoInitialize())) {
      AutoInitializeDto autoInitializeDto = JsonUtils.readValue(parameter.getAutoInitialize().toString(), AutoInitializeDto.class);
      autoInitializeDto.setParameterId(String.valueOf(getRelativeReferencedParameterId(oldParameterIds, autoInitializeDto.getParameterId(), oldAndNewParameterIdMap)));
    }

    if (!Utility.isEmpty(parameter.getValidations())) {
      ParameterValidationDto parameterValidationDto = JsonUtils.readValue(parameter.getValidations().toString(), ParameterValidationDto.class);
      parameterValidationDto.getCriteriaValidations()
        .forEach(criteriaValidationDto -> {
          if (criteriaValidationDto.getCriteriaType() == Type.SelectorType.PARAMETER) {
            long valueParameterId = getRelativeReferencedParameterId(oldParameterIds, criteriaValidationDto.getValueParameterId(), oldAndNewParameterIdMap);
            criteriaValidationDto.setValueParameterId(Long.toString(valueParameterId));

            if (!Utility.isEmpty(criteriaValidationDto.getLowerValueParameterId())) {
              long lowerValueParameterId = getRelativeReferencedParameterId(oldParameterIds, criteriaValidationDto.getLowerValueParameterId(), oldAndNewParameterIdMap);
              criteriaValidationDto.setLowerValueParameterId(Long.toString(lowerValueParameterId));
            }

            if (!Utility.isEmpty(criteriaValidationDto.getUpperValueParameterId())) {
              long upperValueParameterId = getRelativeReferencedParameterId(oldParameterIds, criteriaValidationDto.getUpperValueParameterId(), oldAndNewParameterIdMap);
              criteriaValidationDto.setUpperValueParameterId(Long.toString(upperValueParameterId));
            }
          }

        });
      parameterValidationDto.getResourceParameterValidations()
        .forEach(resourceParameterPropertyValidationDto -> {
          if (!Utility.isEmpty(resourceParameterPropertyValidationDto.getParameterId())) {
            long parameterId = getRelativeReferencedParameterId(oldParameterIds, resourceParameterPropertyValidationDto.getParameterId(), oldAndNewParameterIdMap);
            resourceParameterPropertyValidationDto.setParameterId(Long.toString(parameterId));
          }
        });


      parameter.setValidations(JsonUtils.valueToNode(parameterValidationDto));
    }
  }

  private Task createTaskCopy(Task referencedTask, User principalUserEntity) {
    Task task = new Task();
    task.setId(IdGenerator.getInstance().nextId());
    task.setName(referencedTask.getName());
    task.setOrderTree(referencedTask.getOrderTree() + 1);
    task.setArchived(false);
    task.setHasStop(false);
    task.setSoloTask(false);
    task.setStage(referencedTask.getStage());
    task.setCreatedAt(DateTimeUtils.now());
    task.setCreatedBy(principalUserEntity);
    task.setModifiedAt(DateTimeUtils.now());
    task.setModifiedBy(principalUserEntity);
    return task;
  }

  private List<Parameter> copyParameter(Set<Long> parameterIds, Map<Long, Long> oldParameterIdNewParameterIdMap, User principalUserEntity) {
    return parameterRepository.findAllByIdInAndArchived(parameterIds, false).stream()
      .filter(parameter -> !parameter.isArchived())
      .map(referencedParameter -> createParameterCopy(principalUserEntity, referencedParameter, oldParameterIdNewParameterIdMap))
      .toList();
  }

  private Parameter createParameterCopy(User principalUserEntity, Parameter referencedParameter, Map<Long, Long> oldParameterIdNewParameterIdMap) {
    Parameter parameter = new Parameter();
    parameter.setId(IdGenerator.getInstance().nextId());
    parameter.setType(referencedParameter.getType());
    parameter.setVerificationType(referencedParameter.getVerificationType());
    parameter.setLabel(referencedParameter.getLabel());
    parameter.setDescription(referencedParameter.getDescription());
    parameter.setOrderTree(referencedParameter.getOrderTree() + 1);
    parameter.setMandatory(referencedParameter.isMandatory());
    parameter.setArchived(false);
    parameter.setData(referencedParameter.getData());
    parameter.setTask(referencedParameter.getTask());
    parameter.setValidations(referencedParameter.getValidations());
    parameter.setChecklistId(referencedParameter.getChecklistId());
    parameter.setAutoInitialized(referencedParameter.isAutoInitialized());
    parameter.setAutoInitialize(referencedParameter.getAutoInitialize());
    parameter.setRules(null);
    parameter.setTargetEntityType(referencedParameter.getTargetEntityType());
    parameter.setHidden(referencedParameter.isHidden());
    parameter.setMetadata(referencedParameter.getMetadata());
    parameter.setCreatedAt(DateTimeUtils.now());
    parameter.setModifiedAt(DateTimeUtils.now());
    parameter.setCreatedBy(principalUserEntity);
    parameter.setModifiedBy(principalUserEntity);
    oldParameterIdNewParameterIdMap.put(referencedParameter.getId(), parameter.getId());
    return parameter;
  }

  private List<Task> saveTask(List<Task> taskList) {
    return taskRepository.saveAll(taskList);
  }

  private void saveParameter(List<Parameter> parameterList) {
    parameterRepository.saveAll(parameterList);
  }

  private static Long getRelativeReferencedParameterId(Set<Long> oldParameterIds, String resourceParameterFilterField, Map<Long, Long> oldAndNewParameterIdMap) {
    return oldParameterIds.contains(Long.parseLong(resourceParameterFilterField)) ? oldAndNewParameterIdMap.get(Long.parseLong(resourceParameterFilterField)) : Long.parseLong(resourceParameterFilterField);
  }
}
