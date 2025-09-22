package com.leucine.streem.handler;

import com.leucine.streem.dto.projection.TrainedUsersView;
import com.leucine.streem.dto.projection.impl.TrainedUsersViewImpl;
import com.leucine.streem.repository.ITrainedUserTaskMappingRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class TrainedUserMaterializedViewHandler {
  private final ITrainedUserTaskMappingRepository trainedUsersTaskMappingRepository;

  @PersistenceContext
  private EntityManager entityManager;

  @Transactional
  public List<TrainedUsersView> getAllTrainedUserViews(Long checklistId, Long facilityId, String query) {
    String viewName = "trained_user_ids_mv_" + checklistId + facilityId;
    log.info(viewName);
    String createMaterializeView = String.format("""
            CREATE MATERIALIZED VIEW IF NOT EXISTS %s AS
            SELECT tu.users_id AS userid,
                   tu.user_groups_id AS usergroupid,
                   tutm.tasks_id AS taskid
            FROM trained_user_tasks_mapping tutm
                 INNER JOIN trained_users tu ON tutm.trained_users_id = tu.id
            WHERE tu.checklists_id = %d
              AND tu.facilities_id = %d
            """, viewName, checklistId, facilityId);

    log.info("Creating materialized view for checklistId: {} and facilityId: {}", checklistId, facilityId);
    entityManager.createNativeQuery(createMaterializeView).executeUpdate();;
    log.info("Refreshing materialized view for checklistId: {} and facilityId: {}", checklistId, facilityId);

    String refreshMaterializedView = String.format("REFRESH MATERIALIZED VIEW %s", viewName);
    entityManager.createNativeQuery(refreshMaterializedView).executeUpdate();;
    log.info("Adding index in materialized view for checklistId: {} and facilityId: {}", checklistId, facilityId);

    String addIndexInMaterializedViewOnUserId = String.format("CREATE INDEX IF NOT EXISTS idx_%s_user_id ON %s(userid)", viewName, viewName);
    entityManager.createNativeQuery(addIndexInMaterializedViewOnUserId).executeUpdate();;

    log.info("Adding index in materialized view for checklistId: {} and facilityId: {}", checklistId, facilityId);
    String addIndexInMaterializedViewOnUserGroupId = String.format("CREATE INDEX IF NOT EXISTS idx_%s_user_group_id ON %s(usergroupid)", viewName, viewName);
    entityManager.createNativeQuery(addIndexInMaterializedViewOnUserGroupId).executeUpdate();;

    log.info("Getting result view for checklistId: {} and facilityId: {}", checklistId, facilityId);
    String sql = String.format("""
            WITH eligibleusergroups AS (SELECT ug.id          AS usergroupid,
                                               ug.name        AS usergroupname,
                                               ug.description AS usergroupdescription
                                        FROM user_groups ug
                                                 INNER JOIN %s tui ON tui.usergroupid = ug.id),
                
                 eligibleusers AS (SELECT u.id          AS userid,
                                          u.employee_id AS employeeid,
                                          u.first_name  AS firstname,
                                          u.last_name   AS lastname,
                                          u.email       AS emailid
                                   FROM users u
                                            INNER JOIN %s tui ON tui.userid = u.id
                                   WHERE (CAST(:query AS VARCHAR) IS NULL OR
                                          (u.first_name ILIKE CONCAT('%%', CAST(:query AS VARCHAR), '%%')
                                              OR u.last_name ILIKE CONCAT('%%', CAST(:query AS VARCHAR), '%%')))
                                      OR u.email ILIKE CONCAT('%%', CAST(:query AS VARCHAR), '%%')
                                      OR u.username ILIKE CONCAT('%%', CAST(:query AS VARCHAR), '%%')
                                      OR u.employee_id ILIKE CONCAT('%%', CAST(:query AS VARCHAR), '%%'))
                
            SELECT tui.userid              AS userid,
                   tui.usergroupid         AS usergroupid,
                   tui.taskid              AS taskid,
                   ug.usergroupname        AS usergroupname,
                   ug.usergroupdescription AS usergroupdescription,
                   u.employeeid            AS employeeid,
                   u.firstname             AS firstname,
                   u.lastname              AS lastname,
                   u.emailid               AS emailid
            FROM %s tui
                     LEFT JOIN eligibleusergroups ug ON tui.usergroupid = ug.usergroupid
                     LEFT JOIN eligibleusers u ON tui.userid = u.userid
            """, viewName, viewName, viewName);

    log.info(sql);

    List<Object[]> results = entityManager.createNativeQuery(sql)
      .setParameter("query", query)
      .getResultList();

    return results.stream()
      .map(result -> new TrainedUsersViewImpl(
        result[0] != null ? result[0].toString() : null,   // userid (BigInteger to String or null)
        result[1] != null ? result[1].toString() : null,   // usergroupid (BigInteger to String or null)
        result[2] != null ? result[2].toString() : null,   // taskid (BigInteger to String or null)
        result[3] != null ? (String) result[3] : null,     // usergroupname (String or null)
        result[4] != null ? (String) result[4] : null,     // usergroupdescription (String or null)
        result[5] != null ? (String) result[5] : null,     // employeeid (String or null)
        result[6] != null ? (String) result[6] : null,     // firstname (String or null)
        result[7] != null ? (String) result[7] : null,     // lastname (String or null)
        result[8] != null ? (String) result[8] : null      // emailid (String or null)
      ))
      .collect(Collectors.toList());

  }
}
