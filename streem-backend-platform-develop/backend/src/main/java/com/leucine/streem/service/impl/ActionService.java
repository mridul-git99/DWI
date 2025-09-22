package com.leucine.streem.service.impl;

import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.ActionDto;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.EffectDto;
import com.leucine.streem.dto.mapper.IActionMapper;
import com.leucine.streem.dto.mapper.IEffectMapper;
import com.leucine.streem.dto.request.CreateActionRequest;
import com.leucine.streem.model.Action;
import com.leucine.streem.model.ActionFacilityMapping;
import com.leucine.streem.model.Facility;
import com.leucine.streem.model.User;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.repository.*;
import com.leucine.streem.service.IActionService;
import com.leucine.streem.service.ICodeService;
import com.leucine.streem.util.DateTimeUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;


@Service
@AllArgsConstructor
@Slf4j
public class ActionService implements IActionService {
  private final ICodeService codeService;
  private final IActionRepository actionRepository;
  private final IActionMapper actionMapper;
  private final IEffectRepository effectRepository;
  private final IEffectMapper effectMapper;
  private final IUserRepository userRepository;
  private final IFacilityRepository facilityRepository;
  private final IChecklistRepository checklistRepository;
  private final IActionFacilityRepository actionFacilityRepository;

  @Override
  public BasicDto archiveAction(Long actionId) {
    log.info("[archiveAction] archiving action: {}", actionId);
    Action action = actionRepository.findById(actionId)
      .orElseThrow(() -> new RuntimeException("Action not found"));
    action.setArchived(true);
    actionRepository.save(action);
    BasicDto basicDto = new BasicDto();
    basicDto.setMessage("Action archived successfully");
    return basicDto;
  }

  @Override
  public ActionDto updateAction(Long actionId, ActionDto actionDto) {
    log.info("[updateAction] updating action: {}", actionDto);

    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());

    Action action = actionRepository.findById(actionId)
      .orElseThrow(() -> new RuntimeException("Action not found"));
    action.setName(actionDto.getName());
    action.setDescription(actionDto.getDescription());
    action.setSuccessMessage(actionDto.getSuccessMessage());
    action.setFailureMessage(actionDto.getFailureMessage());
    action.setTriggerEntityId(Long.parseLong(actionDto.getTriggerEntityId()));
    action.setTriggerType(actionDto.getTriggerType());
    action.setModifiedAt(DateTimeUtils.now());
    action.setModifiedBy(principalUserEntity);

    actionRepository.save(action);
    return actionMapper.toDto(action);
  }

  @Override
  public Page<ActionDto> getActions(Long checklistId, Pageable pageable) {
    Page<Action> actions = actionRepository.findByChecklistIdAndArchived(checklistId, false, pageable);
    List<ActionDto> actionDtoList = actionMapper.toDto(actions.getContent().stream().filter(action -> !action.isArchived()).toList());
    List<EffectDto> effectDtoList = effectMapper.toDto(effectRepository.findAllByActionsIdInOrderByOrderTree(
      actionDtoList.stream()
        .map(actionDto -> Long.parseLong(actionDto.getId())).toList()
    ).stream().filter(effect -> !effect.isArchived()).toList());

    Map<Long, List<EffectDto>> actionIdEffectsMap = effectDtoList.stream()
      .collect(groupingBy(effectDto -> Long.valueOf(effectDto.getActionsId())));

    actionDtoList.forEach(actionDto -> {
      actionDto.setEffects(actionIdEffectsMap.get(Long.parseLong(actionDto.getId())));
    });


    return new PageImpl<>(actionDtoList, pageable, actions.getTotalElements());
  }

  @Override
  @Transactional(rollbackOn = Exception.class)
  public BasicDto createAction(CreateActionRequest createActionRequest) {
    log.info("[createAction] creating action: {}", createActionRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());
    Facility facility = facilityRepository.getReferenceById(principalUser.getCurrentFacilityId());
    Action action = new Action();
    action.setName(createActionRequest.getName());
    action.setDescription(createActionRequest.getDescription());
    action.setCode(codeService.getCode(Type.EntityType.ACTION, principalUser.getOrganisationId()));
    action.setSuccessMessage(createActionRequest.getSuccessMessage());
    action.setFailureMessage(createActionRequest.getFailureMessage());
    action.setTriggerEntityId(createActionRequest.getTriggerEntityId());
    action.setTriggerType(createActionRequest.getTriggerType());
    action.setCreatedBy(principalUserEntity);
    action.setModifiedBy(principalUserEntity);
    action.setCreatedAt(DateTimeUtils.now());
    action.setModifiedAt(DateTimeUtils.now());
    action.setChecklist(checklistRepository.getReferenceById(createActionRequest.getChecklistId()));


    Action savedAction = actionRepository.save(action);

    ActionFacilityMapping actionFacilityMapping = new ActionFacilityMapping(savedAction, facility);
    actionFacilityMapping.setCreatedBy(principalUserEntity);
    actionFacilityMapping.setModifiedBy(principalUserEntity);
    actionFacilityMapping.setCreatedAt(DateTimeUtils.now());
    actionFacilityMapping.setModifiedAt(DateTimeUtils.now());
    actionFacilityRepository.save(actionFacilityMapping);

    BasicDto basicDto = new BasicDto();
    basicDto.setId(savedAction.getId().toString());
    basicDto.setMessage("Action created successfully");
    return basicDto;
  }
}
