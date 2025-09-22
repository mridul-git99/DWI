package com.leucine.streem.migration.timer;

import com.leucine.streem.constant.TaskPauseReason;
import com.leucine.streem.migration.properties.Queries;
import com.leucine.streem.migration.properties.config.PropertyLoader;
import com.leucine.streem.migration.timer.dto.TaskExecutionDto;
import com.leucine.streem.util.IdGenerator;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.ArrayList;
import java.util.List;


@Slf4j
public class TaskTimerChangeSet implements CustomTaskChange {
  private JdbcTemplate jdbcTemplate;
  private DataSourceTransactionManager transactionManager;

  @Override
  public String getConfirmationMessage() {
    return "Task execution timers have been migrated successfully.";
  }

  @Override
  public void setUp() {
  }

  @Override
  public void setFileOpener(ResourceAccessor resourceAccessor) {
  }

  @Override
  public ValidationErrors validate(Database database) {
    return null;
  }

  @Override
  public void execute(Database database) throws CustomChangeException {
    try {
      initializeJdbcTemplate();
      DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
      TransactionStatus status = transactionManager.getTransaction(definition);
      try {
        migrateTaskTimers();
        transactionManager.commit(status);
      } catch (Exception e) {
        transactionManager.rollback(status);
        throw new RuntimeException("Failed to migrate task execution timers", e);
      }
    } catch (Exception e) {
      throw new CustomChangeException("Error during task timer migration", e);
    }
  }

  private void initializeJdbcTemplate() {
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

  private void migrateTaskTimers() {
    String getCompletedTaskExecutionsNotInTimersTable = Queries.GET_COMPLETED_TASK_EXECUTIONS_NOT_IN_TIMERS_TABLE;

    List<TaskExecutionDto> taskExecutionDtos = jdbcTemplate.query(getCompletedTaskExecutionsNotInTimersTable, (rs, rowNum) -> {
      TaskExecutionDto dto = new TaskExecutionDto();
      dto.setId(rs.getLong("id"));
      dto.setEndedAt(rs.getLong("ended_at"));
      dto.setEndedBy(rs.getLong("ended_by"));
      return dto;
    });

    // Prepare batch arguments for the insert query
    List<Object[]> batchArgs = new ArrayList<>();
    for (TaskExecutionDto taskExecutionDto : taskExecutionDtos) {
      long id = IdGenerator.getInstance().nextId();
      long createdAt = taskExecutionDto.getEndedAt();
      long modifiedAt = taskExecutionDto.getEndedAt();
      long createdBy = taskExecutionDto.getEndedBy();
      long modifiedBy = taskExecutionDto.getEndedBy();

      Object[] args = new Object[]{
        id,
        taskExecutionDto.getId(),
        taskExecutionDto.getEndedAt(),
        null, // paused_at is null
        TaskPauseReason.TASK_COMPLETED.toString(), // reason
        null, // comment is null
        createdBy,
        createdAt,
        modifiedBy,
        modifiedAt
      };

      batchArgs.add(args);
    }

    // Perform the batch insert
    if (!batchArgs.isEmpty()) {
      String insertQuery = "INSERT INTO task_execution_timers (id, task_executions_id, paused_at, resumed_at, reason, comment, created_by, created_at, modified_by, modified_at) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
      try {
        jdbcTemplate.batchUpdate(insertQuery, batchArgs);
      } catch (Exception e) {
        log.info("Error during batch update: {}", e.getMessage());
      }
    } else {
      log.info("No entries to insert.");
    }

  }
}
