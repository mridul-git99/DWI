package com.leucine.streem.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.constant.TableName;
import com.leucine.streem.constant.Type;
import com.leucine.streem.model.helper.UserAuditIdentifiableBase;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@Entity
@Table(name = TableName.INTERLOCKS)
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class Interlock extends UserAuditIdentifiableBase implements Serializable {
  private static final long serialVersionUID = -3623930069948465034L;

  @Column(columnDefinition = "varchar", nullable = false)
  @Enumerated(EnumType.STRING)
  private Type.InterlockTargetEntityType targetEntityType;

  @Column(columnDefinition = "bigint")
  private Long targetEntityId;

  @org.hibernate.annotations.Type(type = "jsonb")
  @Column(name = "validations", columnDefinition = "jsonb", nullable = false)
  private JsonNode validations;

}
