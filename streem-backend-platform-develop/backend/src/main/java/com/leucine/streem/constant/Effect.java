package com.leucine.streem.constant;

import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.model.Action;
import com.leucine.streem.model.helper.UserAuditIdentifiableBase;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = TableName.EFFECTS)
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class Effect extends UserAuditIdentifiableBase {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "actions_id", nullable = false, updatable = false)
  private Action action;

  @Column(name = "actions_id", nullable = false, updatable = false, insertable = false)
  private Long actionsId;


  @Column(name = "order_tree", nullable = false)
  private Integer orderTree;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private EffectType effectType;

  @Column(name = "query ", columnDefinition = "jsonb")
  @org.hibernate.annotations.Type(type = "jsonb")
  private JsonNode query;

  @Column(name = "api_endpoint", columnDefinition = "jsonb")
  @org.hibernate.annotations.Type(type = "jsonb")
  private JsonNode apiEndpoint;

  @Column(name = "api_method", columnDefinition = "text")
  private String apiMethod;

  @org.hibernate.annotations.Type(type = "jsonb")
  @Column(name = "api_payload", columnDefinition = "jsonb")
  private JsonNode apiPayload;

  @org.hibernate.annotations.Type(type = "jsonb")
  @Column(name = "api_headers", columnDefinition = "jsonb")
  private JsonNode apiHeaders;

  @Column(name = "name", columnDefinition = "text", nullable = false)
  private String name;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Column(name = "archived", columnDefinition = "boolean")
  private boolean archived;

  @Column(name = "javascript_enabled", columnDefinition = "boolean DEFAULT false", nullable = false)
  private boolean javascriptEnabled = false;

}
