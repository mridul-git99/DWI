package com.leucine.streem.model;

import com.leucine.streem.constant.TableName;
import com.leucine.streem.model.helper.UserAuditIdentifiableBase;
import lombok.*;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = TableName.TRAINED_USER_TASK_MAPPING)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TrainedUserTaskMapping extends UserAuditIdentifiableBase implements Serializable {

  @Serial
  private static final long serialVersionUID = 984496697480536718L;

  @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  @JoinColumn(name = "trained_users_id", nullable = false)
  private TrainedUser trainedUser;


  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tasks_id", updatable = false)
  private Task task;

  @Column(columnDefinition = "bigint", name = "tasks_id", updatable = false, insertable = false)
  private Long taskId;

}
