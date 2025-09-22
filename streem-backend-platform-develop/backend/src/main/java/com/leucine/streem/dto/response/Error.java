package com.leucine.streem.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Error {
  private String id;
  private String userId;
  private String userGroupId;
  private String type;
  private String code;
  private String message;
  private Object errorInfo;

  @Override
  public String toString() {
    return "Error{" +
      "id='" + id + '\'' +
      ", userId='" + userId + '\'' +
      ", userGroupId='" + userGroupId + '\'' +
      ", type='" + type + '\'' +
      ", code='" + code + '\'' +
      ", message='" + message + '\'' +
      ", errorInfo=" + errorInfo +
      '}';
  }
}
