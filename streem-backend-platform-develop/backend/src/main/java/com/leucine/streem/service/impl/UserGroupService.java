package com.leucine.streem.service.impl;

import com.leucine.streem.constant.CollectionKey;
import com.leucine.streem.constant.Misc;
import com.leucine.streem.constant.Operator;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.RoleDto;
import com.leucine.streem.dto.UserDto;
import com.leucine.streem.dto.UserGroupDto;
import com.leucine.streem.dto.mapper.IUserGroupMapper;
import com.leucine.streem.dto.mapper.IUserMapper;
import com.leucine.streem.dto.projection.RoleBasicView;
import com.leucine.streem.dto.request.RemoveUserRequest;
import com.leucine.streem.dto.request.UserGroupCreateRequest;
import com.leucine.streem.dto.request.UserGroupUpdateRequest;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.ExceptionType;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.Facility;
import com.leucine.streem.model.User;
import com.leucine.streem.model.UserGroup;
import com.leucine.streem.model.UserGroupMember;
import com.leucine.streem.model.compositekey.UserGroupMemberCompositeKey;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.model.helper.SpecificationBuilder;
import com.leucine.streem.model.helper.search.SearchCriteria;
import com.leucine.streem.repository.*;
import com.leucine.streem.service.INotificationService;
import com.leucine.streem.service.IUserGroupAuditService;
import com.leucine.streem.service.IUserGroupService;
import com.leucine.streem.util.DateTimeUtils;
import com.leucine.streem.util.Utility;
import com.leucine.streem.util.ValidationUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class UserGroupService implements IUserGroupService {
  private final IUserGroupRepository userGroupRepository;
  private final IUserGroupMemberRepository userGroupMemberRepository;
  private final IUserGroupMapper userGroupMapper;
  private final IUserMapper userMapper;
  private final IUserRepository userRepository;
  private final IFacilityRepository facilityRepository;
  private final IUserGroupAuditService userGroupAuditService;
  private final ITaskExecutionAssigneeRepository taskExecutionAssigneeRepository;
  private final ITrainedUserRepository trainedUsersRepository;
  private final IJobRepository jobRepository;
  private final INotificationService notificationService;

  @Override
  public UserGroupDto getById(Long id) throws ResourceNotFoundException {
    log.info("[getById] Request to get userGroup by id, id: {}", id);
    UserGroup userGroup = userGroupRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException(id.toString(), ErrorCode.USER_GROUP_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    UserGroupDto userGroupDto = userGroupMapper.toDto(userGroup);
    List<Long> allUserIds = userGroupMemberRepository.getAllUserIdsOfUserGroup(id);
    userGroupDto.setAllUserIds(allUserIds.stream().map(String::valueOf).toList());
    return userGroupDto;
  }

  @Override
  public BasicDto unArchive(Long id, String reason) throws ResourceNotFoundException {
    log.info("[unArchive] Request to unArchive userGroup , id: {}, reason: {}", id, reason);

    UserGroup userGroup = userGroupRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException(id.toString(), ErrorCode.USER_GROUP_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    userGroup.setActive(true);
    userGroupRepository.save(userGroup);

    userGroupAuditService.unarchive(userGroup, reason);
    return new BasicDto(null, "success", null);
  }

  @Override
  @Transactional
  public BasicDto archive(Long id, String reason) throws ResourceNotFoundException, StreemException {
    log.info("[archive] Request to archive userGroup , id: {}, reason: {}", id, reason);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    UserGroup userGroup = userGroupRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException(id.toString(), ErrorCode.USER_GROUP_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    boolean isUserGroupAssignedToAnyInProgressTask = taskExecutionAssigneeRepository.isUserGroupAssignedToInProgressTasks(id);
    if (isUserGroupAssignedToAnyInProgressTask) {
      ValidationUtils.invalidate(id, ErrorCode.USER_GROUP_ASSIGNED_TO_IN_PROGRESS_TASKS);
    }

    notificationService.archiveGroup(userGroup, reason, principalUser);
    userGroup.setActive(false);
    userGroupRepository.save(userGroup);
    userGroupAuditService.archive(userGroup, reason);
    taskExecutionAssigneeRepository.removeUserGroupAssignees(id);
    trainedUsersRepository.deleteByUserGroupId(id);
    jobRepository.updateJobToUnassignedIfNoUserAssigned();
    return new BasicDto(null, "success", null);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public UserGroupDto edit(String userGroupId, UserGroupUpdateRequest userGroupUpdateRequest) throws StreemException, ResourceNotFoundException {
    log.info("[edit] Request to edit userGroup , userGroupId: {}, userGroupUpdateRequest: {}", userGroupId, userGroupUpdateRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    UserGroup userGroup = userGroupRepository.findById(Long.valueOf(userGroupId))
      .orElseThrow(() -> new ResourceNotFoundException(userGroupId, ErrorCode.USER_GROUP_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));


    UserGroupDto oldUserGroupDto = userGroupMapper.toDto(userGroup);
    List<UserDto> oldUserDtos = userGroupMemberRepository.findByUserGroupId(userGroup.getId()).stream().map(userGroupMember -> userMapper.toDto(userGroupMember.getUser())).toList();
    oldUserGroupDto.setUsers(oldUserDtos);

    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());

    validateUserGroupUpdateRequest(userGroupUpdateRequest, principalUser.getCurrentFacilityId(), userGroup.getId());

    userGroup.setName(userGroupUpdateRequest.getName());
    userGroup.setDescription(userGroupUpdateRequest.getDescription());
    userGroup.setModifiedAt(DateTimeUtils.now());
    userGroup.setModifiedBy(principalUserEntity);

    UserGroup savedUserGroup = userGroupRepository.save(userGroup);

    if (!Utility.isEmpty(userGroupUpdateRequest.getAssignedUserIds())) {
      validateUserRoles(userGroupUpdateRequest.getAssignedUserIds());
      List<User> userList = userRepository.findAllById(userGroupUpdateRequest.getAssignedUserIds());
      List<UserGroupMember> userGroupMembers = new ArrayList<>();
      for (User user : userList) {
        UserGroupMember userGroupMember = new UserGroupMember();
        userGroupMember.setUserGroupMemberCompositeKey(new UserGroupMemberCompositeKey(userGroup.getId(), user.getId()));
        userGroupMember.setUser(user);
        userGroupMember.setUserGroup(savedUserGroup);
        userGroupMember.setCreatedAt(DateTimeUtils.now());
        userGroupMember.setCreatedBy(principalUserEntity);
        userGroupMember.setModifiedAt(DateTimeUtils.now());
        userGroupMember.setModifiedBy(principalUserEntity);
        userGroupMembers.add(userGroupMember);
      }
      userGroupMemberRepository.saveAll(userGroupMembers);
      notificationService.addedToUserGroup(userGroupMembers, principalUser.getOrganisationId());
    }

    boolean removeAllUsers = userGroupUpdateRequest.isUnAssignAllUsers();
    Set<Long> removedUserIds = userGroupUpdateRequest.getRemovedUser().stream().map(RemoveUserRequest::getUserId).collect(Collectors.toSet());
    if (removeAllUsers) {
      removedUserIds.addAll(userGroupMemberRepository.getAllUserIdsOfUserGroup(userGroup.getId()));
    }

    if (!Utility.isEmpty(removedUserIds)) {
      List<User> users = userRepository.findAllById(removedUserIds);
      notificationService.groupMembershipUpdate(userGroup, userGroupUpdateRequest, users, principalUser);
    }
    if (!Utility.isEmpty(userGroupUpdateRequest.getRemovedUser())) {
      userGroupMemberRepository.deleteByUserGroupIdAndUserIdIn(Long.valueOf(userGroupId), removedUserIds);
    }
    UserGroupDto userGroupDto = userGroupMapper.toDto(savedUserGroup);
    userGroupDto.setUserCount(userGroupMemberRepository.countByUserGroupId(savedUserGroup.getId()).intValue());

    userGroupAuditService.update(principalUser.getOrganisationId(), principalUser.getCurrentFacilityId(), principalUser.getId(), oldUserGroupDto, savedUserGroup, userGroupUpdateRequest);
    return userGroupDto;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public BasicDto create(UserGroupCreateRequest userGroupCreateRequest) throws StreemException {
    log.info("[create] Request to create userGroup , userGroupCreateRequest: {}", userGroupCreateRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());

    validateUserGroupCreateRequest(userGroupCreateRequest, principalUser.getCurrentFacilityId());
    Facility facility = facilityRepository.getReferenceById(principalUser.getCurrentFacilityId());
    UserGroup userGroup = new UserGroup();
    userGroup.setName(userGroupCreateRequest.getName());
    userGroup.setFacility(facility);
    userGroup.setDescription(userGroupCreateRequest.getDescription());
    userGroup.setActive(true);
    userGroup.setCreatedAt(DateTimeUtils.now());
    userGroup.setCreatedBy(principalUserEntity);
    userGroup.setModifiedAt(DateTimeUtils.now());
    userGroup.setModifiedBy(principalUserEntity);

    UserGroup savedUserGroup = userGroupRepository.save(userGroup);
    validateUserRoles(userGroupCreateRequest.getUserIds());
    if (!Utility.isEmpty(userGroupCreateRequest.getUserIds())) {
      List<User> userList = userRepository.findAllById(userGroupCreateRequest.getUserIds());
      List<UserGroupMember> userGroupMembers = new ArrayList<>();
      for (User user : userList) {
        UserGroupMember userGroupMember = new UserGroupMember();
        userGroupMember.setUserGroupMemberCompositeKey(new UserGroupMemberCompositeKey(savedUserGroup.getId(), user.getId()));
        userGroupMember.setUser(user);
        userGroupMember.setUserGroup(savedUserGroup);
        userGroupMember.setCreatedAt(DateTimeUtils.now());
        userGroupMember.setCreatedBy(principalUserEntity);
        userGroupMember.setModifiedAt(DateTimeUtils.now());
        userGroupMember.setModifiedBy(principalUserEntity);
        userGroupMembers.add(userGroupMember);
      }
      userGroupMemberRepository.saveAll(userGroupMembers);
      notificationService.addedToUserGroup(userGroupMembers, principalUser.getOrganisationId());

    }
    userGroupAuditService.create(principalUser.getOrganisationId(), principalUser.getCurrentFacilityId(), savedUserGroup, userGroupCreateRequest.getReason());
    return new BasicDto(savedUserGroup.getId().toString(), "success", null);
  }

  @Override
  public Page<UserDto> getAllMembers(String id, String filters, Pageable pageable) {
    log.info("[getAllMembers] Request to get all members of userGroup , id: {}, filters: {}, pageable: {}", id, filters, pageable);
    SearchCriteria userGroupSearchCriteria = (new SearchCriteria()).setField(CollectionKey.USER_GROUP_ID).setOp(Operator.Search.EQ.toString()).setValues(Collections.singletonList(Long.valueOf(id)));
    Specification<UserGroupMember> userGroupMemberSpecification = SpecificationBuilder.createSpecification(filters, Collections.singletonList(userGroupSearchCriteria));
    Page<UserGroupMember> userGroupMemberPage = userGroupMemberRepository.findAll(userGroupMemberSpecification, pageable);
    List<UserDto> userDtos = userMapper.toUserDto(userGroupMemberPage.getContent());
    for (UserDto userDto : userDtos) {
      List<RoleBasicView> roles = userRepository.getUserRoles(userDto.getId());
      userDto.setRoles(roles.stream().map(role -> new RoleDto(role.getId(), role.getName())).toList());
    }
    return new PageImpl<>(userDtos, pageable, userGroupMemberPage.getTotalElements());
  }

  @Override
  public Page<UserGroupDto> getAll(String filters, Pageable pageable) {
    log.info("[getAll] Request to get all userGroups , filters: {}, pageable: {}", filters, pageable);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    SearchCriteria facilitySearchCriteria = null;
    Long currentFacilityId = principalUser.getCurrentFacilityId();

    if (currentFacilityId != null && !currentFacilityId.equals(Misc.ALL_FACILITY_ID)) {
      facilitySearchCriteria =
        (new SearchCriteria()).setField(CollectionKey.FACILITY_ID).setOp(Operator.Search.EQ.toString()).setValues(Collections.singletonList(currentFacilityId));
    }

    Specification<UserGroup> specification = SpecificationBuilder.createSpecification(filters, Collections.singletonList(facilitySearchCriteria));
    Page<UserGroup> userGroupPage = userGroupRepository.findAll(specification, pageable);
    List<UserGroupDto> userGroupDtos = userGroupMapper.toDto(userGroupPage.getContent());

    Set<Long> userGroupIds = userGroupDtos.stream()
      .map(userGroupDto -> Long.valueOf(userGroupDto.getId()))
      .collect(Collectors.toSet());

    List<Long> userGroupIdList = new ArrayList<>(userGroupIds);
    List<UserGroupMember> userGroupMembers = userGroupMemberRepository.findByUserGroupIdIn(userGroupIdList);

    Map<Long, List<UserDto>> userGroupMembersMap = userGroupMembers.stream()
      .collect(Collectors.groupingBy(
        userGroupMember -> userGroupMember.getUserGroup().getId(),
        Collectors.mapping(userGroupMember -> userMapper.toDto(userGroupMember.getUser()), Collectors.toList())
      ));

    for (UserGroupDto userGroupDto : userGroupDtos) {
      List<UserDto> users = userGroupMembersMap.getOrDefault(Long.valueOf(userGroupDto.getId()), Collections.emptyList());
      userGroupDto.setUsers(users);
      userGroupDto.setUserCount(users.size());
    }
    return new PageImpl<>(userGroupDtos, pageable, userGroupPage.getTotalElements());
  }


  // TODO: refactor validate methods
  private void validateUserGroupCreateRequest(UserGroupCreateRequest userGroupCreateRequest, Long currentFacilityId) throws StreemException {
    log.info("[validateUserGroupCreateRequest] Request to validate userGroupCreateRequest , userGroupCreateRequest: {} , currentFacilityId: {}", userGroupCreateRequest, currentFacilityId);
    validateUniqueUserGroup(userGroupCreateRequest.getName(), currentFacilityId, null);
    if (Utility.isEmpty(userGroupCreateRequest.getName()) || Utility.isEmpty(userGroupCreateRequest.getDescription()) || Utility.isEmpty(userGroupCreateRequest.getReason())) {
      ValidationUtils.invalidate((Long) null, ErrorCode.MANDATORY_FIELDS_MISSING);
    }
  }

  private void validateUserGroupUpdateRequest(UserGroupUpdateRequest userGroupUpdateRequest, Long currentFacilityId, Long id) throws StreemException {
    validateUniqueUserGroup(userGroupUpdateRequest.getName(), currentFacilityId, id);
    validateUserGroupArchival(id);
    if (Utility.isEmpty(userGroupUpdateRequest.getName()) || Utility.isEmpty(userGroupUpdateRequest.getDescription()) || Utility.isEmpty(userGroupUpdateRequest.getReason())) {
      ValidationUtils.invalidate((Long) null, ErrorCode.MANDATORY_FIELDS_MISSING);
    }
  }

  private void validateUserGroupArchival(Long id) throws StreemException {
    if (!userGroupRepository.existsByIdAndActive(id, true)) {
      ValidationUtils.invalidate((Long) null, ErrorCode.USER_GROUP_NOT_FOUND);
    }
  }

  private void validateUniqueUserGroup(String name, Long facilityId, Long id) throws StreemException {
    if (userGroupRepository.existsByNameAndFacilityIdAndActive(name, facilityId, true, id)) {
      ValidationUtils.invalidate((Long) null, ErrorCode.USER_GROUP_ALREADY_EXISTS);
    }
  }

  private void validateUserRoles(Set<Long> userIds) throws StreemException {
    boolean invalidUsers = userRepository.existsByRoles(userIds, Misc.USER_GROUP_ROLES);
    if (invalidUsers) {
      ValidationUtils.invalidate((Long) null, ErrorCode.INVALID_USER_ROLES);
    }
  }

}
