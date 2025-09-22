package com.leucine.streem.validator;

import java.util.Collection;

public class AnyValueValidator implements ConstraintValidator {
  private boolean isValid;
  private final Collection<?> expectedValues;
  private final String errorMessage;

  public AnyValueValidator(Collection<?> expectedValues, String errorMessage) {
    this.expectedValues = expectedValues;
    this.errorMessage = errorMessage;
  }

  @Override
  public void validate(Object value) {
    if (value instanceof Collection<?> collection) {
      this.isValid = collection.stream().anyMatch(expectedValues::contains);
    } else {
      this.isValid = false;
    }
  }

  @Override
  public boolean isValid() {
    return this.isValid;
  }

  @Override
  public String getErrorMessage() {
    return this.errorMessage;
  }
}
