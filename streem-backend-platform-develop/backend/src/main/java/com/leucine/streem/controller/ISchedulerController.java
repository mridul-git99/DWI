package com.leucine.streem.controller;

import com.leucine.streem.dto.*;
import com.leucine.streem.dto.request.CreateProcessSchedulerRequest;
import com.leucine.streem.dto.request.UpdateSchedulerRequest;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.MultiStatusException;
import com.leucine.streem.exception.ParameterExecutionException;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/v1/schedulers")
public interface ISchedulerController {
  @PostMapping
  @ResponseBody
  Response<SchedulerPartialDto> createScheduler(@RequestBody CreateProcessSchedulerRequest createProcessSchedulerRequest) throws Exception;

  @GetMapping
  Response<Page<SchedulerPartialDto>> getAllScheduler(@RequestParam(name = "filters", defaultValue = "") String filters, Pageable pageable);

  @GetMapping("/{schedulerId}")
  Response<SchedulerDto> getScheduler(@PathVariable Long schedulerId) throws ResourceNotFoundException;

  @PatchMapping("/{schedulerId}")
  Response<SchedulerDto> updateScheduler(@PathVariable Long schedulerId, @RequestBody UpdateSchedulerRequest updateSchedulerRequest) throws Exception;

  @GetMapping("/{schedulerId}/info")
  Response<SchedulerInfoDto> getSchedulerInfo(@PathVariable Long schedulerId) throws ResourceNotFoundException;

  @PatchMapping("/{schedulerId}/archive")
  Response<BasicDto> archiveScheduler(@PathVariable Long schedulerId) throws ResourceNotFoundException, StreemException;

  @GetMapping("/calendar")
  Response<List<CalendarEventDto>> getSchedulerCalendar(@RequestParam(name = "startTime") long startTime, @RequestParam(name = "endTime") long endTime, @RequestParam(name = "filters", defaultValue = "") String filters) throws StreemException;

}
