package com.leucine.streem.repository;

import com.leucine.streem.constant.Type;
import com.leucine.streem.model.TaskSchedules;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ITaskSchedulesRepository extends JpaRepository<TaskSchedules, Long> {
  List<TaskSchedules> findByReferencedTaskIdAndCondition(Long referencedTaskId, Type.ScheduledTaskCondition condition);
}
