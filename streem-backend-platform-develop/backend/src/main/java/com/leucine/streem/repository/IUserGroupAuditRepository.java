package com.leucine.streem.repository;

import com.leucine.streem.model.UserGroupAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IUserGroupAuditRepository extends JpaRepository<UserGroupAudit, Long >, JpaSpecificationExecutor<UserGroupAudit> {

  @Override
  Page<UserGroupAudit> findAll(Specification specification, Pageable pageable);
}
