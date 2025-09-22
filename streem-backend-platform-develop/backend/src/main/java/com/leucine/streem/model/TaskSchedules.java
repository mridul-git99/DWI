package com.leucine.streem.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.constant.TableName;
import com.leucine.streem.constant.Type;
import com.leucine.streem.model.helper.UserAuditIdentifiableBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;

@Entity
@NoArgsConstructor
@Table(name = TableName.TASK_SCHEDULES)
@Getter
@Setter
public class TaskSchedules extends UserAuditIdentifiableBase implements Serializable {
  @Serial
  private static final long serialVersionUID = -3369894986242697968L;

  @Column(name = "type", columnDefinition = "varchar", length = 50, nullable = false)
  @Enumerated(EnumType.STRING)
  private Type.ScheduledTaskType type;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "referenced_tasks_id", updatable = false)
  private Task referencedTask;

  //This is the id of the task that is being scheduled
  @Column(columnDefinition = "bigint", name = "referenced_tasks_id", insertable = false, updatable = false)
  private Long referencedTaskId;

  @Column(name = "condition", columnDefinition = "varchar", length = 50, nullable = false)
  @Enumerated(EnumType.STRING)
  private Type.ScheduledTaskCondition condition;

  @org.hibernate.annotations.Type(type = "jsonb")
  @Column(columnDefinition = "jsonb default '{}'", nullable = false)
  private JsonNode startDateDuration;

  private Integer startDateInterval;

  @org.hibernate.annotations.Type(type = "jsonb")
  @Column(columnDefinition = "jsonb default '{}'", nullable = false)
  private JsonNode dueDateDuration;

  private Integer dueDateInterval;
}
