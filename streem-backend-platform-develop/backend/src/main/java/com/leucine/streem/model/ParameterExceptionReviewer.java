package com.leucine.streem.model;

import com.leucine.streem.constant.TableName;
import com.leucine.streem.model.helper.UserAuditIdentifiableBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = TableName.PARAMETER_EXCEPTION_REVIEWER)
public class ParameterExceptionReviewer extends UserAuditIdentifiableBase implements Serializable {

  private static final long serialVersionUID = 5353539921910517186L;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_groups_id")
  private UserGroup userGroup;


  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "users_id")
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
  @JoinColumn(name = "exceptions_id", updatable = false)
  private ParameterException exceptions;

  @Column(columnDefinition = "bigint", name = "exceptions_id", updatable = false, insertable = false)
  private Long exceptionId;

  @Column(columnDefinition = "boolean default false", nullable = false)
  private boolean actionPerformed;

}
