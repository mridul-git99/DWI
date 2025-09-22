package com.leucine.streem.controller.impl;

import com.leucine.streem.controller.ISchedulerController;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.request.CreateProcessSchedulerRequest;
import com.leucine.streem.dto.request.UpdateSchedulerRequest;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.MultiStatusException;
import com.leucine.streem.exception.ParameterExecutionException;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.service.ISchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class SchedulerController implements ISchedulerController {
  private final ISchedulerService schedulerService;

  @Autowired
  public SchedulerController(ISchedulerService schedulerService) {
    this.schedulerService = schedulerService;
  }

  @Override
  public Response<SchedulerPartialDto> createScheduler(CreateProcessSchedulerRequest createProcessSchedulerRequest) throws StreemException, IOException, ResourceNotFoundException, MultiStatusException {
    return Response.builder().data(schedulerService.createScheduler(createProcessSchedulerRequest)).build();
  }

  @Override
  public Response<Page<SchedulerPartialDto>> getAllScheduler(String filters, Pageable pageable) {
    return Response.builder().data(schedulerService.getAllScheduler(filters, pageable)).build();
  }

  @Override
  public Response<SchedulerDto> getScheduler(Long schedulerId) throws ResourceNotFoundException {
    return Response.builder().data(schedulerService.getScheduler(schedulerId)).build();
  }

  @Override
  public Response<SchedulerDto> updateScheduler(Long schedulerId, UpdateSchedulerRequest updateSchedulerRequest) throws StreemException, IOException, ResourceNotFoundException, MultiStatusException {
    return Response.builder().data(schedulerService.updateScheduler(schedulerId, updateSchedulerRequest)).build();
  }

  @Override
  public Response<SchedulerInfoDto> getSchedulerInfo(Long schedulerId) throws ResourceNotFoundException {
    return Response.builder().data(schedulerService.getSchedulerInfo(schedulerId)).build();
  }

  @Override
  public Response<BasicDto> archiveScheduler(Long schedulerId) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(schedulerService.archiveScheduler(schedulerId)).build();
  }

  @Override
  public Response<List<CalendarEventDto>> getSchedulerCalendar(long startTime, long endTime, String filters) throws StreemException {
    return Response.builder().data(schedulerService.getSchedulerCalendar(startTime, endTime, filters)).build();
  }


}
