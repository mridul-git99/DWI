package com.leucine.streem.dto.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class VariationMediaRequest implements Serializable {
  @Serial
  private static final long serialVersionUID = -1272843969059441928L;

  private String mediaId;
  private String name;
  private String description;
  private String link;
  private String type;
  private Boolean archived;

}
