package com.leucine.streem.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.response.Response;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/migrations")
public interface IMigrationController {

  @PatchMapping("/jobLogs")
  @ResponseBody
  Response<BasicDto> jobLogs() throws Exception;


  @PatchMapping("/autoInitialiseAndRelationFilter")
  @ResponseBody
  Response<BasicDto> autoInitialiseAndRelationFilter() throws Exception;

  @PatchMapping("/rules")
  Response<BasicDto> rules() throws JsonProcessingException;

  @PatchMapping("/addStopDependency")
  Response<BasicDto> addStopDependency() throws JsonProcessingException;

  @PatchMapping("rules/fix/{checklistId}")
  Response<BasicDto> fixRules( @PathVariable String checklistId) throws JsonProcessingException;

  @PatchMapping("/fixObjects")
  Response<BasicDto> fixObjects() throws JsonProcessingException;

  @PatchMapping("/createIndex")
  @ResponseBody
  Response<BasicDto> createIndex() throws Exception;

  @PatchMapping("/rebuild-object-searchable")
  Response<BasicDto> enableSearchable();

  @PatchMapping("/fix-job-logs-annotations")
  Response<BasicDto> fixJobLogsAnnotations();
}
