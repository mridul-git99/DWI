package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.dto.projection.CorrectionListViewProjection;
import com.leucine.streem.model.Correction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(rollbackFor = Exception.class)
public interface ICorrectionRepository extends JpaRepository<Correction, Long> {

  @Query(value = Queries.FIND_LATEST_CORRECTION_BY_PARAMETER_VALUE_ID, nativeQuery = true)
  CorrectionListViewProjection findLatestCorrection(@Param("parameterValueId") Long parameterValueId);
  @Query(value = Queries.GET_LATEST_CORRECTION_BY_PARAMETER_VALUE_ID, nativeQuery = true)
  Correction getLatestCorrectionByParameterValueId(@Param("parameterValueId") Long parameterValueId);
  @Query(value = Queries.GET_ALL_CORRECTIONS, nativeQuery = true)
  List<CorrectionListViewProjection> getAllCorrections(@Param("userId") Long userId,@Param("facilityId") Long facilityId,@Param("useCaseId") Long useCaseId,@Param("status") String status, @Param("parameterName") String parameterName, @Param("processName") String processName, @Param("jobId") Long jobId, @Param("initiatedBy") Long initiatedBy, @Param("limit") int limit, @Param("offset") long offset);
  @Query(value = Queries.GET_ALL_CORRECTIONS_COUNT, nativeQuery = true)
  long getAllCorrectionsCount(@Param("userId") Long userId,@Param("facilityId") Long facilityId,@Param("useCaseId") Long useCaseId,@Param("status") String status,@Param("parameterName") String parameterName, @Param("processName") String processName, @Param("jobId") Long jobId, @Param("initiatedBy") Long initiatedBy);

  //We get Corrections in Ascending order of correction id.
  @Query(value = Queries.GET_ALL_CORRECTIONS_BY_PARAMETER_VALUE_ID, nativeQuery = true)
  List<CorrectionListViewProjection> getAllCorrectionsByParameterValueId(@Param("parameterValueId") Long parameterValueId);

  @Query(value = Queries.IS_CORRECTION_PENDING, nativeQuery = true)
  boolean isCorrectionPending(@Param("jobId") Long jobId, @Param("userId") Long userId);
}
