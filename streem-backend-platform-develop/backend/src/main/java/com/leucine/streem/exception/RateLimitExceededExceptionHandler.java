package com.leucine.streem.exception;

import com.leucine.streem.dto.response.Error;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.model.helper.PrincipalUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@ControllerAdvice
public class RateLimitExceededExceptionHandler {

  @ExceptionHandler(RateLimitExceededException.class)
  public ResponseEntity<Object> handleRateLimitExceededException(RateLimitExceededException ex) {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Long userId = principalUser.getId();

    log.error("[handleRateLimitExceededException] userId: {}, error: ",userId, ex);
    List<Error> errorList = new ArrayList<>();
    errorList.add(Error.builder().type(ExceptionType.RATE_LIMIT_EXCEEDED.get())
      .userId(String.valueOf(userId))
      .code(ErrorCode.RATE_LIMIT_EXCEEDED.getCode())
      .message(ErrorCode.RATE_LIMIT_EXCEEDED.getDescription()).build());
    return new ResponseEntity<>(errorList, HttpStatus.TOO_MANY_REQUESTS);
  }
}
