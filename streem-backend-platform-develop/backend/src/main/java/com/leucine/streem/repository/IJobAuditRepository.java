package com.leucine.streem.repository;

import com.leucine.streem.model.JobAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IJobAuditRepository extends JpaRepository<JobAudit, Long>, JpaSpecificationExecutor<JobAudit> {
  List<JobAudit> findByJobIdOrderByTriggeredAtDesc(Long jobId);
}
