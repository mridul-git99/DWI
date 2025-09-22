package com.leucine.streem.dto.mapper;

import com.leucine.streem.collections.Report;
import com.leucine.streem.dto.ReportDto;
import com.leucine.streem.dto.mapper.helper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper
public interface IReportMapper extends IBaseMapper<ReportDto, Report> {
}
