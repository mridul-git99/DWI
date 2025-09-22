package com.leucine.streem.service;

import com.leucine.streem.dto.ChecklistAuditDto;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.PrincipalUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface IChecklistAuditService {
  Page<ChecklistAuditDto> getAuditsByChecklistId(Long checklistId, String filters, Pageable pageable) throws StreemException;

  void create(Long checklistId, String code, PrincipalUser principalUser);

  void createStage(Long checklistId,Integer stageOrderTree, PrincipalUser principalUser);

  void updateStage(Long checklistId, Stage stage, PrincipalUser principalUser);

  void createTask(Long checklistId, Task task, PrincipalUser principalUser);

  void updateTask(Long checklistId, Task task, PrincipalUser principalUser);

  void addParameter(Long checklistId, Task task, Parameter parameter,  PrincipalUser principalUser);

  void createParameter(Long checklistId, Parameter parameter, PrincipalUser principalUser);

  void updateParameter(Long id, Parameter parameter, PrincipalUser principalUser);

  void mapParameterToTask(Long id, Task task, Parameter parameter, PrincipalUser principalUser);

  void unmapParameter(Long id, Task task, Parameter parameter, PrincipalUser principalUser);

  void publish(Long checklistId, String code, PrincipalUser principalUser);

  void archive(Long checklistId, String code, String reason, PrincipalUser principalUser);

  void archiveStage(Long checklistId, Stage stage, PrincipalUser principalUser);

  void archiveTask(Long checklistId, Task task, PrincipalUser principalUser);

  void archiveParameter(Long checklistId, Task task, Parameter parameter, PrincipalUser principalUser);

  void unarchive(Long checklistId, String code, String reason, PrincipalUser principalUser);

  void revise(Long checklistId, String checklistCode, Long prototypeId, String prototypeCode, PrincipalUser principalUser);

  void deprecate(Long checklistId, String code, String revisedChecklistCode, PrincipalUser principalUser);

  void recall(Long checklistId, String code, String reason, PrincipalUser principalUser);

  void importChecklist(Long checklistId, String code, PrincipalUser principalUser);

  void mapTrainedUsers(Long checklistId, PrincipalUser principalUser, List<User> usersList);

  void unmapTrainedUsers(Long checklistId, PrincipalUser principalUser, List<User> usersList, String reason);

  void mapTrainedUserGroups(Long checklistId, PrincipalUser principalUser, List<UserGroup> userGroupList);

  void unmapTrainedUserGroups(Long checklistId, PrincipalUser principalUser, List<UserGroup> userGroupList, String reason);

  void assignTrainedUsersToTask(Long checklistId, PrincipalUser principalUser, Map<User, List<Task>> userTaskAssignments);

  void assignTrainedUserGroupsToTask(Long checklistId, PrincipalUser principalUser, Map<UserGroup, List<Task>> userGroupTaskAssignments);

  void unAssignTrainedUsersToTask(Long checklistId, PrincipalUser principalUser, Map<User, List<Task>> userTaskAssignments);

  void unAssignTrainedUserGroupsToTask(Long checklistId, PrincipalUser principalUser, Map<UserGroup, List<Task>> userGroupTaskAssignments);

  void unmapTrainedUsersDueToArchival(List<TrainedUser> trainedUsers, String reason, PrincipalUser archivedByUser, PrincipalUser systemPrincipalUser);

  void downloadProcessTemplatePdf(Long checklistId, String name, String code, PrincipalUser principalUser);

}
