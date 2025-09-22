package com.leucine.streem.service.impl;

import com.leucine.streem.config.JaasServiceProperty;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.mapper.IUserMapper;
import com.leucine.streem.dto.request.*;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ExceptionType;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.TrainedUser;
import com.leucine.streem.model.User;
import com.leucine.streem.model.UserGroupMember;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.repository.*;
import com.leucine.streem.service.IJobAuditService;
import com.leucine.streem.service.IUserService;
import com.leucine.streem.util.DateTimeUtils;
import com.leucine.streem.util.PdfGeneratorUtil;
import com.leucine.streem.util.Utility;
import com.leucine.streem.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements IUserService {
  private final IUserRepository userRepository;
  private final IOrganisationRepository organisationRepository;
  private final IFacilityRepository facilityRepository;
  private final RestTemplate jaasRestTemplate;
  private final JaasServiceProperty jaasServiceProperty;
  private final IJobRepository jobRepository;
  private final ITaskExecutionAssigneeRepository taskExecutionAssigneeRepository;
  private final IUserMapper mapper;
  private final IUserMapper userMapper;
  private final IJobAuditService jobAuditService;
  private final ITrainedUserRepository trainedUserRepository;
  private final IUserGroupMemberRepository userGroupMemberRepository;
  private final PdfGeneratorUtil pdfGeneratorUtil;
  private final ChecklistAuditService checklistAuditService;
  private final UserGroupAuditService userGroupAuditService;

  @Override
  public Response<Object> getAll(String filters, Pageable pageable) {
    log.info("[getAll] Request to get all users, filters: {}, pageable: {}", filters, pageable);
    HttpEntity<Response> response = jaasRestTemplate.exchange(
      Utility.toUriString(jaasServiceProperty.getUserAllUrl(), filters, pageable), HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), Response.class);
    return response.getBody();
  }

  @Override
  public Response<Object> getAll(boolean archived, String filters, Pageable pageable) {
    log.info("[getAll] Request to get all users, archived: {}, filters: {}, pageable: {}", archived, filters, pageable);
    HttpEntity<Response> response = jaasRestTemplate.exchange(
      Utility.toUriString(jaasServiceProperty.getUserUrl(), Map.of("archived", archived, "filters", filters), pageable),
      HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), Response.class
    );
    return response.getBody();
  }

  @Override
  public Response<Object> getAllByRoles(Set<String> roles) {
    log.info("[getAllByRoles] Request to get all users, roles: {}", roles);
    FilterAssigneeRequest filterAssigneeRequest = new FilterAssigneeRequest();
//    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(jaasServiceProperty.getUserByRolesUrl()).queryParam("roles", roles);
    HttpEntity<Response> response = jaasRestTemplate.exchange(
      Utility.toUriString(jaasServiceProperty.getUserByRolesUrl(),
        Map.of("roles", roles)),
      HttpMethod.PATCH, new HttpEntity<>(filterAssigneeRequest), Response.class
    );
    return response.getBody();
  }

  @Override
  public Response<Object> getAllByRoles(List<String> roles, String filters, boolean isAssigned, FilterAssigneeRequest filterAssigneeRequest, Pageable pageable) {
    log.info("[getAllByRoles] Request to get all users, roles: {}, filters: {}, isAssigned: {}, assignees: {}, pageable: {}", roles, filters, isAssigned, filterAssigneeRequest.getAssignees(), pageable);
    filters = Utility.isEmpty(filters)? "": filters;
    HttpEntity<Response> response = jaasRestTemplate.exchange(
      Utility.toUriString(jaasServiceProperty.getUserByRolesUrl(),
        Map.of("roles", roles, "filters", filters, "isAssigned", isAssigned), pageable),
      HttpMethod.PATCH, new HttpEntity<>(filterAssigneeRequest), Response.class
    );
    return response.getBody();
  }

  @Override
  public Response<Object> getAllByRoles(List<String> roles, String filters, Pageable pageable) {
    log.info("[getAllByRoles] Request to get all users, roles: {}, filters: {}, pageable: {}", roles, filters, pageable);
    FilterAssigneeRequest filterAssigneeRequest = new FilterAssigneeRequest();
    HttpEntity<Response> response = jaasRestTemplate.exchange(
      Utility.toUriString(jaasServiceProperty.getUserByRolesUrl(), Map.of("roles", roles, "filters", filters), pageable),
      HttpMethod.PATCH, new HttpEntity<>(filterAssigneeRequest), Response.class
    );
    return response.getBody();
  }

  @Override
  public Response<Object> getAllUserAudit(String filters, Pageable pageable) {
    log.info("[getAllUserAudit] Request to get all user audits, filters: {}, pageable: {}", filters, pageable);
    HttpEntity<Response> response = jaasRestTemplate.exchange(
      Utility.toUriString(jaasServiceProperty.getUserAuditsUrl(), filters, pageable),
      HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), Response.class
    );
    return response.getBody();
  }

  @Override
  public Response<Object> switchFacility(Long usersId, Long facilityId) {
    log.info("[switchFacility] Request to switch facility, usersId {}, facilityId {}", usersId, facilityId);
    HttpEntity<PasswordUpdateRequest> entity = new HttpEntity<>(new HttpHeaders());
    ResponseEntity<Response> responseEntity = jaasRestTemplate.exchange(jaasServiceProperty.getSwitchFacilityUrl(usersId, facilityId), HttpMethod.PATCH, entity, Response.class);
    return responseEntity.getBody();
  }

  @Override
  public Response<Object> getById(Long userId) {
    log.info("[getById] Request to get user, userId: {}", userId);
    ResponseEntity<Response> response = jaasRestTemplate.getForEntity(jaasServiceProperty.getUserUrl(userId), Response.class);
    return response.getBody();
  }

  @Override
  public UserDto create(UserAddRequest userAddRequest) {
    log.info("[create] Request to add an user, userAddRequest: {}", userAddRequest);
    ResponseEntity<PrincipalUser> responseEntity = jaasRestTemplate.postForEntity(jaasServiceProperty.getUserUrl(), userAddRequest, PrincipalUser.class);
    PrincipalUser principalUser = responseEntity.getBody();
    syncUser(principalUser);
    return mapper.toDto(principalUser);
  }

  @Override
  public Response<Object> isUsernameAvailable(UsernameCheckRequest usernameCheckRequest) {
    log.info("[isUsernameAvailable] Request to check Username availability, usernameCheckRequest: {}", usernameCheckRequest);
    ResponseEntity<Response> responseEntity = jaasRestTemplate.postForEntity(jaasServiceProperty.getCheckUsernameUrl(), usernameCheckRequest, Response.class);
    return responseEntity.getBody();
  }

  @Override
  public Response<Object> isEmailAvailable(EmailCheckRequest emailCheckRequest) {
    log.info("[isEmailAvailable] Request to check Email availability, emailCheckRequest: {}", emailCheckRequest);
    ResponseEntity<Response> responseEntity = jaasRestTemplate.postForEntity(jaasServiceProperty.getCheckEmailUrl(), emailCheckRequest, Response.class);
    return responseEntity.getBody();
  }

  @Override
  public Response<Object> isEmployeeIdAvailable(EmployeeIdCheckRequest employeeIdCheckRequest) {
    log.info("[isEmployeeIdAvailable] Request to Employee Id availability, employeeIdCheckRequest: {}", employeeIdCheckRequest);
    ResponseEntity<Response> responseEntity = jaasRestTemplate.postForEntity(jaasServiceProperty.getCheckEmployeeIdUrl(), employeeIdCheckRequest, Response.class);
    return responseEntity.getBody();
  }

  @Override
  public UserDto update(final UserUpdateRequest userUpdateRequest) {
    log.info("[update] Request to update an user, userUpdateRequest: {}", userUpdateRequest);
    HttpEntity<UserUpdateRequest> entity = new HttpEntity<>(userUpdateRequest, new HttpHeaders());
    ResponseEntity<PrincipalUser> responseEntity = jaasRestTemplate.exchange(jaasServiceProperty.getUserUrl(userUpdateRequest.getId()), HttpMethod.PATCH, entity, PrincipalUser.class);
    PrincipalUser principalUser = responseEntity.getBody();
    syncUser(principalUser);
    return mapper.toDto(principalUser);
  }

  @Override
  public UserDto updateBasicInformation(final UserBasicInformationUpdateRequest userBasicInformationUpdateRequest) {
    log.info("[updateBasicInformation] Request to update basic information of user, userBasicInformationUpdateRequest: {}", userBasicInformationUpdateRequest);
    HttpEntity<UserBasicInformationUpdateRequest> entity = new HttpEntity<>(userBasicInformationUpdateRequest, new HttpHeaders());
    ResponseEntity<PrincipalUser> responseEntity = jaasRestTemplate.exchange(jaasServiceProperty.getUpdateUserBasicInformationUrl(userBasicInformationUpdateRequest.getId()), HttpMethod.PATCH, entity, PrincipalUser.class);
    PrincipalUser principalUser = responseEntity.getBody();
    syncUser(principalUser);
    return mapper.toDto(principalUser);
  }

  @Override
  public Response<Object> updatePassword(PasswordUpdateRequest passwordUpdateRequest) {
    log.info("[updatePassword] Request to update password");
    HttpEntity<PasswordUpdateRequest> entity = new HttpEntity<>(passwordUpdateRequest, new HttpHeaders());
    ResponseEntity<Response> responseEntity = jaasRestTemplate.exchange(jaasServiceProperty.getUpdateUserPasswordUrl(passwordUpdateRequest.getUserId()), HttpMethod.PATCH, entity, Response.class);
    return responseEntity.getBody();
  }

  @Override
  public Response<Object> getChallengeQuestionsAnswer(Long usersId, String token) {
    log.info("[getChallengeQuestionsAnswer] Request to get challenge question details");
    ResponseEntity<Response> responseEntity = jaasRestTemplate.exchange(
      Utility.toUriString(jaasServiceProperty.getChallengeQuestionsAnswerUrl(usersId), Collections.singletonMap("token", token)),
      HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), Response.class
    );
    return responseEntity.getBody();
  }

  @Override
  public Response<Object> updateChallengeQuestionsAnswer(ChallengeQuestionsAnswerUpdateRequest challengeQuestionsAnswerUpdateRequest) {
    log.info("[updateChallengeQuestionsAnswer] Request to update challenge question details");
    HttpEntity<ChallengeQuestionsAnswerUpdateRequest> entity = new HttpEntity<>(challengeQuestionsAnswerUpdateRequest, new HttpHeaders());
    ResponseEntity<Response> responseEntity = jaasRestTemplate.exchange(jaasServiceProperty.getChallengeQuestionsAnswerUrl(challengeQuestionsAnswerUpdateRequest.getUserId()), HttpMethod.PATCH, entity, Response.class);
    return responseEntity.getBody();
  }

  @Override
  public UserDto resetToken(Long userId) {
    log.info("[resetToken] Request to reset token, userId: {}", userId);
    ResponseEntity<PrincipalUser> responseEntity = jaasRestTemplate.exchange(jaasServiceProperty.getResetTokenUrl(userId), HttpMethod.PATCH, new HttpEntity<>(new HttpHeaders()), PrincipalUser.class);
    PrincipalUser principalUser = responseEntity.getBody();
    return mapper.toDto(principalUser);
  }

  @Override
  public UserDto cancelToken(Long userId) {
    log.info("[cancelToken] Request to cancel token, userId: {}", userId);
    ResponseEntity<PrincipalUser> responseEntity = jaasRestTemplate.exchange(jaasServiceProperty.getCancelTokenUrl(userId), HttpMethod.PATCH, new HttpEntity<>(new HttpHeaders()), PrincipalUser.class);
    PrincipalUser principalUser = responseEntity.getBody();
    return mapper.toDto(principalUser);
  }

  @Override
  @Transactional
  public UserDto archive(Long userId, UserArchiveRequest userArchiveRequest) throws ResourceNotFoundException, StreemException {
    log.info("[archive] Request to archive user, userId: {}", userId);
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new ResourceNotFoundException(userId, ErrorCode.USER_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    String archiveReason = userArchiveRequest.getReason();
    if (Utility.isEmpty(archiveReason)) {
      ValidationUtils.invalidate(userId, ErrorCode.REASON_CANNOT_BE_EMPTY);
    }

    User systemUser = userRepository.findById(User.SYSTEM_USER_ID).get();
    PrincipalUser systemPrincipalUser = userMapper.toPrincipalUser(systemUser);
    User archivingUser = userRepository.findById(userId).get();

    PrincipalUser archivedUser = userMapper.toPrincipalUser(archivingUser);


    handleUnassignmentsOfUserAndJobStateUpdate(userId, archiveReason, archivedUser, systemPrincipalUser);
    PrincipalUser archivedByUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    List<TrainedUser> trainedUsers = trainedUserRepository.findByUserId(userId);
    if (!trainedUsers.isEmpty()) {
      checklistAuditService.unmapTrainedUsersDueToArchival(trainedUsers, archiveReason, archivedByUser, systemPrincipalUser);
      trainedUserRepository.deleteByUserId(userId);
    }

    List<UserGroupMember> userGroupMembers = userGroupMemberRepository.findByUserId(userId);
    if (!userGroupMembers.isEmpty()) {
      userGroupAuditService.removeUserDueToArchival(userGroupMembers, archiveReason, archivedByUser, systemPrincipalUser);
      userGroupMemberRepository.deleteByUserId(userId);
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<UserArchiveRequest> entity = new HttpEntity<>(userArchiveRequest, new HttpHeaders());
    ResponseEntity<PrincipalUser> responseEntity = jaasRestTemplate.exchange(jaasServiceProperty.getArchiveUserUrl(userId), HttpMethod.PATCH, entity, PrincipalUser.class);
    PrincipalUser archivedUserFromJaas = responseEntity.getBody();

    return mapper.toDto(archivedUserFromJaas);
  }

  private void handleUnassignmentsOfUserAndJobStateUpdate(Long userId, String reason, PrincipalUser archivedUser, PrincipalUser systemPrincipalUser) {

    PrincipalUser archivedByUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    //TODO Sathyam : reuse unassign logic
    //unassign users from non completed tasks
    Set<Long> jobIds = taskExecutionAssigneeRepository.unassignUsersFromNonStartedAndInProgessTasks(userId);

    if (!Utility.isEmpty(jobIds)) {
      jobAuditService.userUnassignmentLogsForJobs(jobIds, reason, systemPrincipalUser, archivedUser, archivedByUser);
    }
    //if archived user was the only user assigned to job
    //set job state to UNASSIGNED
    jobRepository.updateJobToUnassignedIfNoUserAssigned();
  }

  @Override
  public UserDto unarchive(Long userId, UserUnarchiveRequest userUnarchiveRequest) throws ResourceNotFoundException {
    log.info("[unArchive] Request to unarchiveUser user, userId: {}", userId);
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new ResourceNotFoundException(userId, ErrorCode.USER_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<UserUnarchiveRequest> entity = new HttpEntity<>(userUnarchiveRequest, new HttpHeaders());
    ResponseEntity<PrincipalUser> responseEntity = jaasRestTemplate.exchange(jaasServiceProperty.getUnarchiveUserUrl(userId), HttpMethod.PATCH, entity, PrincipalUser.class);
    PrincipalUser principalUser = responseEntity.getBody();
    user.setArchived(false);
    userRepository.save(user);
    return mapper.toDto(principalUser);
  }

  @Override
  public UserDto unlock(Long userId) {
    log.info("[unlock] Request to unlock user, userId: {}", userId);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    ResponseEntity<PrincipalUser> responseEntity = jaasRestTemplate.exchange(jaasServiceProperty.getUnlockUserUrl(userId), HttpMethod.PATCH, new HttpEntity<>(new HttpHeaders()), PrincipalUser.class);
    PrincipalUser principalUser = responseEntity.getBody();
    return mapper.toDto(principalUser);
  }

  @Override
  public Response<List<PartialUserDto>> searchDirectoryUsers(String query, int limit) {
    log.info("[getAll] Request to get all users like {}", query);
    HttpEntity<String> entity = new HttpEntity<>(new HttpHeaders());
    ResponseEntity<Response> response = jaasRestTemplate.exchange(jaasServiceProperty.getDirectoryUsersUrl(query, limit), HttpMethod.GET, entity, Response.class);
    return response.getBody();
  }

  @Override
  public boolean syncUser(PrincipalUser principalUser) {
    if (null != principalUser) {
      Optional<User> optionalUser = userRepository.findById(principalUser.getId());
      if (optionalUser.isEmpty()) {
//      List<Facility> facilities = facilityRepository.findAllById(principalUser.getFacilities().stream().map(facilityDto -> Long.valueOf(facilityDto.getId())).collect(Collectors.toSet()));
        User user = new User();
        user.setId(principalUser.getId());
        user.setEmployeeId(principalUser.getEmployeeId());
        user.setFirstName(principalUser.getFirstName());
        user.setLastName(principalUser.getLastName());
        user.setEmail(principalUser.getEmail());
        user.setOrganisation(organisationRepository.findById(principalUser.getOrganisationId()).orElse(null));
        userRepository.save(user);
      } else {
        User user = optionalUser.get();
        user.setEmployeeId(principalUser.getEmployeeId());
        user.setFirstName(principalUser.getFirstName());
        user.setLastName(principalUser.getLastName());
        user.setEmail(principalUser.getEmail());
        user.setArchived(principalUser.isArchived());
        userRepository.save(user);
      }
      return true;
    }
    return false;
  }

  @Override
  public BasicDto validateArchiveUser(Long userId) throws StreemException {
    log.info("[archive] Request to validate archive user, userId: {}", userId);
    if (taskExecutionAssigneeRepository.isUserAssignedToInProgressTasks(userId)) {
      ValidationUtils.invalidate(userId, ErrorCode.CANNOT_ARCHIVE_USER);
    }
    var basicDto = new BasicDto();
    basicDto.setMessage("success");
    return basicDto;
  }

  @Override
  public byte[] generateUserAuditPdf(String filters, Pageable pageable) throws IOException {
    log.info("[getUserAuditPdf] Request to Download user audits, filters: {}, pageable: {}", filters, pageable);

    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.APPLICATION_PDF));

    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<byte[]> response = jaasRestTemplate.exchange(
      Utility.toUriString(jaasServiceProperty.getDownloadUserAudits(), filters, pageable),
      HttpMethod.GET,
      entity,
      byte[].class
    );

    return response.getBody();
  }


}
