package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.constant.State;
import com.leucine.streem.dto.projection.ChecklistJobLiteView;
import com.leucine.streem.dto.projection.ChecklistPropertyView;
import com.leucine.streem.dto.projection.ChecklistView;
import com.leucine.streem.dto.projection.JobLogMigrationChecklistView;
import com.leucine.streem.model.Checklist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface IChecklistRepository extends JpaRepository<Checklist, Long>, JpaSpecificationExecutor<Checklist> {

  @Override
  Page<Checklist> findAll(Specification specification, Pageable pageable);


  List<Checklist> findAllByIdIn(Collection<Long> id, Sort sort);


  @Query(value = Queries.GET_CHECKLIST_BY_TASK_ID)
  Optional<Checklist> findByTaskId(@Param("taskId") Long taskId);

  @Modifying(clearAutomatically = true)
  @Query(value = Queries.UPDATE_CHECKLIST_STATE)
  void updateState(@Param("state") State.Checklist state, @Param("checklistId") Long checklistId);

  @Query(value = Queries.GET_CHECKLIST_CODE)
  String getChecklistCodeByChecklistId(@Param("checklistId") Long checklistId);

  @Transactional(rollbackFor = Exception.class)
  @Modifying
  @Query(value = Queries.DELETE_CHECKLIST_FACILITY_MAPPING, nativeQuery = true)
  void removeChecklistFacilityMapping(@Param("checklistId") Long checklistId, @Param("facilityIds") Set<Long> facilityIds);

  @Query(value = Queries.GET_CHECKLIST_STATE_BY_STAGE_ID)
  State.Checklist findByStageId(@Param("stageId") Long stageId);

  List<Checklist> findByUseCaseId(Long useCaseId);

  @Query(value = Queries.GET_CHECKLIST_BY_STATE)
  List<Long> findByStateInOrderByStateDesc(@Param("state") Set<State.Checklist> stateSet);

  @Query(value = Queries.GET_CHECKLIST_BY_STATE_NOT)
  Set<Long> findByStateNot(@Param("state") State.Checklist state);

  @Query(value = "SELECT c.id as id, c.name as name, c.code as code, c.state as state from checklists c where id = :id", nativeQuery = true)
  JobLogMigrationChecklistView findChecklistInfoById(@Param("id") Long id);

  @Modifying
  @Query(value = Queries.UPDATE_CHECKLIST_DURING_RECALL, nativeQuery = true)
  void updateChecklistDuringRecall(@Param("checklistId") Long checklistId, @Param("userId") Long userId);

  @Query(value = Queries.GET_ALL_APPLICABLE_PROCESS, nativeQuery = true)
  List<Long> findAllChecklistIdsForCurrentFacilityAndOrganisationByObjectTypeInData(@Param("facilityId") Long facilityId, @Param("organisationId") Long organisationId, @Param("objectTypeId") String objectTypeId, @Param("useCaseId") Long useCaseId, @Param("name") String name, @Param("archived") boolean archived);


  @Query(value = Queries.GET_ALL_CHECKLIST_BY_IDS_ORDER_BY_ID, nativeQuery = true)
  List<ChecklistView> getAllByIdsIn(@Param("checklistIds") List<Long> checklistIds);

  @Query(value = "SELECT c.id, c.name, c.code " +
    "FROM checklists c " +
    "WHERE c.id = :checklistId", nativeQuery = true)
  ChecklistJobLiteView getChecklistJobLiteDtoById(@Param("checklistId") Long checklistId);

  @Query(value = Queries.BULK_LOAD_PROPERTIES_FOR_CHECKLISTS, nativeQuery = true)
  List<ChecklistPropertyView> bulkLoadPropertiesForChecklists(@Param("checklistIds") Set<Long> checklistIds);

}
