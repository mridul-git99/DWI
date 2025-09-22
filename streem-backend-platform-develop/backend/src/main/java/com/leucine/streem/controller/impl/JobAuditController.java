package com.leucine.streem.controller.impl;

import com.leucine.streem.controller.IJobAuditController;
import com.leucine.streem.dto.JobAuditDto;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.service.IJobAuditService;
import com.leucine.streem.util.DateTimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JobAuditController implements IJobAuditController {
  private final IJobAuditService jobAuditService;

  @Autowired
  public JobAuditController(IJobAuditService jobAuditService) {
    this.jobAuditService = jobAuditService;
  }

  @Override
  public Response<Page<JobAuditDto>> getAuditsByJobId(Long jobId, String filters, Pageable pageable) throws StreemException {
    return Response.builder().data(jobAuditService.getAuditsByJobId(jobId, filters, pageable)).build();
  }


}
