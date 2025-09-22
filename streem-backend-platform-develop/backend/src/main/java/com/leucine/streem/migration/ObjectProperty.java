package com.leucine.streem.migration;

import com.leucine.streem.collections.ObjectType;
import com.leucine.streem.collections.EntityObject;
import com.leucine.streem.collections.Property;
import com.leucine.streem.collections.PropertyValue;
import com.leucine.streem.collections.Relation;
import com.leucine.streem.collections.MappedRelation;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.repository.IEntityObjectRepository;
import com.leucine.streem.repository.IObjectTypeRepository;
import com.leucine.streem.service.IEntityObjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ObjectProperty {
  private final IObjectTypeRepository objectTypeRepository;
  private final IEntityObjectRepository entityObjectRepository;
  private final IEntityObjectService entityObjectService;

  @Transactional(rollbackFor = Exception.class)
  public BasicDto fixObjects() {
    List<ObjectType> objectTypes = objectTypeRepository.findAll();
    System.out.println("Fetched all object types.");

    // Prepare maps for quick access with unique keys per object type
    Map<String, Set<String>> objectTypeProperties = new HashMap<>();
    Map<String, Property> allProperties = new HashMap<>();
    Map<String, Set<String>> objectTypeRelations = new HashMap<>();
    Map<String, Relation> allRelations = new HashMap<>();

    for (ObjectType objectType : objectTypes) {
      String objectTypeKey = objectType.getExternalId();
      Set<String> propertiesSet = objectType.getProperties().stream()
              .map(prop -> objectTypeKey + ":" + prop.getDisplayName() + ":" + prop.getId().toString()) // Create composite key
              .collect(Collectors.toSet());
      objectTypeProperties.put(objectTypeKey, propertiesSet);

      // Cache Property details with composite keys
      objectType.getProperties().forEach(prop -> {
        String compositeKey = objectTypeKey + ":" + prop.getDisplayName() + ":" + prop.getId().toString();
        allProperties.put(compositeKey, prop);
      });

      Set<String> relationsSet = objectType.getRelations().stream()
              .map(rel -> objectTypeKey + ":" + rel.getDisplayName() + ":" + rel.getId().toString()) // Create composite key
              .collect(Collectors.toSet());
      objectTypeRelations.put(objectTypeKey, relationsSet);
      // Cache Relation details with composite keys
      objectType.getRelations().forEach(rel -> {
        String compositeKey = objectTypeKey + ":" + rel.getDisplayName() + ":" + rel.getId().toString();
        allRelations.put(compositeKey, rel);
      });
    }

    for (ObjectType objectType : objectTypes) {
      List<EntityObject> objects = entityObjectRepository.findAll(objectType.getExternalId());
      System.out.println("Fetched all objects for ObjectType: " + objectType.getExternalId());

      String objectTypeKey = objectType.getExternalId();
      Set<String> typeProperties = objectTypeProperties.get(objectTypeKey);
      Set<String> typeRelations = objectTypeRelations.get(objectTypeKey);

      for (EntityObject object : objects) {
        Set<String> objectProperties = object.getProperties().stream()
                .map(prop -> objectTypeKey + ":" + prop.getDisplayName() + ":" + prop.getId().toString()) // Use composite key
                .collect(Collectors.toSet());

        // Identify extra and missing properties
        Set<String> extraProperties = objectProperties.stream()
          .filter(prop -> !typeProperties.contains(prop))
          .collect(Collectors.toSet());
        Set<String> missingProperties = typeProperties.stream()
          .filter(prop -> !objectProperties.contains(prop))
          .collect(Collectors.toSet());

        // Remove extra properties
        if (!extraProperties.isEmpty()) {
          object.getProperties().removeIf(prop -> extraProperties.contains(objectTypeKey + ":" + prop.getDisplayName() + ":" + prop.getId().toString()));
          System.out.println("Removing extra properties from object: " + object.getId());
        }

        // Add missing properties
        if (!missingProperties.isEmpty()) {
          // Assuming missingProp already contains the composite key (objectTypeKey + ":" + propertyExternalId)
          missingProperties.forEach(missingProp -> {
            Property property = allProperties.get(missingProp);
            if (property != null) {
              PropertyValue propertyValue = new PropertyValue();
              propertyValue.setId(property.getId());
              propertyValue.setExternalId(property.getExternalId()); // Use the unmodified ID or handle this case appropriately
              propertyValue.setDisplayName(property.getDisplayName());
              propertyValue.setChoices(new ArrayList<>());
              object.getProperties().add(propertyValue);
            }
          });
          System.out.println("Adding missing properties to object: " + object.getId());
        }

        // Handle relations
        Set<String> objectRelations = object.getRelations().stream()
                .map(rel -> objectTypeKey + ":" + rel.getDisplayName() + ":" + rel.getId().toString()) // Use composite key
                .collect(Collectors.toSet());

        // Identify extra and missing relations
        Set<String> extraRelations = objectRelations.stream()
          .filter(rel -> !typeRelations.contains(rel))
          .collect(Collectors.toSet());
        Set<String> missingRelations = typeRelations.stream()
          .filter(rel -> !objectRelations.contains(rel))
          .collect(Collectors.toSet());

        // Remove extra relations
        if (!extraRelations.isEmpty()) {
          object.getRelations().removeIf(rel -> extraRelations.contains(objectTypeKey + ":" + rel.getDisplayName() + ":" + rel.getId().toString()));
          System.out.println("Removing extra relations from object: " + object.getExternalId());
        }

        // Add missing relations
        if (!missingRelations.isEmpty()) {
          // Assuming missingRel already contains the composite key (objectTypeKey + ":" + relationExternalId)
          missingRelations.forEach(missingRel -> {
            Relation relation = allRelations.get(missingRel);
            if (relation != null) {
              MappedRelation mappedRelation = new MappedRelation();
              mappedRelation.setId(relation.getId());
              mappedRelation.setExternalId(relation.getExternalId()); // Use the unmodified ID or handle this case appropriately
              mappedRelation.setDisplayName(relation.getDisplayName());
              mappedRelation.setObjectTypeId(relation.getObjectTypeId());
              mappedRelation.setFlags(relation.getFlags());
              mappedRelation.setTargets(new ArrayList<>()); // Initialize targets list
              object.getRelations().add(mappedRelation);
            }
          });
          System.out.println("Adding missing relations to object: " + object.getExternalId());
        }

        // Save the updated object
        if (!extraProperties.isEmpty() || !missingProperties.isEmpty() || !extraRelations.isEmpty() || !missingRelations.isEmpty()) {
          entityObjectRepository.save(object, objectType.getExternalId());
          System.out.println("Saved updated object: " + object.getExternalId());
        }
      }
    }
    entityObjectService.enableSearchable();
    return new BasicDto(null, "Success", null);
  }
}
