package com.leucine.streem.dto;

import lombok.Data;
import org.springframework.util.StringUtils;

import javax.validation.constraints.AssertTrue;

/**
 * Request DTO for individual parameter verification operations.
 * Used in bulk verification requests and supports both traditional peer verification 
 * and same session verification.
 * 
 * @author Development Team
 * @since 1.0
 */
@Data
public class VerifyRequest {
  
  /**
   * The ID of the parameter execution to be verified.
   */
  private Long parameterExecutionId;
  
  /**
   * Timestamp when the verification was checked.
   */
  private Long checkedAt;
  
  /**
   * Flag to indicate if this is a same session verification.
   * Defaults to false for backward compatibility with existing clients.
   * When true, initiatorJwtToken must be provided.
   */
  private Boolean sameSession = false;
  
  /**
   * JWT token of the user who initiated the same session verification.
   * Required when sameSession is true, ignored when sameSession is false.
   * This token is used to extract the initiator's user context for audit purposes.
   */
  private String initiatorJwtToken;
  
  /**
   * Validation method to ensure that same session verification requests
   * include the required initiator JWT token.
   * 
   * @return true if the request is valid, false otherwise
   */
  @AssertTrue(message = "Same session verification requires initiator JWT token")
  public boolean isValidSameSessionRequest() {
    return !Boolean.TRUE.equals(sameSession) || StringUtils.hasText(initiatorJwtToken);
  }
}
