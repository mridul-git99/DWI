package com.leucine.streem.migration.approval;

public class Queries {
  public static final String GET_ALL_PARAMETER_VALUE_IDS_WITH_PENDING_FOR_APPROVAL = """
    select pv.id, jobs_id, task_executions_id, j.facilities_id, pv.reason, pv.created_at, pv.created_by, pv.modified_at, pv.modified_by, pv.value, j.organisations_id
    from parameter_values pv
             inner join jobs j on j.id = pv.jobs_id
    where pv.state = 'PENDING_FOR_APPROVAL' or pv.parameter_value_approval_id is not null
    order by pv.modified_at
    """;

  public static final String GET_ALL_JOB_ASSIGNEES_WITH_USER_GROUP_USERS_BY_ROLES = """
    WITH TaskExecutionUserMappingUsersAndUsergroups AS (SELECT DISTINCT teum.users_id,
                                                                        teum.user_groups_id,
                                                                        te.jobs_id
                                                        FROM task_execution_user_mapping teum
                                                                 INNER JOIN task_executions te ON teum.task_executions_id = te.id
                                                        WHERE te.jobs_id = ANY (?)
                                                          AND (teum.users_id IS NOT NULL OR teum.user_groups_id IS NOT NULL)),
        
         UserGroupUsers AS (SELECT DISTINCT ugm.users_id, jobs_id
                            FROM TaskExecutionUserMappingUsersAndUsergroups teum
                                     INNER JOIN user_group_members ugm ON teum.user_groups_id = ugm.groups_id),
        
        
         Roles AS (SELECT id
                   FROM roles
                   WHERE name IN ('ACCOUNT_OWNER', 'PROCESS_PUBLISHER', 'FACILITY_ADMIN', 'SUPERVISOR')),
        
         ParameterValueApprovals AS (SELECT DISTINCT pva.users_id, jobs_id
                                     FROM parameter_value_approvals pva
                                              INNER JOIN parameter_values pv ON pva.id = pv.parameter_value_approval_id
                                     WHERE jobs_id = ANY (?)),
        
         CombinedUsersAndUserGroups AS (SELECT users_id, jobs_id
                                        FROM TaskExecutionUserMappingUsersAndUsergroups
                                        WHERE users_id IS NOT NULL
                                        UNION
                                        (SELECT users_id, jobs_id FROM UserGroupUsers)
                                        UNION
                                        (SELECT users_id, jobs_id FROM ParameterValueApprovals)),
        
         FilteredUserRolesMapping AS (SELECT urm.users_id, jobs_id
                                      FROM user_roles_mapping urm
                                               INNER JOIN Roles r ON urm.roles_id = r.id
                                               INNER JOIN CombinedUsersAndUserGroups cuug ON urm.users_id = cuug.users_id)
        
    SELECT furm.users_id AS users_id, jobs_id
    FROM FilteredUserRolesMapping furm
    """;

  public static final String CREATE_EXCEPTION_ENTRY_FOR_PARAMETER_VALUE = """
    INSERT INTO exceptions (id, code, value, parameter_values_id, task_executions_id, facilities_id, jobs_id, status, initiators_reason, reviewers_reason, previous_state, created_by, created_at, modified_by, modified_at)
    VALUES (?, ?, ?, ?, ?, ? ,?, ?, ?, ?, ?, ?, ?, ?, ?)
    """;
  public static final String CREATE_EXCEPTION_REVIEWERS_ENTRY = """
    INSERT INTO exception_reviewers (id, exceptions_id, users_id, user_groups_id, created_by, created_at, modified_by, modified_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """;

  public static final String UPDATE_EXCEPTION_STATUS = """
    update exceptions e
    SET status = CASE
                     WHEN (pva.state) = 'APPROVED' then 'ACCEPTED'
                     ELSE 'REJECTED' END
    FROM parameter_value_approvals pva, parameter_values pv
    where e.parameter_values_id = pv.id and pv.parameter_value_approval_id = pva.id
    """;


  public static final String UPDATED_ACTION_PERFORMED = """
    update exception_reviewers er
    SET action_performed = true
    FROM parameter_value_approvals pva,
         exceptions e,
         parameter_values pv
    where er.users_id = pva.users_id
      and er.exceptions_id = e.id
      and pv.parameter_value_approval_id = pva.id
      and e.status in  ('ACCEPTED', 'REJECTED')
    """;

  public static final String CREATE_OR_UPDATE_CODE = "INSERT INTO codes (counter, type, clause, organisations_id) VALUES (1, ?, ?, ?) "
    + "ON CONFLICT (organisations_id, type, clause) DO UPDATE "
    + "SET counter = codes.counter + 1 returning *";

  public static final String UPDATE_HAS_EXCEPTION = """
    update parameter_values pv
    set has_exceptions = true
    from exceptions e
    where pv.id = e.parameter_values_id
    """;

  public static final String UPDATE_PARAMETER_VALUE_STATE_TO_EXECUTED_WITH_ACCEPTED_EXCEPTIONS = """
    update parameter_values pv
    set state = 'EXECUTED'
    from exceptions e
    where pv.id = e.parameter_values_id
      and e.status = ('ACCEPTED')
    """;

  public static final String UPDATE_BLOCKED_JOBS_STATE_TO_IN_PROGRESS = """
    UPDATE jobs
    SET state = 'IN_PROGRESS'
    WHERE state = 'BLOCKED'
    """;
}
