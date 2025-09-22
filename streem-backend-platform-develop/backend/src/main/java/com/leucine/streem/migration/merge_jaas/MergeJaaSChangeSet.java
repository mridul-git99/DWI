package com.leucine.streem.migration.merge_jaas;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

public class MergeJaaSChangeSet implements CustomTaskChange {
  @Override
  public void execute(Database database) throws CustomChangeException {
    MergeJaaS202404032134 jaaS202404032134 = new MergeJaaS202404032134();
    jaaS202404032134.execute();
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
