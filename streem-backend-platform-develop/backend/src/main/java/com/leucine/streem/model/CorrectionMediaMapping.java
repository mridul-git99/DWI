package com.leucine.streem.model;

import com.leucine.streem.constant.TableName;
import com.leucine.streem.model.helper.UserAuditIdentifiableBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = TableName.CORRECTION_MEDIA_MAPPING)
public class CorrectionMediaMapping  extends UserAuditIdentifiableBase implements Serializable {
  private static final long serialVersionUID = 6357544475800744744L;

  @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
  @JoinColumn(name = "corrections_id", updatable = false)
  private Correction correction;

  @Column(columnDefinition = "bigint", name = "corrections_id", updatable = false, insertable = false)
  private Long correctionId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "medias_id", nullable = false)
  private Media media;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "parameter_values_id", updatable = false)
  private ParameterValue parameterValue;

  @Column(columnDefinition = "boolean default false")
  private boolean isOldMedia = false;

  @Column(columnDefinition = "boolean default false")
  private boolean archived = false;
}
