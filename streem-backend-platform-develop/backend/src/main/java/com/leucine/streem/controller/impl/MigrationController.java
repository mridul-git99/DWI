package com.leucine.streem.controller.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.controller.IMigrationController;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.migration.*;
import com.leucine.streem.service.IEntityObjectService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class MigrationController implements IMigrationController {
  private final AutoInitialiseAndRelationFilter autoInitialiseAndRelationFilter;
  private final JobLogs jobLogs;
  private final Rules rules;
  private final AddStopDependency addStopDependency;
  private final ObjectProperty objectProperty;
  private final createIndex createIndex;
  private final IEntityObjectService entityObjectService;
  private final JobLogsAnnotations jobLogsAnnotations;

  @Override
  public Response<BasicDto> jobLogs() throws Exception {
    return Response.builder().data(jobLogs.execute()).build();
  }

  @Override
  public Response<BasicDto> autoInitialiseAndRelationFilter() throws Exception {
    return Response.builder().data(autoInitialiseAndRelationFilter.execute()).build();
  }

  @Override
  public Response<BasicDto> rules() {
    return Response.builder().data(rules.execute()).build();
  }

  @Override
  public Response<BasicDto> fixRules(String checklistId) {
    return Response.builder().data(rules.fixChecklistId(checklistId)).build();
  }

  @Override
  public Response<BasicDto> addStopDependency() {
    return Response.builder().data(addStopDependency.execute()).build();
  }

  @Override
  public Response<BasicDto> fixObjects() throws JsonProcessingException {
    return Response.builder().data(objectProperty.fixObjects()).build();
  }

  @Override
  public Response<BasicDto> enableSearchable() {
    return Response.builder().data(entityObjectService.enableSearchable()).build();
  }

  @Override
  public Response<BasicDto> createIndex() throws Exception {
    return Response.builder().data(createIndex.execute()).build();
  }

  @Override
  public Response<com.leucine.streem.dto.BasicDto> fixJobLogsAnnotations() {
    return Response.builder().data(jobLogsAnnotations.execute()).build();
  }
}
