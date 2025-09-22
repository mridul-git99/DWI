package com.leucine.streem.repository;

import com.leucine.streem.constant.Action;
import com.leucine.streem.constant.Queries;
import com.leucine.streem.dto.projection.VariationView;
import com.leucine.streem.model.Variation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IVariationRepository extends JpaRepository<Variation, Long> {
  @Query(value = Queries.GET_ALL_VARIATIONS_OF_A_JOB, nativeQuery = true)
  Page<VariationView> findAllByJobIdAndParameterName(@Param("jobId") Long jobId, @Param("parameterName") String parameterName, Pageable pageable);


  @Modifying
  @Query(value = Queries.RECONFIGURE_VARIATIONS_OF_PARAMETER_VALUE, nativeQuery = true)
  void reconfigureVariationsOfParameterValue(@Param("parameterValueId") Long parameterValueId);

  @Query(value = Queries.GET_ALL_VARIATIONS_OF_PARAMETER_VALUE_ID, nativeQuery = true)
  List<VariationView> findVariationsByParameterValueId(@Param("parameterValueId") Long parameterValueId);

  List<Variation> findAllByParameterValueIdAndType(Long parameterValueId, Action.Variation type);

  @Query(value = Queries.CHECK_IF_VARIATION_NAME_OR_NUMBER_EXISTS_FOR_A_JOB, nativeQuery = true)
  boolean existsAllByVariationNumberOrNameForJob(@Param("variationNumber") String variationNumber, @Param("name") String name, @Param("jobId") Long jobId, @Param("taskId") Long taskId);

  @Query(value = Queries.CHECK_IF_VARIATION_EXISTS_FOR_CONFIG_IDS_AND_PARAMETER_VALUE_IDS, nativeQuery = true)
  boolean existsByConfigIdsForParameterValueId(@Param("configIds") List<String> configIds, @Param("parameterValueId") Long parameterValueId);

  @Query(value = """
    select v.* from variations v
    inner join parameter_values pv on pv.id = v.parameter_values_id
    inner join task_executions te on te.id = pv.task_executions_id
    where te.id = :previousTaskExecutionId
    """, nativeQuery = true)
  List<Variation> findByTaskExecutionId(@Param("previousTaskExecutionId") Long previousTaskExecutionId);

  @Modifying
  @Transactional(rollbackFor = Exception.class)
  @Query(value = Queries.DELETE_BY_VARIATION_ID, nativeQuery = true)
  void deleteByVariationId(@Param("variationId") Long variationId);

}
