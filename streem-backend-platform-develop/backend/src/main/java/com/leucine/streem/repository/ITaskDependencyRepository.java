package com.leucine.streem.repository;

import com.leucine.streem.model.TaskDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ITaskDependencyRepository extends JpaRepository<TaskDependency, Long> {
  List<TaskDependency> findAllByDependentTaskId(Long taskId);

  List<TaskDependency> save(List<TaskDependency> taskDependencies);

  List<TaskDependency> findAllByDependentTaskIdIn(List<Long> taskIds);
}
