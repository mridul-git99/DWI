package com.leucine.streem.dto.mapper;

import com.leucine.streem.config.MediaConfig;
import com.leucine.streem.dto.MediaDto;
import com.leucine.streem.dto.mapper.helper.IBaseMapper;
import com.leucine.streem.model.TaskMediaMapping;
import com.leucine.streem.repository.ITaskMediaMappingRepository;
import com.leucine.streem.util.Utility;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper
public abstract class ITaskMediaMapper implements IBaseMapper<MediaDto, TaskMediaMapping> {
  @Autowired
  protected MediaConfig mediaConfig;

  @Autowired
  private ITaskMediaMappingRepository taskMediaMappingRepository;

  @Override
  public MediaDto toDto(TaskMediaMapping tmm) {
    if (Utility.isEmpty(tmm)) {
      return null;
    }

    TaskMediaMapping taskMediaMapping = taskMediaMappingRepository.findByIdWithMedia(tmm.getTaskMediaId().getTaskId(), tmm.getTaskMediaId().getMediaId());

    if (Utility.isEmpty(taskMediaMapping) || Utility.isEmpty(taskMediaMapping.getMedia())) {
      return null;
    }

    MediaDto dto = new MediaDto();
    dto.setId(String.valueOf(taskMediaMapping.getMedia().getId()));
    dto.setName(taskMediaMapping.getMedia().getName());
    dto.setFilename(taskMediaMapping.getMedia().getFilename());
    dto.setOriginalFilename(taskMediaMapping.getMedia().getOriginalFilename());
    dto.setDescription(taskMediaMapping.getMedia().getDescription());
    dto.setLink(mediaConfig.getCdn() + java.io.File.separator +
      taskMediaMapping.getMedia().getRelativePath() + java.io.File.separator +
      taskMediaMapping.getMedia().getFilename());
    dto.setType(taskMediaMapping.getMedia().getType());
    dto.setArchived(taskMediaMapping.getMedia().isArchived());
    return dto;
  }
}
