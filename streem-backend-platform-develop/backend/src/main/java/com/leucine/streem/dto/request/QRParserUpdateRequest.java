package com.leucine.streem.dto.request;

import com.leucine.streem.collections.parser.SplitDataRuleDto;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class QRParserUpdateRequest implements Serializable {
  @Serial
  private static final long serialVersionUID = -2752856869503545235L;
  private String displayName;

  private List<SplitDataRuleDto> rules;
}
