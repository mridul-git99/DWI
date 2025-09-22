package com.leucine.streem.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.constant.Action;
import com.leucine.streem.constant.TableName;
import com.leucine.streem.model.helper.UserAuditIdentifiableBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = TableName.VARIATIONS)
public class Variation extends UserAuditIdentifiableBase implements Serializable {
  @Serial
  private static final long serialVersionUID = 7342531505657562073L;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parameter_values_id", nullable = false)
  private ParameterValue parameterValue;

  @Column(columnDefinition = "bigint", name = "parameter_values_id", updatable = false, insertable = false)
  private Long parameterValueId;


  @Type(type = "jsonb")
  @Column(name = "new_details", columnDefinition = "jsonb default '{}'", nullable = false)
  private JsonNode newDetails;

  @Type(type = "jsonb")
  @Column(name = "old_details", columnDefinition = "jsonb default '{}'", nullable = false)
  private JsonNode oldDetails;


  @Column(columnDefinition = "varchar", length = 50, nullable = false)
  @Enumerated(EnumType.STRING)
  private Action.Variation type;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "jobs_id", updatable = false)
  private Job job;


  @Column(name = "jobs_id", columnDefinition = "bigint", insertable = false, updatable = false)
  private Long jobId;

  @Column(name = "name", columnDefinition = "text", nullable = false)
  private String name;

  @Column(name = "description", columnDefinition = "text", nullable = false)
  private String description;

  @Column(name = "variation_number", columnDefinition = "text")
  private String variationNumber;

  @Column(name = "config_id", columnDefinition = "text", nullable = false)
  private String configId;

 // Currently only one media per variation is supported
  @OneToMany(mappedBy = "variation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<VariationMediaMapping> medias = new HashSet<>();


  public void addMedia(Media media, User principalUserEntity) {
    VariationMediaMapping variationMediaMapping = new VariationMediaMapping(this, media, principalUserEntity);
    medias.add(variationMediaMapping);
  }
}
