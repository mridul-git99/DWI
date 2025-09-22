package com.leucine.streem.controller;

import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.JobAnnotationDto;
import com.leucine.streem.dto.request.JobAnnotationDeleteRequest;
import com.leucine.streem.dto.request.JobAnnotationRequest;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/jobs/annotations")
public interface IJobAnnotationController {

  @GetMapping
  Response<Page<JobAnnotationDto>> getJobAnnotationsByJobId(@RequestParam(value = "filters",  required = false) String filters, Pageable pageable) throws ResourceNotFoundException;

  @PostMapping
  Response<JobAnnotationDto> createJobAnnotation(@RequestBody JobAnnotationRequest jobAnnotation) throws ResourceNotFoundException;

  @PatchMapping("/{jobId}")
  @Deprecated
  Response<JobAnnotationDto> updateJobAnnotation(@PathVariable Long jobId, @RequestBody JobAnnotationRequest jobAnnotation) throws ResourceNotFoundException;

  @DeleteMapping("/{jobId}")
  @Deprecated
  Response<BasicDto> deleteJobAnnotation(@PathVariable Long jobId, @RequestBody JobAnnotationDeleteRequest jobAnnotationDeleteRequest) throws ResourceNotFoundException;

}


