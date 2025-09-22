package com.leucine.streem.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Getter
@Setter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
public class JobAnnotationMediaCompositeKey implements Serializable {
  private static final long serialVersionUID = 5252607487636908015L;

  @Column(name = "jobs_id", columnDefinition = "bigint")
  private Long jobId;

  @Column(name = "job_annotations_id", columnDefinition = "bigint")
  private Long jobAnnotationId;

  @Column(name = "medias_id", columnDefinition = "bigint")
  private Long mediaId;

  public JobAnnotationMediaCompositeKey(Long jobId, Long jobAnnotationId, Long mediaId) {
    this.jobId = jobId;
    this.jobAnnotationId = jobAnnotationId;
    this.mediaId = mediaId;
  }

}
