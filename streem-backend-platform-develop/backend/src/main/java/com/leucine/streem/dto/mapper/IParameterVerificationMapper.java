package com.leucine.streem.dto.mapper;

import com.leucine.streem.dto.ParameterVerificationDto;
import com.leucine.streem.dto.ParameterVerificationListViewDto;
import com.leucine.streem.dto.mapper.helper.IBaseMapper;
import com.leucine.streem.dto.projection.ParameterVerificationListViewProjection;
import com.leucine.streem.model.ParameterVerification;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper
public abstract class IParameterVerificationMapper implements IBaseMapper<ParameterVerificationDto, ParameterVerification> {

  @Override
  @Mapping(source = "user", target = "requestedTo")
  @Mapping(source = "parameterValue.state", target = "evaluationState")
  @Mapping(source = "parameterValue.id", target = "parameterExecutionId")
  public abstract ParameterVerificationDto toDto(ParameterVerification parameterVerification);

  @Mapping(source = "modifiedById", target = "modifiedBy.id")
  @Mapping(source = "modifiedByEmployeeId", target = "modifiedBy.employeeId")
  @Mapping(source = "modifiedByFirstName", target = "modifiedBy.firstName")
  @Mapping(source = "modifiedByLastName", target = "modifiedBy.lastName")
  @Mapping(source = "createdById", target = "createdBy.id")
  @Mapping(source = "createdByEmployeeId", target = "createdBy.employeeId")
  @Mapping(source = "createdByFirstName", target = "createdBy.firstName")
  @Mapping(source = "createdByLastName", target = "createdBy.lastName")
  @Mapping(source = "requestedToId", target = "requestedTo.id")
  @Mapping(source = "requestedToEmployeeId", target = "requestedTo.employeeId")
  @Mapping(source = "requestedToFirstName", target = "requestedTo.firstName")
  @Mapping(source = "requestedToLastName", target = "requestedTo.lastName")
  @Named("toParameterListViewDto")
  public abstract ParameterVerificationListViewDto toParameterListViewDto(ParameterVerificationListViewProjection parameterVerification);

  @IterableMapping(qualifiedByName = "toParameterListViewDto")
  public abstract List<ParameterVerificationListViewDto> toParameterListViewDto(Iterable<ParameterVerificationListViewProjection> parameterVerification);
}
