package com.leucine.streem.model;

import com.leucine.streem.model.helper.UserAuditOptionalBase;
import com.leucine.streem.util.DateTimeUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serial;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "task_dependencies")
public class TaskDependency extends UserAuditOptionalBase {

  @Serial
  private static final long serialVersionUID = 1L;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dependent_task_id", nullable = false)
  private Task dependentTask;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "prerequisite_task_id", nullable = false)
  private Task prerequisiteTask;

  @PrePersist
  public void beforePersist() {
    createdAt = DateTimeUtils.now();
    modifiedAt = DateTimeUtils.now();
  }
}
