package com.leucine.streem.model;

import com.leucine.streem.constant.TableName;
import com.leucine.streem.model.helper.UserAuditIdentifiableBase;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;

@Entity
@Table(name = TableName.TRAINED_USER_PROCESS_PERMISSION_MAPPING)
@Getter
@Setter
public class TrainedUserProcessPermissionMapping extends UserAuditIdentifiableBase implements Serializable {
  @Serial
  private static final long serialVersionUID = 2567170327632100880L;


  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "process_permissions_id", nullable = false, updatable = false)
  private ProcessPermission processPermission;

  @Column(columnDefinition = "bigint", name = "process_permissions_id", updatable = false, insertable = false)
  private Long processPermissionId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trained_users_id", nullable = false, updatable = false)
  private TrainedUser trainedUser;

  @Column(columnDefinition = "bigint", name = "trained_users_id", updatable = false, insertable = false)
  private Long trainedUserId;
}
