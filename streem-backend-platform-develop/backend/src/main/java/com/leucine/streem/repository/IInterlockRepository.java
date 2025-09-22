package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.projection.IdView;
import com.leucine.streem.dto.projection.ObjectPropertyRelationChecklistView;
import com.leucine.streem.model.Interlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IInterlockRepository extends JpaRepository<Interlock, Long>, JpaSpecificationExecutor<Interlock> {

  Optional<Interlock> findFirstByTargetEntityTypeAndTargetEntityId(Type.InterlockTargetEntityType targetEntityType, Long targetEntityId);

  List<Interlock> findByTargetEntityTypeAndTargetEntityId(Type.InterlockTargetEntityType targetEntityType, Long targetEntityId);

  Optional<Interlock> findFirstByTargetEntityType(Type.InterlockTargetEntityType targetEntityType);

  @Query(value = Queries.GET_ALL_INTERLOCK_CONDITIONS_WHERE_OBJECT_TYPE_PROPERTY_IS_USED, nativeQuery = true)
  List<IdView> getAllInterlockConditionsWhereObjectTypePropertyIsUsed(@Param("propertyId") String propertyId);

  @Query(value = Queries.GET_CHECKLIST_TASK_INFO_BY_INTERLOCK_ID, nativeQuery = true)
  ObjectPropertyRelationChecklistView getChecklistAndTaskInfoByInterlockId(@Param("interlockId") Long interlockId);

  @Query(value = Queries.GET_ALL_PARAMETERS_WHERE_PARAMETER_ID_USED_IN_INTERLOCKS, nativeQuery = true)
  List<IdView> getAllParameterWhereParameterIdUsedInInterlocks(@Param("parameterId") String parameterId, @Param("checklistsId") Long checklistsId);
}
