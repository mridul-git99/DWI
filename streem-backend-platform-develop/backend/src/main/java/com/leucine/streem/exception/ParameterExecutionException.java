package com.leucine.streem.exception;

import com.leucine.streem.dto.response.Error;
import lombok.Getter;

import java.util.List;

@Getter
public class ParameterExecutionException extends Exception {
  public List<Error> errorList;

  public ParameterExecutionException(final String message) {
    super(message);
  }

  public ParameterExecutionException(final String message, final List<Error> errors) {
    super(message);
    this.errorList = errors;
  }

  public ParameterExecutionException(final List<Error> errors) {
    super("Parameter execution failed with errors");
    this.errorList = errors;
  }
}
