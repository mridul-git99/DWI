package com.leucine.streem.migration.task;

import com.leucine.streem.migration.properties.config.PropertyLoader;
import com.leucine.streem.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@Slf4j
public class TaskExecutionUserMappingIdGeneration202403042316 {
  private JdbcTemplate jdbcTemplate;
  private DataSourceTransactionManager transactionManager;

  public void execute() {
    initialiseJdbcTemplate();
    DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
    TransactionStatus status = transactionManager.getTransaction(definition);
    try {
      createColumnIdForTaskExecutionUserMapping();
      generateTaskExecutionUserMappingId();
      addPrimaryKeyForTaskExecutionUserMapping();
      transactionManager.commit(status);
    } catch (Exception e) {
      log.error("Error while migrating job properties", e);
      throw new RuntimeException(e);
    }
  }

  private void addPrimaryKeyForTaskExecutionUserMapping() {
    String query = "ALTER TABLE task_execution_user_mapping ADD PRIMARY KEY (id)";
    jdbcTemplate.execute(query);
  }

  private void generateTaskExecutionUserMappingId() {
    String query = "SELECT task_executions_id, users_id FROM task_execution_user_mapping";

    jdbcTemplate.query(query, (rs, rowNum) -> {
      long taskExecutionsId = rs.getLong("task_executions_id");
      long usersId = rs.getLong("users_id");

      long newId = IdGenerator.getInstance().generateUnique();

      String updateQuery = "UPDATE task_execution_user_mapping SET id = ? WHERE task_executions_id = ? AND users_id = ?";
      jdbcTemplate.update(updateQuery, newId, taskExecutionsId, usersId);

      return null;
    });
  }


  private void createColumnIdForTaskExecutionUserMapping() {
    String query = "ALTER TABLE task_execution_user_mapping ADD COLUMN id BIGINT";
    jdbcTemplate.execute(query);
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
