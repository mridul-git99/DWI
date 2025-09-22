package com.leucine.streem.constant;

public final class Queries {

  public static final String GET_TEMP_PARAMETER_VALUE_BY_PARAMETER_ID_AND_TASK_EXECUTION_ID = "select tv from TempParameterValue tv where tv" +
    ".parameterId = :parameterId and tv.taskExecutionId = :taskExecutionId";
  public static final String CREATE_OR_UPDATE_CODE = "INSERT INTO codes (counter, type, clause, organisations_id) VALUES (1, ?, ?, ?) "
    + "ON CONFLICT (organisations_id, type, clause) DO UPDATE "
    + "SET counter = codes.counter + 1 returning *";
  //TODO remove hardcoded states in both the below queries so query can be used in other places
  public static final String GET_ALL_INCOMPLETE_PARAMETER_VALUE_ID_AND_TASK_EXECUTION_ID_BY_JOB_ID = """
    select distinct pv.id as parameterValueId, te.id as taskExecutionId
    from jobs j
             inner join task_executions te on j.id = te.jobs_id
             inner join parameters p on te.tasks_id = p.tasks_id
             inner join parameter_values pv on p.id = pv.parameters_id and pv.jobs_id = j.id
        
    where te.state not in ('COMPLETED',
                           'COMPLETED_WITH_EXCEPTION', 'SKIPPED',
                           'COMPLETED_WITH_CORRECTION')
      and p.is_mandatory = true
      and p.archived = false
      and pv.state <> 'EXECUTED'
      and pv.hidden <> true
      and j.id = :jobId
    """;
  public static final String GET_INCOMPLETE_PARAMETER_IDS_BY_JOB_ID_AND_TASK_EXECUTION_ID = """
    select av.id
    from parameter_values av
    inner join parameters p on av.parameters_id = p.id
    where av.jobs_id = :jobId
      and av.task_executions_id = :taskExecutionId
      and av.state <> 'EXECUTED'
      and av.hidden <> true
      and p.is_mandatory = true
      
    """;
  public static final String GET_INCOMPLETE_TEMP_PARAMETER_IDS_BY_JOB_ID_AND_TASK_EXECUTION_ID = """
    select tpv.id
        from temp_parameter_values tpv
        inner join parameters p on tpv.parameters_id = p.id
        where tpv.jobs_id = :jobId
          and tpv.task_executions_id = :taskExecutionId
          and tpv.state <> 'EXECUTED'
          and tpv.hidden <> true
          and p.is_mandatory = true
    """;
  public static final String GET_EXECUTABLE_PARAMETER_IDS_BY_TASK_ID = "select act.id from Parameter act where act.task.id = :taskId and act.type not in('INSTRUCTION', 'MATERIAL')";
  public static final String UPDATE_PARAMETER_VALUES = "update parameter_values set value = :value, choices = :choices, state=:state, reason = :reason , modified_by = :modifiedBy, modified_at = :modifiedAt where parameters_id = :parameterId and task_executions_id = :taskExecutionId";
  public static final String UPDATE_TASK_EXECUTION_ENABLE_CORRECTION = "update task_executions set correction_enabled='true', correction_reason=:correctionReason, corrected_at = null, corrected_by = null where id = :id";
  public static final String UPDATE_TASK_EXECUTION_CANCEL_CORRECTION = "update task_executions set correction_enabled='false',correction_reason=null where id = :id";
  public static final String UPDATE_TEMP_PARAMETER_VALUE_AND_STATE_BY_PARAMETER_AND_JOB_ID = "update temp_parameter_values set value=:value, state=:state, modified_at = :modifiedAt, modified_by = :modifiedBy where parameters_id=:parameterId and " +
    "task_executions_id = :taskExecutionId";
  public static final String GET_ALL_TASK_ASSIGNEES_DETAILS_BY_JOB_ID = """
    WITH taskExecutionAssigneeDetail AS (SELECT teum.users_id                                         AS users_id,
                                                teum.user_groups_id                                   AS user_groups_id,
                                                COUNT(*)                                              AS assigned_tasks,
                                                COUNT(CASE WHEN teum.state = 'SIGNED_OFF' THEN 1 END) AS signed_off_tasks,
        
                                                COUNT(CASE
                                                          WHEN (teum.state != 'SIGNED_OFF' AND tex.state IN
                                                                                               ('COMPLETED', 'SKIPPED',
                                                                                                'COMPLETED_WITH_EXCEPTION',
                                                                                                'COMPLETED_WITH_CORRECTION'))
                                                              THEN 1
                                                    END)                                              AS pending_sign_offs,
        
                                                CASE
                                                    WHEN (count(teum.task_executions_id)) = :totalExecutionIds THEN TRUE
                                                    ELSE FALSE END                                    AS completely_assigned
                                         FROM task_execution_user_mapping teum
                                                  inner join task_executions tex
                                                             on tex.id = teum.task_executions_id
                                         WHERE teum.task_executions_id IN
                                               (SELECT te.id FROM task_executions te WHERE jobs_id = :jobId)
                                         GROUP BY teum.users_id, teum.user_groups_id)
    SELECT tad.users_id            as userId,
           tad.assigned_tasks      as assignedTasks,
           tad.pending_sign_offs   as pendingSignOffs,
           tad.completely_assigned as completelyAssigned,
           tad.signed_off_tasks    as signedOffTasks,
           u.first_name            AS firstName,
           u.last_name             AS lastName,
           u.employee_id           AS employeeId,
           tad.user_groups_id      as userGroupId,
           ug.name                 AS userGroupName
    FROM taskExecutionAssigneeDetail tad
             left join users u on tad.users_id = u.id
             left join user_groups ug on tad.user_groups_id = ug.id
    """;
  public static final String GET_ALL_JOB_ASSIGNEES = "select u.id as id, tex.jobs_id as jobId, u.first_name as firstName, u.last_name lastName, u.employee_id as employeeId from " +
    "task_execution_user_mapping teum inner join users u on teum.users_id =  u.id inner join task_executions tex on tex.id = teum.task_executions_id where tex.jobs_id in :jobIds group by u.id, tex.jobs_id";
  public static final String GET_ALL_JOB_ASSIGNEES_COUNT = "select count(distinct u.id) from task_execution_user_mapping teum inner join users u on teum.users_id =  u.id inner join task_executions tex on tex.id = teum.task_executions_id where tex.jobs_id = :jobId";
  public static final String IS_ALL_TASK_UNASSIGNED = "select CASE WHEN count(te)>0 THEN true ELSE false END from task_execution_user_mapping te where task_executions_id in " +
    "(select tex.id from task_executions tex inner join jobs j on j.id = tex.jobs_id and j.id = :jobId)";
  public static final String IS_USER_ASSIGNED_TO_ANY_TASK = """
    SELECT CASE
               WHEN EXISTS (SELECT 1
                            FROM task_execution_user_mapping te
                                     INNER JOIN task_executions tex ON te.task_executions_id = tex.id
                                     INNER JOIN jobs j ON tex.jobs_id = j.id AND j.id = :jobId
                                     LEFT JOIN user_group_members ugm ON te.user_groups_id = ugm.groups_id
                            WHERE te.users_id = :userId
                               OR ugm.users_id = :userId) THEN true
               ELSE false
               END
    """;
  public static final String IS_COLLABORATOR_MAPPING_EXISTS_BY_CHECKLIST_AND_USER_ID_AND_COLLBORATOR_TYPE = "select CASE WHEN count(c)>0 THEN true ELSE false END from ChecklistCollaboratorMapping c where " +
    "c.checklist.id = :checklistId and c.user.id = :userId and type in :types";
  public static final String UPDATE_STAGE_ORDER_BY_STAGE_ID = "update stages set order_tree = :order, modified_by = :userId, modified_at = :modifiedAt where id = :stageId";
  public static final String UNASSIGN_REVIEWERS_FROM_CHECKLIST = "delete from checklist_collaborator_mapping where checklists_id = :checklistId and phase = :phase and " +
    "users_id in :userIds";
  public static final String GET_ALL_COLLABORATORS_BY_CHECKLIST_ID_AND_PHAST_TYPE = "select u.id, u.first_name as firstName, u.last_name as lastName, u.employee_id as employeeId, crm.state, crm.order_tree as orderTree, crm.type, crm.modified_at as modifiedAt from " +
    "checklist_collaborator_mapping crm join users u on crm.users_id = u.id where crm.checklists_id = :checklistId and crm.phase_type = :phaseType and crm" +
    ".phase = (select max(phase) from checklists where id = :checklistId)";
  public static final String GET_ALL_COLLABORATORS_BY_CHECKLIST_ID_AND_TYPE = "select u.id, u.first_name as firstName, u.last_name as lastName, u.email as email, u.employee_id as employeeId, crm.state, crm.order_tree as orderTree, crm.type , crm.modified_at as modifiedAt " +
    "from checklist_collaborator_mapping crm join users u on crm.users_id = u.id where crm.checklists_id = :checklistId and crm.type = :type and " +
    "crm.phase = (select max(crmi.phase) from checklist_collaborator_mapping crmi where crmi.checklists_id = :checklistId and crmi.type = :type)";
  public static final String GET_ALL_COLLABORATORS_BY_CHECKLIST_ID_AND_TYPE_ORDER_BY_AND_MODIFIED_AT_ORDER_TREE = """
    select u.id,
           u.first_name    as firstName,
           u.last_name     as lastName,
           u.email         as email,
           u.employee_id   as employeeId,
           crm.state,
           crm.order_tree  as orderTree,
           crm.type,
           crm.modified_at as modifiedAt
    from checklist_collaborator_mapping crm
             join users u on crm.users_id = u.id
    where crm.checklists_id = :checklistId
      and crm.type = :type
      and crm.phase =
          (select max(phase) from checklist_collaborator_mapping where checklists_id = :checklistId and type = :type)
    order by crm.order_tree, crm.modified_at desc
    """;
  public static final String GET_ALL_COLLABORATORS_BY_CHECKLIST_ID_AND_TYPE_IN = """
    select u.id,
           u.first_name   as firstName,
           u.last_name    as lastName,
           u.email        as email,
           u.employee_id  as employeeId,
           crm.state,
           crm.order_tree as orderTree,
           crm.type
    from checklist_collaborator_mapping crm
             join users u on crm.users_id = u.id
    where crm.checklists_id = :checklistId
      and crm.type in :types
      and crm.phase =
          (select max(phase) from checklist_collaborator_mapping where checklists_id = :checklistId and type in :types)
    """;
  public static final String DELETE_AUTHOR_FROM_CHECKLIST = "delete from checklist_collaborator_mapping where checklists_id = :checklistId and " +
    " users_id in :userIds and type in ('AUTHOR','PRIMARY_AUTHOR')";
  public static final String GET_CHECKLIST_BY_TASK_ID = "select c from Checklist c inner join c.stages s inner join s.tasks t where t.id = :taskId";
  public static final String UNASSIGN_USERS_FROM_NON_STARTED_AND_IN_PROGRESS_TASKS = """
    WITH affected_tasks AS (
        SELECT te.id, te.jobs_id
        FROM task_executions te
        JOIN task_execution_user_mapping teum ON te.id = teum.task_executions_id
        WHERE te.jobs_id IN (SELECT j.id FROM jobs j WHERE j.state IN ('NOT_STARTED', 'IN_PROGRESS', 'ASSIGNED'))
          AND te.state NOT IN ('COMPLETED', 'COMPLETED_WITH_EXCEPTION')
          AND teum.users_id = :userId
    )
    DELETE FROM task_execution_user_mapping teum
    USING affected_tasks at
    WHERE teum.task_executions_id = at.id
      AND teum.users_id = :userId
    RETURNING at.jobs_id
    """;
  public static final String SET_JOB_TO_UNASSIGNED_IF_NO_USER_IS_ASSIGNED = """
    update jobs job
    set state = 'UNASSIGNED'
    where job.id in (select te.jobs_id
                     from task_execution_user_mapping teum
                              right outer join task_executions te on teum.task_executions_id = te.id
                     where te.id in (select tex.id
                                     from task_executions tex
                                              inner join jobs j on j.id = tex.jobs_id and j.state = 'ASSIGNED')
                     group by te.jobs_id
                     having count(teum.task_executions_id) = 0)
    """;
  public static final String IS_USER_ASSIGNED_TO_IN_PROGRESS_TASKS = "select case when count(teum.users_id) > 0 THEN true else false end from task_execution_user_mapping teum where teum.task_executions_id in " +
    "(select te.id from task_executions te where te.jobs_id in (select id from jobs j where j.state = 'IN_PROGRESS') and te.state = 'IN_PROGRESS') and teum.users_id = :userId";
  public static final String GET_NON_COMPLETED_TASKS_BY_JOB_ID = "select te.tasks_id from task_executions te where te.jobs_id = :jobId and te.state not in ('NOT_STARTED', 'COMPLETED', 'SKIPPED', 'COMPLETED_WITH_EXCEPTION')";
  public static final String GET_ENABLED_FOR_CORRECTION_TASKS_BY_JOB_ID = "select te.tasks_id from task_executions te where te.jobs_id = :jobId and te.correction_enabled = true";
  public static final String UPDATE_TASK_ASSIGNEE_STATE = "update task_execution_user_mapping set state = :state, modified_by = :modifiedBy, modified_at = :modifiedAt where users_id = :userId and task_executions_id in :taskExecutionIds";
  public static final String UNASSIGN_USERS_FROM_TASK_EXECUTIONS = "delete from task_execution_user_mapping where task_executions_id in :taskExecutionIds and users_id in :userIds and state <> 'SIGNED_OFF' and action_performed = false";
  //TODO resuse states as set of constants applies everywhere
  public static final String GET_NON_SIGNED_OFF_TASKS_BY_JOB_AND_USER_ID = "select te.tasks_id from task_executions te inner join task_execution_user_mapping teum on teum.task_executions_id = te.id " +
    "and teum.users_id = :userId and jobs_id = :jobId where teum.state != 'SIGNED_OFF' and te.state in ('COMPLETED', 'SKIPPED', 'COMPLETED_WITH_EXCEPTION', 'COMPLETED_WITH_CORRECTION')";
  public static final String GET_RECENT_VERSION_BY_ANCESTOR = "SELECT MAX(version) from Version v where v.ancestor = :ancestor";
  public static final String GET_PROTOTYPE_CHECKLIST_ID_BY_ANCESTOR = "SELECT self from Version v where v.ancestor = :ancestor and v.parent IS NOT NULL and v.version IS NULL ORDER BY v.createdAt DESC";
  public static final String GET_CHECKLIST_CODE = "SELECT code from Checklist c where c.id = :checklistId";

  public static final String UPDATE_CHECKLIST_STATE = "UPDATE Checklist c SET c.state = :state where c.id = :checklistId";
  public static final String UPDATE_DEPRECATE_VERSION_BY_PARENT = "UPDATE Version v SET v.deprecatedAt = :deprecatedAt where v.self = :parent and v.deprecatedAt IS NULL";
  public static final String DELETE_TASK_MEDIA_MAPPING = "delete from task_media_mapping where tasks_id = :taskId and medias_id =:mediaId";
  public static final String IS_ACTIVE_JOB_EXIST_FOR_GIVEN_CHECKLIST = "select CASE WHEN count(j.id)>0 THEN true ELSE false END from Job j where j.checklist.id = :checklistId and state not in :jobStates";
  public static final String READ_TEMP_PARAMETER_VALUE_BY_JOB_AND_STAGE_ID = "select av from TempParameterValue av where av.job.id = :jobId and av.parameter.id in " +
    "(select a.id from Parameter a where a.task.id in (select t.id from Task t where t.stage.id = :stageId))";
  public static final String READ_PARAMETER_VALUE_BY_JOB_ID_AND_STAGE_ID = """
    WITH taskIds AS (select t.id as taskId
                     from tasks t
                     where t.stages_id = :stageId),
         parameterIds AS (select p.id as parameterId
                        from parameters p
                                 inner join taskIds t on p.tasks_id = t.taskId)
    
    select pv.*
    from parameter_values pv
    inner join parameterIds p on pv.parameters_id = p.parameterId
    and pv.jobs_id = :jobId
    """;
  public static final String READ_TASK_EXECUTION_BY_JOB_AND_STAGE_ID = """
    WITH tasks AS (
        SELECT t.id as taskId
        FROM tasks t
        WHERE t.stages_id = :stageId
    )
    select te.* from task_executions te
    inner join tasks t on te.tasks_id = t.taskId
    where te.jobs_id = :jobId
    order by te.order_tree
    """;
  public static final String INCREMENT_TASK_COMPLETE_COUNT_BY_JOB_ID_AND_STAGE_ID = "update r_stage_execution set completed_tasks = completed_tasks + 1 where jobs_id = :jobId and stages_id = :stageId";
  public static final String GET_STAGE_EXECUTION_REPORT_BY_JOB_ID = "select s from StageExecutionReport s where jobId = :jobId";
  public static final String GET_STAGE_BY_TASK_ID = "select s from Stage s inner join s.tasks t where t.id = :taskId";
  public static final String GET_STAGES_BY_TASK_IDS = "select s from Stage s inner join s.tasks t where t.id in :taskIds";
  public static final String GET_STAGE_ID_BY_TASK_ID = "select s.id from Stage s inner join s.tasks t where t.id = :taskId";

  public static final String GET_TOTAL_TASKS_VIEW_BY_CHECKLIST_ID = "select s.id as stageId, s.name as stageName, count(t.id) as totalTasks from stages s inner join tasks t on t.stages_id = s.id where s.checklists_id = :checklistId group by s.id";
  public static final String UPDATE_STATE_TO_IN_PROGRESS_IN_STAGE_REPORT_BY_JOB_ID_AND_STAGE_ID = "update r_stage_execution set tasks_in_progress = true where jobs_id = :jobId and stages_id = :stageId";
  public static final String DELETE_STAGE_EXECUTION_BY_JOB_ID = "DELETE FROM r_stage_execution where jobs_id = :jobId";
  public static final String UPDATE_TASK_ORDER = "update tasks set order_tree = :order, modified_by = :userId, modified_at = :modifiedAt where id = :taskId";
  public static final String DELETE_COLLABORATOR_COMMENTS_BY_CHECKLIST_COLLABORATOR_MAPPING = "delete from checklist_collaborator_comments where checklist_collaborator_mappings_id = :checklistCollaboratorMappingId";
  public static final String GET_TASK_BY_PARMETER_ID = "select t from Task t inner join t.parameters a where a.id = :parameterId";
  public static final String GET_TASK_EXECUTION_USER_MAPPING_BY_TASK_EXECUTION_AND_USER_ID_IN = "select state as assigneeState, action_performed as isActionPerformed, users_id as userId, task_executions_id as taskExecutionId " +
    "from task_execution_user_mapping teum where teum.task_executions_id = :taskExecutionId and teum.users_id in :userIds";
  public static final String GET_TASK_EXECUTION_USER_MAPPING_BY_TASK_EXECUTION_IN_AND_USER_ID_IN = "select state as assigneeState, action_performed as isActionPerformed, users_id as userId, task_executions_id as taskExecutionId " +
    "from task_execution_user_mapping teum where teum.task_executions_id in :taskExecutionIds and teum.users_id in :userIds";
  public static final String GET_TASK_EXECUTION_USER_MAPPING_BY_TASK_EXECUTION_IN = """
    WITH taskExecutionAssigneeDetail AS (SELECT m.users_id                  AS users_id,
                                                m.user_groups_id           AS user_groups_id,
                                                count(m.task_executions_id) as assigned_tasks,
                                                case
                                                    when (count(m.task_executions_id) <> :totalExecutionIds) then false
                                                    else true end           as completely_assigned
                                         FROM task_execution_user_mapping m
                                         where m.task_executions_id in :taskExecutionIds
                                         group by m.users_id, m.user_groups_id)
    SELECT tad.users_id            as userId,
           tad.assigned_tasks      as assignedTasks,
           tad.completely_assigned as completelyAssigned,
           u.first_name            AS firstName,
           u.last_name             AS lastName,
           u.employee_id           AS employeeId,
           ug.id                   AS userGroupId,
           ug.name                 AS userGroupName
    FROM taskExecutionAssigneeDetail tad
             left join users u on tad.users_id = u.id
             left join user_groups ug on tad.user_groups_id = ug.id
          where ((:isUser and tad.users_id is not null)
        OR (:isUserGroup and tad.user_groups_id is not null))
    """;
  public static final String GET_TASK_USER_MAPPING_BY_TASK_IN = """
    WITH taskAssigneeDetail AS (SELECT tu.users_id          AS users_id,
                                       tu.user_groups_id    AS user_groups_id,
                                       count(tutm.tasks_id) as assigned_tasks,
                                       case
                                           when (count(tutm.tasks_id) <> :totalTaskIds) then false
                                           else true end    as completely_assigned
                                FROM trained_user_tasks_mapping tutm
                                         inner join trained_users tu on tu.id = tutm.trained_users_id
                                where tutm.tasks_id in :taskIds
                                  and tu.checklists_id = :checklistId
                                  and tu.facilities_id = :facilityId
                                group by tu.users_id, tu.user_groups_id)
    SELECT tad.users_id            as userId,
           tad.user_groups_id      as userGroupId,
           tad.assigned_tasks      as assignedTasks,
           tad.completely_assigned as completelyAssigned,
           u.first_name            AS firstName,
           u.last_name             AS lastName,
           u.employee_id           AS employeeId,
           ug.name                 AS userGroupName
    FROM taskAssigneeDetail tad
             left join users u on tad.users_id = u.id
             left join user_groups ug on tad.user_groups_id = ug.id
             WHERE ((:isUser and tad.users_id is not null)
        OR (:isUserGroup and tad.user_groups_id is not null)
        )
    order by u.first_name, u.last_name, ug.name
    """;
  public static final String GET_TASK_EXECUTION_COUNT_BY_JOB_ID = "select count(id) from task_executions where jobs_id=:jobId";
  public static final String GET_PARAMETER_VALUES_BY_JOB_ID_AND_TASK_ID_AND_PARAMETER_TYPE_IN = "select av from ParameterValue av inner join av.job j inner join av.parameter a inner join a.task t where a.type in :parameterTypes and j.id = :jobId and t.id in :taskIds";
  public static final String GET_STAGES_BY_JOB_ID_WHERE_ALL_TASK_EXECUTION_STATE_IN = "select distinct st from TaskExecution tex inner join tex.job js inner join tex.task ts inner join " +
    "ts.stage st where js.id = :jobId group by st having count(case when tex.state in :taskExecutionStates then 1 else null end) = count(tex.task.id) order by st.orderTree";
  public static final String GET_TASK_EXECUTIONS_BY_JOB_ID_AND_STAGE_ID_IN = "select te from TaskExecution te inner join te.task t inner join t.stage s inner join te.job j where j.id=:jobId and s.id in :stageIds order by s.orderTree, t.orderTree";
  public static final String GET_ALL_MEDIAS_WHERE_ID_IN = "select m from Media m where id in :mediaIds";
  public static final String GET_STAGES_BY_CHECKLIST_ID_AND_ORDER_BY_ORDER_TREE = "select s from Stage s where s.checklistId=:checklistId and s.archived=false order by orderTree";
  public static final String GET_TASKS_BY_STAGE_ID_IN_AND_ORDER_BY_ORDER_TREE = "select t from Task t inner join t.stage s where t.stageId in :stageIds and s.archived=false and t.archived=false order by s.orderTree, t.orderTree";
  public static final String GET_PARAMETERS_BY_TASK_ID_IN_AND_ORDER_BY_ORDER_TREE = "select a from Parameter a inner join a.task t inner join t.stage s where a.taskId in :taskIds and s.archived=false and t.archived=false and a.archived=false order by s.orderTree, t.orderTree, a.orderTree";
  public static final String GET_CHECKLIST_DEFAULT_USER_IDS_BY_CHECKLIST_ID = "select tu.user.id from TrainedUserTaskMapping tutm inner join TrainedUser tu on tutm.trainedUser.id = tu.id where tu.checklist.id = :checklistId and tu.facility.id =:facilityId";
  public static final String GET_ENABLED_PARAMETERS_COUNT_BY_PARAMETER_TYPE_IN_AND_ID_IN = "select count(a.id) from Parameter a where a.id in :parameterIds and a.type in :types and a.archived=false";
  public static final String GET_PARAMETER_VALUES_BY_JOB_ID_AND_PARAMETER_TARGET_ENTITY_TYPE_IN = "select av from ParameterValue av inner join av.job j inner join av.parameter a where a.targetEntityType in :targetEntityTypes and j.id = :jobId";

  public static final String DELETE_TASK_AUTOMATION_MAPPING = "delete from task_automation_mapping where tasks_id = :taskId and automations_id =:automationsId";
  public static final String GET_ALL_AUTOMATIONS_IN_TASK_AUTOMATION_MAPPING_BY_TASK_ID = """
    select a
        from Automation a
        inner join TaskAutomationMapping tam on tam.automationId = a.id
        where
              a.triggerType = :triggerType
              and tam.taskId = :taskId
        order by tam.orderTree
    """;
  public static final String DELETE_CHECKLIST_FACILITY_MAPPING = "delete from checklist_facility_mapping where checklists_id= :checklistId and facilities_id in :facilityIds";
  public static final String GET_PARAMETERS_COUNT_BY_CHECKLIST_ID_AND_PARAMETER_ID_IN_AND_TARGET_ENTITY_TYPE = "select count(id) from Parameter a where a.archived = false and a.checklistId=:checklistId and a.id in :parameterIds and a.targetEntityType=:targetEntityType";
  public static final String UPDATE_PARAMETERS_TARGET_ENTITY_TYPE = "UPDATE Parameter a SET a.targetEntityType = :targetEntityType where a.id in :parameterIds";

  public static final String GET_PARAMETERS_BY_CHECKLIST_ID_AND_TARGET_ENTITY_TYPE = "select a from Parameter a where a.archived = false and a.checklistId=:checklistId and a.targetEntityType=:targetEntityType order by a.orderTree";
  public static final String GET_ARCHIVED_PARAMETERS_BY_REFERENCED_PARAMETER_ID = "select * from parameters where archived = true and id in ( :referencedParameterIds )";
  public static final String UPDATE_PARAMETER_TARGET_ENTITY_TYPE_BY_CHECKLIST_ID_AND_TARGET_ENTITY_TYPE = "update Parameter a set a.targetEntityType=:updatedTargetEntityType where a.checklistId=:checklistId and a.targetEntityType=:targetEntityType";
  public static final String UPDATE_PARAMETER_ORDER = "update parameters set order_tree = :order, modified_by = :userId, modified_at = :modifiedAt where id = :parameterId";

  public static final String UPDATE_PARAMETER_VALUE_VISIBILITY = """
    UPDATE ParameterValue pv
    SET hidden = :visibility
    WHERE pv.id IN(:parameterValueIds)
    """;

  public static final String GET_CHECKLIST_STATE_BY_STAGE_ID = "SELECT c.state FROM Checklist c inner join c.stages s where s.id=:stageId";
  public static final String FIND_JOB_PROCESS_INFO = """
    select j.id as jobId, j.code as jobCode, c.name as processName, c.id as processId, c.code as processCode
    from jobs j
             inner join checklists c on j.checklists_id = c.id
    where j.id = :jobId
    """;
  public static final String FIND_TASK_EXECUTION_TIMER_AT_PAUSE = "SELECT tm FROM TaskExecutionTimer tm WHERE tm.taskExecutionId = :taskExecutionId and tm.resumedAt IS NULL";

  public static final String UPDATE_VISIBILITY_OF_PARAMETERS = """
    UPDATE Parameter p
    SET p.hidden =
        CASE
            WHEN p.id IN :hiddenParameterIds THEN true
            WHEN p.id IN :visibleParameterIds THEN false
        END
    WHERE p.id IN (:hiddenParameterIds, :visibleParameterIds)
    """;

  public static final String IS_LINKED_PARAMETER_EXISTS_BY_PARAMETER_ID = """
    select exists(select id
                  from parameters
                  where checklists_id = :checklistId
                    and auto_initialize ->> 'parameterId' = :parameterId
                    and archived = false);
                    """;

  public static final String FIND_BY_JOB_ID_AND_PARAMETER_VALUES_ID_AND_VERIFICATION_TYPE_AND_USER_ID = """
    SELECT *
    from parameter_verifications
    where jobs_id = :jobId
      and parameter_values_id = :parameterValueId
      and verification_type = :verificationType
      and (:userId is null or users_id = :userId)
    order by id desc
    limit 1
    """;
  public static final String FIND_BY_JOB_ID_AND_PARAMETER_ID_AND_PARAMETER_VERIFICATION_TYPE = "SELECT * from parameter_verifications pvf join parameter_values pv on pvf.parameter_values_id = pv.id join parameters p on pv.parameters_id = p.id where pvf.jobs_id = :jobId and p.id = :parameterId and pvf.verification_type = :verificationType order by pvf.id desc limit 1";

  public static final String GET_ALL_CHECKLIST_IDS_BY_OBJECT_TYPE_IN_DATA = """
    select p.checklists_id
    from parameters p where p.data->>'objectTypeId'= :objectTypeId
    """;

  public static final String GET_ALL_JOB_IDS_BY_TARGET_ENTITY_TYPE_AND_OBJECT_TYPE_IN_DATA = """
      SELECT pv.jobs_id FROM parameter_values pv JOIN parameters p ON p.id = pv.parameters_id WHERE p.target_entity_type = :targetEntityType AND choices @> :objectId
    """;

  public static final String GET_ALL_JOB_IDS_BY_OBJECT_TYPE_IN_DATA = """
      SELECT pv.jobs_id FROM parameter_values pv WHERE choices @> :objectId
    """;

  public static final String GET_FIRST_PARAMETER_VALUE_BY_OBJECT_ID = """
      SELECT * FROM parameter_values pv WHERE choices @> :objectId ORDER BY id DESC LIMIT 1
    """;

  public static final String GET_VERIFICATION_INCOMPLETE_PARAMETER_EXECUTION_IDS_BY_TASK_EXECUTION_ID = """
    select pv.id
        from parameter_values pv
                 inner join public.parameters p on p.id = pv.parameters_id
                 inner join tasks t on p.tasks_id = t.id
        where pv.task_executions_id = :taskExecutionId
          and pv.state in ( 'APPROVAL_PENDING' , 'VERIFICATION_PENDING', 'BEING_EXECUTED', 'BEING_EXECUTED_AFTER_REJECTED', 'PENDING_FOR_APPROVAL')
          and p.verification_type != 'NONE'
          and pv.hidden <> true
    """;
  public static final String IS_JOB_EXISTS_BY_SCHEDULER_ID_AND_DATE_GREATER_THAN_EXPECTED_START_DATE = "select CASE WHEN count(j.id)>0 THEN true ELSE false END from Job j where j.schedulerId = :schedulerId and j.expectedStartDate >= :date";
  public static final String GET_CHECKLIST_BY_STATE = "SELECT c.id FROM Checklist c WHERE c.state in :state";
  public static final String GET_CHECKLIST_BY_STATE_NOT = "SELECT c.id FROM Checklist c WHERE c.state != :state";
  public static final String  GET_ALL_PENDING_FOR_APPROVAL_PARAMETER_STATUS = """
    SELECT DISTINCT ON (e.id, e.rules_id)
           pv.id          AS parameterValueId,
           p.id           AS parameterId,
           pv.jobs_id     AS jobId,
           p.label        AS parameterName,
           t.name         AS taskName,
           cl.name        AS processName,
           pv.modified_at AS modifiedAt,
           j.code         AS jobCode,
           t.id           AS taskId,
           te.id          AS taskExecutionId,
           pv.created_at  AS createdAt,
           e.created_by   AS exceptionInitiatedBy,
           rules_id       AS rulesId

    FROM exceptions e
             JOIN parameter_values pv ON e.parameter_values_id = pv.id
             JOIN jobs j ON e.jobs_id = j.id
             JOIN parameters p ON pv.parameters_id = p.id
             JOIN exception_reviewers er ON e.id = er.exceptions_id
             JOIN checklists cl ON j.checklists_id = cl.id
             INNER JOIN users u ON u.id = e.created_by
             LEFT JOIN tasks t ON t.id = p.tasks_id
             LEFT JOIN task_executions te ON pv.task_executions_id = te.id
    WHERE e.facilities_id = :facilityId
      AND pv.has_exceptions = TRUE
      AND (pv.state = 'PENDING_FOR_APPROVAL')
      AND e.status = 'INITIATED'
      AND (j.state = 'IN_PROGRESS' or j.state = 'ASSIGNED')
      AND (CAST(:jobId AS VARCHAR) IS NULL OR j.id = :jobId)
      AND (:requestedBy IS NULL OR e.created_by = :requestedBy)
      AND (:showAllException = TRUE OR :userId IN (er.users_id, e.created_by))
      AND ((CAST(:parameterName AS VARCHAR) IS NULL OR p.label ILIKE CONCAT('%', CAST(:parameterName AS VARCHAR), '%'))
        OR (CAST(:processName AS VARCHAR) IS NULL OR cl.name ILIKE CONCAT('%', CAST(:processName AS VARCHAR), '%')))
      AND (CAST(:objectId AS VARCHAR) IS NULL OR j.id IN (SELECT pv.jobs_id
                                                          FROM parameter_values pv
                                                                   INNER JOIN parameters p ON p.id = pv.parameters_id
                                                                   CROSS JOIN JSONB_ARRAY_ELEMENTS(pv.choices) AS choice
                                                          WHERE choice ->> 'objectId' = :objectId
                                                            AND p.type = 'RESOURCE'))
    AND (:useCaseId IS NULL OR j.use_cases_id = :useCaseId)
    ORDER BY e.id, e.rules_id
    """;

  public static final String AUTOMATION_EXISTS_BY_TASK_ID_AND_TRIGGER_TYPE_AND_AUTOMATION_ACTION_TYPES =
    "select CASE WHEN count(a.id)>0 THEN true ELSE false END from TaskAutomationMapping tam inner join tam.automation a where tam.taskId = :taskId and a.triggerType = :triggerType and a.actionType in :actionTypes";
  public static final String UPDATE_PRIMARY_AUTHOR = """
    UPDATE checklist_collaborator_mapping SET created_by = :userId, modified_by = :userId, users_id = :userId
    WHERE checklists_id = :checklistId and type = 'PRIMARY_AUTHOR'
    """;

  public static final String FIND_LATEST_PARAMETER_VALUE_BY_JOB_ID_AND_PARAMETER_ID = """
    select * from parameter_values pv where pv.jobs_id = :jobId and pv.parameters_id = :parameterId order by pv.id desc limit 1
    """;

  public static final String FIND_PARAMETER_VALUES_BY_JOB_ID_AND_PARAMETER_ID_IN = """
    select pv from ParameterValue pv where pv.jobId = :jobId and pv.id in :ids
    """;
  public static final String FIND_TASK_EXECUTION_DETAILS_BY_JOB_ID = """
    select te.tasks_id   as taskId,
           te.started_at as startedAt,
           te.ended_at   as endedAt,
           u1.id as taskStartedById,
           u1.first_name as taskStartedByFirstName,
           u1.last_name  as taskStartedByLastName,
           u1.employee_id as taskStartedByEmployeeId,
           u2.id as taskModifiedById,
           u2.first_name as taskModifiedByFirstName,
           u2.last_name  as taskModifiedByLastName,
           u2.employee_id as taskModifiedByEmployeeId,
           t.name as name,
           te.jobs_id as jobId
    from task_executions te
             left join public.users u1 on te.started_by = u1.id
             inner join public.users u2 on te.modified_by = u2.id
             inner join public.tasks t on t.id = te.tasks_id
    where te.jobs_id in :jobIds
              """;
  public static final String FIND_LATEST_TASK_EXECUTION_BY_TASK_ID_AND_JOB_ID_ORDER_BY_ORDER_TREE_DESC = """
    select *
        from task_executions te
        where te.tasks_id = :taskId
        and te.jobs_id = :jobId
        order by te.order_tree desc
        limit 1
    """;
  public static final String FIND_TASK_EXECUTIONS_NOT_IN_COMPLETED_STATE_BY_TASK_ID_AND_JOB_ID = """
        SELECT id as id
        from task_executions te
        where te.tasks_id = :taskId
          and te.jobs_id = :jobId
          and (te.state not in ('COMPLETED', 'COMPLETED_WITH_EXCEPTION', 'SKIPPED') or te.correction_enabled = true)
          
    """;
  public static final String DELETE_BY_TASK_EXECUTION_ID = """
    DELETE FROM task_executions te WHERE te.id = :id
                """;
  public static final String CHECK_IF_ANY_TASK_RECURRENCE_CONTAINS_STOP_RECURRENCE = """
       SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END
                                FROM task_executions te
                                WHERE te.continue_recurrence = false
                                AND te.tasks_id = :taskId
                                AND te.jobs_id = :jobId
                                AND te.order_tree = 1
    """;
  public static final String SET_CONTINUE_RECCURENCE_TO_FALSE = """
       update task_executions  set continue_recurrence = false
        where tasks_id = :taskId and jobs_id = :jobId
    """;
  public static final String FIND_ALL_STARTED_TASK_EXECUTIONS_AFTER_ORDER_TREE = """
      select te.id as id
      from tasks t
               inner join stages s on s.id = t.stages_id
               inner join task_executions te on te.tasks_id = t.id
      where s.order_tree > :orderTree
        and te.state not in ('NOT_STARTED')
        and te.jobs_id = :jobId
    """;

  public static final String FIND_ALL_STARTED_TASK_EXECUTIONS_IN_A_STAGE_AFTER_TASK_ORDER_TREE = """
    select te.id as id
    from task_executions te
             inner join tasks t on te.tasks_id = t.id
             inner join stages s on s.id = t.stages_id
    where t.order_tree > :taskOrderTree
      and te.jobs_id = :jobId
      and te.state <> 'NOT_STARTED'
      and s.id = :stageId
        """;
  public static final String FIND_ALL_NON_COMPLETED_TASK_EXECUTIONS_BEFORE_ORDER_TREE_WHICH_HAS_ADD_STOP = """ 
    WITH hidden_task_executions AS (
            SELECT te.id
            FROM task_executions te
                     INNER JOIN parameter_values pv ON te.id = pv.task_executions_id
            WHERE te.jobs_id = :jobId
            GROUP BY te.id
            HAVING COUNT(CASE WHEN pv.hidden = false THEN 1 END) = 0
        )
        SELECT te.id
        FROM tasks t
                 INNER JOIN task_executions te ON te.tasks_id = t.id
                 INNER JOIN stages s ON s.id = t.stages_id
        WHERE s.order_tree < :orderTree
          AND t.has_stop = true
          AND te.state NOT IN ('COMPLETED', 'COMPLETED_WITH_EXCEPTION', 'SKIPPED')
          AND te.jobs_id = :jobId
          AND te.id NOT IN (SELECT id FROM hidden_task_executions)
            """;

  public static final String FIND_ALL_NON_COMPLETED_TASK_EXECUTIONS_OF_CURRENT_STAGE_BEFORE_ORDER_TREE_WHICH_HAS_ADD_STOP = """
    WITH hidden_task_executions AS (
            SELECT te.id
            FROM task_executions te
                     INNER JOIN parameter_values pv ON te.id = pv.task_executions_id
            WHERE te.jobs_id = :jobId
            GROUP BY te.id
            HAVING COUNT(CASE WHEN pv.hidden = false THEN 1 END) = 0
        )
        SELECT te.id
        FROM tasks t
                 INNER JOIN task_executions te ON te.tasks_id = t.id
                 INNER JOIN stages s ON s.id = t.stages_id
        WHERE s.id = :stageId
          AND t.has_stop = true
          AND t.order_tree < :orderTree
          AND te.state NOT IN ('COMPLETED', 'COMPLETED_WITH_EXCEPTION', 'SKIPPED')
          AND te.jobs_id = :jobId
          AND te.id NOT IN (SELECT id FROM hidden_task_executions)
            """;

  public static final String FIND_ALL_TASK_EXECUTION_WITH_SCHEDULING_ENABLED = """
    select te from TaskExecution te inner join Task t on t.id = te.taskId inner join TaskSchedules ts on t.taskSchedulesId = ts.id where t.enableScheduling = true and ts.type = 'JOB' and te.jobId = :jobId
                                            """;

  public static final String FIND_TEMP_PARAMETER_VERIFICATION_BY_JOB_ID_AND_TEMP_PARAMETER_VALUES_ID_AND_VERIFICATION_TYPE_AND_USER_ID = """
    SELECT *
    from temp_parameter_verifications
    where jobs_id = :jobId
      and temp_parameter_values_id = :tempParameterValueId
      and verification_type = :verificationType
      and :userId is NULL or users_id = :userId
    order by id desc
    limit 1
    """;
  public static final String FIND_TEMP_PARAMETER_VERIFICATION_BY_JOB_ID_AND_PARAMETER_ID_AND_TEMP_PARAMETER_VERIFICATION_TYPE = """
    SELECT *
    from temp_parameter_verifications tpvf
             join temp_parameter_values tpv on tpvf.temp_parameter_values_id = tpv.id
             join parameters p on tpv.parameters_id = p.id
    where tpvf.jobs_id = :jobId
      and p.id = :parameterId
      and tpvf.verification_type = :verificationType
    order by tpvf.id desc
    limit 1
    """;
  public static final String UPDATE_TEMP_PARAMETER_VALUES_STATE_BY_ID = "update temp_parameter_values set state = :state where id = :id";
  public static final String FIND_LATEST_SELF_AND_PEER_PARAMETER_VERIFICATIONS_BY_JOB_ID = """
    SELECT DISTINCT ON (pvf.parameter_values_id, pvf.verification_type, pvf.users_id)
        pvf.*
    FROM parameter_verifications pvf
    INNER JOIN parameter_values pv ON pv.id = pvf.parameter_values_id
    WHERE verification_type IN ('SELF', 'PEER')
    AND pvf.modified_at >= pv.modified_at
    AND pv.jobs_id = :jobId
    ORDER BY pvf.parameter_values_id, pvf.verification_type DESC, pvf.users_id, pvf.id DESC
    """;
  public static final String FIND_LATEST_SELF_AND_PEER_TEMP_PARAMETER_VERIFICATIONS_BY_JOB_ID = """
    SELECT DISTINCT ON (tpvf.temp_parameter_values_id, tpvf.verification_type, tpvf.users_id)
        tpvf.*
    FROM temp_parameter_verifications tpvf
    INNER JOIN temp_parameter_values tpv ON tpv.id = tpvf.temp_parameter_values_id
    WHERE verification_type IN ('SELF', 'PEER')
    AND tpvf.modified_at >= tpv.modified_at
    AND tpv.jobs_id = :jobId
    ORDER BY tpvf.temp_parameter_values_id, tpvf.verification_type DESC, tpvf.users_id, tpvf.id DESC
    """;
  public static final String ARCHIVE_TEMP_PARAMETER_MEDIA_MAPPING_BY_TEMP_PARAMETER_VALUE_ID_AND_MEDIA_IDS = """
    update temp_parameter_value_media_mapping
    set archived = true
    where temp_parameter_values_id = :tempParameterValueId
      and medias_id in (:archivedMediaIds)
    """;

  public static final String GET_TEMP_VERIFICATION_INCOMPLETE_PARAMETER_EXECUTION_IDS_BY_TASK_EXECUTION_ID = """
    select p.id
    from temp_parameter_values tpv
             inner join public.parameters p on p.id = tpv.parameters_id
             inner join tasks t on p.tasks_id = t.id
    where tpv.task_executions_id = :taskExecutionId
      and tpv.state not in ( 'EXECUTED' , 'NOT_STARTED')
      and p.verification_type != 'NONE'
      and tpv.hidden <> true
    """;

  /*
   * This query is used to get all the verification filters and relevant information in form of projection. Since pagination did not work with union, we had to use this approach of offset and limit.
   * This query gets all data from parameter_verification table which match the filter criteria and then all data from temp_parameter_verification table which match the filter criteria.
   * To handle dynamic filter like jobId can be null, So we are doing this null check first (:jobId IS NULL OR pvf.jobs_id = :jobId) and explicit typecasting was required for varchar types as hibernate was unable to map null to varchar
   */

  public static final String GET_ALL_VERIFICATION_FILTERS = """
    select *
    from (SELECT pvf.id                  as id,
                 mb.id                   as modifiedById,
                 mb.employee_id          as modifiedByEmployeeId,
                 mb.first_name           as modifiedByFirstName,
                 mb.last_name            as modifiedByLastName,
                 cb.id                   as createdById,
                 cb.employee_id          as createdByEmployeeId,
                 cb.first_name           as createdByFirstName,
                 cb.last_name            as createdByLastName,
                 rt.id                   as requestedToId,
                 rt.employee_id          as requestedToEmployeeId,
                 rt.first_name           as requestedToFirstName,
                 rt.last_name            as requestedToLastName,
                 pvf.modified_at         as modifiedAt,
                 pvf.created_at          as createdAt,
                 pvf.jobs_id             as jobId,
                 j.code                  as code,
                 t.id                    as taskId,
                 t.name                  as taskName,
                 s.id                    as stageId,
                 pvf.parameter_values_id as parameterValueId,
                 p.label                 as parameterName,
                 pvf.verification_type   as verificationType,
                 pvf.verification_status as verificationStatus,
                 pvf.comments            as comments,
                 c.name                  as processName,
                 te.id                   as taskExecutionId
          FROM parameter_verifications pvf
                   JOIN (SELECT jobs_id, parameter_values_id, verification_type, max(modified_at) as max_time
                         FROM parameter_verifications
                         GROUP BY jobs_id, parameter_values_id, verification_type, users_id) pvfs
                        ON pvf.jobs_id = pvfs.jobs_id
                            AND pvf.parameter_values_id = pvfs.parameter_values_id
                            AND pvf.verification_type = pvfs.verification_type
                            AND pvf.modified_at = pvfs.max_time
                   INNER JOIN jobs j on j.id = pvf.jobs_id
                   INNER JOIN public.parameter_values pv on pvf.parameter_values_id = pv.id
                   INNER JOIN public.parameters p on p.id = pv.parameters_id
                   INNER JOIN public.tasks t on p.tasks_id = t.id
                   INNER JOIN public.task_executions te on pv.task_executions_id=te.id
                   INNER join public.stages s on s.id = t.stages_id
                   INNER JOIN public.checklists c on c.id = j.checklists_id
                   INNER JOIN users mb on pvf.modified_by = mb.id
                   INNER JOIN users cb on pvf.created_by = cb.id
                   INNER JOIN users rt on pvf.users_id = rt.id
          WHERE (:jobId IS NULL OR pvf.jobs_id = :jobId)
        
            AND (:requestedBy IS NULL OR pvf.created_by = :requestedBy)
            AND (:requestedTo IS NULL OR pvf.users_id = :requestedTo)
            AND (CAST(:status AS VARCHAR) IS NULL OR pvf.verification_status = CAST(:status AS VARCHAR))
            AND ((CAST(:parameterName AS VARCHAR) IS NULL OR p.label ILIKE CONCAT('%', CAST(:parameterName AS VARCHAR), '%'))
            OR (CAST(:processName AS VARCHAR) IS NULL OR c.name ILIKE CONCAT('%', CAST(:processName AS VARCHAR), '%')))
            AND (CAST(:objectId AS VARCHAR) IS NULL OR pvf.jobs_id in (SELECT pv.jobs_id
                                                                       FROM parameter_values pv
                                                                                inner join parameters p on p.id = pv.parameters_id
                                                                                cross join jsonb_array_elements(pv.choices) AS choice
                                                                       WHERE choice ->> 'objectId' = :objectId
                                                                         and p.type = 'RESOURCE'))
          AND j.state not in ('COMPLETED', 'COMPLETED_WITH_EXCEPTION')
          AND te.state NOT IN ('COMPLETED_WITH_EXCEPTION')
          AND j.facilities_id = :facilityId
          AND (:useCaseId IS NULL OR j.use_cases_id = :useCaseId)
        
          union
          SELECT tpvf.id                       as id,
                 mb.id                         as modifiedById,
                 mb.employee_id                as modifiedByEmployeeId,
                 mb.first_name                 as modifiedByFirstName,
                 mb.last_name                  as modifiedByLastName,
                 cb.id                         as createdById,
                 cb.employee_id                as createdByEmployeeId,
                 cb.first_name                 as createdByFirstName,
                 cb.last_name                  as createdByLastName,
                 rt.id                         as requestedToId,
                 rt.employee_id                as requestedToEmployeeId,
                 rt.first_name                 as requestedToFirstName,
                 rt.last_name                  as requestedToLastName,
                 tpvf.modified_at              as modifiedAt,
                 tpvf.created_at               as createdAt,
                 tpvf.jobs_id                  as jobId,
                 j.code                        as code,
                 t.id                          as taskId,
                 t.name                        as taskName,
                 s.id                          as stageId,
                 tpvf.temp_parameter_values_id as parameterValueId,
                 p.label                       as parameterName,
                 tpvf.verification_type        as verificationType,
                 tpvf.verification_status      as verificationStatus,
                 tpvf.comments                 as comments,
                 c.name                        as processName,
                 te.id                         as taskExecutionId
          FROM temp_parameter_verifications tpvf
                   JOIN (SELECT jobs_id, temp_parameter_values_id, verification_type, users_id, max(modified_at) as max_time
                         FROM temp_parameter_verifications
                         GROUP BY jobs_id, temp_parameter_values_id, verification_type, users_id) tpvfs
                        ON tpvf.jobs_id = tpvfs.jobs_id
                            AND tpvf.temp_parameter_values_id = tpvfs.temp_parameter_values_id
                            AND tpvf.verification_type = tpvfs.verification_type
                            AND tpvf.modified_at = tpvfs.max_time
                   INNER JOIN jobs j on j.id = tpvf.jobs_id
                   INNER JOIN public.temp_parameter_values tpv on tpvf.temp_parameter_values_id = tpv.id
                   INNER JOIN public.parameters p on p.id = tpv.parameters_id
                   INNER JOIN public.tasks t on p.tasks_id = t.id
                   INNER JOIN public.task_executions te on tpv.task_executions_id=te.id
                   INNER join public.stages s on s.id = t.stages_id
                   INNER JOIN public.checklists c on c.id = j.checklists_id
                   INNER JOIN users mb on tpvf.modified_by = mb.id
                   INNER JOIN users cb on tpvf.created_by = cb.id
                   INNER JOIN users rt on tpvf.users_id = rt.id
          WHERE (:jobId IS NULL OR tpvf.jobs_id = :jobId)
        
            AND (:requestedBy IS NULL OR tpvf.created_by = :requestedBy)
            AND (:requestedTo IS NULL OR tpvf.users_id = :requestedTo)
            AND (CAST(:status AS VARCHAR) IS NULL OR tpvf.verification_status = CAST(:status AS VARCHAR))
            AND ((CAST(:parameterName AS VARCHAR) IS NULL OR p.label ILIKE CONCAT('%', CAST(:parameterName AS VARCHAR), '%'))
            OR (CAST(:processName AS VARCHAR) IS NULL OR c.name ILIKE CONCAT('%', CAST(:processName AS VARCHAR), '%')))
            AND (CAST(:objectId AS VARCHAR) IS NULL OR tpvf.jobs_id in (SELECT tpv.jobs_id
                                                                        FROM temp_parameter_values pv
                                                                                 inner join parameters p on p.id = tpv.parameters_id
                                                                                 cross join jsonb_array_elements(tpv.choices) AS choice
                                                                        WHERE choice ->> 'objectId' = :objectId
                                                                          and p.type = 'RESOURCE'))
          AND j.state not in ('COMPLETED', 'COMPLETED_WITH_EXCEPTION')
          AND te.state NOT IN ('COMPLETED_WITH_EXCEPTION')
          AND j.facilities_id = :facilityId
          AND (:useCaseId IS NULL OR j.use_cases_id = :useCaseId)
          ) as unioned_query
    order by id desc
    LIMIT :limit OFFSET :offset
    """;
  /*
   *This just returns the count of the above query which would help in pagination
   *
   */
  // TODO carve out the subquery as CTE and use it in both queries
  // TODO check if cast can be removed
  public static final String TOTAL_COUNT_VERIFICATION_FILTER = """
    select count(*)
    from (SELECT pvf.id as id
          FROM parameter_verifications pvf
                   JOIN (SELECT jobs_id, parameter_values_id, verification_type, users_id, max(modified_at) as max_time
                         FROM parameter_verifications
                         GROUP BY jobs_id, parameter_values_id, verification_type, users_id) pvfs
                        ON pvf.jobs_id = pvfs.jobs_id
                            AND pvf.parameter_values_id = pvfs.parameter_values_id
                            AND pvf.verification_type = pvfs.verification_type
                            AND pvf.modified_at = pvfs.max_time
                   INNER JOIN parameter_values pv on pvf.parameter_values_id = pv.id
                   INNER JOIN parameters p on p.id = pv.parameters_id
                   INNER JOIN public.task_executions te on pv.task_executions_id = te.id
                   INNER JOIN jobs j on j.id = pvf.jobs_id
                   INNER JOIN public.checklists c on c.id = j.checklists_id
        
          WHERE (:jobId IS NULL OR pvf.jobs_id = :jobId)
        
            AND (:requestedBy IS NULL OR pvf.created_by = :requestedBy)
            AND (:requestedTo IS NULL OR pvf.users_id = :requestedTo)
            AND (CAST(:status AS VARCHAR) IS NULL OR pvf.verification_status = CAST(:status AS VARCHAR))
            AND ((CAST(:parameterName AS VARCHAR) IS NULL OR p.label ILIKE CONCAT('%', CAST(:parameterName AS VARCHAR), '%'))
            OR (CAST(:processName AS VARCHAR) IS NULL OR c.name ILIKE CONCAT('%', CAST(:processName AS VARCHAR), '%')))
            AND (CAST(:objectId AS VARCHAR) IS NULL OR pvf.jobs_id in (SELECT pv.jobs_id
                                                                       FROM parameter_values pv
                                                                                inner join parameters p on p.id = pv.parameters_id
                                                                                cross join jsonb_array_elements(pv.choices) AS choice
                                                                       WHERE choice ->> 'objectId' = :objectId
                                                                         and p.type = 'RESOURCE'))
            AND j.state not in ('COMPLETED', 'COMPLETED_WITH_EXCEPTION')
            AND te.state NOT IN ('COMPLETED_WITH_EXCEPTION')
                 AND j.facilities_id = :facilityId
                  AND (:useCaseId IS NULL OR j.use_cases_id = :useCaseId)
            
        
          union
          SELECT tpvf.id as id
          FROM temp_parameter_verifications tpvf
                   JOIN (SELECT jobs_id, temp_parameter_values_id, verification_type, max(modified_at) as max_time
                         FROM temp_parameter_verifications
                         GROUP BY jobs_id, temp_parameter_values_id, verification_type) tpvfs
                        ON tpvf.jobs_id = tpvfs.jobs_id
                            AND tpvf.temp_parameter_values_id = tpvfs.temp_parameter_values_id
                            AND tpvf.verification_type = tpvfs.verification_type
                            AND tpvf.modified_at = tpvfs.max_time
        
                   INNER JOIN public.temp_parameter_values tpv on tpvf.temp_parameter_values_id = tpv.id
                   INNER JOIN public.parameters p on p.id = tpv.parameters_id
                   INNER JOIN public.task_executions te on tpv.task_executions_id = te.id
                   INNER JOIN jobs j on tpvf.jobs_id = j.id
                   INNER JOIN public.checklists c on c.id = j.checklists_id
          WHERE (:jobId IS NULL OR tpvf.jobs_id = :jobId)
        
            AND (:requestedBy IS NULL OR tpvf.created_by = :requestedBy)
            AND (:requestedTo IS NULL OR tpvf.users_id = :requestedTo)
            AND (CAST(:status AS VARCHAR) IS NULL OR tpvf.verification_status = CAST(:status AS VARCHAR))
            AND ((CAST(:parameterName AS VARCHAR) IS NULL OR p.label ILIKE CONCAT('%', CAST(:parameterName AS VARCHAR), '%'))
            OR (CAST(:processName AS VARCHAR) IS NULL OR c.name ILIKE CONCAT('%', CAST(:processName AS VARCHAR), '%')))
            AND (CAST(:objectId AS VARCHAR) IS NULL OR tpvf.jobs_id in (SELECT tpv.jobs_id
                                                                        FROM temp_parameter_values pv
                                                                                 inner join parameters p on p.id = tpv.parameters_id
                                                                                 cross join jsonb_array_elements(tpv.choices) AS choice
                                                                        WHERE choice ->> 'objectId' = :objectId
                                                                          and p.type = 'RESOURCE'))
            AND j.state not in ('COMPLETED', 'COMPLETED_WITH_EXCEPTION')
            AND te.state NOT IN ('COMPLETED_WITH_EXCEPTION')
            AND j.facilities_id = :facilityId
            AND (:useCaseId IS NULL OR j.use_cases_id = :useCaseId)
            ) as unioned_query
    """;
  public static final String GET_ALL_PARAMETERS_USED_IN_RULES = """
    SELECT :parameterId as id
    FROM parameters p
    WHERE (rules @> :hideRulesJson
        or rules @> :showRulesJson)
       or (
        rules != '[]'
            and rules != 'null'
            and rules is not null
            and p.id = :parameterId)
    """;
  public static final String GET_ALL_AUTOINITIALIZED_PARAMETERS_WHERE_PARAMETER_IS_USED = """
    select aip.id as id
      from auto_initialized_parameters aip
      where auto_initialized_parameters_id = :parameterId
      or referenced_parameters_id = :parameterId
    """;
  public static final String GET_ALL_PARAMETERS_WHERE_PARAMETER_ID_IS_USED_IN_AUTOMATION = """
    SELECT a.id as id
    FROM automations a
    WHERE (action_details ->> 'parameterId' = :parameterId
        OR action_details ->> 'referencedParameterId' = :parameterId
        OR action_details ->> 'offsetParameterId' = :parameterId
        OR EXISTS (SELECT 1
                   FROM jsonb_array_elements(a.action_details -> 'configuration') config
                   WHERE config ->> 'parameterId' = :parameterId))
      AND a.archived = false
        """;
  public static final String GET_ALL_PARAMETERS_WHERE_PARAMETER_ID_USED_IN_INTERLOCKS = """
    SELECT :parameterId as id
    FROM interlocks i
             CROSS JOIN lateral jsonb_array_elements(i.validations -> 'resourceParameterValidations') AS v(elem)
    WHERE v.elem ->> 'parameterId' = :parameterId
      AND cast(i.target_entity_id AS bigint) IN
          (SELECT t.id
           FROM tasks t
           WHERE t.archived = false
             AND t.stages_id IN
                 (SELECT s.id
                  FROM stages s
                  WHERE s.archived = false
                    AND s.checklists_id = :checklistsId))
    """;
  public static final String GET_ALL_PARAMETERS_USED_IN_PROPERTY_FILTERS = """
    SELECT p.id as id
    FROM parameters p
             JOIN LATERAL jsonb_array_elements(p.data -> 'propertyFilters' -> 'fields') AS fields_element ON TRUE
    WHERE fields_element ->> 'referencedParameterId' = :parameterId
    and p.archived = false
    """;
  public static final String GET_ALL_PARAMETERS_USED_IN_PROPERTY_VALIDATIONS = """
    SELECT p.id AS id
    FROM parameters p
    JOIN LATERAL jsonb_array_elements(p.validations) AS validation
         ON jsonb_typeof(p.validations) = 'array'
            AND jsonb_typeof(p.validations) != 'object'
            AND p.validations IS NOT NULL
            AND validation -> 'propertyValidations' IS NOT NULL
            AND jsonb_typeof(validation -> 'propertyValidations') = 'array'
            AND jsonb_array_length(validation -> 'propertyValidations') > 0
    JOIN LATERAL jsonb_array_elements(validation -> 'propertyValidations') AS propertyValidation
         ON TRUE
    WHERE propertyValidation ->> 'referencedParameterId' = :parameterId
      AND p.archived = false
    """;

  public static final String GET_ALL_PARAMETERS_USED_IN_RESOURCE_VALIDATION = """
    SELECT DISTINCT :parameterId AS id
    FROM parameters p,
         jsonb_array_elements(validations) AS validation,
         jsonb_array_elements(validation -> 'resourceParameterValidations') AS resourceValidation
    WHERE p.archived = false
      AND jsonb_typeof(validations) = 'array'
      AND jsonb_typeof(validations) != 'object'
      AND validations IS NOT NULL
      AND validation ->> 'validationType' = 'RESOURCE'
      AND validation -> 'resourceParameterValidations' IS NOT NULL
      AND jsonb_typeof(validation -> 'resourceParameterValidations') = 'array'
      AND jsonb_array_length(validation -> 'resourceParameterValidations') > 0
      AND resourceValidation ->> 'parameterId' = :parameterId
    """;

  public static final String GET_CHECKLIST_TASK_INFO_BY_PARAMETER_ID_FOR_RESOURCE_VALIDATION = """
    SELECT
        c.id AS checklistId,
        c.name AS checklistName,
        c.code AS checklistCode,
        t.id AS taskId,
        t.name AS taskName,
        s.name AS stageName
    FROM parameters p
    JOIN LATERAL jsonb_array_elements(p.validations) AS validation ON true
    JOIN LATERAL jsonb_array_elements(validation -> 'resourceParameterValidations') AS resourceValidation ON true
    JOIN checklists c ON p.checklists_id = c.id
    LEFT JOIN tasks t ON p.tasks_id = t.id
    LEFT JOIN stages s ON t.stages_id = s.id
    WHERE p.archived = false
      AND jsonb_typeof(p.validations) = 'array'
      AND p.validations IS NOT NULL
      AND validation ->> 'validationType' = 'RESOURCE'
      AND validation -> 'resourceParameterValidations' IS NOT NULL
      AND jsonb_typeof(validation -> 'resourceParameterValidations') = 'array'
      AND jsonb_array_length(validation -> 'resourceParameterValidations') > 0
      AND CAST(resourceValidation ->> 'parameterId' AS bigint) = :parameterId
    LIMIT 1;
    """;

  public static final String GET_ALL_PARAMETERS_USED_IN_CREATE_OBJECT_AUTOMATION_MAPPING = """
    SELECT a.id
    FROM   automations a
    WHERE  Jsonb_array_length(a.action_details -> 'configuration') > 0
           AND EXISTS (SELECT 1
                       FROM   Jsonb_array_elements(
                              a.action_details -> 'configuration') AS
                              config
                       WHERE  config ->> 'parameterId' = :parameterId);
    """;

  public static final String RECONFIGURE_VARIATIONS_OF_PARAMETER_VALUE = """
      UPDATE parameter_values pv
      SET has_variations =
              (SELECT CASE
                          WHEN COUNT(v.id) > 0 THEN true
                          ELSE false
                          END
               FROM variations v
               WHERE v.parameter_values_id = :parameterValueId)
      where pv.id = :parameterValueId

    """;
  public static final String GET_ALL_VARIATIONS_OF_PARAMETER_VALUE_ID = """
    select v.name                      as name,
           v.id                        as id,
           v.type                      as type,
           v.description               as description,
           p.label                     as parameterName,
           p.type                      as parameterType,
           t.order_tree                as taskOrderTree,
           CAST(v.new_details as text) as newVariation,
           s.order_tree                as stageOrderTree,
           v.jobs_id                   as jobId,
           pv.parameters_id            as parameterId,
           v.variation_number          as variationNumber,
           CAST(v.old_details as text) as oldVariation
                      
    from variations v
             inner join jobs j on j.id = v.jobs_id
             inner join parameter_values pv on pv.id = v.parameter_values_id
             inner join parameters p on p.id = pv.parameters_id
             inner join tasks t on p.tasks_id = t.id
             inner join stages s on s.id = t.stages_id
    where pv.id = :parameterValueId
    order by id desc
                  """;
  public static final String GET_ALL_VARIATIONS_OF_A_JOB = """
    select v.name                      as name,
           v.id                        as id,
           v.type                      as type,
           v.description               as description,
           p.label                     as parameterName,
           p.type                      as parameterType,
           t.order_tree                as taskOrderTree,
           te.order_tree               as taskExecutionOrderTree,
           CAST(v.new_details as text) as newVariation,
           s.order_tree                as stageOrderTree,
           v.jobs_id                   as jobId,
           pv.parameters_id            as parameterId,
           v.variation_number          as variationNumber,
           CAST(v.old_details as text) as oldVariation
        
    from variations v
             inner join jobs j on j.id = v.jobs_id
             inner join parameter_values pv on pv.id = v.parameter_values_id
             inner join task_executions te on pv.task_executions_id = te.id
             inner join parameters p on p.id = pv.parameters_id
             inner join tasks t on p.tasks_id = t.id
             inner join stages s on s.id = t.stages_id
    where v.jobs_id = :jobId
      and (CAST(:parameterName AS VARCHAR) IS NULL or p.label ilike CONCAT('%', CAST(:parameterName AS VARCHAR), '%'))
    order by s.order_tree, t.order_tree, p.order_tree, pv.id
    """;
  public static final String GET_ALL_PARAMETERS_ALLOWED_FOR_VARIATION = """
    select pv.*
        from parameter_values pv
                 inner join task_executions te on pv.task_executions_id = te.id
                 inner join parameters p on p.id = pv.parameters_id
                 inner join tasks t on p.tasks_id = t.id
                 inner join stages s on s.id = t.stages_id
        where pv.jobs_id = :jobId
          and ((p.type = 'RESOURCE' and (
            p.data ->> 'propertyFilters' != '{}' or
            p.data ->> 'propertyValidations' != '[]'))
            or
               (p.type = 'NUMBER' and p.validations ->> 'resourceParameterValidations' != '[]')
            or p.type = 'SHOULD_BE')
          and te.state = 'NOT_STARTED'
          and p.label ilike CONCAT('%', CAST(:parameterName AS VARCHAR), '%')
        order by s.order_tree, t.order_tree, p.order_tree
                        """;
  public static final String CHECK_IF_VARIATION_NAME_OR_NUMBER_EXISTS_FOR_A_JOB = """
    select case when count(v.id) > 0 then true else false end
    from variations v
             inner join parameter_values pv on pv.id = v.parameter_values_id
             inner join task_executions te on te.id = pv.task_executions_id
             inner join tasks t on t.id = te.tasks_id
    where v.jobs_id = :jobId
      and t.id <> :taskId
      and (v.variation_number = :variationNumber
        or v.name = :name)
      """;
  public static final String CHECK_IF_VARIATION_EXISTS_FOR_CONFIG_IDS_AND_PARAMETER_VALUE_IDS = """
          SELECT EXISTS(SELECT 1
                        FROM variations v
                        WHERE v.parameter_values_id = :parameterValueId
                          AND v.config_id IN (:configIds))
    """;

  public static final String GET_ENABLED_FOR_CORRECTION_TASKS_BY_JOB_ID_AND_TASK_ID = "select te.id from task_executions te where te.jobs_id = :jobId and te.correction_enabled = true and te.tasks_id = :taskId";
  public static final String GET_ALL_PARAMETER_RULE_MAPPINGS = """
    SELECT DISTINCT prm.triggering_parameters_id as id
        FROM parameter_rule_mapping prm
        WHERE prm.impacted_parameters_id = :impactedParameterId
        """;
  public static final String COUNT_ALL_PARAMETER_VALUE_BY_TASK_EXECUTION_AND_VISIBILITY = """
    SELECT count(pv.id) FROM parameter_values pv WHERE task_executions_id = :taskExecutionId AND hidden = :hidden
    """;

  public static final String COUNT_PARAMETER_VALUE_BY_PARAMETER_ID_AND_JOB_ID = """
    SELECT count(pv.id)
    FROM parameter_values pv
    WHERE parameters_id = :parameterId
      AND jobs_id = :jobId
    """;

  public static final String FIND_TASK_EXECUTIONS_NOT_IN_COMPLETED_STATE_OR_NOT_STARTED_BY_TASK_ID_AND_JOB_ID = """
        SELECT id as id
        from task_executions te
        where te.tasks_id = :taskId
          and te.jobs_id = :jobId
          and (te.state not in ('COMPLETED', 'COMPLETED_WITH_EXCEPTION', 'SKIPPED', 'NOT_STARTED') or te.correction_enabled = true)
          
    """;


  public static final String DELETE_JOB_ANNOTATION_MEDIA_MAPPING_BY_JOB_ID = "delete from job_annotation_media_mapping where jobs_id = :jobId";
  public static final String GET_ALL_PARAMETER_BY_CHECKLIST_ID_AND_OBJECT_TYPE_ID = """
    select p.id as id,
           cast(p.data as text) as data,
           p.checklists_id as checklistId
        
    from parameters p
    where p.checklists_id in (:checklistIds)
      and p.data ->> 'objectTypeId' = :objectTypeId
      and p.archived = false
    """;
  public static final String FIND_ALL_ELIGIBLE_PARAMETER_IDS_TO_BE_AUTOINITIALIZED_BY_REFERENCED_PARAMETER_ID = """
      SELECT aip.auto_initialized_parameters_id
    from auto_initialized_parameters aip
             inner join parameters p on p.id = aip.auto_initialized_parameters_id
             inner join parameter_values pv on pv.parameters_id = p.id
             left join tasks t on p.tasks_id = t.id
             left join stages s on t.stages_id = s.id
             left join  task_executions te on pv.task_executions_id = te.id
    WHERE aip.referenced_parameters_id = :referencedParameterId
    and (te.state in ('IN_PROGRESS', 'PAUSED') or (p.target_entity_type = 'PROCESS'))
    and pv.hidden=false
    and p.id not in (:executedParameterIds)
    and pv.jobs_id = :jobId
    order by s.order_tree,t.order_tree,p.order_tree
    """;
  public static final String GET_NON_HIDDEN_AUTO_INITIALISED_PARAMETERS_BY_TASK_EXECUTION_ID = """
    SELECT distinct p.id as autoInitializedParameterId,
     aip.referenced_parameters_id as referencedParameterId,
     p.order_tree
    from parameter_values pv
             inner join parameters p on p.id = pv.parameters_id
             inner join public.auto_initialized_parameters aip on p.id = aip.auto_initialized_parameters_id
    where pv.hidden = false
      and (p.is_auto_initialized = true or p.type = 'CALCULATION')
      and pv.task_executions_id = :taskExecutionId
      and p.archived = false
    order by p.order_tree
                """;
  public static final String IS_PARAMETER_USED_IN_AUTOINITIALISATION = """
    select exists(select 1 from auto_initialized_parameters where auto_initialized_parameters_id = :parameterId or referenced_parameters_id = :parameterId)
    """;
  public static final String GET_ALL_PARAMETER_RULE_MAPPINGS_BY_IMPACTED_PARAMETER_IDS = """
    select p from ParameterRuleMapping prm
    inner join Parameter p on p.id = prm.triggeringParameter.id
    where prm.impactedParameter.id in :impactedParameterIds
    """;
  public static final String GET_MASTER_TASK_PARAMETER_VALUE = """
    select pv.*
           from parameter_values pv
           where pv.parameters_id = :parameterId and pv.jobs_id = :jobId
           limit 1
    """;
  public static final String FIND_ALL_PARAMETERS_ELIGIBLE_FOR_AUTOINITIALISATION = """
    select pv.parameters_id from parameter_values pv
    inner join  parameters p on pv.parameters_id = p.id
    left join  task_executions te on pv.task_executions_id = te.id
    where (te.state in ('IN_PROGRESS', 'PAUSED') or (p.target_entity_type = 'PROCESS'))
    and pv.hidden=false
    and pv.id in (:showParameterExecutionIds)
    and p.id not in (:executedParameterIds)
    and (p.is_auto_initialized = true or p.type = 'CALCULATION')
    and pv.jobs_id = :jobId
    """;
  public static final String CHECK_IF_LATEST_REFERENCED_PARAMETER_IS_EXECUTED = """
                SELECT CASE
                         WHEN ( p.verification_type = 'NONE'
                                AND pv.state IN ( 'EXECUTED', 'BEING_EXECUTED' )
                                 OR p.verification_type != 'NONE'
                                    AND pv.state = 'EXECUTED' )
                              AND pv.value IS NOT NULL
                              AND pv.has_active_exception = false THEN true
                         ELSE false
                       end AS is_valid
                FROM   parameter_values pv
                       JOIN parameters p
                         ON p.id = pv.parameters_id
                WHERE  pv.jobs_id = :jobId
                       AND pv.parameters_id = :parameterId
                ORDER  BY pv.id DESC
                LIMIT  1
          """;
  public static final String CHECK_IF_RESOURCE_PARAMETER_HASACTIVE_EXCEPTIONS = """
      SELECT EXISTS (
          SELECT 1
          FROM parameter_values pv
          WHERE pv.has_active_exception = true
                AND pv.jobs_id = :jobId
                AND pv.id = :parameterId
      )
    """;
  public static final String FIND_BY_JOB_ID_AND_PARAMETER_ID_WITH_CORRECTION_ENABLED = """
    select pv.id from parameter_values pv
    inner join task_executions te on pv.task_executions_id = te.id
    where pv.jobs_id = :jobId and pv.parameters_id = :parameterId and te.correction_enabled = true
    """;
  public static final String CHECK_IF_DEPENDENT_PARAMETERS_OF_CALCULATION_PARAMETER_NOT_EXECUTED = """
    select exists(select 1
                  from auto_initialized_parameters aip
                           inner join parameters p on p.id = aip.referenced_parameters_id
                           inner join temp_parameter_values tpv on p.id = tpv.parameters_id
                  where auto_initialized_parameters_id = :parameterId
                    and tpv.state = 'NOT_STARTED'
                    and jobs_id = :jobId)
    """;
  public static final String FIND_LATEST_SELF_AND_PEER_PARAMETER_VERIFICATIONS_BY_PARAMETER_VALUE_ID = """
    SELECT DISTINCT ON (pvf.parameter_values_id, pvf.verification_type)
        pvf.*
    FROM parameter_verifications pvf
    INNER JOIN parameter_values pv ON pv.id = pvf.parameter_values_id
    WHERE verification_type IN ('SELF', 'PEER')
    AND pvf.modified_at >= pv.modified_at
    AND pv.id = :parameterValueId
    ORDER BY pvf.parameter_values_id, pvf.verification_type DESC, pvf.id DESC
    """;
  public static final String FIND_TASK_EXECUTION_ENABLED_FOR_CORRECTION = """
    select * from task_executions te where te.tasks_id = :taskId and te.jobs_id = :jobId and te.correction_enabled = true
    """;
  public static final String FIND_ALL_NON_ARCHIVED_TASK_OF_PROCESS_WHERE_SCHEDULING_IS_ENABLED = """
    SELECT t FROM Task t
    INNER JOIN Stage s on s.id = t.stageId
    WHERE t.enableScheduling = :enableScheduling and s.checklistId = :checklistId and t.archived = false
    """;
  public static final String FIND_ALL_NON_ARCHIVED_TASK_OF_PROCESS_WHERE_RECURRENCE_IS_ENABLED = """
    SELECT t.* FROM tasks t
    INNER JOIN stages s on s.id = t.stages_id
    WHERE t.enable_recurrence= :enableRecurrence and s.checklists_id = :checklistId and t.archived = false
    """;

  public static final String GET_DISTINCT_PROPERTY_VALUES_BY_PROPERTY_ID_AND_FACILITY_ID = """
    SELECT DISTINCT cpv.value
    FROM   facility_use_case_property_mapping fucpm
           JOIN checklist_property_values cpv
             ON cpv.facility_use_case_property_mapping_id = fucpm.id
           JOIN checklists c
             ON cpv.checklists_id = c.id
    WHERE  fucpm.facilities_id = :facilityId
           AND fucpm.properties_id = :propertyId
           AND c.archived = :archived
           AND c.state != :state
           AND LOWER(cpv.value) LIKE CONCAT('%', LOWER(CAST(:propertyNameInput AS VARCHAR)), '%')
           order by cpv.value
    LIMIT :limit OFFSET :offset
     """;

  public static final String GET_TOTAL_PROPERTY_VALUES_BY_PROPERTY_ID_AND_FACILITY_ID = """
    SELECT COUNT(DISTINCT cpv.value)
    FROM   facility_use_case_property_mapping fucpm
           JOIN checklist_property_values cpv
             ON cpv.facility_use_case_property_mapping_id = fucpm.id
           JOIN checklists c
             ON cpv.checklists_id = c.id
    WHERE  fucpm.facilities_id = :facilityId
           AND fucpm.properties_id = :propertyId
           AND c.archived = :archived
           AND c.state != :state
           AND LOWER(cpv.value) LIKE CONCAT('%', LOWER(CAST(:propertyNameInput AS VARCHAR)), '%')
     """;
  public static final String GET_ALL_PARAMETERS_WHERE_OBJECT_TYPE_PROPERTY_IS_USED_IN_PROPERTY_FILTERS = """
    SELECT p.id as id
    FROM parameters p
    JOIN checklists c ON p.checklists_id = c.id
    JOIN LATERAL jsonb_array_elements(p.data -> 'propertyFilters' -> 'fields') AS fields_element ON TRUE
    WHERE c.state != 'DEPRECATED'
      AND c.archived = false
      AND p.archived = false
      AND p.type = 'RESOURCE'
      AND fields_element ->> 'field' = 'searchable.' || :propertyId
    """;
  public static final String GET_ALL_PARAMETERS_WHERE_OBJECT_TYPE_RELATION_IS_USED_IN_PROPERTY_FILTERS = """
    SELECT p.id as id
    FROM parameters p
    JOIN checklists c ON p.checklists_id = c.id
    JOIN LATERAL jsonb_array_elements(p.data -> 'propertyFilters' -> 'fields') AS fields_element ON TRUE
    WHERE c.state != 'DEPRECATED'
      AND c.archived = false
      AND p.archived = false
      AND p.type = 'RESOURCE'
      AND fields_element ->> 'field' = 'searchable.' || :relationId
    """;

  public static final String GET_ALL_PARAMETERS_WHERE_OBJECT_TYPE_PROPERTY_IS_USED_IN_VALIDATION = """
    SELECT p.id as id
    FROM parameters p
    JOIN checklists c ON p.checklists_id = c.id
    JOIN LATERAL jsonb_array_elements(p.validations -> 'resourceParameterValidations') AS validation ON TRUE
    WHERE c.state != 'DEPRECATED'
      AND c.archived = false
      AND p.archived = false
      AND p.type = 'NUMBER'
      AND validation ->> 'propertyId' = :propertyId
      AND jsonb_typeof(p.validations -> 'resourceParameterValidations') = 'array'
    """;

  public static final String GET_ALL_PARAMETERS_WHERE_OBJECT_TYPE_PROPERTY_IS_USED_IN_PROPERTY_VALIDATION = """
    SELECT p.id AS id
    FROM parameters p
    JOIN checklists c ON p.checklists_id = c.id
    JOIN LATERAL jsonb_array_elements(p.data -> 'propertyValidations') AS validations_element ON TRUE
    WHERE c.state != 'DEPRECATED'
      AND c.archived = false
      AND p.archived = false
      AND p.type = 'RESOURCE'
      AND p.data ->>'propertyValidations' != '[]' and  validations_element ->> 'propertyId' = :propertyId
    """;

  public static final String GET_ALL_AUTOMATIONS_WHERE_OBJECT_TYPE_PROPERTY_IS_USED = """
    SELECT a.id as id
    FROM automations a
             JOIN task_automation_mapping tam ON a.id = tam.automations_id
             JOIN tasks t ON tam.tasks_id = t.id
             JOIN stages s ON t.stages_id = s.id
             JOIN checklists c ON s.checklists_id = c.id
    WHERE a.archived = false
      AND t.archived = false
      AND s.archived = false
      AND c.state != 'DEPRECATED'
      AND c.archived = false
      AND a.action_details ->> 'propertyId' = :propertyId
    """;

  public static final String GET_ALL_AUTOMATIONS_WHERE_OBJECT_TYPE_RELATION_IS_USED = """
    SELECT a.id as id
    FROM automations a
             JOIN task_automation_mapping tam ON a.id = tam.automations_id
             JOIN tasks t ON tam.tasks_id = t.id
             JOIN stages s ON t.stages_id = s.id
             JOIN checklists c ON s.checklists_id = c.id
    WHERE a.archived = false
      AND t.archived = false
      AND s.archived = false
      AND c.state != 'DEPRECATED'
      AND c.archived = false
      AND a.action_details ->> 'relationId' = :relationId
    """;
  public static final String GET_ALL_AUTOINITIALIZED_PARAMETERS_WHERE_OBJECT_TYPE_PROPERTY_IS_USED = """    
    SELECT p.id as id
    FROM parameters p
    JOIN checklists c ON p.checklists_id = c.id
    WHERE c.state != 'DEPRECATED'
      AND c.archived = false
      AND p.archived = false
      AND auto_initialize != '{}' AND (auto_initialize -> 'property' ->> 'id') = :propertyId
    """;

  public static final String GET_ALL_AUTOINITIALIZED_PARAMETERS_WHERE_OBJECT_TYPE_RELATION_IS_USED = """    
    SELECT p.id as id
    FROM parameters p
    JOIN checklists c ON p.checklists_id = c.id
    WHERE c.state != 'DEPRECATED'
      AND c.archived = false
      AND p.archived = false
      AND auto_initialize != '{}' AND (auto_initialize -> 'relation' ->> 'id') = :relationId
    """;
  public static final String GET_ALL_INTERLOCK_CONDITIONS_WHERE_OBJECT_TYPE_PROPERTY_IS_USED = """    
    SELECT id
    FROM interlocks
    WHERE EXISTS (SELECT 1
                  FROM tasks t
                           JOIN stages s ON t.stages_id = s.id
                           JOIN checklists c ON s.checklists_id = c.id
                  WHERE t.id = cast(target_entity_id as bigint)
                    AND t.archived = false
                    AND s.archived = false
                    AND c.archived = false
                    AND c.state != 'DEPRECATED')
      AND EXISTS (SELECT 1
                  FROM jsonb_array_elements(validations -> 'resourceParameterValidations') AS elem
                  WHERE elem ->> 'propertyId' = :propertyId);
    """;

  public static final String GET_CHECKLIST_TASK_INFO_BY_PARAMETER_ID = """    
    SELECT
        c.id AS checklistId,
        c.name AS checklistName,
        c.code AS checklistCode,
        t.id AS taskId,
        t.name AS taskName,
        s.name AS stageName
    FROM
        parameters p
    JOIN
        checklists c ON p.checklists_id = c.id
    LEFT JOIN
        tasks t ON p.tasks_id = t.id
    LEFT JOIN
        stages s ON t.stages_id = s.id
    WHERE
        p.id = :parameterId
    """;

  public static final String GET_CHECKLIST_TASK_INFO_BY_AUTOMATION_ID = """    
    SELECT DISTINCT
        c.id AS checklistId,
        c.name AS checklistName,
        c.code AS checklistCode,
        t.id AS taskId,
        t.name AS taskName,
        s.name AS stageName
    FROM
        task_automation_mapping tam
    JOIN
        tasks t ON tam.tasks_id = t.id
    JOIN
        stages s ON t.stages_id = s.id
    JOIN
        checklists c ON s.checklists_id = c.id
    WHERE
        tam.automations_id = :automationId
        """;

  public static final String GET_CHECKLIST_TASK_INFO_BY_INTERLOCK_ID = """    
    SELECT
        c.id AS checklistId,
        c.name AS checklistName,
        c.code AS checklistCode,
        t.id AS taskId,
        t.name AS taskName,
        s.name AS stageName
    FROM
        interlocks i
    JOIN
        tasks t ON cast(i.target_entity_id as bigint) = t.id
    JOIN
        stages s ON t.stages_id = s.id
    JOIN
        checklists c ON s.checklists_id = c.id
    WHERE
        i.id = :interlockId
        """;
  public static final String GET_PARAMETER_IDS_BY_CHECKLIST_ID_AND_TARGET_ENTITY_TYPE = "select a.id from Parameter a where a.archived = false and a.checklistId=:checklistId and a.targetEntityType=:targetEntityType order by a.orderTree";

  public static final String UPDATE_CHECKLIST_DURING_RECALL = """
    update checklists set state = 'BEING_BUILT', created_by = :userId, modified_by= :userId, review_cycle = 1 where id = :checklistId
    """;
  public static final String FIND_ALL_TASK_AUTOMATION_MAPPING = """
    select tmm.*
        from task_media_mapping tmm
                 inner join tasks t on t.id = tmm.tasks_id
                 inner join stages s on t.stages_id = s.id
                 inner join public.checklists c on c.id = s.checklists_id
        where c.id in (:checklistIds)
          and t.archived = false
    """;
  public static final String FIND_TASK_MEDIA_MAPPING_WITH_TASK_MEDIA_ID = """
    SELECT tmm.medias_id, tmm.tasks_id, tmm.created_at, tmm.modified_at, tmm.created_by, tmm.modified_by,
               m.name AS media_name, m.filename, m.description, m.type, m.archived, m.original_filename, m.relative_path
        FROM task_media_mapping tmm
        JOIN medias m ON tmm.medias_id = m.id
        WHERE tmm.tasks_id = :taskId AND tmm.medias_id = :mediaId """;
  public static final String GET_PENDING_FOR_APPROVAL_PARAMETER_IDS_BY_JOB_ID_AND_TASK_EXECUTION_ID = """
    select pv.id
     from parameter_values pv
       where pv.task_executions_id = :taskExecutionId
       and pv.state = 'PENDING_FOR_APPROVAL'
       and pv.hidden = false
     """;
  public static final String GET_ALL_CHECKLIST_BY_IDS_ORDER_BY_ID = """
    select c.id, c.code, c.name, c.color_code as colorCode from checklists c where id in (:checklistIds) order by id desc
    """;
  public static final String GET_ALL_APPLICABLE_PROCESS = """
    select distinct c.id
    from checklists c
             inner join checklist_facility_mapping cfm on c.id = cfm.checklists_id
             inner join parameters p on p.checklists_id = c.id
    where (cfm.facilities_id = :facilityId or facilities_id = -1)
      and c.organisations_id = :organisationId
      and p.type='RESOURCE'
      and p.data->>'objectTypeId'= :objectTypeId
      and c.archived = :archived
      and c.use_cases_id = :useCaseId
      and c.state = 'PUBLISHED'
      and (CAST(:name as varchar) IS NULL OR c.name ilike '%' || :name || '%')
      order by c.id desc
    """;
  public static final String GET_INCOMPLETE_SHOULD_BE_PENDING_APPROVAL_BY_JOB_ID_AND_TASK_EXECUTION_ID = """
    select av.id
    from parameter_values av
    inner join parameters p on av.parameters_id = p.id
    where av.jobs_id = :jobId
      and av.task_executions_id = :taskExecutionId
      and av.hidden <> true
      and av.state = 'PENDING_FOR_APPROVAL'
    """;
  public static final String DELETE_STALE_ENTRIES_IN_PARAMETER_VERIFICATIONS = """
    delete from parameter_verifications where parameter_values_id = :parameterValueId and verification_type = :verificationType and verification_status = :verificationStatus
    """;
  public static final String DELETE_STALE_ENTRIES_IN_TEMP_PARAMETER_VERIFICATIONS = """
    delete from temp_parameter_verifications where temp_parameter_values_id = :tempParameterValueId and verification_type = :verificationType and verification_status = :verificationStatus
    """;
  public static final String GET_ALL_PARAMETERS_USED_IN_CALCULATION = """
    SELECT DISTINCT :parameterId as id
    FROM parameters p, jsonb_each(p.data->'variables') as vars(key, value)
    WHERE value->>'parameterId' = :parameterId and p.type='CALCULATION' and p.checklists_id = :checklistId and p.archived = false
    """;
  public static final String GET_PENDING_TASKS_OF_USER_FOR_JOB = """
    SELECT DISTINCT :userId        AS userId,
           tex.jobs_id    AS jobId,
           tex.id         AS taskExecutionId,
           tex.order_tree AS taskExecutionOrderTree,
           tex.state      AS taskExecutionState,
           t.id           AS taskId,
           t.name         AS taskName,
           t.order_tree   AS taskOrderTree,
           s.id           AS stageId,
           s.name         AS stageName,
           s.order_tree   AS stageOrderTree
    FROM   task_execution_user_mapping teum
           left join user_groups ug
                  ON ug.id = teum.user_groups_id
           left join user_group_members ugm
                  ON ugm.groups_id = ug.id
           left join users u
                  ON teum.users_id = u.id
           inner join task_executions tex
                   ON tex.id = teum.task_executions_id
           inner join tasks t
                   ON tex.tasks_id = t.id
           inner join stages s
                   ON t.stages_id = s.id
           inner join jobs j
                   ON j.id = tex.jobs_id
           inner join parameter_values pv
                   ON pv.task_executions_id = tex.id
    WHERE  tex.jobs_id IN :jobIds
           AND j.state IN :jobPendingStates
           AND ( u.id = :userId
                  OR ugm.users_id = :userId )
           AND pv.hidden = FALSE
    GROUP  BY u.id,
              tex.jobs_id,
              tex.id,
              t.id,
              s.id
    """;

  public static final String GET_ENGAGED_USERS_FOR_JOB = """
    SELECT u.id           AS userId,
           u.employee_id  AS employeeId,
           u.first_name   AS firstName,
           u.last_name    AS lastName,
           teum.action_performed AS actionPerformed,
           tex.jobs_id    AS jobId,
           tex.id         AS taskExecutionId
    FROM   task_execution_user_mapping teum
           inner join users u
                   ON teum.users_id = u.id
           inner join task_executions tex
                   ON tex.id = teum.task_executions_id
    WHERE  tex.jobs_id IN :jobIds
    GROUP  BY u.id,
              tex.jobs_id,
              tex.id,
              teum.action_performed
    """;

  public static final String GET_MY_JOBS = """
    SELECT distinct j.id as id, j.created_at as createdAt
    FROM jobs j
             JOIN task_executions te ON te.jobs_id = j.id
             JOIN task_execution_user_mapping teum ON teum.task_executions_id = te.id
             JOIN checklists c ON j.checklists_id = c.id
    WHERE j.organisations_id = :organisationId
      AND j.facilities_id = :facilityId
      AND j.use_cases_id = :usecaseId
      AND teum.users_id = :userId
      AND te.state IN :taskExecutionStates
      AND j.state IN :jobStates
      AND (CAST(:checklistAncestorId as VARCHAR) is NULL or j.checklist_ancestor_id = :checklistAncestorId)
      AND (CAST(:name as VARCHAR) is NULL or c.name ilike CONCAT('%', CAST(:name AS VARCHAR), '%'))
      AND (CAST(:code as VARCHAR) is NULL or j.code ilike CONCAT('%', CAST(:code AS VARCHAR), '%'))
      AND (:pom = false or te.id NOT IN (SELECT id
                                         FROM (SELECT te.id
                                               FROM task_executions te
                                               WHERE te.jobs_id = j.id
                                                 AND NOT EXISTS (SELECT 1
                                                                 FROM parameter_values pv
                                                                 WHERE pv.task_executions_id = te.id
                                                                   AND pv.hidden = false)) as tpi))
      AND (CAST(:objectId AS VARCHAR) IS NULL or j.id in (SELECT pv.jobs_id
                                                          FROM parameter_values pv
                                                                   inner join parameters p on p.id = pv.parameters_id
                                                                   cross join jsonb_array_elements(pv.choices) AS choice
                                                          WHERE choice ->> 'objectId' = :objectId
                                                            and p.type = 'RESOURCE'
                                                            and p.target_entity_type = 'PROCESS'))
    order by j.created_at desc
    LIMIT :limit OFFSET :offset
    """;
  public static final String GET_MY_JOBS_COUNT = """        
    SELECT COUNT(distinct j.id)
    FROM jobs j
             JOIN task_executions te ON te.jobs_id = j.id
             JOIN task_execution_user_mapping teum ON teum.task_executions_id = te.id
             JOIN checklists c ON j.checklists_id = c.id
    WHERE j.organisations_id = :organisationId
      AND j.facilities_id = :facilityId
      AND j.use_cases_id = :usecaseId
      AND teum.users_id = :userId
      AND te.state IN :taskExecutionStates
      AND (CAST(:checklistAncestorId as VARCHAR) is NULL or j.checklist_ancestor_id = :checklistAncestorId)
      AND (CAST(:name as VARCHAR) is NULL or c.name ilike CONCAT('%', CAST(:name AS VARCHAR), '%'))
      AND (CAST(:code as VARCHAR) is NULL or j.code ilike CONCAT('%', CAST(:code AS VARCHAR), '%'))
      AND (:pom = false or te.id NOT IN (SELECT id
                                         FROM (SELECT te.id
                                               FROM task_executions te
                                               WHERE te.jobs_id = j.id
                                                 AND NOT EXISTS (SELECT 1
                                                                 FROM parameter_values pv
                                                                 WHERE pv.task_executions_id = te.id
                                                                   AND pv.hidden = false)) as tpi))
      AND (CAST(:objectId AS VARCHAR) IS NULL or j.id in (SELECT pv.jobs_id
                                                          FROM parameter_values pv
                                                                   inner join parameters p on p.id = pv.parameters_id
                                                                   cross join jsonb_array_elements(pv.choices) AS choice
                                                          WHERE choice ->> 'objectId' = :objectId
                                                            and p.type = 'RESOURCE'
                                                            and p.target_entity_type = 'PROCESS'))
      AND j.state IN :jobStates
    """;


  public static final String GET_PARAMETERS_USED_IN_LEAST_COUNT = """
    SELECT DISTINCT :parameterId as id
    FROM parameters p
    WHERE p.checklists_id = :checklistId
      AND p.archived = false
      AND p.data->>'leastCount' IS NOT NULL
      AND EXISTS (SELECT 1
                  FROM jsonb_each(p.data) AS data(key, value)
                  WHERE key = 'leastCount'
                    AND value ->> 'selector' = 'PARAMETER'
                    AND value->>'referencedParameterId' = :parameterId)
              """;

  public static final String GET_TASK_EXECUTION_USER_MAPPING_BY_TASK_EXECUTION_AND_USER_GROUP_ID_IN = """
    select state              as assigneeState,
           action_performed   as isActionPerformed,
           users_id           as userId,
           task_executions_id as taskExecutionId,
           user_groups_id     as userGroupId
    from task_execution_user_mapping teum
    where teum.task_executions_id in (:taskExecutionIds)
      and teum.user_groups_id in :assignedUserGroupIds
    """;

  public static final String UNASSIGN_USER_GROUPS_FROM_TASK_EXECUTIONS = "delete from task_execution_user_mapping where task_executions_id in :taskExecutionIds and user_groups_id in :userGroupsId and state <> 'SIGNED_OFF' and action_performed = false";

  //TODO: ADD role and email
  public static final String FIND_ALL_TRAINED_USERS_BY_CHECKLIST_ID_AND_FACILITYID = """
    WITH trainedUsersData AS (select tu.id             as trainedUserId,
                                     tu.users_id       as userId,
                                     tu.user_groups_id as userGroupId,
                                     tu.checklists_id  as checklistId,
                                     tu.facilities_id  as facilityId
                              from trained_users tu
                              where tu.facilities_id = :facilityId
                                and tu.checklists_id = :checklistId),
         trainedUserTaskMappingResult AS (SELECT tutm.tasks_id, tu.trainedUserId as trainedUserId
                                          from trainedUsersData tu
                                                   inner join trained_user_tasks_mapping tutm
                                                              on tu.trainedUserId = tutm.trained_users_id
                                          where tu.checklistId = :checklistId
                                            and tu.facilityId = :facilityId),
        
         usersData AS (select u.employee_id    as employeeId,
                              u.first_name     as firstName,
                              u.last_name      as lastName,
                              u.id             as userId,
                              u.email          as emailId,
                              u.username       as username,
                              tu.trainedUserId as trainedUserId
                       from trainedUsersData tu
                                inner join users u on tu.userId = u.id),
         userGroupsData AS (select ug.name          as userGroupName,
                                   ug.description   as userGroupDescription,
                                   tu.trainedUserId as trainedUserId,
                                   ug.id            as userGroupId
                            from trainedUsersData tu
                                     inner join user_groups ug on tu.userGroupId = ug.id)
        
    SELECT distinct tu.trainedUserId                                               AS id,
           tu.userId                                                      AS userId,
           tu.userGroupId                                                 AS userGroupId,
           tu.checklistId                                                 AS checklistId,
           tu.facilityId                                                  AS facilityId,
           u.employeeId                                                   AS employeeId,
           u.firstName                                                    AS firstName,
           u.lastName                                                     AS lastName,
           ug.userGroupName                                               AS userGroupName,
           u.emailId                                                      AS emailId,
           ug.userGroupDescription                                        AS userGroupDescription,
           (CASE WHEN tutm.tasks_id IS NOT NULL THEN TRUE ELSE FALSE END) AS status,
           LOWER(u.firstName)                                             AS lowerFirstName,
           LOWER(u.lastName)                                              AS lowerLastName,
           LOWER(ug.userGroupName)                                        AS lowerUserGroupName
    FROM trainedUsersData tu
             LEFT JOIN usersData u ON tu.userId = u.userId
             LEFT JOIN userGroupsData ug ON tu.userGroupId = ug.userGroupId
             LEFT JOIN trainedUserTaskMappingResult tutm ON tu.trainedUserId = tutm.trainedUserId
    WHERE tu.checklistId = :checklistId
      AND tu.facilityId = :facilityId
      AND ((:isUser AND tu.userId IS NOT NULL)
        OR (:isUserGroup AND tu.userGroupId IS NOT NULL))
      AND (CAST(:query AS VARCHAR) IS NULL OR (u.firstName ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
        OR u.lastName ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
        OR u.employeeId ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
        OR u.emailId ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
        OR u.username ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
        OR ug.userGroupName ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')))
    ORDER BY lowerFirstName, lowerLastName, lowerUserGroupName
    LIMIT :limit OFFSET :offset
    """;
  public static final String DELETE_BY_CHECKLIST_ID_AND_USER_ID_IN_AND_TASK_ID_IN = """
    delete
    from trained_user_tasks_mapping tutm
    where tutm.tasks_id in (:taskIds)
      and exists (select 1
                  from trained_users tu
                  where tu.id = tutm.trained_users_id
                    and tu.checklists_id = :checklistId
                    and tu.users_id in (:userIds))
    """;
  public static final String DELETE_BY_CHECKLIST_ID_AND_USER_GROUP_ID_IN_AND_TASK_ID_IN = """
    delete
    from trained_user_tasks_mapping tutm
    where tutm.tasks_id in (:taskIds)
      and exists (select 1
                  from trained_users tu
                  where tu.id = tutm.trained_users_id
                    and tu.checklists_id = :checklistId
                    and tu.user_groups_id in (:userGroupIds))
    """;

  public static final String FIND_ALL_TRAINED_USERS_WITH_TASK_ID_BY_CHECKLIST_ID_AND_FACILITY_ID = """
    WITH trainedUsersData AS (select tu.id             as id,
                                     tu.users_id       as userId,
                                     tu.user_groups_id as userGroupId,
                                     tu.checklists_id  as checklistId,
                                     tu.facilities_id  as facilityId
                              from trained_users tu
                              where tu.facilities_id = :facilityId
                              and tu.id in :trainedUserIds
                                and tu.checklists_id = :checklistId),
         trainedUserTaskMappingResult AS (SELECT tutm.tasks_id as taskId, tu.id as trainedUserId
                                          from trainedUsersData tu
                                                   inner join trained_user_tasks_mapping tutm on tu.id = tutm.trained_users_id
                                          where tu.checklistId = :checklistId
                                            and tu.facilityId = :facilityId),
            
         usersData AS (select u.employee_id as employeeId,
                              u.first_name  as firstName,
                              u.last_name   as lastName,
                              u.id          as userId,
                              tu.id         as trainedUserId
                       from trainedUsersData tu
                                inner join users u on tu.userId = u.id),
         userGroupsData AS (select ug.name        as userGroupName,
                                   ug.description as userGroupDescription,
                                   tu.id          as trainedUserId,
                                   ug.id          as userGroupId
                            from trainedUsersData tu
                                     inner join user_groups ug on tu.userGroupId = ug.id),
         trainedUserTaskMappingStatus AS (select distinct tu.id                                                as trainedUserId,
                                                          (case when tutm.id is null then false else true end) as status
                                          from trainedUsersData tu
                                                   left join trained_user_tasks_mapping tutm on tu.id = tutm.trained_users_id
                                          where tu.checklistId = :checklistId
                                            and tu.facilityId = :facilityId)
            
    SELECT distinct tu.id                   as id,
           tu.userId               as userId,
           tu.userGroupId          as userGroupId,
           tutm.taskId           as taskId,
           tu.checklistId          as checklistId,
           tu.facilityId           as facilityId,
           u.employeeId            as employeeId,
           u.firstName             as firstName,
           u.lastName              as lastName,
           ug.userGroupName        as userGroupName,
           ug.userGroupDescription as userGroupDescription,
           tus.status              as status
    from trainedUsersData tu
             left join usersData u on tu.userId = u.userId
             left join userGroupsData ug on tu.userGroupId = ug.userGroupId
             left join trainedUserTaskMappingResult tutm on tu.id = tutm.trainedUserId
             left join trainedUserTaskMappingStatus tus on tu.id = tus.trainedUserId
    WHERE tu.checklistId = :checklistId
      AND tu.facilityId = :facilityId
      AND ((:isUser and tu.userId is not null)
        OR (:isUserGroup and tu.userGroupId is not null)
        )
        """;
  public static final String EXISTS_BY_CHECKLIST_ID_AND_TASK_ID_AND_USER_ID_AND_FACILITY_ID = """
    SELECT EXISTS(SELECT 1
                  FROM trained_user_tasks_mapping tutm
                           INNER JOIN trained_users tu ON tutm.trained_users_id = tu.id
                  WHERE tu.checklists_id = :checklistId
                    AND tu.facilities_id = :facilityId
                    AND tu.users_id = :userId
                    AND tutm.tasks_id = :taskId)
    """;
  public static final String EXISTS_BY_CHECKLIST_ID_AND_TASK_ID_AND_USER_GROUP_ID_AND_FACILITY_ID = """
    SELECT EXISTS( SELECT 1
                  FROM trained_user_tasks_mapping tutm
                           INNER JOIN trained_users tu ON tutm.trained_users_id = tu.id
                  WHERE tu.checklists_id = :checklistId
                    AND tu.facilities_id = :facilityId
                    AND tu.user_groups_id = :userGroupId
                    AND tutm.tasks_id = :taskId)
    """;
  public static final String IS_USER_GROUP_ASSIGNED_TO_IN_PROGRESS_TASKS = """
    SELECT CASE
               WHEN EXISTS (SELECT 1
                            FROM task_execution_user_mapping teum
                                     JOIN task_executions te ON teum.task_executions_id = te.id
                                     JOIN jobs j ON te.jobs_id = j.id
                            WHERE j.state = 'IN_PROGRESS'
                              AND te.state = 'IN_PROGRESS'
                              AND teum.user_groups_id = :userGroupId) THEN true
               ELSE false
               END
    """;
  public static final String REMOVE_USER_GROUP_ASSIGNEES = "delete from task_execution_user_mapping where user_groups_id = :userGroupId";
  public static final String GET_USER_WHO_PERFORMED_CORRECTION_BY_CORRECTION_ID = """
    SELECT u.*
    FROM correctors c
    JOIN users u ON c.users_id = u.id
    WHERE c.corrections_id = :correctionId
    AND c.action_performed = true
    """;
  public static final String CHECK_IF_ALL_PARAMETERS_IN_TASK_COMPLETED_WITH_CORRECTION = """
    SELECT EXISTS (SELECT 1
                   FROM parameter_values
                   WHERE task_executions_id = :taskExecutionId
                     AND state IN ('APPROVAL_PENDING', 'VERIFICATION_PENDING')
                     AND has_corrections = true)
    """;
  public static final String GET_LATEST_CORRECTION_BY_PARAMETER_VALUE_ID = """
    SELECT *
    FROM corrections
    WHERE parameter_values_id = :parameterValueId
    ORDER BY created_at DESC
    LIMIT 1
    """;
  public static final String FIND_LATEST_CORRECTION_BY_PARAMETER_VALUE_ID = """
    SELECT
        c.id,
        c.created_at AS createdAt,
        c.created_by AS initiatorUserId,
        c.task_executions_id AS taskExecutionId,
        c.code AS code,
        c.status AS status,
        c.initiators_reason AS initiatorsReason,
        c.correctors_reason AS correctorsReason,
        c.reviewers_reason AS reviewersReason,
        c.old_value AS oldValue,
        c.new_value AS newValue,
        CAST(c.old_choices AS VARCHAR) AS oldChoices,
        CAST(c.new_choices AS VARCHAR) AS newChoices,
        j.id AS jobId,
        j.code AS jobCode,
        p.label AS parameterName,
        p.id AS parameterId,
        cl.name AS processName,
        u.first_name AS initiatorFirstName,
        u.last_name AS initiatorLastName,
        u.employee_id AS initiatorEmployeeId,
        t.name AS taskName
    FROM
        corrections c
    JOIN
        parameter_values pv ON c.parameter_values_id = pv.id
    JOIN
        jobs j ON c.jobs_id = j.id
    JOIN
        parameters p ON pv.parameters_id = p.id
    LEFT JOIN
        correctors cr ON c.id = cr.corrections_id
    LEFT JOIN
        reviewers r ON c.id = r.corrections_id
    LEFT JOIN
        checklists cl ON j.checklists_id = cl.id
    INNER JOIN
        users u ON u.id = c.created_by
    JOIN
        tasks t ON t.id = p.tasks_id
    WHERE
        pv.id = :parameterValueId
    ORDER BY
        c.id DESC
    LIMIT 1
    """;
  public static final String GET_ALL_CORRECTIONS = """
        SELECT distinct c.id,
                        c.created_at         AS createdAt,
                        c.created_by         AS initiatorUserId,
                        c.task_executions_id AS taskExecutionId,
                        c.code               AS code,
                        c.status             AS status,
                        c.initiators_reason  AS initiatorsReason,
                        c.correctors_reason  AS correctorsReason,
                        c.reviewers_reason   AS reviewersReason,
                        c.old_value          AS oldValue,
                        c.new_value          AS newValue,
                        (CAST (c.old_choices AS VARCHAR))        AS oldChoices,
                        (CAST (c.new_choices AS VARCHAR))        AS newChoices,
                        j.id                 AS jobId,
                        j.code               AS jobCode,
                        p.label              AS parameterName,
                        p.id                 AS parameterId,
                        cl.name              AS processName,
                        u.first_name         AS initiatorFirstName,
                        u.last_name          AS initiatorLastName,
                        u.employee_id        AS initiatorEmployeeId,
                        t.name               AS taskName
        FROM corrections c
                 JOIN parameter_values pv ON c.parameter_values_id = pv.id
                 JOIN jobs j ON c.jobs_id = j.id
                 JOIN parameters p ON pv.parameters_id = p.id
                 LEFT JOIN correctors cr ON c.id = cr.corrections_id
                 LEFT JOIN reviewers r ON c.id = r.corrections_id
                 LEFT JOIN checklists cl ON j.checklists_id = cl.id
                 INNER JOIN users u on u.id = c.created_by
                 JOIN tasks t on t.id = p.tasks_id
        WHERE c.facilities_id = :facilityId
          AND pv.has_corrections = TRUE
          AND c.status IN ('INITIATED', 'CORRECTED', 'ACCEPTED', 'REJECTED')
          AND (:initiatedBy IS NULL OR c.created_by = :initiatedBy)
          AND j.state = 'IN_PROGRESS'
          AND ((CAST(:parameterName AS VARCHAR) IS NULL OR p.label ILIKE CONCAT('%', CAST(:parameterName AS VARCHAR), '%'))
          OR (CAST(:processName AS VARCHAR) IS NULL OR cl.name ILIKE CONCAT('%', CAST(:processName AS VARCHAR), '%')))
          AND (CAST(:status as VARCHAR) IS NULL or c.status = :status)
          AND (:userId IN (cr.users_id, r.users_id, c.created_by))
          AND (:jobId IS NULL OR c.jobs_id = :jobId)
          AND (:useCaseId IS NULL OR j.use_cases_id = :useCaseId)
        order by id desc
        offset :offset limit :limit
    """;
  public static final String GET_ALL_CORRECTIONS_COUNT = """
      SELECT COUNT(*) AS total_count
          FROM (
              SELECT DISTINCT c.id
              FROM corrections c
                       JOIN parameter_values pv ON c.parameter_values_id = pv.id
                       JOIN jobs j ON c.jobs_id = j.id
                       JOIN parameters p ON pv.parameters_id = p.id
                       LEFT JOIN correctors cr ON c.id = cr.corrections_id
                       LEFT JOIN reviewers r ON c.id = r.corrections_id
                       LEFT JOIN checklists cl ON j.checklists_id = cl.id
              WHERE c.facilities_id = :facilityId
                AND pv.has_corrections = TRUE
                AND c.status IN ('INITIATED', 'CORRECTED', 'ACCEPTED', 'REJECTED')
                AND (:initiatedBy IS NULL OR c.created_by = :initiatedBy)
                AND j.state = 'IN_PROGRESS'
                AND ((CAST(:parameterName AS VARCHAR) IS NULL OR p.label ILIKE CONCAT('%', CAST(:parameterName AS VARCHAR), '%'))
                OR (CAST(:processName AS VARCHAR) IS NULL OR cl.name ILIKE CONCAT('%', CAST(:processName AS VARCHAR), '%')))
                AND (CAST(:status AS VARCHAR) IS NULL OR c.status = :status)
                AND (:userId IN (cr.users_id, r.users_id, c.created_by))
                AND (:jobId IS NULL OR c.jobs_id = :jobId)
                AND (:useCaseId IS NULL OR j.use_cases_id = :useCaseId)
          ) AS subquery;
    """;


  public static final String DELETE_TASK_EXECUTION_FOR_ID = "delete from task_execution_user_mapping teum where teum.task_executions_id = :taskExecutionId";

  public static final String CHECK_INCOMPLETE_DEPENDENCIES = """
    WITH preRequisiteTasks As (select td.prerequisite_task_id as id
                               from tasks t
                                        inner join task_dependencies td on t.id = td.dependent_task_id
                               where td.dependent_task_id = :taskId),
         hiddenTaskExecution AS (SELECT distinct pv.task_executions_id
                                 FROM parameter_values pv
                                          inner join task_executions te on pv.task_executions_id = te.id
                                 WHERE pv.jobs_id = :jobId
                                   AND pv.hidden = false
                                   AND te.tasks_id in (select td.id from preRequisiteTasks td)),
         validTaskExecutions AS (select te.*
                                 from task_executions te
                                          inner join preRequisiteTasks on te.tasks_id = preRequisiteTasks.id
                                          right join hiddenTaskExecution hte on te.id = hte.task_executions_id
                                 where te.state not in ('SKIPPED', 'COMPLETED', 'COMPLETED_WITH_EXCEPTION')
                                   and te.jobs_id = :jobId)
        
        
    select vTE.*
    from validTaskExecutions vTE
          """;
  public static final String FIND_ALL_INVALID_REFERENCED_TASKS_USED_IN_TASK_EXECUTOR_LOCK = """
    WITH referencedTasks AS (SELECT t.id          AS taskId,
                                    tel.lock_type AS lockType,
                                    t.name        AS taskName,
                                    t.order_tree  AS taskOrderTree,
                                    s.order_tree  AS stageOrderTree
                             FROM tasks t
                                      INNER JOIN task_executor_locks tel ON t.id = tel.referenced_tasks_id
                                      INNER JOIN stages s ON t.stages_id = s.id
                             WHERE tel.tasks_id = :taskId),
        
         taskExecutions AS (SELECT te.id AS taskExecutionId, te.tasks_id AS taskId
                            FROM task_executions te
                            WHERE te.type IN ('MASTER', 'RECURRING')
                              AND te.tasks_id IN (SELECT taskId FROM referencedTasks)),
        
         hasToBeReferencedTasks AS (SELECT taskId
                                    FROM referencedTasks
                                    WHERE lockType = 'EQ'),
         cannotBeReferencedTasks AS (SELECT taskId
                                     FROM referencedTasks
                                     WHERE lockType = 'NIN'),
        
        
         nonHiddenAndNotSkippedTaskExecutions AS (SELECT DISTINCT pv.task_executions_id AS taskExecutionId,
                                                                  te.state              AS taskExecutionState,
                                                                  te.tasks_id           AS taskId,
                                                                  te.started_by         AS startedBy,
                                                                  te.order_tree         AS taskExecutionOrderTree,
                                                                  rt.taskName           AS taskName,
                                                                  rt.taskOrderTree      AS taskOrderTree,
                                                                  rt.stageOrderTree     AS stageOrderTree,
                                                                  te.ended_by           AS endedBy
                                                  FROM parameter_values pv
                                                           INNER JOIN task_executions te ON pv.task_executions_id = te.id
                                                           INNER JOIN referencedTasks rt ON te.tasks_id = rt.taskId
                                                  WHERE pv.jobs_id = :jobId
                                                    AND pv.hidden = FALSE
                                                    AND te.state != 'SKIPPED'
                                                    AND te.id IN (SELECT taskExecutionId FROM taskExecutions)),
        
         taskPauseAndResumeAudits AS (select tet.created_by as userId, nhte.taskId as taskId
                                      from task_execution_timers tet
                                               inner join nonHiddenAndNotSkippedTaskExecutions nhte
                                                          on tet.task_executions_id = nhte.taskExecutionId
                                                              and reason <> 'TASK_COMPLETED'
                                      UNION
                                      select tet.modified_by as userId, nhte.taskId as taskId
                                      from task_execution_timers tet
                                               inner join nonHiddenAndNotSkippedTaskExecutions nhte
                                                          on tet.task_executions_id = nhte.taskExecutionId
                                                              and reason <> 'TASK_COMPLETED'),
        
         taskStartCompleteAudits AS (select nhte.startedBy as userId, nhte.taskId as taskId
                                     from nonHiddenAndNotSkippedTaskExecutions nhte
                                     where nhte.startedBy is not null
                                     union
                                     select nhte.endedBy as userId, nhte.taskId as taskId
                                     from nonHiddenAndNotSkippedTaskExecutions nhte
                                     where nhte.endedBy is not null
                                       and nhte.taskExecutionState in ('COMPLETED', 'COMPLETED_WITH_EXCEPTION')),
        
         parameterExecutionAudits AS (select ja.triggered_by as userId, ja.tasks_id as taskId
                                      from job_audits ja
                                      where ja.jobs_id = :jobId
                                        and ja.tasks_id in (select taskId from referencedTasks)
                                        and ja.action = 'EXECUTE_PARAMETER'),
         allExecutors AS (select tpra.userId, tpra.taskId
                          from taskPauseAndResumeAudits tpra
                          union
                          select tsa.userId, tsa.taskId
                          from taskStartCompleteAudits tsa
                          union
                          select pea.userId, pea.taskId
                          from parameterExecutionAudits pea),
        
         violatedHasToBeReferencedTasks AS (SELECT nhte.taskExecutionId
                                            FROM nonHiddenAndNotSkippedTaskExecutions nhte
                                            WHERE nhte.taskExecutionState = 'NOT_STARTED'
                                              AND nhte.taskId IN (SELECT taskId FROM hasToBeReferencedTasks)),
        
         violatedCannotBeReferencedTasks AS (SELECT nhte.taskExecutionId
                                             FROM nonHiddenAndNotSkippedTaskExecutions nhte
                                             WHERE nhte.taskExecutionState NOT IN
                                                   ('COMPLETED', 'COMPLETED_WITH_EXCEPTION', 'SKIPPED')
                                               AND nhte.taskId IN (SELECT taskId FROM cannotBeReferencedTasks)),
        
        
         preliminaryResult AS (SELECT (CASE
                                           WHEN rt.lockType = 'EQ'
                                               THEN EXISTS (SELECT 1
                                                            from allExecutors ae
                                                            where ae.userId = :currentUserId
                                                              and rt.taskId = ae.taskId)
                                           ELSE NOT (EXISTS (SELECT 1
                                                             from allExecutors ae
                                                             where ae.userId = :currentUserId
                                                               and rt.taskId = ae.taskId))
             END)                                                 AS isValidUser,
                                      rt.lockType                 as lockType,
                                      nhte.taskExecutionId        as taskExecutionId,
                                      rt.taskName                 as taskName,
                                      rt.taskOrderTree            as taskOrderTree,
                                      nhte.taskExecutionOrderTree as taskExecutionOrderTree,
                                      rt.stageOrderTree           as stageOrderTree,
                                      (CASE
                                           WHEN rt.lockType = 'EQ'
                                               THEN NOT EXISTS(SELECT 1
                                                               from violatedHasToBeReferencedTasks v
                                                               where v.taskExecutionId = nhte.taskExecutionId)
                                           ELSE NOT EXISTS(SELECT 1
                                                           from violatedCannotBeReferencedTasks v
                                                           where v.taskExecutionId = nhte.taskExecutionId)
                                          END)                    as isValidTaskState
                               from nonHiddenAndNotSkippedTaskExecutions nhte
                                        inner join referencedTasks rt on nhte.taskId = rt.taskId)
        
    select *
    from preliminaryResult
    where NOT (isValidTaskState AND isValidUser)
    """;
  public static final String FIND_ALL_TASKS_WHERE_TASK_EXECUTOR_LOCK_HAS_ONE_REFERENCED_TASK = """
    select tel.tasks_id
          from task_executor_locks tel
          where tel.referenced_tasks_id = :taskId
          group by tel.tasks_id having count(tel.tasks_id) = 1
          """;

  public static final String REMOVE_TASK_EXECUTOR_LOCK_BY_TASK_ID = """
      DELETE FROM task_executor_locks tel
      WHERE tel.tasks_id = :taskId
    """;
  public static final String REMOVE_TASK_EXECUTOR_LOCK_BY_TASK_ID_OR_REFERENCED_TASK_ID = """
      DELETE FROM task_executor_locks tel
      WHERE tel.tasks_id = :taskId or referenced_tasks_id = :taskId
    """;
  public static final String FIND_TASK_EXECUTOR_LOCK_BY_TASK_ID = """
    select distinct s.order_tree as stageOrderTree, t.name as taskName, t.id as taskId, t.order_tree as taskOrderTree, tel.lock_type as condition
    from task_executor_locks tel
             inner join tasks t on tel.referenced_tasks_id = t.id
             inner join stages s on t.stages_id = s.id
    where tel.tasks_id = :taskId
    """;

  public static final String DELETE_BY_TASK_ID = """
    DELETE FROM trained_user_tasks_mapping
    WHERE tasks_id = :taskId
""";

  public static final String UPDATE_HAS_TASK_EXECUTOR_LOCK_FLAG = """
          update tasks set has_executor_lock = :isExecutorLock
          where id in :tasksWhereTaskExecutorLockIsUsedOnce
          """;

  public static final String UPDATE_HAS_INTERLOCKS_FLAG= """
   UPDATE tasks SET has_interlocks = :flag WHERE id = :tasksId
    """;

  public static final String REMOVE_HAS_INTERLOCKS_FLAG= """
      UPDATE tasks t
      SET has_interlocks = false
      WHERE t.id = :tasksId
      AND (
          NOT EXISTS (
              SELECT 1
              FROM interlocks i
              WHERE i.target_entity_id = t.id
              GROUP BY i.target_entity_id
              HAVING COUNT(*) > 0
          )
      )
    """;
  public static final String DELETE_HAS_INTERLOCKS_FLAG= """
    delete from interlocks
    where id = :interlocksIds
    AND jsonb_extract_path(validations, 'resourceParameterValidations') = '[]';
    """;
  public static final String UPDATE_TASKS_HAS_STOP_TO_FALSE_FOR_CHECKLIST_ID = """
    update tasks set has_stop = false from stages s, checklists c where tasks.stages_id = s.id and s.checklists_id = c.id and c.id = :checklistId
    """;
  public static final String GET_PARAMETER_TARGET_ENTITY_TYPE_BY_PARAMETER_IDS = """
    select p.id  from parameters p where p.checklists_id in :checklistIds and p.target_entity_type = 'PROCESS' order by p.order_tree
    """;

  public static final String GET_ALL_JOB_ASSIGNEES_WITH_USER_GROUP_USERS = """
    SELECT u.id          AS id,
           u.last_name   AS lastName,
           u.first_name  AS firstName,
           u.employee_id AS employeeId
    FROM task_execution_user_mapping teum
             INNER JOIN
         task_executions te ON te.id = teum.task_executions_id
             INNER JOIN
         users u ON u.id = teum.users_id
    WHERE (CAST(:query AS VARCHAR) IS NULL OR u.first_name ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
        OR u.last_name ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
        OR u.employee_id ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%'))
      AND u.archived = FALSE
      AND te.jobs_id = :jobId
        
    UNION
        
    SELECT u.id          AS id,
           u.last_name   AS lastName,
           u.first_name  AS firstName,
           u.employee_id AS employeeId
    FROM task_execution_user_mapping teum
             INNER JOIN
         task_executions te ON te.id = teum.task_executions_id
             INNER JOIN
         user_groups ug ON teum.user_groups_id = ug.id
             INNER JOIN
         user_group_members ugm ON ugm.groups_id = ug.id
             INNER JOIN
         users u ON u.id = ugm.users_id
    WHERE (CAST(:query AS VARCHAR) IS NULL OR u.first_name ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
        OR u.last_name ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
        OR u.employee_id ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%'))
      AND u.archived = FALSE
      AND te.jobs_id = :jobId
        
    ORDER BY firstName
    """;
  public static final String COUNT_ALL_TRAINED_USERS_WITH_ASSIGNED_TASKS_BY_CHECKLIST_ID_AND_FACILITY_ID = """
    WITH trainedUsersData AS (select tu.id             as trainedUserId,
                                     tu.users_id       as userId,
                                     tu.user_groups_id as userGroupId,
                                     tu.checklists_id  as checklistId,
                                     tu.facilities_id  as facilityId
                              from trained_users tu
                              where tu.facilities_id = :facilityId
                                and tu.checklists_id = :checklistId),
         trainedUserTaskMappingResult AS (SELECT tutm.tasks_id, tu.trainedUserId as trainedUserId
                                          from trainedUsersData tu
                                                   inner join trained_user_tasks_mapping tutm
                                                              on tu.trainedUserId = tutm.trained_users_id
                                          where tu.checklistId = :checklistId
                                            and tu.facilityId = :facilityId),
        
         usersData AS (select u.employee_id    as employeeId,
                              u.first_name     as firstName,
                              u.last_name      as lastName,
                              u.id             as userId,
                              u.email          as emailId,
                              u.username       as username,
                              tu.trainedUserId as trainedUserId
                       from trainedUsersData tu
                                inner join users u on tu.userId = u.id),
         userGroupsData AS (select ug.name          as userGroupName,
                                   ug.description   as userGroupDescription,
                                   tu.trainedUserId as trainedUserId,
                                   ug.id            as userGroupId
                            from trainedUsersData tu
                                     inner join user_groups ug on tu.userGroupId = ug.id)
                                     
    SELECT count(distinct tu.trainedUserId)
    FROM trainedUsersData tu
             LEFT JOIN usersData u ON tu.userId = u.userId
             LEFT JOIN userGroupsData ug ON tu.userGroupId = ug.userGroupId
             LEFT JOIN trainedUserTaskMappingResult tutm ON tu.trainedUserId = tutm.trainedUserId
    WHERE tu.checklistId = :checklistId
      AND tu.facilityId = :facilityId
      AND ((:isUser AND tu.userId IS NOT NULL)
        OR (:isUserGroup AND tu.userGroupId IS NOT NULL))
      AND (CAST(:query AS VARCHAR) IS NULL OR (u.firstName ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
        OR u.lastName ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
        OR u.employeeId ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
        OR u.emailId ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
        OR u.username ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
        OR ug.userGroupName ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')))
    """;

  public static final String GET_ALL_NON_TRAINED_USERS_BY_CHECKLIST_ID_AND_FACILITY_ID = """
    WITH trainedUserIds AS (SELECT tu.users_id as userId
                            from trained_users tu
                            WHERE tu.checklists_id = :checklistId
                              and tu.users_id is not null
                              and tu.facilities_id = :facilityId),
        
         validRoles AS (select id
                        from roles
                        where name not in ('SYSTEM_ADMIN', 'GLOBAL_ADMIN')),
         eligibleUsers AS (SELECT distinct u.id                as userId,
                                           u.first_name        as firstName,
                                           u.last_name         as lastName,
                                           LOWER(u.first_name) AS lowerFirstName,
                                           LOWER(u.last_name)  AS lowerLastName,
                                           u.employee_id       as employeeId
                           from users u
                                    inner join user_facilities_mapping ufm on u.id = ufm.users_id
                                    inner join user_roles_mapping urm on u.id = urm.users_id
                                    inner join validRoles vr on urm.roles_id = vr.id
                           where NOT EXISTS(SELECT 1
                                            from trainedUserIds tui
                                            WHERE tui.userId = u.id)
                             and u.id > 100
                             and u.archived = false
                             and ufm.facilities_id = :facilityId
                             and (CAST(:query AS VARCHAR) IS NULL OR
                                  (u.first_name ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
                                      OR u.last_name ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
                                      OR u.email ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
                                      OR u.username ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
                                      OR u.employee_id ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%'))))
        
    SELECT *
    FROM eligibleUsers
    ORDER BY lowerFirstName, lowerLastName
    LIMIT :limit OFFSET :offset
    """;
  public static final String GET_ALL_NON_TRAINED_USERS_COUNT_BY_CHECKLIST_ID_AND_FACILITY_ID = """
    WITH trainedUserIds AS (SELECT tu.users_id as userId
                            from trained_users tu
                            WHERE tu.checklists_id = :checklistId
                              and tu.users_id is not null),
         validRoles AS (select id
                        from roles
                        where name not in ('SYSTEM_ADMIN', 'GLOBAL_ADMIN')),
        
         eligibleUsers AS (SELECT distinct u.id as userId
                           from users u
                                    inner join user_facilities_mapping ufm on u.id = ufm.users_id
                                    inner join user_roles_mapping urm on urm.users_id = u.id
                                    inner join validRoles vr on vr.id = urm.roles_id
                           where NOT EXISTS(SELECT 1
                                            from trainedUserIds tui
                                            WHERE tui.userId = u.id)
                             and u.id > 100
                             and u.archived = false
                             and ufm.facilities_id = :facilityId
                             and (CAST(:query AS VARCHAR) IS NULL OR
                                  (u.first_name ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
                                      OR u.last_name ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
                                      OR u.email ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
                                      OR u.username ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
                                      OR u.employee_id ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%'))))
        
    SELECT count(*)
    FROM eligibleUsers
    """;
  public static final String GET_ALL_NON_TRAINED_USER_GROUPS_BY_CHECKLIST_ID_AND_FACILITY_ID = """
    WITH trainedUserGroupIds AS (SELECT tu.user_groups_id as userGroupId
                                 from trained_users tu
                                 WHERE tu.checklists_id = :checklistId
                                   and tu.user_groups_id is not null),
         eligibleUserGroups AS (SELECT ug.id          as userGroupId,
                                       ug.name        as userGroupName,
                                       ug.description as userGroupDescription,
                                       LOWER(ug.name) AS lowerUserGroupName
                                from user_groups ug
                                WHERE NOT EXISTS(SELECT 1
                                                 from trainedUserGroupIds tug
                                                 WHERE tug.userGroupId = ug.id)
                                  AND ug.active = true
                                  AND ug.facility_id = :currentFacilityId
                                  AND (CAST(:query AS VARCHAR) IS NULL OR
                                       ug.name ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')))
    SELECT *
    from eligibleUserGroups
    ORDER BY lowerUserGroupName
    LIMIT :limit OFFSET :offset
    """;

  public static final String GET_ALL_NON_TRAINED_USER_GROUP_COUNT = """
    WITH trainedUserGroupIds AS (SELECT tu.user_groups_id as userGroupId
                                 from trained_users tu
                                 WHERE tu.checklists_id = :checklistId
                                   and tu.user_groups_id is not null),
         eligibleUserGroups AS (SELECT ug.id          as userGroupId,
                                       ug.name        as userGroupName,
                                       ug.description as userGroupDescription,
                                       LOWER(ug.name) AS lowerUserGroupName
                                from user_groups ug
                                WHERE NOT EXISTS(SELECT 1
                                                 from trainedUserGroupIds tug
                                                 WHERE tug.userGroupId = ug.id)
                                  AND ug.active = true
                                  AND ug.facility_id = :currentFacilityId
                                  AND (CAST(:query AS VARCHAR) IS NULL OR
                                       ug.name ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')))
    SELECT count(*)
    from eligibleUserGroups
    """;

  public static final String GET_ALL_ASSIGNED_TRAINED_USERS_OR_GROUPS = """
    WITH trainedUserIds AS MATERIALIZED (SELECT tu.users_id       as userId,
                                   tu.user_groups_id as userGroupId,
                                   tutm.tasks_id     as taskId
                            FROM trained_user_tasks_mapping tutm
                                     inner join trained_users tu on tutm.trained_users_id = tu.id
                            where tu.checklists_id = :checklistId
                              and tu.facilities_id = :facilityId),
        
         eligibleUserGroups AS (SELECT ug.id          as userGroupId,
                                       ug.name        as userGroupName,
                                       ug.description as userGroupDescription
                                FROM user_groups ug
                                         inner join trainedUserIds tui on tui.userGroupId = ug.id),
        
         eligibleUsers AS (SELECT id            as userId,
                                  u.employee_id as employeeId,
                                  u.first_name  as firstName,
                                  u.last_name   as lastName,
                                  u.email       as emailId
                           from users u
                                    inner join trainedUserIds tui on tui.userId = u.id
                           where (CAST(:query AS VARCHAR) IS NULL OR
                                  (u.first_name ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
                                      OR u.last_name ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')))
                                      OR u.email ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
                                      OR u.username ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
                                      OR u.employee_id ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%'))
        
    SELECT tui.userId              as userId,
           tui.userGroupId         as userGroupId,
           tui.taskId              as taskId,
           ug.userGroupName        as userGroupName,
           ug.userGroupDescription as userGroupDescription,
           u.employeeId            as employeeId,
           u.firstName             as firstName,
           u.lastName              as lastName,
           u.emailId               as emailId
        
    from trainedUserIds tui
             LEFT JOIN eligibleUserGroups ug ON tui.userGroupId = ug.userGroupId
             LEFT JOIN eligibleUsers u ON tui.userId = u.userId
    """;
    public static final String GET_PARAMETER_VALUE_BY_JOB_IDS = """
     select pv from ParameterValue pv
              inner join Parameter p on p.id = pv.parameterId and p.targetEntityType = :targetEntityType
     where pv.jobId in (:jobIds)
    """;
  public static final String FIND_ALL_USER_IDS_OF_FACILITY = """
    select u.id from users u
    inner join user_facilities_mapping ufm on u.id = ufm.users_id
    where ufm.facilities_id = :currentFacilityId and id > 100
    """;
  public static final String FIND_ALL_USER_GROUP_IDS_OF_FACILITY = """
    select ug.id from user_groups ug
    where ug.facility_id = :currentFacilityId
    """;
  public static final String FIND_ALL_TRAINED_USER_IDS_BY_CHECKLIST_ID = """
    SELECT tu.users_id
        FROM trained_users tu
        WHERE tu.checklists_id = :checklistId and tu.users_id is not null
    """;
  public static final String FIND_ALL_TRAINED_USER_GROUP_IDS_BY_CHEKCLISTID = """
          select tu.user_groups_id
          from trained_users tu
          where tu.checklists_id = :checklistId and tu.user_groups_id is not null
          """;

  public static final String VERIFY_USER_IS_ASSIGNED_TO_THE_CHECKLIST = """
    SELECT EXISTS (
            SELECT 1
            FROM public.trained_users tu
            WHERE tu.checklists_id = :checklist_id
              AND (
                  tu.users_id = :users_id
                  OR EXISTS (
                      SELECT 1
                      FROM public.user_group_members ugm
                      WHERE ugm.groups_id = tu.user_groups_id
                        AND ugm.users_id = :users_id
                  )
              )
        ) AS result
""";

  public static final String CHECK_IF_USERS_EXISTS_BY_ROLES = """
    SELECT EXISTS(SELECT 1
                  FROM users u
                           inner join user_roles_mapping urm on u.id = urm.users_id
                           inner join roles r on urm.roles_id = r.id
                  where r.name in :roles
                    and u.id in :userIds)
    """;
  public static final String FIND_LATEST_EXCEPTION_BY_PARAMETER_VALUE_ID = """
    SELECT exc.*
           FROM exceptions AS exc
                    INNER JOIN (SELECT rules_id, MAX(id) AS max_id
                                FROM exceptions
                                WHERE parameter_values_id = :parameterValueId
                                GROUP BY rules_id) AS latest_exc ON exc.id = latest_exc.max_id
           ORDER BY exc.id DESC
    """;

  public static final String GET_ALL_TASK_ASSIGNEES_DETAILS_BY_TASK_EXECUTION_IDS = """
        WITH taskExecutionAssigneeDetail AS (SELECT m.users_id           AS users_id,
                                                    m.action_performed   AS action_performed,
                                                    m.task_executions_id AS task_executions_id,
                                                    m.state              AS state,
                                                    m.user_groups_id     AS user_groups_id
                                             FROM task_execution_user_mapping m
                                             where m.task_executions_id in :taskExecutionIds)
        SELECT tad.users_id           as userId,
               tad.action_performed   as isActionPerformed,
               tad.task_executions_id as taskExecutionId,
               tad.state              as state,
               u.first_name           AS firstName,
               u.last_name            AS lastName,
               u.employee_id          AS employeeId,
               u.email                AS email,
                ug.name                AS userGroupName,
                ug.description         AS userGroupDescription,
                ug.id                  AS userGroupId
        FROM taskExecutionAssigneeDetail tad
                 left join users u on tad.users_id = u.id
                 left join user_groups ug on tad.user_groups_id = ug.id
    """;

  public static final String READ_TASK_EXECUTION_BY_JOB_ID = """
    WITH stageIds AS (SELECT s.id as stageId
                      from stages s
                      where s.checklists_id = :checklistId),
         taskIds AS (SELECT t.id as taskId
                   FROM tasks t
                   inner join stageIds s on t.stages_id = s.stageId)
    select te.*
    from task_executions te
             inner join taskIds t on te.tasks_id = t.taskId
    where te.jobs_id = :jobId
    order by te.order_tree
    """;

  public static final String GET_ALL_JOB_ASSIGNEES_WITH_USER_GROUP_USERS_BY_ROLES = """
    WITH TaskExecutionUserMappingUsersAndUserGroups AS (SELECT DISTINCT teum.users_id       AS userId,
                                                                        teum.user_groups_id AS userGroupId
                                                        FROM task_execution_user_mapping teum
                                                                 INNER JOIN public.task_executions te ON teum.task_executions_id = te.id
                                                        WHERE te.jobs_id = :jobId
                                                          AND (teum.users_id IS NOT NULL OR teum.user_groups_id IS NOT NULL)),
        
         userGroupUsers AS (SELECT DISTINCT ugm.users_id AS userId
                            FROM TaskExecutionUserMappingUsersAndUserGroups teum
                                     INNER JOIN user_group_members ugm ON teum.userGroupId = ugm.groups_id),
        
         Roles AS (SELECT id
                   FROM roles
                   WHERE name IN (:roles)),
        
         FilteredUserRolesMapping AS (SELECT urm.users_id
                                      FROM user_roles_mapping urm
                                               INNER JOIN Roles r ON urm.roles_id = r.id
                                      WHERE urm.users_id IN (SELECT userId
                                                             FROM TaskExecutionUserMappingUsersAndUserGroups
                                                             WHERE userId IS NOT NULL
                                                             UNION
                                                             (SELECT userId FROM userGroupUsers)))
        
    SELECT u.id AS id, u.email AS email, u.first_name AS firstName, u.last_name AS lastName, u.employee_id AS employeeId
    FROM FilteredUserRolesMapping furm
             INNER JOIN users u ON furm.users_id = u.id
    WHERE (CAST(:query AS VARCHAR) IS NULL OR u.first_name ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
        OR u.last_name ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
        OR u.employee_id ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%'))
        AND u.archived = FALSE
    ORDER BY u.first_name, u.last_name
    """;

  public static final String GET_ALL_TASK_EXECUTION_ASSIGNEES_BY_TASK_EXECUTION_ID = """
    SELECT u.id    AS id,
           u.email AS email,
           u.username as username
    FROM task_execution_user_mapping teum
             INNER JOIN users u ON u.id = teum.users_id
    WHERE teum.task_executions_id = :taskExecutionId

    UNION

    SELECT u.id    AS id,
           u.email AS email,
           u.username as username
    FROM task_execution_user_mapping teum
             INNER JOIN user_groups ug ON teum.user_groups_id = ug.id
             INNER JOIN user_group_members ugm ON ugm.groups_id = ug.id
             INNER JOIN users u ON u.id = ugm.users_id
    WHERE teum.task_executions_id = :taskExecutionId
    """;
  public static final String GET_ALL_LATEST_DEPENDANT_TASK_EXECUTION_IDS_HAVING_PREREQUISITE_TASK_ID = """
    WITH TaskExecutionDependants AS (SELECT te.id as id, te.order_tree as orderTree, te.tasks_id as taskId, t.name as taskName, c.name as checklistName, j.code as jobCode
                                     FROM task_dependencies td
                                              inner join task_executions te on te.tasks_id = td.dependent_task_id
                                              inner join parameter_values pv on pv.task_executions_id = te.id
                                              inner join tasks t on t.id = td.dependent_task_id
                                              inner join stages s on s.id = t.stages_id
                                              inner join checklists c on c.id = s.checklists_id
                                              inner join jobs j on te.jobs_id = j.id
                                     WHERE prerequisite_task_id = :preRequisiteTaskId and te.jobs_id = :jobId and pv.hidden = false),
              
         RankedTaskExecutions AS (SELECT id, taskId, taskName, ROW_NUMBER() OVER (PARTITION BY taskId ORDER BY orderTree DESC) AS row_num, checklistName, jobCode
                                  FROM TaskExecutionDependants)
              
    SELECT DISTINCT id as id, taskId as taskId, taskName as taskName, checklistName as checklistName, jobCode as jobCode
    from RankedTaskExecutions rte
    where rte.row_num = 1
          """;
  public static final String GET_ALL_COMPLETED_PREREQUISITE_TASK_DETAILS = """
          WITH TaskExecutionPreRequisites AS (SELECT te.id as id, te.tasks_id as taskId, te.order_tree as orderTree, t.name as taskName, te.state as state
                                           FROM task_dependencies td
                                                    inner join task_executions te on te.tasks_id = td.prerequisite_task_id
                                                    inner join tasks t on t.id = td.prerequisite_task_id
                                           WHERE dependent_task_id = :dependantTaskId and te.jobs_id = :jobId),
          
          RankedTaskExecutions AS (SELECT id, taskId, taskName, ROW_NUMBER() OVER (PARTITION BY taskId ORDER BY orderTree DESC) AS row_num, state
                                        FROM TaskExecutionPreRequisites
                                        )
          
          SELECT DISTINCT id as id, taskId as taskId, taskName as taskName
          from RankedTaskExecutions rte
          where rte.row_num = 1 and rte.state in ('COMPLETED', 'SKIPPED', 'COMPLETED_WITH_EXCEPTION')
          """;
  public static final String GET_ALL_REFERENCED_PARAMETERS_OF_AN_AUTOINITIALISED_PARAMETER = """
    SELECT aip.referenced_parameters_id as id from auto_initialized_parameters aip
    WHERE aip.auto_initialized_parameters_id = :autoInitializedParameterId
""";

  public static final String GET_PARAMETERS_USED_IN_NUMBER_CRITERIA_VALIDATION = """
    SELECT DISTINCT :parameterId AS id
    FROM parameters p,
         jsonb_array_elements(p.validations) AS validation,
         jsonb_array_elements(validation -> 'criteriaValidations') AS criteriaValidation
    WHERE p.checklists_id = :checklistId
      AND p.archived = false
      AND p.type = 'NUMBER'
      AND jsonb_typeof(p.validations) = 'array'
      AND jsonb_array_length(p.validations) > 0
      AND validation ->> 'validationType' = 'CRITERIA'
      AND validation -> 'criteriaValidations' IS NOT NULL
      AND jsonb_typeof(validation -> 'criteriaValidations') = 'array'
      AND jsonb_array_length(validation -> 'criteriaValidations') > 0
      AND criteriaValidation ->> 'criteriaType' = 'PARAMETER'
      AND (
           criteriaValidation ->> 'valueParameterId' = :parameterId
           OR criteriaValidation ->> 'lowerValueParameterId' = :parameterId
           OR criteriaValidation ->> 'upperValueParameterId' = :parameterId
      )
""";

  public static final String GET_ALL_PARAMETERS_WHERE_PROPERTY_ID_IS_USED_IN = """
    SELECT p.id as id
    from parameters p
    join checklists c ON p.checklists_id = c.id
    where c.state != 'DEPRECATED'
      and c.archived = false
      and p.archived = false
      and p.metadata is not null
      and (p.metadata != CAST('{}' AS JSONB))
      and p.metadata ->> 'propertyId' = :propertyId
    """;
  public static final String GET_ALL_PARAMETER_VALUES_BY_TASK_EXECUTION_ID = """
    SELECT pv.id                       AS id,
           pv.parameters_id            AS parameterid,
           pv.value                    AS value,
           CAST(pv.choices AS TEXT)    AS choices,
           CAST(p.data AS TEXT)        AS data,
           pv.reason                   AS reason,
           p.type                      AS type,
           pv.state                    AS state,
           CAST(p.validations AS TEXT) AS validations,
           pv.has_exceptions           AS hasExceptions,
           pv.hidden                   AS hidden
    FROM parameter_values pv
             INNER JOIN parameters p ON pv.parameters_id = p.id
    WHERE task_executions_id = :taskExecutionId
    """;
  public static final String ARE_ALL_PARAMETER_VALUES_HIDDEN_BY_TASK_EXECUTION_ID = """
    SELECT NOT EXISTS (
        SELECT 1
        FROM parameter_values pv
        WHERE pv.task_executions_id = :taskExecutionId
        AND pv.hidden = false
    ) AS allHidden
    """;
  public static final String GET_ALL_MEDIAS_DATA_BY_TASK_EXECUTION_ID = """
        SELECT pv.id as parameterValueId, pvmm.medias_id as mediaId FROM parameter_value_media_mapping pvmm
        inner join parameter_values pv on pvmm.parameter_values_id = pv.id
        where task_executions_id = :taskExecutionId
    """;
  public static final String GET_USER_ROLES = """
    SELECT r.id as id, r.name as name  from roles r inner join user_roles_mapping urm on urm.roles_id = r.id  where users_id = :userId 
    """;
  public static final String GET_JOB_STATE_BY_JOB_ID = """
    SELECT j.state
    FROM Job j
    WHERE j.id = :jobId
    """;

  public static final String DELETE_BY_VARIATION_ID = """
    DELETE from variations where id = :variationId
        """;
    public static final String INCREASE_ORDER_TREE_BY_ONE_AFTER_PARAMETER = """
      UPDATE parameters p
      set order_tree = p.order_tree + 1
      where p.tasks_id = :taskId
        and p.order_tree >= :orderTree
        and p.id <> :parameterId
      """;
  public static final String INCREASE_ORDER_TREE_BY_ONE_AFTER_TASK = """
    UPDATE tasks SET order_tree = order_tree + 1 WHERE stages_id = :stageId AND order_tree >= :orderTree AND id <> :taskId
          """;
  public static final String INCREASE_ORDER_TREE_BY_ONE_AFTER_STAGE = """
          UPDATE stages SET order_tree = order_tree + 1 WHERE checklists_id = :checklistId AND order_tree > :orderTree AND id <> :stageId
    """;
  public static final String GET_ALL_TASK_IDS_BY_STAGE_ID = """
    SELECT id from tasks where stages_id = :stageId and archived = false
    """;
  public static final String FIND_PARAMETER_IDS_BY_TASK_ID = """
    SELECT id
    FROM parameters
    WHERE tasks_id = :taskId
    """;

  public static final String WAS_USER_RESTRICTED_FROM_RECALLING_OR_REVISING_CHECKLIST = """
    SELECT
        CASE 
            WHEN SUM(CASE WHEN ccm.type = 'REVIEWER' OR ccm.type = 'SIGN_OFF_USER' THEN 0 ELSE 1 END) = 0 
            THEN true 
            ELSE false 
        END AS isRestricted
    FROM 
        checklist_collaborator_mapping ccm
    JOIN 
        versions v1 ON v1.self = ccm.checklists_id
    JOIN 
        versions v2 ON v2.self = :checklistId AND v1.ancestor = v2.ancestor
    WHERE 
        ccm.users_id = :userId
    """;
  public static final String IS_ACTIVE_SCHEDULER_EXIST_FOR_GIVEN_CHECKLIST = """
    SELECT CASE
               WHEN COUNT(s.id) > 0 THEN true
               ELSE false
               END
    FROM schedulers s
    WHERE s.checklists_id = :checklistId
      AND s.archived = FALSE
      AND s.state != 'DEPRECATED'
    """;
  public static final String GET_LATEST_JOB_ANNOTATION = """
      SELECT * FROM job_annotations
      WHERE jobs_id = :jobId
      ORDER BY id DESC LIMIT 1
      """;

  public static final String GET_ALL_PARAMETERS_USED_IN_DATE_DATE_TIME_VALIDATIONS = """
    SELECT DISTINCT CAST(dateTimeValidation ->> 'referencedParameterId' AS bigint) AS id
    FROM parameters p,
         jsonb_array_elements(p.validations) AS validation,
         jsonb_array_elements(validation -> 'dateTimeParameterValidations') AS dateTimeValidation
    WHERE p.archived = false
      AND p.type IN ('DATE', 'DATE_TIME')
      AND jsonb_typeof(p.validations) = 'array'
      AND jsonb_typeof(validation -> 'dateTimeParameterValidations') = 'array'
      AND dateTimeValidation ->> 'selector' = 'PARAMETER'
      AND dateTimeValidation ->> 'referencedParameterId' IS NOT NULL
      AND CAST(dateTimeValidation ->> 'referencedParameterId' AS bigint) = :parameterId
    """;


  public static final String GET_ALL_CORRECTIONS_BY_PARAMETER_VALUE_ID = """
    SELECT
        c.id,
        c.created_at AS createdAt,
        c.created_by AS initiatorUserId,
        c.task_executions_id AS taskExecutionId,
        c.code AS code,
        c.status AS status,
        c.initiators_reason AS initiatorsReason,
        c.correctors_reason AS correctorsReason,
        c.reviewers_reason AS reviewersReason,
        c.old_value AS oldValue,
        c.new_value AS newValue,
        CAST(c.old_choices AS VARCHAR) AS oldChoices,
        CAST(c.new_choices AS VARCHAR) AS newChoices,
        j.id AS jobId,
        j.code AS jobCode,
        p.label AS parameterName,
        p.id AS parameterId,
        cl.name AS processName,
        u.first_name AS initiatorFirstName,
        u.last_name AS initiatorLastName,
        u.employee_id AS initiatorEmployeeId,
        t.name AS taskName
    FROM
        corrections c
    JOIN
        parameter_values pv ON c.parameter_values_id = pv.id
    JOIN
        jobs j ON c.jobs_id = j.id
    JOIN
        parameters p ON pv.parameters_id = p.id
    LEFT JOIN
        checklists cl ON j.checklists_id = cl.id
    INNER JOIN
        users u ON u.id = c.created_by
    JOIN
        tasks t ON t.id = p.tasks_id
    WHERE
        pv.id = :parameterValueId
    ORDER BY
        c.id ASC
    """;

  public static final String GET_ALL_PARAMETER_VALUES_BY_JOB_ID = """
    SELECT pv.id                       AS id,
           pv.parameters_id            AS parameterId,
           pv.value                    AS value,
           CAST(pv.choices AS TEXT)    AS choices,
           CAST(p.data AS TEXT)        AS data,
           pv.reason                   AS reason,
           p.type                      AS type,
           pv.state                    AS state,
           CAST(p.validations AS TEXT) AS validations,
           pv.has_exceptions           AS hasExceptions,
           pv.hidden                   AS hidden,
           pv.has_corrections          AS hasCorrections
    FROM parameter_values pv
             INNER JOIN parameters p ON pv.parameters_id = p.id
    WHERE jobs_id = :jobId
    """;

  public static final String RECALL_VERIFICATION_STATE_FOR_HIDDEN_PARAMETER_VALUES = """
    WITH updated_values AS (
        UPDATE parameter_values pv
        SET state = 'BEING_EXECUTED'
        FROM parameter_verifications pvf
        WHERE pv.id IN (:hideIds)
        AND pv.hidden = true
        AND pvf.parameter_values_id = pv.id
        AND (pvf.verification_type = 'PEER'
        OR pvf.verification_type = 'SELF')
        AND pv.state = 'APPROVAL_PENDING'
        RETURNING pv.id
    )
  
    DELETE FROM parameter_verifications pvf
    WHERE pvf.parameter_values_id IN (SELECT id FROM updated_values)
  """;
  public static final String RECALL_VERIFICATION_STATE_FOR_HIDDEN_PARAMETER_VALUES_WITH_EXCEPTIONS = """
    WITH updated_values AS (
        UPDATE parameter_values pv
        SET state = ex.previous_state,
            has_exceptions = CASE
                WHEN NOT EXISTS (
                    SELECT 1
                    FROM exceptions exe
                    WHERE exe.parameter_values_id = pv.id
                    AND exe.status != 'INITIATED'
                )
                THEN false
                ELSE pv.has_exceptions
            END
        FROM exceptions ex
        WHERE pv.id = ex.parameter_values_id
        AND pv.hidden = true
        AND ex.status = 'INITIATED'
        AND pv.id IN (:hideIds)
        RETURNING ex.id
    )

    DELETE FROM exceptions ex
    WHERE ex.id IN (SELECT id FROM updated_values)
""";

  public static final String GET_TASK_EXECUTION_LITE_BY_JOB_ID= """
    WITH parameter_values_cte
         AS (SELECT pv.task_executions_id,
                    Sum(CASE
                          WHEN pv.hidden = false THEN 1
                          ELSE 0
                        END) AS non_hidden_count
             FROM   parameter_values pv
             WHERE  pv.jobs_id = :jobId
             GROUP  BY pv.task_executions_id),
         task_executions_cte
         AS (SELECT te.id,
                    te.order_tree,
                    te.state,
                    te.type,
                    te.tasks_id,
                    te.jobs_id
             FROM   task_executions te
             WHERE  te.jobs_id = :jobId)
    SELECT te.id         AS id,
           te.order_tree AS orderTree,
           te.state      AS state,
           te.type       AS type,
           te.tasks_id   AS taskId,
           CASE
             WHEN COALESCE(pv_cte.non_hidden_count, 0) > 0 THEN false
             ELSE true
           END           AS hidden
    FROM   task_executions_cte te
           LEFT JOIN parameter_values_cte pv_cte
                  ON te.id = pv_cte.task_executions_id
    ORDER  BY te.order_tree ASC
    """;

  public static final String IS_CORRECTION_PENDING = """
                SELECT EXISTS (
                    WITH latest_corrections AS (
                        SELECT
                            c.parameter_values_id,
                            MAX(c.id) AS latest_id
                        FROM corrections c
                        WHERE c.jobs_id = :jobId
                        GROUP BY c.parameter_values_id
                    )
                    SELECT 1
                    FROM corrections c
                    JOIN latest_corrections lc
                      ON c.id = lc.latest_id
                    LEFT JOIN correctors cr
                      ON c.id = cr.corrections_id
                    LEFT JOIN reviewers r
                      ON c.id = r.corrections_id
                    WHERE c.status IN ('INITIATED', 'CORRECTED')
                      AND (:userId IS NULL
                           OR :userId IN (cr.users_id, r.users_id, c.created_by))
                ) AS has_initiated_or_corrected
          """;

  public static final String IS_VERIFICATION_PENDING_ON_USER = """
    SELECT EXISTS (WITH latest_verifications AS
                            (SELECT pv.parameter_values_id,
                                    MAX(pv.id) AS latest_id
                             FROM parameter_verifications pv
                             WHERE pv.jobs_id = :jobId
                               AND pv.users_id = :userId
                             GROUP BY pv.parameter_values_id)
                   SELECT 1
                   FROM parameter_verifications pvf
                            INNER JOIN latest_verifications lvf ON pvf.id = lvf.latest_id
                            INNER JOIN parameter_values pv ON pvf.parameter_values_id = pv.id
                            INNER JOIN task_executions te ON pv.task_executions_id = te.id
    
                   WHERE pvf.verification_status = 'PENDING'
                     AND te.state NOT IN ('COMPLETED_WITH_EXCEPTION', 'SKIPPED')
                     AND pvf.users_id = :userId) AS has_pending_verifications
    """;

  public static final String TRAINED_USER_MATERIALIZED_VIEW = """
    CREATE MATERIALIZED VIEW IF NOT EXISTS :viewName AS
    SELECT tu.users_id       AS userid,
           tu.user_groups_id AS usergroupid,
           tutm.tasks_id     AS taskid
    FROM trained_user_tasks_mapping tutm
             INNER JOIN trained_users tu ON tutm.trained_users_id = tu.id
    WHERE tu.checklists_id = :checklistId
      AND tu.facilities_id = :facilityId
    """;

  public static final String ADD_INDEX_IN_TRAINED_USER_MATERIALISED_VIEW_ON_USER_ID = """
    CREATE INDEX IF NOT EXISTS idx_userid ON :viewName (userid)
    """;

  public static final String ADD_INDEX_IN_TRAINED_USER_MATERIALISED_VIEW_ON_USER_GROUP_ID = """
      CREATE INDEX IF NOT EXISTS idx_userid ON :viewName (usergroupid)
    """;

  public static final String GET_ALL_ASSIGNED_TRAINED_USER = """
    WITH eligibleusergroups AS (SELECT ug.id          AS usergroupid,
                                       ug.name        AS usergroupname,
                                       ug.description AS usergroupdescription
                                FROM user_groups ug
                                         INNER JOIN :viewName tui ON tui.usergroupid = ug.id),
    
         eligibleusers AS (SELECT u.id          AS userid,
                                  u.employee_id AS employeeid,
                                  u.first_name  AS firstname,
                                  u.last_name   AS lastname,
                                  u.email       AS emailid
                           FROM users u
                                    INNER JOIN :viewName tui ON tui.userid = u.id
                           WHERE (CAST(:query AS VARCHAR) IS NULL OR
                                  (u.first_name ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
                                      OR u.last_name ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')))
                              OR u.email ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
                              OR u.username ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%')
                              OR u.employee_id ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%'))
    
    SELECT tui.userid              AS userid,
           tui.usergroupid         AS usergroupid,
           tui.taskid              AS taskid,
           ug.usergroupname        AS usergroupname,
           ug.usergroupdescription AS usergroupdescription,
           u.employeeid            AS employeeid,
           u.firstname             AS firstname,
           u.lastname              AS lastname,
           u.emailid               AS emailid
    FROM :viewName tui
             LEFT JOIN eligibleusergroups ug ON tui.usergroupid = ug.usergroupid
             LEFT JOIN eligibleusers u ON tui.userid = u.userid
    """;
  public static final String REFRESH_MATERIALISED_TRAINED_USER_VIEW_FOR_CHECKLIST_ID = """
    REFRESH MATERIALIZED VIEW :viewName
    """;

  public static final String FIND_BY_CHECKLIST_PHASETYPE_TYPE_PHASE = """
        SELECT *
        FROM checklist_collaborator_mapping 
        WHERE checklists_id = :checklistId 
          AND phase_type = :phaseType 
          AND type = :type 
          AND phase = :phase
    """;

  public static final String GET_VERSION_BY_SELF = """
    SELECT * from versions WHERE self=:selfId
    """;

  public static final String IS_EXCEPTION_PENDING_ON_USER = """
    SELECT EXISTS (WITH latest_exceptions AS
                            (SELECT e.id      AS exception_id,
                                    MAX(e.id) AS latest_id
                             FROM exceptions e
                             WHERE e.jobs_id = :jobId
                             GROUP BY e.id)
                   SELECT 1
                   FROM exceptions e
                            JOIN latest_exceptions le
                                 ON e.id = le.latest_id
                            LEFT JOIN
                        (SELECT exceptions_id,
                                users_id
                         FROM exception_reviewers
                         WHERE users_id = :userId) er
                        ON e.id = er.exceptions_id
                   WHERE e.status = 'INITIATED'
                     AND (
                       e.created_by = :userId
                           OR er.users_id IS NOT NULL)) AS has_initiated_exceptions_or_reviewer
    """;

  public static final String IS_CJF_EXCEPTION_PENDING = """
    SELECT EXISTS(
        SELECT 1
        FROM parameter_values pv
        JOIN parameters p ON pv.parameters_id = p.id
        WHERE jobs_id = :jobId
          AND pv.task_executions_id IS NULL
          AND state IN ('BEING_EXECUTED', 'PENDING_FOR_APPROVAL')
          AND pv.hidden = false
          AND (
              p.is_mandatory = true
              OR (pv.value IS NOT NULL
                  OR (jsonb_typeof(pv.choices) = 'array' AND jsonb_array_length(pv.choices) > 0)
              )
          )
    )
            """;

  public static final String GET_TASK_PAUSE_RESUME_AUDIT_BY_TASK_EXECUTION_ID = """
          SELECT
              tet.id AS id,
              tet.paused_at AS pausedAt,
              tet.resumed_at AS resumedAt,
              tet.reason AS reason,
              tet.comment AS comment,
              tet.task_executions_id AS taskExecutionId,
              
              pausedUser.first_name AS pausedByFirstName,
              pausedUser.last_name AS pausedByLastName,
              pausedUser.employee_id AS pausedByEmployeeId,
              pausedUser.id AS pausedByUserId,

              resumedUser.first_name AS resumedByFirstName,
              resumedUser.last_name AS resumedByLastName,
              resumedUser.employee_id AS resumedByEmployeeId,
              resumedUser.id AS resumedByUserId
          FROM
              task_execution_timers tet
          LEFT JOIN
              users pausedUser ON tet.created_by = pausedUser.id
          LEFT JOIN
              users resumedUser ON tet.modified_by = resumedUser.id
          WHERE
              tet.task_executions_id = :taskExecutionId
              AND tet.reason != 'TASK_COMPLETED'
          ORDER BY
              tet.id ASC
              """;
  
  public static final String GET_LATEST_PARAMETER_VALUES = """
    WITH latest_parameter_values AS (SELECT pv.parameters_id,
                                            MAX(pv.id) AS max_id
                                     FROM parameter_values pv
                                     WHERE pv.jobs_id = :jobId
                                     GROUP BY pv.parameters_id)
    SELECT pv.*
    FROM parameter_values pv
             INNER JOIN
         latest_parameter_values lpv
         ON
             pv.id = lpv.max_id
    ORDER BY pv.parameters_id
    """;
  public static final String DELETE_USER_FROM_TRAINED_USERS = """
    delete
    from trained_users
    WHERE users_id = :userId
    """;
  public static final String DELETE_USER_FROM_USER_GROUP_MEMBER = """
    delete
    from user_group_members
    WHERE users_id = :userId
    """;

  public static final String FIND_USER_GROUP_MEMBERS_BY_USER_ID = """
    SELECT ugm.* 
    FROM user_group_members ugm 
    WHERE ugm.users_id = :userId
    """;

  public static final String UPDATE_HAS_EXECUTOR_LOCK_BY_IDS = "UPDATE tasks SET has_executor_lock = :hasExecutorLock WHERE id IN (:taskIds)";

  public static final String BULK_LOAD_PROPERTIES_FOR_CHECKLISTS = """
    SELECT 
        cpv.checklists_id as checklistId,
        fupm.label_alias as propertyLabel,
        cpv.value as propertyValue
    FROM checklist_property_values cpv
    INNER JOIN facility_use_case_property_mapping fupm ON cpv.facility_use_case_property_mapping_id = fupm.id
    WHERE cpv.checklists_id IN (:checklistIds)
      AND fupm.label_alias IS NOT NULL
    ORDER BY cpv.checklists_id, fupm.label_alias
    """;

  public static final String BULK_LOAD_JOBS_WITH_FILTERS_STATIC = """
    WITH filtered_jobs AS (
        SELECT 
            j.id,
            j.code,
            j.state,
            j.created_at AS createdAt,           
            j.checklists_id AS checklistsId,    
            c.code AS checklistCode,             
            c.name AS checklistName,            
            u.first_name AS firstName,          
            u.last_name AS lastName,            
            u.employee_id AS employeeId
        FROM jobs j
        INNER JOIN checklists c ON j.checklists_id = c.id  
        INNER JOIN users u ON j.created_by = u.id
        WHERE j.organisations_id = :organisationId
          AND (:facilityId = -1 OR j.facilities_id = :facilityId)
          -- Regular filter conditions
          AND (j.state IN :stateFilter)
          AND (:useCaseIdFilter IS NULL OR j.use_cases_id = :useCaseIdFilter)
          AND (CAST(:checklistAncestorIdFilter AS VARCHAR) IS NULL OR j.checklist_ancestor_id = :checklistAncestorIdFilter)
          AND (CAST(:codeFilter AS VARCHAR) IS NULL OR j.code ILIKE CONCAT('%', COALESCE(CAST(:codeFilter AS VARCHAR), ''), '%'))
          AND (CAST(:checklistNameFilter AS VARCHAR) IS NULL OR c.name ILIKE CONCAT('%', COALESCE(CAST(:checklistNameFilter AS VARCHAR), ''), '%'))
          AND (:expectedEndDateLt IS NULL OR j.expected_end_date < CAST(:expectedEndDateLt AS BIGINT))
          AND (:expectedStartDateGt IS NULL OR j.expected_start_date > CAST(:expectedStartDateGt AS BIGINT))
          AND (:expectedStartDateLt IS NULL OR j.expected_start_date < CAST(:expectedStartDateLt AS BIGINT))
          AND (:expectedStartDateIsNull IS NULL OR 
               (:expectedStartDateIsNull = true AND j.expected_start_date IS NULL) OR
               (:expectedStartDateIsNull = false AND j.expected_start_date IS NOT NULL))
          -- StartedAt filters
          AND (:startedAtGte IS NULL OR j.started_at >= CAST(:startedAtGte AS BIGINT))
          AND (:startedAtLte IS NULL OR j.started_at <= CAST(:startedAtLte AS BIGINT))
          -- ObjectId filter using the same pattern as getJobIdsHavingObjectInChoicesForAllParameters
          AND (CAST(:objectIdChoicesJson AS VARCHAR) IS NULL OR j.id IN (
              SELECT pv.jobs_id 
              FROM parameter_values pv 
              INNER JOIN parameters p ON p.id = pv.parameters_id 
              WHERE p.type = 'RESOURCE'
                AND pv.choices @> CAST(:objectIdChoicesJson AS jsonb)
          ))
          -- Created by filter
          AND (:createdById IS NULL OR j.created_by = :createdById)
        ORDER BY j.id desc
    )
    SELECT * FROM filtered_jobs
    """;

  private Queries() {
  }

}
