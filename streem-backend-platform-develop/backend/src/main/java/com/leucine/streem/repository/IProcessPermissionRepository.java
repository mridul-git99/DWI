package com.leucine.streem.repository;

import com.leucine.streem.constant.ProcessPermissionType;
import com.leucine.streem.model.ProcessPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface IProcessPermissionRepository extends JpaRepository<ProcessPermission, Long> {
  List<ProcessPermission> findByTypeIn(Set<ProcessPermissionType> type);
}
