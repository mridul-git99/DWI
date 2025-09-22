package com.leucine.streem.dto.mapper;


import com.leucine.streem.dto.CorrectorDto;
import com.leucine.streem.dto.mapper.helper.IBaseMapper;
import com.leucine.streem.model.Corrector;
import org.mapstruct.Mapper;

@Mapper
public interface ICorrectorMapper extends IBaseMapper<CorrectorDto, Corrector> {
}
