package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecutionEngagedUserDto implements Serializable {
  private String id;
  private String employeeId;
  private String firstName;
  private String lastName;
  private boolean actionPerformed;
}
