package com.leucine.streem.model.compositekey;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
public class ParameterRuleMappingCompositeKey implements Serializable {
  @Serial
  private static final long serialVersionUID = 5706477668157235989L;

  private Long parameterRuleId;
  private Long impactedParameterId;
  private Long triggeringParameterId;

}
