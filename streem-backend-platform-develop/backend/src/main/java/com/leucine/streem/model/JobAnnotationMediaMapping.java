package com.leucine.streem.model;

import com.leucine.streem.constant.TableName;
import com.leucine.streem.model.helper.UserAuditBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = TableName.JOB_ANNOTATION_MEDIA_MAPPING)
public class JobAnnotationMediaMapping extends UserAuditBase implements Serializable {

  private static final long serialVersionUID = 6357544475200744745L;

  @EmbeddedId
  private JobAnnotationMediaCompositeKey jobAnnotationMediaId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "job_annotations_id", nullable = false, insertable = false, updatable = false)
  private JobAnnotation jobAnnotation;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "jobs_id", nullable = false, insertable = false, updatable = false)
  private Job job;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "medias_id", nullable = false, insertable = false, updatable = false)
  private Media media;

  public JobAnnotationMediaMapping(JobAnnotation jobAnnotation, Job job, Media media, User principalUserEntity) {
    this.jobAnnotation = jobAnnotation;
    this.job = job;
    this.media = media;
    createdBy = principalUserEntity;
    modifiedBy = principalUserEntity;
    jobAnnotationMediaId = new JobAnnotationMediaCompositeKey(job.getId(), jobAnnotation.getId(), media.getId());
  }

}
