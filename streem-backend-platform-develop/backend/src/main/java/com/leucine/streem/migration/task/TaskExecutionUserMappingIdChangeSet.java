package com.leucine.streem.migration.task;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

public class TaskExecutionUserMappingIdChangeSet implements CustomTaskChange {
  @Override
  public void execute(Database database) throws CustomChangeException {
    TaskExecutionUserMappingIdGeneration202403042316 taskExecutionUserMappingIdGeneration202403042316 = new TaskExecutionUserMappingIdGeneration202403042316();
    taskExecutionUserMappingIdGeneration202403042316.execute();
  }

  @Override
  public String getConfirmationMessage() {
    return null;
  }

  @Override
  public void setUp() throws SetupException {

  }

  @Override
  public void setFileOpener(ResourceAccessor resourceAccessor) {

  }

  @Override
  public ValidationErrors validate(Database database) {
    return null;
  }
}
