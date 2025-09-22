package com.leucine.streem.quartz;

import com.leucine.streem.constant.Misc;
import com.leucine.streem.dto.RoleDto;
import com.leucine.streem.dto.mapper.IUserMapper;
import com.leucine.streem.model.Scheduler;
import com.leucine.streem.model.User;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.repository.ISchedulerRepository;
import com.leucine.streem.repository.IUserRepository;
import com.leucine.streem.service.ICreateJobService;
import com.leucine.streem.util.DateTimeUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

// TODO make this generic, include a factory to call schedulers by its JOB_TYPE maybe ?
@Component
@AllArgsConstructor
@Slf4j
public class ProcessJob extends QuartzJobBean {

  private final ISchedulerRepository schedulerRepository;
  private final ICreateJobService createJobService;
  private final IUserRepository userRepository;
  private final IUserMapper userMapper;

  @Override
  protected void executeInternal(JobExecutionContext context) {
    try {
      log.info("[executeInternal] scheduler trigger request, context: {}, executedAt: {} ", context, LocalDateTime.now());
      User principalUserEntity = userRepository.findById(Long.valueOf(Misc.SYSTEM_USER_ID)).get();
      PrincipalUser principalUser = userMapper.toPrincipalUser(principalUserEntity);
      UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principalUser, null, null);
      SecurityContextHolder.getContext().setAuthentication(authentication);

      JobDetail jobDetail = context.getJobDetail();
      String jobKey = jobDetail.getKey().getName();

      Scheduler scheduler = schedulerRepository.findById(Long.valueOf(jobKey)).get();

      createJobService.createScheduledJob(scheduler.getId(), DateTimeUtils.now());

    } catch (Exception ex) {
      //TODO retry ?
      log.error("[executeInternal] scheduler trigger request error", ex);
    }
  }

}
