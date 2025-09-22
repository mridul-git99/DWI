package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"objectId"})
public class ResourceParameterChoiceDto implements Serializable {
  @Serial
  private static final long serialVersionUID = 7640717474536802863L;
  private String objectId;
  private String objectDisplayName;
  private String objectExternalId;
  private String collection;
  private boolean isExceptionApproved = false;
}
