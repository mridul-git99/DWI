package com.leucine.streem.migration.approval;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

public class ParameterApprovalChangeSet implements CustomTaskChange {
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

  @Override
  public void execute(Database database) throws CustomChangeException {
    ParameterApprovalMigration202405231628 parameterApprovalMigration202405231628 = new ParameterApprovalMigration202405231628();
    parameterApprovalMigration202405231628.execute();
  }
}
