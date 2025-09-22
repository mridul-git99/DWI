package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.dto.projection.ParameterVerificationListViewProjection;
import com.leucine.streem.model.ParameterVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IParameterVerificationRepository extends JpaRepository<ParameterVerification, Long>, JpaSpecificationExecutor<ParameterVerification> {
  @Query(value = Queries.FIND_BY_JOB_ID_AND_PARAMETER_VALUES_ID_AND_VERIFICATION_TYPE_AND_USER_ID, nativeQuery = true)
  ParameterVerification findByJobIdAndParameterValueIdAndVerificationTypeAndUserId(@Param("jobId") Long jobId, @Param("parameterValueId") Long parameterValueId, @Param("verificationType") String verificationType, @Param("userId") Long userId);

  @Query(value = Queries.FIND_LATEST_SELF_AND_PEER_PARAMETER_VERIFICATIONS_BY_JOB_ID, nativeQuery = true)
  List<ParameterVerification> findLatestSelfAndPeerVerificationOfParametersInJob(@Param("jobId") Long jobId);

  @Query(value = Queries.FIND_LATEST_SELF_AND_PEER_PARAMETER_VERIFICATIONS_BY_PARAMETER_VALUE_ID, nativeQuery = true)
  List<ParameterVerification> findLatestSelfAndPeerVerificationOfParameterValueId(@Param("parameterValueId") Long parameterValueId);

  @Query(value = Queries.FIND_BY_JOB_ID_AND_PARAMETER_ID_AND_PARAMETER_VERIFICATION_TYPE, nativeQuery = true)
  ParameterVerification findByJobIdAndParameterIdAndVerificationType(@Param("jobId") Long jobId, @Param("parameterId") Long parameterId, @Param("verificationType") String verificationType);

  List<ParameterVerification> findByJobIdAndParameterValueIdIn(Long jobId, List<Long> parameterValueIds);

  @Query(value = Queries.GET_ALL_VERIFICATION_FILTERS, nativeQuery = true)
  List<ParameterVerificationListViewProjection> getVerificationFilterView(@Param("status") String status, @Param("jobId") Long jobId, @Param("requestedTo") Long requestedTo, @Param("requestedBy") Long requestedBy, @Param("parameterName") String parameterName, @Param("processName") String processName, @Param("objectId") String objectId, @Param("limit") int limit, @Param("offset") long offset,@Param("facilityId") Long facilityId, @Param("useCaseId") Long useCaseId);


  @Query(value = Queries.TOTAL_COUNT_VERIFICATION_FILTER, nativeQuery = true)
  long getVerificationFilterViewCount(@Param("status") String status, @Param("jobId") Long jobId, @Param("requestedTo") Long requestedTo, @Param("requestedBy") Long requestedBy, @Param("parameterName") String parameterName, @Param("processName") String processName, @Param("objectId") String objectId, @Param("facilityId") Long facilityId, @Param("useCaseId") Long useCaseId);

  @Modifying
  @Query(value = Queries.DELETE_STALE_ENTRIES_IN_PARAMETER_VERIFICATIONS, nativeQuery = true)
  void deleteStaleEntriesByParameterValueIdAndVerificationType(@Param("parameterValueId") Long parameterValueId, @Param("verificationType") String verificationType, @Param("verificationStatus") String verificationStatus);
  @Query(value = Queries.IS_VERIFICATION_PENDING_ON_USER, nativeQuery = true)
  boolean isVerificationPendingOnUser(@Param("jobId") Long jobId, @Param("userId") Long userId);
}
