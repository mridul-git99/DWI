package com.leucine.streem.dto;

import lombok.Data;
import org.springframework.util.StringUtils;

import javax.validation.constraints.AssertTrue;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Request DTO for bulk peer verification operations.
 * Supports both traditional peer verification and same session verification
 * through the individual VerifyRequest objects in the list.
 * 
 * Each VerifyRequest in the peerVerify list can independently specify:
 * - sameSession: boolean flag to indicate same session verification
 * - initiatorJwtToken: JWT token of the initiator (required when sameSession=true)
 * 
 * This allows mixed bulk operations where some verifications are traditional
 * and others are same session verification within the same request.
 * 
 * @author Development Team
 * @since 1.0
 * @see VerifyRequest for individual verification request details
 */
@Data
public class BulkPeerVerificationRequest implements Serializable {
  @Serial
  private static final long serialVersionUID = 8128123881237123L;
  
  /**
   * List of individual peer verification requests.
   * Each request can be either traditional or same session verification
   * based on the sameSession flag in the VerifyRequest.
   */
  private List<VerifyRequest> peerVerify;


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
