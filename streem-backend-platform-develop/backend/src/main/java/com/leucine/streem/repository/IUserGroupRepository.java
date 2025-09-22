package com.leucine.streem.repository;

import com.leucine.streem.model.UserGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IUserGroupRepository extends JpaRepository<UserGroup, Long>, JpaSpecificationExecutor<UserGroup> {
  @Override
  Page<UserGroup> findAll(Specification specification, Pageable pageable);

  @Query(value = """
    SELECT EXISTS(
                        SELECT 1
                        FROM user_groups ug
                        WHERE ug.name = :name
                          AND ug.facility_id = :facilityId
                          AND ug.active = :active
                          AND (:id is null or ug.id <> :id)
                    )
          """, nativeQuery = true)
  boolean existsByNameAndFacilityIdAndActive(@Param("name") String name, @Param("facilityId") Long facilityId, @Param("active") boolean active, @Param("id") Long id);

  boolean existsByIdAndActive(Long id, boolean b);
}
