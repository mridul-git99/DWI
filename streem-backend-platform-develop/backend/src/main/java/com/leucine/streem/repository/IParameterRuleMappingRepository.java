package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.model.Parameter;
import com.leucine.streem.model.ParameterRuleMapping;
import com.leucine.streem.model.compositekey.ParameterRuleMappingCompositeKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.util.Set;

public interface IParameterRuleMappingRepository extends JpaRepository<ParameterRuleMapping, ParameterRuleMappingCompositeKey> {

  @Transactional
  void deleteAllByTriggeringParameterId(Long triggeringParameterId);

  @Query(value = Queries.GET_ALL_PARAMETER_RULE_MAPPINGS,nativeQuery = true)
  Set<Long> findAllByImpactedParameterId(@Param("impactedParameterId") Long impactedParameterId);

  @Query(value = Queries.GET_ALL_PARAMETER_RULE_MAPPINGS_BY_IMPACTED_PARAMETER_IDS)
  Set<Parameter> findAllByImpactedParameterIdIn(@Param("impactedParameterIds") Set<Long> impactedParameterIds);
}
