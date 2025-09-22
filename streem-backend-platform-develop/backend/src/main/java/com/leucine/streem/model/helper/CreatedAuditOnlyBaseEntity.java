package com.leucine.streem.model.helper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.leucine.streem.model.User;
import com.leucine.streem.util.DateTimeUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@Setter
@Accessors(chain = true)
@MappedSuperclass
public class CreatedAuditOnlyBaseEntity implements Serializable {

  private static final long serialVersionUID = -6784372930267256341L;

  @ManyToOne(targetEntity = User.class, fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", insertable = true, updatable = false)
  protected User createdBy;

  @Column(name = "created_by", columnDefinition = "bigint", insertable = false, updatable = false)
  protected Long createdById;

  @JsonIgnore
  @Column(name = "created_at", columnDefinition = "bigint", insertable = true, updatable = false, nullable = false)
  protected Long createdAt;

  @PrePersist
  public void prePersist() {
    createdAt = DateTimeUtils.now();
  }

}
