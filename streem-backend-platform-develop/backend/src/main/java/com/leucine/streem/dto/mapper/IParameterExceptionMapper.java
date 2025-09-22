package com.leucine.streem.dto.mapper;

import com.leucine.streem.dto.ParameterExceptionDto;
import com.leucine.streem.dto.mapper.helper.IBaseMapper;
import com.leucine.streem.model.ParameterException;
import org.mapstruct.Mapper;

@Mapper
public abstract class IParameterExceptionMapper implements IBaseMapper<ParameterExceptionDto, ParameterException> {
}
