package com.leucine.streem.service.impl;


import com.leucine.streem.collections.JobLog;
import com.leucine.streem.collections.JobLogMediaData;
import com.leucine.streem.config.MediaConfig;
import com.leucine.streem.constant.JobLogMisc;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.JobAnnotationDto;
import com.leucine.streem.dto.mapper.IJobAnnotationMapper;
import com.leucine.streem.dto.mapper.IUserMapper;
import com.leucine.streem.dto.request.JobAnnotationDeleteRequest;
import com.leucine.streem.dto.request.JobAnnotationRequest;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.ExceptionType;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.model.Job;
import com.leucine.streem.model.JobAnnotation;
import com.leucine.streem.model.Media;
import com.leucine.streem.model.User;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.model.helper.SpecificationBuilder;
import com.leucine.streem.repository.*;
import com.leucine.streem.service.ICodeService;
import com.leucine.streem.service.IJobAnnotationService;
import com.leucine.streem.service.IJobAuditService;
import com.leucine.streem.service.IJobLogService;
import com.leucine.streem.util.IdGenerator;
import com.leucine.streem.util.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobAnnotationService implements IJobAnnotationService {


  private final IJobAnnotationRepository jobAnnotationRepository;
  private final IJobAnnotationMapper jobAnnotationMapper;
  private final IUserRepository userRepository;
  private final IMediaRepository mediaRepository;
  private final IJobAnnotationMediaMappingRepository jobAnnotationMediaMappingRepository;
  private final IJobRepository jobRepository;
  private final IJobAuditService jobAuditService;
  private final IJobLogService jobLogService;
  private final IUserMapper userMapper;
  private final ICodeService codeService;
  @Autowired
  private MediaConfig mediaConfig;

  @Override
  public Page<JobAnnotationDto> getJobAnnotationByJobId(String filters, Pageable pageable) {
    log.info("[getJobAnnotationsByJobId] Request to get Job Annotation by filters: {}", filters);
    Specification<JobAnnotation> specification = SpecificationBuilder.createSpecification(filters, new ArrayList<>());
    Page<JobAnnotation> jobAnnotation = jobAnnotationRepository.findAll(specification, pageable);
    return new PageImpl<>(jobAnnotationMapper.toDto(jobAnnotation.getContent()), pageable, jobAnnotation.getTotalElements());
  }

  @Override
  public JobAnnotationDto saveJobAnnotation(JobAnnotationRequest jobAnnotationRequest) throws ResourceNotFoundException {
    log.info("[saveJobAnnotation] Request to save Job Annotation, jobAnnotationRequest: {}", jobAnnotationRequest);
    List<Media> medias = mediaRepository.findAllById(jobAnnotationRequest.getIds());
    Job job = jobRepository.findById(jobAnnotationRequest.getJobId())
      .orElseThrow(() -> new ResourceNotFoundException(jobAnnotationRequest.getJobId(), ErrorCode.JOB_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    JobAnnotation jobAnnotation = new JobAnnotation();
    jobAnnotation.setId(IdGenerator.getInstance().nextId());
    jobAnnotation.setRemarks(jobAnnotationRequest.getRemarks());
    jobAnnotation.setJob(job);
    jobAnnotation.setCode(codeService.getCode(Type.EntityType.JOB_ANNOTATION, principalUser.getOrganisationId()));
    jobAnnotation.setCreatedBy(principalUserEntity);
    jobAnnotation.setModifiedBy(principalUserEntity);
    jobAnnotation.addAllMedias(job, medias, principalUserEntity);
    JobAnnotation savedJobAnnotation = jobAnnotationRepository.save(jobAnnotation);
    JobAnnotationDto jobAnnotationDto = jobAnnotationMapper.toDto(savedJobAnnotation);
    jobAuditService.saveJobAnnotation(jobAnnotationDto, principalUser);
    jobLogService.recordJobLogTrigger(String.valueOf(jobAnnotationDto.getJobId()), JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.ANNOTATION_REMARK,
      JobLogMisc.ANNOTATION_REMARK, null, jobAnnotationDto.getRemarks(), jobAnnotationDto.getRemarks(), userMapper.toUserAuditDto(principalUserEntity));

    List<JobLogMediaData> jobLogMedias = getJobLogMediaData(medias);
    jobLogService.recordJobLogTrigger(String.valueOf(jobAnnotationDto.getJobId()), JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.ANNOTATION_MEDIA,
      JobLogMisc.ANNOTATION_MEDIA, jobLogMedias, null, null, userMapper.toUserAuditDto(principalUserEntity));

    return jobAnnotationDto;
  }

  @Override
  public JobAnnotationDto updateJobAnnotation(Long jobId, JobAnnotationRequest jobAnnotationRequest) throws ResourceNotFoundException {
    log.info("[updateJobAnnotation] Request to update Job Annotation, jobId: {}, jobAnnotationRequest: {}", jobId, jobAnnotationRequest);

    jobAnnotationMediaMappingRepository.deleteAllByJobId(jobAnnotationRequest.getJobId());
    List<Media> medias = mediaRepository.findAllById(jobAnnotationRequest.getIds());
    Job job = jobRepository.findById(jobId)
      .orElseThrow(() -> new ResourceNotFoundException(jobAnnotationRequest.getJobId(), ErrorCode.JOB_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    JobAnnotation latestJobAnnotation = Optional.ofNullable(jobAnnotationRepository.findLatestByJobId(jobId))
      .orElseThrow(() -> new ResourceNotFoundException(jobId, ErrorCode.JOB_ANNOTATION_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    latestJobAnnotation.setRemarks(jobAnnotationRequest.getRemarks());
    latestJobAnnotation.setJob(job);
    latestJobAnnotation.addAllMedias(job, medias, principalUserEntity);
    JobAnnotation savedJobAnnotation = jobAnnotationRepository.save(latestJobAnnotation);
    JobAnnotationDto jobAnnotationDto = jobAnnotationMapper.toDto(savedJobAnnotation);
    jobAuditService.saveJobAnnotation(jobAnnotationDto, principalUser);
    jobLogService.recordJobLogTrigger(String.valueOf(jobAnnotationDto.getJobId()), JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.ANNOTATION_REMARK,
      JobLogMisc.ANNOTATION_REMARK, null, jobAnnotationDto.getRemarks(), jobAnnotationDto.getRemarks(), userMapper.toUserAuditDto(principalUserEntity));
    if (!Utility.isEmpty(jobAnnotationDto.getMedias())) {
      List<JobLogMediaData> jobLogMedias = getJobLogMediaData(medias);
      jobLogService.recordJobLogTrigger(String.valueOf(jobAnnotationDto.getJobId()), JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.ANNOTATION_MEDIA,
        JobLogMisc.ANNOTATION_MEDIA, jobLogMedias, null, null, userMapper.toUserAuditDto(principalUserEntity));
    }
    return jobAnnotationDto;
  }

  @Override
  public BasicDto deleteJobAnnotation(Long jobId, JobAnnotationDeleteRequest jobAnnotationDeleteRequest) throws ResourceNotFoundException {
    log.info("[deleteJobAnnotation] Request to delete Job Annotation, jobId: {}", jobId);
    jobAnnotationMediaMappingRepository.deleteAllByJobId(jobId);

    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());

    jobAnnotationRepository.deleteByJobId(jobId);
    jobAuditService.deleteJobAnnotation(jobId, jobAnnotationDeleteRequest.getReason(), principalUser);
    jobLogService.recordJobLogTrigger(String.valueOf(jobId), JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.ANNOTATION_REMARK,
      JobLogMisc.ANNOTATION_REMARK, null, "", "", userMapper.toUserAuditDto(principalUserEntity));
    jobLogService.recordJobLogTrigger(String.valueOf(jobId), JobLog.COMMON_COLUMN_ID, Type.JobLogTriggerType.ANNOTATION_MEDIA,
      JobLogMisc.ANNOTATION_MEDIA, new ArrayList<>(), null, null, userMapper.toUserAuditDto(principalUserEntity));

    return (new BasicDto()).setMessage("Deleted Job Annotation Successfully");
  }

  @Override
  public List<JobLogMediaData> getJobLogMediaData(List<Media> medias) {
    List<JobLogMediaData> jobLogMedias = new ArrayList<>();
    if (!Utility.isEmpty(medias)) {
      for (Media media : medias) {
        JobLogMediaData jobLogMediaData = new JobLogMediaData();
        jobLogMediaData.setName(media.getOriginalFilename());
        jobLogMediaData.setType(media.getType());
        jobLogMediaData.setDescription(media.getDescription());
        String link = mediaConfig.getCdn() + java.io.File.separator + media.getRelativePath() + java.io.File.separator + media.getFilename();
        jobLogMediaData.setLink(link);
        jobLogMedias.add(jobLogMediaData);
      }
    }
    return jobLogMedias;
  }
}
