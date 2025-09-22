package com.leucine.streem.quartz;

import com.leucine.streem.constant.State;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.projection.TrainedUsersView;
import com.leucine.streem.model.Checklist;
import com.leucine.streem.model.Job;
import com.leucine.streem.repository.IJobRepository;
import com.leucine.streem.service.IChecklistService;
import com.leucine.streem.service.INotificationService;
import com.leucine.streem.util.DateTimeUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
@Slf4j
public class JobNotificationEmailDispatch extends QuartzJobBean {
  private final IJobRepository jobRepository;
  private final INotificationService notificationService;
  private final IChecklistService checklistService;

  @Override
  protected void executeInternal(JobExecutionContext context) {
    try {
      log.info("[executeInternal] JobDelayEmailDispatch scheduler trigger request, context: {}, executedAt: {} ", context, DateTimeUtils.now());
      JobDetail jobDetail = context.getJobDetail();
      String jobGroup = jobDetail.getKey().getGroup();
      String jobKey = jobDetail.getKey().getName();
      Job job = jobRepository.findById(Long.valueOf(jobKey)).get();
      if (job.getState().equals(State.Job.ASSIGNED)) {
        Checklist checklist = job.getChecklist();
        List<TrainedUsersView> defaultUserDtoList = checklistService.getTrainedUsersOfFacility(checklist.getId(), job.getFacilityId());
        Set<Long> defaultUserIds = defaultUserDtoList.stream().map(userDto -> Long.valueOf(userDto.getUserId())).collect(Collectors.toSet());
        if (jobGroup.equals(Type.ScheduledJobGroup.JOB_DELAY.name())) {
          notificationService.notifyJobStartDelayed(defaultUserIds, job.getId(), job.getOrganisationId());
        } else if (jobGroup.equals(Type.ScheduledJobGroup.JOB_OVERDUE.name())) {
          notificationService.notifyJobOverDue(defaultUserIds, job.getId(), job.getOrganisationId());
        }
      }
    } catch (Exception ex) {
      log.error("Error while executing JobDelayEmailDispatch trigger request", ex);
      throw new RuntimeException(ex);
    }
  }
}
