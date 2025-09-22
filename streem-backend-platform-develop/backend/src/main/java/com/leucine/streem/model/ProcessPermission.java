package com.leucine.streem.model;

import com.leucine.streem.constant.ProcessPermissionType;
import com.leucine.streem.constant.TableName;
import com.leucine.streem.model.helper.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;

@Entity
@Table(name = TableName.PROCESS_PERMISSION)
@Getter
@Setter
public class ProcessPermission extends BaseEntity implements Serializable {
  @Serial
  private static final long serialVersionUID = 8696377191557412403L;


  @Column(name = "type", columnDefinition = "varchar", length = 50, nullable = false)
  @Enumerated(EnumType.STRING)
  private ProcessPermissionType type;

  @Column(columnDefinition = "text")
  private String description;

}
