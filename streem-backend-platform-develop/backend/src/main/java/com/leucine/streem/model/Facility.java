package com.leucine.streem.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.leucine.streem.constant.TableName;
import com.leucine.streem.model.helper.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = TableName.FACILITIES)
public class Facility extends BaseEntity implements Serializable {
  private static final long serialVersionUID = -2370840437917599044L;

  @Column(columnDefinition = "varchar", length = 255)
  private String name;

  @Column(columnDefinition = "boolean default false", nullable = false)
  private boolean archived = false;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organisations_id", nullable = false)
  private Organisation organisation;


  @JsonIgnore
  @Column(columnDefinition = "bigint", updatable = false, nullable = false)
  private Long createdAt;

  @JsonIgnore
  @Column(columnDefinition = "bigint", nullable = false)
  private Long modifiedAt;

  @Column(name = "time_zone", columnDefinition = "VARCHAR(30)", nullable = false)
  private String timeZone;

  @Column(name = "date_format", columnDefinition = "VARCHAR(50)", nullable = false)
  private String dateFormat;

  @Column(name = "time_format", columnDefinition = "VARCHAR(50)", nullable = false)
  private String timeFormat;

  @Column(name = "date_time_format", columnDefinition = "VARCHAR(50)", nullable = false)
  private String dateTimeFormat;
}
