package com.leucine.streem.service.impl;

import com.leucine.streem.constant.Action;
import com.leucine.streem.constant.Operator;
import com.leucine.streem.dto.ChecklistAuditDto;
import com.leucine.streem.dto.mapper.IChecklistAuditMapper;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.model.helper.SpecificationBuilder;
import com.leucine.streem.model.helper.search.SearchCriteria;
import com.leucine.streem.repository.IChecklistAuditRepository;
import com.leucine.streem.repository.IStageRepository;
import com.leucine.streem.service.IChecklistAuditService;
import com.leucine.streem.util.DateTimeUtils;
import com.leucine.streem.util.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChecklistAuditService implements IChecklistAuditService {
    public static final String CREATE_CHECKLIST = "{0} {1} (ID:{2}) created the Prototype (ID:{3})";
    public static final String CREATE_STAGE = "{0} {1} (ID:{2}) created the Stage {3}";
    public static final String UPDATE_STAGE = "{0} {1} (ID:{2}) provided a name \"{3}\" to Stage {4}";
    public static final String CREATE_TASK = "{0} {1} (ID:{2}) created the Task {3} of the Stage \"{4}\"";
    public static final String UPDATE_TASK = "{0} {1} (ID:{2}) provided a name \"{3}\" to Task {4} of the Stage \"{5}\"";
    public static final String ADD_PARAMETER = "{0} {1} (ID:{2}) added the Parameter \"{3}\"  of type \"{4}\"  to the Task \"{5}\" as Task{6} of the Stage \"{7}\"";
    public static final String UNMAP_PARAMETER_TASK = "{0} {1} (ID:{2}) removed the Parameter \"{3}\"  of type \"{4}\"  from the Task \"{5}\" as Task{6} of the Stage \"{7}\"";
    public static final String CREATE_PARAMETER = "{0} {1} (ID:{2}) created the Parameter \"{3}\" of type \"{4}\"";
    public static final String UPDATE_PARAMETER = "{0} {1} (ID:{2}) updated the Parameter \"{3}\" of type \"{4}\" of the Task \"{5}\" as Task{6} of the Stage \"{7}\"";
    public static final String UPDATE_PARAMETER_CJF = "{0} {1} (ID:{2}) updated the Parameter \"{3}\" of type \"{4}\"";
    public static final String PUBLISH_CHECKLIST = "{0} {1} (ID:{2}) published the Process (ID:{3})";
    public static final String ARCHIVE_CHECKLIST = "{0} {1} (ID:{2}) archived Process (ID:{3}) stating reason \"{4}\"";
    public static final String ARCHIVE_STAGE = "{0} {1} (ID:{2}) archived the Stage \"{3}\" as Stage {4}";
    public static final String ARCHIVE_TASK = "{0} {1} (ID:{2}) archived the Task \"{3}\" as Task{4} of Stage \"{5}\"";
    public static final String ARCHIVE_PARAMETER = "{0} {1} (ID:{2}) archived the Parameter \"{3}\" of type \"{4}\" of the Task \"{5}\" as Task{6} of the Stage \"{7}\"";
    public static final String ARCHIVE_PARAMETER_CJF = "{0} {1} (ID:{2}) archived the Parameter \"{3}\" of type \"{4}\"";
    public static final String UNARCHIVE_CHECKLIST = "{0} {1} (ID:{2}) unarchived Process (ID:{3}) stating reason \"{4}\"";
    public static final String REVISE_CHECKLIST_PARENT = "{0} {1} (ID:{2}) created a new Prototype (ID:{3}) as a revision to this Process";
    public static final String REVISE_CHECKLIST_PROTOTYPE = "{0} {1} (ID:{2}) created a this Prototype (ID:{3}) as a revision to Process (ID:{4})";
    public static final String DEPRECATE_CHECKLIST = "{0} {1} (ID:{2}) deprecated this Process (ID:{3}) by publishing the Process (ID:{4}) as a revision to this Process";
    public static final String RECALLED_PROTOTYPE = "{0} {1} (ID:{2}) recalled the Prototype: (ID:{3}) stating reason \"{4}\"";
    public static final String IMPORT_CHECKLIST = "{0} {1} (ID:{2}) created a new Prototype (ID:{3}) by importing a Process";
    public static final String MAP_TRAINED_USER_TO_CHECKLIST = "{0} {1} (ID:{2}) added {3} {4} (ID:{5}) as Trained User of this process";
    public static final String MAP_TRAINED_USER_GROUPS_TO_CHECKLIST = "{0} {1} (ID:{2}) added User Group {3} as Trained User of this process";
    public static final String UNMAP_TRAINED_USER_FROM_CHECKLIST = "{0} {1} (ID:{2}) stated reason \"{3}\" to remove {4} {5} (ID:{6}) as Trained User of this process";
    public static final String UNMAP_TRAINED_USER_GROUPS_FROM_CHECKLIST = "{0} {1} (ID:{2}) stated reason \"{3}\" to remove User Group {4} as Trained User of this process";
    public static final String ASSIGN_TRAINED_USER_TO_TASK = "{0} {1} (ID:{2}) assigned {3} {4} (ID:{5}) as Trained User to Task {6} of the Stage {7}";
    public static final String ASSIGN_TRAINED_USER_GROUP_TO_TASK = "{0} {1} (ID:{2}) assigned User Group {3} as Trained User to Task {4} of the Stage {5}";
    public static final String UNASSIGN_TRAINED_USER_TO_TASK = "{0} {1} (ID:{2}) unassigned {3} {4} (ID:{5}) as Trained User from Task {6} of the Stage {7}";
    public static final String UNASSIGN_TRAINED_USER_GROUP_TO_TASK = "{0} {1} (ID:{2}) unassigned User Group {3} as Trained User from Task {4} of the Stage {5}";
    public static final String USER_ARCHIVAL_TRAINED_USER_REMOVAL = "{0} (ID:{1}) removed user {2} {3} (ID:{4}) from trained users for process \"{5}\" due to user archival by {6} {7} (ID:{8}) stating reason: \"{9}\"";
    public static final String DOWNLOAD_PROCESS_TEMPLATE_PDF = "{0} {1} (ID:{2}) downloaded the Process Template PDF for {3} (ID:{4})";

    private final IChecklistAuditRepository checklistAuditRepository;
    private final IChecklistAuditMapper checklistAuditMapper;
    private final IStageRepository stageRepository;

    @Override
    public Page<ChecklistAuditDto> getAuditsByChecklistId(Long checklistId, String filters, Pageable pageable) throws StreemException {
        SearchCriteria mandatorySearchCriteria = new SearchCriteria()
                .setField("checklistId")
                .setOp(Operator.Search.EQ.toString())
                .setValues(Collections.singletonList(checklistId));
        Specification<ChecklistAudit> specification = SpecificationBuilder.createSpecification(filters, Collections.singletonList(mandatorySearchCriteria));

        Page<ChecklistAudit> checklistAudits = checklistAuditRepository.findAll(specification, pageable);
        List<ChecklistAuditDto> checklistAuditDtos = checklistAuditMapper.toDto(checklistAudits.getContent());
        return new PageImpl<>(checklistAuditDtos, pageable, checklistAudits.getTotalElements());
    }

    @Override
    public void create(Long checklistId, String code, PrincipalUser principalUser) {
        String details = formatMessage(CREATE_CHECKLIST, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), code);
        checklistAuditRepository.save(getChecklistAudit(details, checklistId, Action.ChecklistAudit.CREATE, principalUser));
        System.out.println("details = " + details);
    }

    @Override
    public void createStage(Long checklistId, Integer stageOrderTree, PrincipalUser principalUser) {
        String details = formatMessage(CREATE_STAGE, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), String.valueOf(stageOrderTree));
        checklistAuditRepository.save(getChecklistAudit(details, checklistId, Action.ChecklistAudit.CREATE, principalUser));
    }

    @Override
    public void updateStage(Long checklistId, Stage stage, PrincipalUser principalUser) {
        String details = formatMessage(UPDATE_STAGE, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), stage.getName(), stage.getOrderTree().toString());
        checklistAuditRepository.save(getChecklistAudit(details, checklistId, Action.ChecklistAudit.CREATE, principalUser));
    }

    @Override
    public void createTask(Long checklistId, Task task, PrincipalUser principalUser) {
      Stage taskStage = task.getStage();
      Integer stageOrderTree = taskStage.getOrderTree();
      String taskLocation = getTaskLocation(stageOrderTree, task.getOrderTree());
        String details = formatMessage(CREATE_TASK, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), taskLocation, taskStage.getName());
        checklistAuditRepository.save(getChecklistAudit(details, checklistId, Action.ChecklistAudit.CREATE, principalUser));
    }

    @Override
    public void updateTask(Long checklistId, Task task, PrincipalUser principalUser) {
        Stage taskStage = task.getStage();
        Integer stageOrderTree = taskStage.getOrderTree();
        String taskLocation = getTaskLocation(stageOrderTree, task.getOrderTree());
        String details = formatMessage(UPDATE_TASK, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), task.getName(), taskLocation, taskStage.getName());
        checklistAuditRepository.save(getChecklistAudit(details, checklistId, Action.ChecklistAudit.CREATE, principalUser));
    }

    @Override
    public void addParameter(Long checklistId, Task task, Parameter parameter, PrincipalUser principalUser) {
        Stage taskStage = task.getStage();
        Integer stageOrderTree = taskStage.getOrderTree();
        String taskLocation = getTaskLocation(stageOrderTree, task.getOrderTree());
        String details = formatMessage(ADD_PARAMETER, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), parameter.getLabel(), String.valueOf(parameter.getType()), task.getName(), taskLocation, taskStage.getName());
        checklistAuditRepository.save(getChecklistAudit(details, checklistId, Action.ChecklistAudit.CREATE, principalUser));
    }

    @Override
    public void createParameter(Long checklistId, Parameter parameter, PrincipalUser principalUser) {
        String details = formatMessage(CREATE_PARAMETER, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), parameter.getLabel(), String.valueOf(parameter.getType()));
        checklistAuditRepository.save(getChecklistAudit(details, checklistId, Action.ChecklistAudit.CREATE, principalUser));
    }

    @Override
    public void updateParameter(Long checklistId, Parameter parameter, PrincipalUser principalUser){
      String details = "";
      Task task = null;
      if(!Utility.isEmpty(parameter.getTask())) {
        task = parameter.getTask();
      }
      if(!Utility.isEmpty(task)) {
        Stage taskStage = task.getStage();
        Integer stageOrderTree = taskStage.getOrderTree();
        String taskLocation = getTaskLocation(stageOrderTree, task.getOrderTree());
        details = formatMessage(UPDATE_PARAMETER, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), parameter.getLabel(), String.valueOf(parameter.getType()), task.getName(), taskLocation, taskStage.getName());
      }else{
        details = formatMessage(UPDATE_PARAMETER_CJF, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), parameter.getLabel(), String.valueOf(parameter.getType()));
      }
      checklistAuditRepository.save(getChecklistAudit(details, checklistId, Action.ChecklistAudit.CREATE, principalUser));
    }

    @Override
    public void mapParameterToTask(Long checklistId, Task task, Parameter parameter, PrincipalUser principalUser) {
        Stage taskStage = task.getStage();
        Integer stageOrderTree = taskStage.getOrderTree();
        String taskLocation = getTaskLocation(stageOrderTree, task.getOrderTree());
        String details = formatMessage(ADD_PARAMETER, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), parameter.getLabel(), String.valueOf(parameter.getType()), task.getName(), taskLocation, taskStage.getName());
        checklistAuditRepository.save(getChecklistAudit(details, checklistId, Action.ChecklistAudit.CREATE, principalUser));
    }

    @Override
    public void unmapParameter(Long checklistId, Task task, Parameter parameter, PrincipalUser principalUser) {
        Stage taskStage = task.getStage();
        Integer stageOrderTree = taskStage.getOrderTree();
        String taskLocation = getTaskLocation(stageOrderTree, task.getOrderTree());
        String details = formatMessage(UNMAP_PARAMETER_TASK, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), parameter.getLabel(), String.valueOf(parameter.getType()), task.getName(), taskLocation, taskStage.getName());
        checklistAuditRepository.save(getChecklistAudit(details, checklistId, Action.ChecklistAudit.CREATE, principalUser));
    }

    @Override
    public void publish(Long checklistId, String code, PrincipalUser principalUser) {
        String details = formatMessage(PUBLISH_CHECKLIST, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), code);
        checklistAuditRepository.save(getChecklistAudit(details, checklistId, Action.ChecklistAudit.PUBLISH, principalUser));
    }

    @Override
    public void archive(Long checklistId, String code, String reason, PrincipalUser principalUser) {
        String details = formatMessage(ARCHIVE_CHECKLIST, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), code, reason);
        checklistAuditRepository.save(getChecklistAudit(details, checklistId, Action.ChecklistAudit.ARCHIVE, principalUser));
    }

    @Override
    public void archiveStage(Long checklistId, Stage stage, PrincipalUser principalUser) {
        String details = formatMessage(ARCHIVE_STAGE, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), stage.getName(), stage.getOrderTree().toString());
        checklistAuditRepository.save(getChecklistAudit(details, checklistId, Action.ChecklistAudit.ARCHIVE, principalUser));
    }

    @Override
    public void archiveTask(Long checklistId, Task task, PrincipalUser principalUser) {
        Stage taskStage = task.getStage();
        Integer stageOrderTree = taskStage.getOrderTree();
        String taskLocation = getTaskLocation(stageOrderTree, task.getOrderTree());
        String details = formatMessage(ARCHIVE_TASK, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), task.getName(), taskLocation, task.getStage().getName());
        checklistAuditRepository.save(getChecklistAudit(details, checklistId, Action.ChecklistAudit.ARCHIVE, principalUser));
    }

    @Override
    public void archiveParameter(Long checklistId, Task task, Parameter parameter, PrincipalUser principalUser) {
      String details = "";
       if(!Utility.isEmpty(task)) {
         Stage taskStage = task.getStage();
         Integer stageOrderTree = taskStage.getOrderTree();
         String taskLocation = getTaskLocation(stageOrderTree, task.getOrderTree());
         details = formatMessage(ARCHIVE_PARAMETER, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), parameter.getLabel(), String.valueOf(parameter.getType()), task.getName(), taskLocation, taskStage.getName());
       }else {
          details = formatMessage(ARCHIVE_PARAMETER_CJF, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), parameter.getLabel(), String.valueOf(parameter.getType()));
       }
       checklistAuditRepository.save(getChecklistAudit(details, checklistId, Action.ChecklistAudit.ARCHIVE, principalUser));
    }

    @Override
    public void unarchive(Long checklistId, String code, String reason, PrincipalUser principalUser) {
        String details = formatMessage(UNARCHIVE_CHECKLIST, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), code, reason);
        checklistAuditRepository.save(getChecklistAudit(details, checklistId, Action.ChecklistAudit.UNARCHIVE, principalUser));
    }

    @Override
    public void revise(Long checklistId, String checklistCode, Long prototypeId, String prototypeCode, PrincipalUser principalUser) {
        String details = formatMessage(REVISE_CHECKLIST_PARENT, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), prototypeCode);
        checklistAuditRepository.save(getChecklistAudit(details, checklistId, Action.ChecklistAudit.REVISE, principalUser));

        details = formatMessage(REVISE_CHECKLIST_PROTOTYPE, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), prototypeCode, checklistCode);
        checklistAuditRepository.save(getChecklistAudit(details, prototypeId, Action.ChecklistAudit.REVISE, principalUser));
    }

    @Override
    public void recall(Long checklistId, String code, String reason, PrincipalUser principalUser) {
        String details = formatMessage(RECALLED_PROTOTYPE, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), code, reason);
        checklistAuditRepository.save(getChecklistAudit(details, checklistId, Action.ChecklistAudit.RECALL, principalUser));

    }

    @Override
    public void deprecate(Long checklistId, String code, String parentChecklistcode, PrincipalUser principalUser) {
        String details = formatMessage(DEPRECATE_CHECKLIST, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), code, parentChecklistcode);
        checklistAuditRepository.save(getChecklistAudit(details, checklistId, Action.ChecklistAudit.DEPRECATE, principalUser));
    }

    @Override
    public void importChecklist(Long checklistId, String code, PrincipalUser principalUser) {
        String details = formatMessage(IMPORT_CHECKLIST, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), code);
        checklistAuditRepository.save(getChecklistAudit(details, checklistId, Action.ChecklistAudit.IMPORT, principalUser));
    }

    @Override
    public void mapTrainedUsers(Long checklistId, PrincipalUser principalUser, List<User> usersList) {
        List<ChecklistAudit> checklistAudits = new ArrayList<>();

        for (User user : usersList) {
            String details = formatMessage(MAP_TRAINED_USER_TO_CHECKLIST, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), user.getFirstName(), user.getLastName(), user.getEmployeeId());
            ChecklistAudit checklistAudit = getChecklistAudit(details, checklistId, Action.ChecklistAudit.TRAINED, principalUser, user.getId());
            checklistAudits.add(checklistAudit);
        }
        if (!checklistAudits.isEmpty()) {
            checklistAuditRepository.saveAll(checklistAudits);
        }
    }

    @Override
    public void unmapTrainedUsers(Long checklistId, PrincipalUser principalUser, List<User> usersList, String reason) {
        List<ChecklistAudit> checklistAudits = new ArrayList<>();

        for (User user : usersList) {
            String details = formatMessage(UNMAP_TRAINED_USER_FROM_CHECKLIST, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), reason, user.getFirstName(), user.getLastName(), user.getEmployeeId());
            ChecklistAudit checklistAudit = getChecklistAudit(details, checklistId, Action.ChecklistAudit.TRAINED, principalUser, user.getId());
            checklistAudits.add(checklistAudit);
        }
        if (!checklistAudits.isEmpty()) {
            checklistAuditRepository.saveAll(checklistAudits);
        }
    }

    @Override
    public void assignTrainedUsersToTask(Long checklistId, PrincipalUser principalUser, Map<User, List<Task>> userTaskAssignments) {
        List<ChecklistAudit> checklistAudits = new ArrayList<>();

        for (Map.Entry<User, List<Task>> entry : userTaskAssignments.entrySet()) {
            User assignedUser = entry.getKey();
            List<Task> tasksAssigned = entry.getValue();

            for (Task task : tasksAssigned) {
                Stage taskStage = task.getStage();
                Integer stageOrderTree = taskStage.getOrderTree();
                String taskLocation = getTaskLocation(stageOrderTree, task.getOrderTree());
                String details = formatMessage(ASSIGN_TRAINED_USER_TO_TASK, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), assignedUser.getFirstName(), assignedUser.getLastName(), assignedUser.getEmployeeId(), taskLocation, String.valueOf(stageOrderTree));
                ChecklistAudit checklistAudit = getChecklistAudit(details, checklistId, Action.ChecklistAudit.TRAINED, principalUser, assignedUser.getId(), task.getId(), task.getStageId());
                checklistAudits.add(checklistAudit);
            }
        }
        if (!checklistAudits.isEmpty()) {
            checklistAuditRepository.saveAll(checklistAudits);
        }
    }

    @Override
    public void unAssignTrainedUsersToTask(Long checklistId, PrincipalUser principalUser, Map<User, List<Task>> userTaskAssignments) {
        List<ChecklistAudit> checklistAudits = new ArrayList<>();
        for (Map.Entry<User, List<Task>> entry : userTaskAssignments.entrySet()) {
            User assignedUser = entry.getKey();
            List<Task> tasksAssigned = entry.getValue();
            Map<Long, Stage> stageMap = getStageMapFromTasks(tasksAssigned);
            for (Task task : tasksAssigned) {

                Stage taskStage = stageMap.get(task.getStageId());
                ;
                Integer stageOrderTree = taskStage.getOrderTree();
                String taskLocation = getTaskLocation(stageOrderTree, task.getOrderTree());
                String details = formatMessage(UNASSIGN_TRAINED_USER_TO_TASK, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), assignedUser.getFirstName(), assignedUser.getLastName(), assignedUser.getEmployeeId(), taskLocation, String.valueOf(stageOrderTree));
                ChecklistAudit checklistAudit = getChecklistAudit(details, checklistId, Action.ChecklistAudit.TRAINED, principalUser, assignedUser.getId(), task.getId(), task.getStageId());
                checklistAudits.add(checklistAudit);
            }
        }
        if (!checklistAudits.isEmpty()) {
            checklistAuditRepository.saveAll(checklistAudits);
        }
    }

    @Override
    public void mapTrainedUserGroups(Long checklistId, PrincipalUser principalUser, List<UserGroup> userGroupList) {
        List<ChecklistAudit> checklistAudits = new ArrayList<>();

        for (UserGroup userGroup : userGroupList) {
            String details = formatMessage(MAP_TRAINED_USER_GROUPS_TO_CHECKLIST, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), userGroup.getName());
            ChecklistAudit checklistAudit = getChecklistAudit(details, checklistId, Action.ChecklistAudit.TRAINED, principalUser, userGroup.getId());
            checklistAudits.add(checklistAudit);
        }
        if (!checklistAudits.isEmpty()) {
            checklistAuditRepository.saveAll(checklistAudits);
        }
    }

    @Override
    public void unmapTrainedUserGroups(Long checklistId, PrincipalUser principalUser, List<UserGroup> userGroupList, String reason) {
        List<ChecklistAudit> checklistAudits = new ArrayList<>();

        for (UserGroup userGroup : userGroupList) {
            String details = formatMessage(UNMAP_TRAINED_USER_GROUPS_FROM_CHECKLIST, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), reason, userGroup.getName());
            ChecklistAudit checklistAudit = getChecklistAudit(details, checklistId, Action.ChecklistAudit.TRAINED, principalUser, userGroup.getId());
            checklistAudits.add(checklistAudit);
        }
        if (!checklistAudits.isEmpty()) {
            checklistAuditRepository.saveAll(checklistAudits);
        }
    }

    @Override
    public void assignTrainedUserGroupsToTask(Long checklistId, PrincipalUser principalUser, Map<UserGroup, List<Task>> userGroupTaskAssignments) {
        List<ChecklistAudit> checklistAudits = new ArrayList<>();

        for (Map.Entry<UserGroup, List<Task>> entry : userGroupTaskAssignments.entrySet()) {
            UserGroup assignedUserGroup = entry.getKey();
            List<Task> tasksAssigned = entry.getValue();
            for (Task task : tasksAssigned) {
                Stage taskStage = task.getStage();
                Integer stageOrderTree = taskStage.getOrderTree();
                String taskLocation = getTaskLocation(stageOrderTree, task.getOrderTree());
                String details = formatMessage(ASSIGN_TRAINED_USER_GROUP_TO_TASK, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), assignedUserGroup.getName(), taskLocation, String.valueOf(stageOrderTree));
                ChecklistAudit checklistAudit = getChecklistAudit(details, checklistId, Action.ChecklistAudit.TRAINED, principalUser, assignedUserGroup.getId(), task.getId(), task.getStageId());

                checklistAudits.add(checklistAudit);
            }
        }

        if (!checklistAudits.isEmpty()) {
            checklistAuditRepository.saveAll(checklistAudits);
        }
    }

    @Override
    public void unAssignTrainedUserGroupsToTask(Long checklistId, PrincipalUser principalUser, Map<UserGroup, List<Task>> userGroupTaskAssignments) {
        List<ChecklistAudit> checklistAudits = new ArrayList<>();
        for (Map.Entry<UserGroup, List<Task>> entry : userGroupTaskAssignments.entrySet()) {
            UserGroup assignedUserGroup = entry.getKey();
            List<Task> tasksAssigned = entry.getValue();
            Map<Long, Stage> stageMap = getStageMapFromTasks(tasksAssigned);
            for (Task task : tasksAssigned) {
                Stage taskStage = stageMap.get(task.getStageId());
                Integer stageOrderTree = taskStage.getOrderTree();
                String taskLocation = getTaskLocation(stageOrderTree, task.getOrderTree());
                String details = formatMessage(UNASSIGN_TRAINED_USER_GROUP_TO_TASK, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), assignedUserGroup.getName(), taskLocation, String.valueOf(stageOrderTree));
                ChecklistAudit checklistAudit = getChecklistAudit(details, checklistId, Action.ChecklistAudit.TRAINED, principalUser, assignedUserGroup.getId(), task.getId(), task.getStageId());

                checklistAudits.add(checklistAudit);
            }
        }
        if (!checklistAudits.isEmpty()) {
            checklistAuditRepository.saveAll(checklistAudits);
        }
    }

    @Override
    public void unmapTrainedUsersDueToArchival(List<TrainedUser> trainedUsers, String reason, PrincipalUser archivedByUser, PrincipalUser systemUser) {
        // Group by checklist for efficient processing
        Map<Checklist, List<TrainedUser>> trainedUsersByChecklist = trainedUsers.stream()
            .collect(Collectors.groupingBy(TrainedUser::getChecklist));
        
        List<ChecklistAudit> checklistAudits = new ArrayList<>();
        
        for (Map.Entry<Checklist, List<TrainedUser>> entry : trainedUsersByChecklist.entrySet()) {
            Checklist checklist = entry.getKey();
            List<TrainedUser> trainedUsersForChecklist = entry.getValue();

          for (TrainedUser trainedUser : trainedUsersForChecklist) {
            User user = trainedUser.getUser();

            String details = formatMessage(USER_ARCHIVAL_TRAINED_USER_REMOVAL,
              systemUser.getFirstName(), systemUser.getEmployeeId(),
              user.getFirstName(), user.getLastName(), user.getEmployeeId(),
              checklist.getName(),
              archivedByUser.getFirstName(), archivedByUser.getLastName(), archivedByUser.getEmployeeId(),
              reason
            );

            ChecklistAudit checklistAudit = getChecklistAudit(
              details, checklist.getId(), Action.ChecklistAudit.TRAINED,
              archivedByUser, user.getId()
            );
            checklistAudits.add(checklistAudit);
          }
        }
        
        if (!checklistAudits.isEmpty()) {
            checklistAuditRepository.saveAll(checklistAudits);
        }
    }

    @Override
    public void downloadProcessTemplatePdf(Long checklistId, String name, String code, PrincipalUser principalUser) {
        String details = formatMessage(DOWNLOAD_PROCESS_TEMPLATE_PDF, principalUser.getFirstName(), principalUser.getLastName(), principalUser.getEmployeeId(), name, code);
        checklistAuditRepository.save(getChecklistAudit(details, checklistId, Action.ChecklistAudit.CREATE, principalUser));
    }

    //TODO facility id probably needs to be the selected facility or
    // do we need facility id to be saved ?
    private ChecklistAudit getChecklistAudit(String details, Long checklistId, Action.ChecklistAudit action, PrincipalUser principalUser) {
        return new ChecklistAudit()
                .setChecklistId(checklistId)
                .setDetails(details)
                .setTriggeredAt(DateTimeUtils.now())
                .setTriggeredAt(DateTimeUtils.now())
                .setTriggeredBy(principalUser.getId())
                .setAction(action)
                .setOrganisationsId(principalUser.getOrganisationId());
    }

    //For Task Assignments
    private ChecklistAudit getChecklistAudit(String details, Long checklistId, Action.ChecklistAudit action, PrincipalUser principalUser, Long triggeredForId, Long taskId, Long stageId) {
        return new ChecklistAudit()
                .setChecklistId(checklistId)
                .setDetails(details)
                .setTriggeredAt(DateTimeUtils.now())
                .setTriggeredAt(DateTimeUtils.now())
                .setTriggeredBy(principalUser.getId())
                .setAction(action)
                .setOrganisationsId(principalUser.getOrganisationId())
                .setTriggeredFor(triggeredForId)
                .setTaskId(taskId)
                .setStageId(stageId);
    }

    //For Mapping Of User
    private ChecklistAudit getChecklistAudit(String details, Long checklistId, Action.ChecklistAudit action, PrincipalUser principalUser, Long triggeredForId) {
        return new ChecklistAudit()
                .setChecklistId(checklistId)
                .setDetails(details)
                .setTriggeredAt(DateTimeUtils.now())
                .setTriggeredAt(DateTimeUtils.now())
                .setTriggeredBy(principalUser.getId())
                .setAction(action)
                .setOrganisationsId(principalUser.getOrganisationId())
                .setTriggeredFor(triggeredForId);
    }


    private String formatMessage(String pattern, String... replacements) {
        for (int i = 0; i < replacements.length; i++) {
            pattern = pattern.replace("{" + i + "}", replacements[i]);
        }
        return pattern;
    }

    private String getTaskLocation(Integer stageOrderTree, Integer taskOrderTree) {
        return stageOrderTree + "." + taskOrderTree;
    }

    private Map<Long, Stage> getStageMapFromTasks(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> stageIds = tasks.stream()
                .map(Task::getStageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Stage> stages = stageRepository.findAllById(stageIds);
        return stages.stream()
                .collect(Collectors.toMap(Stage::getId, stage -> stage));
    }

}
