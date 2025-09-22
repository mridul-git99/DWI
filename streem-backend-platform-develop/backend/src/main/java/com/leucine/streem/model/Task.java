package com.leucine.streem.model;

import com.leucine.streem.constant.TableName;
import com.leucine.streem.dto.request.AutomationRequest;
import com.leucine.streem.model.helper.UserAuditIdentifiableBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = TableName.TASKS)
public class Task extends UserAuditIdentifiableBase implements Serializable {
  private static final long serialVersionUID = -6192033289683199381L;

  @Column(columnDefinition = "varchar", length = 512, nullable = false)
  private String name;

  @Column(columnDefinition = "integer", nullable = false)
  private Integer orderTree;

  @Column(columnDefinition = "boolean default false", nullable = false)
  private boolean hasStop = false;

  @Column(columnDefinition = "boolean default false", nullable = false)
  private boolean isSoloTask = false;

  @Column(columnDefinition = "boolean default false", nullable = false)
  private boolean isTimed = false;

  //This is not enum because it can be null
  // TODO Make this enum and have one of its value NA. also the set the default of columnDefinition to NA
  @Column(columnDefinition = "varchar", length = 50)
  private String timerOperator;

  @Column(nullable = false, columnDefinition = "boolean default false")
  private boolean archived = false;

  @Column(nullable = false, columnDefinition = "boolean default false")
  private boolean isMandatory = false;

  @Column(columnDefinition = "bigint")
  private Long minPeriod;

  @Column(columnDefinition = "bigint")
  private Long maxPeriod;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "stages_id", nullable = false)
  private Stage stage;

  @Column(columnDefinition = "bigint", name = "stages_id", updatable = false, insertable = false)
  private Long stageId;

  @Column(columnDefinition = "boolean", name = "enable_recurrence", nullable = false)
  private boolean enableRecurrence = false;

  @Column(columnDefinition = "boolean", name = "enable_scheduling", nullable = false)
  private boolean enableScheduling = false;

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  @JoinColumn(name = "task_recurrences_id", referencedColumnName = "id")
  private TaskRecurrence taskRecurrence;

  @Column(columnDefinition = "bigint", name = "task_recurrences_id", insertable = false, updatable = false)
  private Long taskRecurrenceId;

  @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "task_schedules_id", referencedColumnName = "id")
  private TaskSchedules taskSchedules;

  @Column(columnDefinition = "bigint", name = "task_schedules_id", insertable = false, updatable = false)
  private Long taskSchedulesId;

  @Column(name = "has_bulk_verification", columnDefinition = "boolean default false", nullable = false)
  private boolean hasBulkVerification = false;

  @Column(name = "has_interlocks", columnDefinition = "boolean default false", nullable = false)
  private boolean hasInterlocks = false;

  @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
  @OrderBy("order_tree")
  @Where(clause = "archived =  false")
  private Set<Parameter> parameters = new HashSet<>();

  //TODO this was added because everytime something gets updated in task
  // we send whole response and media gets reordered
  @OneToMany(fetch = FetchType.LAZY, mappedBy = "task", cascade = CascadeType.ALL)
  @OrderBy("created_at")
  private Set<TaskMediaMapping> medias = new HashSet<>();

  @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @OrderBy("order_tree")
  private Set<TaskAutomationMapping> automations = new HashSet<>();

  @OneToMany(mappedBy = "prerequisiteTask", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<TaskDependency> dependentTasks = new HashSet<>();

  @OneToMany(mappedBy = "dependentTask", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<TaskDependency> prerequisiteTasks = new HashSet<>();

  public void addDependency(Task prerequisiteTask) {
    TaskDependency dependency = new TaskDependency();
    dependency.setPrerequisiteTask(prerequisiteTask);
    dependency.setDependentTask(this);
    prerequisiteTasks.add(dependency);
  }

  @Column(columnDefinition = "boolean default false", nullable = false)
  private boolean hasExecutorLock = false;


  public void addMedia(Media media, User principalUserEntity) {
    TaskMediaMapping taskMediaMapping = new TaskMediaMapping(this, media, principalUserEntity);
    medias.add(taskMediaMapping);
  }

  public void addParameter(Parameter parameter) {
    parameter.setTask(this);
    parameters.add(parameter);
  }

  public void addAutomation(Automation automation, AutomationRequest automationRequest, User principalUserEntity) {
    TaskAutomationMapping taskAutomationMapping = new TaskAutomationMapping(this, automation, automationRequest.getOrderTree(), automationRequest.getDisplayName(), principalUserEntity);
    automations.add(taskAutomationMapping);
  }
}
