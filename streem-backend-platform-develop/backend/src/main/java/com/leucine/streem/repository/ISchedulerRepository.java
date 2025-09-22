package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.constant.State;
import com.leucine.streem.model.Scheduler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ISchedulerRepository extends JpaRepository<Scheduler, Long>, JpaSpecificationExecutor<Scheduler> {
  @Override
  Page<Scheduler> findAll(Specification specification, Pageable pageable);

  List<Scheduler> findByChecklistId(Long checklistId);

  @Query(value = Queries.IS_ACTIVE_SCHEDULER_EXIST_FOR_GIVEN_CHECKLIST, nativeQuery = true)
  boolean findByChecklistIdWhereSchedulerIsActive(@Param("checklistId") Long checklistId);

  List<Scheduler> findAll(Specification specification);

}
