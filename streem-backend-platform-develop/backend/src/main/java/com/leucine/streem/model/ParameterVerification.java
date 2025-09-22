package com.leucine.streem.model;

import com.leucine.streem.constant.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serial;

/**
 * Entity class for parameter verification
 * It stores the verification details for parameter value verifications
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = TableName.PARAMETER_VERIFICATIONS)
public class ParameterVerification extends VerificationBase {
  @Serial
  private static final long serialVersionUID = 4593012668682993733L;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "parameter_values_id", updatable = false)
  private ParameterValue parameterValue;

  @Column(name = "parameter_values_id", columnDefinition = "bigint", insertable = false, updatable = false)
  private Long parameterValueId;

}
