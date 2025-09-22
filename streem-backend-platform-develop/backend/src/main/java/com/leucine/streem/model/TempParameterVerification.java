package com.leucine.streem.model;


import com.leucine.streem.constant.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serial;

/**
 * Entity class for parameter verification
 * It stores the verification details for temp parameter value verifications
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = TableName.TEMP_PARAMETER_VERIFICATIONS)
public class TempParameterVerification extends VerificationBase {
  @Serial
  private static final long serialVersionUID = -2757075136442331231L;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "temp_parameter_values_id", updatable = false)
  private TempParameterValue tempParameterValue;

  @Column(name = "temp_parameter_values_id", columnDefinition = "bigint", insertable = false, updatable = false)
  private Long tempParameterValueId;

}
