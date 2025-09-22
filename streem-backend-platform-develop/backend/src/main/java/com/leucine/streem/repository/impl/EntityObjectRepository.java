package com.leucine.streem.repository.impl;

import com.leucine.streem.collections.EntityObject;
import com.leucine.streem.collections.helper.MongoFilter;
import com.leucine.streem.collections.partial.PartialEntityObject;
import com.leucine.streem.constant.CollectionKey;
import com.leucine.streem.model.helper.search.SearchCriteria;
import com.leucine.streem.model.helper.search.SearchOperator;
import com.leucine.streem.repository.IEntityObjectRepository;
import com.leucine.streem.util.Utility;
import com.mongodb.client.model.IndexOptions;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.leucine.streem.collections.helper.MongoFilter.REGEX_LIKE;

@Repository
@RequiredArgsConstructor
public class EntityObjectRepository implements IEntityObjectRepository {
  private final MongoTemplate mongoTemplate;

  @Override
  public List<EntityObject> findAll(String collectionName) {
    return mongoTemplate.findAll(EntityObject.class, collectionName);
  }

  @Override
  public Optional<EntityObject> findById(String collectionName, String id) {
    Query query = new Query();
    query.addCriteria(Criteria.where(CollectionKey.ID).is(id));
    return Optional.ofNullable(mongoTemplate.findOne(query, EntityObject.class, collectionName));
  }

  @Override
  public List<EntityObject> findByObjectTypeId(String collectionName, String id) {
    Query query = new Query();
    query.addCriteria(Criteria.where(CollectionKey.OBJECT_TYPE_ID).is(new ObjectId(id)));
    return mongoTemplate.find(query, EntityObject.class, collectionName);
  }

  @Override
  public PartialEntityObject findPartialById(String collectionName, String id) {
    Query query = new Query();
    query.fields().include(CollectionKey.ID, CollectionKey.COLLECTION, CollectionKey.EXTERNAL_ID, CollectionKey.DISPLAY_NAME);
    query.addCriteria(Criteria.where(CollectionKey.ID).is(id));
    return mongoTemplate.findOne(query, PartialEntityObject.class, collectionName);
  }

  @Override
  public List<EntityObject> findByIds(String collectionName, List<String> ids) {
    Query query = new Query();
    query.addCriteria(Criteria.where(CollectionKey.ID).in(ids));
    return mongoTemplate.find(query, EntityObject.class, collectionName);
  }

  @Override
  public List<PartialEntityObject> findPartialByIds(String collectionName, List<String> ids) {
    Query query = new Query();
    query.fields().include(CollectionKey.ID, CollectionKey.COLLECTION, CollectionKey.EXTERNAL_ID, CollectionKey.DISPLAY_NAME);
    query.addCriteria(Criteria.where(CollectionKey.ID).in(ids));
    return mongoTemplate.find(query, PartialEntityObject.class, collectionName);
  }

  @Override
  public List<PartialEntityObject> findPartialByIdsAndUsageStatus(String collectionName, List<String> ids, int usageStatus) {
    Query query = new Query();
    query.fields().include(CollectionKey.ID, CollectionKey.COLLECTION, CollectionKey.EXTERNAL_ID, CollectionKey.DISPLAY_NAME);
    query.addCriteria(Criteria.where(CollectionKey.ID).in(ids))
      .addCriteria(Criteria.where(CollectionKey.USAGE_STATUS).is(usageStatus));
    return mongoTemplate.find(query, PartialEntityObject.class, collectionName);
  }

  @Override
  public Page<EntityObject> findAllByUsageStatus(String collectionName, int usageStatus, String propertyExternalId, String propertyValue, Long facilityId, String filters, Pageable pageable) {
    List<SearchCriteria> criteriaList = getSearchCriteria(usageStatus, propertyExternalId, propertyValue, facilityId);
    Query filteredQuery = MongoFilter.buildQuery(filters, criteriaList);
    long count = mongoTemplate.count(filteredQuery, collectionName);
    filteredQuery.with(pageable);
    var entityObjects = mongoTemplate.find(filteredQuery, EntityObject.class, collectionName);
    return PageableExecutionUtils.getPage(entityObjects, pageable, () -> count);
  }

  @Override
  public Page<PartialEntityObject> findPartialByUsageStatus(String collectionName, int usageStatus, String propertyExternalId, String propertyValue, Long facilityId, String filters, String query, Pageable pageable) {
    List<SearchCriteria> criteriaList = getSearchCriteria(usageStatus, propertyExternalId, propertyValue, facilityId);
    List<Criteria> combinedCriteriaList = MongoFilter.getCriteriaList(filters, criteriaList, query);

    if (!Utility.isEmpty(query)) {
      Criteria orCriteria = new Criteria().orOperator(
        Criteria.where("displayName").regex(String.format(REGEX_LIKE, query)),
        Criteria.where("externalId").regex(String.format(REGEX_LIKE, query))
      );
      combinedCriteriaList.add(orCriteria);
    }

    Criteria combinedCriteria = new Criteria().andOperator(combinedCriteriaList.toArray(new Criteria[0]));
    Query filteredQuery = new Query(combinedCriteria);
    filteredQuery.fields().include(CollectionKey.ID, CollectionKey.COLLECTION, CollectionKey.EXTERNAL_ID, CollectionKey.DISPLAY_NAME);

    long count = mongoTemplate.count(filteredQuery, collectionName);
    filteredQuery.with(pageable);
    var entityObjects = mongoTemplate.find(filteredQuery, PartialEntityObject.class, collectionName);
    return PageableExecutionUtils.getPage(entityObjects, pageable, () -> count);
  }

  @Override
  public List<EntityObject> saveAll(List<EntityObject> entityObjects, String collectionName) {
    List<EntityObject> savedObjects = new ArrayList<>();
    for (EntityObject entityObject : entityObjects) {
      EntityObject savedEntityObject = save(entityObject, collectionName);
      savedObjects.add(savedEntityObject);
    }
    return savedObjects;
  }

  @Override
  public Optional<EntityObject> findByExternalIdAndUsageStatusAndFacilityId(String collectionName, String externalId, int usageStatus, String facilityId) {
    Query query = new Query();
    query.addCriteria(Criteria.where(CollectionKey.EXTERNAL_ID).is(externalId))
         .addCriteria(Criteria.where(CollectionKey.USAGE_STATUS).is(usageStatus))
         .addCriteria(Criteria.where(CollectionKey.FACILITY_ID).is(facilityId));
    return Optional.ofNullable(mongoTemplate.findOne(query, EntityObject.class, collectionName));
  }

  @Override
  public List<EntityObject> findAllByUsageStatusAndFacilityId(String collectionName, int usageStatus, String facilityId) {
    Query query = new Query();
    query.addCriteria(Criteria.where(CollectionKey.USAGE_STATUS).is(usageStatus))
         .addCriteria(Criteria.where(CollectionKey.FACILITY_ID).is(facilityId))
         .with(Sort.by(Sort.Direction.DESC, CollectionKey.ID));
    return mongoTemplate.find(query, EntityObject.class, collectionName);
  }

  @Override
  public EntityObject save(EntityObject entityObject, String collectionName) {
    Document fields = new Document();
    fields.put(CollectionKey.USAGE_STATUS, 1);
    fields.put(CollectionKey.FACILITY_ID, 1);
    fields.put(CollectionKey.EXTERNAL_ID, 1);

    // Create partial filter for usageStatus = 1
    Document partialFilterExpression = new Document();
    partialFilterExpression.put(CollectionKey.USAGE_STATUS, 1);

    // Create index options
    IndexOptions indexOptions = new IndexOptions()
      .unique(true)
      .partialFilterExpression(partialFilterExpression);

    // Create and ensure the index
    mongoTemplate.getCollection(collectionName)
      .createIndex(fields, indexOptions);

    return mongoTemplate.save(entityObject, collectionName);
  }

  private static List<SearchCriteria> getSearchCriteria(int usageStatus, String propertyExternalId, String propertyValue, Long facilityId) {
    List<SearchCriteria> criteriaList = new ArrayList<>();
    SearchCriteria usageStatusCriteria = new SearchCriteria();
    usageStatusCriteria.setOp(SearchOperator.EQ.name());
    usageStatusCriteria.setField(CollectionKey.USAGE_STATUS);
    usageStatusCriteria.setValues(List.of(usageStatus));
    criteriaList.add(usageStatusCriteria);

    if (!Utility.isEmpty(propertyExternalId) && !Utility.isEmpty(propertyValue)) {
      SearchCriteria propertyExternalIdSearchCriteria = new SearchCriteria();
      propertyExternalIdSearchCriteria.setOp(SearchOperator.EQ.name());
      propertyExternalIdSearchCriteria.setField(CollectionKey.PROPERTY_EXTERNAL_ID);
      propertyExternalIdSearchCriteria.setValues(List.of(propertyExternalId));

      criteriaList.add(propertyExternalIdSearchCriteria);
    }
    if (facilityId != null) {
      SearchCriteria facilitySearchCriteria = new SearchCriteria();
      facilitySearchCriteria.setOp(SearchOperator.EQ.name());
      facilitySearchCriteria.setField(CollectionKey.FACILITY_ID);
      facilitySearchCriteria.setValues(List.of(String.valueOf(facilityId)));
      criteriaList.add(facilitySearchCriteria);
    }
    return criteriaList;
  }
}
