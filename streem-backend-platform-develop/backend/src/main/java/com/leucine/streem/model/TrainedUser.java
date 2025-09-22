package com.leucine.streem.model;

import com.leucine.streem.model.helper.UserAuditIdentifiableBase;
import lombok.*;

import javax.persistence.*;
import java.io.Serial;

@Getter
@Setter
@Entity
@Table(name = "trained_users")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TrainedUser extends UserAuditIdentifiableBase {

  @Serial
  private static final long serialVersionUID = 8488595690551140716L;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "checklists_id", nullable = false)
  private Checklist checklist;

  @Column(columnDefinition = "bigint", name = "checklists_id", updatable = false, insertable = false)
  private Long checklistId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "users_id", referencedColumnName = "id")
  private User user;

  @Column(columnDefinition = "bigint", name = "users_id", updatable = false, insertable = false)
  private Long userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "facilities_id", nullable = false, updatable = false)
  private Facility facility;

  @Column(columnDefinition = "bigint", name = "facilities_id", updatable = false, insertable = false)
  private Long facilityId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_groups_id", updatable = false)
  private UserGroup userGroup;

  @Column(columnDefinition = "bigint", name = "user_groups_id", updatable = false, insertable = false)
  private Long userGroupId;
}
