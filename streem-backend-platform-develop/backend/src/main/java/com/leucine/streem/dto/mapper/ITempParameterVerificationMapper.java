package com.leucine.streem.dto.mapper;

import com.leucine.streem.dto.ParameterVerificationDto;
import com.leucine.streem.dto.mapper.helper.IBaseMapper;
import com.leucine.streem.model.TempParameterVerification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public abstract class ITempParameterVerificationMapper implements IBaseMapper<ParameterVerificationDto, TempParameterVerification> {
  @Override
  @Mapping(source = "user", target = "requestedTo")
  @Mapping(source = "tempParameterValue.state", target = "evaluationState")
  public abstract ParameterVerificationDto toDto(TempParameterVerification tempParameterVerification);

}
