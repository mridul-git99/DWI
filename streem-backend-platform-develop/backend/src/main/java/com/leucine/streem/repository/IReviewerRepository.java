package com.leucine.streem.repository;

import com.leucine.streem.model.Corrector;
import com.leucine.streem.model.Reviewer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Repository
@Transactional(rollbackFor = Exception.class)
public interface IReviewerRepository extends JpaRepository<Reviewer, Long> {
  List<Reviewer> findByCorrectionId(Long id);

  List<Reviewer> findAllByCorrectionIdIn(Set<Long> correctionIdSet);
}
