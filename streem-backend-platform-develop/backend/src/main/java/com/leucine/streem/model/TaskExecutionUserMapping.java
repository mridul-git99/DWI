package com.leucine.streem.model;

import com.leucine.streem.constant.State;
import com.leucine.streem.constant.TableName;
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
@Table(name = TableName.TASK_EXECUTION_USER_MAPPING)
public class TaskExecutionUserMapping extends UserAuditIdentifiableBase implements Serializable {
  @Serial
  private static final long serialVersionUID = 1451874300783043851L;


  @Column(columnDefinition = "boolean default false", nullable = false)
  private boolean actionPerformed = false;

  @Column(columnDefinition = "varchar", length = 50, nullable = false)
  @Enumerated(EnumType.STRING)
  private State.TaskExecutionAssignee state;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "task_executions_id", nullable = false)
  private TaskExecution taskExecution;

  @Column(columnDefinition = "bigint", name = "task_executions_id", updatable = false, insertable = false)
  private Long taskExecutionsId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "users_id", nullable = false)
  private User user;

  @Column(columnDefinition = "bigint", name = "users_id", updatable = false, insertable = false)
  private Long usersId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_groups_id", referencedColumnName = "id", updatable = false, nullable = false)
  private UserGroup userGroup;


  public TaskExecutionUserMapping(TaskExecution taskExecution, User user, User principalUser) {
    this.taskExecution = taskExecution;
    this.user = user;
    actionPerformed = false;
    state = State.TaskExecutionAssignee.IN_PROGRESS;
    createdBy = principalUser;
    modifiedBy = principalUser;
  }

  public TaskExecutionUserMapping(Long taskExecutionsId, Long usersId, User principalUser) {
    this.taskExecutionsId = taskExecutionsId;
    this.usersId = usersId;
    actionPerformed = false;
    state = State.TaskExecutionAssignee.IN_PROGRESS;
    createdBy = principalUser;
    modifiedBy = principalUser;
  }

  public TaskExecutionUserMapping(TaskExecution taskExecution, UserGroup userGroup, User principalUser) {
    this.taskExecution = taskExecution;
    this.userGroup = userGroup;
    actionPerformed = false;
    state = State.TaskExecutionAssignee.IN_PROGRESS;
    createdBy = principalUser;
    modifiedBy = principalUser;
  }
}
