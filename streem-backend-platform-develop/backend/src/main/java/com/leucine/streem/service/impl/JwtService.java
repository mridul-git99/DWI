package com.leucine.streem.service.impl;

import com.leucine.streem.config.JaasServiceProperty;
import com.leucine.streem.dto.RoleDto;
import com.leucine.streem.dto.request.AuthorizationRequest;
import com.leucine.streem.dto.response.Error;
import com.leucine.streem.exception.ExceptionType;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.service.IJwtService;
import com.leucine.streem.dto.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Implementation of JWT service for parsing and validating JWT tokens.
 * 
 * This service validates JWT tokens using the JAAS service to support same session
 * verification where initiator JWT tokens need to be processed independently
 * from the standard authentication flow.
 * 
 * Instead of parsing tokens locally, this service delegates to the JAAS service
 * for proper token validation and user context extraction.
 * 
 * @author Development Team
 * @since 1.0
 */
@Slf4j
@Service
public class JwtService implements IJwtService {

    private static final String BEARER_TOKEN_PREFIX = "Bearer ";

    @Autowired
    private JaasServiceProperty jaasServiceProperty;

    @Autowired
    @Qualifier("authenticationFilterRestTemplate")
    private RestTemplate restTemplate;

    @Override
    public PrincipalUser parseAndValidate(String token) throws StreemException {
        log.debug("[parseAndValidate] Validating JWT token via JAAS service for same session verification");
        
        try {
            // Use JAAS service to validate the token, similar to AuthenticationFilter
            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.set("Authorization", BEARER_TOKEN_PREFIX + token);
            
            // Create a dummy authorization request for token validation
            AuthorizationRequest authRequest = AuthorizationRequest.builder()
                    .path("/v1/parameter-verifications") // Generic path for validation
                    .method("PATCH")
                    .build();
            
            HttpEntity<AuthorizationRequest> requestEntity = new HttpEntity<>(authRequest, requestHeaders);
            
            ResponseEntity<PrincipalUser> jaasResponse = restTemplate.exchange(
                    jaasServiceProperty.getAuthorizeUrl(), 
                    HttpMethod.PATCH, 
                    requestEntity,
                    PrincipalUser.class
            );
            
            PrincipalUser principalUser = jaasResponse.getBody();
            if (principalUser == null) {
                throw new StreemException("Invalid token - no user context returned");
            }
            
            log.debug("[parseAndValidate] Successfully validated token for user ID: {}", principalUser.getId());
            return principalUser;
            
        } catch (HttpClientErrorException e) {
            log.warn("[parseAndValidate] JAAS service rejected token: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            Error error = Error.builder()
                    .type(ExceptionType.UNAUTHORIZED.get())
                    .code(ErrorCode.NOT_AUTHORIZED.getCode())
                    .message("Token validation failed: " + e.getMessage())
                    .build();
            throw new StreemException("Failed to validate JWT token", Collections.singletonList(error));
        } catch (Exception e) {
            log.error("[parseAndValidate] Unexpected error validating JWT token: {}", e.getMessage(), e);
            Error error = Error.builder()
                    .type(ExceptionType.SERVICE_ERROR.get())
                    .code(ErrorCode.NOT_AUTHORIZED.getCode())
                    .message("Failed to parse JWT token")
                    .build();
            throw new StreemException("Failed to parse JWT token", Collections.singletonList(error));
        }
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            parseAndValidate(token);
            return true;
        } catch (Exception e) {
            log.debug("[isTokenValid] Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Long extractUserId(String token) throws StreemException {
        try {
            PrincipalUser principalUser = parseAndValidate(token);
            if (principalUser.getId() != null) {
                return principalUser.getId();
            }
            
            Error error = Error.builder()
                    .type(ExceptionType.BAD_REQUEST.get())
                    .code(ErrorCode.NOT_AUTHORIZED.getCode())
                    .message("User ID not found in JWT token")
                    .build();
            throw new StreemException("User ID not found in JWT token", Collections.singletonList(error));
            
        } catch (StreemException e) {
            throw e; // Re-throw StreemException as-is
        } catch (Exception e) {
            log.warn("[extractUserId] Failed to extract user ID from JWT token: {}", e.getMessage());
            Error error = Error.builder()
                    .type(ExceptionType.BAD_REQUEST.get())
                    .code(ErrorCode.NOT_AUTHORIZED.getCode())
                    .message("Failed to extract user ID from JWT token")
                    .build();
            throw new StreemException("Failed to extract user ID from JWT token", Collections.singletonList(error));
        }
    }

    @Override
    public boolean isTokenExpired(String token) {
        try {
            parseAndValidate(token);
            return false; // If validation succeeds, token is not expired
        } catch (Exception e) {
            log.debug("[isTokenExpired] Token validation failed, considering as expired: {}", e.getMessage());
            return true; // Consider invalid tokens as expired
        }
    }

}
