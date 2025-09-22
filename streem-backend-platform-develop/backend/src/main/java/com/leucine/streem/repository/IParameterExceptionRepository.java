package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.model.ParameterException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(rollbackFor = Exception.class)
public interface IParameterExceptionRepository extends JpaRepository<ParameterException, Long> {
  @Query(value = Queries.FIND_LATEST_EXCEPTION_BY_PARAMETER_VALUE_ID, nativeQuery = true)
  List<ParameterException> findLatestException(@Param("parameterValueId") Long parameterValueId);

  @Query(value = Queries.IS_EXCEPTION_PENDING_ON_USER, nativeQuery = true)
  boolean isExceptionPendingOnUser(@Param("jobId") Long jobId, @Param("userId") Long userId);

  @Query(value = Queries.IS_CJF_EXCEPTION_PENDING, nativeQuery = true)
  boolean isCJFExceptionPendingOnUser(@Param("jobId") Long jobId);

  @Query(value = """
    SELECT EXISTS (SELECT pv.id
                   FROM parameter_values pv
                            INNER JOIN parameters p ON pv.parameters_id = p.id
                            INNER JOIN exceptions e ON pv.id = e.parameter_values_id
                   WHERE p.target_entity_type = 'PROCESS'
                     AND e.status = 'REJECTED' AND pv.jobs_id =:jobId) AS result
    
    """, nativeQuery = true)
  boolean isExceptionRejectedOnCjf(@Param("jobId") Long jobId);
}
