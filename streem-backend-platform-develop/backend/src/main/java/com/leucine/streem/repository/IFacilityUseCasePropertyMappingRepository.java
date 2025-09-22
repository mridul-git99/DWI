package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.model.FacilityUseCasePropertyMapping;
import com.leucine.streem.model.compositekey.FacilityUseCasePropertyCompositeKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IFacilityUseCasePropertyMappingRepository extends JpaRepository<FacilityUseCasePropertyMapping, FacilityUseCasePropertyCompositeKey>, JpaSpecificationExecutor<FacilityUseCasePropertyMapping> {
  List<FacilityUseCasePropertyMapping> findAllByFacilityIdAndUseCaseId(Long facilityId, Long useCaseId);

  @Query(value = Queries.GET_DISTINCT_PROPERTY_VALUES_BY_PROPERTY_ID_AND_FACILITY_ID, nativeQuery = true)
  List<Object> findDistinctPropertyValuesOfFacilityAndProperty(@Param("facilityId") Long facilityId,
                                                               @Param("propertyId") Long propertyId,
                                                               @Param("state") String state,
                                                               @Param("archived") boolean archived,
                                                               @Param("limit") int limit, @Param("offset") long offset,
                                                               @Param("propertyNameInput") String propertyNameInput);

  @Query(value = Queries.GET_TOTAL_PROPERTY_VALUES_BY_PROPERTY_ID_AND_FACILITY_ID, nativeQuery = true)
  long findTotalPropertyValuesOfFacilityAndProperty(@Param("facilityId") Long facilityId,
                                                    @Param("propertyId") Long propertyId,
                                                    @Param("state") String state,
                                                    @Param("archived") boolean archived,
                                                    @Param("propertyNameInput") String propertyNameInput);
}
