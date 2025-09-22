package com.leucine.streem.model;

import com.leucine.streem.model.compositekey.ParameterRuleMappingCompositeKey;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;

@Entity
@Table(name = "parameter_rule_mapping")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ParameterRuleMapping implements Serializable {

  @Serial
  private static final long serialVersionUID = 7716876838763714621L;
  @EmbeddedId
  private ParameterRuleMappingCompositeKey parameterRuleMappingCompositeKey;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("parameterRuleId")
  @JoinColumn(name = "parameter_rules_id")
  private ParameterRule parameterRule;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("impactedParameterId")
  @JoinColumn(name = "impacted_parameters_id")
  private Parameter impactedParameter;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("triggeringParameterId")
  @JoinColumn(name = "triggering_parameters_id")
  private Parameter triggeringParameter;

}
