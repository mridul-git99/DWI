package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.model.TempParameterValue;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ITempParameterValueRepository extends JpaRepository<TempParameterValue, Long> {

  @Query(Queries.GET_TEMP_PARAMETER_VALUE_BY_PARAMETER_ID_AND_TASK_EXECUTION_ID)
  Optional<TempParameterValue> findByParameterIdAndTaskExecutionId(@Param("parameterId") Long parameterId, @Param("taskExecutionId") Long taskExecutionId);

  @EntityGraph(value = "readTempParameterValue", type = EntityGraph.EntityGraphType.FETCH)
  List<TempParameterValue> readByJobId(Long id);

  @EntityGraph(value = "readTempParameterValue", type = EntityGraph.EntityGraphType.FETCH)
  @Query(Queries.READ_TEMP_PARAMETER_VALUE_BY_JOB_AND_STAGE_ID)
  List<TempParameterValue> readByJobIdAndStageId(@Param("jobId") Long jobId, @Param("stageId") Long stageId);

  @EntityGraph(value = "readTempParameterValue", type = EntityGraph.EntityGraphType.FETCH)
  List<TempParameterValue> readByTaskExecutionIdAndParameterIdIn(@Param("taskExecutionId") Long taskExecutionId, @Param("parameterIds") List<Long> parameterIds);

  @Modifying(clearAutomatically = true)
  @Transactional(rollbackFor = Exception.class)
  @Query(value = Queries.UPDATE_TEMP_PARAMETER_VALUE_AND_STATE_BY_PARAMETER_AND_JOB_ID, nativeQuery = true)
  void updateParameterValuesAndState(@Param("taskExecutionId") Long taskExecutionId, @Param("parameterId") Long parameterId, @Param("value") String value, @Param("state") String state, @Param("modifiedBy") Long modifiedBy, @Param("modifiedAt") Long modifiedAt);

  @Modifying(clearAutomatically = true)
  @Transactional(rollbackFor = Exception.class)
  @Query(value = Queries.UPDATE_TEMP_PARAMETER_VALUES_STATE_BY_ID, nativeQuery = true)
  void updateTempParameterValueByStateAndId(@Param("state") String state, @Param("id") Long id);

  @Query(value = Queries.GET_INCOMPLETE_TEMP_PARAMETER_IDS_BY_JOB_ID_AND_TASK_EXECUTION_ID, nativeQuery = true)
  List<Long> findTempIncompleteMandatoryParameterIdsByJobIdAndTaskExecutionId(@Param("jobId") Long jobId, @Param("taskExecutionId") Long taskExecutionId);

  @Query(value = Queries.GET_TEMP_VERIFICATION_INCOMPLETE_PARAMETER_EXECUTION_IDS_BY_TASK_EXECUTION_ID, nativeQuery = true)
  List<Long> findVerificationIncompleteParameterExecutionIdsByTaskExecutionId(@Param("taskExecutionId") Long taskExecutionId);

  @Query(value = Queries.CHECK_IF_DEPENDENT_PARAMETERS_OF_CALCULATION_PARAMETER_NOT_EXECUTED, nativeQuery = true)
  boolean checkIfDependentParametersOfCalculationParameterNotExecuted(@Param("jobId") Long jobId,@Param("parameterId") Long parameterId);

  List<TempParameterValue> findAllByTaskExecutionId(Long taskExecutionId);
}
