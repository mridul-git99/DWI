package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.model.JobAnnotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface IJobAnnotationRepository extends JpaRepository<JobAnnotation, Long>, JpaSpecificationExecutor<JobAnnotation> {
  List<JobAnnotation> findByJobId(Long jobId);

  @Query(value = Queries.GET_LATEST_JOB_ANNOTATION, nativeQuery = true)
  JobAnnotation findLatestByJobId(@Param("jobId") Long jobId);


  @Transactional
  void deleteByJobId(Long jobId);

}
