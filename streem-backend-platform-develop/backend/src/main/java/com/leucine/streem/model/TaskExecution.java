package com.leucine.streem.model;

import com.leucine.streem.constant.State;
import com.leucine.streem.constant.TableName;
import com.leucine.streem.constant.Type;
import com.leucine.streem.model.helper.UserAuditOptionalBase;
import com.leucine.streem.util.DateTimeUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@NamedEntityGraph(
  name = "readTaskExecution",
  attributeNodes = {
    @NamedAttributeNode(value = "assignees", subgraph = "assignees")
  },
  subgraphs = {
    @NamedSubgraph(name = "assignees", attributeNodes = {
      @NamedAttributeNode(value = "user")
    })
  }
)
@NamedEntityGraph(
  name = "readTaskExecutionWithTask",
  attributeNodes = {
    @NamedAttributeNode(value = "assignees", subgraph = "assignees"),
    @NamedAttributeNode(value = "task")
  },
  subgraphs = {
    @NamedSubgraph(name = "assignees", attributeNodes = {
      @NamedAttributeNode(value = "user")
    })
  }
)
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = TableName.TASK_EXECUTIONS)
public class TaskExecution extends UserAuditOptionalBase implements Serializable {
  @Serial
  private static final long serialVersionUID = -5003035154297337018L;

  @Column(columnDefinition = "varchar", length = 50, nullable = false)
  @Enumerated(EnumType.STRING)
  private State.TaskExecution state;

  @Column(columnDefinition = "varchar", length = 50, nullable = false)
  @Enumerated(EnumType.STRING)
  private Type.TaskExecutionType type;

  @Column(columnDefinition = "text")
  private String correctionReason;

  @Column(columnDefinition = "boolean default false", nullable = false)
  private boolean correctionEnabled;

  //TODO possibly have reasons in separate entity with dedicated columns
  @Column(columnDefinition = "text")
  private String reason;

  @Column(columnDefinition = "bigint")
  private Long startedAt;

  @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  @JoinColumn(name = "started_by", referencedColumnName = "id")
  public User startedBy;

  @Column(columnDefinition = "bigint")
  private Long endedAt;

  @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  @JoinColumn(name = "ended_by", referencedColumnName = "id")
  public User endedBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tasks_id", updatable = false, nullable = false)
  private Task task;

  @Column(name = "tasks_id", columnDefinition = "bigint", insertable = false, updatable = false)
  private Long taskId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "jobs_id", updatable = false, nullable = false)
  private Job job;

  @Column(name = "jobs_id", columnDefinition = "bigint", insertable = false, updatable = false)
  private Long jobId;

  @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.DETACH})
  @JoinColumn(name = "corrected_by", referencedColumnName = "id")
  public User correctedBy;

  @Column(columnDefinition = "bigint")
  private Long correctedAt;

  @Column(columnDefinition = "bigint")
  private Long duration;

  @Column(columnDefinition = "integer", nullable = false)
  private Integer orderTree;

  @Column(columnDefinition = "boolean default false", nullable = false)
  private boolean continueRecurrence;

  // todo add logic to set true when this task scheduled
  @Column(name = "is_scheduled", columnDefinition = "boolean default false", nullable = false)
  private boolean scheduled;

  @Column(columnDefinition = "text")
  private String recurringPrematureStartReason;

  @Column(columnDefinition = "text")
  private String recurringOverdueCompletionReason;


  @Column(columnDefinition = "bigint")
  private Long recurringExpectedStartedAt;

  @Column(columnDefinition = "bigint")
  private Long recurringExpectedDueAt;

  @Column(columnDefinition = "bigint")
  private Long schedulingExpectedStartedAt;

  @Column(columnDefinition = "bigint")
  private Long schedulingExpectedDueAt;

  @Column(columnDefinition = "text")
  private String scheduleOverdueCompletionReason;

  @Column(columnDefinition = "text")
  private String schedulePrematureStartReason;

  @OneToMany(mappedBy = "taskExecution", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<ParameterValue> parameterValues = new HashSet<>();

  @PrePersist
  public void beforePersist() {
    createdAt = DateTimeUtils.now();
    modifiedAt = DateTimeUtils.now();
  }

  @OneToMany(mappedBy = "taskExecution", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<TaskExecutionUserMapping> assignees = new HashSet<>();
}
