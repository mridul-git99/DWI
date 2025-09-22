package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.model.TempParameterVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ITempParameterVerificationRepository extends JpaRepository<TempParameterVerification, Long> {

  @Query(value = Queries.FIND_TEMP_PARAMETER_VERIFICATION_BY_JOB_ID_AND_TEMP_PARAMETER_VALUES_ID_AND_VERIFICATION_TYPE_AND_USER_ID, nativeQuery = true)
  TempParameterVerification findByJobIdAndParameterValueIdAndVerificationTypeAndUserId(@Param("jobId") Long jobId, @Param("tempParameterValueId") Long tempParameterValueId, @Param("verificationType") String verificationType, @Param("userId") Long userId);

  @Query(value = Queries.FIND_TEMP_PARAMETER_VERIFICATION_BY_JOB_ID_AND_PARAMETER_ID_AND_TEMP_PARAMETER_VERIFICATION_TYPE, nativeQuery = true)
  TempParameterVerification findByJobIdAndParameterIdAndVerificationType(@Param("jobId") Long jobId, @Param("parameterId") Long parameterId, @Param("verificationType") String verificationType);

  @Query(value = Queries.FIND_LATEST_SELF_AND_PEER_TEMP_PARAMETER_VERIFICATIONS_BY_JOB_ID, nativeQuery = true)
  List<TempParameterVerification> findLatestSelfAndPeerVerificationOfParametersInJob(@Param("jobId") Long jobId);

  @Modifying(clearAutomatically = true)
  void deleteAllByTempParameterValueIdIn(List<Long> tempParameterValueIds);

  List<TempParameterVerification> findByJobIdAndTempParameterValueIdIn(Long jobId, List<Long> tempParameterValueIds);

  @Modifying
  @Query(value = Queries.DELETE_STALE_ENTRIES_IN_TEMP_PARAMETER_VERIFICATIONS, nativeQuery = true)
  void deleteStaleEntriesByTempParameterValueIdAndVerificationType(@Param("tempParameterValueId") Long tempParameterValueId, @Param("verificationType") String verificationType, @Param("verificationStatus") String verificationStatus);
}
