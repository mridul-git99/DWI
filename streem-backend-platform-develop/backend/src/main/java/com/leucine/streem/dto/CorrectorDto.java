package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CorrectorDto implements Serializable {
  private static final long serialVersionUID = -3026728201574438753L;

  private UserDto user;
  private UserGroupDto userGroup;
  private boolean actionPerformed;
  private Long modifiedAt;
}
