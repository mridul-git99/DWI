package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParameterExceptionReviewerDto implements Serializable {
  private static final long serialVersionUID = -3026728201694430753L;

  private UserDto user;
  private UserGroupDto userGroup;
  private boolean actionPerformed;
  private Long modifiedAt;
}
