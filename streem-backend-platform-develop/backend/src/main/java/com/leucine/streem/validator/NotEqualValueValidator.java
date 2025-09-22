package com.leucine.streem.validator;

import com.leucine.streem.util.Utility;

import java.util.Collection;
import java.util.Objects;

public class NotEqualValueValidator implements ConstraintValidator {
  private boolean isValid;
  private final Object value;
  private final String errorMessage;

  public NotEqualValueValidator(double value, String errorMessage) {
    this.value = value;
    this.errorMessage = errorMessage;
  }

  public NotEqualValueValidator(Object value, String errorMessage) {
    this.value = value;
    this.errorMessage = errorMessage;
  }

  @Override
  public void validate(Object value) {
    if (!Utility.isEmpty(value)) {
      if (Utility.isNumeric(value)) {
        this.isValid = Double.parseDouble(value.toString()) != Double.parseDouble(this.value.toString());
      } else if (value instanceof Collection<?> collection) {
        Collection<?> valueCollection = (Collection<?>) this.value;
        this.isValid = !Objects.equals(collection, valueCollection);
      } else {
        this.isValid = !Objects.equals(value, this.value);
      }
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
