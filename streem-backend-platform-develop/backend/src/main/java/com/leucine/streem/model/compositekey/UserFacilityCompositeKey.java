package com.leucine.streem.model.compositekey;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Data
@NoArgsConstructor
@EqualsAndHashCode
@Embeddable
public class UserFacilityCompositeKey implements Serializable {
  private static final long serialVersionUID = -2637738888688115603L;

  @Column(name = "users_id", columnDefinition = "bigint")
  private Long userId;

  @Column(name = "facilities_id", columnDefinition = "bigint")
  private Long facilityId;

  public UserFacilityCompositeKey(Long userId, Long facilityId) {
    this.userId = userId;
    this.facilityId = facilityId;
  }
}

