package com.leucine.streem.dto.mapper;

import com.leucine.streem.dto.JobAnnotationDto;
import com.leucine.streem.dto.mapper.helper.IBaseMapper;
import com.leucine.streem.model.JobAnnotation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = {IJobAnnotationMediaMapper.class})
public abstract class IJobAnnotationMapper implements IBaseMapper<JobAnnotationDto, JobAnnotation> {

  @Override
  @Mapping(source = "job.id", target = "jobId")
  @Mapping(source = "createdBy.id", target = "createdBy.id")
  @Mapping(source = "createdBy.employeeId", target = "createdBy.employeeId")
  @Mapping(source = "createdBy.firstName", target = "createdBy.firstName")
  @Mapping(source = "createdBy.lastName", target = "createdBy.lastName")
  @Mapping(source = "modifiedBy.id", target = "modifiedBy.id")
  @Mapping(source = "modifiedBy.employeeId", target = "modifiedBy.employeeId")
  @Mapping(source = "modifiedBy.firstName", target = "modifiedBy.firstName")
  @Mapping(source = "modifiedBy.lastName", target = "modifiedBy.lastName")
  public abstract JobAnnotationDto toDto(JobAnnotation jobAnnotation);

}
