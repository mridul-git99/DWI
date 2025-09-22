package com.leucine.streem.service;

import com.leucine.streem.dto.*;
import com.leucine.streem.dto.request.CreateProcessSchedulerRequest;
import com.leucine.streem.dto.request.UpdateSchedulerRequest;
import com.leucine.streem.exception.MultiStatusException;
import com.leucine.streem.exception.ParameterExecutionException;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;

public interface ISchedulerService {
  SchedulerPartialDto createScheduler(CreateProcessSchedulerRequest createProcessSchedulerRequest) throws ResourceNotFoundException, StreemException, IOException, MultiStatusException;

  Page<SchedulerPartialDto> getAllScheduler(String filters, Pageable pageable);

  SchedulerDto getScheduler(Long schedulerId) throws ResourceNotFoundException;

  SchedulerDto updateScheduler(Long schedulerId, UpdateSchedulerRequest updateSchedulerRequest) throws ResourceNotFoundException, StreemException, IOException, MultiStatusException;

  SchedulerInfoDto getSchedulerInfo(Long schedulerId) throws ResourceNotFoundException;

  BasicDto archiveScheduler(Long schedulerId) throws ResourceNotFoundException, StreemException;

  void findAndDeprecateSchedulersForChecklist(Long checklistId, User user) throws StreemException;

  List<CalendarEventDto> getSchedulerCalendar(long startTime, long endTime, String filters) throws StreemException;

}
