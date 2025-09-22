package com.leucine.streem.dto.request;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ImportActionRequest extends CreateActionRequest{
  private String id;
  private List<ImportEffectRequest> effectRequests;
}
