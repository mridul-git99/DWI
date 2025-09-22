package com.leucine.streem.dto.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.config.MediaConfig;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.mapper.helper.IBaseMapper;
import com.leucine.streem.dto.projection.CorrectionListViewProjection;
import com.leucine.streem.model.Correction;
import com.leucine.streem.model.Media;
import com.leucine.streem.util.JsonUtils;
import com.leucine.streem.util.Utility;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper
public abstract class ICorrectionMapper implements IBaseMapper<CorrectionDto, Correction> {
  @Autowired
  protected MediaConfig mediaConfig;

  protected ICorrectionMapper() {
  }

  public List<CorrectionDto> toDtoList(Map<Long, CorrectionListViewProjection> correctionListViewProjectionMap, Map<Long, List<CorrectorDto>> correctionIdCorrectorListMap, Map<Long, List<ReviewerDto>> correctionIdReviewerListMap, Map<Long, List<Media>> oldCorrectionIdMediaListMap, Map<Long, List<Media>> newCorrectionIdMediaListMap) throws JsonProcessingException {
    List<CorrectionDto> correctionDtoList = new ArrayList<>();
    for (CorrectionListViewProjection correctionListViewProjection : correctionListViewProjectionMap.values()) {
      CorrectionDto correctionDto = new CorrectionDto();
      correctionDto.setId(correctionListViewProjection.getId());
      correctionDto.setStatus(correctionListViewProjection.getStatus());
      correctionDto.setInitiatorsReason(correctionListViewProjection.getInitiatorsReason());
      correctionDto.setCorrectorsReason(correctionListViewProjection.getCorrectorsReason());
      correctionDto.setReviewersReason(correctionListViewProjection.getReviewersReason());
      correctionDto.setCorrector(correctionIdCorrectorListMap.get(Long.parseLong(correctionDto.getId())));
      correctionDto.setReviewer(correctionIdReviewerListMap.get(Long.parseLong(correctionDto.getId())));
      correctionDto.setOldValue(correctionListViewProjection.getOldValue());
      correctionDto.setNewValue(correctionListViewProjection.getNewValue());
      String oldChoicesNode = correctionListViewProjectionMap.get(Long.parseLong(correctionDto.getId())).getOldChoices();
      String newChoicesNode = correctionListViewProjectionMap.get(Long.parseLong(correctionDto.getId())).getNewChoices();

      if (!Utility.isEmpty(oldChoicesNode)) {
        correctionDto.setOldChoices(JsonUtils.valueToNode(oldChoicesNode));
      }
      if (!Utility.isEmpty(newChoicesNode)) {
        correctionDto.setNewChoices(JsonUtils.valueToNode(newChoicesNode));
      }

      correctionDto.setCode(correctionListViewProjection.getCode());
      correctionDto.setCreatedAt(correctionListViewProjection.getCreatedAt());

      UserAuditDto createdByUser = new UserAuditDto();
      createdByUser.setId(correctionListViewProjection.getInitiatorUserId());
      createdByUser.setEmployeeId(correctionListViewProjection.getInitiatorEmployeeId());
      createdByUser.setFirstName(correctionListViewProjection.getInitiatorFirstName());
      createdByUser.setLastName(correctionListViewProjection.getInitiatorLastName());

      correctionDto.setCreatedBy(createdByUser);
      correctionDto.setJobId(correctionListViewProjection.getJobId());
      correctionDto.setTaskExecutionId(correctionListViewProjection.getTaskExecutionId());
      correctionDto.setProcessName(correctionListViewProjection.getProcessName());
      correctionDto.setParameterName(correctionListViewProjection.getParameterName());
      correctionDto.setJobCode(correctionListViewProjection.getJobCode());
      correctionDto.setTaskName(correctionListViewProjection.getTaskName());
      correctionDto.setParameterId(correctionListViewProjection.getParameterId());
      correctionDto.setOldMedias(toMediaDtoList(oldCorrectionIdMediaListMap.get(Long.valueOf(correctionDto.getId()))));
      correctionDto.setNewMedias(toMediaDtoList(newCorrectionIdMediaListMap.get(Long.valueOf(correctionDto.getId()))));
      correctionDtoList.add(correctionDto);
    }
    return correctionDtoList;
  }

  @AfterMapping
  void populateCreatedBy(@MappingTarget CorrectionDto dto, CorrectionListViewProjection projection) {
    dto.setCreatedBy(new UserAuditDto().setFirstName(projection.getInitiatorFirstName()).setLastName(projection.getInitiatorLastName()).setEmployeeId(projection.getInitiatorEmployeeId()).setId(projection.getId()));
  }

  public CorrectionDto toDto(CorrectionListViewProjection correctionListViewProjection, Map<Long, List<CorrectorDto>> correctionIdCorrectorListMap, Map<Long, List<ReviewerDto>> correctionIdReviewerListMap,List<Media> oldMediaList, List<Media> newMediaList) {
    CorrectionDto correctionDto = new CorrectionDto();
    correctionDto.setId(correctionListViewProjection.getId());
    correctionDto.setStatus(correctionListViewProjection.getStatus());
    correctionDto.setInitiatorsReason(correctionListViewProjection.getInitiatorsReason());
    correctionDto.setCorrectorsReason(correctionListViewProjection.getCorrectorsReason());
    correctionDto.setReviewersReason(correctionListViewProjection.getReviewersReason());
    correctionDto.setCorrector(correctionIdCorrectorListMap.get(Long.parseLong(correctionDto.getId())));
    correctionDto.setReviewer(correctionIdReviewerListMap.get(Long.parseLong(correctionDto.getId())));
    correctionDto.setOldValue(correctionListViewProjection.getOldValue());
    correctionDto.setNewValue(correctionListViewProjection.getNewValue());
    String oldChoicesNode = correctionListViewProjection.getOldChoices();
    String newChoicesNode = correctionListViewProjection.getNewChoices();

    if (!Utility.isEmpty(oldChoicesNode)) {
      try {
        correctionDto.setOldChoices(JsonUtils.valueToNode(oldChoicesNode));
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }
    if (!Utility.isEmpty(newChoicesNode)) {
      try {
        correctionDto.setNewChoices(JsonUtils.valueToNode(newChoicesNode));
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    correctionDto.setCode(correctionListViewProjection.getCode());
    correctionDto.setCreatedAt(correctionListViewProjection.getCreatedAt());

    UserAuditDto createdByUser = new UserAuditDto();
    createdByUser.setId(correctionListViewProjection.getInitiatorUserId());
    createdByUser.setEmployeeId(correctionListViewProjection.getInitiatorEmployeeId());
    createdByUser.setFirstName(correctionListViewProjection.getInitiatorFirstName());
    createdByUser.setLastName(correctionListViewProjection.getInitiatorLastName());

    correctionDto.setCreatedBy(createdByUser);
    correctionDto.setJobId(correctionListViewProjection.getJobId());
    correctionDto.setTaskExecutionId(correctionListViewProjection.getTaskExecutionId());
    correctionDto.setProcessName(correctionListViewProjection.getProcessName());
    correctionDto.setParameterName(correctionListViewProjection.getParameterName());
    correctionDto.setJobCode(correctionListViewProjection.getJobCode());
    correctionDto.setTaskName(correctionListViewProjection.getTaskName());
    correctionDto.setParameterId(correctionListViewProjection.getParameterId());
    correctionDto.setOldMedias(toMediaDtoList(oldMediaList));
    correctionDto.setNewMedias(toMediaDtoList(newMediaList));

    return correctionDto;
  }

  private MediaDto toMediaDto(Media media) {
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

  private List<MediaDto> toMediaDtoList(List<Media> medias) {
    if (medias == null) {
      return Collections.emptyList();
    }
    return medias.stream().map(this::toMediaDto).collect(Collectors.toList());
  }


}
