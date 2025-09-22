package com.leucine.streem.service.impl;

import com.leucine.streem.constant.ProcessPermissionType;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.mapper.ITrainedUserMapper;
import com.leucine.streem.dto.projection.TrainedUsersView;
import com.leucine.streem.dto.request.TrainedUserMappingRequest;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.handler.TrainedUserMaterializedViewHandler;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.repository.*;
import com.leucine.streem.service.IChecklistAuditService;
import com.leucine.streem.service.IChecklistTrainedUserService;
import com.leucine.streem.util.DateTimeUtils;
import com.leucine.streem.util.Utility;
import com.leucine.streem.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChecklistTrainedUserService implements IChecklistTrainedUserService {
  private final ITrainedUserTaskMappingRepository trainedUsersTaskMappingRepository;
  private final ITrainedUserRepository trainedUsersRepository;
  private final IUserGroupRepository userGroupRepository;
  private final IUserRepository userRepository;
  private final IFacilityRepository facilityRepository;
  private final IChecklistRepository checklistRepository;
  private final ITrainedUserMapper trainedUserMapper;
  private final ITrainedUsersProcessPermissionMappingRepository trainedUserProcessPermissionMappingRepository;
  private final IProcessPermissionRepository processPermissionRepository;
  private final IChecklistAuditService checklistAuditService;
  private final TrainedUserMaterializedViewHandler trainedUserMaterializedViewHandler;

  @Override
  //TODO: Rename entity from TrainedUserTaskMapping to TrainedUser or something similar
  public Page<TrainedUsersDto> getTrainedUsers(Long checklistId, Boolean isUser, Boolean isUserGroup, String query, Pageable pageable) throws StreemException {
    log.info("[getDefaultUsers] Request to get default user for checklist, checklistId: {}", checklistId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Long currentFacilityId = principalUser.getCurrentFacilityId();
    if (Objects.equals(currentFacilityId, null)) {
      ValidationUtils.invalidate(principalUser.getId(), ErrorCode.CANNOT_ASSIGN_TRAINING_USER_IN_ALL_FACILITY);
    }

    if (isUserGroup == null && isUser == null) {
      isUser = true;
      isUserGroup = true;
    }
    int limit = pageable.getPageSize();
    int offset = (int) pageable.getOffset();


    List<TrainedUsersView> trainedUsersViews = getTrainedUsersOfFacility(checklistId, currentFacilityId, isUser, isUserGroup, query, limit, offset);
    long count = trainedUsersTaskMappingRepository.countAllTrainedUsersWithAssignedTasksByChecklistIdAndFacilityId(checklistId, currentFacilityId, isUser, isUserGroup, query);

    Set<String> trainedUserIds = trainedUsersViews.stream().map(TrainedUsersView::getId).collect(Collectors.toSet());

    List<TrainedUsersView> trainedUsersTaskMapping = trainedUsersTaskMappingRepository.findAllTrainedUsersWithAssignedTasksByChecklistIdAndFacilityId(checklistId, currentFacilityId, isUser, isUserGroup, trainedUserIds);
    Map<String, Set<String>> userIdTaskIdMapping = trainedUsersTaskMapping.stream().filter(trainedUsersView -> !Utility.isEmpty(trainedUsersView.getUserId()))
      .collect(Collectors.groupingBy(TrainedUsersView::getUserId, Collectors.mapping(TrainedUsersView::getTaskId, Collectors.toSet())));

    Map<String, Set<String>> userGroupIdTaskIdMapping = trainedUsersTaskMapping.stream().filter(trainedUsersView -> !Utility.isEmpty(trainedUsersView.getUserGroupId()))
      .collect(Collectors.groupingBy(TrainedUsersView::getUserGroupId, Collectors.mapping(TrainedUsersView::getTaskId, Collectors.toSet())));


    return new PageImpl<>(trainedUserMapper.toDtoList(trainedUsersViews, userIdTaskIdMapping, userGroupIdTaskIdMapping), pageable, count);
  }

  @Override
  @Transactional
  public List<TrainedUserTaskMappingDto> getAllTrainedUserTaskMapping(Long checklistId, String query) {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    List<TrainedUsersView> trainedUserViews = trainedUserMaterializedViewHandler.getAllTrainedUserViews(checklistId, principalUser.getCurrentFacilityId(), query);

    Set<String> taskIds = trainedUserViews.stream().map(TrainedUsersView::getTaskId).collect(Collectors.toSet());

    Map<String, Set<PartialUserDto>> taskIdUserDtoMap = trainedUserViews.stream()
      .filter(trainedUsersView -> !Utility.isEmpty(trainedUsersView.getUserId()))
      .collect(Collectors.groupingBy(TrainedUsersView::getTaskId, Collectors.mapping(trainedUsersView -> new PartialUserDto(trainedUsersView.getUserId(), trainedUsersView.getEmployeeId(), trainedUsersView.getEmailId(), trainedUsersView.getFirstName(), trainedUsersView.getLastName(), null), Collectors.toSet())));

    Map<String, Set<UserGroupDto>> taskIdUserGroupDtoMap = trainedUserViews.stream()
      .filter(trainedUsersView -> !Utility.isEmpty(trainedUsersView.getUserGroupId()))
      .collect(Collectors.groupingBy(TrainedUsersView::getTaskId, Collectors.mapping(trainedUsersView -> new UserGroupDto(trainedUsersView.getUserGroupId(), trainedUsersView.getUserGroupName(), trainedUsersView.getUserGroupDescription(), true), Collectors.toSet())));

    List<TrainedUserTaskMappingDto> trainedUserTaskMappingDtos = new ArrayList<>();
    for (String taskId : taskIds) {
      TrainedUserTaskMappingDto trainedUserTaskMappingDto = new TrainedUserTaskMappingDto();

      if (taskIdUserDtoMap.containsKey(taskId)) {
        trainedUserTaskMappingDto.setUsers((taskIdUserDtoMap.get(taskId)));
      }
      if (taskIdUserGroupDtoMap.containsKey(taskId)) {
        trainedUserTaskMappingDto.setUserGroups(taskIdUserGroupDtoMap.get(taskId));
      }
      trainedUserTaskMappingDto.setTaskId(taskId);
      trainedUserTaskMappingDtos.add(trainedUserTaskMappingDto);
    }
    return trainedUserTaskMappingDtos;
  }

  @Override
  public Page<TrainedUsersDto> getUnTrainedUsers(Long checklistId, Boolean isUser, Boolean isUserGroup, String query, Pageable pageable) {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Long currentFacilityId = principalUser.getCurrentFacilityId();
    return getNonTrainedUsersOfFacility(checklistId, currentFacilityId, isUser, isUserGroup, query, pageable);
  }

  @Override
  public BasicDto mapTrainedUsers(Long checklistId, TrainedUserMappingRequest trainedUserMappingRequest) throws StreemException {
    log.info("[mapDefaultUsers] Request to map default user for checklist, checklistId: {}", checklistId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());

    Checklist checklist = checklistRepository.getReferenceById(checklistId);
    Facility facility = facilityRepository.getReferenceById(principalUser.getCurrentFacilityId());
    List<TrainedUser> trainedUsers = new ArrayList<>();

    List<User> mappedUsers = new ArrayList<>();
    List<UserGroup> mappedUserGroups = new ArrayList<>();
    List<User> unmappedUsers = new ArrayList<>();
    List<UserGroup> unmappedUserGroups = new ArrayList<>();

    if (!Utility.isEmpty(trainedUserMappingRequest.getAssignedUserIds())) {
      Set<Long> alreadyMappedUserIds = trainedUsersRepository.validateIfUsersAreTrainedUsersForChecklist(checklistId, trainedUserMappingRequest.getAssignedUserIds(), principalUser.getCurrentFacilityId());

      if (!Utility.isEmpty(alreadyMappedUserIds)) {
        ValidationUtils.invalidate((Long) null, ErrorCode.SOME_USERS_ARE_ALREADY_MAPPED_TO_CHECKLIST);
      }
      List<User> userList = userRepository.findAllById(trainedUserMappingRequest.getAssignedUserIds());


      for (User user : userList) {
        //TODO: refactor this
        TrainedUser trainedUser = new TrainedUser();
        trainedUser.setChecklist(checklist);
        trainedUser.setFacility(facility);
        trainedUser.setUser(user);
        trainedUser.setCreatedAt(DateTimeUtils.now());
        trainedUser.setModifiedAt(DateTimeUtils.now());
        trainedUser.setCreatedBy(principalUserEntity);
        trainedUser.setModifiedBy(principalUserEntity);
        trainedUsers.add(trainedUser);
      }
      mappedUsers.addAll(userList);
    }

    if (!Utility.isEmpty(trainedUserMappingRequest.getAssignedUserGroupIds())) {
      Set<Long> alreadyMappedUserGroupIds = trainedUsersRepository.validateIfUserGroupsAreTrainedUserGroupsForChecklist(checklistId, trainedUserMappingRequest.getAssignedUserGroupIds(), principalUser.getCurrentFacilityId());
      if (!Utility.isEmpty(alreadyMappedUserGroupIds)) {
        ValidationUtils.invalidate((Long) null, ErrorCode.SOME_USER_GROUPS_ARE_ALREADY_MAPPED_TO_CHECKLIST);
      }
      List<UserGroup> userGroupList = userGroupRepository.findAllById(trainedUserMappingRequest.getAssignedUserGroupIds());
      for (UserGroup userGroup : userGroupList) {
        //TODO: refactor this
        TrainedUser trainedUser = new TrainedUser();
        trainedUser.setChecklist(checklist);
        trainedUser.setFacility(facility);
        trainedUser.setUserGroup(userGroup);
        trainedUser.setCreatedAt(DateTimeUtils.now());
        trainedUser.setModifiedAt(DateTimeUtils.now());
        trainedUser.setCreatedBy(principalUserEntity);
        trainedUser.setModifiedBy(principalUserEntity);
        trainedUsers.add(trainedUser);
      }
      mappedUserGroups.addAll(userGroupList);
    }

    List<TrainedUser> savedTrainedUsers = trainedUsersRepository.saveAll(trainedUsers);

    if (!Utility.isEmpty(trainedUserMappingRequest.getProcessPermissionTypes())) {
      Map<ProcessPermissionType, ProcessPermission> processPermissionTypeProcessPermissionMap = processPermissionRepository.findByTypeIn(trainedUserMappingRequest.getProcessPermissionTypes()).stream().collect(Collectors.toMap(ProcessPermission::getType, processPermission -> processPermission));
      List<TrainedUserProcessPermissionMapping> trainedUserProcessPermissionMappings = new ArrayList<>();
      for (TrainedUser trainedUser : savedTrainedUsers) {
        for (ProcessPermissionType processPermissionType : trainedUserMappingRequest.getProcessPermissionTypes()) {
          TrainedUserProcessPermissionMapping trainedUserProcessPermissionMapping = new TrainedUserProcessPermissionMapping();
          trainedUserProcessPermissionMapping.setTrainedUser(trainedUser);
          trainedUserProcessPermissionMapping.setProcessPermission(processPermissionTypeProcessPermissionMap.get(processPermissionType));
          trainedUserProcessPermissionMapping.setCreatedAt(DateTimeUtils.now());
          trainedUserProcessPermissionMapping.setModifiedAt(DateTimeUtils.now());
          trainedUserProcessPermissionMapping.setCreatedBy(principalUserEntity);
          trainedUserProcessPermissionMapping.setModifiedBy(principalUserEntity);
          trainedUserProcessPermissionMappings.add(trainedUserProcessPermissionMapping);
        }
      }
      trainedUserProcessPermissionMappingRepository.saveAll(trainedUserProcessPermissionMappings);
    }

    if (!Utility.isEmpty(trainedUserMappingRequest.getUnassignedUserIds())) {
      List<User> userList = userRepository.findAllById(trainedUserMappingRequest.getUnassignedUserIds());
      trainedUsersRepository.deleteByChecklistIdAndFacilityIdAndUserIdIn(checklistId, principalUser.getCurrentFacilityId(), trainedUserMappingRequest.getUnassignedUserIds());
      unmappedUsers.addAll(userList);
    }

    if (!Utility.isEmpty(trainedUserMappingRequest.getUnassignedUserGroupIds())) {
      List<UserGroup> userGroupList = userGroupRepository.findAllById(trainedUserMappingRequest.getUnassignedUserGroupIds());
      trainedUsersRepository.deleteByChecklistIdAndFacilityIdAndUserGroupIdIn(checklistId, principalUser.getCurrentFacilityId(), trainedUserMappingRequest.getUnassignedUserGroupIds());
      unmappedUserGroups.addAll(userGroupList);
    }

    if (!mappedUsers.isEmpty()) {
      checklistAuditService.mapTrainedUsers(checklist.getId(), principalUser, mappedUsers);
    }
    if (!unmappedUsers.isEmpty()) {
      checklistAuditService.unmapTrainedUsers(checklist.getId(), principalUser, unmappedUsers, trainedUserMappingRequest.getReason());
    }
    if (!mappedUserGroups.isEmpty()) {
      checklistAuditService.mapTrainedUserGroups(checklist.getId(), principalUser, mappedUserGroups);
    }
    if (!unmappedUserGroups.isEmpty()) {
      checklistAuditService.unmapTrainedUserGroups(checklist.getId(), principalUser, unmappedUserGroups, trainedUserMappingRequest.getReason());
    }

    return new BasicDto(null, "message", null);
  }

  @Override
  public Page<TrainedUsersDto> getNonTrainedUsersOfFacility(Long checklistId, Long facilityId, Boolean isUser, Boolean isUserGroup, String query, Pageable pageable) {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (isUser == null) {
      isUser = false;
    }
    if (isUserGroup == null) {
      isUserGroup = false;
    }
    int limit = pageable.getPageSize();
    int offset = (int) pageable.getOffset();
    if (isUser) {
      List<TrainedUsersView> trainedUsersViews = trainedUsersTaskMappingRepository.findAllNonTrainedUsersByChecklistIdAndFacilityId(checklistId, facilityId, query, limit, offset);
      long resultCount = trainedUsersTaskMappingRepository.countAllNonTrainedUsersByChecklistIdAndFacilityId(checklistId, principalUser.getCurrentFacilityId(), query);
      return new PageImpl<>(trainedUserMapper.toDtoList(trainedUsersViews), pageable, resultCount);
    } else if (isUserGroup) {
      List<TrainedUsersView> trainedUsersViews = trainedUsersTaskMappingRepository.findAllNonTrainedUserGroupsByChecklistIdAndFacilityId(checklistId, facilityId, query, limit, offset);
      long resultCount = trainedUsersTaskMappingRepository.countAllNonTrainedUserGroupsByChecklistIdAndFacilityId(checklistId, principalUser.getCurrentFacilityId(), query);
      return new PageImpl<>(trainedUserMapper.toDtoList(trainedUsersViews), pageable, resultCount);
    }
    return new PageImpl<>(Collections.emptyList(), pageable, 0);
  }

  @Override
  public List<TrainedUsersView> getTrainedUsersOfFacility(Long checklistId, Long facilityId, Boolean isUser, Boolean isUserGroup, String query, int limit, int offset) {
    if (isUser == null) {
      isUser = true;
    }
    if (isUserGroup == null) {
      isUserGroup = true;
    }
    return trainedUsersTaskMappingRepository.findAllByChecklistIdAndFacilityId(checklistId, facilityId, isUser, isUserGroup, query, limit, offset);
  }
}
