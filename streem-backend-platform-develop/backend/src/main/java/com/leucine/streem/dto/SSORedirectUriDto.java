package com.leucine.streem.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * Example
 * For Sign off frontend sends the state of the application as follows:
 * {
 *   "checklistId": "403074605063438336",
 *   "location": "/checklists/403074605063438336",
 *   "state": "SIGN_OFF"
 * }
 */
@Data
public class SSORedirectUriDto implements Serializable {

  @Serial
  private static final long serialVersionUID = -6178484375841105981L;

  private Map<Object, Object> data;

}
