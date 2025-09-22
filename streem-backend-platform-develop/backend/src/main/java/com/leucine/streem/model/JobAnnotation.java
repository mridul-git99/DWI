package com.leucine.streem.model;

import com.leucine.streem.constant.TableName;
import com.leucine.streem.model.helper.UserAuditIdentifiableBase;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
@Entity
@Table(name = TableName.JOB_ANNOTATIONS)
public class JobAnnotation extends UserAuditIdentifiableBase implements Serializable {
  private static final long serialVersionUID = 5872389438932087429L;

  @Column(columnDefinition = "text")
  private String remarks;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "jobs_id")
  private Job job;

  @Column(columnDefinition = "varchar", length = 20, nullable = false, updatable = false)
  private String code;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "jobAnnotation", cascade = CascadeType.ALL)
  private List<JobAnnotationMediaMapping> medias = new ArrayList<>();

  public void addMedia(Job job, Media media, User principalUserEntity) {
    JobAnnotationMediaMapping jobAnnotationMediaMapping = new JobAnnotationMediaMapping(this, job, media, principalUserEntity);
    medias.add(jobAnnotationMediaMapping);
  }

  public void addAllMedias(Job job, List<Media> medias, User principalUserEntity) {
    for (Media media : medias) {
      JobAnnotationMediaMapping jobAnnotationMediaMapping = new JobAnnotationMediaMapping(this, job, media, principalUserEntity);
      this.medias.add(jobAnnotationMediaMapping);
    }
  }
}
