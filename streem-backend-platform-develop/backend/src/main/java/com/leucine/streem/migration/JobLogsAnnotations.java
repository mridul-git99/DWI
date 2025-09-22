package com.leucine.streem.migration;

import com.leucine.streem.collections.JobLog;
import com.leucine.streem.collections.JobLogData;
import com.leucine.streem.collections.JobLogMediaData;
import com.leucine.streem.config.MediaConfig;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.model.JobAnnotation;
import com.leucine.streem.model.JobAnnotationMediaMapping;
import com.leucine.streem.repository.IJobAnnotationRepository;
import com.leucine.streem.repository.IJobLogRepository;
import com.leucine.streem.util.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobLogsAnnotations {
  private static final Set<Type.JobLogTriggerType> ANNOTATION_TYPES = Set.of(
    Type.JobLogTriggerType.ANNOTATION_REMARK,
    Type.JobLogTriggerType.ANNOTATION_MEDIA
  );

  private final IJobLogRepository jobLogRepository;
  private final IJobAnnotationRepository jobAnnotationRepository;
  private final MediaConfig mediaConfig;


  @Transactional
  public BasicDto execute() {

    List<JobAnnotation> allAnnotations = jobAnnotationRepository.findAll();
    if (Utility.isEmpty(allAnnotations)) {
      return new BasicDto().setMessage("No annotations found; nothing to migrate");
    }

    Map<Long, List<JobAnnotation>> annotationsByJobId = allAnnotations.stream()
            .filter(a -> !Utility.isEmpty(a.getJob()) && !Utility.isEmpty(a.getJob().getId()))
            .collect(Collectors.groupingBy(a -> a.getJob().getId()));

    List<JobLog> logsToSave = new ArrayList<>();

    for (Map.Entry<Long, List<JobAnnotation>> entry : annotationsByJobId.entrySet()) {
      Long jobId = entry.getKey();
      List<JobAnnotation> annotationsForJob = entry.getValue();

      JobLog jobLog  = jobLogRepository.findById(String.valueOf(jobId))
              .orElseThrow(() -> new IllegalStateException("JobLog not found for jobId: " + jobId));


      List<JobLogData> cleanedLogs = Optional.ofNullable(jobLog.getLogs())
              .orElseGet(Collections::emptyList)
              .stream()
              .filter(ld -> !ANNOTATION_TYPES.contains(ld.getTriggerType()))
              .collect(Collectors.toList());

      JobAnnotation latestJobAnnotation = pickLatest(annotationsForJob);

      if (!Utility.isEmpty(latestJobAnnotation.getRemarks())) {
        cleanedLogs.add(buildRemarkLog(latestJobAnnotation.getRemarks()));
      }
      if (!Utility.isEmpty(latestJobAnnotation.getMedias())) {
        cleanedLogs.add(buildMediaLog(latestJobAnnotation.getMedias()));
      }

      jobLog.setLogs(cleanedLogs);
      logsToSave.add(jobLog);
    }

    if (!logsToSave.isEmpty()) {
      jobLogRepository.saveAll(logsToSave);
    }
    return new BasicDto().setMessage("Migration Successful");
  }

  private JobAnnotation pickLatest(List<JobAnnotation> list) {
    return list.stream()
            .max(Comparator.comparing(JobAnnotation::getId))
            .orElseThrow();
  }

private JobLogData buildRemarkLog(String remark) {
  return new JobLogData(
    JobLog.COMMON_COLUMN_ID,
    Type.JobLogTriggerType.ANNOTATION_REMARK,
    "Annotation Remark",
    remark,
    remark,
    Collections.emptyList(),
    Collections.emptyMap()
  );


}

private JobLogData buildMediaLog(List<JobAnnotationMediaMapping> medias) {

  List<JobLogMediaData> mediaData = medias.stream().map(mapping -> {
    JobLogMediaData dto = new JobLogMediaData();
    var media = mapping.getMedia();
    dto.setLink(mediaConfig.getCdn() + File.separator + media.getRelativePath() + File.separator + media.getFilename());
    dto.setType(media.getType());
    dto.setName(media.getFilename());
    dto.setDescription(media.getDescription());
    return dto;
  }).collect(Collectors.toList());
  return new JobLogData(JobLog.COMMON_COLUMN_ID,Type.JobLogTriggerType.ANNOTATION_MEDIA,"Annotation Media",null,null,mediaData,Collections.emptyMap());
}

}
