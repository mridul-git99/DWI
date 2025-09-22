package com.leucine.streem.repository;

import com.leucine.streem.model.TaskRecurrence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(rollbackFor = Exception.class)
public interface ITaskRecurrenceRepository extends JpaRepository<TaskRecurrence, Long> {

}
