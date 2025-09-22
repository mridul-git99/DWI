package com.leucine.streem.model.compositekey;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Embeddable
public class ActionFacilityCompositeKey implements Serializable {
  @Column(name = "actions_id", columnDefinition = "bigint")
  private Long actionId;

  @Column(name = "facilities_id", columnDefinition = "bigint")
  private Long facilityId;


  public ActionFacilityCompositeKey(Long actionId, Long facilityId) {
    this.actionId = actionId;
    this.facilityId = facilityId;
  }
}
