package com.leucine.streem.migration.approval;

import com.leucine.streem.constant.Type;
import com.leucine.streem.migration.approval.dto.ExceptionDto;
import com.leucine.streem.migration.approval.dto.ExceptionReviewersDto;
import com.leucine.streem.migration.approval.dto.ParameterValueDto;
import com.leucine.streem.migration.approval.dto.TaskExecutionUserJobDto;
import com.leucine.streem.migration.properties.config.PropertyLoader;
import com.leucine.streem.model.Code;
import com.leucine.streem.repository.mapper.CodeRowMapper;
import com.leucine.streem.util.IdGenerator;
import com.leucine.streem.util.Utility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.ObjectUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ParameterApprovalMigration202405231628 {
  private JdbcTemplate jdbcTemplate;
  private DataSourceTransactionManager transactionManager;
  private static final String CODE_DATE_PATTERN = "MMMyy";
  private static final String CODE_YEAR_PATTERN = "yy";
  private static final String CODE_MONTH_PATTERN = "MM";
  private static final String HYPHEN = "-";

  public void execute() {
    initialiseJdbcTemplate();
    DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
    TransactionStatus status = transactionManager.getTransaction(definition);
    try {
      migratePendingForApprovalParameters();
      transactionManager.commit(status);
    } catch (Exception e) {
      log.error("Error while migrating job properties", e);
      throw new RuntimeException(e);
    }
  }

  private void migratePendingForApprovalParameters() {
    List<ParameterValueDto> parameterValueIdsPendingForApproval = getParameterValueIdsPendingForApproval();
    List<ExceptionDto> exceptionDtos = new ArrayList<>();
    Map<Long, Long> parameterValueIdModifiedByIdMap = new HashMap<>();
    for (ParameterValueDto parameterValueDto : parameterValueIdsPendingForApproval) {
      Long jobId = parameterValueDto.getJobId();
      Long parameterValueId = parameterValueDto.getId();
      ExceptionDto exceptionDto = new ExceptionDto();
      exceptionDto.setId(IdGenerator.getInstance().nextId());
      exceptionDto.setCode(getCode(Type.EntityType.PARAMETER_EXCEPTION, parameterValueDto.getOrganisationId(), Instant.ofEpochSecond(parameterValueDto.getModifiedAt())
        .atZone(ZoneId.systemDefault())
        .toLocalDate()));
      exceptionDto.setValue(parameterValueDto.getValue());
      exceptionDto.setParameterValueId(parameterValueId);
      exceptionDto.setTaskExecutionId(parameterValueDto.getTaskExecutionId());
      exceptionDto.setFacilityId(parameterValueDto.getFacilityId());
      exceptionDto.setStatus("INITIATED");
      exceptionDto.setJobId(jobId);
      exceptionDto.setReviewersReason(null);
      exceptionDto.setInitiatorsReason(parameterValueDto.getReason());
      exceptionDto.setPreviousState(ObjectUtils.isEmpty(parameterValueDto.getValue()) ? "NOT_STARTED" : "EXECUTED");
      exceptionDto.setCreatedBy(parameterValueDto.getModifiedBy());
      exceptionDto.setCreatedAt(parameterValueDto.getModifiedAt());
      exceptionDto.setModifiedBy(parameterValueDto.getModifiedBy());
      exceptionDto.setModifiedAt(parameterValueDto.getModifiedAt());
      parameterValueIdModifiedByIdMap.put(parameterValueId, parameterValueDto.getModifiedBy());
      exceptionDtos.add(exceptionDto);
    }

    List<Object[]> batchArgs = exceptionDtos.stream()
      .map(exceptionDto -> new Object[]{
          exceptionDto.getId(),
          exceptionDto.getCode(),
          exceptionDto.getValue(),
          exceptionDto.getParameterValueId(),
          exceptionDto.getTaskExecutionId(),
          exceptionDto.getFacilityId(),
          exceptionDto.getJobId(),
          exceptionDto.getStatus(),
          exceptionDto.getInitiatorsReason(),
          exceptionDto.getReviewersReason(),
          exceptionDto.getPreviousState(),
          exceptionDto.getCreatedBy(),
          exceptionDto.getCreatedAt(),
          exceptionDto.getModifiedBy(),
          exceptionDto.getModifiedAt()
        }
      ).collect(Collectors.toList());

    jdbcTemplate.batchUpdate(Queries.CREATE_EXCEPTION_ENTRY_FOR_PARAMETER_VALUE, batchArgs);
    String GET_ALL_JOB_ASSIGNEES = Queries.GET_ALL_JOB_ASSIGNEES_WITH_USER_GROUP_USERS_BY_ROLES;
    List<Long> jobIds = exceptionDtos.stream().map(ExceptionDto::getJobId).toList();
    if (!Utility.isEmpty(jobIds)) {
      Set<Long> jobIdsSet = new HashSet<>(jobIds);
      List<Object[]> jobArgs = new ArrayList<>();
      for (Long jobId : jobIdsSet) {
        jobArgs.add(new Object[]{jobId});
      }
      Long[] jobIdsArray = jobIds.toArray(new Long[0]);

      List<TaskExecutionUserJobDto> userIds = jdbcTemplate.query(GET_ALL_JOB_ASSIGNEES, new Object[]{jobIdsArray, jobIdsArray}, (rs, rowNum) -> {
        TaskExecutionUserJobDto taskExecutionUserJobDto = new TaskExecutionUserJobDto();
        taskExecutionUserJobDto.setJobId(rs.getLong("jobs_id"));
        taskExecutionUserJobDto.setUserId(rs.getLong("users_id"));
        return taskExecutionUserJobDto;
      });


      List<ExceptionReviewersDto> exceptionReviewersDtos = new ArrayList<>();
      if (!Utility.isEmpty(userIds)) {
        Map<Long, Set<Long>> jobUserMap = userIds.stream().collect(Collectors.groupingBy(TaskExecutionUserJobDto::getJobId, Collectors.mapping(TaskExecutionUserJobDto::getUserId, Collectors.toSet())));
        for (ExceptionDto exceptionDto : exceptionDtos) {
          Long jobId = exceptionDto.getJobId();
          Set<Long> userIdsForJob = jobUserMap.get(jobId);
          if (!Utility.isEmpty(userIdsForJob)) {
            for (Long userId : userIdsForJob) {
              if (!Objects.equals(userId, exceptionDto.getCreatedBy())) {
                ExceptionReviewersDto exceptionReviewersDto = new ExceptionReviewersDto();
                exceptionReviewersDto.setId(IdGenerator.getInstance().nextId());
                exceptionReviewersDto.setExceptionsId(exceptionDto.getId());
                exceptionReviewersDto.setUsersId(userId);
                exceptionReviewersDto.setCreatedBy(parameterValueIdModifiedByIdMap.get(exceptionDto.getParameterValueId()));
                exceptionReviewersDto.setCreatedAt(exceptionDto.getCreatedAt());
                exceptionReviewersDto.setModifiedBy(parameterValueIdModifiedByIdMap.get(exceptionDto.getParameterValueId()));
                exceptionReviewersDto.setModifiedAt(exceptionDto.getModifiedAt());
                exceptionReviewersDtos.add(exceptionReviewersDto);
              }
            }
          }
        }
        List<Object[]> exceptionReviewersBatchArgs = exceptionReviewersDtos.stream().map(exceptionReviewersDto -> new Object[]{
          exceptionReviewersDto.getId(),
          exceptionReviewersDto.getExceptionsId(),
          exceptionReviewersDto.getUsersId(),
          null,
          exceptionReviewersDto.getCreatedBy(),
          exceptionReviewersDto.getCreatedAt(),
          exceptionReviewersDto.getModifiedBy(),
          exceptionReviewersDto.getModifiedAt()
        }).collect(Collectors.toList());
        jdbcTemplate.batchUpdate(Queries.CREATE_EXCEPTION_REVIEWERS_ENTRY, exceptionReviewersBatchArgs);
      }
    }

    String UPDATE_EXCEPTION_STATUS = Queries.UPDATE_EXCEPTION_STATUS;
    jdbcTemplate.update(UPDATE_EXCEPTION_STATUS);

    String UPDATED_ACTION_PERFORMED = Queries.UPDATED_ACTION_PERFORMED;
    jdbcTemplate.update(UPDATED_ACTION_PERFORMED);

    String UPDATE_HAS_EXCEPTIONS = Queries.UPDATE_HAS_EXCEPTION;
    jdbcTemplate.update(UPDATE_HAS_EXCEPTIONS);

    String UPDATE_PARAMETER_VALUE_STATE_TO_EXECUTED_WITH_ACCEPTED_EXCEPTIONS = Queries.UPDATE_PARAMETER_VALUE_STATE_TO_EXECUTED_WITH_ACCEPTED_EXCEPTIONS;
    jdbcTemplate.update(UPDATE_PARAMETER_VALUE_STATE_TO_EXECUTED_WITH_ACCEPTED_EXCEPTIONS);

    String UPDATE_BLOCKED_JOBS_STATE_TO_IN_PROGRESS = Queries.UPDATE_BLOCKED_JOBS_STATE_TO_IN_PROGRESS;
    jdbcTemplate.update(UPDATE_BLOCKED_JOBS_STATE_TO_IN_PROGRESS);
  }

  private List<ParameterValueDto> getParameterValueIdsPendingForApproval() {
    String query = Queries.GET_ALL_PARAMETER_VALUE_IDS_WITH_PENDING_FOR_APPROVAL;
    return jdbcTemplate.query(query, (rs, rowNum) -> {
      ParameterValueDto parameterValueDto = new ParameterValueDto();
      parameterValueDto.setId(rs.getLong("id"));
      parameterValueDto.setJobId(rs.getLong("jobs_id"));
      parameterValueDto.setValue(rs.getString("value"));
      parameterValueDto.setTaskExecutionId(rs.getLong("task_executions_id"));
      parameterValueDto.setFacilityId(rs.getLong("facilities_id"));
      parameterValueDto.setReason(rs.getString("reason"));
      parameterValueDto.setModifiedAt(rs.getLong("modified_at"));
      parameterValueDto.setModifiedBy(rs.getLong("modified_by"));
      parameterValueDto.setCreatedAt(rs.getLong("created_at"));
      parameterValueDto.setCreatedBy(rs.getLong("created_by"));
      parameterValueDto.setOrganisationId(rs.getLong("organisations_id"));
      return parameterValueDto;
    });
  }

  public String getCode(Type.EntityType entityType, Long organisationsId, LocalDate date) {

    String formattedDate = date.format(DateTimeFormatter.ofPattern(CODE_DATE_PATTERN)).toUpperCase();
    String formattedYear = date.format(DateTimeFormatter.ofPattern(CODE_YEAR_PATTERN)).toUpperCase();
    String formattedMonth = date.format(DateTimeFormatter.ofPattern(CODE_MONTH_PATTERN)).toUpperCase();

    Integer integer = Integer.parseInt(formattedYear + formattedMonth);

    return String.join(HYPHEN, Arrays.asList(entityType.getCode(), formattedDate, getCode(organisationsId, entityType, integer).getCounter().toString()));
  }

  public Code getCode(Long organisationId, Type.EntityType type, Integer clause) {
    return jdbcTemplate.queryForObject(Queries.CREATE_OR_UPDATE_CODE, new Object[]{type.name(), clause, organisationId},
      new CodeRowMapper());
  }

  private void initialiseJdbcTemplate() {
    PropertyLoader propertyLoader = new PropertyLoader();
    String driverName = propertyLoader.getProperty("spring.datasource.driver-class-name");
    String database = propertyLoader.getProperty("spring.jpa.database");
    String datasourceHost = propertyLoader.getProperty("datasource.host");
    String datasourcePort = propertyLoader.getProperty("datasource.port");
    String datasourceDatabase = propertyLoader.getProperty("datasource.database");
    String url = "jdbc:%s://%s:%s/%s".formatted(database, datasourceHost, datasourcePort, datasourceDatabase);
    String username = propertyLoader.getProperty("datasource.username");
    String password = propertyLoader.getProperty("datasource.password");

    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName(driverName);
    dataSource.setUrl(url);
    dataSource.setUsername(username);
    dataSource.setPassword(password);
    jdbcTemplate = new JdbcTemplate(dataSource);
    transactionManager = new DataSourceTransactionManager(dataSource);
  }
}
