package com.leucine.streem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.leucine.streem.config.MediaConfig;
import com.leucine.streem.constant.CollectionMisc;
import com.leucine.streem.constant.EffectType;
import com.leucine.streem.constant.Effect;
import com.leucine.streem.constant.State;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.mapper.*;
import com.leucine.streem.dto.mapper.importmapper.IImportChecklistMapper;
import com.leucine.streem.dto.request.*;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.dto.response.MediaUploadResponse;
import com.leucine.streem.exception.ExceptionType;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.migration.AddStopDependency;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.model.helper.parameter.MaterialParameter;
import com.leucine.streem.repository.*;
import com.leucine.streem.repository.impl.ParameterRepositoryImpl;
import com.leucine.streem.service.*;
import com.leucine.streem.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.leucine.streem.util.JsonUtils.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class ImportExportChecklistService implements IimportExportChecklistService {
  private final IChecklistRepository checklistRepository;
  private final IStageRepository stageRepository;
  private final IChecklistAuditService checklistAuditService;
  private final ITaskRepository taskRepository;
  private final IChecklistMapper checklistMapper;
  private final IFacilityRepository facilityRepository;
  private final IOrganisationRepository organisationRepository;
  private final IFacilityUseCaseMappingRepository facilityUseCaseMappingRepository;
  private final IUserRepository userRepository;
  private final IParameterService parameterService;
  private final ITaskService taskService;
  private final IMediaService mediaService;
  private final IImportChecklistMapper iImportChecklistMapper;
  private final ObjectMapper objectMapper;
  private final ICodeService codeService;
  private final IChecklistService checklistService;
  private final IParameterRepository parameterRepository;
  private final IParameterMapper parameterMapper;
  private final IVersionService versionService;
  private final ITaskMediaMappingRepository taskMediaMappingRepository;
  private final ITaskMediaMapper taskMediaMapper;
  private final ITaskDependencyService taskDependencyService;
  private final AddStopDependency addStopDependency;
  private final ParameterRepositoryImpl parameterRepositoryImpl;
  private final IActionRepository actionRepository;
  private final IMediaRepository mediaRepository;
  private final IActionMapper actionMapper;
  private final IEffectMapper effectMapper;
  private final IEffectRepository effectRepository;
  private final ActionService actionService;
  @Autowired
  private MediaConfig mediaConfig;
  @Autowired
  private EffectService effectService;

  public List<ImportChecklistRequest> exportChecklists(List<Long> checklistIds) {
    log.info("[exportChecklists] Request to export checklists checklistIds: {}", checklistIds);
    List<ChecklistDto> checklists = new ArrayList<>();
    if (Utility.isEmpty(checklistIds)) {
      throw new IllegalArgumentException("checklistIds cannot be null or empty");
    }
    List<Checklist> optionalChecklists = checklistRepository.findAllById(checklistIds);
    for (Checklist checklist : optionalChecklists) {
      if (!Utility.isEmpty(checklist) && checklist.getState().equals(State.Checklist.PUBLISHED)) {
        ChecklistDto checklistDto = checklistMapper.toDto(checklist);
        var parameters = parameterRepository.getParametersByChecklistIdAndTargetEntityType(checklist.getId(), Type.ParameterTargetEntityType.PROCESS);
        var parameterDtos = parameterMapper.toDto(parameters);
        checklistDto.setParameters(parameterDtos);
        var actions = actionRepository.findByChecklistIdAndArchived(checklist.getId() ,false);
        List<Long> actionIds = actions.stream().map(Action::getId).toList();
        List<Effect> effects = effectRepository.findByActionIdIn(actionIds);
        Map<Long, List<Effect>> effectsByAction = effects.stream().collect(Collectors.groupingBy(e -> e.getAction().getId()));
        List<ActionDto> actionDtos = actionMapper.toExport(actions);
        for (ActionDto actionDto : actionDtos) {
          long actionId = Long.parseLong(actionDto.getId());
          List<EffectDto> EffectDtos = effectMapper.toExport(effectsByAction.getOrDefault(actionId, Collections.emptyList()));
          // Ensure each EffectDto's query is wrapped under "root"
          for (EffectDto effectDto : EffectDtos) {
            effectDto.setQuery(wrapQueryWithRoot(effectDto.getQuery()));
          }
          actionDto.setEffects(EffectDtos);
        }
        checklistDto.setActions(actionDtos);
        checklists.add(checklistDto);
      }
    }

    return iImportChecklistMapper.toDto(checklists);
  }

  public List<MultipartFile> getAllMediaMultiPart(List<Long> checklistIds) throws IOException {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    List<Parameter> parameters = parameterRepository.findAllByTypeAndChecklistIdInAndArchived(Type.Parameter.MATERIAL, checklistIds, false);
    String accessToken = principalUser.getToken();
    List<MultipartFile> multipartFiles = new ArrayList<>();
    for (Parameter parameter : parameters) {
      List<MaterialParameter> materialParameters = JsonUtils.readValue(parameter.getData().toString(), new TypeReference<>() {
      });

      for (MaterialParameter materialParameter : materialParameters) {
        multipartFiles.add(downloadFileAsMultipartFile(materialParameter.getLink(), materialParameter.getFilename(), accessToken));
      }
    }
    List<TaskMediaMapping> taskMediaMappingList = taskMediaMappingRepository.findAllByChecklistIdsIn(checklistIds);
    for (TaskMediaMapping taskMediaMapping : taskMediaMappingList) {
      MediaDto mediaDto = taskMediaMapper.toDto(taskMediaMapping);
      multipartFiles.add(downloadFileAsMultipartFile(mediaDto.getLink(), mediaDto.getFilename(), accessToken));
    }

    return multipartFiles;
  }

  @Override
  public void populateMissingMediasDetails(List<Long> ids) throws JsonProcessingException, ResourceNotFoundException {
    List<Parameter> parameters = parameterRepository.findAllByTypeAndChecklistIdInAndArchived(Type.Parameter.MATERIAL, ids, false);
    if(parameters.isEmpty()){
      return;
    }
    for (Parameter parameter : parameters) {
      List<MaterialParameter> materialParameters = JsonUtils.readValue(parameter.getData().toString(), new TypeReference<>() {});
      boolean updated = false;

      for (MaterialParameter materialParameter : materialParameters) {
        if (materialParameter.getLink() == null || materialParameter.getFilename() == null) {
          var media = mediaRepository.findById(Long.valueOf(materialParameter.getMediaId()))
            .orElseThrow(() -> new ResourceNotFoundException(materialParameter.getMediaId(), ErrorCode.MEDIA_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

          String fileName = media.getFilename();
          String link = mediaConfig.getCdn() + java.io.File.separator + media.getRelativePath() + java.io.File.separator + media.getFilename();
          materialParameter.setName(media.getName());
          materialParameter.setLink(link);
          materialParameter.setType(media.getType());
          materialParameter.setFilename(media.getFilename());
          materialParameter.setOriginalFilename(media.getOriginalFilename());
          materialParameter.setDescription(media.getDescription());
          updated = true;

        }
      }

      if (updated) {
        parameter.setData(JsonUtils.valueToNode(materialParameters));
        parameterRepository.save(parameter);
      }
    }
  }

  private MultipartFile downloadFileAsMultipartFile(String link, String fileName, String accessToken) throws IOException {
    URL url = new URL(link);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

    connection.setRequestMethod("GET");
    connection.setRequestProperty("Authorization", "Bearer " + accessToken);

    int responseCode = connection.getResponseCode();
    if (responseCode != HttpURLConnection.HTTP_OK) {
      throw new IOException("Failed to download file: HTTP error code " + responseCode);
    }

    // Directly read the input stream and create MultipartFile
    return new MockMultipartFile(
      fileName,
      "", // you might want to set filename or content type
      "text/plain",
      connection.getInputStream()
    );


  }

  @Transactional(rollbackFor = Exception.class)
  public BasicDto importChecklists(Long useCaseId, MultipartFile file) throws StreemException, ResourceNotFoundException, IOException {
    log.info("[importChecklists] Request to import checklists fileName: {}", file.getName());
    ByteArrayOutputStream processOutputStream = new ByteArrayOutputStream();
    Map<String, ByteArrayOutputStream> fileNameAndOutputStreamMap = new HashMap<>();

    try (InputStream initialStream = file.getInputStream();
         ZipInputStream zipStream = new ZipInputStream(initialStream)) {
      ZipEntry entry;
      while ((entry = zipStream.getNextEntry()) != null) {
        if (entry.getName().contains(".json")) {
          processOutputStream = new ByteArrayOutputStream();
          byte[] buffer = new byte[1024];
          int len;
          while ((len = zipStream.read(buffer)) > 0) {
            processOutputStream.write(buffer, 0, len);
          }
          processOutputStream.toByteArray();

        } else {
          ByteArrayOutputStream mediaOutputStream = new ByteArrayOutputStream();
          byte[] buffer = new byte[1024];
          int len;
          while ((len = zipStream.read(buffer)) > 0) {
            mediaOutputStream.write(buffer, 0, len);
          }
          mediaOutputStream.toByteArray();
          fileNameAndOutputStreamMap.put(entry.getName(), mediaOutputStream);
        }
      }
    } catch (IOException ignored) {

    }
    List<ImportChecklistRequest> importChecklistRequests = JsonUtils.readValue(processOutputStream.toString(StandardCharsets.UTF_8), new TypeReference<>() {
    });

    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());
    Checklist checklist = null;
    for (ImportChecklistRequest importCheckListRequest : importChecklistRequests) {
      log.info("importChecklists: {}", importCheckListRequest);
      Optional<Checklist> optionalChecklist = checklistRepository.findById(Long.valueOf(importCheckListRequest.getId()));
      log.info("optional checklist: {}", optionalChecklist);
      if (optionalChecklist.isPresent()) {
        importCheckListRequest = updateRequestWithNewIds(importCheckListRequest);
      }
      // TODO: we need to check from jaas why currentFacility id is null for global portal, for now fixing this issue by hard coding global portal id as -1"
      Long currentFacilityId;
      if (Utility.isEmpty(principalUser.getCurrentFacilityId())) {
        currentFacilityId = -1L;
      } else {
        currentFacilityId = principalUser.getCurrentFacilityId();
      }

      Facility facility = facilityRepository.getReferenceById(currentFacilityId);
      Organisation organisation = organisationRepository.getReferenceById(principalUser.getOrganisationId());
      FacilityUseCaseMapping facilityUseCaseMapping = facilityUseCaseMappingRepository.findByFacilityIdAndUseCaseId(facility.getId(), useCaseId);
      UseCase useCase = !Utility.isEmpty(facilityUseCaseMapping) ? facilityUseCaseMapping.getUseCase() : null;
      if (Utility.isEmpty(useCase)) {
        ValidationUtils.invalidate(useCaseId, ErrorCode.USE_CASE_NOT_FOUND);
      }

      checklist = createChecklist(principalUser, principalUserEntity, importCheckListRequest, facility, organisation, useCase, currentFacilityId);
      checklistRepository.flush();  // Force immediate save and flush
      final Long checklistid = checklist.getId();

      List<Stage> stages = createStages(principalUserEntity, importCheckListRequest.getStageRequests(), checklist);
      stageRepository.flush();
      createTasks(principalUserEntity, importCheckListRequest.getStageRequests(), stages);
      taskRepository.flush();
      List<Parameter> parameters = new ArrayList<>();
      if (!Utility.isEmpty(importCheckListRequest.getParameterRequests())) {
        for (ImportParameterRequest parameterCreateRequest : importCheckListRequest.getParameterRequests()) {
          if(parameterCreateRequest.getValidations().isEmpty()){
            parameterCreateRequest.setValidations(JsonUtils.valueToNode(new ArrayList<>()));
          }
          if (parameterCreateRequest.getType() == Type.Parameter.NUMBER) {
            handleNumberParameterValidations(parameterCreateRequest);
          }
          if (parameterCreateRequest.getType() == Type.Parameter.RESOURCE  ||parameterCreateRequest.getType() == Type.Parameter.MULTI_RESOURCE ) {
            handleResourceParameterValidations(parameterCreateRequest);
          }
          if (parameterCreateRequest.getType() == Type.Parameter.DATE || parameterCreateRequest.getType() == Type.Parameter.DATE_TIME) {
            handleDateAndDateTimeParameterValidations(parameterCreateRequest);
          }
          if (Utility.isEmpty(parameterCreateRequest.getExceptionApprovalType())) {
            parameterCreateRequest.setExceptionApprovalType(Type.ParameterExceptionApprovalType.NONE);
          }
//          ParameterDto parameterDto = parameterService.createParameter(Long.valueOf(importCheckListRequest.getId()), parameterCreateRequest);
          Parameter parameter = parameterService.prepareParameter(checklist, parameterCreateRequest);
          parameters.add(parameter);
        }
        MapJobParameterRequest mapJobParameterRequest = new MapJobParameterRequest();
        Map<Long, Integer> mappedParameters = new HashMap<>();
        for (Parameter p : parameters) {
          mappedParameters.put(p.getId(), p.getOrderTree());
        }
        mapJobParameterRequest.setMappedParameters(mappedParameters);
        parameterRepositoryImpl.bulkInsertIntoParameters(parameters, principalUserEntity);
        checklistService.configureProcessParameters(checklistid, mapJobParameterRequest);
      }

      List<Parameter> allParameters = new ArrayList<>();
      for (ImportStageRequest importStageRequest : importCheckListRequest.getStageRequests()) {
        for (ImportTaskRequest taskRequest : importStageRequest.getTaskRequests()) {
          List<ImportParameterRequest> taskParamRequests = taskRequest.getParameterRequests();
          Task task = taskRepository.findById(Long.valueOf(taskRequest.getId())).get();
          for (ImportParameterRequest parameterCreateRequest : taskParamRequests) {
            if(parameterCreateRequest.getValidations().isEmpty()){
              parameterCreateRequest.setValidations(JsonUtils.valueToNode(new ArrayList<>()));
            }
            if (parameterCreateRequest.getType().equals(Type.Parameter.MATERIAL)) {
              handleParameterMedias(parameterCreateRequest, fileNameAndOutputStreamMap);
            }
            if (parameterCreateRequest.getType() == Type.Parameter.CALCULATION || parameterCreateRequest.isAutoInitialized()) {
              if (parameterCreateRequest.getVerificationType() != Type.VerificationType.NONE) {
                parameterCreateRequest.setVerificationType(Type.VerificationType.NONE);
              }
            }
            if (parameterCreateRequest.getType() == Type.Parameter.NUMBER) {
              handleNumberParameterValidations(parameterCreateRequest);
            }
            if (parameterCreateRequest.getType() == Type.Parameter.RESOURCE  ||parameterCreateRequest.getType() == Type.Parameter.MULTI_RESOURCE ) {
              handleResourceParameterValidations(parameterCreateRequest);
            }
            if (parameterCreateRequest.getType() == Type.Parameter.DATE || parameterCreateRequest.getType() == Type.Parameter.DATE_TIME) {
              handleDateAndDateTimeParameterValidations(parameterCreateRequest);
            }
            if (Utility.isEmpty(parameterCreateRequest.getExceptionApprovalType())) {
              parameterCreateRequest.setExceptionApprovalType(Type.ParameterExceptionApprovalType.NONE);
            }

            Parameter taskLevelParams = parameterService.prepareTaskParameter(checklist, task , parameterCreateRequest);
            allParameters.add(taskLevelParams);
          }
          for (ImportMediaRequest mediaRequest : taskRequest.getMediaRequests()) {
            ByteArrayOutputStream currentMediaOutputStream = fileNameAndOutputStreamMap.get(mediaRequest.getFileName());
            if (Utility.isEmpty(currentMediaOutputStream)) {
              ValidationUtils.invalidate(mediaRequest.getFileName(), ErrorCode.MEDIA_NOT_FOUND);
            }
            MultipartFile multipartFile = new CustomMultipartFile(new ByteArrayInputStream(currentMediaOutputStream.toByteArray()), mediaRequest.getFileName());
            MediaUploadResponse mediaUploadResponse = mediaService.save(mediaRequest, multipartFile);
            mediaRequest.setLink(mediaUploadResponse.getLink());
            mediaRequest.setMediaId(Long.valueOf(mediaUploadResponse.getMediaId()));
            taskService.addMedia(Long.valueOf(taskRequest.getId()), mediaRequest);
          }
//          taskService.addAutomations(Long.valueOf(taskRequest.getId()), taskRequest.getAutomationRequests());
          for (AutomationRequest automationRequest : taskRequest.getAutomationRequests()) {
            rectifySelectorDuringImport(automationRequest);
            taskService.addAutomation(Long.valueOf(taskRequest.getId()), automationRequest);
          }

          if (!Utility.isEmpty(taskRequest.getTaskRecurrence())) {
            JsonNode emptyNode = JsonNodeFactory.instance.objectNode();
            SetTaskRecurrentRequest currentTaskRecurrence = taskRequest.getTaskRecurrence();
            setDefaultValuesForTaskRecurrence(currentTaskRecurrence, emptyNode);
            taskService.setTaskRecurrence(Long.valueOf(taskRequest.getId()), currentTaskRecurrence);
          }

          if (!Utility.isEmpty(taskRequest.getTaskSchedules())) {
            taskService.setTaskSchedules(Long.valueOf(taskRequest.getId()), taskRequest.getTaskSchedules());
          }

          if (!Utility.isEmpty(taskRequest.getTaskExecutorLock())) {
            taskService.addTaskExecutorLock(Long.valueOf(taskRequest.getId()), taskRequest.getTaskExecutorLock());
          }

          InterlockDto interlockDto = taskRequest.getInterlocks();
          if (!Utility.isEmpty(interlockDto) && !Utility.isEmpty(interlockDto.getValidations())) {
            InterlockRequest interlockRequest = new InterlockRequest();
            interlockRequest.setValidations(interlockDto.getValidations());
            taskService.addInterlockForTask(taskRequest.getId(), interlockRequest);
          }

          if (!Utility.isEmpty(taskRequest.getPrerequisiteTaskIds())) {
            TaskDependencyRequest taskDependencyRequest = TaskDependencyRequest.builder().prerequisiteTaskIds(taskRequest.getPrerequisiteTaskIds()).build();
            taskDependencyService.updateTaskDependency(Long.valueOf(taskRequest.getId()), taskDependencyRequest);
          }
        }
      }
      parameterRepositoryImpl.bulkInsertIntoParameters(allParameters, principalUserEntity);

      if (!Utility.isEmpty(importCheckListRequest.getActionRequests())) {
        for (ImportActionRequest actionsRequest : importCheckListRequest.getActionRequests()) {
          actionsRequest.setChecklistId(Long.valueOf(importCheckListRequest.getId()));
          BasicDto createdAction =actionService.createAction(actionsRequest);
          System.out.println(createdAction.getId() + " " + actionsRequest.getId());
          if (!Utility.isEmpty(actionsRequest.getEffectRequests())) {
            Map<String, Long> effectIdMap = new HashMap<>();
            for (ImportEffectRequest effectRequest : actionsRequest.getEffectRequests()) {
              normalizeEffectApiEndpointUrl(effectRequest, organisation);
              effectRequest.setActionId(Long.valueOf(createdAction.getId()));
              JsonNode query = effectRequest.getQuery();
              if (!Utility.isEmpty(query) && query.has("root") && !query.get("root").isNull()) {
                EffectRootNode effectRootNode = convertJsonNodeToPojo(query.get("root"), EffectRootNode.class);
                if (!Utility.isEmpty(effectRootNode)) {
                  patchEffectIds(effectRootNode, effectIdMap);
                  JsonNode patchedQuery = objectMapper.valueToTree(effectRootNode);
                  effectRequest.setQuery(wrapQueryWithRoot(patchedQuery));
                }
              }

              BasicDto createdEffect  = effectService.createEffects(effectRequest, Long.valueOf(createdAction.getId()));
              effectIdMap.put(effectRequest.getId(), Long.valueOf(createdEffect.getId()));
            }
          }
        }
      }

      addStopDependency.migrateHasStopToDependencyForChecklist(checklist);
    }
    if (Utility.isNotNull(checklist)) {
      checklistAuditService.importChecklist(checklist.getId(), checklist.getCode(), principalUser);
    }

    var basicDto = new BasicDto();
    basicDto.setMessage("success");
    return basicDto;
  }

  private void handleParameterMedias(ParameterCreateRequest parameterCreateRequest, Map<String, ByteArrayOutputStream> fileNameAndOutputStreamMap) throws IOException, StreemException {
    JsonNode data = parameterCreateRequest.getData();
    for (int i = 0; i < data.size(); i++) {
      Map currMedia = objectMapper.convertValue(data.get(i), Map.class);
      String link = (String) currMedia.get("link");
      String filename = (String) currMedia.get("filename");
      String name = (String) currMedia.get("name");

      ImportMediaRequest mediaRequest = new ImportMediaRequest();
      mediaRequest.setName(name);
      mediaRequest.setLink(link);
      mediaRequest.setFileName(filename);

      ByteArrayOutputStream currentMediaOutputStream = fileNameAndOutputStreamMap.get(filename);
      if (Utility.isEmpty(currentMediaOutputStream)) {
        ValidationUtils.invalidate(mediaRequest.getFileName(), ErrorCode.MEDIA_NOT_FOUND);
      }
      MultipartFile file = new CustomMultipartFile(new ByteArrayInputStream(currentMediaOutputStream.toByteArray()), filename);
      MediaUploadResponse mediaUploadResponse = mediaService.save(mediaRequest, file);

      currMedia.put("mediaId", mediaUploadResponse.getMediaId());
      currMedia.put("link", mediaUploadResponse.getLink());
      ((ArrayNode) data).set(i, objectMapper.convertValue(currMedia, JsonNode.class));
    }

    parameterCreateRequest.setData(data);
  }

    private void handleNumberParameterValidations(ParameterCreateRequest parameterCreateRequest) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();

    JsonNode validationsNode = objectMapper.readTree(parameterCreateRequest.getValidations().toString());
    if (validationsNode.isObject() && validationsNode.isEmpty()) {
      parameterCreateRequest.setValidations(objectMapper.createArrayNode());
    }
    List<ParameterValidationDto> parameterValidationDtoList = new ArrayList<>();
    if (validationsNode.isArray()) {
      parameterValidationDtoList = JsonUtils.jsonToCollectionType(parameterCreateRequest.getValidations(), List.class, ParameterValidationDto.class);
    } else if (validationsNode.isObject()) {
      ParameterValidationDto singleValidation = objectMapper.convertValue(validationsNode, ParameterValidationDto.class);
      parameterValidationDtoList.add(singleValidation);
    }

    List<ParameterValidationDto> newParameterValidationDtoList = new ArrayList<>();
    for (ParameterValidationDto parameterValidationDto : parameterValidationDtoList) {
      ParameterValidationDto newParameterValidationDto = new ParameterValidationDto();
      newParameterValidationDto.setRuleId(Utility.generateUuid());
      List<ResourceParameterPropertyValidationDto> resourceParameterValidationList = parameterValidationDto.getResourceParameterValidations();
      List<CriteriaValidationDto> criteriaValidationList = parameterValidationDto.getCriteriaValidations();

      if (!Utility.isEmpty(parameterValidationDto.getExceptionApprovalType())) {
        newParameterValidationDto.setExceptionApprovalType(parameterValidationDto.getExceptionApprovalType());
      } else {
        newParameterValidationDto.setExceptionApprovalType(null);
      }

      if (!Utility.isEmpty(criteriaValidationList) && Utility.isEmpty(parameterValidationDto.getExceptionApprovalType())) {
        newParameterValidationDto.setExceptionApprovalType(Type.ParameterExceptionApprovalType.DEFAULT_FLOW);
        newParameterValidationDto.setRuleId(Utility.generateUuid());
      }
      if (!Utility.isEmpty(resourceParameterValidationList)) {
        newParameterValidationDto.setValidationType(String.valueOf(Type.ParameterRelationValidationType.RESOURCE));
        newParameterValidationDto.setResourceParameterValidations(resourceParameterValidationList);
      }
      if (!Utility.isEmpty(criteriaValidationList)) {
        newParameterValidationDto.setValidationType(String.valueOf(Type.ParameterRelationValidationType.CRITERIA));
        newParameterValidationDto.setCriteriaValidations(criteriaValidationList);
      }
      newParameterValidationDtoList.add(newParameterValidationDto);
    }
    if(!newParameterValidationDtoList.isEmpty()) {
      parameterCreateRequest.setValidations(JsonUtils.valueToNode(newParameterValidationDtoList));
    }
    if (validationsNode.has("resourceParameterValidations") && validationsNode.get("resourceParameterValidations").isEmpty()) {
      parameterCreateRequest.setValidations(objectMapper.createArrayNode());
    }

      JsonNode dataNode = parameterCreateRequest.getData();
      if (dataNode.has("leastCount") && dataNode.get("leastCount").isTextual()) {
        String leastCountValue = dataNode.get("leastCount").asText();
        ((ObjectNode) dataNode).set("leastCount", objectMapper.createObjectNode()
          .put("value", leastCountValue)
          .put("selector", "CONSTANT"));
        parameterCreateRequest.setData(dataNode);
      }
  }

  private void handleDateAndDateTimeParameterValidations(ParameterCreateRequest parameterCreateRequest) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();

    JsonNode validationsNode = objectMapper.readTree(parameterCreateRequest.getValidations().toString());
    if (validationsNode.isObject() && validationsNode.isEmpty()) {
      parameterCreateRequest.setValidations(objectMapper.createArrayNode());
    }

    List<ParameterValidationDto> parameterValidationDtoList = new ArrayList<>();
    if (validationsNode.isArray()) {
      parameterValidationDtoList = JsonUtils.jsonToCollectionType(parameterCreateRequest.getValidations(), List.class, ParameterValidationDto.class);
    }
    List<ParameterValidationDto> newParameterValidationDtoList = new ArrayList<>();

    for (ParameterValidationDto parameterValidationDto : parameterValidationDtoList) {
      ParameterValidationDto newParameterValidationDto = new ParameterValidationDto();
      List<DateParameterValidationDto> dateParameterValidationDtoList = parameterValidationDto.getDateTimeParameterValidations();

      if (!Utility.isEmpty(parameterValidationDto.getExceptionApprovalType())) {
        newParameterValidationDto.setExceptionApprovalType(parameterValidationDto.getExceptionApprovalType());
      } else {
        newParameterValidationDto.setExceptionApprovalType(Type.ParameterExceptionApprovalType.DEFAULT_FLOW);
      }
      newParameterValidationDto.setRuleId(Utility.generateUuid());


      if (!Utility.isEmpty(dateParameterValidationDtoList)) {
        newParameterValidationDto.setDateTimeParameterValidations(dateParameterValidationDtoList);
      }

      newParameterValidationDtoList.add(newParameterValidationDto);
    }
    parameterCreateRequest.setValidations(JsonUtils.valueToNode(newParameterValidationDtoList));
    if (validationsNode.has("dateTimeParameterValidations") && validationsNode.get("dateTimeParameterValidations").isEmpty()) {
      parameterCreateRequest.setValidations(objectMapper.createArrayNode());
    }
  }

/*For the usecase-> where process is imported from one facility to other than copy of process is created if checklist is already present in DB
  This method replaces the old Ids to new Ids and create copy of checklist in DB for requested facility.
 */

  private void handleResourceParameterValidations(ParameterCreateRequest parameterCreateRequest) throws JsonProcessingException {
    List<ParameterValidationDto> newValidationList = new ArrayList<>();

    JsonNode dataNode = parameterCreateRequest.getData();

    if (!Utility.isEmpty(parameterCreateRequest.getValidations())) {
      List<ParameterValidationDto> parameterValidations = JsonUtils.readValue(parameterCreateRequest.getValidations().toString(),new TypeReference<List<ParameterValidationDto>>() {
      });

      for (ParameterValidationDto validationDto : parameterValidations) {
        ParameterValidationDto newValidationDto = new ParameterValidationDto();
        newValidationDto.setRuleId(Utility.generateUuid());

        if (!Utility.isEmpty(validationDto.getPropertyValidations())) {
          newValidationDto.setPropertyValidations(validationDto.getPropertyValidations());
        }

        if (!Utility.isEmpty(validationDto.getExceptionApprovalType())) {
          newValidationDto.setExceptionApprovalType(validationDto.getExceptionApprovalType());
        } else {
          newValidationDto.setExceptionApprovalType(Type.ParameterExceptionApprovalType.DEFAULT_FLOW);
        }

        newValidationList.add(newValidationDto);
      }
    }

    if (!Utility.isEmpty(dataNode) && dataNode.has("propertyValidations") && Utility.isEmpty(newValidationList)) {
      List<ParameterRelationPropertyValidationDto> propertyValidations = JsonUtils.readValue(dataNode.get("propertyValidations").toString(),new TypeReference<List<ParameterRelationPropertyValidationDto>>() {
        });
      if(!Utility.isEmpty(propertyValidations)) {
        for (ParameterRelationPropertyValidationDto propertyValidation : propertyValidations) {
          if (!Utility.isEmpty(propertyValidation.getValue())) {
            propertyValidation.setSelector(Type.SelectorType.CONSTANT);
          }

          ParameterValidationDto newValidation = new ParameterValidationDto();
          newValidation.setRuleId(Utility.generateUuid());
          List<ParameterRelationPropertyValidationDto> singlePropertyValidationList = new ArrayList<>();
          singlePropertyValidationList.add(propertyValidation);
          newValidation.setPropertyValidations(singlePropertyValidationList);
          if (!Utility.isEmpty(parameterCreateRequest.getExceptionApprovalType())) {
            newValidation.setExceptionApprovalType(parameterCreateRequest.getExceptionApprovalType());
          } else {
            newValidation.setExceptionApprovalType(Type.ParameterExceptionApprovalType.DEFAULT_FLOW);
          }
          newValidationList.add(newValidation);
        }
      }

      ((ObjectNode) dataNode).remove("propertyValidations");
      parameterCreateRequest.setData(dataNode);
    }

    parameterCreateRequest.setValidations(JsonUtils.valueToNode(newValidationList));
  }

  private ImportChecklistRequest updateRequestWithNewIds(ImportChecklistRequest importCheckListRequest) throws JsonProcessingException {
    log.info("[updateRequestWithNewIds] creating copy of checklist with new Ids importCheckListRequest: {}", importCheckListRequest);
    Set<String> oldIds = new HashSet<>();

    oldIds.add(importCheckListRequest.getId());

    for (ImportStageRequest importStageRequest : importCheckListRequest.getStageRequests()) {
      oldIds.add(importStageRequest.getId());

      for (ImportTaskRequest importTaskRequest : importStageRequest.getTaskRequests()) {
        oldIds.add(importTaskRequest.getId());

        for (ImportParameterRequest importParameterRequest : importTaskRequest.getParameterRequests()) {
          oldIds.add(importParameterRequest.getId());
        }
        /*we are setting id null here as in add automations we have logic of assigning createdBy user and id if id is null
       and if we are importing in same facility then it will create issues, better to get ids from add automation method
         */
        for (AutomationRequest automationRequest : importTaskRequest.getAutomationRequests()) {
          automationRequest.setId(null);
        }
      }
    }

    if (!Utility.isEmpty(importCheckListRequest.getParameterRequests())) {
      for (ImportParameterRequest importParameterRequest : importCheckListRequest.getParameterRequests()) {
        oldIds.add(importParameterRequest.getId());
      }
    }

    String jsonData = objectMapper.writeValueAsString(importCheckListRequest);
    for (String oldId : oldIds) {
      jsonData = jsonData.replace(oldId, String.valueOf(IdGenerator.getInstance().nextId()));
    }

    return objectMapper.readValue(jsonData, ImportChecklistRequest.class);
  }

  private List<Stage> createStages(User principalUserEntity, List<ImportStageRequest> stageRequests, Checklist checklist) {
    log.info("[createStages] Request to create stages, stageRequests: {}, checklistId: {}", stageRequests, checklist.getId());
    List<Stage> stages = new ArrayList<>();
    for (ImportStageRequest stageRequest : stageRequests) {
      Stage stage = buildStage(principalUserEntity, stageRequest, checklist);
      stages.add(stage);
    }
    stages = stageRepository.saveAll(stages);
    checklist.setStages(new HashSet<>(stages));
    return stages;
  }

  private void createTasks(User principalUserEntity, List<ImportStageRequest> stageRequests, List<Stage> stages) {
    log.info("[createTasks] Request to create tasks stageRequests: {}, stages: {}", stageRequests, stages);
    for (int i = 0; i < stages.size(); i++) {
      Stage stage = stages.get(i);
      List<Task> tasks = new ArrayList<>();
      for (ImportTaskRequest taskRequest : stageRequests.get(i).getTaskRequests()) {
        Task task = buildTask(principalUserEntity, taskRequest, stages.get(i));
        tasks.add(task);
      }
      tasks = taskRepository.saveAll(tasks);
      stage.setTasks(new HashSet<>(tasks));
    }
  }

  private Checklist createChecklist(PrincipalUser principalUser, User principalUserEntity, ImportChecklistRequest importCheckListRequest, Facility facility, Organisation organisation, UseCase useCase, Long currentFacilityId) {
    log.info("[createChecklist] Request to create checklist, importCheckListRequest: {}, facility: {}, organisation: {}, useCase: {}", importCheckListRequest, facility, organisation, useCase);
    Checklist checklist = new Checklist();
    checklist.setId(Long.valueOf(importCheckListRequest.getId()));
    checklist.setName(importCheckListRequest.getName());
    checklist.setDescription(importCheckListRequest.getDescription());
    checklist.setColorCode(importCheckListRequest.getColorCode());
    checklist.setOrganisation(organisation);
    checklist.setOrganisationId(organisation.getId());
    checklist.setCode(codeService.getCode(Type.EntityType.CHECKLIST, principalUser.getOrganisationId()));
    checklist.setUseCase(useCase);
    checklist.setUseCaseId(useCase.getId());
    checklist.setState(State.Checklist.BEING_BUILT);
    checklist.setCreatedBy(principalUserEntity);
    checklist.setModifiedBy(principalUserEntity);
    if (currentFacilityId != -1) {
      checklist.addFacility(facility, principalUserEntity);
    } else {
      checklist.setGlobal(true);
    }

    checklist.addPrimaryAuthor(principalUserEntity, checklist.getReviewCycle(), principalUserEntity);
    Version version = versionService.createNewVersion(checklist.getId(), Type.EntityType.CHECKLIST, principalUserEntity);
    checklist.setVersion(version);

    return checklistRepository.save(checklist);
  }

  private Stage buildStage(User principalUserEntity, ImportStageRequest importStageRequest, Checklist checklist) {
    log.info("[buildStage] Request to build stage, ImportStageRequest: {}, checklistId: {}", importStageRequest, checklist.getId());
    Stage stage = new Stage();
    stage.setId(Long.valueOf(importStageRequest.getId()));
    stage.setModifiedBy(principalUserEntity);
    stage.setCreatedBy(principalUserEntity);
    stage.setChecklist(checklist);
    stage.setName(importStageRequest.getName());
    stage.setOrderTree(importStageRequest.getOrderTree());
    return stage;
  }

  private Task buildTask(User principalUserEntity, ImportTaskRequest taskRequest, Stage stage) {
    log.info("[buildTask] Request to build task, taskRequest: {}, stageId: {}", taskRequest, stage.getId());
    Task task = new Task();
    task.setId(Long.valueOf(taskRequest.getId()));
    task.setName(taskRequest.getName());
    task.setHasStop(taskRequest.isHasStop());
    task.setHasBulkVerification(taskRequest.isHasBulkVerification());
    task.setSoloTask(taskRequest.isSoloTask());
    task.setTimed(taskRequest.isTimed());
    task.setMinPeriod(taskRequest.getMinPeriod());
    task.setMaxPeriod(taskRequest.getMaxPeriod());
    task.setTimerOperator(taskRequest.getTimerOperator());
    task.setMandatory(taskRequest.isMandatory());
    task.setModifiedBy(principalUserEntity);
    task.setCreatedBy(principalUserEntity);
    task.setOrderTree(taskRequest.getOrderTree());
    task.setStage(stage);
    return task;
  }

  private void rectifySelectorDuringImport(AutomationRequest automationRequest) throws JsonProcessingException {
    ObjectNode actionDetails = (ObjectNode) automationRequest.getActionDetails();
    JsonNode selectorNode = automationRequest.getActionDetails().get("selector");
    String selector = null;
    if (selectorNode != null) {
      selector = selectorNode.textValue();
    }
    JsonNode parameterNode = automationRequest.getActionDetails().get("parameterId");
    String parameter = "";
    if (parameterNode != null) {
      parameter = parameterNode.textValue();
    }
    if (Utility.isEmpty(selector)) {
      switch (automationRequest.getActionType()) {
        case SET_PROPERTY, INCREASE_PROPERTY, DECREASE_PROPERTY, SET_RELATION -> {
          if (Utility.isEmpty(parameter)) {
            actionDetails.put("selector", "CONSTANT");
          } else {
            actionDetails.put("selector", "PARAMETER");
          }
        }
        case ARCHIVE_OBJECT, CREATE_OBJECT -> actionDetails.put("selector", "NONE");
      }
    }
    if (Objects.requireNonNull(automationRequest.getActionType()) == Type.AutomationActionType.SET_PROPERTY) {
      JsonNode propertyInputTypeNode = automationRequest.getActionDetails().get("propertyInputType");
      if (propertyInputTypeNode != null) {
        String propertyInputType = propertyInputTypeNode.textValue();
        if ("DATE".equals(propertyInputType) || "DATE_TIME".equals(propertyInputType)) {
          AutomationActionDateTimeDto automationActionDateTimeDto = JsonUtils.readValue(automationRequest.getActionDetails().toString(), AutomationActionDateTimeDto.class);
          if (Utility.isEmpty(automationActionDateTimeDto.getOffsetSelector())) {
            automationActionDateTimeDto.setOffsetSelector(Type.OffSetSelectorType.CONSTANT);
          }
          if(Utility.isEmpty(automationActionDateTimeDto.getOffsetValue()) && !Utility.isEmpty(automationActionDateTimeDto.getValue())){
            automationActionDateTimeDto.setOffsetValue(Double.valueOf(automationActionDateTimeDto.getValue()));
          }
          if(Utility.isEmpty(automationActionDateTimeDto.getOffsetValue()) && Utility.isEmpty(automationActionDateTimeDto.getValue())){
            automationActionDateTimeDto.setOffsetValue(null);
          }
          if(!Utility.isEmpty(automationActionDateTimeDto.getOffsetDateUnit())){
            automationActionDateTimeDto.setOffsetDateUnit(automationActionDateTimeDto.getOffsetDateUnit());
          }
          if(!Utility.isEmpty(automationActionDateTimeDto.getValue()) && (automationActionDateTimeDto.getSelector().equals(Type.SelectorType.CONSTANT))) {
            JsonNode dateUnit = (automationRequest.getActionDetails().get("dateUnit"));
            if (!Utility.isEmpty(dateUnit)){
              if (!Utility.isEmpty(dateUnit.textValue())) {
                automationActionDateTimeDto.setOffsetDateUnit(CollectionMisc.DateUnit.valueOf(dateUnit.textValue()));
              }
            }
          }

          if (automationActionDateTimeDto.getOffsetSelector().equals(Type.OffSetSelectorType.PARAMETER)) {
            if (!Utility.isEmpty(automationActionDateTimeDto.getOffsetParameterId())) {
              automationActionDateTimeDto.setOffsetParameterId(automationActionDateTimeDto.getOffsetParameterId());
            } else if (!Utility.isEmpty(automationActionDateTimeDto.getParameterId())) {
              automationActionDateTimeDto.setOffsetParameterId(automationActionDateTimeDto.getParameterId());
            } else {
              automationActionDateTimeDto.setOffsetParameterId(null);
            }
          }
          actionDetails.put("offsetSelector", automationActionDateTimeDto.getOffsetSelector() != null ? automationActionDateTimeDto.getOffsetSelector().name() : null);
          actionDetails.put("offsetValue",automationActionDateTimeDto.getOffsetParameterId() == null ? automationActionDateTimeDto.getOffsetValue() : null);
          actionDetails.put("value", automationActionDateTimeDto.getValue());
          actionDetails.put("offsetDateUnit", automationActionDateTimeDto.getOffsetDateUnit() != null ? automationActionDateTimeDto.getOffsetDateUnit().name() : null);
          actionDetails.put("offsetParameterId", automationActionDateTimeDto.getOffsetParameterId());
          actionDetails.put("parameterId", automationActionDateTimeDto.getParameterId());
        }
      }
    }
    automationRequest.setActionDetails(actionDetails);
  }

  //set default values for task recurrence keys
  private void setDefaultValuesForTaskRecurrence(SetTaskRecurrentRequest taskRecurrence, JsonNode emptyNode) {
    // Check and set default intervals
    if (Utility.isEmpty(taskRecurrence.getNegativeStartDateToleranceInterval())) {
      taskRecurrence.setNegativeStartDateToleranceInterval(0);
    }
    if (Utility.isEmpty(taskRecurrence.getNegativeDueDateToleranceInterval())) {
      taskRecurrence.setNegativeDueDateToleranceInterval(0);
    }
    if (Utility.isEmpty(taskRecurrence.getPositiveStartDateToleranceInterval())) {
      taskRecurrence.setPositiveStartDateToleranceInterval(0);
    }
    if (Utility.isEmpty(taskRecurrence.getPositiveDueDateToleranceInterval())) {
      taskRecurrence.setPositiveDueDateToleranceInterval(0);
    }

    // Check and set default durations
    if (Utility.isEmpty(taskRecurrence.getNegativeStartDateToleranceDuration())) {
      taskRecurrence.setNegativeStartDateToleranceDuration(emptyNode);
    }
    if (Utility.isEmpty(taskRecurrence.getNegativeDueDateToleranceDuration())) {
      taskRecurrence.setNegativeDueDateToleranceDuration(emptyNode);
    }
    if (Utility.isEmpty(taskRecurrence.getPositiveStartDateToleranceDuration())) {
      taskRecurrence.setPositiveStartDateToleranceDuration(emptyNode);
    }
    if (Utility.isEmpty(taskRecurrence.getPositiveDueDateToleranceDuration())) {
      taskRecurrence.setPositiveDueDateToleranceDuration(emptyNode);
    }
  }

  public static void patchEffectIds(EffectRootNode root,
                                    Map<String, Long> idMap) {

    if (!Utility.isEmpty(root.getChildren())) {
      for (EffectChildNode effectChildNode : root.getChildren()) {
        patchChild(effectChildNode, idMap);
      }
    }
  }

  private static void patchChild(EffectChildNode node,
                                 Map<String, Long> idMap) {

    if (!Utility.isEmpty(node.getChildren())) {
      for (EffectTextNode text : node.getChildren()) {
        patchText(text, idMap);
      }
    }
  }

  private static void patchText(EffectTextNode node,Map<String, Long> idMap) {

    EffectDataNode data = node.getData();
    if (!Utility.isEmpty(data) && !Utility.isEmpty(data.getEntity())) {
        Type.EffectEntityType entity = Type.EffectEntityType.valueOf(data.getEntity().toUpperCase());
          if (entity == Type.EffectEntityType.EFFECT) {
            String oldId = data.getId();
            Long   newId = idMap.get(oldId);
            if (!Utility.isEmpty(newId)) {
              data.setId(String.valueOf(newId));
          }
        }
    }
  }

  /**
   * Ensures the query is always wrapped under a "root" property for import.
   * If already wrapped, returns as is. If not, wraps the query node.
   */
  private JsonNode wrapQueryWithRoot(JsonNode query) {
    if (query == null) return null;
    if (query.has("root")) {
      // Already wrapped
      return query;
    }
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode rootWrapper = mapper.createObjectNode();
    rootWrapper.set("root", query);
    return rootWrapper;
  }

  /**
   * Normalizes the apiEndpoint URL for REST_API POST effects to use the organisation's FQDN.
   */
  private void normalizeEffectApiEndpointUrl(ImportEffectRequest effectRequest, Organisation organisation) {
    if (EffectType.REST_API.equals(effectRequest.getEffectType()) && "POST".equalsIgnoreCase(effectRequest.getApiMethod())) {
      JsonNode apiEndpointNode = effectRequest.getApiEndpoint();
      String url = null;
      if (!Utility.isEmpty(apiEndpointNode) && apiEndpointNode.has("root")) {
        JsonNode rootNode = apiEndpointNode.get("root");
        if (!Utility.isEmpty(rootNode) && rootNode.has("children")) {
          for (JsonNode para : rootNode.get("children")) {
            if (para.has("children")) {
              for (JsonNode textNode : para.get("children")) {
                if (textNode.has("text")) {
                  url = textNode.get("text").asText();
                  break;
                }
              }
            }
            if (!Utility.isEmpty(url)) break;
          }
        }
      }
      if (!Utility.isEmpty(url) && (url.contains("leucinetech") || url.contains("leucine"))) {
        String fqdn = organisation.getFqdn();
        if (!Utility.isEmpty(fqdn) && !fqdn.startsWith("http")) {
          fqdn = "https://" + fqdn;
        }
        try {
          URI oldUri = new URI(url);
          URI fqdnUri = new URI(fqdn);
          String newUrl = fqdnUri.getScheme() + "://" + fqdnUri.getHost() + (fqdnUri.getPort() != -1 ? ":" + fqdnUri.getPort() : "") + oldUri.getPath();
          if (!Utility.isEmpty(apiEndpointNode) && apiEndpointNode.has("root")) {
            JsonNode rootNode = apiEndpointNode.get("root");
            if (!Utility.isEmpty(rootNode) && rootNode.has("children")) {
              for (JsonNode para : rootNode.get("children")) {
                if (para.has("children")) {
                  for (JsonNode textNode : para.get("children")) {
                    if (textNode.has("text")) {
                      ((ObjectNode) textNode).put("text", newUrl);
                    }
                  }
                }
              }
            }
          }
          effectRequest.setApiEndpoint(apiEndpointNode);
        } catch (Exception e) {
          log.warn("Failed to replace FQDN in apiEndpoint: {}", url, e);
        }
      }
    }
  }
}
