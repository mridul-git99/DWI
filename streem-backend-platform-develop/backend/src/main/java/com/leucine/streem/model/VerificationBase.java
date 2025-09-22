package com.leucine.streem.model;


import com.leucine.streem.constant.State;
import com.leucine.streem.constant.Type;
import com.leucine.streem.model.helper.UserAuditOptionalBase;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serial;

@Getter
@Setter
@MappedSuperclass
public abstract class VerificationBase extends UserAuditOptionalBase {
  @Serial
  private static final long serialVersionUID = 7559623061528105118L;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "jobs_id", updatable = false)
  private Job job;

  @Column(name = "jobs_id", columnDefinition = "bigint", insertable = false, updatable = false)
  private Long jobId;

  // it stores the user who was assigned for peer verification
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "users_id", updatable = false, nullable = false)
  private User user;

  @Column(columnDefinition = "varchar", length = 50, nullable = false)
  @Enumerated(EnumType.STRING)
  private Type.VerificationType verificationType;

  @Column(columnDefinition = "varchar", length = 50, nullable = false)
  @Enumerated(EnumType.STRING)
  private State.ParameterVerification verificationStatus;

  @Column(columnDefinition = "text")
  private String comments;

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "user_groups_id", updatable = false, nullable = true)
  private UserGroup userGroup;

  @Column(name = "is_bulk", columnDefinition = "boolean default false", nullable = false)
  private boolean isBulk = false;

}
