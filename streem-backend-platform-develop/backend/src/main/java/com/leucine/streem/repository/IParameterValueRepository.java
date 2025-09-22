package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.projection.IncompleteParameterView;
import com.leucine.streem.dto.projection.ParameterValueMediaView;
import com.leucine.streem.dto.projection.ParameterValueView;
import com.leucine.streem.model.ParameterValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface IParameterValueRepository extends JpaRepository<ParameterValue, Long> {

  Optional<ParameterValue> findByParameterIdAndTaskExecutionId(Long parameterId, Long taskExecutionId);


  List<ParameterValue> findByTaskExecutionIdAndParameterIdIn(Long taskExecutionId, List<Long> parameterIds);


  @Query(value = Queries.READ_PARAMETER_VALUE_BY_JOB_ID_AND_STAGE_ID, nativeQuery = true)
  List<ParameterValue> readByJobIdAndStageId(@Param("jobId") Long jobId, @Param("stageId") Long stageId);

  @Query(value = Queries.GET_INCOMPLETE_PARAMETER_IDS_BY_JOB_ID_AND_TASK_EXECUTION_ID, nativeQuery = true)
  List<Long> findIncompleteMandatoryParameterValueIdsByJobIdAndTaskExecutionId(@Param("jobId") Long jobId, @Param("taskExecutionId") Long taskExecutionId);

  @Query(value = Queries.GET_INCOMPLETE_SHOULD_BE_PENDING_APPROVAL_BY_JOB_ID_AND_TASK_EXECUTION_ID, nativeQuery = true)
  List<Long> findIncompleteMandatoryParameterShouldBePendingForApprovalValueIdsByJobIdAndTaskExecutionId(@Param("jobId") Long jobId, @Param("taskExecutionId") Long taskExecutionId);


  @Query(value = Queries.GET_EXECUTABLE_PARAMETER_IDS_BY_TASK_ID)
  List<Long> findExecutableParameterIdsByTaskId(@Param("taskId") Long taskId);

  @Transactional(rollbackFor = Exception.class)
  @Modifying(clearAutomatically = true)
  @Query(value = Queries.UPDATE_PARAMETER_VALUES, nativeQuery = true)
  void updateParameterValues(@Param("taskExecutionId") Long taskExecutionId, @Param("parameterId") Long parameterId, @Param("state") String state, @Param("value") String value, @Param("choices") String choices, @Param("reason") String reason, @Param("modifiedBy") Long modifiedBy, @Param("modifiedAt") Long modifiedAt);

  @Query(value = Queries.GET_PARAMETER_VALUES_BY_JOB_ID_AND_TASK_ID_AND_PARAMETER_TYPE_IN)
  List<ParameterValue> findByJobIdAndTaskIdParameterTypeIn(@Param("jobId") Long jobId, @Param("taskIds") List<Long> taskIds, @Param("parameterTypes") List<Type.Parameter> parameterTypes);

  @Query(value = Queries.GET_PARAMETER_VALUES_BY_JOB_ID_AND_PARAMETER_TARGET_ENTITY_TYPE_IN)
  List<ParameterValue> findByJobIdAndParameterTargetEntityTypeIn(@Param("jobId") Long jobId, @Param("targetEntityTypes") List<Type.ParameterTargetEntityType> targetEntityTypes);

  @Query(value = Queries.FIND_PARAMETER_VALUES_BY_JOB_ID_AND_PARAMETER_ID_IN)
  List<ParameterValue> findByJobIdAndIdsIn(@Param("jobId") Long jobId, @Param("ids") Set<Long> ids);

  @Modifying
  @Transactional(rollbackFor = Exception.class)
  @Query(value = Queries.UPDATE_PARAMETER_VALUE_VISIBILITY)
  void updateParameterValueVisibility(@Param("parameterValueIds") Set<Long> parameterValueIds, @Param("visibility") boolean visibility);

  @Query(value = Queries.GET_ALL_JOB_IDS_BY_TARGET_ENTITY_TYPE_AND_OBJECT_TYPE_IN_DATA, nativeQuery = true)
  Set<Long> getJobIdsByTargetEntityTypeAndObjectInChoices(@Param("targetEntityType") String targetEntityType, @Param("objectId") String objectId);

  @Query(value = Queries.GET_ALL_JOB_IDS_BY_OBJECT_TYPE_IN_DATA, nativeQuery = true)
  Set<Long> getJobIdsByObjectInChoices(@Param("objectId") String objectId);

  @Query(value = Queries.GET_FIRST_PARAMETER_VALUE_BY_OBJECT_ID, nativeQuery = true)
  ParameterValue findFirstByObjectInChoices(@Param("objectId") String objectId);

  @Query(value = "select p from ParameterValue p where p.jobId = :jobId")
  List<ParameterValue> findAllByJobId(@Param("jobId") Long jobId);

  @Query(value = Queries.GET_VERIFICATION_INCOMPLETE_PARAMETER_EXECUTION_IDS_BY_TASK_EXECUTION_ID, nativeQuery = true)
  List<Long> findVerificationIncompleteParameterExecutionIdsByTaskExecutionId(@Param("taskExecutionId") Long taskExecutionId);


  @Query(value = Queries.FIND_LATEST_PARAMETER_VALUE_BY_JOB_ID_AND_PARAMETER_ID, nativeQuery = true)
  ParameterValue findLatestByJobIdAndParameterId(@Param("jobId") Long jobId, @Param("parameterId") Long parameterId);

  @Query(value = Queries.GET_ALL_INCOMPLETE_PARAMETER_VALUE_ID_AND_TASK_EXECUTION_ID_BY_JOB_ID, nativeQuery = true)
  List<IncompleteParameterView> findIncompleteParametersByJobId(@Param("jobId") Long jobId);

  long countAllByTaskExecutionId(Long taskExecutionId);

  @Query(value = Queries.COUNT_ALL_PARAMETER_VALUE_BY_TASK_EXECUTION_AND_VISIBILITY, nativeQuery = true)
  long countAllByTaskExecutionIdWithHidden(@Param("taskExecutionId") Long taskExecutionId, @Param("hidden") boolean hidden);


  @Query(value = Queries.COUNT_PARAMETER_VALUE_BY_PARAMETER_ID_AND_JOB_ID, nativeQuery = true)
  int countParameterValueByParameterIdAndJobId(@Param("parameterId") Long parameterId, @Param("jobId") Long jobId);

  ParameterValue findByTaskExecutionIdAndParameterId(Long taskExecutionId, Long parameterId);

  @Query(value = Queries.GET_ALL_PARAMETERS_ALLOWED_FOR_VARIATION, nativeQuery = true)
  Page<ParameterValue> getAllParametersAvailableForVariations(@Param("jobId") Long jobId, @Param("parameterName") String parameterName, Pageable pageable);

  @Query(value = Queries.GET_MASTER_TASK_PARAMETER_VALUE, nativeQuery = true)
  ParameterValue getMasterTaskParameterValue(@Param("parameterId") Long parameterId, @Param("jobId") Long jobId);

  @Query(value = Queries.FIND_ALL_PARAMETERS_ELIGIBLE_FOR_AUTOINITIALISATION, nativeQuery = true)
  Set<Long> findParametersEligibleForAutoInitialization(@Param("jobId") Long jobId, @Param("showParameterExecutionIds") Set<Long> showParameterExecutionIds, @Param("executedParameterIds") Set<Long> executedParameterIds);

  @Query(value = Queries.CHECK_IF_LATEST_REFERENCED_PARAMETER_IS_EXECUTED, nativeQuery = true)
  boolean checkIfLatestReferencedParameterIsExecuted(@Param("jobId") Long jobId, @Param("parameterId") Long parameterId);

  @Query(value = Queries.FIND_BY_JOB_ID_AND_PARAMETER_ID_WITH_CORRECTION_ENABLED, nativeQuery = true)
  Long findByJobIdAndParameterIdWithCorrectionEnabled(@Param("jobId") Long jobId, @Param("parameterId") Long parameterId);

  @Query(value = Queries.GET_PENDING_FOR_APPROVAL_PARAMETER_IDS_BY_JOB_ID_AND_TASK_EXECUTION_ID, nativeQuery = true)
  List<Long> findPendingApprovalParameterValueIdsByJobIdAndTaskExecutionId(@Param("taskExecutionId") Long taskExecutionId);

  @Query(value = Queries.CHECK_IF_ALL_PARAMETERS_IN_TASK_COMPLETED_WITH_CORRECTION, nativeQuery = true)
  boolean areAllInitiatedParametersCompletedWithCorrection(@Param("taskExecutionId") Long taskExecutionId);


  @Query(value = Queries.GET_PARAMETER_VALUE_BY_JOB_IDS)
  List<ParameterValue> findAllByJobIdAndTargetEntityType(@Param("jobIds") Set<Long> jobIds, @Param("targetEntityType") Type.ParameterTargetEntityType targetEntityType);

  @Query(value = Queries.GET_ALL_PARAMETER_VALUES_BY_TASK_EXECUTION_ID, nativeQuery = true)
  List<ParameterValueView> findAllExecutionDataByTaskExecutionId(@Param("taskExecutionId") Long taskExecutionId);

  @Query(value = Queries.ARE_ALL_PARAMETER_VALUES_HIDDEN_BY_TASK_EXECUTION_ID, nativeQuery = true)
  boolean areAllParameterValuesHiddenByTaskExecutionId(@Param("taskExecutionId") Long taskExecutionId);

  @Query(value = Queries.GET_ALL_MEDIAS_DATA_BY_TASK_EXECUTION_ID, nativeQuery = true)
  List<ParameterValueMediaView> findAllMediaDetailsByTaskExecutionId(@Param("taskExecutionId") Long taskExecutionId);

  @Query(value = Queries.GET_ALL_PARAMETER_VALUES_BY_JOB_ID, nativeQuery = true)
  Set<ParameterValueView> findAllParameterValueDataByJobId(@Param("jobId") Long jobId);

  List<ParameterValue> findAllByTaskExecutionId(Long taskExecutionId);

  @Modifying
  @Transactional(rollbackFor = Exception.class)
  @Query(value = Queries.RECALL_VERIFICATION_STATE_FOR_HIDDEN_PARAMETER_VALUES, nativeQuery = true)
  void recallVerificationStateForHiddenParameterValues(@Param("hideIds") Set<Long> hideIds);

  @Modifying
  @Transactional(rollbackFor = Exception.class)
  @Query(value = Queries.RECALL_VERIFICATION_STATE_FOR_HIDDEN_PARAMETER_VALUES_WITH_EXCEPTIONS, nativeQuery = true)
  void recallVerificationStateForHiddenParameterValuesWithExceptions(@Param("hideIds") Set<Long> hideIds);

  @Query(value = Queries.CHECK_IF_RESOURCE_PARAMETER_HASACTIVE_EXCEPTIONS, nativeQuery = true)
  boolean checkIfResourceParameterHasActiveExceptions(@Param("jobId") Long jobId, @Param("parameterId") Long parameterId);


  //Latest Response
  @Query(value = """
    SELECT id,
                       value,
                       choices,
                       data,
                       type,
                       label,
                       taskId,
                       parameterValueId,
                       taskExecutionId,
                       hidden,
                       parameterId,
                       taskExecutionState,
                       impactedBy
                FROM (SELECT p.id                                                                  AS id,
                             pv.value                                                              AS value,
                             pv.parameters_id                                                      AS parameterId,
                             CAST(pv.choices AS TEXT)                                              AS choices,
                             CAST(p.data AS TEXT)                                                  AS data,
                             p.type                                                                AS type,
                             p.label                                                               AS label,
                             p.tasks_id                                                            AS taskId,
                             pv.id                                                                 AS parameterValueId,
                             pv.task_executions_id                                                 AS taskExecutionId,
                             pv.hidden                                                             AS hidden,
                             CAST(pv.impacted_by AS TEXT)                                                        AS impactedBy,
                             te.state                                                              AS taskExecutionState,
                             ROW_NUMBER() OVER (PARTITION BY pv.parameters_id ORDER BY pv.id DESC) AS rn
                      FROM parameter_values pv
                               LEFT JOIN task_executions te ON te.id = pv.task_executions_id
                               INNER JOIN parameters p ON pv.parameters_id = p.id
                      WHERE p.id IN (:parameterIds)
                        AND pv.jobs_id = :jobId) ranked
                WHERE rn = 1
    """, nativeQuery = true)
  List<ParameterValueView> getParameterPartialDataByIds(@Param("parameterIds") Set<Long> parameterIds, @Param("jobId") Long jobId);


  //Master Repsponse
  @Query(value = """
    SELECT id, value, choices, data, type, label, taskId, parameterValueId, taskExecutionId, hidden
    FROM (
        SELECT p.id                    as id,
               pv.value                 as value,
               CAST(pv.choices AS TEXT) as choices,
               CAST(p.data AS TEXT)     as data,
               p.type                   as type,
               p.label                  as label,
               p.tasks_id               as taskId,
               pv.id                    as parameterValueId,
               pv.task_executions_id     as taskExecutionId,
               pv.hidden                as hidden,
               ROW_NUMBER() OVER (PARTITION BY pv.parameters_id ORDER BY pv.id ASC) as rn
        FROM parameter_values pv
        INNER JOIN parameters p ON pv.parameters_id = p.id
        WHERE p.id IN (:parameterIds)
          AND pv.jobs_id = :jobId
    ) ranked
    WHERE rn = 1
    """, nativeQuery = true)
  List<ParameterValueView> getParameterPartialDataFOrMasterByIds(@Param("parameterIds") Set<Long> parameterIds, @Param("jobId") Long jobId);

}
