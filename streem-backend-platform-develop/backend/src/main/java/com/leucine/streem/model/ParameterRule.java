package com.leucine.streem.model;

import com.leucine.streem.constant.TableName;
import com.leucine.streem.model.helper.BaseEntity;
import com.vladmihalcea.hibernate.type.array.StringArrayType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = TableName.PARAMETER_RULES)
@TypeDefs(@TypeDef(name = "string-array", typeClass = StringArrayType.class))
public class ParameterRule extends BaseEntity implements Serializable {
  @Serial
  private static final long serialVersionUID = 1298925624296490837L;

  @Column(columnDefinition = "varchar", name = "rules_id", nullable = false)
  private String ruleId;

  // TODO supporting only equals for now, we will update this to enum later
  @Column(columnDefinition = "varchar", name = "operator", nullable = false)
  private String operator;

  @Column(name = "input", columnDefinition = "text",nullable = false)
  @Type(type = "string-array")
  private String[] input;

  @Column(columnDefinition = "boolean", name = "visibility", nullable = false)
  private boolean visibility;

  @OneToMany(mappedBy = "parameterRule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<ParameterRuleMapping> parameterMappings;


}
