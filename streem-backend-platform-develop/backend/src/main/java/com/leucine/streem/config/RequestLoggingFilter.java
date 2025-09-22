package com.leucine.streem.config;

import org.slf4j.MDC;

import javax.servlet.*;
import java.io.IOException;
import java.util.UUID;

public class RequestLoggingFilter implements Filter {
  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    Filter.super.init(filterConfig);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    try {
      // Generate a unique identifier for this request
      String requestId = UUID.randomUUID().toString();
      MDC.put("requestId", requestId);

      chain.doFilter(request, response);
    } finally {
      // Clear MDC data to avoid leaks
      MDC.clear();
    }
  }

  @Override
  public void destroy() {
    Filter.super.destroy();
  }
}
