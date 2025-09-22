package com.leucine.streem.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.constant.Action;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * DTO for {@link com.leucine.streem.model.Variation}
 */
@Data
public class VariationDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -7665588102503386286L;

  private String id;
  private String parameterId;
  private JsonNode details;
  private Action.Variation type;
  private String jobId;
  private String name;
  private String description;
  private String parameterName;
  private JsonNode oldVariation;
  private JsonNode newVariation;
  private Integer taskOrderTree;
  private Integer stageOrderTree;
  private String variationNumber;
  private Integer taskExecutionOrderTree;
  private String parameterType;
  private List<MediaDto> medias;

}
