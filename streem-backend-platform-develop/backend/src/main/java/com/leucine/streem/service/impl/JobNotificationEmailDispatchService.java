package com.leucine.streem.service.impl;

import com.leucine.streem.constant.ErrorMessage;
import com.leucine.streem.constant.SchedulerMisc;
import com.leucine.streem.constant.Type;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.Job;
import com.leucine.streem.quartz.JobNotificationEmailDispatch;
import com.leucine.streem.service.IJobNotificationEmailDispatchService;
import com.leucine.streem.util.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@RequiredArgsConstructor
@Service
@Slf4j
public class JobNotificationEmailDispatchService implements IJobNotificationEmailDispatchService {

  private final Scheduler scheduler;

  @Override
  public void addJobDelayEmailDispatchEvent(Job job) throws StreemException {
    //TODO: Validation : since job we are fetching from createJob Request, no need of Date Validations
    log.info("[addJobDelayEmailDispatchEvent] Request to create a new event, jobId: {}", job.getIdAsString());
    try {
      JobDetail jobDetail = buildJobDelayDispatchScheduler(job.getIdAsString(), Type.ScheduledJobType.JOB_DELAY_EMAIL_DISPATCH, Type.ScheduledJobGroup.JOB_DELAY);
      Trigger trigger = buildJobDelayDispatchTrigger(job.getExpectedStartDate(), jobDetail, Type.ScheduledJobGroup.JOB_DELAY);
      if (!this.scheduler.checkExists(JobKey.jobKey(job.getIdAsString(), Type.ScheduledJobGroup.JOB_DELAY.name()))) {
        this.scheduler.scheduleJob(jobDetail, trigger);
      }
    } catch (Exception ex) {
      log.error(ErrorMessage.ERROR_CREATING_A_JOB_DELAY_EMAIL_DISPATCHER_EVENT, ex);
      throw new StreemException(ErrorMessage.ERROR_CREATING_A_JOB_DELAY_EMAIL_DISPATCHER_EVENT, ex);
    }
  }

  @Override
  public void addJobOverDueEmailDispatchEvent(Job job) throws StreemException {
    log.info("[addJobOverDueEmailDispatchEvent] Request to create a new event, jobId: {}", job.getIdAsString());
    try {
      JobDetail jobDetail = buildJobDelayDispatchScheduler(job.getIdAsString(), Type.ScheduledJobType.JOB_OVERDUE_EMAIL_DISPATCH, Type.ScheduledJobGroup.JOB_OVERDUE);
      Trigger trigger = buildJobDelayDispatchTrigger(job.getExpectedEndDate(), jobDetail, Type.ScheduledJobGroup.JOB_OVERDUE);
      if (!this.scheduler.checkExists(JobKey.jobKey(job.getIdAsString(), Type.ScheduledJobGroup.JOB_OVERDUE.name()))) {
        this.scheduler.scheduleJob(jobDetail, trigger);
      }
    } catch (Exception ex) {
      log.error(ErrorMessage.ERROR_CREATING_A_JOB_OVERDUE_EMAIL_DISPATCHER_EVENT, ex);
      throw new StreemException(ErrorMessage.ERROR_CREATING_A_JOB_OVERDUE_EMAIL_DISPATCHER_EVENT, ex);
    }
  }


  private JobDetail buildJobDelayDispatchScheduler(String identity, Type.ScheduledJobType jobType, Type.ScheduledJobGroup jobGroup) {
    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put(SchedulerMisc.JOB_TYPE, jobType.name());
    return JobBuilder.newJob(JobNotificationEmailDispatch.class).withIdentity(identity, jobGroup.name()).usingJobData(jobDataMap).storeDurably().build();
  }

  private Trigger buildJobDelayDispatchTrigger(Long startAt, JobDetail jobDetail, Type.ScheduledJobGroup jobGroup) {
    LocalDateTime startTimeDate = DateTimeUtils.getLocalDateTime(startAt);
    Instant instant = startTimeDate.toInstant(ZoneOffset.UTC);
    return TriggerBuilder.newTrigger()
      .forJob(jobDetail)
      .withIdentity(jobDetail.getKey().getName(), jobGroup.name())
      .startAt(Date.from(instant))
      .build();
  }

}
