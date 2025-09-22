package com.leucine.streem.constant;

public enum ProcessPermissionType {
  JOB_REVIEWER("Job Reviewer"),
  JOB_EXECUTOR("Job Executor"),
  JOB_MANAGER("Job Manager"),
  JOB_ISSUER("Job Issuer");

  private final String type;

  ProcessPermissionType(String type) {
    this.type = type;
  }

  public String get() {
    return type;
  }
}
