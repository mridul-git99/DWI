package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"userId", "userGroupId"})
public class TrainedUsersDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -265263058061656140L;
  private String userId;
  private String employeeId;
  private String firstName;
  private String lastName;
  private String emailId;
  private List<RoleDto> roles;
  private String userGroupId;
  private String userGroupName;
  private String userGroupDescription;
  private boolean status;
  private Set<String> taskIds = new HashSet<>();
  private List<UserDto> users;
}
