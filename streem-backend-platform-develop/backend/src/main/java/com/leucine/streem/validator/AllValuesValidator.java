package com.leucine.streem.validator;

import com.leucine.streem.util.Utility;

import java.util.Collection;
import java.util.Objects;

public class AllValuesValidator implements ConstraintValidator {
  private boolean isValid;
  private final Collection<?> expectedValues;
  private final String errorMessage;

  public AllValuesValidator(Collection<?> expectedValues, String errorMessage) {
    this.expectedValues = expectedValues;
    this.errorMessage = errorMessage;
  }

  @Override
  public void validate(Object value) {
    if (value instanceof Collection<?> collection) {
      this.isValid = collection.equals(expectedValues);
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
