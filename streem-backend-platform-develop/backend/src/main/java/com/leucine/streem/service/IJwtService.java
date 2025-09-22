package com.leucine.streem.service;

import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.exception.StreemException;
import org.springframework.stereotype.Service;

/**
 * Service interface for JWT token operations.
 * Provides methods to parse, validate, and extract user context from JWT tokens.
 * 
 * This service is specifically designed to support same session verification
 * where both verifier (Bearer token) and initiator (payload token) JWTs need
 * to be processed independently.
 * 
 * @author Development Team
 * @since 1.0
 */
@Service
public interface IJwtService {

    /**
     * Parses and validates a JWT token string to extract user context.
     * 
     * This method can be used to parse any valid JWT token, including:
     * - Bearer tokens from Authorization headers
     * - Initiator tokens from request payloads (for same session verification)
     * 
     * @param token The JWT token string to parse and validate
     * @return PrincipalUser object containing the user context from the token
     * @throws StreemException if the token is invalid, expired, or malformed
     */
    PrincipalUser parseAndValidate(String token) throws StreemException;

    /**
     * Validates a JWT token without extracting the full user context.
     * 
     * This method performs basic validation checks:
     * - Token signature verification
     * - Expiration time validation
     * - Token format validation
     * 
     * @param token The JWT token string to validate
     * @return true if the token is valid, false otherwise
     */
    boolean isTokenValid(String token);

    /**
     * Extracts the user ID from a JWT token without full validation.
     * 
     * This method is useful for quick user identification when full
     * validation is not required or will be performed later.
     * 
     * @param token The JWT token string
     * @return The user ID extracted from the token
     * @throws StreemException if the token cannot be parsed
     */
    Long extractUserId(String token) throws StreemException;

    /**
     * Checks if a JWT token is expired.
     * 
     * @param token The JWT token string to check
     * @return true if the token is expired, false otherwise
     */
    boolean isTokenExpired(String token);
}
