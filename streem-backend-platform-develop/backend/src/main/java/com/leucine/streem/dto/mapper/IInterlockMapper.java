package com.leucine.streem.dto.mapper;

import com.leucine.streem.dto.mapper.helper.IBaseMapper;
import com.leucine.streem.dto.request.InterlockDto;
import com.leucine.streem.model.Interlock;
import org.mapstruct.Mapper;

@Mapper
public interface IInterlockMapper extends IBaseMapper<InterlockDto, Interlock> {

}
