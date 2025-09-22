package com.leucine.streem.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.constant.TableName;
import com.leucine.streem.model.helper.UserAuditIdentifiableBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = TableName.TASK_RECURRENCES)
public class TaskRecurrence extends UserAuditIdentifiableBase implements Serializable {
  private static final long serialVersionUID = -656645357313965612L;

  @Column(columnDefinition = "integer")
  private Integer startDateInterval;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb default '{}'")
  private JsonNode startDateDuration;

  @Column(columnDefinition = "integer")
  private Integer positiveStartDateToleranceInterval;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb default '{}'")
  private JsonNode positiveStartDateToleranceDuration;

  @Column(columnDefinition = "integer")
  private Integer negativeStartDateToleranceInterval;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb default '{}'")
  private JsonNode negativeStartDateToleranceDuration;

  @Column(columnDefinition = "integer")
  private Integer dueDateInterval;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb default '{}'")
  private JsonNode dueDateDuration;

  @Column(columnDefinition = "integer")
  private Integer positiveDueDateToleranceInterval;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb default '{}'")
  private JsonNode positiveDueDateToleranceDuration;

  @Column(columnDefinition = "integer")
  private Integer negativeDueDateToleranceInterval;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb default '{}'")
  private JsonNode negativeDueDateToleranceDuration;

  @OneToOne(mappedBy = "taskRecurrence", fetch = FetchType.LAZY)
  private Task task;

}
