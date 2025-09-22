package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.model.JobAnnotationMediaMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface IJobAnnotationMediaMappingRepository extends JpaRepository<JobAnnotationMediaMapping, Long> {
  @Transactional(rollbackFor = Exception.class)
  @Modifying
  @Query(value = Queries.DELETE_JOB_ANNOTATION_MEDIA_MAPPING_BY_JOB_ID, nativeQuery = true)
  void deleteAllByJobId(@Param("jobId") Long jobId);
}
