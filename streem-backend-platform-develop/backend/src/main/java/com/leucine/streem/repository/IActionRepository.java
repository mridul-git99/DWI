package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.ActionDto;
import com.leucine.streem.model.Action;
import com.leucine.streem.model.Parameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IActionRepository extends JpaRepository<Action, Long> {
  Page<Action> findByTriggerEntityId(Long triggerEntityId, Pageable pageable);

  Page<Action> findByChecklistId(Long checklistId, Pageable pageable);

  List<Action> findByChecklistIdAndArchived(Long checklistId, boolean archived);

  Page<Action> findByChecklistIdAndArchived(Long checklistId, boolean archived, Pageable pageable);
}
