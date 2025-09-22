package com.leucine.streem.dto;

import com.leucine.streem.constant.Type;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class CopyChecklistElementRequest implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private Type.ChecklistElementType type;
  private Long elementId;
}
