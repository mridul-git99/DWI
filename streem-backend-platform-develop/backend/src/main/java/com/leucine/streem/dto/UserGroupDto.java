package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserGroupDto {
  private String id;
  private String name;
  private String description;
  private boolean active;
  private String reason;
  private Long facilityId;
  private List<UserDto> users;
  private Integer userCount;
  private List<String> allUserIds;

  public UserGroupDto(String id, String name, String description, boolean active) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.active = active;
  }
}
