package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.projection.ObjectPropertyRelationChecklistView;
import com.leucine.streem.model.Automation;
import com.leucine.streem.model.TaskAutomationMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ITaskAutomationMappingRepository extends JpaRepository<TaskAutomationMapping, Long> {
  @Transactional(rollbackFor = Exception.class)
  @Modifying
  @Query(value = Queries.DELETE_TASK_AUTOMATION_MAPPING, nativeQuery = true)
  void deleteByTaskIdAndAutomationId(@Param("taskId") Long taskId, @Param("automationsId") Long automationId);

  @Query(value = Queries.GET_ALL_AUTOMATIONS_IN_TASK_AUTOMATION_MAPPING_BY_TASK_ID)
  List<Automation> findAllAutomationsByTaskIdAndTriggerType(@Param("taskId") Long taskId, @Param("triggerType") Type.AutomationTriggerType triggerType);

  @Query(value = Queries.AUTOMATION_EXISTS_BY_TASK_ID_AND_TRIGGER_TYPE_AND_AUTOMATION_ACTION_TYPES)
  Boolean automationExistsByTaskIdAndTriggerTypeAndAutomationActionTypes(@Param("taskId") Long taskId, @Param("triggerType") Type.AutomationTriggerType triggerType, @Param("actionTypes") List<Type.AutomationActionType> actionTypes);

  TaskAutomationMapping findByTaskIdAndAutomationId(Long taskId, Long automationId);

  TaskAutomationMapping findByAutomationId(Long automationId);

  @Query(value = Queries.GET_CHECKLIST_TASK_INFO_BY_AUTOMATION_ID, nativeQuery = true)
  ObjectPropertyRelationChecklistView getChecklistAndTaskInfoByAutomationId(@Param("automationId") Long automationId);

  List<TaskAutomationMapping> findTaskAutomationMappingByAutomationIdIn(List<Long> automationIds);
}
