package com.leucine.streem.repository.impl;

import com.leucine.streem.model.TaskExecutionUserMapping;
import com.leucine.streem.util.DateTimeUtils;
import com.leucine.streem.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
@Slf4j
public class TaskExecutionUserMappingRepositoryImpl {
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  public TaskExecutionUserMappingRepositoryImpl(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Bulk insert task execution user mappings using JDBC batch updates for optimal performance.
   * @param taskExecutionUserMappings List of task execution user mappings to insert
   */
  @Transactional
  public void bulkInsertTaskExecutionUserMappings(List<TaskExecutionUserMapping> taskExecutionUserMappings) {
    log.info("Bulk inserting {} task execution user mappings", taskExecutionUserMappings.size());

    if (taskExecutionUserMappings.isEmpty()) return;

    // Generate IDs for task execution user mappings if needed
    for (TaskExecutionUserMapping mapping : taskExecutionUserMappings) {
      if (mapping.getId() == null) {
        mapping.setId(IdGenerator.getInstance().nextId());
      }
    }

    final String sql = "INSERT INTO task_execution_user_mapping " +
      "(id, state, action_performed, task_executions_id, users_id, " +
      "user_groups_id, created_by, modified_by, created_at, modified_at) " +
      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    // Process in optimal batch sizes
    int batchSize = 1000;
    int totalBatches = (taskExecutionUserMappings.size() + batchSize - 1) / batchSize;

    for (int batch = 0; batch < totalBatches; batch++) {
      final int start = batch * batchSize;
      final int end = Math.min((batch + 1) * batchSize, taskExecutionUserMappings.size());
      final List<TaskExecutionUserMapping> batchMappings = taskExecutionUserMappings.subList(start, end);

      log.debug("Processing batch {}/{} with {} records", batch + 1, totalBatches, batchMappings.size());

      jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
        @Override
        public void setValues(PreparedStatement ps, int i) throws SQLException {
          TaskExecutionUserMapping mapping = batchMappings.get(i);
          long now = DateTimeUtils.now();

          ps.setLong(1, mapping.getId());
          ps.setString(2, mapping.getState().name());
          ps.setBoolean(3, mapping.isActionPerformed());
          ps.setLong(4, mapping.getTaskExecutionsId());
          ps.setObject(5,  mapping.getUsersId());
          ps.setObject(6, mapping.getUserGroup() != null ? mapping.getUserGroup().getId() : null);
          ps.setLong(7, mapping.getCreatedBy().getId());
          ps.setLong(8, mapping.getModifiedBy().getId());
          ps.setLong(9, now);
          ps.setLong(10, now);
        }

        @Override
        public int getBatchSize() {
          return batchMappings.size();
        }
      });
    }

    log.info("Successfully inserted {} task execution user mappings", taskExecutionUserMappings.size());
  }
}
