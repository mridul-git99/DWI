package com.leucine.streem.model;

import com.leucine.streem.constant.TableName;
import com.leucine.streem.model.compositekey.VariationMediaCompositeKey;
import com.leucine.streem.model.helper.UserAuditBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = TableName.VARIATION_MEDIA_MAPPING)
public class VariationMediaMapping extends UserAuditBase implements Serializable {
  @Serial
  private static final long serialVersionUID = -2995847921924111713L;

  @EmbeddedId
  private VariationMediaCompositeKey variationMediaId;

  @Column(columnDefinition = "boolean default false")
  private boolean archived = false;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "variations_id", nullable = false, insertable = false, updatable = false)
  private Variation variation;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "medias_id", nullable = false, insertable = false, updatable = false)
  private Media media;

  public VariationMediaMapping(Variation variation, Media media, User principalUserEntity) {
    this.variation = variation;
    this.media = media;
    createdBy = principalUserEntity;
    modifiedBy = principalUserEntity;
    variationMediaId = new VariationMediaCompositeKey(variation.getId(), media.getId());
  }
}
