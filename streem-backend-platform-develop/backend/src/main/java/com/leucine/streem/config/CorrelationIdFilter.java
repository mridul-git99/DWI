package com.leucine.streem.config;

import com.leucine.streem.model.helper.PrincipalUser;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * Correlation ID filter for enhanced logging with tracing integration
 * Works alongside OpenTelemetry automatic tracing
 * Extracts user information from Spring Security context after authentication
 */
public class CorrelationIdFilter implements Filter {

  private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
  private static final String REQUEST_ID_KEY = "requestId";
  private static final String USER_ID_KEY = "userId";
  private static final String USERNAME_KEY = "username";
  private static final String EMPLOYEE_ID_KEY = "employeeId";
  private static final String ORGANISATION_ID_KEY = "organisationId";
  private static final String FACILITY_ID_KEY = "facilityId";

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
    throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    try {
      // Generate or extract correlation ID
      String correlationId = extractOrGenerateCorrelationId(httpRequest);

      // Extract user information from Spring Security context
      UserInfo userInfo = extractUserInfo();

      // Set MDC context for logging (traceId and spanId are automatically set by OpenTelemetry)
      MDC.put(REQUEST_ID_KEY, correlationId);
      MDC.put(USER_ID_KEY, userInfo.userId);
      MDC.put(USERNAME_KEY, userInfo.username);
      MDC.put(EMPLOYEE_ID_KEY, userInfo.employeeId);
      MDC.put(ORGANISATION_ID_KEY, userInfo.organisationId);
      MDC.put(FACILITY_ID_KEY, userInfo.facilityId);

      // Add correlation ID and user context to response headers for client tracking
      httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);
      httpResponse.setHeader("X-User-ID", userInfo.userId);
      httpResponse.setHeader("X-Username", userInfo.username);

      chain.doFilter(request, response);

    } finally {
      // Clean up MDC keys (traceId/spanId handled by OpenTelemetry)
      MDC.remove(REQUEST_ID_KEY);
      MDC.remove(USER_ID_KEY);
      MDC.remove(USERNAME_KEY);
      MDC.remove(EMPLOYEE_ID_KEY);
      MDC.remove(ORGANISATION_ID_KEY);
      MDC.remove(FACILITY_ID_KEY);
    }
  }

  private String extractOrGenerateCorrelationId(HttpServletRequest request) {
    // Check if correlation ID is provided in request headers
    String correlationId = request.getHeader(CORRELATION_ID_HEADER);

    if (correlationId == null || correlationId.trim().isEmpty()) {
      // Generate new correlation ID
      correlationId = UUID.randomUUID().toString();
    }

    return correlationId;
  }

  private UserInfo extractUserInfo() {
    UserInfo userInfo = new UserInfo();

    try {
      // Get authentication from Spring Security context
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

      if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof PrincipalUser) {
        PrincipalUser principalUser = (PrincipalUser) authentication.getPrincipal();

        // Extract user information from PrincipalUser
        userInfo.userId = principalUser.getId() != null ? principalUser.getId().toString() : "unknown";
        userInfo.username = principalUser.getUsername() != null ? principalUser.getUsername() : "anonymous";
        userInfo.employeeId = principalUser.getEmployeeId() != null ? principalUser.getEmployeeId() : "unknown";
        userInfo.organisationId = principalUser.getOrganisationId() != null ? principalUser.getOrganisationId().toString() : "unknown";
        userInfo.facilityId = principalUser.getCurrentFacilityId() != null ? principalUser.getCurrentFacilityId().toString() : "unknown";

      } else {
        // No authenticated user or different principal type
        userInfo.userId = "anonymous";
        userInfo.username = "anonymous";
        userInfo.employeeId = "unknown";
        userInfo.organisationId = "unknown";
        userInfo.facilityId = "unknown";
      }
    } catch (Exception e) {
      // Log error but don't fail the request
      // Use defaults for anonymous user
      userInfo.userId = "error";
      userInfo.username = "error";
      userInfo.employeeId = "error";
      userInfo.organisationId = "error";
      userInfo.facilityId = "error";
    }

    return userInfo;
  }

  /**
   * Helper class to hold user information
   */
  private static class UserInfo {
    String userId = "anonymous";
    String username = "anonymous";
    String employeeId = "unknown";
    String organisationId = "unknown";
    String facilityId = "unknown";
  }
}

