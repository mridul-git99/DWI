package com.leucine.streem.model.helper.search;

public enum SearchOperator {
  AND("and"),
  ANY("is in"),
  ALL("is all"),
  NOT_ALL("is not all"),
  EQ("is equal to"),
  GT("is greater than"),
  GTE("is greater or equal"),
  GOE("is greater or equal"),
  LT("is less than"),
  LTE("is less than or equal"),
  LOE("is less than or equal"),
  LIKE("is like"),
  NE("is not equal to"),
  OR("or"),
  NEITHER("neither"),
  NIN("is not in"),
  STARTS_WITH("starts with"),
  IS_NOT_SET("is not set");

  private final String operator;

  SearchOperator(String operator) {
    this.operator = operator;
  }

  public String getOperator() {
    return operator;
  }
}
