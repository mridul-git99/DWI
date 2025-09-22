package com.leucine.streem.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.dto.MediaDto;
import lombok.Data;

import java.util.Set;

@Data
public class ParameterCorrectionCorrectorRequest {
  private String correctorReason;
  private String newValue;
  private JsonNode newChoice;
  private Set<MediaDto> medias;
  private Long correctionId;
}


