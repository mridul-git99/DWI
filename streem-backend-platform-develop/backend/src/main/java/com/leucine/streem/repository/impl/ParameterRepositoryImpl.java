package com.leucine.streem.repository.impl;

import com.leucine.streem.model.Parameter;
import com.leucine.streem.model.User;
import com.leucine.streem.util.DateTimeUtils;
import com.leucine.streem.util.IdGenerator;
import lombok.Data;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Data
@Service
public class ParameterRepositoryImpl {
  private final JdbcTemplate jdbcTemplate;

  public void bulkInsertIntoParameters(List<Parameter> parameters, User user){

    String sql = "INSERT INTO public.parameters " +
      "(id, archived, order_tree, \"data\", \"label\", is_mandatory, \"type\", created_at, modified_at, created_by, modified_by, tasks_id, description, validations, target_entity_type, checklists_id, is_auto_initialized, auto_initialize, rules, hidden, verification_type, metadata) " +
      "VALUES (?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?::jsonb)";

    jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
      @Override
      public void setValues(PreparedStatement ps, int i) throws SQLException {
        Parameter param = parameters.get(i);

        if (param.getId() == null) {
          param.setId(IdGenerator.getInstance().nextId());
        }

        long now = DateTimeUtils.now();

        ps.setLong(1, param.getId());
        ps.setBoolean(2, param.isArchived());
        ps.setInt(3, param.getOrderTree());
        ps.setString(4, param.getData() != null ? param.getData().toString() : null);
        ps.setString(5, param.getLabel());
        ps.setBoolean(6, param.isMandatory());
        ps.setString(7, param.getType().toString());
        ps.setLong(8, param.getCreatedAt() != null? param.getCreatedAt() : now);
        ps.setLong(9, param.getModifiedAt() != null? param.getModifiedAt() : now);
        ps.setLong(10, param.getCreatedBy().getId());
        ps.setLong(11, param.getModifiedBy().getId());
        ps.setObject(12, param.getTask() != null ? param.getTask().getId() : null); // Set taskId, not the task itself
        ps.setString(13, param.getDescription());
        ps.setString(14, param.getValidations() != null ? param.getValidations().toString() : null);
        ps.setString(15, param.getTargetEntityType().toString());
        ps.setLong(16, param.getChecklistId());
        ps.setBoolean(17, param.isAutoInitialized());
        ps.setString(18, param.getAutoInitialize() != null ? param.getAutoInitialize().toString() : null);
        ps.setString(19, param.getRules() != null ? param.getRules().toString() : null);
        ps.setBoolean(20, param.isHidden());
        ps.setString(21, param.getVerificationType().toString());
        ps.setString(22, param.getMetadata() != null ? param.getMetadata().toString() : null);
      }

      @Override
      public int getBatchSize() {
        return parameters.size();
      }
    });
  }
}
