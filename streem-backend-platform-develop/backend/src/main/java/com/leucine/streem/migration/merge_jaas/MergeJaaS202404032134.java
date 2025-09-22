package com.leucine.streem.migration.merge_jaas;

import com.leucine.streem.migration.properties.config.PropertyLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
public class MergeJaaS202404032134 {
  private static final List<String> INSERT_DATA_INTO_TABLES = Arrays.asList("challenge_questions", "client_credentials", "client_facility_mapping", "licenses",
    "organisation_facilities_mapping", "services", "organisation_services_mapping", "organisation_settings", "password_history", "password_policies",
    "permissions", "role_permissions_mapping", "scope_groups", "scopes", "roles", "role_scope_groups_mapping", "tokens",
    "user_audits", "user_facilities_mapping", "user_roles_mapping");

  private static final List<String> UPDATE_DATA_INTO_TABLES = Arrays.asList("users", "facilities", "organisations");

  private DataSource sourceDataSource;
  private DataSource targetDataSource;
  private DataSourceTransactionManager transactionManager;

  public static void main(String[] args) {
    MergeJaaS202404032134 jaaS202404032134 = new MergeJaaS202404032134();
    jaaS202404032134.execute();
  }

  private List<String> getColumnNames(String table) throws Exception {
    try (Connection connection = sourceDataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();
      ResultSet columns = metaData.getColumns(null, null, table, null);
      List<String> columnNames = new ArrayList<>();
      while (columns.next()) {
        columnNames.add(columns.getString("COLUMN_NAME"));
      }
      return columnNames;
    }
  }

  private void insert() throws Exception {
    for (String table : INSERT_DATA_INTO_TABLES) {
      List<String> columnNames = getColumnNames(table);
      String insertQuery = prepareInsertQuery(table, columnNames);
      insertTableData(table, columnNames, insertQuery);
    }
  }

  private String prepareInsertQuery(String table, List<String> columnNames) {
    String columnsPart = String.join(", ", columnNames);
    String valuesPart = String.join(", ", Collections.nCopies(columnNames.size(), "?"));
    return "INSERT INTO " + table + " (" + columnsPart + ") VALUES (" + valuesPart + ")";
  }

  private void insertTableData(String table, List<String> columnNames, String insertQuery) throws Exception {
    try (Connection sourceConnection = sourceDataSource.getConnection();
         PreparedStatement sourceStatement = sourceConnection.prepareStatement("SELECT * FROM " + table);
         ResultSet rs = sourceStatement.executeQuery();
         Connection targetConnection = targetDataSource.getConnection();
         PreparedStatement targetStatement = targetConnection.prepareStatement(insertQuery)) {
      ResultSetMetaData metaData = rs.getMetaData();
      targetConnection.setAutoCommit(false);

      int batchSize = 1000;
      int count = 0;

      while (rs.next()) {

        int index = 1;
        for (String columnName : columnNames) {
          Object value = rs.getObject(columnName);
          targetStatement.setObject(index++, value);
        }
//        for (int i = 1; i < columnNames.size() + 1; i++) {
//          int columnType = metaData.getColumnType(i);
//          switch (columnType) {
//            case Types.INTEGER:
//              targetStatement.setInt(i, rs.getInt(i));
//              break;
//            case Types.BIGINT:
//              targetStatement.setLong(i, rs.getLong(i));
//              break;
//            case Types.VARCHAR:
//              targetStatement.setString(i, rs.getString(i));
//              break;
//            case Types.BOOLEAN:
//              targetStatement.setBoolean(i, rs.getBoolean(i));
//              break;
//            case Types.DATE:
//              targetStatement.setDate(i, rs.getDate(i));
//              break;
//            case Types.OTHER:
//              targetStatement.setObject(i, rs.getObject(i));
//              break;
//            default:
//              targetStatement.setObject(i, rs.getObject(i));
//          }
//        }
        targetStatement.addBatch();
        if (++count % batchSize == 0) {
          targetStatement.executeBatch();
          targetConnection.commit();
        }
      }

      if (count % batchSize != 0) {
        targetStatement.executeBatch();
        targetConnection.commit();
      }
      targetConnection.setAutoCommit(true);
    }
  }

  private void update() throws Exception {
    for (String table : UPDATE_DATA_INTO_TABLES) {
      List<String> columnNames = getColumnNames(table);
      String updateQuery = prepareUpdateQuery(table, columnNames, "id");
      updateTableData(table, columnNames, updateQuery, "id");
    }
  }

  private String prepareUpdateQuery(String table, List<String> columnNames, String uniqueIdentifierColumn) {
    StringBuilder updateQueryBuilder = new StringBuilder("UPDATE ");
    updateQueryBuilder.append(table).append(" SET ");

    int setSize = columnNames.size();
    int currentIndex = 1;
    for (String targetColumn : columnNames) {
      if (!targetColumn.equalsIgnoreCase(uniqueIdentifierColumn)) {
        updateQueryBuilder.append(targetColumn).append(" = ?");
        if (currentIndex < setSize) {
          updateQueryBuilder.append(", ");
        }
      }
      currentIndex++;
    }
    updateQueryBuilder.append(" WHERE ").append(uniqueIdentifierColumn).append(" = ?");
    return updateQueryBuilder.toString();
  }

  private void updateTableData(String table, List<String> columnNames, String updateQueryTemplate, String uniqueIdentifierColumn) throws Exception {
    String selectQuery = "SELECT * FROM " + table;
//    String updateQueryTemplate = prepareUpdateQuery(table, columnNames, updateQuery);

    try (Connection sourceConnection = sourceDataSource.getConnection();
         PreparedStatement sourceStatement = sourceConnection.prepareStatement(selectQuery);
         ResultSet rs = sourceStatement.executeQuery();
         Connection targetConnection = targetDataSource.getConnection();
         PreparedStatement updateStatement = targetConnection.prepareStatement(updateQueryTemplate)) {

      targetConnection.setAutoCommit(false);
      while (rs.next()) {
        int index = 1;
        for (String sourceColumn : columnNames) {
          if (!sourceColumn.equalsIgnoreCase(uniqueIdentifierColumn)) {
            Object value = rs.getObject(sourceColumn);
            updateStatement.setObject(index++, value);
          }
        }
        // Set the identifier for the WHERE clause
        updateStatement.setObject(index, rs.getObject(uniqueIdentifierColumn));

        updateStatement.addBatch();
      }

      updateStatement.executeBatch();
      targetConnection.commit();
      targetConnection.setAutoCommit(true);
    }
  }

  public void execute() {
    initialiseJdbcTemplate();
    DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
    TransactionStatus status = transactionManager.getTransaction(definition);
    try {
      insert();
      update();
      transactionManager.commit(status);
    } catch (Exception e) {
      log.error("Error while migrating job properties", e);
      throw new RuntimeException(e);
    }
  }

  private void initialiseJdbcTemplate() {
    PropertyLoader propertyLoader = new PropertyLoader();
    String driverName = propertyLoader.getProperty("spring.datasource.driver-class-name");
    String database = propertyLoader.getProperty("spring.jpa.database");
    String datasourceHost = propertyLoader.getProperty("datasource.host");
    String datasourcePort = propertyLoader.getProperty("datasource.port");
    String datasourceDatabase = propertyLoader.getProperty("datasource.database");
    String jaasDatasourceDatabase = propertyLoader.getProperty("jaas.datasource.database");
    if(jaasDatasourceDatabase == null) {
      log.error("[initialiseJdbcTemplate] jaas.datasource.database is not set in application.properties");
      System.exit(1);
    }
    String targetURL = "jdbc:%s://%s:%s/%s".formatted(database, datasourceHost, datasourcePort, datasourceDatabase);
    String username = propertyLoader.getProperty("datasource.username");
    String password = propertyLoader.getProperty("datasource.password");
    String sourceURL = "jdbc:%s://%s:%s/%s".formatted(database, datasourceHost, datasourcePort, jaasDatasourceDatabase);

    DriverManagerDataSource targetDriverManagerDataSource = new DriverManagerDataSource();
    targetDriverManagerDataSource.setDriverClassName(driverName);
    targetDriverManagerDataSource.setUrl(targetURL);
    targetDriverManagerDataSource.setUsername(username);
    targetDriverManagerDataSource.setPassword(password);
    JdbcTemplate targetJdbcTemplate = new JdbcTemplate(targetDriverManagerDataSource);
    targetDataSource = targetJdbcTemplate.getDataSource();
    transactionManager = new DataSourceTransactionManager(targetDriverManagerDataSource);

    DriverManagerDataSource sourceDriverManagerDataSource = new DriverManagerDataSource();
    sourceDriverManagerDataSource.setDriverClassName(driverName);
    sourceDriverManagerDataSource.setUrl(sourceURL);
    sourceDriverManagerDataSource.setUsername(username);
    sourceDriverManagerDataSource.setPassword(password);
    JdbcTemplate sourceJdbcTemplate = new JdbcTemplate(sourceDriverManagerDataSource);
    sourceDataSource = sourceJdbcTemplate.getDataSource();
//    transactionManager = new DataSourceTransactionManager(sourceJdbcTemplate);
  }

}
