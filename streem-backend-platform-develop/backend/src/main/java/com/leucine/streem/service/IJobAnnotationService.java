package com.leucine.streem.service;

import com.leucine.streem.collections.JobLogMediaData;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.JobAnnotationDto;
import com.leucine.streem.dto.request.JobAnnotationDeleteRequest;
import com.leucine.streem.dto.request.JobAnnotationRequest;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.model.Media;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface IJobAnnotationService {

  Page<JobAnnotationDto> getJobAnnotationByJobId(String filters, Pageable pageable) throws ResourceNotFoundException;

  JobAnnotationDto saveJobAnnotation(JobAnnotationRequest jobAnnotationRequest) throws ResourceNotFoundException;

  JobAnnotationDto updateJobAnnotation(Long jobId, JobAnnotationRequest jobAnnotationRequest) throws ResourceNotFoundException;

  BasicDto deleteJobAnnotation(Long jobId, JobAnnotationDeleteRequest jobAnnotationDeleteRequest) throws ResourceNotFoundException;

  List<JobLogMediaData> getJobLogMediaData(List<Media> medias) throws ResourceNotFoundException;
}
