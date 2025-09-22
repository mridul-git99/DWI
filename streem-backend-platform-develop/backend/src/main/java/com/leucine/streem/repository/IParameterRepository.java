package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.projection.AutoInitializeParameterView;
import com.leucine.streem.dto.projection.IdView;
import com.leucine.streem.dto.projection.ObjectPropertyRelationChecklistView;
import com.leucine.streem.dto.projection.ParameterView;
import com.leucine.streem.model.Parameter;
import com.leucine.streem.model.ParameterValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
public interface IParameterRepository extends JpaRepository<Parameter, Long>, JpaSpecificationExecutor<Parameter> {
  @Query(value = Queries.GET_PARAMETERS_BY_TASK_ID_IN_AND_ORDER_BY_ORDER_TREE)
  List<Parameter> findByTaskIdInOrderByOrderTree(@Param("taskIds") Set<Long> taskIds);

  @Query(value = Queries.GET_ENABLED_PARAMETERS_COUNT_BY_PARAMETER_TYPE_IN_AND_ID_IN)
  Integer getEnabledParametersCountByTypeAndIdIn(@Param("parameterIds") Set<Long> parameterIds, @Param("types") Set<Type.Parameter> types);

  @Query(value = Queries.GET_PARAMETERS_BY_CHECKLIST_ID_AND_TARGET_ENTITY_TYPE)
  List<Parameter> getParametersByChecklistIdAndTargetEntityType(@Param("checklistId") Long checklistId, @Param("targetEntityType") Type.ParameterTargetEntityType targetEntityType);

  @Query(value = Queries.GET_ARCHIVED_PARAMETERS_BY_REFERENCED_PARAMETER_ID, nativeQuery = true)
  List<Parameter> getArchivedParametersByReferencedParameterIds(@Param("referencedParameterIds") List<Long> referencedParameterIds);

  @Modifying(clearAutomatically = true)
  @Transactional(rollbackFor = Exception.class)
  @Query(value = Queries.UPDATE_PARAMETER_TARGET_ENTITY_TYPE_BY_CHECKLIST_ID_AND_TARGET_ENTITY_TYPE)
  void updateParametersTargetEntityType(@Param("checklistId") Long checklistId, @Param("targetEntityType") Type.ParameterTargetEntityType targetEntityType, @Param("updatedTargetEntityType") Type.ParameterTargetEntityType updatedTargetEntityType);

  @Query(value = Queries.GET_PARAMETERS_COUNT_BY_CHECKLIST_ID_AND_PARAMETER_ID_IN_AND_TARGET_ENTITY_TYPE)
  Integer getParametersCountByChecklistIdAndParameterIdInAndTargetEntityType(@Param("checklistId") Long checklistId, @Param("parameterIds") Set<Long> parameterIds, @Param("targetEntityType") Type.ParameterTargetEntityType targetEntityType);

  @Modifying(clearAutomatically = true)
  @Transactional(rollbackFor = Exception.class)
  @Query(value = Queries.UPDATE_PARAMETERS_TARGET_ENTITY_TYPE)
  Integer updateParametersTargetEntityType(@Param("parameterIds") Set<Long> parameterIds, @Param("targetEntityType") Type.ParameterTargetEntityType targetEntityType);

  @Transactional(rollbackFor = Exception.class)
  @Modifying
  @Query(value = Queries.UPDATE_PARAMETER_ORDER, nativeQuery = true)
  void reorderParameter(@Param("parameterId") Long parameterId, @Param("order") Integer order, @Param("userId") Long userId, @Param("modifiedAt") Long modifiedAt);

  @Modifying
  @Transactional(rollbackFor = Exception.class)
  @Query(value = Queries.UPDATE_VISIBILITY_OF_PARAMETERS)
  void updateParameterVisibility(@Param("hiddenParameterIds") Set<Long> hiddenParameterIds, @Param("visibleParameterIds") Set<Long> visibleParameterIds);

  @Query(value = Queries.IS_LINKED_PARAMETER_EXISTS_BY_PARAMETER_ID, nativeQuery = true)
  boolean isLinkedParameterExistsByParameterId(@Param("checklistId") Long checklistId, @Param("parameterId") String parameterId);

  @Query(value = Queries.GET_ALL_CHECKLIST_IDS_BY_OBJECT_TYPE_IN_DATA, nativeQuery = true)
  Set<Long> getChecklistIdsByObjectTypeInData(@Param("objectTypeId") String objectTypeId);

  @Query(value = Queries.GET_ALL_PARAMETER_BY_CHECKLIST_ID_AND_OBJECT_TYPE_ID, nativeQuery = true)
  List<ParameterView> getResourceParametersByObjectTypeIdAndChecklistId(@Param("objectTypeId") String objectTypeId, @Param("checklistIds") List<Long> checklistIds);

  List<Parameter> findByChecklistIdAndArchived(Long checklistId, boolean isArchived);

  @Query(value = Queries.GET_ALL_PARAMETERS_USED_IN_RULES, nativeQuery = true)
  List<IdView> getAllParametersWhereParameterIsUsedInRules(@Param("hideRulesJson") String hideRulesJson, @Param("showRulesJson") String showRulesJson, @Param("parameterId") Long parameterId);

  @Query(value = Queries.GET_ALL_PARAMETERS_USED_IN_PROPERTY_FILTERS, nativeQuery = true)
  List<IdView> getAllParametersWhereParameterIsUsedInPropertyFilters(@Param("parameterId") String parameterId);

  @Query(value = Queries.GET_ALL_PARAMETERS_USED_IN_PROPERTY_VALIDATIONS, nativeQuery = true)
  List<IdView> getAllParametersWhereParameterIsUsedInPropertyValidations(@Param("parameterId") String parameterId);

  @Query(value = Queries.GET_ALL_PARAMETERS_USED_IN_RESOURCE_VALIDATION, nativeQuery = true)
  List<IdView> getAllParametersWhereParameterIsUsedInResourceValidations(@Param("parameterId") String parameterId);

  @Query(value = Queries.GET_NON_HIDDEN_AUTO_INITIALISED_PARAMETERS_BY_TASK_EXECUTION_ID, nativeQuery = true)
  List<AutoInitializeParameterView> getNonHiddenAutoInitialisedParametersByTaskExecutionId(@Param("taskExecutionId") Long taskExecutionId);

  @Query(value = Queries.IS_PARAMETER_USED_IN_AUTOINITIALISATION, nativeQuery = true)
  boolean isParameterUsedInAutoInitialization(@Param("parameterId") Long parameterId);

  @Query(value = Queries.GET_ALL_PARAMETERS_WHERE_OBJECT_TYPE_PROPERTY_IS_USED_IN_PROPERTY_FILTERS, nativeQuery = true)
  List<IdView> getAllParametersWhereObjectTypePropertyIsUsedInPropertyFilters(@Param("propertyId") String propertyId);

  @Query(value = Queries.GET_ALL_PARAMETERS_WHERE_OBJECT_TYPE_PROPERTY_IS_USED_IN_VALIDATION, nativeQuery = true)
  List<IdView> getAllParametersWhereObjectTypePropertyIsUsedInValidation(@Param("propertyId") String propertyId);

  @Query(value = Queries.GET_ALL_PARAMETERS_WHERE_OBJECT_TYPE_PROPERTY_IS_USED_IN_PROPERTY_VALIDATION, nativeQuery = true)
  List<IdView> getAllParametersWhereObjectTypePropertyIsUsedInPropertyValidation(@Param("propertyId") String propertyId);

  @Query(value = Queries.GET_ALL_PARAMETERS_WHERE_OBJECT_TYPE_RELATION_IS_USED_IN_PROPERTY_FILTERS, nativeQuery = true)
  List<IdView> getAllParametersWhereObjectTypeRelationIsUsedInPropertyFilters(@Param("relationId") String relationId);

  @Query(value = Queries.GET_CHECKLIST_TASK_INFO_BY_PARAMETER_ID, nativeQuery = true)
  ObjectPropertyRelationChecklistView getChecklistAndTaskInfoByParameterId(@Param("parameterId") Long parameterId);

  @Query(value = Queries.GET_CHECKLIST_TASK_INFO_BY_PARAMETER_ID_FOR_RESOURCE_VALIDATION, nativeQuery = true)
  ObjectPropertyRelationChecklistView getChecklistAndTaskInfoByParameterIdForResourceValidation(@Param("parameterId") Long parameterId);

  @Query(value = Queries.GET_PARAMETER_IDS_BY_CHECKLIST_ID_AND_TARGET_ENTITY_TYPE)
  Set<Long> getParameterIdsByChecklistIdAndTargetEntityType(@Param("checklistId") Long checklistId, @Param("targetEntityType") Type.ParameterTargetEntityType targetEntityType);

  List<Parameter> findAllByTypeAndChecklistIdInAndArchived(Type.Parameter type, Collection<Long> checklistId, boolean archived);

  @Query(value = Queries.GET_ALL_PARAMETERS_USED_IN_CALCULATION, nativeQuery = true)
  List<IdView> getAllParameterIdsWhereParameterIsUsedInCalculation(@Param("parameterId") String parameterId, @Param("checklistId") Long checklistId);

  @Query(value = Queries.GET_ALL_PARAMETERS_USED_IN_CREATE_OBJECT_AUTOMATION_MAPPING, nativeQuery = true)
  List<IdView> getAllParametersWhereParameterIsUsedInCreateObjectAutomations(@Param("parameterId") String parameterId);

  @Query(value = Queries.GET_PARAMETERS_USED_IN_LEAST_COUNT, nativeQuery = true)
  List<IdView> getParameterIdWhereParameterIsUsedInLeastCount(@Param("parameterId") String parameterId, @Param("checklistId") Long checklistId);

  @Query(value = Queries.GET_ALL_PARAMETERS_WHERE_PROPERTY_ID_IS_USED_IN, nativeQuery = true)
  List<IdView> getAllParametersWherePropertyIdIsUsedIn(@Param("propertyId") String propertyId);


  @Query(value = Queries.GET_PARAMETER_TARGET_ENTITY_TYPE_BY_PARAMETER_IDS, nativeQuery = true)
  List<Long> getParameterTargetEntityTypeByParameterIds(@Param("checklistIds") Set<Long> checklistIds);

  @Query(value = Queries.GET_PARAMETERS_USED_IN_NUMBER_CRITERIA_VALIDATION, nativeQuery = true)
  List<IdView> getParameterIdWhereParameterIsUsedInNumberCriteriaValidation(@Param("parameterId") String parameterId, @Param("checklistId") Long checklistId);

  boolean existsByIdAndType(Long id, Type.Parameter parameter);

  @Query(value = Queries.FIND_PARAMETER_IDS_BY_TASK_ID, nativeQuery = true)
  Set<Long> findParameterIdsByTaskId(@Param("taskId") Long taskId);

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query(value = Queries.INCREASE_ORDER_TREE_BY_ONE_AFTER_PARAMETER, nativeQuery = true)
  void increaseOrderTreeByOneAfterParameter(@Param("taskId") Long taskId, @Param("orderTree") Integer orderTree, @Param("parameterId") Long newElementId);

  List<Parameter> findAllByIdInAndArchived(Set<Long> parameterIds, boolean b);

  @Query(value = Queries.GET_ALL_PARAMETERS_USED_IN_DATE_DATE_TIME_VALIDATIONS, nativeQuery = true)
  List<IdView> getAllParametersWhereParameterIsUsedDateAndDateTimeValidations(@Param("parameterId") String parameterId);


  @Query(value = "SELECT *  FROM parameters  where rules is not null and CAST(rules AS TEXT) <> 'null'", nativeQuery = true)
  List<Parameter> findAllParametersWithRules();

  @Query(value = "SELECT *  FROM parameters  where rules is not null and CAST(rules AS TEXT) <> 'null' and checklists_id = :checklistId", nativeQuery = true)
  List<Parameter> findAllParametersWithRulesForChecklistId(@Param("checklistId") Long checklistId);

  boolean existsByIdAndChecklistId(Long id, Long checklistId);

}
