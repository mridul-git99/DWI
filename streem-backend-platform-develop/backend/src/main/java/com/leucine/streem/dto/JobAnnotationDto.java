package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobAnnotationDto implements Serializable {
  private static final long serialVersionUID = 2726275850385527161L;

  private String id;
  private String remarks;
  private String code;
  private Long jobId;
  private UserAuditDto createdBy;
  private Long createdAt;
  private UserAuditDto modifiedBy;
  private Long modifiedAt;
  private List<MediaDto> medias = new ArrayList<>();
}
