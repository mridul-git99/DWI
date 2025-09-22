package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.constant.State;
import com.leucine.streem.dto.projection.IdView;
import com.leucine.streem.dto.projection.JobExcelProjection;
import com.leucine.streem.dto.projection.JobProcessInfoView;
import com.leucine.streem.dto.projection.PendingForApprovalStatusView;
import com.leucine.streem.model.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Repository
@Transactional(rollbackFor = Exception.class)
public interface IJobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {


  @Override
  Page<Job> findAll(@Nullable Specification<Job> specification, Pageable pageable);

  @Override
  long count(@Nullable Specification<Job> specification);


  @Transactional(rollbackFor = Exception.class)
  @Modifying(clearAutomatically = true)
  @Query(value = Queries.SET_JOB_TO_UNASSIGNED_IF_NO_USER_IS_ASSIGNED, nativeQuery = true)
  void updateJobToUnassignedIfNoUserAssigned();

  @Query(value = Queries.IS_ACTIVE_JOB_EXIST_FOR_GIVEN_CHECKLIST)
  boolean findByChecklistIdWhereStateNotIn(@Param("checklistId") Long checklistId, @Param("jobStates") Set<State.Job> jobStates);

  @Query(value = Queries.FIND_JOB_PROCESS_INFO, nativeQuery = true)
  JobProcessInfoView findJobProcessInfo(@Param("jobId") Long jobId);

  @Query(value = Queries.IS_JOB_EXISTS_BY_SCHEDULER_ID_AND_DATE_GREATER_THAN_EXPECTED_START_DATE)
  boolean isJobExistsBySchedulerIdAndDateGreaterThanOrEqualToExpectedStartDate(@Param("schedulerId") Long schedulerId, @Param("date") Long date);

  @Query(value = Queries.GET_ALL_PENDING_FOR_APPROVAL_PARAMETER_STATUS, nativeQuery = true)
  Page<PendingForApprovalStatusView> getAllPendingForApprovalParameters(@Param("facilityId") long facilityId, @Param("parameterName") String parameterName,
                                                                        @Param("processName") String processName, @Param("objectId") String objectId,
                                                                        @Param("jobId") String jobId, @Param("userId") String userId, @Param("useCaseId") Long useCaseId,
                                                                        @Param("showAllException") boolean showAllException, @Param("requestedBy") Long requestedBy, Pageable pageable);

  List<Job> findAllByChecklistId(Long checklistId);

  @Query(value = Queries.GET_MY_JOBS, nativeQuery = true)
  List<IdView> getMyJobs(@Param("organisationId") Long organisationId, @Param("facilityId") Long facilityId, @Param("usecaseId") Long usecaseId, @Param("jobStates") List<String> jobStates, @Param("taskExecutionStates") List<String> taskExecutionStates, @Param("userId") Long userId, @Param("objectId") String objectId, @Param("pom") boolean pom, @Param("checklistAncestorId") Long checklistAncestorId, @Param("name") String name, @Param("code") String code, @Param("limit") int limit, @Param("offset") long offset);

  @Query(value = Queries.GET_MY_JOBS_COUNT, nativeQuery = true)
  Long countMyJob(@Param("organisationId") Long organisationId, @Param("facilityId") Long facilityId, @Param("usecaseId") Long usecaseId, @Param("jobStates") List<String> jobStates, @Param("taskExecutionStates") List<String> taskExecutionStates, @Param("userId") Long userId, @Param("objectId") String objectId, @Param("pom") boolean pom, @Param("checklistAncestorId") Long checklistAncestorId, @Param("name") String name, @Param("code") String code);

  @Query(value = "SELECT j.* FROM jobs j WHERE j.id IN :ids ORDER BY created_at DESC", nativeQuery = true)
  List<Job> findJobsByIdInOrderBy(@Param("ids") Set<Long> ids);


  @Query(value = """
    SELECT j.checklists_id
    FROM jobs j
    WHERE j.id = :jobId
    """, nativeQuery = true)
  Long getChecklistIdByJobId(@Param("jobId") Long jobId);

  @Query(value = Queries.GET_JOB_STATE_BY_JOB_ID)
  State.Job getStateByJobId(@Param("jobId") Long jobId);

  @Query(value = """
      SELECT * FROM jobs WHERE id IN :ids ORDER BY id DESC
    """, nativeQuery = true)
  List<Job> findAllByIdIn(@Param("ids") Set<Long> ids);

  @Query(value = """
    SELECT j.facilities_id
    FROM jobs j
    WHERE j.id = :jobId
    """, nativeQuery = true)
  Long getFacilityIdByJobId(@Param("jobId") Long jobId);

  @Query(value = Queries.BULK_LOAD_JOBS_WITH_FILTERS_STATIC, nativeQuery = true)
  List<JobExcelProjection> findJobsForExcelDownload(
      @Param("organisationId") Long organisationId,
      @Param("facilityId") Long facilityId,
      @Param("stateFilter") List<String> stateFilter,
      @Param("useCaseIdFilter") Long useCaseIdFilter,
      @Param("checklistAncestorIdFilter") Long checklistAncestorIdFilter,
      @Param("codeFilter") String codeFilter,
      @Param("checklistNameFilter") String checklistNameFilter,
      @Param("expectedEndDateLt") Long expectedEndDateLt,
      @Param("expectedStartDateGt") Long expectedStartDateGt,
      @Param("expectedStartDateLt") Long expectedStartDateLt,
      @Param("expectedStartDateIsNull") Boolean expectedStartDateIsNull,
      @Param("startedAtGte") Long startedAtGte,
      @Param("startedAtLte") Long startedAtLte,
      @Param("objectIdChoicesJson") String objectIdChoicesJson,
      @Param("createdById") Long createdById
  );

}
