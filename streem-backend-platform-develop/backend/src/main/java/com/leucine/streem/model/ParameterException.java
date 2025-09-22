package com.leucine.streem.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.constant.State;
import com.leucine.streem.constant.TableName;
import com.leucine.streem.model.helper.UserAuditIdentifiableBase;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = TableName.PARAMETER_EXCEPTION)
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class ParameterException extends UserAuditIdentifiableBase implements Serializable {
  @Serial
  private static final long serialVersionUID = 5357529931910557186L;

  @Column(columnDefinition = "varchar", length = 20, nullable = false, updatable = false)
  private String code;

  @Column(columnDefinition = "text")
  private String value;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private JsonNode choices;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "parameter_values_id", updatable = false)
  private ParameterValue parameterValue;

  @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
  @JoinColumn(name = "task_executions_id", updatable = false)
  private TaskExecution taskExecution;

  @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
  @JoinColumn(name = "facilities_id", updatable = false)
  private Facility facility;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "jobs_id", updatable = false)
  private Job job;

  @Column(columnDefinition = "varchar", length = 50, nullable = false)
  @Enumerated(EnumType.STRING)
  private State.ParameterException Status;

  @Column(columnDefinition = "text")
  private String initiatorsReason;

  @Column(columnDefinition = "text")
  private String reviewersReason;

  @Column(columnDefinition = "varchar", length = 50)
  @Enumerated(EnumType.STRING)
  private State.ParameterExecution previousState;

  @Column(columnDefinition = "text")
  private String reason;

  @Column(name = "rules_id", columnDefinition = "text", nullable = false)
  private String ruleId;

}
