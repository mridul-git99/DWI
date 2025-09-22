package com.leucine.streem.dto.mapper;

import com.leucine.streem.dto.ReviewerDto;
import com.leucine.streem.dto.mapper.helper.IBaseMapper;
import com.leucine.streem.model.Reviewer;
import org.mapstruct.Mapper;

@Mapper
public interface IReviewerMapper extends IBaseMapper<ReviewerDto, Reviewer> {
}
