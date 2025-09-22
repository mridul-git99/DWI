package com.leucine.streem.model;

import com.leucine.streem.constant.ActionTriggerType;
import com.leucine.streem.constant.TableName;
import com.leucine.streem.model.helper.UserAuditIdentifiableBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = TableName.ACTIONS)
public class Action extends UserAuditIdentifiableBase {

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "code", nullable = false)
  private String code;

  @Enumerated(EnumType.STRING)
  @Column(name = "trigger_type", nullable = false)
  private ActionTriggerType triggerType;

  @Column(name = "trigger_entity_id", nullable = false)
  private Long triggerEntityId;

  @Column(name = "archived", columnDefinition = "boolean default false")
  private boolean archived = false;

  @Column(name = "success_message")
  private String successMessage;

  @Column(name = "failure_message")
  private String failureMessage;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "checklists_id", nullable = false, updatable = false)
  private Checklist checklist;

  @Column(columnDefinition = "bigint", name = "checklists_id", updatable = false, insertable = false)
  private Long checklistId;
}
