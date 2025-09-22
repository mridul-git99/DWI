package com.leucine.streem.migration.trained.users;

import com.leucine.streem.migration.properties.config.PropertyLoader;
import com.leucine.streem.migration.trained.users.dto.TrainedUserDto;
import com.leucine.streem.util.DateTimeUtils;
import com.leucine.streem.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.List;

@Slf4j
public class TrainedUserMappingIdGeneration202403092317 {
  private JdbcTemplate jdbcTemplate;
  private DataSourceTransactionManager transactionManager;

  public void execute() {
    initialiseJdbcTemplate();
    DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
    TransactionStatus status = transactionManager.getTransaction(definition);
    try {
      migrateFromDefaultUsersTableToTrainedUsersTable();
      addColumnTrainedUsersIdToChecklistDefaultUsers();
      transactionManager.commit(status);
    } catch (Exception e) {
      log.error("Error while migrating job properties", e);
      throw new RuntimeException(e);
    }
  }

  private void addColumnTrainedUsersIdToChecklistDefaultUsers() {
    String addColumnTrainedUsersIdToChecklistDefaultUsers = """
      ALTER TABLE checklist_default_users
      ADD COLUMN trained_users_id bigint
      """;
    jdbcTemplate.execute(addColumnTrainedUsersIdToChecklistDefaultUsers);

    String updateTrainedUsersIdInChecklistDefaultUsers = """
      UPDATE checklist_default_users cdu
      SET trained_users_id = (SELECT tu.id FROM trained_users tu WHERE tu.checklists_id = cdu.checklists_id AND tu.facilities_id = cdu.facilities_id AND tu.users_id = cdu.users_id)
      """;
    jdbcTemplate.execute(updateTrainedUsersIdInChecklistDefaultUsers);

    String addForeignKeyConstraint = """
      ALTER TABLE checklist_default_users
      ADD CONSTRAINT fk_trained_users_id
      FOREIGN KEY (trained_users_id)
      REFERENCES trained_users(id) ON DELETE CASCADE
      """;

    jdbcTemplate.execute(addForeignKeyConstraint);

    String addNotNullConstraint = """
      ALTER TABLE checklist_default_users
      ALTER COLUMN trained_users_id SET NOT NULL
      """;
    jdbcTemplate.execute(addNotNullConstraint);
  }

  private void migrateFromDefaultUsersTableToTrainedUsersTable() {
    String getAllDistinctChecklistDefaultUsers = """
      SELECT DISTINCT checklists_id, facilities_id, users_id
      FROM checklist_default_users
      """;
    List<TrainedUserDto> trainedUserMappings = jdbcTemplate.query(getAllDistinctChecklistDefaultUsers, (rs, rowNum) -> {
      TrainedUserDto trainedUserDto = new TrainedUserDto();
      trainedUserDto.setChecklistId(rs.getLong("checklists_id"));
      trainedUserDto.setFacilityId(rs.getLong("facilities_id"));
      trainedUserDto.setUserId(rs.getLong("users_id"));
      return trainedUserDto;
    });

    String insertIntoTrainedUsers = """
      INSERT INTO trained_users (id, users_id, checklists_id, facilities_id, created_at, modified_at, created_by, modified_by)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
      """;

    for (TrainedUserDto trainedUserDto : trainedUserMappings) {
      jdbcTemplate.update(insertIntoTrainedUsers, IdGenerator.getInstance().generateUnique(), trainedUserDto.getUserId(), trainedUserDto.getChecklistId(), trainedUserDto.getFacilityId(), DateTimeUtils.now(), DateTimeUtils.now(), 1, 1);
    }

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
