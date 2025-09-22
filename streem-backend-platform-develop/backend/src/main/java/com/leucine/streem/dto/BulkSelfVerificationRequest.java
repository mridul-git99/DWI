package com.leucine.streem.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Request DTO for bulk self verification operations.
 * Supports both traditional self verification and same session verification
 * through the individual VerifyRequest objects in the list.
 * 
 * Each VerifyRequest in the selfVerify list can independently specify:
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
public class BulkSelfVerificationRequest implements Serializable {
  @Serial
  private static final long serialVersionUID = 8128123881238123L;
  
  /**
   * List of individual self verification requests.
   * Each request can be either traditional or same session verification
   * based on the sameSession flag in the VerifyRequest.
   */
  private List<VerifyRequest> selfVerify;
}
