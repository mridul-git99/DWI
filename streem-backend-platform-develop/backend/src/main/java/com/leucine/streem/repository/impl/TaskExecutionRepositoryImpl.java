package com.leucine.streem.repository.impl;

import com.leucine.streem.model.TaskExecution;
import com.leucine.streem.util.DateTimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class TaskExecutionRepositoryImpl {
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  public TaskExecutionRepositoryImpl(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }


  /**
   * Bulk insert task executions using a single SQL statement with multiple value sets.
   * @param taskExecutions List of task executions to insert
   */
  @Transactional
  public void bulkInsertTaskExecutions(List<TaskExecution> taskExecutions) {
    if (taskExecutions.isEmpty()) return;

    StringBuilder sql = new StringBuilder(
      "INSERT INTO task_executions (id, order_tree, type, state, jobs_id, tasks_id, created_by, modified_by, " +
        "created_at, modified_at, continue_recurrence) VALUES ");

    // Limit batch size to prevent excessively large SQL statements
    int batchSize = 1000;
    int totalBatches = (taskExecutions.size() + batchSize - 1) / batchSize;

    for (int batch = 0; batch < totalBatches; batch++) {
      StringBuilder batchSql = new StringBuilder(sql);
      int start = batch * batchSize;
      int end = Math.min((batch + 1) * batchSize, taskExecutions.size());
      List<TaskExecution> batchTaskExecutions = taskExecutions.subList(start, end);

      // Add placeholders for each record
      for (int i = 0; i < batchTaskExecutions.size(); i++) {
        if (i > 0) batchSql.append(",");
        batchSql.append("(?,?,?,?,?,?,?,?,?,?,?)");
      }

      // Create args array
      Object[] args = new Object[batchTaskExecutions.size() * 11];
      int idx = 0;
      long now = DateTimeUtils.now();

      for (TaskExecution te : batchTaskExecutions) {
        args[idx++] = te.getId();
        args[idx++] = te.getOrderTree();
        args[idx++] = te.getType().name();
        args[idx++] = te.getState().name();
        args[idx++] = te.getJobId();
        args[idx++] = te.getTask().getId();
        args[idx++] = te.getCreatedBy().getId();
        args[idx++] = te.getModifiedBy().getId();
        args[idx++] = now;
        args[idx++] = now;
        args[idx++] = te.isContinueRecurrence();
      }

      // Execute the batch
      jdbcTemplate.update(batchSql.toString(), args);
    }
  }
}
