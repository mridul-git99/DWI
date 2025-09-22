package com.leucine.streem.repository;

import com.leucine.streem.model.ActionFacilityMapping;
import com.leucine.streem.model.compositekey.ActionFacilityCompositeKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IActionFacilityRepository extends JpaRepository<ActionFacilityMapping, ActionFacilityCompositeKey> {
  List<ActionFacilityMapping> findActionFacilityMappingByAction_Id(Long actionId);
}
