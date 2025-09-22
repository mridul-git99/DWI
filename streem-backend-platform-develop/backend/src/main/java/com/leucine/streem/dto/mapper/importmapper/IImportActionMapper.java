package com.leucine.streem.dto.mapper.importmapper;

import com.leucine.streem.dto.ActionDto;
import com.leucine.streem.dto.StageDto;
import com.leucine.streem.dto.mapper.helper.IBaseMapper;
import com.leucine.streem.dto.request.ImportActionRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = {IImportEffectMapper.class})
public interface IImportActionMapper extends IBaseMapper<ImportActionRequest, ActionDto> {
  @Mapping(source = "effects", target = "effectRequests")
  ImportActionRequest toDto(ActionDto e);
}
