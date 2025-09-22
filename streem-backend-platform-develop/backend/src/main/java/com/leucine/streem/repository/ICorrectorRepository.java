package com.leucine.streem.repository;

import com.leucine.streem.model.Corrector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Repository
@Transactional(rollbackFor = Exception.class)
public interface ICorrectorRepository extends JpaRepository<Corrector, Long> {
  List<Corrector> findByCorrectionId(Long correctionId);

  List<Corrector> findAllByCorrectionIdIn(Set<Long> correctionIdSet);
}
