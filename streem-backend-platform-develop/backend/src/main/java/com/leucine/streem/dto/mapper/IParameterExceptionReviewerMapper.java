package com.leucine.streem.dto.mapper;

import com.leucine.streem.dto.ParameterExceptionReviewerDto;
import com.leucine.streem.dto.mapper.helper.IBaseMapper;
import com.leucine.streem.model.ParameterExceptionReviewer;
import org.mapstruct.Mapper;

@Mapper
public interface IParameterExceptionReviewerMapper extends IBaseMapper<ParameterExceptionReviewerDto, ParameterExceptionReviewer> {
}
