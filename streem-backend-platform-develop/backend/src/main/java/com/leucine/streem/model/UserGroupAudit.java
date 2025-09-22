package com.leucine.streem.model;

import com.leucine.streem.constant.TableName;
import com.leucine.streem.model.helper.BaseEntity;
import com.leucine.streem.util.DateTimeUtils;
import lombok.*;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@Entity
@Table(name = TableName.USER_GROUP_AUDITS)
@Builder
@AllArgsConstructor
public class UserGroupAudit extends BaseEntity implements Serializable {
  @Serial
  private static final long serialVersionUID = -7100456896745279907L;

  @Column(name = "organisations_id", columnDefinition = "bigint", nullable = false)
  private Long organisationsId;

  @Column(name = "facilities_id",columnDefinition = "bigint", nullable = false)
  private Long facilityId;

  @Column(columnDefinition = "bigint", nullable = false)
  private Long triggeredBy;

  @Column(columnDefinition = "text")
  private String details;

  @Column
  private Long triggeredAt;

  @Column(name = "user_groups_id",columnDefinition = "bigint", nullable = false)
  private Long userGroupId;

  @PrePersist
  public void beforePersist() {
    triggeredAt = DateTimeUtils.now();
  }

}
