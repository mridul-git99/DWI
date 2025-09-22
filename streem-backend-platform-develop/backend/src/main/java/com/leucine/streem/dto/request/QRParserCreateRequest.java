package com.leucine.streem.dto.request;

import com.leucine.streem.collections.parser.SplitDataRuleDto;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class QRParserCreateRequest implements Serializable {
  @Serial
  private static final long serialVersionUID = -4626930962531966176L;

  private String displayName;
  private String objectTypeId;
  private String rawData;
  private String delimiter;
  private List<SplitDataRuleDto> rules;
}
