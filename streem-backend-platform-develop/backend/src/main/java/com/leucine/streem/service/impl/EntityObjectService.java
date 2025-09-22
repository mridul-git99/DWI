package com.leucine.streem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.collections.*;
import com.leucine.streem.collections.partial.PartialEntityObject;
import com.leucine.streem.collections.partial.PartialObjectType;
import com.leucine.streem.constant.CollectionKey;
import com.leucine.streem.constant.CollectionMisc;
import com.leucine.streem.constant.UsageStatus;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.ImportResult;
import com.leucine.streem.dto.mapper.IUserInfoMapper;
import com.leucine.streem.dto.projection.JobProcessInfoView;
import com.leucine.streem.dto.request.ArchiveObjectRequest;
import com.leucine.streem.dto.request.EntityObjectValueRequest;
import com.leucine.streem.dto.request.UnarchiveObjectRequest;
import com.leucine.streem.dto.response.Error;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.ExceptionType;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.User;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.repository.IEntityObjectRepository;
import com.leucine.streem.repository.IObjectTypeRepository;
import com.leucine.streem.repository.IUserRepository;
import com.leucine.streem.service.ICollectionCodeService;
import com.leucine.streem.service.IEntityObjectChangeLogService;
import com.leucine.streem.service.IEntityObjectService;
import com.leucine.streem.service.IShortCodeService;
import com.leucine.streem.util.DateTimeUtils;
import com.leucine.streem.util.JsonUtils;
import com.leucine.streem.util.Utility;
import com.leucine.streem.util.ValidationUtils;
import com.leucine.streem.validator.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.assertj.core.util.Lists;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.leucine.streem.constant.Misc.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntityObjectService implements IEntityObjectService {
  private final IEntityObjectRepository entityObjectRepository;
  private final IObjectTypeRepository objectTypeRepository;
  private final ICollectionCodeService objectTypeCodeService;
  private final IEntityObjectChangeLogService entityObjectChangeLogService;
  private final IShortCodeService shortCodeService;
  private final IUserRepository userRepository;
  private final IUserInfoMapper userInfoMapper;

  @Override
  public EntityObject findById(String collectionName, String id) throws ResourceNotFoundException {
    log.info("[findById] Request to fetch an Entity Object, collectionName: {}, entityObjectId: {}", collectionName, id);
    return entityObjectRepository.findById(collectionName, id)
      .orElseThrow(() -> new ResourceNotFoundException(id, ErrorCode.ENTITY_OBJECT_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public EntityObject save(EntityObjectValueRequest entityObjectValueRequest, JobProcessInfoView jobProcessInfoView) throws ResourceNotFoundException, StreemException, JsonProcessingException {
    log.info("[save] Request to create Entity Object, EntityObjectValueRequest: {}", entityObjectValueRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());
    Long currentFacilityId = principalUser.getCurrentFacilityId();
    String objectTypeId = entityObjectValueRequest.getObjectTypeId();

    if (Utility.isEmpty(entityObjectValueRequest.getReason())) {
      ValidationUtils.invalidate(objectTypeId, ErrorCode.COULD_NOT_CREATE_ENTITY_OBJECT_REASON_CANNOT_BE_EMPTY);
    }

    if (currentFacilityId == null) {
      ValidationUtils.invalidate(currentFacilityId, ErrorCode.COULD_NOT_CREATE_ENTITY_OBJECT_IN_ALL_FACILITY);
    }

    ObjectType objectType = objectTypeRepository.findById(objectTypeId)
      .orElseThrow(() -> new ResourceNotFoundException(objectTypeId, ErrorCode.OBJECT_TYPE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    Map<String, List<PartialEntityObject>> relationsRequestMap = entityObjectValueRequest.getRelations();

    EntityObject entityObject = new EntityObject();
    entityObject.setVersion(1);
    entityObject.setObjectTypeId(objectType.getId());
    PartialObjectType partialObjectType = new PartialObjectType();
    partialObjectType.setDisplayName(objectType.getDisplayName());
    partialObjectType.setId(objectType.getId());
    partialObjectType.setExternalId(objectType.getExternalId());
    partialObjectType.setVersion(objectType.getVersion());
    entityObject.setObjectType(partialObjectType);

    List<PropertyValue> propertyValues = new ArrayList<>();
    List<MappedRelation> selectedRelations = new ArrayList<>();

    Map<ObjectId, Object> searchableMap = entityObject.getSearchable();
    setObjectTypeProperties(entityObjectValueRequest.getProperties(), propertyValues, new HashMap<>(), objectType, entityObject, searchableMap);
    setCreateObjectFixedProperties(principalUserEntity, objectType, propertyValues, searchableMap);
    setObjectTypeRelations(relationsRequestMap, selectedRelations, new HashMap<>(), objectType, searchableMap, jobProcessInfoView);

    entityObject.setProperties(propertyValues);
    entityObject.setRelations(selectedRelations);

    entityObject.setCollection(objectType.getExternalId());
    Long timestamp = DateTimeUtils.now();
    entityObject.setCreatedAt(timestamp);
    entityObject.setModifiedAt(timestamp);
    entityObject.setCreatedBy(userInfoMapper.toUserInfo(principalUser));
    entityObject.setModifiedBy(userInfoMapper.toUserInfo(principalUser));
    entityObject.setUsageStatus(UsageStatus.ACTIVE.getCode());
    entityObject.setFacilityId(currentFacilityId.toString());
    entityObject.setId(new ObjectId());

    if (Utility.isEmpty(entityObjectValueRequest.getShortCode())) {
      shortCodeService.generateAndSaveShortCode(principalUser, entityObject);
    } else {
      entityObject.setShortCode(entityObjectValueRequest.getShortCode());
      shortCodeService.saveShortCode(principalUser, entityObject);
    }
    var newEntityObject = entityObjectRepository.save(entityObject, objectType.getExternalId());

    saveObjectLogs(principalUser, new EntityObject(), newEntityObject, entityObjectValueRequest.getReason(), jobProcessInfoView);
    return newEntityObject;
  }

  @Override
  //check5
  @Transactional(rollbackFor = Exception.class)
  public EntityObject update(String objectId, EntityObjectValueRequest entityObjectValueRequest, JobProcessInfoView jobProcessInfoView) throws ResourceNotFoundException, StreemException, JsonProcessingException {
    // TODO get only updated fields from UI and update specific fields
    log.info("[update] Request to update Entity Object, EntityObjectValueRequest: {}", entityObjectValueRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());
    Long currentFacilityId = principalUser.getCurrentFacilityId();
    String objectTypeId = entityObjectValueRequest.getObjectTypeId();

    if (Utility.isEmpty(entityObjectValueRequest.getReason())) {
      ValidationUtils.invalidate(objectTypeId, ErrorCode.COULD_NOT_UPDATE_ENTITY_OBJECT_REASON_CANNOT_BE_EMPTY);
    }
    if (currentFacilityId == null) {
      ValidationUtils.invalidate(currentFacilityId, ErrorCode.COULD_NOT_UPDATE_ENTITY_OBJECT_IN_ALL_FACILITY);
    }

    ObjectType objectType = objectTypeRepository.findById(objectTypeId)
      .orElseThrow(() -> new ResourceNotFoundException(objectTypeId, ErrorCode.OBJECT_TYPE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    EntityObject entityObject = entityObjectRepository.findById(objectType.getExternalId(), objectId)
      .orElseThrow(() -> new ResourceNotFoundException(objectId, ErrorCode.ENTITY_OBJECT_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    if (entityObject.getUsageStatus() == UsageStatus.DEPRECATED.getCode()) {
      ValidationUtils.invalidate(objectId, ErrorCode.OBJECT_ALREADY_ARCHIVED);
    }

    EntityObject oldEntityObject = JsonUtils.readValue(JsonUtils.writeValueAsString(entityObject), EntityObject.class);
    Map<String, List<PartialEntityObject>> relationsRequestMap = entityObjectValueRequest.getRelations();

    List<PropertyValue> propertyValues = entityObject.getProperties();
    List<MappedRelation> selectedRelations = entityObject.getRelations();

    Map<String, PropertyValue> existingPropertyValuesMap = propertyValues.stream().collect(Collectors.toMap(pv -> pv.getId().toString(), Function.identity()));
    Map<String, MappedRelation> existingRelationsMap = selectedRelations.stream().collect(Collectors.toMap(m -> m.getId().toString(), Function.identity()));


    Map<ObjectId, Object> searchableMap = entityObject.getSearchable();
    setObjectTypeProperties(entityObjectValueRequest.getProperties(), propertyValues, existingPropertyValuesMap, objectType, entityObject, searchableMap);
    setUpdateObjectSystemProperties(principalUserEntity, propertyValues, searchableMap);
    setObjectTypeRelations(relationsRequestMap, selectedRelations, existingRelationsMap, objectType, searchableMap, jobProcessInfoView);


    entityObject.setProperties(propertyValues);
    entityObject.setRelations(selectedRelations);

    entityObject.setCollection(objectType.getExternalId());
    Long timestamp = DateTimeUtils.now();
    entityObject.setModifiedAt(timestamp);
    entityObject.setModifiedBy(userInfoMapper.toUserInfo(principalUser));
    shortCodeService.generateAndSaveShortCode(principalUser, entityObject);
    var updatedEntityObject = entityObjectRepository.save(entityObject, objectType.getExternalId());
    saveObjectLogs(principalUser, oldEntityObject, updatedEntityObject, entityObjectValueRequest.getReason(), jobProcessInfoView);
    return updatedEntityObject;
  }

  @Override
  public Page<EntityObject> findAllByUsageStatus(String collectionName, int usageStatus, String propertyExternalId, String propertyValue, String filters, Pageable pageable) {
    log.info("[findAllByUsageStatus] Request to get all Entity Object by Usage Status, collectionName: {}, usageStatus: {}", collectionName, usageStatus);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return entityObjectRepository.findAllByUsageStatus(collectionName, usageStatus, propertyExternalId, propertyValue, principalUser.getCurrentFacilityId(), filters, pageable);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public BasicDto enableSearchable() {
    List<ObjectType> objectTypeList = objectTypeRepository.findAll();
    objectTypeList.forEach(objectType -> {
      List<EntityObject> entityObjects = entityObjectRepository.findAll(objectType.getExternalId());
      createSearchableField(entityObjects, objectType);
    });
    return new BasicDto("success", null, null);
  }

  @Override
  public BasicDto unarchiveObject(UnarchiveObjectRequest unarchiveObjectRequest, String objectId, JobProcessInfoView jobProcessInfoView) throws ResourceNotFoundException, StreemException, JsonProcessingException {
    log.info("[unarchiveObject] Request to unArchive object by object id: {}, collection name: {}, reason: {}", objectId, unarchiveObjectRequest.collectionName(), unarchiveObjectRequest.reason());
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());
    EntityObject entityObject = findById(unarchiveObjectRequest.collectionName(), objectId);
    EntityObject oldEntityObject = JsonUtils.readValue(JsonUtils.writeValueAsString(entityObject), EntityObject.class);
    if (entityObject.getUsageStatus() == UsageStatus.ACTIVE.getCode()) {
      ValidationUtils.invalidate(objectId, ErrorCode.OBJECT_ALREADY_UNARCHIVED);
    }
    if (Utility.isEmpty(unarchiveObjectRequest.reason())) {
      ValidationUtils.invalidate(objectId, ErrorCode.ARCHIVE_REASON_CANNOT_BE_EMPTY);
    }
    entityObject.setUsageStatus(UsageStatus.ACTIVE.getCode());
    List<PropertyValue> propertyValues = entityObject.getProperties();
    for (PropertyValue propertyValue : propertyValues) {
      switch (propertyValue.getExternalId()) {
        case USAGE_STATUS_EXTERNAL_ID -> propertyValue.setValue(String.valueOf(UsageStatus.ACTIVE.getCode()));
        case UPDATED_AT_EXTERNAL_ID -> propertyValue.setValue(String.valueOf(DateTimeUtils.now()));
        case UPDATED_BY_EXTERNAL_ID -> propertyValue.setValue(principalUserEntity.getIdAsString());
      }
    }
    entityObject.setProperties(propertyValues);

    EntityObject newEntityObject = entityObjectRepository.save(entityObject, unarchiveObjectRequest.collectionName());

    saveObjectLogs(principalUser, oldEntityObject, newEntityObject, unarchiveObjectRequest.reason(), jobProcessInfoView);
    BasicDto basicDto = new BasicDto();
    basicDto.setId(entityObject.getId().toString());
    basicDto.setMessage("success");
    return basicDto;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public BasicDto archiveObject(ArchiveObjectRequest archiveObjectRequest, String objectId, JobProcessInfoView jobProcessInfoView) throws ResourceNotFoundException, StreemException, JsonProcessingException {
    log.info("[archiveObject] Request to archive object by object id: {}, collection name: {}, reason: {}", objectId, archiveObjectRequest.collectionName(), archiveObjectRequest.reason());
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());

    EntityObject entityObject = findById(archiveObjectRequest.collectionName(), objectId);
    EntityObject oldEntityObject = JsonUtils.readValue(JsonUtils.writeValueAsString(entityObject), EntityObject.class);

    if (entityObject.getUsageStatus() == UsageStatus.DEPRECATED.getCode()) {
      ValidationUtils.invalidate(objectId, ErrorCode.OBJECT_ALREADY_ARCHIVED);
    }
    if (Utility.isEmpty(archiveObjectRequest.reason())) {
      ValidationUtils.invalidate(objectId, ErrorCode.ARCHIVE_REASON_CANNOT_BE_EMPTY);
    }
    entityObject.setUsageStatus(UsageStatus.DEPRECATED.getCode());
    List<PropertyValue> propertyValues = entityObject.getProperties();
    for (PropertyValue propertyValue : propertyValues) {
      switch (propertyValue.getExternalId()) {
        case USAGE_STATUS_EXTERNAL_ID -> propertyValue.setValue(String.valueOf(UsageStatus.DEPRECATED.getCode()));
        case UPDATED_AT_EXTERNAL_ID -> propertyValue.setValue(String.valueOf(DateTimeUtils.now()));
        case UPDATED_BY_EXTERNAL_ID -> propertyValue.setValue(principalUserEntity.getIdAsString());
      }
    }
    entityObject.setProperties(propertyValues);
    EntityObject newEntityObject = entityObjectRepository.save(entityObject, archiveObjectRequest.collectionName());

    saveObjectLogs(principalUser, oldEntityObject, newEntityObject, archiveObjectRequest.reason(), jobProcessInfoView);

    BasicDto basicDto = new BasicDto();
    basicDto.setId(entityObject.getId().toString());
    basicDto.setMessage("success");
    return basicDto;
  }

  @Override
  public Page<PartialEntityObject> findPartialByUsageStatus(String collectionName, int usageStatus, String propertyExternalId, String propertyValue, Pageable pageable, String filters, String query) {
    log.info("[findPartialByUsageStatus] Request to get all Partial Entity Object by Usage Status, collectionName: {}, usageStatus: {}", collectionName, usageStatus);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return entityObjectRepository.findPartialByUsageStatus(collectionName, usageStatus, propertyExternalId, propertyValue, principalUser.getCurrentFacilityId(), filters, query, pageable);
  }

  private void setObjectTypeProperties(Map<String, Object> propertiesRequestMap, List<PropertyValue> propertyValues,
                                       Map<String, PropertyValue> existingPropertyValuesMap, ObjectType objectType, EntityObject entityObject, Map<ObjectId, Object> searchableMap) throws StreemException {
    // TODO Check instance of before casting and handle error
    List<Error> errorList = new ArrayList<>();

    for (Property property : objectType.getProperties()) {
      // We do not want to update some fixed properties, the function set fixed properties will manage it
      if (CREATE_PROPERTIES.contains(property.getExternalId())) {
        continue;
      }
      String value = null;
      List<String> choices = new ArrayList<>();
      List<PropertyOption> propertyValueChoices = new ArrayList<>();
      boolean isPropertyValueBeingUpdated = false;
      boolean isPropertyValueBeingCreated = false;
      PropertyValue propertyValue = null;

      if (existingPropertyValuesMap.containsKey(property.getId().toString()) && propertiesRequestMap.containsKey(property.getId().toString())) {
        isPropertyValueBeingUpdated = true;
        propertyValue = existingPropertyValuesMap.get(property.getId().toString());
      } else if (!existingPropertyValuesMap.containsKey(property.getId().toString())) {
        isPropertyValueBeingCreated = true;
      }

      if (!Utility.isEmpty(propertyValue) && ((property.getFlags() >> CollectionMisc.Flag.IS_MANDATORY.get() & 1)) == 1 && Utility.isEmpty(propertiesRequestMap.get(property.getId().toString()))) {
        ValidationUtils.invalidate(objectType.getId().toString(), ErrorCode.OBJECT_TYPE_MANDATORY_PROPERTIES_NOT_SET);
      }

      if (isPropertyValueBeingUpdated || isPropertyValueBeingCreated) {
        if (!Utility.isEmpty(propertiesRequestMap.get(property.getId().toString()))) {
          if (CollectionMisc.PROPERTY_DROPDOWN_TYPES.contains(property.getInputType())) {
            if (Utility.isCollection(propertiesRequestMap.get((property.getId().toString())))) {
              choices = (List<String>) propertiesRequestMap.get(property.getId().toString());
              Map<String, PropertyOption> optionMap = property.getOptions().stream().collect(Collectors.toMap(po -> po.getId().toString(), p -> p));
              for (String choice : choices) {
                PropertyOption selectedOption = optionMap.get(choice);
                propertyValueChoices.add(selectedOption);
              }
            } else {
              ValidationUtils.invalidate(property.getId().toString(), ErrorCode.ENTITY_OBJECT_PROPERTY_INVALID_INPUT);
            }
          } else {
            if (Utility.isString(propertiesRequestMap.get(property.getId().toString()))) {
              value = (String) propertiesRequestMap.get(property.getId().toString());
            } else {
              ValidationUtils.invalidate(property.getId().toString(), ErrorCode.ENTITY_OBJECT_PROPERTY_INVALID_INPUT);
            }
          }
          validateProperties(property, value, choices, errorList);
        }

        if (!isPropertyValueBeingUpdated) {
          propertyValue = new PropertyValue();
          propertyValue.setChoices(null);
          propertyValue.setValue(null);
        }

        if (CollectionKey.EXTERNAL_ID.equals(property.getExternalId())) {
          if (!Utility.isEmpty(value)) {
            value = value.trim();
          }
          if (isPropertyValueBeingUpdated) {
            ValidationUtils.invalidate(String.valueOf(property.getId()), ErrorCode.OBJECT_EXTERNAL_ID_NOT_UPDATABLE);
          } else {
            entityObject.setExternalId(value);
          }

        }

        if (CollectionKey.DISPLAY_NAME.equals(property.getExternalId())) {
          entityObject.setDisplayName(value);
        }

        propertyValue.setValue(value);
        propertyValue.setChoices(propertyValueChoices);
        propertyValue.setDisplayName(property.getDisplayName());
        if (CollectionKey.EXTERNAL_ID.equals(property.getExternalId()) && ((property.getFlags() >> CollectionMisc.Flag.IS_AUTOGENERATE.get() & 1)) == 1) {
          value = objectTypeCodeService.getCode(property.getAutogeneratePrefix());
          propertyValue.setValue(value);
          entityObject.setExternalId(value);
        }
        propertyValue.setExternalId(property.getExternalId());

        propertyValue.setId(property.getId());

        if (!isPropertyValueBeingUpdated) {
          propertyValues.add(propertyValue);
        }
      }
      if (propertiesRequestMap.containsKey(property.getId().toString()) && !Utility.isEmpty(propertiesRequestMap.get(property.getId().toString()))) {
        if (!CollectionMisc.PROPERTY_DROPDOWN_TYPES.contains(property.getInputType())) {
          switch (property.getInputType()) {
            case DATE, DATE_TIME -> {
              if (Utility.isEmpty(propertiesRequestMap.get(property.getId().toString()))) {
                searchableMap.put(property.getId(), 0);
              } else {
                searchableMap.put(property.getId(), Long.valueOf(propertiesRequestMap.get(property.getId().toString()).toString()));
              }
            }
            case NUMBER -> {
              if (Utility.isEmpty(propertiesRequestMap.get(property.getId().toString()))) {
                searchableMap.put(property.getId(), 0);
              } else {
                searchableMap.put(property.getId(), Double.valueOf(propertiesRequestMap.get(property.getId().toString()).toString()));
              }
            }
            case SINGLE_LINE, MULTI_LINE ->
              searchableMap.put(property.getId(), propertiesRequestMap.get(property.getId().toString()).toString());
          }
        } else {
          // TODO refactor
          if (property.getInputType().equals(CollectionMisc.PropertyType.SINGLE_SELECT) || property.getInputType().equals(CollectionMisc.PropertyType.MULTI_SELECT)) {
            List<String> choiceList = (List<String>) propertiesRequestMap.get(property.getId().toString());
            searchableMap.put(property.getId(), choiceList);
          }
        }
      }

      // This check is required because when we get an update request, we only get the properties that are being updated.
      if (propertiesRequestMap.containsKey(property.getId().toString())) {
        convertAndSetSearchablePropertyValue(searchableMap, propertyValue, property);
      }

      entityObject.setSearchable(searchableMap);
    }

    if (!Utility.isEmpty(errorList)) {
      ValidationUtils.invalidate(ErrorCode.ENTITY_OBJECT_PROPERTIES_VALIDATION.getDescription(), errorList);
    }
  }

  //TODO: get changes from demo zydus branch
  private void setObjectTypeRelations(Map<String, List<PartialEntityObject>> relationsRequestMap, List<MappedRelation> selectedRelations,
                                      Map<String, MappedRelation> existingRelationsMap, ObjectType objectType, Map<ObjectId, Object> searchableMap, JobProcessInfoView jobProcessInfoView) throws StreemException {

    if (Utility.isEmpty(objectType.getRelations())) {
      return;
    }

    List<Relation> relations = objectType.getRelations().stream().sorted(Comparator.comparing(Relation::getSortOrder)).toList();
    Map<String, List<String>> collectionObjectMap;
    for (Relation relation : relations) {
      collectionObjectMap = new HashMap<>();
      List<PartialEntityObject> partialEntityObjects = relationsRequestMap.get(relation.getId().toString());
      if (!Utility.isEmpty(relationsRequestMap.get(relation.getId().toString()))) {
        for (PartialEntityObject partialEntityObject : relationsRequestMap.get(relation.getId().toString())) {
          if (collectionObjectMap.get(partialEntityObject.getCollection()) != null) {
            collectionObjectMap.get(partialEntityObject.getCollection()).add(partialEntityObject.getId().toString());
          } else {
            collectionObjectMap.put(partialEntityObject.getCollection(), Lists.list(partialEntityObject.getId().toString()));
          }
        }
        for (Map.Entry<String, List<String>> entry : collectionObjectMap.entrySet()) {
          List<PartialEntityObject> peo = entityObjectRepository.findPartialByIdsAndUsageStatus(entry.getKey(), entry.getValue(), CollectionMisc.UsageStatus.ACTIVE.get());
          if (peo.size() == partialEntityObjects.size()) {
            MappedRelation mp = new MappedRelation();
            boolean isRelationGettingUpdated = false;
            // Relation is getting updated if true
            if (existingRelationsMap.containsKey(relation.getId().toString())) {
              isRelationGettingUpdated = true;
              mp = existingRelationsMap.get(relation.getId().toString());
            }
            mp.setId(relation.getId());
            mp.setExternalId(relation.getExternalId());
            mp.setDisplayName(relation.getDisplayName());
            mp.setObjectTypeId(relation.getObjectTypeId());
            mp.setFlags(relation.getFlags());
            mp.setTargets(peo.stream().sorted(Comparator.comparing(PartialEntityObject::getExternalId)).map(p -> {
              MappedRelationTarget mappedRelationTarget = new MappedRelationTarget();
              mappedRelationTarget.setId(p.getId());
              mappedRelationTarget.setType(relation.getTarget().getType());
              mappedRelationTarget.setCollection(p.getCollection());
              mappedRelationTarget.setExternalId(p.getExternalId());
              mappedRelationTarget.setDisplayName(p.getDisplayName());
              return mappedRelationTarget;
            }).toList());

            if (!isRelationGettingUpdated) {
              selectedRelations.add(mp);
            }
            List<String> targetIds = mp.getTargets().stream()
              .map(target -> target.getId().toString())
              .collect(Collectors.toList());
            searchableMap.put(relation.getId(), targetIds);

          } else {
            // TODO Throw exception as few of the target no longer exist
          }
        }
      } else {
        if (existingRelationsMap.containsKey(relation.getId().toString())) {
          MappedRelation mappedRelation = existingRelationsMap.get(relation.getId().toString());
          selectedRelations.remove(mappedRelation);
          searchableMap.put(relation.getId(), null);
        }
      }
      //TODO: When revision comes in picture, we need to remove the null check on jobProcessInfoView
      // to log each if clause conditions


      System.out.println("[setObjectTypeRelations] Checking if clause:  isMandatory: " + ((relation.getFlags() >> CollectionMisc.Flag.IS_MANDATORY.get() & 1) == 1) +
        ", isEmpty relation: " + Utility.isEmpty(relationsRequestMap.get(relation.getId().toString())) +
        ", status: " + (relation.getUsageStatus() == UsageStatus.ACTIVE.getCode()) +
        ", isEmpty job: " + Utility.isEmpty(jobProcessInfoView));

      if (((relation.getFlags() >> CollectionMisc.Flag.IS_MANDATORY.get() & 1)) == 1 && Utility.isEmpty(relationsRequestMap.get(relation.getId().toString())) && relation.getUsageStatus() == UsageStatus.ACTIVE.getCode() && Utility.isEmpty(jobProcessInfoView)) {
        ValidationUtils.invalidate(objectType.getId().toString(), ErrorCode.OBJECT_TYPE_MANDATORY_RELATIONS_NOT_SET);
      }
    }
  }

  private void validateProperties(Property property, String value, List<String> choices, List<Error> errorList) {
    // TODO checks needed ? Date ? valid date provided or not
    var validations = property.getValidations();
    if (!Utility.isEmpty(validations)) {
      for (PropertyValidation validation : validations) {
        ConstraintValidator validator = null;
        switch (validation.getConstraint()) {
          case LT -> {
            validator = new LessThanValidator(Double.parseDouble(validation.getValue()), validation.getErrorMessage());
            validator.validate(value);
          }
          case GT -> {
            validator = new GreaterThanValidator(Double.parseDouble(validation.getValue()), validation.getErrorMessage());
            validator.validate(value);
          }
          case LTE -> {
            validator = new LessThanOrEqualValidator(Double.parseDouble(validation.getValue()), validation.getErrorMessage());
            validator.validate(value);
          }
          case GTE -> {
            validator = new GreaterThanOrEqualValidator(Double.parseDouble(validation.getValue()), validation.getErrorMessage());
            validator.validate(value);
          }
          case NE -> {
            validator = new NotEqualValueValidator(Double.parseDouble(validation.getValue()), validation.getErrorMessage());
            validator.validate(value);
          }
          case MIN -> {
            if (CollectionMisc.PROPERTY_DROPDOWN_TYPES.contains(property.getInputType())) {
              validator = new MinChoiceValidator(Integer.parseInt(validation.getValue()), validation.getErrorMessage());
              validator.validate(choices);
            } else {
              validator = new MinLengthValidator(Integer.parseInt(validation.getValue()), validation.getErrorMessage());
              validator.validate(value);
            }
          }
          case MAX -> {
            if (CollectionMisc.PROPERTY_DROPDOWN_TYPES.contains(property.getInputType())) {
              validator = new MaxChoiceValidator(Integer.parseInt(validation.getValue()), validation.getErrorMessage());
              validator.validate(choices);
            } else {
              validator = new MaxLengthValidator(Integer.parseInt(validation.getValue()), validation.getErrorMessage());
              validator.validate(value);
            }
          }
          case PATTERN -> {
            validator = new RegexValidator(validation.getValue(), validation.getErrorMessage());
            validator.validate(value);
          }
        }
        if (null != validator && !validator.isValid()) {
          ValidationUtils.addError(property.getId().toString(), errorList, ErrorCode.ENTITY_OBJECT_PROPERTIES_VALIDATION, validator.getErrorMessage());
        }
      }
    }
  }

  private void saveObjectLogs(PrincipalUser principalUser, EntityObject entityObject, EntityObject updatedEntityObject, String reason, JobProcessInfoView jobProcessInfoView) throws ResourceNotFoundException {
    entityObjectChangeLogService.save(principalUser, entityObject, updatedEntityObject, reason, jobProcessInfoView);
  }

  private void createSearchableField(List<EntityObject> entityObjects, ObjectType objectType) {
    for (EntityObject entityObject : entityObjects) {
      List<PropertyValue> propertyValues = entityObject.getProperties();
      List<MappedRelation> relationValues = entityObject.getRelations();

      Map<ObjectId, Object> searchable = new HashMap<>();

      Map<String, Property> propertyMap = objectType.getProperties()
        .stream()
        .collect(Collectors.toMap(p -> p.getId().toString(), Function.identity()));

      Map<String, Relation> relationMap = objectType.getRelations()
        .stream()
        .collect(Collectors.toMap(p -> p.getId().toString(), Function.identity()));

      for (PropertyValue propertyValue : propertyValues) {
        Property property = propertyMap.get(propertyValue.getId().toString());
        if (!Utility.isEmpty(property)) {
          convertAndSetSearchablePropertyValue(searchable, propertyValue, property);
        }
      }

      for (MappedRelation mappedRelation : relationValues) {
        Relation relation = relationMap.get(mappedRelation.getId().toString());
        Object value = null;
        if (!Utility.isEmpty(relation)) {
          switch (relation.getTarget().getCardinality()) {
            case ONE_TO_ONE -> {
              if (!Utility.isEmpty(mappedRelation.getTargets())) {
                value = mappedRelation.getTargets().get(0).getId().toString();
              }
            }
            case ONE_TO_MANY -> {
              if (!Utility.isEmpty(mappedRelation.getTargets())) {
                value = mappedRelation.getTargets().stream().map(MappedRelationTarget::getId).map(ObjectId::toString).collect(Collectors.toList());
              }
            }
          }
          searchable.put(mappedRelation.getId(), value);
        }
        entityObject.setSearchable(searchable);
      }
    }
    entityObjectRepository.saveAll(entityObjects, objectType.getExternalId());
  }

  private static void convertAndSetSearchablePropertyValue(Map<ObjectId, Object> searchable, PropertyValue propertyValue, Property property) {
    Object value = null;
    // For value properties
    if (!Utility.isEmpty(propertyValue.getValue()) && !CollectionMisc.PROPERTY_DROPDOWN_TYPES.contains(property.getInputType())) {
      switch (property.getInputType()) {
        case DATE, DATE_TIME -> {
          if ("NaN".equals(propertyValue.getValue())) {
            propertyValue.setValue(null);
          } else if (propertyValue.getValue().contains("-")) {
            value = (DateTimeUtils.getEpochFromDate(propertyValue.getValue()));
            propertyValue.setValue(value.toString());
          } else {
            value = Long.valueOf(propertyValue.getValue());
          }
        }
        case SINGLE_LINE, MULTI_LINE -> value = propertyValue.getValue();
        case NUMBER -> value = Double.valueOf(propertyValue.getValue());
        // TODO Add support for multiselect and fix single select if required
      }
    }
    // For choices properties
    if (!Utility.isEmpty(propertyValue.getChoices()) && CollectionMisc.PROPERTY_DROPDOWN_TYPES.contains(property.getInputType())) {
      switch (property.getInputType()) {
        case SINGLE_SELECT -> value = propertyValue.getChoices().get(0).getId().toString();
        case MULTI_SELECT ->
          value = (List<String>) propertyValue.getChoices().stream().map(propertyOption -> propertyOption.getId().toString()).collect(Collectors.toList());
      }
    }
    searchable.put(propertyValue.getId(), value);
  }

  private void setCreateObjectFixedProperties(User principalUserEntity, ObjectType objectType, List<PropertyValue> propertyValues, Map<ObjectId, Object> searchableMap) {

    for (Property property : objectType.getProperties()) {
      String externalId = property.getExternalId();
      if (!CREATE_PROPERTIES.contains(externalId)) {
        continue;
      }

      PropertyValue propertyValue = new PropertyValue();
      propertyValue.setId(property.getId());
      propertyValue.setExternalId(property.getExternalId());
      propertyValue.setDisplayName(property.getDisplayName());
      searchableMap.put(property.getId(), propertyValue);
      Object value = null;
      switch (externalId) {
        case CREATED_AT_EXTERNAL_ID, UPDATED_AT_EXTERNAL_ID -> {
          value = DateTimeUtils.now();
          propertyValue.setValue(value.toString());
        }
        case CREATED_BY_EXTERNAL_ID, UPDATED_BY_EXTERNAL_ID -> {
          value = principalUserEntity.getId();
          propertyValue.setValue(value.toString());
        }
        case USAGE_STATUS_EXTERNAL_ID -> {
          value = CollectionMisc.UsageStatus.ACTIVE.get();
          propertyValue.setValue(String.valueOf(value));
        }
      }
      searchableMap.put(property.getId(), value);
      propertyValues.add(propertyValue);
    }
  }

  private void setUpdateObjectSystemProperties(User principalUserEntity, List<PropertyValue> propertyValues, Map<ObjectId, Object> searchableMap) {
    for (PropertyValue propertyValue : propertyValues) {
      if (UPDATE_PROPERTIES.contains(propertyValue.getExternalId()))
        if (propertyValue.getExternalId().equals(UPDATED_AT_EXTERNAL_ID)) {
          propertyValue.setValue(String.valueOf(DateTimeUtils.now()));
          searchableMap.put(propertyValue.getId(), propertyValue.getValue());
        } else if (propertyValue.getExternalId().equals(UPDATED_BY_EXTERNAL_ID)) {
          searchableMap.put(propertyValue.getId(), principalUserEntity.getId());
          propertyValue.setValue(principalUserEntity.getIdAsString());
        }
    }
  }


  private EntityObjectValueRequest createEntityObjectRequest(Row row, List<String> headers, ObjectType objectType,
                                                             Map<String, ObjectId> propertyMapping, Map<String, ObjectId> relationMapping) throws StreemException, ResourceNotFoundException {

    EntityObjectValueRequest request = new EntityObjectValueRequest();
    request.setObjectTypeId(objectType.getId().toString());
    request.setReason("Bulk import from Excel");

    Map<String, Object> properties = new HashMap<>();
    Map<String, List<PartialEntityObject>> relations = new HashMap<>();

    // Process each cell in the row
    for (int cellIndex = 0; cellIndex < headers.size() && cellIndex < row.getLastCellNum(); cellIndex++) {
      String header = headers.get(cellIndex);
      Cell cell = row.getCell(cellIndex);
      String cellValue = getCellValueAsString(cell);

      if (header.startsWith("property_")) {
        ObjectId propertyId = propertyMapping.get(header);
        if (propertyId != null) {
          Property property = findPropertyById(objectType, propertyId);
          // ALWAYS add property, even if empty (null means clear the property)
          Object parsedValue = Utility.isEmpty(cellValue) ? null : parsePropertyValue(cellValue, property);
          properties.put(propertyId.toString(), parsedValue);
        }
      } else if (header.startsWith("relation_")) {
        ObjectId relationId = relationMapping.get(header);
        if (relationId != null) {
          if (!Utility.isEmpty(cellValue)) {
            Relation relation = findRelationById(objectType, relationId);
            List<PartialEntityObject> targets = parseRelationTargets(cellValue, relation);
            relations.put(relationId.toString(), targets);
          } else {
            // Empty relation means clear it
            relations.put(relationId.toString(), new ArrayList<>());
          }
        }
      }
      // Skip usageStatus as it's handled automatically
    }

    request.setProperties(properties);
    request.setRelations(relations);

    return request;
  }

  private String getCellValueAsString(Cell cell) {
    if (cell == null) {
      return "";
    }

    switch (cell.getCellType()) {
      case STRING:
        return cell.getStringCellValue().trim();
      case NUMERIC:
        if (DateUtil.isCellDateFormatted(cell)) {
          return String.valueOf(cell.getDateCellValue().getTime());
        } else {
          // Handle both integers and decimals
          double numericValue = cell.getNumericCellValue();
          if (numericValue == Math.floor(numericValue)) {
            return String.valueOf((long) numericValue);
          } else {
            return String.valueOf(numericValue);
          }
        }
      case BOOLEAN:
        return String.valueOf(cell.getBooleanCellValue());
      case FORMULA:
        return cell.getCellFormula();
      default:
        return "";
    }
  }

  private Property findPropertyById(ObjectType objectType, ObjectId propertyId) {
    return objectType.getProperties().stream()
      .filter(p -> p.getId().equals(propertyId))
      .findFirst()
      .orElse(null);
  }

  private Relation findRelationById(ObjectType objectType, ObjectId relationId) {
    return objectType.getRelations().stream()
      .filter(r -> r.getId().equals(relationId))
      .findFirst()
      .orElse(null);
  }

  private Object parsePropertyValue(String cellValue, Property property) throws StreemException, ResourceNotFoundException {
    if (CollectionMisc.PROPERTY_DROPDOWN_TYPES.contains(property.getInputType())) {
      // Handle multi-select choices
      return parseChoices(cellValue, property);
    } else {
      // Handle simple values
      return cellValue;
    }
  }

  private List<String> parseChoices(String cellValue, Property property) throws ResourceNotFoundException {
    List<String> choiceIds = new ArrayList<>();
    String[] displayNames = cellValue.split(",");

    // Create a map of displayName -> PropertyOption for quick lookup
    Map<String, PropertyOption> optionMap = property.getOptions().stream()
      .collect(Collectors.toMap(PropertyOption::getDisplayName, Function.identity()));

    for (String displayName : displayNames) {
      displayName = displayName.trim();
      PropertyOption option = optionMap.get(displayName);
      if (option == null) {
        throw new ResourceNotFoundException(displayName, ErrorCode.ENTITY_OBJECT_PROPERTY_INVALID_INPUT, ExceptionType.ENTITY_NOT_FOUND);
      }
      choiceIds.add(option.getId().toString());
    }

    return choiceIds;
  }

  private List<PartialEntityObject> parseRelationTargets(String cellValue, Relation relation) throws ResourceNotFoundException {
    List<PartialEntityObject> targets = new ArrayList<>();
    String[] externalIds = cellValue.split(",");

    // Get target collection from relation's objectTypeId
    ObjectType targetObjectType = objectTypeRepository.findById(relation.getObjectTypeId())
      .orElseThrow(() -> new ResourceNotFoundException(relation.getObjectTypeId(), ErrorCode.OBJECT_TYPE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    String targetCollection = targetObjectType.getExternalId();
    
    // Get current facility ID for proper isolation
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    String facilityId = principalUser.getCurrentFacilityId().toString();

    for (String externalId : externalIds) {
      externalId = externalId.trim();

      // Query database to find the actual target EntityObject
      Optional<EntityObject> targetObjectOpt = entityObjectRepository
          .findByExternalIdAndUsageStatusAndFacilityId(
              targetCollection, 
              externalId, 
              UsageStatus.ACTIVE.getCode(), 
              facilityId);
      
      if (targetObjectOpt.isPresent()) {
        EntityObject targetObject = targetObjectOpt.get();
        
        PartialEntityObject partialObject = new PartialEntityObject();
        partialObject.setId(targetObject.getId()); // ✅ CORRECT: Actual target object ID
        partialObject.setExternalId(externalId);
        partialObject.setCollection(targetCollection);
        partialObject.setDisplayName(targetObject.getDisplayName()); // ✅ CORRECT: Actual displayName
        
        targets.add(partialObject);
      } else {
        // Throw exception if target object not found
        throw new ResourceNotFoundException(
            externalId, 
            ErrorCode.ENTITY_OBJECT_NOT_FOUND, 
            ExceptionType.ENTITY_NOT_FOUND);
      }
    }

    return targets;
  }

  private ImportResult processRowWithDuplicateCheck(Row row, List<String> headers, ObjectType objectType,
                                                    Map<String, ObjectId> propertyMapping, Map<String, ObjectId> relationMapping,
                                                    String facilityId, int rowNumber) throws StreemException, ResourceNotFoundException, JsonProcessingException {

    // 1. Extract externalId from Excel row first
    String externalId = extractExternalIdFromRow(row, headers, objectType, propertyMapping);

    if (Utility.isEmpty(externalId)) {
      // If no external ID, create full request and save new object
      EntityObjectValueRequest request = createEntityObjectRequest(row, headers, objectType, propertyMapping, relationMapping);
      EntityObject created = save(request, null);
      return new ImportResult(created, com.leucine.streem.dto.ImportAction.CREATED,
        "Created new object", rowNumber);
    }

    // 2. Check for existing object using unique index fields
    Optional<EntityObject> existingOpt = entityObjectRepository
        .findByExternalIdAndUsageStatusAndFacilityId(
            objectType.getExternalId(), externalId, UsageStatus.ACTIVE.getCode(), facilityId);

    if (existingOpt.isPresent()) {
      EntityObject existing = existingOpt.get();
      
      // 3. Create delta request with ONLY changed values
      EntityObjectValueRequest deltaRequest = createDeltaUpdateRequest(
          row, headers, objectType, propertyMapping, relationMapping, existing);
      
      // 4. Check if there are any changes
      if (deltaRequest.getProperties().isEmpty() && deltaRequest.getRelations().isEmpty()) {
        return new ImportResult(existing, com.leucine.streem.dto.ImportAction.SKIPPED, 
            "No changes detected", rowNumber);
      } else {
        // 5. Update with only changed values
        EntityObject updated = update(existing.getId().toString(), deltaRequest, null);
        return new ImportResult(updated, com.leucine.streem.dto.ImportAction.UPDATED, 
            "Updated " + deltaRequest.getProperties().size() + " properties", rowNumber);
      }
    } else {
      // CREATE: Object doesn't exist - create full request
      EntityObjectValueRequest request = createEntityObjectRequest(row, headers, objectType, propertyMapping, relationMapping);
      EntityObject created = save(request, null);
      return new ImportResult(created, com.leucine.streem.dto.ImportAction.CREATED, 
          "Created new object", rowNumber);
    }
  }

  private String extractExternalIdFromRequest(EntityObjectValueRequest request, ObjectType objectType) {
    // Find the external_id property in the object type
    for (Property property : objectType.getProperties()) {
      if (CollectionKey.EXTERNAL_ID.equals(property.getExternalId())) {
        Object value = request.getProperties().get(property.getId().toString());
        return value != null ? value.toString() : null;
      }
    }
    return null;
  }

  private String extractExternalIdFromRow(Row row, List<String> headers, ObjectType objectType, Map<String, ObjectId> propertyMapping) {
    // Find the external_id property in the object type
    for (Property property : objectType.getProperties()) {
      if (CollectionKey.EXTERNAL_ID.equals(property.getExternalId())) {
        String expectedHeader = "property_" + property.getExternalId();
        if (headers.contains(expectedHeader)) {
          int cellIndex = headers.indexOf(expectedHeader);
          if (cellIndex >= 0 && cellIndex < row.getLastCellNum()) {
            Cell cell = row.getCell(cellIndex);
            return getCellValueAsString(cell);
          }
        }
      }
    }
    return null;
  }

  private EntityObjectValueRequest createDeltaUpdateRequest(Row row, List<String> headers, ObjectType objectType,
                                                           Map<String, ObjectId> propertyMapping, Map<String, ObjectId> relationMapping,
                                                           EntityObject existingObject) throws StreemException, ResourceNotFoundException {
    EntityObjectValueRequest request = new EntityObjectValueRequest();
    request.setObjectTypeId(objectType.getId().toString());
    request.setReason("Bulk import from Excel - Delta Update");

    Map<String, Object> changedProperties = new HashMap<>();
    Map<String, List<PartialEntityObject>> changedRelations = new HashMap<>();

    // Create maps for existing values for efficient lookup
    Map<String, PropertyValue> existingPropertyMap = new HashMap<>();
    if (existingObject.getProperties() != null) {
      for (PropertyValue pv : existingObject.getProperties()) {
        existingPropertyMap.put(pv.getId().toString(), pv);
      }
    }

    Map<String, MappedRelation> existingRelationMap = new HashMap<>();
    if (existingObject.getRelations() != null) {
      for (MappedRelation mr : existingObject.getRelations()) {
        existingRelationMap.put(mr.getId().toString(), mr);
      }
    }

    // Check properties for changes
    for (Property property : objectType.getProperties()) {
      if (CREATE_PROPERTIES.contains(property.getExternalId())) {
        continue; // Skip system properties
      }

      String expectedHeader = "property_" + property.getExternalId();
      if (headers.contains(expectedHeader)) {
        int cellIndex = headers.indexOf(expectedHeader);
        if (cellIndex >= 0 && cellIndex < row.getLastCellNum()) {
          Cell cell = row.getCell(cellIndex);
          String cellValue = getCellValueAsString(cell);

          // Parse Excel value
          Object excelValue = Utility.isEmpty(cellValue) ? null : parsePropertyValue(cellValue, property);

          // Get existing value
          Object existingValue = getExistingPropertyValue(existingPropertyMap.get(property.getId().toString()), property);

          // Compare values
          if (!Objects.equals(excelValue, existingValue)) {
            changedProperties.put(property.getId().toString(), excelValue);
          }
        }
      }
    }

    // Check relations for changes
    for (Relation relation : objectType.getRelations()) {
      String expectedHeader = "relation_" + relation.getExternalId();
      if (headers.contains(expectedHeader)) {
        int cellIndex = headers.indexOf(expectedHeader);
        if (cellIndex >= 0 && cellIndex < row.getLastCellNum()) {
          Cell cell = row.getCell(cellIndex);
          String cellValue = getCellValueAsString(cell);

          // Parse Excel value
          List<PartialEntityObject> excelTargets = Utility.isEmpty(cellValue) ? 
              new ArrayList<>() : parseRelationTargets(cellValue, relation);

          // Get existing value
          List<String> existingTargetIds = getExistingRelationTargets(existingRelationMap.get(relation.getId().toString()));

          // Compare values
          List<String> excelTargetIds = excelTargets.stream()
              .map(target -> target.getExternalId())
              .sorted()
              .collect(Collectors.toList());

          if (!Objects.equals(excelTargetIds, existingTargetIds)) {
            changedRelations.put(relation.getId().toString(), excelTargets);
          }
        }
      }
    }

    request.setProperties(changedProperties);
    request.setRelations(changedRelations);
    return request;
  }

  private Object getExistingPropertyValue(PropertyValue propertyValue, Property property) {
    if (propertyValue == null) {
      return null;
    }

    if (CollectionMisc.PROPERTY_DROPDOWN_TYPES.contains(property.getInputType())) {
      if (propertyValue.getChoices() != null && !propertyValue.getChoices().isEmpty()) {
        // For multi-select, return list of choice IDs
        return propertyValue.getChoices().stream()
            .map(c -> c.getId().toString())
            .collect(Collectors.toList());
      }
    } else {
      // For regular values
      return propertyValue.getValue();
    }
    return null;
  }

  private List<String> getExistingRelationTargets(MappedRelation mappedRelation) {
    if (mappedRelation == null || mappedRelation.getTargets() == null || mappedRelation.getTargets().isEmpty()) {
      return new ArrayList<>();
    }

    return mappedRelation.getTargets().stream()
        .map(target -> target.getExternalId())
        .sorted()
        .collect(Collectors.toList());
  }

  private boolean hasEntityObjectChanged(EntityObject existing, EntityObjectValueRequest newRequest, ObjectType objectType) {
    // Compare properties
    if (!compareProperties(existing.getProperties(), newRequest.getProperties(), objectType)) {
      return true;
    }

    // Compare relations
    if (!compareRelations(existing.getRelations(), newRequest.getRelations())) {
      return true;
    }

    return false; // No changes detected
  }

  private boolean compareProperties(List<PropertyValue> existing, Map<String, Object> newProperties, ObjectType objectType) {
    // Create complete maps for ALL properties in ObjectType, not just existing ones
    Map<String, Object> existingMap = new HashMap<>();
    Map<String, Object> newMap = new HashMap<>();

    // Process ALL properties from ObjectType definition
    for (Property property : objectType.getProperties()) {
      if (CREATE_PROPERTIES.contains(property.getExternalId())) {
        continue; // Skip system properties
      }

      String propertyId = property.getId().toString();

      // Get existing value (null if not found)
      PropertyValue existingPV = existing.stream()
        .filter(pv -> pv.getId().toString().equals(propertyId))
        .findFirst().orElse(null);

      Object existingValue = null;
      if (existingPV != null) {
        if (existingPV.getChoices() != null && !existingPV.getChoices().isEmpty()) {
          // For multi-select, store list of choice IDs
          existingValue = existingPV.getChoices().stream()
            .map(c -> c.getId().toString())
            .collect(Collectors.toList());
        } else {
          // For regular values
          existingValue = existingPV.getValue();
        }
      }
      existingMap.put(propertyId, existingValue);

      // Get new value (null if not provided - this means clear the property)
      Object newValue = newProperties.getOrDefault(propertyId, null);
      newMap.put(propertyId, newValue);
    }

    // Compare complete maps - this will detect:
    // 1. Properties that changed values
    // 2. Properties that were cleared (null in new but had value in existing)
    // 3. Properties that were added (null in existing but have value in new)
    return existingMap.equals(newMap);
  }

  private boolean compareRelations(List<MappedRelation> existing, Map<String, List<PartialEntityObject>> newRelations) {
    // Create map of existing relation targets by relation ID
    Map<String, List<String>> existingMap = new HashMap<>();
    for (MappedRelation mr : existing) {
      if (mr.getTargets() != null && !mr.getTargets().isEmpty()) {
        List<String> targetExternalIds = mr.getTargets().stream()
          .map(target -> target.getExternalId())
          .sorted()
          .collect(Collectors.toList());
        existingMap.put(mr.getId().toString(), targetExternalIds);
      } else {
        existingMap.put(mr.getId().toString(), new ArrayList<>());
      }
    }

    // Create map of new relation targets by relation ID
    Map<String, List<String>> newMap = new HashMap<>();
    for (Map.Entry<String, List<PartialEntityObject>> entry : newRelations.entrySet()) {
      if (entry.getValue() != null && !entry.getValue().isEmpty()) {
        List<String> targetExternalIds = entry.getValue().stream()
          .map(target -> target.getExternalId())
          .sorted()
          .collect(Collectors.toList());
        newMap.put(entry.getKey(), targetExternalIds);
      } else {
        newMap.put(entry.getKey(), new ArrayList<>());
      }
    }

    // Compare maps
    return existingMap.equals(newMap);
  }
}
