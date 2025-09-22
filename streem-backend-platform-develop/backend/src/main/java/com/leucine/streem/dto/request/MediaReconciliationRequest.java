package com.leucine.streem.dto.request;

import com.leucine.streem.dto.MediaDto;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MediaReconciliationRequest {
  private List<MediaDto> medias = new ArrayList<>();
}
