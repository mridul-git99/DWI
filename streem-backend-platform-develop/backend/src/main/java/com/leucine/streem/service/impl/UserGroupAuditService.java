package com.leucine.streem.service.impl;

import com.amazonaws.services.quicksight.model.ResourceNotFoundException;
import com.leucine.streem.constant.Operator;
import com.leucine.streem.dto.UserDto;
import com.leucine.streem.dto.UserGroupDto;
import com.leucine.streem.dto.request.RemoveUserRequest;
import com.leucine.streem.dto.request.UserGroupUpdateRequest;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.BaseEntity;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.model.helper.SpecificationBuilder;
import com.leucine.streem.model.helper.search.SearchCriteria;
import com.leucine.streem.repository.IFacilityRepository;
import com.leucine.streem.repository.IUserGroupAuditRepository;
import com.leucine.streem.repository.IUserGroupMemberRepository;
import com.leucine.streem.repository.IUserRepository;
import com.leucine.streem.service.IUserGroupAuditService;
import com.leucine.streem.util.DateTimeUtils;
import com.leucine.streem.util.Utility;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.leucine.streem.constant.Misc.ALL_FACILITY_ID;

@Service
@AllArgsConstructor
public class UserGroupAuditService implements IUserGroupAuditService {
  private static final String CREATE_USER_GROUP = "{0} {1} (ID:{2}) created an user group with name: \"{3}\" with description: \"{4}\" with reason: \"{5}\" in facility {6} with members: {7} ";
  private static final String UPDATE_USER_GROUP_NAME = "{0} {1} (ID:{2}) updated user group with name: \"{3}\" to name: \"{4}\" with reason: \"{5}\" in facility {6}";
  private static final String UPDATE_USER_GROUP_DESCRIPTION = "{0} {1} (ID:{2}) updated description of user group with name : \"{3}\" to description: \"{4}\" with reason: \"{5}\" in facility {6}";
  private static final String ADD_MEMBERS_TO_USER_GROUP = "{0} {1} (ID:{2}) added members : {3} to user group with name: \"{4}\" with reason: \"{5}\" in facility {6}";
  private static final String REMOVE_MEMBERS_FROM_USER_GROUP = "{0} {1} (ID:{2}) removed members : {3} from user group with name: \"{4}\" with reason: \"{5}\" in facility {6}";
  private static final String USER_ARCHIVAL_REMOVAL = "{0} (ID:{1}) removed user {2} {3} (ID:{4}) from user group \"{5}\" due to user archival by {6} {7} (ID:{8}) stating reason: \"{9}\"";

  private final IUserGroupAuditRepository userGroupAuditRepository;
  private final IUserRepository userRepository;
  private final IUserGroupMemberRepository userGroupMemberRepository;
  private final IFacilityRepository facilityRepository;

  @Override
  public void create(Long organisationsId, Long facilityId, UserGroup userGroup, String reason) {
    User user = userGroup.getCreatedBy();
    Facility facility = userGroup.getFacility();
    List<UserGroupMember> userGroupMembers = userGroupMemberRepository.findByUserGroupId(userGroup.getId());
    List<User> members = userGroupMembers.stream().map(UserGroupMember::getUser).toList();
    String userGroupName = userGroup.getName();
    String userGroupDescription = userGroup.getDescription();
    String facilityName = facility.getName();

    String[] memberDetails = new String[members.size()];
    for (int i = 0; i < memberDetails.length; i++) {
      User member = members.get(i);
      String memberDetail = formatMessage("{0} {1} (ID:{2})", member.getFirstName(), member.getLastName(), member.getEmployeeId());
      memberDetails[i] = memberDetail;
    }

    String details = String.valueOf(formatMessage(CREATE_USER_GROUP, user.getFirstName(), user.getLastName(), user.getEmployeeId(), userGroupName, userGroupDescription, reason, facilityName, String.join(", ", memberDetails)));

    UserGroupAudit userGroupAudit = getInfoAudit(organisationsId, facilityId, userGroup.getId(), user.getId(), details);
    userGroupAuditRepository.save(userGroupAudit);

  }

  @Override
  public void update(Long organisationsId, Long facilityId, Long triggeredBy, UserGroupDto oldUserGroupDto, UserGroup newUserGroup, UserGroupUpdateRequest userGroupUpdateRequest) {
    User triggeredByUser = userRepository.findById(triggeredBy).
      orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + triggeredBy));

    String oldName = oldUserGroupDto.getName();
    String oldDescription = oldUserGroupDto.getDescription();
    String newName = newUserGroup.getName();
    String newDescription = newUserGroup.getDescription();
    String facilityName = facilityRepository.getReferenceById(facilityId).getName();

    List<UserDto> oldMembers = oldUserGroupDto.getUsers();
    List<UserGroupMember> newUserGroupMembers = userGroupMemberRepository.findByUserGroupId(newUserGroup.getId());
    Map<String, UserDto> oldMembersMap = oldMembers.stream().collect(Collectors.toMap(UserDto::getId, Function.identity()));
    List<User> newMembers = newUserGroupMembers.stream().map(UserGroupMember::getUser).toList();
    Map<String, User> newMembersMap = newMembers.stream().collect(Collectors.toMap(BaseEntity::getIdAsString, Function.identity()));

    List<User> addedMembers = newMembers.stream().filter(user -> !oldMembersMap.containsKey(user.getIdAsString())).toList();
    List<UserDto> removedMembers = oldMembers.stream().filter(userDto -> !newMembersMap.containsKey(userDto.getId())).toList();

    String[] addedMemberDetails = new String[addedMembers.size()];
    for (int i = 0; i < addedMemberDetails.length; i++) {
      User member = addedMembers.get(i);
      String memberDetail = formatMessage("{0} {1} (ID:{2}) ", member.getFirstName(), member.getLastName(), member.getEmployeeId());
      addedMemberDetails[i] = memberDetail;
    }

    Map<Long, String> userRemovalReasonMap = userGroupUpdateRequest.getRemovedUser().stream()
      .collect(Collectors.toMap(RemoveUserRequest::getUserId, RemoveUserRequest::getReason));

    String[] removedMemberDetails = new String[removedMembers.size()];
    for (int i = 0; i < removedMemberDetails.length; i++) {
      UserDto member = removedMembers.get(i);
      String reason, memberDetail;
      if (!userGroupUpdateRequest.isUnAssignAllUsers()) {
        reason = userRemovalReasonMap.get(Long.parseLong(member.getId()));
        memberDetail = formatMessage("{0} {1} (ID:{2}) with removal reason \"{3}\"", member.getFirstName(), member.getLastName(), member.getEmployeeId(), reason);
      } else {
        memberDetail = formatMessage("{0} {1} (ID:{2})", member.getFirstName(), member.getLastName(), member.getEmployeeId());
      }
      removedMemberDetails[i] = memberDetail;
    }

    if (!oldName.equals(newName)) {

      String details = String.valueOf(formatMessage(UPDATE_USER_GROUP_NAME, triggeredByUser.getFirstName(), triggeredByUser.getLastName(), triggeredByUser.getEmployeeId(), oldName, newName, userGroupUpdateRequest.getReason(), facilityName));
      UserGroupAudit userGroupAudit = getInfoAudit(organisationsId, facilityId, newUserGroup.getId(), triggeredBy, details);
      userGroupAuditRepository.save(userGroupAudit);
    }

    if (!oldDescription.equals(newDescription)) {
      String details = String.valueOf(formatMessage(UPDATE_USER_GROUP_DESCRIPTION, triggeredByUser.getFirstName(), triggeredByUser.getLastName(), triggeredByUser.getEmployeeId(), oldName, newDescription, userGroupUpdateRequest.getReason(), facilityName));
      UserGroupAudit userGroupAudit = getInfoAudit(organisationsId, facilityId, newUserGroup.getId(), triggeredBy, details);
      userGroupAuditRepository.save(userGroupAudit);
    }

    if (!addedMembers.isEmpty()) {
      String details = String.valueOf(formatMessage(ADD_MEMBERS_TO_USER_GROUP, triggeredByUser.getFirstName(), triggeredByUser.getLastName(), triggeredByUser.getEmployeeId(), String.join(", ", addedMemberDetails), oldName, userGroupUpdateRequest.getReason(), facilityName));
      UserGroupAudit userGroupAudit = getInfoAudit(organisationsId, facilityId, newUserGroup.getId(), triggeredBy, details);
      userGroupAuditRepository.save(userGroupAudit);
    }

    if (!removedMembers.isEmpty()) {
      String details = String.valueOf(formatMessage(REMOVE_MEMBERS_FROM_USER_GROUP, triggeredByUser.getFirstName(), triggeredByUser.getLastName(), triggeredByUser.getEmployeeId(), String.join(", ", removedMemberDetails), oldName, userGroupUpdateRequest.getReason(), facilityName));
      UserGroupAudit userGroupAudit = getInfoAudit(organisationsId, facilityId, newUserGroup.getId(), triggeredBy, details);
      userGroupAuditRepository.save(userGroupAudit);
    }


  }

  @Override
  @Transactional
  public void archive(UserGroup userGroup, String reason) {
    User user = userGroup.getModifiedBy();
    Facility facility = facilityRepository.findById(userGroup.getFacilityId()).get();
    String userGroupName = userGroup.getName();
    String userGroupDescription = userGroup.getDescription();
    String facilityName = facility.getName();

    String details = formatMessage("{0} {1} (ID:{2}) archived user group with name: \"{3}\" with description: \"{4}\" with reason: \"{5}\" in facility {6}",
      user.getFirstName(), user.getLastName(), user.getEmployeeId(), userGroupName, userGroupDescription, reason, facilityName);


    UserGroupAudit userGroupAudit = getInfoAudit(facility.getOrganisation().getId(), facility.getId(), userGroup.getId(), user.getId(), details);
    userGroupAuditRepository.save(userGroupAudit);
  }


  @Override
  public Page<UserGroupAudit> getAudits(String filters, Pageable pageable) {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    SearchCriteria facilitySearchCriteria = null;
    Long currentFacilityId = principalUser.getCurrentFacilityId();
    if (currentFacilityId != null && !currentFacilityId.equals(ALL_FACILITY_ID)) {
      facilitySearchCriteria = (new SearchCriteria()).setField("facilityId").setOp(Operator.Search.EQ.toString()).setValues(Collections.singletonList(currentFacilityId));
    }
    Specification<UserGroupAudit> specification;
    if (!Utility.isEmpty(facilitySearchCriteria)) {
      specification = SpecificationBuilder.createSpecification(filters, new ArrayList<>());
    } else {
      specification = SpecificationBuilder.createSpecification(filters, List.of(facilitySearchCriteria));
    }
    return userGroupAuditRepository.findAll(specification, pageable);

  }

  @Override
  public void unarchive(UserGroup userGroup, String reason) {
    User user = userGroup.getModifiedBy();
    Facility facility = userGroup.getFacility();
    String userGroupName = userGroup.getName();
    String userGroupDescription = userGroup.getDescription();
    String facilityName = facility.getName();

    String details = formatMessage("{0} {1} (ID:{2}) unarchived user group with name: \"{3}\" with description: \"{4}\" with reason: \"{5}\" in facility {6}",
      user.getFirstName(), user.getLastName(), user.getEmployeeId(), userGroupName, userGroupDescription, reason, facilityName);

    UserGroupAudit userGroupAudit = getInfoAudit(facility.getOrganisation().getId(), facility.getId(), userGroup.getId(), user.getId(), details);
    userGroupAuditRepository.save(userGroupAudit);
  }

  @Override
  public void removeUserDueToArchival(List<UserGroupMember> userGroupMembers, String reason, PrincipalUser archivedByUser, PrincipalUser systemUser) {
    for (UserGroupMember member : userGroupMembers) {
      UserGroup userGroup = member.getUserGroup();
      User user = member.getUser();

      String details = formatMessage(USER_ARCHIVAL_REMOVAL,
        systemUser.getFirstName(), systemUser.getEmployeeId(),
        user.getFirstName(), user.getLastName(), user.getEmployeeId(),
        userGroup.getName(),
        archivedByUser.getFirstName(), archivedByUser.getLastName(), archivedByUser.getEmployeeId(),
        reason
      );

      UserGroupAudit userGroupAudit = getInfoAudit(
        userGroup.getFacility().getOrganisation().getId(),
        userGroup.getFacilityId(),
        userGroup.getId(),
        archivedByUser.getId(),
        details
      );
      userGroupAuditRepository.save(userGroupAudit);
    }
  }

  private String formatMessage(String pattern, String... replacements) {
    for (int i = 0; i < replacements.length; i++) {
      if (replacements[i] != null) {
        pattern = pattern.replace("{" + i + "}", replacements[i]);
      } else {
        pattern = pattern.replace("{" + i + "}", "");
      }
    }

    return pattern;
  }

  private UserGroupAudit getInfoAudit(Long organisationsId, Long facilityId, Long userGroupId, Long triggeredBy, String details) {
    return UserGroupAudit.builder()
      .organisationsId(organisationsId)
      .facilityId(facilityId)
      .userGroupId(userGroupId)
      .triggeredBy(triggeredBy)
      .details(details)
      .triggeredAt(DateTimeUtils.now())
      .build();
  }
}
