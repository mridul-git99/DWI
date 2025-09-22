package com.leucine.streem.controller.impl;

import com.leucine.streem.controller.IJobAnnotationController;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.JobAnnotationDto;
import com.leucine.streem.dto.request.JobAnnotationDeleteRequest;
import com.leucine.streem.dto.request.JobAnnotationRequest;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.service.IJobAnnotationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class JobAnnotationController implements IJobAnnotationController {

  @Autowired
  private IJobAnnotationService jobAnnotationService;


  @Override
  public Response<Page<JobAnnotationDto>> getJobAnnotationsByJobId(String filters, Pageable pageable) throws ResourceNotFoundException {
    return Response.builder().data(jobAnnotationService.getJobAnnotationByJobId(filters, pageable)).build();
  }

  @Override
  public Response<JobAnnotationDto> createJobAnnotation(JobAnnotationRequest jobAnnotation) throws ResourceNotFoundException {
    return Response.builder().data(jobAnnotationService.saveJobAnnotation(jobAnnotation)).build();
  }


  @Override
  public Response<JobAnnotationDto> updateJobAnnotation(Long jobId, JobAnnotationRequest jobAnnotation) throws ResourceNotFoundException {
    return Response.builder().data(jobAnnotationService.updateJobAnnotation(jobId, jobAnnotation)).build();
  }

  @Override
  public Response<BasicDto> deleteJobAnnotation(Long jobId, JobAnnotationDeleteRequest jobAnnotationDeleteRequest) throws ResourceNotFoundException {
    return Response.builder().data(jobAnnotationService.deleteJobAnnotation(jobId, jobAnnotationDeleteRequest)).build();
  }

}
