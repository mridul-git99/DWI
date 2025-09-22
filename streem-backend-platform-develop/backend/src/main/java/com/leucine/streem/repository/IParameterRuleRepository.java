package com.leucine.streem.repository;

import com.leucine.streem.model.ParameterRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface IParameterRuleRepository extends JpaRepository<ParameterRule, Long> {

  @Modifying(clearAutomatically = true)
  @Transactional(rollbackFor = Exception.class)
  void deleteByIdIn(List<Long> impactingParameterId);
}
