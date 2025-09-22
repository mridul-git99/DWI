package com.leucine.streem.repository.impl;

import com.leucine.streem.model.ParameterValue;
import com.leucine.streem.util.DateTimeUtils;
import com.leucine.streem.util.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Repository
public class ParameterValueRepositoryImpl {
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  public ParameterValueRepositoryImpl(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Bulk insert parameter values using JDBC batch processing.
   *
   * @param parameterValues List of parameter values to insert
   */
  @Transactional
  public void bulkInsertParameterValues(List<ParameterValue> parameterValues) {
    if (parameterValues.isEmpty()) return;

    final String sql = "INSERT INTO parameter_values (id, state, hidden, parameters_id, jobs_id, task_executions_id, " +
      "created_by, modified_by, created_at, modified_at, has_variations, has_corrections, " +
      "client_epoch, version, value, choices) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)";

    jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
      @Override
      public void setValues(PreparedStatement ps, int i) throws SQLException {
        ParameterValue pv = parameterValues.get(i);

        // Generate ID if needed
        if (pv.getId() == null) {
          pv.setId(IdGenerator.getInstance().nextId());
        }

        long now = DateTimeUtils.now();

        int idx = 1;
        ps.setLong(idx, pv.getId());
        idx++;

        ps.setString(idx, pv.getState().name());
        idx++;

        ps.setBoolean(idx, pv.isHidden());
        idx++;

        ps.setLong(idx, pv.getParameter().getId());
        idx++;

        ps.setLong(idx, pv.getJob().getId());
        idx++;

        if (pv.getTaskExecution() != null) {
          ps.setLong(idx, pv.getTaskExecution().getId());
        } else {
          ps.setNull(idx, Types.BIGINT);
        }
        idx++;

        ps.setLong(idx, pv.getCreatedBy().getId());
        idx++;

        if (pv.getModifiedBy() != null) {
          ps.setLong(idx, pv.getModifiedBy().getId());
        } else {
          ps.setNull(idx, Types.BIGINT);
        }
        idx++;

        ps.setLong(idx, now);
        idx++;

        if (pv.getModifiedAt() != null) {
          ps.setLong(idx, pv.getModifiedAt());
        } else {
          ps.setLong(idx, now);
        }
        idx++;

        ps.setBoolean(idx, false); // has_variations
        idx++;

        ps.setBoolean(idx, false); // has_corrections
        idx++;

        ps.setLong(idx, now);      // client_epoch
        idx++;

        ps.setLong(idx, 0L);       // version
        idx++;

        if (pv.getValue() != null) {
          ps.setString(idx, pv.getValue());
        } else {
          ps.setNull(idx, Types.VARCHAR);
        }
        idx++;

        // Handle JsonNode choices - using string representation and letting PostgreSQL cast it to jsonb
        if (pv.getChoices() != null) {
          ps.setString(idx, pv.getChoices().toString());
        } else {
          ps.setNull(idx, Types.VARCHAR);
        }
      }

      @Override
      public int getBatchSize() {
        return parameterValues.size();
      }
    });
  }

//  @Transactional
  public int bulkUpdateParameterValueVisibility(Set<Long> parameterValueIds, boolean visibility) {
    if (parameterValueIds.isEmpty())
      return 0;

    // Convert Set to List for batch processing
    List<Long> idList = new ArrayList<>(parameterValueIds);

    // Use a large batch size to minimize round trips
    final int BATCH_SIZE = 10000;
    final long currentTime = DateTimeUtils.now();

    final String sql = "UPDATE parameter_values SET hidden = ?, modified_at = ? WHERE id = ?";

    int totalUpdated = 0;

    // Process in batches to avoid memory issues with very large sets
    for (int i = 0; i < idList.size(); i += BATCH_SIZE) {
      final int currentBatchSize = Math.min(BATCH_SIZE, idList.size() - i);
      final int startIndex = i;

      int[] batchResult = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
        @Override
        public void setValues(PreparedStatement ps, int j) throws SQLException {
          ps.setBoolean(1, visibility);
          ps.setLong(2, currentTime);
          ps.setLong(3, idList.get(startIndex + j));
        }

        @Override
        public int getBatchSize() {
          return currentBatchSize;
        }
      });

      // Count properly - only count positive values (actual row updates)
      for (int result : batchResult) {
        if (result > 0) {
          totalUpdated += result;
        }
      }
    }
    return totalUpdated;
  }


}
