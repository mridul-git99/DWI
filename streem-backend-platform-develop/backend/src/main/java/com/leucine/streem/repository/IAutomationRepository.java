package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.dto.projection.IdView;
import com.leucine.streem.model.Automation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IAutomationRepository extends JpaRepository<Automation, Long> {

  @Query(value = Queries.GET_ALL_PARAMETERS_WHERE_PARAMETER_ID_IS_USED_IN_AUTOMATION, nativeQuery = true)
  List<IdView> getAllParametersWhereParameterIdUsedInAutomation(@Param("parameterId") String parameterId);

  @Query(value = Queries.GET_ALL_AUTOMATIONS_WHERE_OBJECT_TYPE_PROPERTY_IS_USED, nativeQuery = true)
  List<IdView> getAllAutomationsWhereObjectTypePropertyIsUsed(@Param("propertyId") String propertyId);

  @Query(value = Queries.GET_ALL_AUTOMATIONS_WHERE_OBJECT_TYPE_RELATION_IS_USED, nativeQuery = true)
  List<IdView> getAllAutomationsWhereObjectTypeRelationIsUsed(@Param("relationId") String relationId);
}
