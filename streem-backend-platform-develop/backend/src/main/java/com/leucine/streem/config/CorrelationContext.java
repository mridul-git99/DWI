package com.leucine.streem.config;

import com.leucine.streem.model.helper.PrincipalUser;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component; /**
 * Utility class for accessing correlation context in your application code
 */
@Component
public class CorrelationContext {

  private static final String REQUEST_ID_KEY = "requestId";
  private static final String USER_ID_KEY = "userId";
  private static final String USERNAME_KEY = "username";
  private static final String EMPLOYEE_ID_KEY = "employeeId";
  private static final String ORGANISATION_ID_KEY = "organisationId";
  private static final String FACILITY_ID_KEY = "facilityId";
  private static final String TRACE_ID_KEY = "traceId";
  private static final String SPAN_ID_KEY = "spanId";

  // Correlation IDs
  public static String getCorrelationId() {
    return MDC.get(REQUEST_ID_KEY);
  }

  public static String getTraceId() {
    return MDC.get(TRACE_ID_KEY);
  }

  public static String getSpanId() {
    return MDC.get(SPAN_ID_KEY);
  }

  // User context
  public static String getUserId() {
    return MDC.get(USER_ID_KEY);
  }

  public static String getUsername() {
    return MDC.get(USERNAME_KEY);
  }

  public static String getEmployeeId() {
    return MDC.get(EMPLOYEE_ID_KEY);
  }

  public static String getOrganisationId() {
    return MDC.get(ORGANISATION_ID_KEY);
  }

  public static String getFacilityId() {
    return MDC.get(FACILITY_ID_KEY);
  }

  // Setters (for special cases)
  public static void setUserId(String userId) {
    MDC.put(USER_ID_KEY, userId);
  }

  public static void setCustomAttribute(String key, String value) {
    MDC.put(key, value);
  }

  public static void removeCustomAttribute(String key) {
    MDC.remove(key);
  }

  /**
   * Get complete correlation context for logging
   */
  public static String getFullContext() {
    return String.format("traceId=%s, spanId=%s, requestId=%s, userId=%s, username=%s, employeeId=%s, orgId=%s, facilityId=%s",
      getTraceId(), getSpanId(), getCorrelationId(), getUserId(), getUsername(),
      getEmployeeId(), getOrganisationId(), getFacilityId());
  }

  /**
   * Get user context only
   */
  public static String getUserContext() {
    return String.format("userId=%s, username=%s, employeeId=%s, orgId=%s, facilityId=%s",
      getUserId(), getUsername(), getEmployeeId(), getOrganisationId(), getFacilityId());
  }

  /**
   * Check if user is authenticated
   */
  public static boolean isUserAuthenticated() {
    String userId = getUserId();
    return userId != null && !userId.equals("anonymous") && !userId.equals("unknown") && !userId.equals("error");
  }

  /**
   * Get current user's PrincipalUser object from Security Context
   */
  public static PrincipalUser getCurrentUser() {
    try {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null && authentication.getPrincipal() instanceof PrincipalUser) {
        return (PrincipalUser) authentication.getPrincipal();
      }
    } catch (Exception e) {
      // Return null if unable to get current user
    }
    return null;
  }
}
