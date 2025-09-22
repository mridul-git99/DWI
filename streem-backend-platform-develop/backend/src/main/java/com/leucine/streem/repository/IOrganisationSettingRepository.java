package com.leucine.streem.repository;

import com.leucine.streem.model.OrganisationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IOrganisationSettingRepository extends JpaRepository<OrganisationSetting, Long> {
  
  /**
   * Find organisation setting by organisation ID
   * @param organisationId the organisation ID
   * @return Optional OrganisationSetting
   */
  Optional<OrganisationSetting> findByOrganisationId(Long organisationId);
  
  /**
   * Find logo URL by organisation ID
   * @param organisationId the organisation ID
   * @return Optional logo URL string
   */
  @Query("SELECT os.logoUrl FROM OrganisationSetting os WHERE os.organisationId = :organisationId")
  Optional<String> findLogoUrlByOrganisationId(@Param("organisationId") Long organisationId);
}
