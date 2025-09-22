package com.leucine.streem.model;

import com.leucine.streem.constant.TableName;
import com.leucine.streem.constant.Type;
import com.leucine.streem.model.helper.UserAuditIdentifiableBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = TableName.TASK_EXECUTOR_LOCKS)
public class TaskExecutorLock extends UserAuditIdentifiableBase implements Serializable {
  @Serial
  private static final long serialVersionUID = 7911707058721601337L;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tasks_id", nullable = false)
  private Task task;

  @Column(name = "tasks_id", columnDefinition = "bigint", insertable = false, updatable = false)
  private Long taskId;


  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "referenced_tasks_id", nullable = false)
  private Task referencedTask;

  @Column(name = "referenced_tasks_id", columnDefinition = "bigint", insertable = false, updatable = false)
  private Long referencedTaskId;


  @Column(columnDefinition = "varchar", length = 50, nullable = false)
  @Enumerated(EnumType.STRING)
  private Type.TaskExecutorLockType lockType;
}
