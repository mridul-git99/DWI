package com.leucine.streem.model.compositekey;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class VariationMediaCompositeKey implements Serializable {
  @Serial
  private static final long serialVersionUID = -8893425612185896797L;

  @Column(name = "variations_id", columnDefinition = "bigint")
  private Long variationId;

  @Column(name = "medias_id", columnDefinition = "bigint")
  private Long mediaId;
}
