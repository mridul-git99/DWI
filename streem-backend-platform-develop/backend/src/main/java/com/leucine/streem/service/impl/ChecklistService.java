package com.leucine.streem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leucine.streem.collections.EntityObject;
import com.leucine.streem.collections.PropertyValue;
import com.leucine.streem.constant.*;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.mapper.*;
import com.leucine.streem.dto.projection.*;
import com.leucine.streem.dto.request.*;
import com.leucine.streem.dto.response.Error;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.ExceptionType;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.*;
import com.leucine.streem.model.helper.parameter.*;
import com.leucine.streem.model.helper.search.SearchCriteria;
import com.leucine.streem.repository.*;
import com.leucine.streem.service.*;
import com.leucine.streem.util.*;
import com.leucine.streem.validator.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.leucine.streem.constant.Misc.ALL_FACILITY_ID;
import static com.leucine.streem.model.helper.search.Selector.PARAMETER;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChecklistService implements IChecklistService {
  private final IChecklistRepository checklistRepository;
  private final IChecklistCollaboratorMappingRepository checklistCollaboratorMappingRepository;
  private final IChecklistMapper checklistMapper;
  private final IUserMapper userMapper;
  private final ICodeService codeService;
  private final IChecklistAuditService checklistAuditService;
  private final IFacilityRepository facilityRepository;
  private final IOrganisationRepository organisationRepository;
  private final IFacilityUseCaseMappingRepository facilityUseCaseMappingRepository;
  private final IPropertyService propertyService;
  private final IUserRepository userRepository;
  private final IVersionRepository versionRepository;
  private final IVersionService versionService;
  private final IStageRepository stageRepository;
  private final IJobRepository jobRepository;
  private final IChecklistCollaboratorService checklistCollaboratorService;
  private final IParameterRepository parameterRepository;
  private final ITrainedUserTaskMappingRepository trainedUserTaskMappingRepository;
  private final ITrainedUserMapper trainedUserMapper;
  private final ITaskRepository taskRepository;
  private final IParameterMapper parameterMapper;
  private final IFacilityMapper facilityMapper;
  private final IJobLogService jobLogService;
  private final INotificationService notificationService;
  private final IChecklistCollaboratorCommentsRepository checklistCollaboratorCommentsRepository;
  private final IChecklistAuditRepository checklistAuditRepository;
  private final ICustomViewService customViewService;
  private final IEntityObjectRepository entityObjectRepository;
  private final IObjectTypeRepository objectRepository;
  private final IParameterValidationService parameterValidationService;
  private final IUserGroupRepository userGroupRepository;
  private final ITrainedUserRepository trainedUsersRepository;
  private final IElementCopyService elementCopyService;
  private final ISchedulerRepository schedulerRepository;
  private final ISchedulerService schedulerService;
  private final IInterlockService interlockService;
  private final IAutoInitializedParameterRepository autoInitializedParameterRepository;
  private final PdfGeneratorUtil pdfGeneratorUtil;

  @Override
  public Page<ChecklistPartialDto> getAllChecklist(String filters, Pageable pageable) {
    log.info("[getAllChecklist] Request to get all checklists, filters: {}, pageable: {}", filters, pageable);

    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    SearchCriteria organisationSearchCriteria = (new SearchCriteria()).setField(Checklist.ORGANISATION_ID).setOp(Operator.Search.EQ.toString()).setValues(Collections.singletonList(principalUser.getOrganisationId()));
    SearchCriteria facilitySearchCriteria = null;
    Long currentFacilityId = principalUser.getCurrentFacilityId();
    if (currentFacilityId != null && !currentFacilityId.equals(ALL_FACILITY_ID)) {
      facilitySearchCriteria =
        (new SearchCriteria()).setField(Checklist.FACILITY_ID).setOp(Operator.Search.EQ.toString()).setValues(Collections.singletonList(currentFacilityId));
    }

    //TODO make specification extend the original one
    //Specification<Checklist> specification = SpecificationBuilder.createSpecification(filters);
    Specification<Checklist> specification = ChecklistSpecificationBuilder.createSpecification(filters, Arrays.asList(organisationSearchCriteria, facilitySearchCriteria));
    //When checklist is filtered on properties, only the respective
    //property it is filtered on is fetched, thereby forcing us to fetch the property again
    //and fetching is much slower. To avoid this fetch only the top level entity without
    //using the entity graph collect the ids and read the data using entity graph for these ids
    Page<Checklist> checklistPage = checklistRepository.findAll(specification, pageable);
    Set<Long> ids = checklistPage.getContent()
      .stream().map(BaseEntity::getId).collect(Collectors.toSet());
    List<Checklist> checklists = checklistRepository.findAllByIdIn(ids, pageable.getSort());

    return new PageImpl<>(checklistMapper.toPartialDto(checklists), pageable, checklistPage.getTotalElements());
  }

  @Override
  public ChecklistDto getChecklistById(Long checklistId) throws ResourceNotFoundException {
    log.info("[getChecklistById] Request to get checklist, checklistId: {}", checklistId);
    var parameters = parameterRepository.getParametersByChecklistIdAndTargetEntityType(checklistId, Type.ParameterTargetEntityType.PROCESS);
    var checklist = checklistRepository.findById(checklistId)
      .orElseThrow(() -> new ResourceNotFoundException(checklistId, ErrorCode.PROCESS_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    var checklistDto
      = checklistMapper.toDto(checklist);
    var parameterDtos = parameterMapper.toDto(parameters);
    checklistDto.setParameters(parameterDtos);

    return checklistDto;
  }

  @Override
  public ChecklistInfoDto getChecklistInfoById(Long checklistId) throws ResourceNotFoundException {
    log.info("[getChecklistInfoById] Request to get checklist info, checklistId: {}", checklistId);
    // TODO Optimize this.
    try {
      Checklist checklist = checklistRepository.findById(checklistId)
        .orElseThrow(() -> new ResourceNotFoundException(checklistId, ErrorCode.PROCESS_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

      List<ChecklistCollaboratorView> signOffCollaborators = checklistCollaboratorMappingRepository.findAllByTypeOrderByOrderTreeAndModifiedAt(checklist.getId(), Type.Collaborator.SIGN_OFF_USER.toString());
      List<ChecklistSignOffDto> signOffUsers = signOffCollaborators.stream().map(u -> (new ChecklistSignOffDto()).setId(u.getId())
        .setEmail(u.getEmail())
        .setEmployeeId(u.getEmployeeId())
        .setFirstName(u.getFirstName())
        .setLastName(u.getLastName())
        .setState(u.getState())
        .setSignedAt(State.ChecklistCollaborator.SIGNED.name().equals(u.getState()) ? u.getModifiedAt() : null)
        .setOrderTree(u.getOrderTree())).collect(Collectors.toList());

      List<VersionDto> versionHistory = null;
      Long ancestor = Utility.isNull(checklist.getVersion()) ? null : checklist.getVersion().getAncestor();
      if (Utility.isNotNull(ancestor)) {
        List<Version> versions = versionRepository.findAllByAncestorOrderByVersionDesc(ancestor);
        if (!Utility.isEmpty(versions)) {
          List<Checklist> previousChecklists = checklistRepository.findAllById(versions.stream().map(Version::getSelf).collect(Collectors.toSet()));
          Map<Long, IdCodeHolder> map = previousChecklists.stream().filter(c -> ((c.getState() == State.Checklist.PUBLISHED) || c.getState() == State.Checklist.DEPRECATED)).collect(Collectors.toMap(Checklist::getId, c -> new IdCodeHolder(c.getId(), c.getCode(), c.getName())));
          versionHistory = versions.stream().filter(v -> map.get(v.getSelf()) != null).map(v -> {
            IdCodeHolder idCodeHolder = map.get(v.getSelf());
            return ((new VersionDto()).setId(String.valueOf(v.getSelf())).setCode(idCodeHolder.getCode()).setName(idCodeHolder.getName()).setVersionNumber(v.getVersion()).setDeprecatedAt(v.getDeprecatedAt()));
          }).collect(Collectors.toList());
        }
      }
      ChecklistInfoDto checklistInfoDto = new ChecklistInfoDto();
      checklistInfoDto.setId(String.valueOf(checklistId))
        .setName(checklist.getName())
        .setDescription(checklist.getDescription())
        .setCode(checklist.getCode())
        .setState(checklist.getState())
        .setAuthors(checklistCollaboratorService.getAllAuthors(checklist.getId()))
        .setPhase(checklist.getReviewCycle())
        .setSignOff(signOffUsers)
        .setVersions(versionHistory)
        .setRelease((new ReleaseDto().setReleaseAt(checklist.getReleasedAt()).setReleaseBy(userMapper.toUserAuditDto(checklist.getReleasedBy()))))
        .setAudit((new AuditDto()).setCreatedAt(checklist.getCreatedAt()).setCreatedBy(userMapper.toUserAuditDto(checklist.getCreatedBy()))
          .setModifiedAt(checklist.getModifiedAt()).setModifiedBy(userMapper.toUserAuditDto(checklist.getModifiedBy())));
      return checklistInfoDto;
    } catch (Exception e) {
      log.error("[getChecklistInfoById] Error fetching checklist", e);
      throw e;
    }
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public ChecklistDto createChecklist(CreateChecklistRequest createChecklistRequest) throws StreemException {
    log.info("[createChecklist] Request to create a checklist, createChecklistRequest: {}", createChecklistRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    User principalUserEntity = userRepository.getOne(principalUser.getId());
    Facility facility = facilityRepository.getOne(createChecklistRequest.getFacilityId());
    Organisation organisation = organisationRepository.getReferenceById(principalUser.getOrganisationId());
    FacilityUseCaseMapping facilityUseCaseMapping = facilityUseCaseMappingRepository.findByFacilityIdAndUseCaseId(facility.getId(), createChecklistRequest.getUseCaseId());
    UseCase useCase = facilityUseCaseMapping != null ? facilityUseCaseMapping.getUseCase() : null;

    if (useCase == null) {
      ValidationUtils.invalidate(createChecklistRequest.getUseCaseId(), ErrorCode.USE_CASE_NOT_FOUND);
    }

    List<Error> errorList = new ArrayList<>();

    Checklist checklist = new Checklist();
    checklist.setId(IdGenerator.getInstance().nextId());
    checklist.setName(createChecklistRequest.getName());
    checklist.setDescription(createChecklistRequest.getDescription());
    checklist.setCode(codeService.getCode(Type.EntityType.CHECKLIST, principalUser.getOrganisationId()));
    checklist.setState(State.Checklist.BEING_BUILT);
    checklist.setCreatedBy(principalUserEntity);
    checklist.setModifiedBy(principalUserEntity);
    checklist.setOrganisation(organisation);
    checklist.setColorCode(createChecklistRequest.getColorCode());
    if (!Objects.equals(principalUser.getCurrentFacilityId(), null)) {
      checklist.addFacility(facility, principalUserEntity);
    } else {
      checklist.setGlobal(true);
    }
    checklist.setOrganisationId(organisation.getId());
    checklist.setUseCase(useCase);
    checklist.setUseCaseId(useCase.getId());


    //add primary author from user context
    setProperties(checklist, createChecklistRequest.getProperties(), principalUserEntity, errorList);
    addAuthors(checklist, createChecklistRequest.getAuthors(), principalUserEntity);
    addPrimaryAuthor(checklist, principalUserEntity);
    checklist.getStages().add(createStage(principalUserEntity, checklist));

    if (!errorList.isEmpty()) {
      throw new StreemException(ErrorMessage.COULD_NOT_CREATE_CHECKLIST, errorList);
    }

    checklist = checklistRepository.save(checklist);
    checklistAuditService.create(checklist.getId(), checklist.getCode(), principalUser);

    Version version = versionService.createNewVersion(checklist.getId(), Type.EntityType.CHECKLIST, principalUserEntity);
    checklist.setVersion(version);
    checklistRepository.save(checklist);

    return checklistMapper.toDto(checklist);
  }

  @Override
  public BasicDto archiveChecklist(Long checklistId, String reason) throws ResourceNotFoundException, StreemException {
    log.info("[archiveChecklist] Request to archive checklist, checklistId: {}, reason: {}", checklistId, reason);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (Utility.isEmpty(reason)) {
      ValidationUtils.invalidate(checklistId, ErrorCode.ARCHIVE_REASON_CANNOT_BE_EMPTY);
    }
    reason = reason.trim();

    User principalUserEntity = userRepository.getOne(principalUser.getId());
    Checklist checklist = checklistRepository.findById(checklistId)
      .orElseThrow(() -> new ResourceNotFoundException(checklistId, ErrorCode.PROCESS_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    validateIfChecklistCanBeArchived(checklistId);

    /*
      Checklist can be archived in any state by unarchived Author, Owner, Facility Admin
    */

    if (!Objects.equals(checklist.getCreatedBy().getId(), principalUser.getId())) {
      validateRolesForChecklistArchival(principalUser, true, checklist.isGlobal());
    }

    checklist.setArchived(true);
    checklist.setModifiedBy(principalUserEntity);
    checklist = checklistRepository.save(checklist);

    checklistAuditService.archive(checklistId, checklist.getCode(), reason, principalUser);

    BasicDto basicDto = new BasicDto();
    basicDto.setId(checklist.getIdAsString())
      .setMessage("success");
    return basicDto;
  }

  private void validateRolesForChecklistArchival(PrincipalUser principalUser, boolean archive, boolean isGlobal) throws StreemException {
    if (isGlobal) {
      boolean globalArchivalRolesMatch = principalUser.getRoleNames()
        .stream().anyMatch(Misc.GLOBAL_CHECKLIST_ARCHIVAL_ROLES::contains);
      if (!globalArchivalRolesMatch) {
        if (!archive) {
          ValidationUtils.invalidate(principalUser.getId(), ErrorCode.GLOBAL_PROCESS_CAN_ONLY_BE_ARCHIVED_BY);
        } else {
          ValidationUtils.invalidate(principalUser.getId(), ErrorCode.GLOBAL_PROCESS_CAN_ONLY_BE_UNARCHIVED_BY);
        }
      }
    } else {
      boolean archivalRolesMatch = principalUser.getRoleNames()
        .stream().anyMatch(Misc.CHECKLIST_ARCHIVAL_ROLES::contains);
      if (!archivalRolesMatch) {
        if (archive) {
          ValidationUtils.invalidate(principalUser.getId(), ErrorCode.PROCESS_CAN_ONLY_BE_ARCHIVED_BY);
        } else {
          ValidationUtils.invalidate(principalUser.getId(), ErrorCode.PROCESS_CAN_ONLY_BE_UNARCHIVED_BY);
        }
      }
    }
  }

  @Override
  public BasicDto validateChecklistArchival(Long checklistId) throws ResourceNotFoundException, StreemException {
    log.info("[validateChecklistArchival] Request to validate if checklist can be archived, checklistId: {}", checklistId);
    Checklist checklist = checklistRepository.findById(checklistId)
      .orElseThrow(() -> new ResourceNotFoundException(checklistId, ErrorCode.PROCESS_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    validateIfChecklistCanBeArchived(checklistId);

    BasicDto basicDto = new BasicDto();
    basicDto.setId(checklist.getIdAsString())
      .setMessage("checklist can be archived");
    return basicDto;
  }

  @Override
  public BasicDto unarchiveChecklist(Long checklistId, String reason) throws ResourceNotFoundException, StreemException {
    log.info("[unarchiveChecklist] Request to unArchive checklist, checklistId: {}, reason: {}", checklistId, reason);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (Utility.isEmpty(reason)) {
      ValidationUtils.invalidate(checklistId, ErrorCode.UNARCHIVE_REASON_CANNOT_BE_EMPTY);
    }
    reason = reason.trim();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    Checklist checklist = checklistRepository.findById(checklistId)
      .orElseThrow(() -> new ResourceNotFoundException(checklistId, ErrorCode.PROCESS_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    if (!Objects.equals(checklist.getCreatedBy().getId(), principalUser.getId())) {
      validateRolesForChecklistArchival(principalUser, false, checklist.isGlobal());
    }

    checklist.setArchived(false);
    checklist.setModifiedBy(principalUserEntity);
    checklist = checklistRepository.save(checklist);

    checklistAuditService.unarchive(checklistId, checklist.getCode(), reason, principalUser);

    BasicDto basicDto = new BasicDto();
    basicDto.setId(checklist.getIdAsString())
      .setMessage("success");
    return basicDto;
  }

  @Override
  public BasicDto updateChecklist(Long checklistId, ChecklistUpdateRequest checklistUpdateRequest) throws ResourceNotFoundException, StreemException {
    log.info("[updateChecklist] Request to update checklist, checklistId: {}, checklistUpdateRequest: {}", checklistId, checklistUpdateRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    Checklist checklist = checklistRepository.findById(checklistId)
      .orElseThrow(() -> new ResourceNotFoundException(checklistId, ErrorCode.PROCESS_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    validateChecklistModificationState(checklist.getId(), checklist.getState());
    validateIfUserIsAuthorForPrototype(checklist.getId(), principalUser.getId());

    List<Error> errorList = new ArrayList<>();
    if (null != checklistUpdateRequest.getName()) {
      checklist.setName(checklistUpdateRequest.getName());
    }
    if (null != checklistUpdateRequest.getDescription()) {
      checklist.setDescription(checklistUpdateRequest.getDescription());
    }
    setProperties(checklist, checklistUpdateRequest.getProperties(), principalUserEntity, errorList);

    Map<Long, Boolean> authors = checklist.getCollaborators().stream()
      .filter(c -> c.getPhaseType().equals(State.ChecklistCollaboratorPhaseType.BUILD))
      .collect(Collectors.toMap(a -> a.getUser().getId(), ChecklistCollaboratorMapping::isPrimary));

    Set<Long> reviewers = checklist.getCollaborators().stream()
      .filter(c -> c.getPhaseType().equals(State.ChecklistCollaboratorPhaseType.REVIEW))
      .map(a -> a.getUser().getId()).collect(Collectors.toSet());
    for (Long id : checklistUpdateRequest.getAddAuthorIds()) {
      if (authors.containsKey(id)) {
        ValidationUtils.addError(id, errorList, ErrorCode.PROCESS_AUTHOR_ALREADY_ASSIGNED);
      }
      if (reviewers.contains(id)) {
        ValidationUtils.addError(id, errorList, ErrorCode.PROCESS_REVIEWER_CANNOT_BE_AUTHOR);
      }
    }

    for (Long id : checklistUpdateRequest.getRemoveAuthorIds()) {
      if (authors.containsKey(id)) {
        boolean isPrimaryAuthor = authors.get(id);
        if (isPrimaryAuthor) {
          ValidationUtils.addError(id, errorList, ErrorCode.CANNOT_UNASSIGN_PRIMARY_AUTHOR_FROM_PROCESS);
        }
      } else {
        ValidationUtils.addError(id, errorList, ErrorCode.PROCESS_AUTHOR_NOT_ASSIGNED);
      }
    }

    if (!errorList.isEmpty()) {
      throw new StreemException(ErrorMessage.COULD_NOT_UPDATE_CHECKLIST, errorList);
    }

    for (Long id : checklistUpdateRequest.getAddAuthorIds()) {
      checklist.addAuthor(userRepository.getOne(id), checklist.getReviewCycle(), principalUserEntity);
    }

    checklist.setModifiedBy(principalUserEntity);
    checklist.setColorCode(checklistUpdateRequest.getColorCode());
    checklistRepository.save(checklist);
    checklistCollaboratorMappingRepository.deleteAuthors(checklistId, checklistUpdateRequest.getRemoveAuthorIds());

    notificationService.notifyAuthors(checklistUpdateRequest.getAddAuthorIds(), checklistId, principalUser.getOrganisationId());
    BasicDto basicDto = new BasicDto();
    basicDto.setMessage("success");
    return basicDto;
  }

  @Override
  public BasicDto validateChecklist(Long checklistId) throws ResourceNotFoundException, IOException, StreemException {
    log.info("[validateChecklist] Request to validate checklist, checklistId: {}", checklistId);
    Checklist checklist = checklistRepository.findById(checklistId)
      .orElseThrow(() -> new ResourceNotFoundException(checklistId, ErrorCode.PROCESS_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    List<Error> errorList = new ArrayList<>();
    Set<Stage> stages = checklist.getStages();

    if(checklist.isArchived()) {
      ValidationUtils.invalidate(checklist.getId(), ErrorCode.PROCESS_ARCHIVED_VALIDATION);
    }

    List<Parameter> unmappedParameters = parameterRepository.getParametersByChecklistIdAndTargetEntityType(checklistId, Type.ParameterTargetEntityType.UNMAPPED);
    if (!Utility.isEmpty(unmappedParameters)) {
      ValidationUtils.invalidate(checklistId, ErrorCode.UNMAPPED_PARAMETERS_EXISTS);
    }

    if (stages.isEmpty()) {
      ValidationUtils.addError(checklist.getId(), errorList, ErrorCode.PROCESS_EMPTY_STAGE_VALIDATION);
    } else {
      for (Stage stage : stages) {
        if (Utility.isEmpty(stage.getName())) {
          ValidationUtils.addError(stage.getId(), errorList, ErrorCode.STAGE_NAME_CANNOT_BE_EMPTY);
        }
        validateTasks(checklistId, stage, errorList);
      }
    }

    List<Parameter> processParameterList = parameterRepository.getParametersByChecklistIdAndTargetEntityType(checklistId, Type.ParameterTargetEntityType.PROCESS);
    validateIfParameterBeingCJFMappedToTask(processParameterList, errorList);
    checkForCyclicTaskDependencies(checklist);

    if (!Utility.isEmpty(errorList)) {
      ValidationUtils.invalidate("Checklist configuration incomplete", errorList);
    }

    BasicDto basicDto = new BasicDto();
    basicDto.setMessage("success");
    return basicDto;
  }

  @Override
  public Checklist findById(Long checklistId) throws ResourceNotFoundException {
    return checklistRepository.findById(checklistId)
      .orElseThrow(() -> new ResourceNotFoundException(checklistId, ErrorCode.PROCESS_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
  }

  @Override
  public Checklist findByTaskId(Long taskId) throws ResourceNotFoundException {
    return checklistRepository.findByTaskId(taskId)
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.PROCESS_BY_TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
  }

  /**
   * method validates if for the given checklist id and user id
   * does collaborator mapping entry exists for collaborator types
   * author and primary author
   *
   * @param checklistId
   * @param userId
   * @throws StreemException
   */
  @Override
  public void validateIfUserIsAuthorForPrototype(Long checklistId, Long userId) throws StreemException {
    if (!checklistCollaboratorMappingRepository.isCollaboratorMappingExistsByChecklistAndUserIdAndCollaboratorType(checklistId, userId, Type.AUTHOR_TYPES)) {
      ValidationUtils.invalidate(checklistId, ErrorCode.USER_NOT_ALLOWED_TO_MODIFY_PROCESS);
    }
  }

  @Override
  public void validateChecklistModificationState(Long checklistId, State.Checklist state) throws StreemException {
    if (!State.CHECKLIST_EDIT_STATES.contains(state)) {
      ValidationUtils.invalidate(checklistId, ErrorCode.PROCESS_CANNOT_BE_MODFIFIED);
    }
  }


  @Override
  public List<TrainedUsersView> getTrainedUsersOfFacility(Long checklistId, Long facilityId) {
    return trainedUserTaskMappingRepository.findAllByChecklistIdAndFacilityId(checklistId, facilityId, true, false, null, 1000, 0);
  }

  @Override
  public List<TaskAssigneeView> getTaskAssignmentDetails(Long checklistId, boolean isUser, boolean isUserGroup, Set<Long> taskIds) {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Long currentFacilityId = principalUser.getCurrentFacilityId();
    return taskRepository.findByTaskIdIn(checklistId, taskIds, taskIds.size(), currentFacilityId, isUser, isUserGroup);
  }

  @Override
  public List<FacilityDto> getFacilityChecklistMapping(Long checklistId) throws ResourceNotFoundException {
    log.info("[getFacilityChecklistMapping] Request to get checklist facility mapping, checklistId: {}", checklistId);
    Checklist checklist = checklistRepository.findById(checklistId)
      .orElseThrow(() -> new ResourceNotFoundException(checklistId, ErrorCode.PROCESS_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    return facilityMapper.toDto(checklist.getFacilities().stream().map(ChecklistFacilityMapping::getFacility).collect(Collectors.toList()));
  }

  @Override
  public BasicDto bulkAssignmentFacilityIds(Long checklistId, ChecklistFacilityAssignmentRequest checklistFacilityAssignmentRequest) throws ResourceNotFoundException {
    var checklist = findById(checklistId);
    var assignedIds = checklistFacilityAssignmentRequest.getAssignedFacilityIds();
    var unassignedIds = checklistFacilityAssignmentRequest.getUnassignedFacilityIds();

    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    Set<Facility> assignedFacilities = Set.copyOf(facilityRepository.findAllById(assignedIds));

    checklist.addFacility(assignedFacilities, principalUserEntity);
    checklistRepository.save(checklist);

    checklistRepository.removeChecklistFacilityMapping(checklistId, unassignedIds);
    var basicDto = new BasicDto();
    basicDto.setMessage("success");
    return basicDto;
  }

  @Override
  public BasicDto bulkAssignDefaultUsers(Long checklistId, ChecklistTaskAssignmentRequest checklistTaskAssignmentRequest, boolean notify) throws StreemException {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Long currentFacilityId = principalUser.getCurrentFacilityId();
    if (Objects.equals(currentFacilityId, null)) {
      ValidationUtils.invalidate(principalUser.getId(), ErrorCode.CANNOT_ASSIGN_TRAINING_USER_IN_ALL_FACILITY);
    }
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());
    Set<Long> assignedTaskIds = checklistTaskAssignmentRequest.getTaskIds();
    Set<Long> assignedUserIds = checklistTaskAssignmentRequest.getAssignedUserIds();
    Set<Long> unassignedIds = checklistTaskAssignmentRequest.getUnassignedUserIds();
    Set<Long> assignedUserGroupIds = checklistTaskAssignmentRequest.getAssignedUserGroupIds();
    Set<Long> unassignedUserGroupIds = checklistTaskAssignmentRequest.getUnassignedUserGroupIds();
//    Set<Permission.PROCESS> assignedPermissions = checklistTaskAssignmentRequest.getAssignedPermissions();

    if (checklistTaskAssignmentRequest.isAllUsersSelected()) {
      Set<Long> userList = trainedUsersRepository.findAllUserIdsByChecklistId(checklistId);
      assignedUserIds.addAll(userList);
    }

    if (checklistTaskAssignmentRequest.isAllUserGroupsSelected()) {
      Set<Long> userGroupList = trainedUsersRepository.findAllUserGroupIdsByChecklistId(checklistId);
      assignedUserGroupIds.addAll(userGroupList);
    }

    List<User> assignedUsers = userRepository.findAllById(assignedUserIds);
    List<UserGroup> assignedUserGroups = userGroupRepository.findAllById(assignedUserGroupIds);
    List<Task> tasks = taskRepository.findAllById(assignedTaskIds);
    Map<User, List<Task>> userTaskAssignments = new HashMap<>();
    Map<UserGroup, List<Task>> userGroupTaskAssignments = new HashMap<>();
    Map<User, List<Task>> userTaskUnAssignments = new HashMap<>();
    Map<UserGroup, List<Task>> userGroupTaskUnAssignments = new HashMap<>();


    List<TrainedUserTaskMapping> trainedUserTaskMappings = new ArrayList<>();
    if (!Utility.isEmpty(assignedUserIds)) {
      Map<Long, TrainedUser> trainedUserMap = trainedUsersRepository.findAllByChecklistIdAndFacilityIdAndUserIdIn(checklistId, currentFacilityId ,assignedUserIds)
        .stream().collect(Collectors.toMap(TrainedUser::getUserId, Function.identity()));

      for (User user : assignedUsers) {
        List<Task> assignedTasksForUser = new ArrayList<>();
        for (Task task : tasks) {
          boolean isUserMappedToTask = validateIfUserIsMappedToTask(checklistId, task.getId(), user.getId(), currentFacilityId);
          if (!isUserMappedToTask) {
            TrainedUserTaskMapping trainedUserTaskMapping = new TrainedUserTaskMapping();
            trainedUserTaskMapping.setTrainedUser(trainedUserMap.get(user.getId()));
            trainedUserTaskMapping.setTask(task);
            trainedUserTaskMapping.setCreatedAt(DateTimeUtils.now());
            trainedUserTaskMapping.setCreatedBy(principalUserEntity);
            trainedUserTaskMapping.setModifiedAt(DateTimeUtils.now());
            trainedUserTaskMapping.setModifiedBy(principalUserEntity);
            trainedUserTaskMappings.add(trainedUserTaskMapping);

            assignedTasksForUser.add(task);
          }

        }
        userTaskAssignments.put(user, assignedTasksForUser);
      }
    }

    if (!Utility.isEmpty(assignedUserGroupIds)) {
      Map<Long, TrainedUser> trainedUserMap = trainedUsersRepository.findAllByChecklistIdAndUserGroupIdIn(checklistId, assignedUserGroupIds)
        .stream().collect(Collectors.toMap(TrainedUser::getUserGroupId, Function.identity()));
      for (UserGroup userGroup : assignedUserGroups) {
        List<Task> assignedTasksForUserGroup = new ArrayList<>();
        for (Task task : tasks) {
          boolean isUserGroupMappedToTask = validateIfUserGroupIsMappedToTask(checklistId, task.getId(), userGroup.getId(), currentFacilityId);
          if (!isUserGroupMappedToTask) {
            TrainedUserTaskMapping trainedUserTaskMapping = new TrainedUserTaskMapping();
            trainedUserTaskMapping.setTask(task);
            trainedUserTaskMapping.setTrainedUser(trainedUserMap.get(userGroup.getId()));
            trainedUserTaskMapping.setCreatedAt(DateTimeUtils.now());
            trainedUserTaskMapping.setCreatedBy(principalUserEntity);
            trainedUserTaskMapping.setModifiedAt(DateTimeUtils.now());
            trainedUserTaskMapping.setModifiedBy(principalUserEntity);
            trainedUserTaskMappings.add(trainedUserTaskMapping);

            assignedTasksForUserGroup.add(task);
          }
        }
        userGroupTaskAssignments.put(userGroup, assignedTasksForUserGroup);
      }
    }

    if (!Utility.isEmpty(trainedUserTaskMappings)) {
      trainedUserTaskMappingRepository.saveAll(trainedUserTaskMappings);
    }

    if (!Utility.isEmpty(unassignedIds)) {
      List<User> unassignedUsers = userRepository.findAllById(unassignedIds);

      for (User user : unassignedUsers) {
        List<Task> unassignedTasksForUser = new ArrayList<>();
        for (Task task : tasks) {
          boolean isUserMappedToTask = validateIfUserIsMappedToTask(checklistId, task.getId(), user.getId(), currentFacilityId);
          if (isUserMappedToTask) {
            unassignedTasksForUser.add(task);
          }
        }
        if (!unassignedTasksForUser.isEmpty()) {
          userTaskUnAssignments.put(user, unassignedTasksForUser);
        }
      }

      trainedUserTaskMappingRepository.deleteByChecklistIdAndUserIdInAndTaskIdIn(checklistId, unassignedIds, assignedTaskIds);
    }

    if (!Utility.isEmpty(unassignedUserGroupIds)) {
      List<UserGroup> unassignedUserGroups = userGroupRepository.findAllById(unassignedUserGroupIds);

      for (UserGroup userGroup : unassignedUserGroups) {
        List<Task> unassignedTasksForUserGroup = new ArrayList<>();
        for (Task task : tasks) {
          boolean isUserGroupMappedToTask = validateIfUserGroupIsMappedToTask(checklistId, task.getId(), userGroup.getId(), currentFacilityId);
          if (isUserGroupMappedToTask) {
            unassignedTasksForUserGroup.add(task);
          }
        }
        if (!unassignedTasksForUserGroup.isEmpty()) {
          userGroupTaskUnAssignments.put(userGroup, unassignedTasksForUserGroup);
        }
      }
      trainedUserTaskMappingRepository.deleteByChecklistIdAndUserGroupIdInAndTaskIdIn(checklistId, unassignedUserGroupIds, assignedTaskIds);
    }

    if (!userTaskAssignments.isEmpty()) {
      checklistAuditService.assignTrainedUsersToTask(checklistId, principalUser, userTaskAssignments);
    }
    if (!userGroupTaskAssignments.isEmpty()) {
      checklistAuditService.assignTrainedUserGroupsToTask(checklistId, principalUser, userGroupTaskAssignments);
    }

    if (!userTaskUnAssignments.isEmpty()) {
      checklistAuditService.unAssignTrainedUsersToTask(checklistId, principalUser, userTaskUnAssignments);
    }

    if (!userGroupTaskUnAssignments.isEmpty()) {
      checklistAuditService.unAssignTrainedUserGroupsToTask(checklistId, principalUser, userGroupTaskUnAssignments);
    }

    var basicDto = new BasicDto();
    basicDto.setMessage("success");
    return basicDto;
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public ChecklistBasicDto customPublishChecklist(Long checklistId) throws ResourceNotFoundException, StreemException, JsonProcessingException {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    Checklist checklist = checklistRepository.findById(checklistId).orElseThrow(() -> new ResourceNotFoundException(checklistId, ErrorCode.PROCESS_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    validateIfUserIsPrimaryAuthor(checklist, principalUserEntity.getId());
    checklist = checklistCollaboratorService.finalizeAndSaveChecklist(checklistId);
    saveToChecklistCollabaratorMapping(checklist, principalUserEntity);
    saveToChecklistAudit(checklist, principalUser);
    saveToChecklistComment(checklist, principalUserEntity);
    saveToVersion(checklist);
    if (!Utility.isEmpty(checklist.getVersion().getParent())) {
      Long parent = checklist.getVersion().getParent();
      schedulerService.findAndDeprecateSchedulersForChecklist(parent, principalUserEntity);
    }
    return checklistMapper.toChecklistBasicDto(checklist);
  }

  private void validateIfUserIsPrimaryAuthor(Checklist checklist, Long principalUsersId) throws StreemException {
    if (!isPrimaryAuthor(checklist, principalUsersId)) {
      ValidationUtils.invalidate(principalUsersId, ErrorCode.ONLY_PRIMARY_AUTHORS_ALLOWED);
    }
  }

  private boolean isPrimaryAuthor(Checklist checklist, Long principalUsersId) {
    return checklist.getCollaborators().stream().anyMatch(a -> a.getUser().getId().equals(principalUsersId) && a.isPrimary());
  }

  private boolean validateIfUserGroupIsMappedToTask(Long checklistId, Long taskId, Long userGroupId, Long currentFacilityId) {
    return trainedUserTaskMappingRepository.existsByChecklistIdAndTaskIdAndUserGroupIdAndFacilityId(checklistId, taskId, userGroupId, currentFacilityId);
  }

  private boolean validateIfUserIsMappedToTask(Long checklistId, Long taskId, Long userId, Long currentFacilityId) {
    return trainedUserTaskMappingRepository.existsByChecklistIdAndTaskIdAndUserIdAndFacilityId(checklistId, taskId, userId, currentFacilityId);
  }

  private void saveToChecklistCollabaratorMapping(Checklist checklist, User user) {
    List<State.ChecklistCollaborator> stateList = Misc.stateList;
    List<Type.Collaborator> typeList = Misc.typeList;
    List<Integer> phaseList = Misc.phaseList;
    List<Integer> orderTreeList = Misc.orderTreeList;
    List<State.ChecklistCollaboratorPhaseType> phaseTypeList = Misc.phaseTypeList;
    for (int i = 1; i < stateList.size(); i++) {
      ChecklistCollaboratorMapping cmp = new ChecklistCollaboratorMapping(checklist, user, typeList.get(i), phaseList.get(i), phaseTypeList.get(i), user);
      Long id = IdGenerator.getInstance().nextId();
      cmp.setId(id);
      cmp.setState(stateList.get(i));
      cmp.setOrderTree(orderTreeList.get(i));
      checklistCollaboratorMappingRepository.save(cmp);
    }
  }

  private void saveToChecklistAudit(Checklist checklist, PrincipalUser principalUser) {
    checklistAuditService.publish(checklist.getId(), checklist.getCode(), principalUser);
  }

  private void saveToChecklistComment(Checklist checklist, User principalUser) throws ResourceNotFoundException {
    List<ChecklistCollaboratorMapping> activeChecklistCollaboratorMapping = checklistCollaboratorMappingRepository.findByChecklistAndPhaseTypeAndTypeAndPhase(checklist.getId(), String.valueOf(State.ChecklistCollaboratorPhaseType.REVIEW), String.valueOf(Type.Collaborator.REVIEWER), 2);
    for (ChecklistCollaboratorMapping cmp : activeChecklistCollaboratorMapping) {
      ChecklistCollaboratorComments checklistCollaboratorComments = new ChecklistCollaboratorComments();
      checklistCollaboratorComments.setChecklist(checklist).setComments("All OK, No Comments").setReviewState(State.ChecklistCollaborator.COMMENTED_OK).setChecklistCollaboratorMapping(cmp).setModifiedBy(principalUser).setCreatedBy(principalUser);
      checklistCollaboratorComments = checklistCollaboratorCommentsRepository.save(checklistCollaboratorComments);
    }
  }

  private void saveToVersion(Checklist checklist) throws ResourceNotFoundException {
    Version version = versionRepository.findVersionBySelf(checklist.getId());
    Integer recentVersion = versionRepository.findRecentVersionByAncestor(version.getAncestor());
    if (Utility.isEmpty(recentVersion)) {
      version.setVersion(1);
    } else {
      version.setVersion(recentVersion + 1);
      Long parentChecklistId = version.getParent();
      if (!Utility.isEmpty(parentChecklistId)) {
        versionRepository.deprecateVersion(DateTimeUtils.now(), parentChecklistId);
        checklistRepository.updateState(State.Checklist.DEPRECATED, parentChecklistId);
      }
    }
    version.setVersionedAt(DateTimeUtils.now());
    versionRepository.save(version);
  }


  @Transactional(rollbackFor = Exception.class)
  @Override
  public List<ParameterInfoDto> configureProcessParameters(Long checklistId, MapJobParameterRequest mapJobParameterRequest) throws ResourceNotFoundException, StreemException {
    log.info("[configureProcessParameters] Request to configure Job Parameters, checklistId: {}, configureProcessParameters: {}", checklistId, mapJobParameterRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Checklist checklist = checklistRepository.findById(checklistId)
      .orElseThrow(() -> new ResourceNotFoundException(checklistId, ErrorCode.PROCESS_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    validateChecklistModificationState(checklist.getId(), checklist.getState());
    validateIfUserIsAuthorForPrototype(checklist.getId(), principalUser.getId());
    Set<Long> processParameterIds = parameterRepository.getParameterIdsByChecklistIdAndTargetEntityType(checklistId, Type.ParameterTargetEntityType.PROCESS);
    // All parameters to be unmapped now stored in processParameterIds
    processParameterIds.removeAll(mapJobParameterRequest.getMappedParameters().keySet());
    // Validating if processParameterIds can be unmapped
    for (Long processParameterId : processParameterIds) {
      parameterValidationService.validateIfParameterCanBeArchived(processParameterId, checklistId, true);
    }


    parameterRepository.updateParametersTargetEntityType(checklistId, Type.ParameterTargetEntityType.PROCESS, Type.ParameterTargetEntityType.UNMAPPED);

    var parameterIdsToMap = mapJobParameterRequest.getMappedParameters().keySet();
    var unmappedParametersCount = parameterRepository.getParametersCountByChecklistIdAndParameterIdInAndTargetEntityType(checklistId, parameterIdsToMap, Type.ParameterTargetEntityType.UNMAPPED);
    if (unmappedParametersCount != parameterIdsToMap.size()) {
      ValidationUtils.invalidate(checklistId, ErrorCode.ERROR_MAPPING_PARAMETER);
    }
    validateParameterVerification(parameterIdsToMap);

    parameterRepository.updateParametersTargetEntityType(parameterIdsToMap, Type.ParameterTargetEntityType.PROCESS);

    // TODO update batch
    for (Map.Entry<Long, Integer> parameterOrder : mapJobParameterRequest.getMappedParameters().entrySet()) {
      var parameterId = parameterOrder.getKey();
      var order = parameterOrder.getValue();
      parameterRepository.reorderParameter(parameterId, order, principalUser.getId(), DateTimeUtils.now());
    }

    return parameterMapper.toBasicDto(parameterRepository.getParametersByChecklistIdAndTargetEntityType(checklistId, Type.ParameterTargetEntityType.PROCESS));
  }

  @Override
  public BasicDto reconfigureJobLogColumns(Long checklistId) throws ResourceNotFoundException {
    Checklist checklist = checklistRepository.findById(checklistId)
      .orElseThrow(() -> new ResourceNotFoundException(checklistId, ErrorCode.PROCESS_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    List<JobLogColumn> jobLogColumns = jobLogService.getJobLogColumnForChecklist(checklist);
    JsonNode jsonNode = JsonUtils.valueToNode(jobLogColumns);
    checklist.setJobLogColumns(jsonNode);
    checklistRepository.save(checklist);
    customViewService.reConfigureCustomView(checklistId, jobLogColumns);
    var basicDto = new BasicDto();
    basicDto.setMessage("success");
    return basicDto;
  }

  @Override
  @Transactional
  public IChecklistElementDto copyChecklistElement(Long checklistId, CopyChecklistElementRequest copyChecklistRequest) throws ResourceNotFoundException, StreemException {
    log.info("[copyChecklistElement] Request to copy checklist entities, checklistId: {}, copyChecklistRequest: {}", checklistId, copyChecklistRequest);

    return elementCopyService.copyChecklistElements(checklistId, copyChecklistRequest);
  }

  @Override
  public Page<ChecklistView> getAllByResource(String objectTypeId, String objectId, Long useCaseId, boolean archived, String name, Pageable pageable) throws JsonProcessingException {
    log.info("[getAllByResource] Request to find all checklists by resource, objectTypeId: {}, useCaseId: {}, archived: {},  pageable: {}", objectTypeId, useCaseId, archived, pageable);

    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Long organisationId = principalUser.getOrganisationId();
    Long currentFacilityId = principalUser.getCurrentFacilityId();
    if (Utility.isEmpty(name)) {
      name = null;
    }

    List<Long> checklistIds = checklistRepository.findAllChecklistIdsForCurrentFacilityAndOrganisationByObjectTypeInData(currentFacilityId, organisationId, objectTypeId, useCaseId, name, archived);

    if (!Utility.isEmpty(checklistIds)) {
      List<Long> validChecklistIds = filterValidChecklists(objectTypeId, objectId, checklistIds);

      int start = (int) pageable.getOffset();
      int end = Math.min((start + pageable.getPageSize()), validChecklistIds.size());
      List<Long> paginatedChecklistIds = validChecklistIds.subList(start, end);

      List<ChecklistView> validChecklists = checklistRepository.getAllByIdsIn(paginatedChecklistIds);


      return new PageImpl<>(validChecklists, pageable, validChecklistIds.size());

    } else {
      return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }
  }

  @Override
  public BasicDto validateIfCurrentUserCanRecallChecklist(Long checklistId) throws ResourceNotFoundException, StreemException {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());

    Checklist checklist = checklistRepository.findById(checklistId).orElseThrow(() -> new ResourceNotFoundException(checklistId, ErrorCode.PROCESS_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    if (versionRepository.wasUserRestrictedFromRecallingOrRevisingChecklist(principalUserEntity.getId(), checklistId)) {
      ValidationUtils.invalidate(checklistId, ErrorCode.APPROVER_REVIEWER_CANNOT_RECALL);
    }
    validateChecklistRecallState(checklist.getState());
    var basicDto = new BasicDto();
    basicDto.setMessage("success");
    return basicDto;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ChecklistReviewDto recallChecklist(Long checklistId, RecallProcessDto recallProcessDto) throws StreemException, ResourceNotFoundException {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());

    Checklist checklist = checklistRepository.findById(checklistId)
      .orElseThrow(() -> new ResourceNotFoundException(checklistId, ErrorCode.PROCESS_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    validateIfReasonProvidedForRecallChecklist(checklistId, recallProcessDto.reason());

    checklistRepository.updateChecklistDuringRecall(checklist.getId(), principalUserEntity.getId());
    // delete all the non-primary authors
    checklistCollaboratorMappingRepository.deleteAllByChecklistIdAndTypeNot(checklistId, Type.Collaborator.PRIMARY_AUTHOR);
    // delete all the comments from the collaborator
    checklistCollaboratorCommentsRepository.deleteAllByChecklistId(checklistId);

    // update primary author
    checklistCollaboratorMappingRepository.updatePrimaryAuthor(principalUser.getId(), checklistId);

    //deleted autoinitialized parameter entry
    autoInitializedParameterRepository.deleteByChecklistId(checklist.getId());
    checklistAuditService.recall(checklistId, checklist.getCode(), recallProcessDto.reason(), principalUser);

    // get updated checklist.
    checklist = checklistRepository.findById(checklistId)
      .orElseThrow(() -> new ResourceNotFoundException(checklistId, ErrorCode.PROCESS_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    return checklistMapper.toChecklistReviewDto(checklist, checklistCollaboratorMappingRepository.findAllByChecklistId(checklistId));
  }

  @Override
  public JobLogsColumnDto getJobLogColumns(Long checklistId) throws ResourceNotFoundException, IOException {
    Checklist checklist = checklistRepository.findById(checklistId)
            .orElseThrow(() -> new ResourceNotFoundException(checklistId, ErrorCode.PROCESS_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    List<ChecklistJobLogColumnDto> jobLogColumnDtoList = JsonUtils.jsonToCollectionType(checklist.getJobLogColumns(), List.class, ChecklistJobLogColumnDto.class);
    Set<String> ignoredTriggeredType = Set.of("PARAMETER_SELF_VERIFIED_AT", "PARAMETER_PEER_VERIFIED_AT", "PARAMETER_PEER_STATUS");
    jobLogColumnDtoList = jobLogColumnDtoList.stream().filter(checklistJobLogColumnDto -> !ignoredTriggeredType.contains(checklistJobLogColumnDto.getTriggerType()))
            .toList();

    JobLogsColumnDto jobLogsColumnDto = new JobLogsColumnDto();
    jobLogsColumnDto.setId(String.valueOf(checklist.getId()));
    jobLogsColumnDto.setName(checklist.getName());
    jobLogsColumnDto.setJobLogColumns(jobLogColumnDtoList);

    return jobLogsColumnDto;
  }

  private void validateIfReasonProvidedForRecallChecklist(Long checklistId, String reason) throws StreemException {
    if (Utility.isEmpty(reason)) {
      ValidationUtils.invalidate(checklistId, ErrorCode.PROVIDE_REASON_FOR_RECALL);
    }
  }
  private void validateRolesForProcessRecall(Long userId, List<RoleDto> roles, Long currentFacilityId) throws StreemException {
    boolean allowedRoles;
    if (Objects.equals(currentFacilityId, ALL_FACILITY_ID) || Utility.isEmpty(currentFacilityId)) {
      allowedRoles = roles.stream().anyMatch(roleDto -> Misc.PROCESS_RECALL_ROLES_FOR_GLOBAL_FACILITY.contains(roleDto.getName()));
    } else {
      allowedRoles = roles.stream().anyMatch(roleDto -> Misc.PROCESS_RECALL_ROLES_FOR_FACILITY.contains(roleDto.getName()));
    }
    if (!allowedRoles) {
      ValidationUtils.invalidate(userId, ErrorCode.PROCESS_RECALL_ACCESS_DENIED);
    }
  }

  private void validateChecklistRecallState(State.Checklist state) throws StreemException {
    if (state == State.Checklist.BEING_BUILT || state == State.Checklist.PUBLISHED) {
      ValidationUtils.invalidate(state.toString(), ErrorCode.PROCESS_RECALL_INVALID_STATE);
    }
  }

  private List<Long> filterValidChecklists(String objectTypeId, String objectId, List<Long> checklistIds) throws JsonProcessingException {
    Optional<com.leucine.streem.collections.ObjectType> objectType = objectRepository.findById(objectTypeId);
    Optional<EntityObject> entityObject = entityObjectRepository.findById(objectType.get().getExternalId(), objectId);

    Map<String, List<String>> propertyIdAndChoicesIdMap = entityObject.get().getProperties().stream()
      .filter(pv -> !Utility.isEmpty(pv.getChoices()))
      .collect(Collectors.toMap(
        pv -> pv.getId().toString(),
        pv -> pv.getChoices().stream().map(po -> po.getId().toString()).collect(Collectors.toList())
      ));
    Map<String, String> propertyIdAndConstantsMap = entityObject.get().getProperties().stream()
      .filter(pv -> !Utility.isEmpty(pv.getValue()))
      .collect(Collectors.toMap(
        pv -> pv.getId().toString(),
        PropertyValue::getValue)
      );
    Map<String, List<String>> relationIdAndTargetIdMap = entityObject.get().getRelations().stream()
      .collect(Collectors.toMap(
        pv -> pv.getId().toString(),
        pv -> pv.getTargets().stream().map(po -> po.getId().toString()).collect(Collectors.toList())
      ));

    List<Long> validChecklists = new ArrayList<>();

    boolean isValidChecklist;
    List<ParameterView> allParameters = parameterRepository.getResourceParametersByObjectTypeIdAndChecklistId(objectTypeId, checklistIds);
    Map<Long, List<ParameterView>> checklistParameterMap = allParameters.stream().collect(Collectors.groupingBy(ParameterView::getChecklistId));
    for (Long checklistId : checklistIds) {
      isValidChecklist = true;
      //only those parameters of checklist will be considered which are mapped to given object type id
      List<ParameterView> checklistParameters = checklistParameterMap.getOrDefault(checklistId, new ArrayList<>());
      for (ParameterView parameter : checklistParameters) {
        ResourceParameter resourceParameter = JsonUtils.readValue(parameter.getData(), ResourceParameter.class);
        //we will consider checklist if any of its parameter has no validations and filters
        if (!Utility.isEmpty(resourceParameter) && (!Utility.isEmpty(resourceParameter.getPropertyValidations()) || !Utility.isEmpty(resourceParameter.getPropertyFilters()))) {

          // applicable process is only process
          if (!Utility.isEmpty(resourceParameter.getPropertyFilters())) {
            isValidChecklist = validatePropertyFilters(resourceParameter, propertyIdAndChoicesIdMap, relationIdAndTargetIdMap, propertyIdAndConstantsMap, isValidChecklist);
          }

        }
        if (isValidChecklist) {
          validChecklists.add(checklistId);
          break;
        }
      }
    }
    return validChecklists;
  }

  private static boolean validatePropertyFilters(ResourceParameter resourceParameter, Map<String, List<String>> propertyIdAndChoicesIdMap, Map<String, List<String>> relationIdAndTargetIdMap, Map<String, String> propertyIdAndConstantsMap, boolean isValidChecklist) {
    ResourceParameterFilter resourceParameterFilter = resourceParameter.getPropertyFilters();
    for (ResourceParameterFilterField resourceParameterFilterField : resourceParameterFilter.getFields()) {
      if (resourceParameterFilterField.getSelector().equals(PARAMETER)) {
        continue;
      }
      String fieldId = resourceParameterFilterField.getField().replace("searchable.", "");
      // This check is added because display name is present only for Single Select or Multi Select types
        switch (resourceParameterFilterField.getFieldType()) {
          case PROPERTY -> {
            if (propertyIdAndChoicesIdMap.containsKey(fieldId)) {
              boolean exists = propertyIdAndChoicesIdMap.get(fieldId).contains(String.valueOf(resourceParameterFilterField.getValues().get(0)));
              isValidChecklist = validateDropdownField(resourceParameterFilterField.getOp().toString(), exists);
            } else if (propertyIdAndConstantsMap.containsKey(fieldId)) {
              isValidChecklist = validateConstantField(resourceParameterFilterField.getOp().toString(), propertyIdAndConstantsMap.get(fieldId), resourceParameterFilterField.getValues().get(0).toString());
            }
            else  {
              isValidChecklist = false;
            }

          }
          case RELATION -> {
            if (!relationIdAndTargetIdMap.containsKey(fieldId)) {
              isValidChecklist = false;
            } else {
              boolean exists = relationIdAndTargetIdMap.get(fieldId).contains(String.valueOf(resourceParameterFilterField.getValues().get(0)));
              isValidChecklist = validateDropdownField(resourceParameterFilterField.getOp().toString(), exists);
            }
          }
        }

      if (!isValidChecklist) {
        break;
      }
    }
    return isValidChecklist;
  }

  private static boolean validateConstantField(String searchOperator, String entityObjectValue, String parameterValue) {
    ConstraintValidator validator = null;
    if (!Utility.isEmpty(parameterValue)) {
      switch (searchOperator) {
        case "EQ" -> {
          validator = new EqualValueValidator(parameterValue, null);
          validator.validate(entityObjectValue);
        }
        case "NE" -> {
          validator = new NotEqualValueValidator(parameterValue, null);
          validator.validate(entityObjectValue);
        }
        case "GT" -> {
          validator = new GreaterThanValidator(Double.parseDouble(parameterValue), null);
          validator.validate(entityObjectValue);
        }
        case "LT" -> {
          validator = new LessThanValidator(Double.parseDouble(parameterValue), null);
          validator.validate(entityObjectValue);
        }
        case "GTE", "GOE" -> {
          validator = new GreaterThanOrEqualValidator(Double.parseDouble(parameterValue), null);
          validator.validate(entityObjectValue);
        }
        case "LTE", "LOE" -> {
          validator = new LessThanOrEqualValidator(Double.parseDouble(parameterValue), null);
          validator.validate(entityObjectValue);
        }
      }

    }
    return !Utility.isEmpty(validator) && validator.isValid();
  }

  private static boolean validateDropdownField(String searchOperator, boolean exists) {
    switch (searchOperator) {
      case "EQ" -> {
        return exists;
      }
      case "NE" -> {
        return !exists;
      }
    }
    return false;
  }

  private boolean isUserMappedToTask(Map<Long, Set<Long>> taskUserMapping, Task task, User user) {
    var userIdSet = taskUserMapping.get(task.getId());
    if (Utility.isEmpty(userIdSet))
      return false;
    return userIdSet.contains(user.getId());
  }

  private void setProperties(Checklist checklist, List<PropertyRequest> propertyRequestList, User user, List<Error> errorList) throws StreemException {

    //Get only properties of checklist that are not archived
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Long currentFacilityId = principalUser.getCurrentFacilityId() == null ? ALL_FACILITY_ID : principalUser.getCurrentFacilityId();

    Map<Long, PropertyRequest> propertyValuesMap = propertyRequestList.stream().collect(Collectors.toMap(PropertyRequest::getId, Function.identity()));
    Map<Long, ChecklistPropertyValue> checklistProperties = checklist.getChecklistPropertyValues().stream()
      .collect(Collectors.toMap(p -> p.getFacilityUseCasePropertyMapping().getProperty().getId(), Function.identity()));
    var facilityUseCasePropertyMappings = propertyService.getPropertiesByFacilityIdAndUseCaseIdAndPropertyType(currentFacilityId, checklist.getUseCaseId(), Type.PropertyType.CHECKLIST);
    for (FacilityUseCasePropertyMapping facilityUseCasePropertyMapping : facilityUseCasePropertyMappings) {
      Property property = facilityUseCasePropertyMapping.getProperty();
      if (facilityUseCasePropertyMapping.isMandatory() && (!propertyValuesMap.containsKey(property.getId())
        || null == propertyValuesMap.get(property.getId())
        || Utility.isEmpty(propertyValuesMap.get(property.getId()).getValue()))) {
        ValidationUtils.addError(property.getId(), errorList, ErrorCode.MANDATORY_PROCESS_PROPERTY_NOT_SET);
      } else {
        if (propertyValuesMap.containsKey(property.getId())) {
          PropertyRequest propertyRequest = propertyValuesMap.get(property.getId());
          validatePropertyValueSize(propertyRequest, errorList);
          if (checklistProperties.containsKey(property.getId())) {
            ChecklistPropertyValue checklistPropertyValue = checklistProperties.get(property.getId());
            checklistPropertyValue.setModifiedBy(user);
            checklistPropertyValue.setValue(propertyRequest.getValue());
          } else {
            checklist.addProperty(facilityUseCasePropertyMapping, propertyRequest.getValue(), user);
          }
        } else {
          checklist.addProperty(facilityUseCasePropertyMapping, null, user);
        }
      }
    }
  }

  private void validatePropertyValueSize(PropertyRequest propertyRequest, List<Error> errorList) {
    if (!Utility.isEmpty(propertyRequest.getValue()) && propertyRequest.getValue().length() > 255) {
      ValidationUtils.addError(propertyRequest.getId(), errorList, ErrorCode.PROCESS_PROPERTY_VALUE_SIZE_EXCEEDED);
    }
  }

  private Stage createStage(User principalUserEntity, Checklist checklist) {
    Task task = createTask(principalUserEntity);
    Stage stage = new Stage();
    stage.setModifiedBy(principalUserEntity);
    stage.setCreatedBy(principalUserEntity);
    stage.setChecklist(checklist);
    stage.setName("");
    stage.getTasks().add(task);
    stage.setOrderTree(1);
    task.setStage(stage);
    return stage;
  }

  private Task createTask(User principalUserEntity) {
    Task task = new Task();
    task.setName("");
    task.setModifiedBy(principalUserEntity);
    task.setCreatedBy(principalUserEntity);
    task.setOrderTree(1);
    return task;
  }

  private void addPrimaryAuthor(Checklist checklist, User user) {
    checklist.addPrimaryAuthor(user, checklist.getReviewCycle(), user);
  }

  private void addAuthors(Checklist checklist, Set<Long> authors, User user) {
    for (Long authorId : authors) {
      checklist.addAuthor(userRepository.getOne(authorId), checklist.getReviewCycle(), user);
    }
  }

  private void validateTasks(Long checklistId, Stage stage, List<Error> errorList) throws IOException, StreemException {
    Set<Task> tasks = stage.getTasks();
    if (tasks.isEmpty()) {
      ValidationUtils.addError(stage.getId(), errorList, ErrorCode.PROCESS_EMPTY_TASK_VALIDATION);
    } else {
      for (Task task : tasks) {
        if (Utility.isEmpty(task.getName())) {
          ValidationUtils.addError(task.getId(), errorList, ErrorCode.TASK_NAME_CANNOT_BE_EMPTY);
        }
        if (!Utility.isEmpty(task.getAutomations())) {
          validateTaskAutomation(task.getId(), errorList, task.getAutomations());
        }
        if(task.isHasInterlocks()){
          interlockService.validateTaskInterlocks(task, errorList);
        }
        validateParameters(checklistId, task, errorList);
      }

    }
  }

  private void validateParameters(Long checklistId, Task task, List<Error> errorList) throws IOException, StreemException {
    boolean hasExecutableParameters = false;
    Map<Long, Parameter> calculationParametersMap = new HashMap<>();
    Map<Type.VerificationType, Long> VerificationTypeMap = new HashMap<>();
    boolean hasBulkVerify = task.isHasBulkVerification();
    long countParametersWithVerifications = 0L;
    for (Parameter parameter : task.getParameters()) {
      Type.Parameter parameterType = parameter.getType();
      if (!Type.NON_EXECUTABLE_PARAMETER_TYPES.contains(parameterType)) {
        hasExecutableParameters = true;
      }
      if (Utility.isEmpty(parameter.getLabel())) {
        ValidationUtils.addError(parameter.getId(), errorList, ErrorCode.PARAMETER_LABEL_CANNOT_BE_EMPTY);
      }
      if (parameter.isAutoInitialized() && Utility.isEmpty(parameter.getAutoInitialize())) {
        ValidationUtils.addError(parameter.getId(), errorList, ErrorCode.PARAMETER_AUTO_INITIALIZE_INVALID_DATE);
      }
      switch (parameterType) {
        case CALCULATION -> {
          calculationParametersMap.put(parameter.getId(), parameter);
          validateCalculationParameter(parameter, errorList);
        }
        case CHECKLIST -> validateChecklistParameter(parameter, errorList);
        case NUMBER -> validateNumberParameter(parameter, errorList);
        case INSTRUCTION -> validateInstructionParameter(parameter, errorList);
        case MATERIAL -> validateMaterialParameter(parameter, errorList);
        case MULTISELECT -> validateMultiSelectParameter(parameter, errorList);
        case SHOULD_BE -> validateShouldBeParameter(parameter, errorList);
        case SINGLE_SELECT -> validateSingleSelectParameter(parameter, errorList);
        case YES_NO -> validateYesNoParameter(parameter, errorList);
      }
      if (Type.VERIFICATION_TYPES.contains(parameter.getVerificationType())) {
        VerificationTypeMap.put(parameter.getVerificationType(),
          VerificationTypeMap.getOrDefault(parameter.getVerificationType(), 0L) + 1);
      }
    }
    if (!hasExecutableParameters) {
      ValidationUtils.addError(task.getId(), errorList, ErrorCode.TASK_SHOULD_HAVE_ATLEAST_ONE_EXECUTABLE_PARAMETER);
    }
    checkForCyclicDependencyOnCalculationParameter(calculationParametersMap, checklistId);

    long bothCount = VerificationTypeMap.getOrDefault(Type.VerificationType.BOTH, 0L);
    long selfCount = VerificationTypeMap.getOrDefault(Type.VerificationType.SELF, 0L);
    long peerCount = VerificationTypeMap.getOrDefault(Type.VerificationType.PEER, 0L);

    boolean validVerification = (selfCount >= 2 || peerCount >= 2 || bothCount >= 2) || ((selfCount == 1 || peerCount == 1) && bothCount > 0);

    if (hasBulkVerify && !validVerification) {
      ValidationUtils.addError(task.getId(), errorList, ErrorCode.TASK_BULK_VERIFY_SHOULD_HAVE_VERIFICATION);
    }
  }

  private void validateTaskAutomation(Long taskId, List<Error> errorList, Set<TaskAutomationMapping> taskAutomationMappings) {
    List<Long> referencedParameterIds = new ArrayList<>();
    for (TaskAutomationMapping taskAutomationMapping : taskAutomationMappings) {
      JsonNode actionDetails = taskAutomationMapping.getAutomation().getActionDetails();
      if (!Utility.isEmpty(actionDetails.get("referencedParameterId"))) {
        referencedParameterIds.add(Long.valueOf(actionDetails.asText()));
      }
      if (!Type.ACTION_TYPES_WITH_NO_REFERENCED_PARAMETER_ID.contains(taskAutomationMapping.getAutomation().getActionType()) && actionDetails.get("selector").toString().equals(Type.SelectorType.PARAMETER.name())) {
        referencedParameterIds.add(Long.valueOf(actionDetails.get("parameterId").asText()));
      }
    }

    List<Parameter> archivedParameters = parameterRepository.getArchivedParametersByReferencedParameterIds(referencedParameterIds);
    if (!Utility.isEmpty(archivedParameters)) {
      ValidationUtils.addError(taskId, errorList, ErrorCode.TASK_AUTOMATION_INVALID_MAPPED_PARAMETERS);
    }
  }

  private void validateMaterialParameter(Parameter parameter, List<Error> errorList) throws IOException {
    List<MaterialParameter> materialParameters = JsonUtils.jsonToCollectionType(parameter.getData().toString(), List.class, MaterialParameter.class);

    if (parameter.isMandatory()) {
      ValidationUtils.addError(parameter.getId(), errorList, ErrorCode.MATERIAL_PARAMETER_CANNOT_BE_MANDATORY);
    }

    if (Utility.isEmpty(materialParameters)) {
      ValidationUtils.addError(parameter.getId(), errorList, ErrorCode.MATERIAL_PARAMETER_LIST_CANNOT_BE_EMPTY);
    } else {
      for (MaterialParameter materialParameter : materialParameters) {
        if (Utility.isEmpty(materialParameter.getName())) {
          ValidationUtils.addError(parameter.getId(), errorList, ErrorCode.MATERIAL_PARAMETER_NAME_CANNOT_BE_EMPTY);
        }
      }
    }
  }

  private void validateYesNoParameter(Parameter parameter, List<Error> errorList) throws IOException {
    List<YesNoParameter> yesNoParameters = JsonUtils.jsonToCollectionType(parameter.getData().toString(), List.class, YesNoParameter.class);

    if (Utility.isEmpty(yesNoParameters) || yesNoParameters.size() != 2) {
      ValidationUtils.addError(parameter.getId(), errorList, ErrorCode.YES_NO_PARAMETER_SHOULD_HAVE_EXACTLY_TWO_OPTIONS);
    } else {
      for (YesNoParameter yesNoParameter : yesNoParameters) {
        if (Utility.isEmpty(yesNoParameter.getName())) {
          ValidationUtils.addError(parameter.getId(), errorList, ErrorCode.YES_NO_PARAMETER_OPTIONS_NAME_CANNOT_BE_EMPTY);
        }
      }
      if (Utility.isEmpty(parameter.getLabel())) {
        ValidationUtils.addError(parameter.getId(), errorList, ErrorCode.YES_NO_PARAMETER_TITLE_CANNOT_BE_EMPTY);
      }
    }
  }

  private void validateMultiSelectParameter(Parameter parameter, List<Error> errorList) throws IOException {
    List<MultiSelectParameter> multiSelectParameters = JsonUtils.jsonToCollectionType(parameter.getData().toString(), List.class, MultiSelectParameter.class);
    if (Utility.isEmpty(multiSelectParameters)) {
      ValidationUtils.addError(parameter.getId(), errorList, ErrorCode.MULTISELECT_PARAMETER_OPTIONS_CANNOT_BE_EMPTY);
    } else {
      for (MultiSelectParameter multiSelectParameter : multiSelectParameters) {
        if (Utility.isEmpty(multiSelectParameter.getName())) {
          ValidationUtils.addError(parameter.getId(), errorList, ErrorCode.MULTISELECT_PARAMETER_OPTIONS_NAME_CANNOT_BE_EMPTY);
        }
      }
    }
  }

  private void validateSingleSelectParameter(Parameter parameter, List<Error> errorList) throws IOException {
    List<SingleSelectParameter> singleSelectParameters = JsonUtils.jsonToCollectionType(parameter.getData().toString(), List.class, SingleSelectParameter.class);
    if (Utility.isEmpty(singleSelectParameters)) {
      ValidationUtils.addError(parameter.getId(), errorList, ErrorCode.SINGLE_SELECT_PARAMETER_OPTIONS_CANNOT_BE_EMPTY);
    } else {
      for (SingleSelectParameter singleSelectParameter : singleSelectParameters) {
        if (Utility.isEmpty(singleSelectParameter.getName())) {
          ValidationUtils.addError(parameter.getId(), errorList, ErrorCode.SINGLE_SELECT_PARAMETER_OPTIONS_NAME_CANNOT_BE_EMPTY);
        }
      }
    }
  }

  private void validateChecklistParameter(Parameter parameter, List<Error> errorList) throws IOException {
    List<ChecklistParameter> checklistParameters = JsonUtils.jsonToCollectionType(parameter.getData().toString(), List.class, ChecklistParameter.class);
    if (Utility.isEmpty(checklistParameters)) {
      ValidationUtils.addError(parameter.getId(), errorList, ErrorCode.PROCESS_PARAMETER_OPTIONS_CANNOT_BE_EMPTY);
    } else {
      for (ChecklistParameter checklistParameter : checklistParameters) {
        if (Utility.isEmpty(checklistParameter.getName())) {
          ValidationUtils.addError(parameter.getId(), errorList, ErrorCode.PROCESS_PARAMETER_OPTIONS_NAME_CANNOT_BE_EMPTY);
        }
      }
    }
  }

  private void validateShouldBeParameter(Parameter parameter, List<Error> errorList) throws JsonProcessingException {
    ShouldBeParameter shouldBeParameter = JsonUtils.readValue(parameter.getData().toString(), ShouldBeParameter.class);
    if (Utility.isEmpty(shouldBeParameter.getOperator())) {
      ValidationUtils.addError(parameter.getId(), errorList, ErrorCode.SHOULD_BE_PARAMETER_OPERATOR_CANNOT_BE_EMPTY);
    } else {
      Operator.Parameter operator = Operator.Parameter.valueOf(shouldBeParameter.getOperator());
      switch (operator) {
        case BETWEEN:
          //TODO possibly have different error codes for lower and upper value
          if (Utility.isEmpty(shouldBeParameter.getLowerValue()) || !Utility.isNumeric(shouldBeParameter.getLowerValue())) {
            ValidationUtils.addError(parameter.getId(), errorList, ErrorCode.SHOULD_BE_PARAMETER_VALUE_INVALID);
          } else if (Utility.isEmpty(shouldBeParameter.getUpperValue()) || !Utility.isNumeric(shouldBeParameter.getUpperValue())) {
            ValidationUtils.addError(parameter.getId(), errorList, ErrorCode.SHOULD_BE_PARAMETER_VALUE_INVALID);
          } else {
            double lowerValue = Double.parseDouble(shouldBeParameter.getLowerValue());
            double upperValue = Double.parseDouble(shouldBeParameter.getUpperValue());

            if (lowerValue > upperValue) {
              ValidationUtils.addError(parameter.getId(), errorList, ErrorCode.SHOULD_BE_PARAMETER_LOWER_VALUE_CANNOT_BE_MORE_THAN_UPPER_VALUE);
            }
          }
          break;
        case EQUAL_TO:
        case LESS_THAN:
        case LESS_THAN_EQUAL_TO:
        case MORE_THAN:
        case MORE_THAN_EQUAL_TO:
          if (Utility.isEmpty(shouldBeParameter.getValue()) || !Utility.isNumeric(shouldBeParameter.getValue())) {
            ValidationUtils.addError(parameter.getId(), errorList, ErrorCode.SHOULD_BE_PARAMETER_VALUE_INVALID);
          }
      }
    }
  }

  private void validateInstructionParameter(Parameter parameter, List<Error> errorList) throws JsonProcessingException {
    InstructionParameter instructionParameter = JsonUtils.readValue(parameter.getData().toString(), InstructionParameter.class);
    if (parameter.isMandatory()) {
      ValidationUtils.addError(parameter.getId(), errorList, ErrorCode.INSTRUCTION_PARAMETER_CANNOT_BE_MANDATORY);
    }
    if (Utility.isEmpty(instructionParameter.getText())) {
      ValidationUtils.addError(parameter.getId(), errorList, ErrorCode.INSTRUCTION_PARAMETER_TEXT_CANNOT_BE_EMPTY);
    }
  }

  private void validateNumberParameter(Parameter parameter, List<Error> errorList) throws IOException {
    List<ParameterValidationDto> parameterValidationDtoList = JsonUtils.jsonToCollectionType(parameter.getValidations(), List.class, ParameterValidationDto.class);

    Set<Long> parameterIds = parameterValidationDtoList.stream()
      .filter(dto -> !Utility.isEmpty(dto.getResourceParameterValidations()))
      .flatMap(dto -> dto.getResourceParameterValidations().stream())
      .map(resourceDto -> Long.valueOf(resourceDto.getParameterId()))
      .collect(Collectors.toSet());

    if (!parameterIds.isEmpty()) {
      int count = parameterRepository.getEnabledParametersCountByTypeAndIdIn(parameterIds, Type.ALLOWED_PARAMETER_TYPES_NUMBER_PARAMETER_VALIDATION);

      if (count != parameterIds.size()) {
        ValidationUtils.addError(parameter.getId(), errorList, ErrorCode.PARAMETER_VALIDATIONS_INCONSISTENT_DATA);
      }
    }
  }

  private void validateIfChecklistCanBeArchived(Long checklistId) throws StreemException {
    // If the checklist contains active schedulers or active jobs which are not completed
    // it cannot be archived

    if (schedulerRepository.findByChecklistIdWhereSchedulerIsActive(checklistId)) {
      ValidationUtils.invalidate(checklistId, ErrorCode.CANNOT_ARCHIVE_PROCESS_WITH_ACTIVE_SCHEDULERS);
    }
    if (jobRepository.findByChecklistIdWhereStateNotIn(checklistId, State.JOB_COMPLETED_STATES)) {
      ValidationUtils.invalidate(checklistId, ErrorCode.CANNOT_ARCHIVE_PROCESS_WITH_ACTIVE_JOBS);
    }
  }

  void checkForCyclicDependencyOnCalculationParameter(Map<Long, Parameter> calculationParametersMap, Long checklistId) throws JsonProcessingException, StreemException {
    // map to create incremental vertex number for calculation parameters
    Map<Long, Integer> parameterIdVertexNumberMap = new HashMap<>();
    List<List<Integer>> directedGraph = new ArrayList<>();

    for (Parameter parameter : calculationParametersMap.values()) {
      CalculationParameter calculationParameter = JsonUtils.readValue(parameter.getData().toString(), CalculationParameter.class);
      Set<Long> parameterIds = calculationParameter.getVariables().values().stream().map(cav -> Long.valueOf(cav.getParameterId()))
        .collect(Collectors.toSet());
      int vertexIndex;
      // same calculation parameter can be in the graph many times
      // so if exists in the map already use existing vertex number
      // or else get the next incremental vertex
      if (parameterIdVertexNumberMap.containsKey(parameter.getId())) {
        vertexIndex = parameterIdVertexNumberMap.get(parameter.getId());
      } else {
        directedGraph.add(new ArrayList<>());
        vertexIndex = directedGraph.size() - 1; // -1 because the vertex needs to starts from 0
        parameterIdVertexNumberMap.put(parameter.getId(), vertexIndex);
      }

      for (Long parameterId : parameterIds) {
        // calculation parameters can have other dependent parameters, eg: number, parameter
        // exclude such parameters since we only need to detect cycle for calculation parameters
        if (calculationParametersMap.containsKey(parameterId)) {
          if (parameterIdVertexNumberMap.containsKey(parameterId)) {
            directedGraph.get(vertexIndex).add(parameterIdVertexNumberMap.get(parameterId));
          } else {
            directedGraph.add(new ArrayList<>());
            int variableParameterVertexIndex = directedGraph.size() - 1;
            parameterIdVertexNumberMap.put(parameterId, variableParameterVertexIndex);
            directedGraph.get(vertexIndex).add(variableParameterVertexIndex);
          }
        }
      }
    }

    boolean hasCycle = CycleDetectionUtil.isCyclic(directedGraph, directedGraph.size());
    if (hasCycle) {
      ValidationUtils.invalidate(checklistId, ErrorCode.DETECTED_A_CYCLE_IN_CALCULATION_PARAMETER);
    }
  }

  // TODO: optimize
  private void validateCalculationParameter(Parameter parameter, List<Error> errorList) throws JsonProcessingException {
    CalculationParameter calculationParameter = JsonUtils.readValue(parameter.getData().toString(), CalculationParameter.class);
    List<Long> calculationParameterVariableIds = calculationParameter.getVariables().values().stream()
      .map(cav -> Long.valueOf(cav.getParameterId())).toList();
    boolean isParameterArchived = parameterRepository.getArchivedParametersByReferencedParameterIds(calculationParameterVariableIds).size() > 0;
    if (isParameterArchived) {
      ValidationUtils.addError(parameter.getId(), errorList, ErrorCode.CALCULATION_PARAMETER_VARIABLE_CONTAINS_ARCHIVED_PARAMETER);
    }
  }

  private void validateParameterVerification(Set<Long> parameterIdsToMap) throws StreemException {
    List<Parameter> parameters = parameterRepository.findAllById(parameterIdsToMap);
    if (!Utility.isEmpty(parameters)) {
      for (Parameter parameter : parameters) {
        if (!parameter.getVerificationType().equals(Type.VerificationType.NONE)) {
          ValidationUtils.invalidate(parameter.getId(), ErrorCode.PARAMETER_CANNOT_BE_ASSIGNED_FOR_CREATE_JOB_FORM);
        }
      }
    }
  }

  private void validateIfParameterBeingCJFMappedToTask(List<Parameter> parameterList, List<Error> errorList) throws IOException {
    for (Parameter processParameter : parameterList) {
      if (Objects.requireNonNull(processParameter.getType()) == Type.Parameter.CALCULATION) {
        CalculationParameter calculationParameter = JsonUtils.readValue(processParameter.getData().toString(), CalculationParameter.class);
        Set<Long> parameterIds = calculationParameter.getVariables().values().stream()
          .map(cav -> Long.valueOf(cav.getParameterId())).collect(Collectors.toSet());
        List<Parameter> parameters = parameterRepository.findAllById(parameterIds).stream()
          .filter(parameter -> parameter.getTargetEntityType() == Type.ParameterTargetEntityType.TASK)
          .toList();
        if (!Utility.isEmpty(parameters)) {
          ValidationUtils.addError(processParameter.getId(), errorList, ErrorCode.CJF_PARAMETER_CANNOT_BE_AUTOINITIALIAZED_BY_TASK_PARAMETER);
        }
      } else {
        if (processParameter.isAutoInitialized()) {
          AutoInitializeDto autoInitializeDto = JsonUtils.readValue(processParameter.getAutoInitialize().toString(), AutoInitializeDto.class);
          Parameter autoInitializingParameter = parameterRepository.getReferenceById(Long.valueOf(autoInitializeDto.getParameterId()));
          if (processParameter.getTargetEntityType() == Type.ParameterTargetEntityType.PROCESS && autoInitializingParameter.getTargetEntityType() == Type.ParameterTargetEntityType.TASK) {
            ValidationUtils.addError(processParameter.getId(), errorList, ErrorCode.CJF_PARAMETER_CANNOT_BE_AUTOINITIALIAZED_BY_TASK_PARAMETER);
          }
        }

        if (Objects.requireNonNull(processParameter.getType()) == Type.Parameter.NUMBER) {
          NumberParameter numberParameter = JsonUtils.readValue(processParameter.getData().toString(), NumberParameter.class);
          List<ParameterValidationDto> parameterValidationDtoList = JsonUtils.jsonToCollectionType(
            processParameter.getValidations().toString(), List.class, ParameterValidationDto.class);

          LeastCount leastCount = numberParameter.getLeastCount();

          // Handling least count validation
          if (!Utility.isEmpty(leastCount) && leastCount.getSelector().equals(Type.SelectorType.PARAMETER)) {
            Parameter leastCountReferencedParameter = parameterRepository.getReferenceById(Long.valueOf(leastCount.getReferencedParameterId()));
            if (processParameter.getTargetEntityType() == Type.ParameterTargetEntityType.PROCESS
              && leastCountReferencedParameter.getTargetEntityType() == Type.ParameterTargetEntityType.TASK) {
              ValidationUtils.addError(processParameter.getId(), errorList, ErrorCode.LEAST_COUNT_CJF_PARAMETER_CANNOT_BE_LINKED_BY_TASK_PARAMETER);
            }
          }

          for (ParameterValidationDto parameterValidationDto : parameterValidationDtoList) {
            List<CriteriaValidationDto> criteriaValidationDtoList = parameterValidationDto.getCriteriaValidations();
            List<ResourceParameterPropertyValidationDto> resourceParameterPropertyValidationDtoList = parameterValidationDto.getResourceParameterValidations();

            // Handling criteria validations
            if (!Utility.isEmpty(criteriaValidationDtoList)) {
              for (CriteriaValidationDto criteriaValidationDto : criteriaValidationDtoList) {
                if (criteriaValidationDto.getCriteriaType() == Type.SelectorType.PARAMETER) {
                  List<Long> criteriaValidationReferencedParameterIds = Stream.of(
                      criteriaValidationDto.getValueParameterId(),
                      criteriaValidationDto.getLowerValueParameterId(),
                      criteriaValidationDto.getUpperValueParameterId()
                    )
                    .filter(Objects::nonNull)
                    .map(Long::valueOf)
                    .collect(Collectors.toList());

                  List<Parameter> criteriaValidationReferencedParameterList = parameterRepository.findAllById(criteriaValidationReferencedParameterIds);
                  for (Parameter criteriaValidationParameter : criteriaValidationReferencedParameterList) {
                    if (processParameter.getTargetEntityType() == Type.ParameterTargetEntityType.PROCESS
                      && criteriaValidationParameter.getTargetEntityType() == Type.ParameterTargetEntityType.TASK) {
                      ValidationUtils.addError(criteriaValidationParameter.getId(), errorList, ErrorCode.CRITERIA_VALIDATION_CJF_PARAMETER_CANNOT_BE_LINKED_BY_TASK_PARAMETER);
                    }
                  }
                }
              }
            }

            // Handling resource parameter property validations
            if (!Utility.isEmpty(resourceParameterPropertyValidationDtoList)) {
              for (ResourceParameterPropertyValidationDto resourceParameterPropertyValidationDto : resourceParameterPropertyValidationDtoList) {
                List<Long> resourceValidationReferencedParameterIds = Stream.of(
                    resourceParameterPropertyValidationDto.getParameterId()
                  )
                  .filter(Objects::nonNull)
                  .map(Long::valueOf)
                  .collect(Collectors.toList());

                List<Parameter> resourceValidationReferencedParameterList = parameterRepository.findAllById(resourceValidationReferencedParameterIds);
                for (Parameter resourceValidationParameter : resourceValidationReferencedParameterList) {
                  if (processParameter.getTargetEntityType() == Type.ParameterTargetEntityType.PROCESS
                    && resourceValidationParameter.getTargetEntityType() == Type.ParameterTargetEntityType.TASK) {
                    ValidationUtils.addError(resourceValidationParameter.getId(), errorList, ErrorCode.RESOURCE_VALIDATION_CJF_PARAMETER_CANNOT_BE_LINKED_BY_TASK_PARAMETER);
                  }
                }
              }
            }
          }
        }

        if (!Utility.isEmpty(processParameter.getType()) && (processParameter.getType() == Type.Parameter.DATE || processParameter.getType() == Type.Parameter.DATE_TIME)) {

          List<ParameterValidationDto> parameterValidationDtoList = JsonUtils.jsonToCollectionType(
            processParameter.getValidations().toString(), List.class, ParameterValidationDto.class);

          for (ParameterValidationDto parameterValidationDto : parameterValidationDtoList) {
            List<DateParameterValidationDto> dateParameterValidationDtoList = parameterValidationDto.getDateTimeParameterValidations();
            if (!Utility.isEmpty(dateParameterValidationDtoList)) {
              for (DateParameterValidationDto dateParameterValidationDto : dateParameterValidationDtoList) {
                if (dateParameterValidationDto.getSelector() == Type.SelectorType.PARAMETER) {
                  List<Long> dateAndDateTimeValidationReferencedParameterIds = Stream.of(
                      dateParameterValidationDto.getReferencedParameterId()
                    )
                    .filter(Objects::nonNull)
                    .map(Long::valueOf)
                    .collect(Collectors.toList());

                  List<Parameter> dateAndDateTimeValidationReferencedParameterList = parameterRepository.findAllById(dateAndDateTimeValidationReferencedParameterIds);
                  for (Parameter dateAndDateTimeValidtionParameter : dateAndDateTimeValidationReferencedParameterList) {
                    if (processParameter.getTargetEntityType() == Type.ParameterTargetEntityType.PROCESS
                      && dateAndDateTimeValidtionParameter.getTargetEntityType() == Type.ParameterTargetEntityType.TASK) {
                      ValidationUtils.addError(dateAndDateTimeValidtionParameter.getId(), errorList, ErrorCode.DATE_PARAMETER_VALIDATION_CJF_PARAMETER_CANNOT_BE_LINKED_BY_TASK_PARAMETER);
                    }
                  }
                }
              }
            }

          }
        }
        if (Objects.requireNonNull(processParameter.getType()) == Type.Parameter.RESOURCE) {
          List<ParameterValidationDto> parameterValidationDtoList = JsonUtils.jsonToCollectionType(
            processParameter.getValidations().toString(), List.class, ParameterValidationDto.class);



          for (ParameterValidationDto parameterValidationDto : parameterValidationDtoList) {

            List<ParameterRelationPropertyValidationDto> resourcePropertyValidationList = parameterValidationDto.getPropertyValidations();
            if (!Utility.isEmpty(resourcePropertyValidationList)) {
              for (ParameterRelationPropertyValidationDto resourcePropertyValidation : resourcePropertyValidationList) {
                if (resourcePropertyValidation.getSelector() == Type.SelectorType.PARAMETER) {
                  List<Long> resourcePropertyValidationReferencedParameterIds = Stream.of(
                      resourcePropertyValidation.getReferencedParameterId()
                    )
                    .filter(Objects::nonNull)
                    .map(Long::valueOf)
                    .collect(Collectors.toList());

                  List<Parameter> criteriaValidationReferencedParameterList = parameterRepository.findAllById(resourcePropertyValidationReferencedParameterIds);
                  for (Parameter criteriaValidationParameter : criteriaValidationReferencedParameterList) {
                    if (processParameter.getTargetEntityType() == Type.ParameterTargetEntityType.PROCESS
                      && criteriaValidationParameter.getTargetEntityType() == Type.ParameterTargetEntityType.TASK) {
                      ValidationUtils.addError(criteriaValidationParameter.getId(), errorList, ErrorCode.RESOURCE_PROPERTY_VALIDATION_CJF_PARAMETER_CANNOT_BE_LINKED_BY_TASK_PARAMETER);
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private boolean isUserGroupMappedToTask(Map<Long, Set<Long>> taskUserGroupMapping, Task task, UserGroup userGroup) {
    return taskUserGroupMapping.containsKey(task.getId()) && taskUserGroupMapping.get(task.getId()).contains(userGroup.getId());
  }

  private void checkForCyclicTaskDependencies(Checklist checklist) throws StreemException {
    Map<Long, Set<Long>> taskPrerequisiteMap = new HashMap<>();
    for (Stage stage : checklist.getStages()) {
      for (Task task : stage.getTasks()) {
        Set<Long> prerequisiteTaskIds = task.getPrerequisiteTasks().stream().map(taskDependency -> taskDependency.getPrerequisiteTask().getId()).collect(Collectors.toSet());
        taskPrerequisiteMap.put(task.getId(), prerequisiteTaskIds);
      }
    }
    validateCyclicDependents(taskPrerequisiteMap);
  }

  private void validateCyclicDependents(Map<Long, Set<Long>> taskPrerequisiteMap) throws StreemException {
    for (Map.Entry<Long, Set<Long>> entry : taskPrerequisiteMap.entrySet()) {
      Set<Long> visited = new HashSet<>();
      Set<Long> currentPath = new HashSet<>();
      if (isCyclic(entry.getKey(), taskPrerequisiteMap, visited, currentPath)) {
        ValidationUtils.invalidate(entry.getKey(), ErrorCode.CYCLIC_TASK_DEPENDENCY);
      }
    }
  }

  private boolean isCyclic(Long key, Map<Long, Set<Long>> taskPrerequisiteMap, Set<Long> visited, Set<Long> currentPath) {
    if (currentPath.contains(key)) {
      return true;
    }
    if (visited.contains(key)) {
      return false;
    }
    visited.add(key);
    currentPath.add(key);
    for (Long task : taskPrerequisiteMap.get(key)) {
      if (isCyclic(task, taskPrerequisiteMap, visited, currentPath)) {
        return true;
      }
    }
    currentPath.remove(key);
    return false;
  }

  @Override
  public byte[] generateProcessTemplatePdf(Long checklistId) throws ResourceNotFoundException, IOException {
    log.info("[generateProcessTemplatePdf] Request to generate process template PDF, checklistId: {}", checklistId);

    // Get checklist data
    ChecklistDto checklistDto = getChecklistById(checklistId);

    // Prepare PDF generation data
    GeneratedPdfDataDto pdfData = new GeneratedPdfDataDto();
    pdfData.setChecklistDto(checklistDto);
    pdfData.setGeneratedOn(DateTimeUtils.now());
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User currentUser = userRepository.getReferenceById(principalUser.getId());
    pdfData.setUserFullName(Utility.getFullNameAndEmployeeId(currentUser.getFirstName(), currentUser.getLastName(), currentUser.getEmployeeId()));
    pdfData.setUserId(currentUser.getEmployeeId());

    // Set facility information
    if (principalUser.getCurrentFacilityId() != null) {
      Facility facility = facilityRepository.getReferenceById(principalUser.getCurrentFacilityId());
      pdfData.setFacility(facilityMapper.toDto(facility));
    }

    // Generate PDF using PdfGeneratorUtil
    byte[] pdf = pdfGeneratorUtil.generatePdf(Type.PdfType.PROCESS_TEMPLATE, pdfData);

    // Audit the download/generation
    checklistAuditService.downloadProcessTemplatePdf(checklistId, checklistDto.getName(), checklistDto.getCode(), principalUser);

    return pdf;
  }
}
