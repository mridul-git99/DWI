package com.leucine.streem.dto.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.leucine.streem.config.MediaConfig;
import com.leucine.streem.constant.Action;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.mapper.helper.IBaseMapper;
import com.leucine.streem.dto.projection.CorrectionListViewProjection;
import com.leucine.streem.dto.request.ParameterCreateRequest;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.parameter.MaterialParameter;
import com.leucine.streem.repository.*;
import com.leucine.streem.util.JsonUtils;
import com.leucine.streem.util.Utility;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Mapper(uses = {IParameterVerificationMapper.class, IUserMapper.class}, nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class IParameterMapper implements IBaseMapper<ParameterDto, Parameter>, IAuditMapper {
  @Autowired
  protected MediaConfig mediaConfig;

  @Autowired
  IParameterVerificationMapper parameterVerificationMapper;

  @Autowired
  ITempParameterVerificationMapper tempParameterVerificationMapper;

  @Autowired
  ICorrectionRepository correctionRepository;

  @Autowired
  ICorrectionMediaMappingRepository correctionMediaMappingRepository;
  @Autowired
  ICorrectorRepository correctorRepository;

  @Autowired
  IReviewerRepository reviewerRepository;

  @Autowired
  ICorrectionMapper correctionMapper;

  @Autowired
  ICorrectorMapper correctorMapper;

  @Autowired
  IReviewerMapper reviewerMapper;
  @Autowired
  IParameterExceptionRepository parameterExceptionRepository;
  @Autowired
  IParameterExceptionMapper parameterExceptionMapper;
  @Autowired
  IParameterExceptionReviewerRepository parameterExceptionReviewerRepository;
  @Autowired
  IParameterExceptionReviewerMapper parameterExceptionReviewerMapper;


  MediaDto toMediaDto(Media media) {
    var mediaDto = new MediaDto();
    mediaDto.setId(media.getIdAsString());
    mediaDto.setType(media.getType());
    mediaDto.setName(media.getName());
    mediaDto.setDescription(media.getDescription());
    mediaDto.setLink(mediaConfig.getCdn() + java.io.File.separator + media.getRelativePath() + java.io.File.separator + media.getFilename());
    mediaDto.setArchived(media.isArchived());
    mediaDto.setFilename(media.getFilename());
    return mediaDto;
  }

  public abstract Parameter toEntity(ParameterCreateRequest parameterCreateRequest);

  public abstract ParameterInfoDto toBasicDto(Parameter parameter);

  public abstract List<ParameterInfoDto> toBasicDto(List<Parameter> parameters);

  MaterialParameter toMaterialParameter(ParameterMediaMapping parameterMediaMapping, MaterialMediaDto materialMediaDto) {
    Media media = parameterMediaMapping.getMedia();
    MaterialParameter materialParameter = new MaterialParameter();
    materialParameter.setName(media.getName());
    materialParameter.setQuantity(materialMediaDto.getQuantity());
    materialParameter.setOriginalFilename(media.getOriginalFilename());
    materialParameter.setId(materialMediaDto.getId());
    materialParameter.setMediaId(media.getIdAsString());
    materialParameter.setType(media.getType());
    materialParameter.setDescription(media.getDescription());
    materialParameter.setLink(mediaConfig.getCdn() + java.io.File.separator + media.getRelativePath() + java.io.File.separator + media.getFilename());
    materialParameter.setFilename(media.getFilename());
    return materialParameter;
  }

  public abstract TempParameterDto toTempParameterDto(Parameter parameter);

  @Named(value = "toParameterDto")
  @Mapping(target = "response", ignore = true)
  public abstract ParameterDto toDto(Parameter parameter,
                                     @Context Map<Long, List<ParameterValue>> parameterValueMap,
                                     @Context Map<Long, TaskExecution> taskExecutionMap,
                                     @Context Map<Long, List<TempParameterValue>> tempParameterValueMap,
                                     @Context Map<Long, List<TaskPauseReasonOrComment>> pauseReasonOrCommentMap,
                                     @Context Map<Long, List<ParameterVerification>> parameterVerificationMapPeerAndSelf,
                                     @Context Map<Long, List<TempParameterVerification>> tempParameterVerificationMapPeerAndSelf);

  @Named(value = "toParameterDtoList")
  @IterableMapping(qualifiedByName = "toParameterDto")
  public abstract List<ParameterDto> toDto(Set<Parameter> parameters,
                                           @Context Map<Long, List<ParameterValue>> parameterValueMap,

                                           @Context Map<Long, TaskExecution> taskExecutionMap,
                                           @Context Map<Long, List<TempParameterValue>> tempParameterValueMap,
                                           @Context Map<Long, List<TaskPauseReasonOrComment>> pauseReasonOrCommentMap,
                                           @Context Map<Long, List<ParameterVerification>> parameterVerificationMapPeerAndSelf,
                                           @Context Map<Long, List<TempParameterVerification>> tempParameterVerificationMapPeerAndSelf);

  @AfterMapping
  public void setParameterValues(Parameter parameter, @MappingTarget ParameterDto parameterDto) {
    if (Type.Parameter.MATERIAL.equals(parameter.getType()) && !Utility.isEmpty(parameter.getData().toString())) {
      try {
        parameterDto.setData(JsonUtils.valueToNode(getMaterialParameters(parameter)));
        parameterDto.setMetadata(parameter.getMetadata());
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  @AfterMapping
  public void setParameterValues(Parameter parameter, @MappingTarget ParameterDto parameterDto,
                                 @Context Map<Long, List<ParameterValue>> parameterIdParameterValueListMap,
                                 @Context Map<Long, TaskExecution> taskExecutionMap,
                                 @Context Map<Long, List<TempParameterValue>> tempParameterValueMap,
                                 @Context Map<Long, List<TaskPauseReasonOrComment>> pauseReasonOrCommentMap,
                                 @Context Map<Long, List<ParameterVerification>> parameterVerificationMapPeerAndSelf,
                                 @Context Map<Long, List<TempParameterVerification>> tempParameterVerificationMapPeerAndSelf
  ) {
    List<ParameterValue> parameterValues = parameterIdParameterValueListMap.get(parameter.getId());
    Map<Long, CorrectionDto> correctionDtoMap = getCorrectionDtoMap(parameterValues, parameter);
    if (Utility.isEmpty(correctionDtoMap)) {
      correctionDtoMap = new HashMap<>();
    }
    Map<Long, List<ParameterExceptionDto>> exceptionDtoMap = getParameterExceptionDto(parameterValues);
    if (Utility.isEmpty(exceptionDtoMap)) {
      exceptionDtoMap = new HashMap<>();
    }
    //Logic for error correction

    if (Type.Parameter.MATERIAL.equals(parameterDto.getType()) && !Utility.isEmpty(parameter.getData())) {
      try {
        parameterDto.setData(JsonUtils.valueToNode(getMaterialParameters(parameter)));
        parameterDto.setMetadata(parameter.getMetadata());
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    Map<Long, Map<Long, TempParameterValue>> tempParameterValueTaskExecutionIdMap = new HashMap<>();

    // TODO optimize this
    if (!Utility.isEmpty(tempParameterValueMap)) {
      for (Map.Entry<Long, List<TempParameterValue>> entry : tempParameterValueMap.entrySet()) {
        Long parameterId = entry.getKey();
        List<TempParameterValue> tempParameterValues = entry.getValue();
        for (TempParameterValue tempParameterValue : tempParameterValues) {
          Map<Long, TempParameterValue> innerMap = tempParameterValueTaskExecutionIdMap.computeIfAbsent(tempParameterValue.getTaskExecutionId(), k -> new HashMap<>());
          innerMap.put(parameterId, tempParameterValue);
        }
      }
    }

    if (!Utility.isEmpty(parameterValues)) {
      List<ParameterValueDto> responses = new ArrayList<>();
      for (ParameterValue parameterValue : parameterValues) {
        ParameterValueDto parameterValueDto = new ParameterValueDto();
        TaskExecution taskExecution = taskExecutionMap.get(parameterValue.getTaskExecutionId());
        if (null != parameterValue) {
          parameterValueDto.setId(parameterValue.getIdAsString());
          parameterValueDto.setState(parameterValue.getState());

          // Why can task execution be empty? In the case of CJF parameter, we don't have a task execution
          // In this case, we allow executing the clause only for parameter of target entity type 'TASK' since it has task execution
          if (parameter.getTargetEntityType() == Type.ParameterTargetEntityType.TASK) {
            parameterValueDto.setTaskExecutionId(parameterValue.getTaskExecutionId().toString());
          }
          if (!Utility.isEmpty(taskExecution)) {
            parameterValueDto.setTaskExecutionOrderTree(taskExecution.getOrderTree());
          }

          // In this case, if it's a cjf parameter, we need to send its data, hence we allow executing this clause
//          if (Utility.isEmpty(taskExecution) || !taskExecution.isCorrectionEnabled() || parameter.getType() == Type.Parameter.MATERIAL || parameter.getType() == Type.Parameter.INSTRUCTION) {
          parameterValueDto.setChoices(parameterValue.getChoices());
          parameterValueDto.setValue(parameterValue.getValue());
          parameterValueDto.setReason(parameterValue.getReason());
          updateParameterApprovalDto(parameterValueDto, parameterValue.getParameterValueApproval());
          List<MediaDto> mediaDtos = new ArrayList<>();
          List<ParameterValueMediaMapping> parameterValueMediaMappings = parameterValue.getMedias();
          if (null != parameterValueMediaMappings) {
            for (ParameterValueMediaMapping actMedia : parameterValueMediaMappings) {
              if (!actMedia.isArchived()) {
                mediaDtos.add(toMediaDto(actMedia.getMedia()));
              }
            }
            parameterValueDto.setMedias(mediaDtos);
          }
          List<ParameterVerification> parameterVerifications = parameterVerificationMapPeerAndSelf.get(parameterValue.getId());
          List<ParameterVerificationDto> parameterVerificationDtos = new ArrayList<>();
          if (!Utility.isEmpty(parameterVerifications)) {
            for (ParameterVerification parameterVerification : parameterVerifications) {
              ParameterVerificationDto parameterVerificationDto = parameterVerificationMapper.toDto(parameterVerification);
              parameterVerificationDtos.add(parameterVerificationDto);
            }
            parameterValueDto.setParameterVerifications(parameterVerificationDtos);
          }
          List<VariationDto> variationDtos = new ArrayList<>();
          if (parameterValue.isHasVariations()) {
            setVariationsForParameter(parameter, parameterValue, variationDtos);
          }
          parameterValueDto.setVariations(variationDtos);

          parameterValueDto.setState(parameterValue.getState());
          parameterValueDto.setHidden(parameterValue.isHidden());
          parameterValueDto.setAudit(IAuditMapper.createAuditDto(parameterValue.getModifiedBy(), parameterValue.getModifiedAt()));
          parameterValueDto.setCorrection(correctionDtoMap.get(parameterValue.getId()));
          parameterValueDto.setException(exceptionDtoMap.get(parameterValue.getId()));
          parameterValueDto.setHasActiveException(parameterValue.isHasActiveException());
          responses.add(parameterValueDto);
//          } else {
//            Map<Long, TempParameterValue> map = tempParameterValueTaskExecutionIdMap.get(parameterValue.getTaskExecutionId());
//            TempParameterValue tempParameterValue = map.get(parameter.getId());
//
//            if (!Utility.isEmpty(tempParameterVerificationMapPeerAndSelf)) {
//
//              List<TempParameterVerification> tempParameterVerifications = tempParameterVerificationMapPeerAndSelf.get(tempParameterValue.getId());
//              List<ParameterVerificationDto> parameterVerificationDtos = new ArrayList<>();
//              tempParameterVerifications = Utility.isEmpty(tempParameterVerifications) ? new ArrayList<>() : tempParameterVerifications;
//
//              for (TempParameterVerification tempParameterVerification : tempParameterVerifications) {
//                ParameterVerificationDto parameterVerificationDto = tempParameterVerificationMapper.toDto(tempParameterVerification);
//                parameterVerificationDtos.add(parameterVerificationDto);
//              }
//              parameterValueDto.setParameterVerifications(parameterVerificationDtos);
//            }
//
//            parameterValueDto.setChoices(tempParameterValue.getChoices());
//            parameterValueDto.setValue(tempParameterValue.getValue());
//            parameterValueDto.setReason(tempParameterValue.getReason());
//            List<MediaDto> mediaDtos = new ArrayList<>();
//            List<TempParameterValueMediaMapping> parameterValueMedias = tempParameterValue.getMedias();
//            if (null != parameterValueMedias) {
//              for (TempParameterValueMediaMapping actMedia : parameterValueMedias) {
//                if (!actMedia.isArchived()) {
//                  mediaDtos.add(toMediaDto(actMedia.getMedia()));
//                }
//              }
//              parameterValueDto.setMedias(mediaDtos);
//            }
//            parameterValueDto.setState(tempParameterValue.getState());
//            parameterValueDto.setHidden(tempParameterValue.isHidden());
//            parameterValueDto.setAudit(IAuditMapper.createAuditDto(tempParameterValue.getModifiedBy(), tempParameterValue.getModifiedAt()));
//            responses.add(parameterValueDto);
//          }
        }
      }

      parameterDto.setResponse(responses);
    }
  }

  private void setVariationsForParameter(Parameter parameter, ParameterValue parameterValue, List<VariationDto> variationDtos) {
    List<Variation> variations = parameterValue.getVariations();


    ArrayNode newFilterArrayNode = JsonUtils.createArrayNode();
    ArrayNode oldFilterArrayNode = JsonUtils.createArrayNode();
    ArrayNode newValidationArrayNode = JsonUtils.createArrayNode();
    ArrayNode oldValidationArrayNode = JsonUtils.createArrayNode();

    List<VariationDto> filterVariationDtos = new ArrayList<>();
    List<VariationDto> validationVariationDtos = new ArrayList<>();

    for (Variation variation : variations) {

      VariationDto variationDto = new VariationDto();
      variationDto.setId(variation.getIdAsString());
      variationDto.setName(variation.getName());
      variationDto.setDescription(variation.getDescription());
      variationDto.setType(variation.getType());
      variationDto.setJobId(variation.getJobId().toString());
      variationDto.setVariationNumber(variation.getVariationNumber());
      variationDto.setOldVariation(variation.getOldDetails());
      variationDto.setNewVariation(variation.getNewDetails());
      variationDto.setOldVariation(variation.getOldDetails());
      variationDto.setParameterId(parameter.getId().toString());
      variationDto.setParameterName(parameter.getLabel());
      variationDto.setParameterType(parameter.getType().toString());

      if (variation.getType() == Action.Variation.FILTER) {
        newFilterArrayNode.add(variation.getNewDetails());
        oldFilterArrayNode.add(variation.getOldDetails());
        filterVariationDtos.add(variationDto);
      }
      if (variation.getType() == Action.Variation.VALIDATION) {
        newValidationArrayNode.add(variation.getNewDetails());
        oldValidationArrayNode.add(variation.getOldDetails());
        validationVariationDtos.add(variationDto);
      }

      List<MediaDto> variationMediaDtoList = new ArrayList<>();
      Set<VariationMediaMapping> medias = variation.getMedias();

      if (null != medias) {
        for (VariationMediaMapping variationMediaMapping : medias) {
          if (!variationMediaMapping.isArchived()) {
            variationMediaDtoList.add(toMediaDto(variationMediaMapping.getMedia()));
          }
        }
        variationDto.setMedias(variationMediaDtoList);
      }
      variationDtos.add(variationDto);
    }
    variationDtos.removeIf(variationDto -> variationDto.getType() == Action.Variation.FILTER);
    variationDtos.removeIf(variationDto -> variationDto.getType() == Action.Variation.VALIDATION);

    if (!Utility.isEmpty(filterVariationDtos)) {
      VariationDto filterVariationDto = filterVariationDtos.get(0);
      filterVariationDto.setNewVariation(newFilterArrayNode);
      filterVariationDto.setOldVariation(oldFilterArrayNode);
      variationDtos.add(filterVariationDto);
    }
    if (!Utility.isEmpty(validationVariationDtos)) {
      VariationDto validationVariationDto = validationVariationDtos.get(0);
      validationVariationDto.setNewVariation(newValidationArrayNode);
      validationVariationDto.setOldVariation(oldValidationArrayNode);
      variationDtos.add(validationVariationDto);
    }
  }


  private void updateParameterApprovalDto(ParameterValueDto parameterValueDto, ParameterValueApproval parameterValueApproval) {
    if (parameterValueApproval != null) {
      ParameterValueApprovalDto parameterValueApprovalDto = new ParameterValueApprovalDto();
      UserAuditDto userAuditDto = new UserAuditDto();
      if (null != parameterValueApproval.getUser()) {
        userAuditDto.setId(parameterValueApproval.getUser().getIdAsString());
        userAuditDto.setEmployeeId(parameterValueApproval.getUser().getEmployeeId());
        userAuditDto.setFirstName(parameterValueApproval.getUser().getFirstName());
        userAuditDto.setLastName(parameterValueApproval.getUser().getLastName());
      }
      parameterValueApprovalDto.setApprover(userAuditDto);
      parameterValueApprovalDto.setId(parameterValueApproval.getIdAsString());
      parameterValueApprovalDto.setState(parameterValueApproval.getState());
      parameterValueApprovalDto.setCreatedAt(parameterValueApproval.getCreatedAt());
      parameterValueDto.setParameterValueApprovalDto(parameterValueApprovalDto);
    }
  }

  private List<MaterialParameter> getMaterialParameters(Parameter parameter) throws JsonProcessingException {
    List<MaterialMediaDto> materialMediaDtos = JsonUtils.readValue(parameter.getData().toString(),
      new TypeReference<>() {
      });
    List<ParameterMediaMapping> parameterMediaMappings = parameter.getMedias();
    Map<Long, ParameterMediaMapping> parameterMediaMappingMap = parameterMediaMappings.stream().collect(Collectors.toMap(am -> am.getMedia().getId(), Function.identity()));

    List<MaterialParameter> materialParameters = new ArrayList<>();

    for (MaterialMediaDto materialMediaDto : materialMediaDtos) {
      if (!Utility.isEmpty(materialMediaDto.getMediaId()) && !parameterMediaMappingMap.get(Long.valueOf(materialMediaDto.getMediaId())).isArchived()) {
        materialParameters.add(toMaterialParameter(parameterMediaMappingMap.get(Long.valueOf(materialMediaDto.getMediaId())), materialMediaDto));
      } else {
        MaterialParameter materialParameter = new MaterialParameter();
        materialParameter.setName(materialMediaDto.getName());
        materialParameter.setQuantity(materialMediaDto.getQuantity());
        materialParameter.setId(materialMediaDto.getId());
        materialParameters.add(materialParameter);
      }
    }
    return materialParameters;
  }

  public List<MediaDto> getMedias(List<ParameterValueMediaMapping> parameterValueMediaMappings) {
    List<MediaDto> mediaDtos = new ArrayList<>();
    if (null != parameterValueMediaMappings) {
      for (ParameterValueMediaMapping actMedia : parameterValueMediaMappings) {
        MediaDto mediaDto = toMediaDto(actMedia.getMedia());
        // TODO: this is a workaround, this method is used for audits only do not use this anywhere else without fixing this
        // parameter execution medias get archived in the mapping entity not at the media level
        // to show the right audits we are marking archived true for the medias
        if (actMedia.isArchived()) {
          mediaDto.setArchived(true);
        }
        mediaDtos.add(mediaDto);
      }
    }
    return mediaDtos;
  }

  private Map<Long, CorrectionDto> getCorrectionDtoMap(List<ParameterValue> parameterValues, Parameter parameter) {
    Map<Long, CorrectionListViewProjection> corrections = new HashMap<>();
    Map<Long, List<Media>> oldCorrectionIdMediaListMap = new HashMap<>();
    Map<Long, List<Media>> newCorrectionIdMediaListMap = new HashMap<>();

    for (ParameterValue parameterValue : parameterValues) {
      if (parameterValue.isHasCorrections()) {
        CorrectionListViewProjection correction = correctionRepository.findLatestCorrection(parameterValue.getId());
        corrections.put(parameterValue.getId(), correction);
        List<CorrectionMediaMapping> oldMediaMappingList = new ArrayList<>();
        if (parameter.getType() == Type.Parameter.SIGNATURE) {
          oldMediaMappingList = correctionMediaMappingRepository.findByCorrectionIdAndIsOldMediaAndArchived(Long.parseLong(correction.getId()), true, true);
        } else {
          oldMediaMappingList = correctionMediaMappingRepository.findByCorrectionIdAndIsOldMedia(Long.parseLong(correction.getId()), true);
        }
        List<Media> oldMediaList = oldMediaMappingList.stream()
          .map(CorrectionMediaMapping::getMedia)
          .collect(Collectors.toList());
        oldCorrectionIdMediaListMap.put(parameterValue.getId(), oldMediaList);

        List<CorrectionMediaMapping> newMediaMappingList = new ArrayList<>();
        if (parameter.getType() == Type.Parameter.SIGNATURE) {
          newMediaMappingList = correctionMediaMappingRepository.findByCorrectionIdAndIsOldMediaAndArchived(Long.parseLong(correction.getId()), false, false);
        } else {
          newMediaMappingList = correctionMediaMappingRepository.findByCorrectionIdAndArchived(Long.parseLong(correction.getId()), false);
        }
        List<Media> newMediaList = newMediaMappingList.stream()
          .map(CorrectionMediaMapping::getMedia)
          .collect(Collectors.toList());
        newCorrectionIdMediaListMap.put(parameterValue.getId(), newMediaList);
      }
    }

    Map<Long, CorrectionDto> correctionDtoMap = new HashMap<>();
    for (Map.Entry<Long, CorrectionListViewProjection> entryMap : corrections.entrySet()) {
      Long parameterValueId = entryMap.getKey();
      CorrectionListViewProjection latestCorrection = entryMap.getValue();

      List<Corrector> correctorList = correctorRepository.findByCorrectionId(Long.valueOf(latestCorrection.getId()));
      Map<Long, List<CorrectorDto>> correctorIdMap = correctorList.stream()
        .collect(Collectors.groupingBy(Corrector::getCorrectionId, Collectors.mapping(correctorMapper::toDto, Collectors.toList())));
      List<Reviewer> reviewerList = reviewerRepository.findByCorrectionId(Long.valueOf(latestCorrection.getId()));
      Map<Long, List<ReviewerDto>> reviewerIdMap = reviewerList.stream()
        .collect(Collectors.groupingBy(Reviewer::getCorrectionId, Collectors.mapping(reviewerMapper::toDto, Collectors.toList())));
      CorrectionDto correctionDto = correctionMapper.toDto(latestCorrection, correctorIdMap, reviewerIdMap, oldCorrectionIdMediaListMap.get(parameterValueId), newCorrectionIdMediaListMap.get(parameterValueId));
      correctionDtoMap.put(parameterValueId, correctionDto);
    }
    return correctionDtoMap;
  }

  private Map<Long, List<ParameterExceptionDto>> getParameterExceptionDto(List<ParameterValue> parameterValues) {
    Map<Long, List<ParameterExceptionDto>> exceptions = new HashMap<>();
    for (ParameterValue parameterValue : parameterValues) {
      List<ParameterExceptionDto> exceptionDtos = new ArrayList<>();
      if (parameterValue.isHasExceptions()) {
        List<ParameterException> exceptionList = parameterExceptionRepository.findLatestException(parameterValue.getId());
        for (ParameterException exception : exceptionList) {
          List<ParameterExceptionReviewer> exceptionReviewer = parameterExceptionReviewerRepository.findByExceptionId(exception.getId());
          ParameterExceptionDto exceptionDto = parameterExceptionMapper.toDto(exception);
          List<ParameterExceptionReviewerDto> exceptionReviewerDtos = parameterExceptionReviewerMapper.toDto(exceptionReviewer);
          exceptionDto.setReviewer(exceptionReviewerDtos);
          exceptionDtos.add(exceptionDto);
        }
        exceptions.put(parameterValue.getId(), exceptionDtos);
      }
    }
    return exceptions;
  }
}
