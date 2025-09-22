package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.dto.projection.IdView;
import com.leucine.streem.model.AutoInitializedParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Repository
@Transactional(rollbackFor = Exception.class)
public interface IAutoInitializedParameterRepository extends JpaRepository<AutoInitializedParameter, Long>, JpaSpecificationExecutor<AutoInitializedParameter> {
  @Modifying
  void deleteByChecklistId(Long checklistId);

  @Query(value = Queries.FIND_ALL_ELIGIBLE_PARAMETER_IDS_TO_BE_AUTOINITIALIZED_BY_REFERENCED_PARAMETER_ID, nativeQuery = true)
  List<Long> findAllEligibleParameterIdsToAutoInitializeByReferencedParameterId(@Param("referencedParameterId") Long referencedParameterId, @Param("executedParameterIds") Set<Long> executedParameterIds, @Param("jobId") Long jobId);

  @Query(value = Queries.GET_ALL_AUTOINITIALIZED_PARAMETERS_WHERE_PARAMETER_IS_USED, nativeQuery = true)
  List<IdView> getAllAutoInitializedParametersWhereParameterIsUsed(@Param("parameterId") Long parameterId);

  @Query(value = Queries.GET_ALL_AUTOINITIALIZED_PARAMETERS_WHERE_OBJECT_TYPE_PROPERTY_IS_USED, nativeQuery = true)
  List<IdView> getAllAutoInitializedParametersWhereObjectTypePropertyIsUsed(@Param("propertyId") String propertyId);

  @Query(value = Queries.GET_ALL_AUTOINITIALIZED_PARAMETERS_WHERE_OBJECT_TYPE_RELATION_IS_USED, nativeQuery = true)
  List<IdView> getAllAutoInitializedParametersWhereObjectTypeRelationIsUsed(@Param("relationId") String relationId);

  boolean existsByAutoInitializedParameterId(Long autoInitializedParameterId);

  @Query(value = Queries.GET_ALL_REFERENCED_PARAMETERS_OF_AN_AUTOINITIALISED_PARAMETER, nativeQuery = true)
  List<IdView> getReferencedParameterIdByAutoInitializedParameterId(@Param("autoInitializedParameterId") Long autoInitializedParameterId);

}
