package com.leucine.streem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.leucine.streem.config.MongoConfig;
import com.leucine.streem.constant.Effect;
import com.leucine.streem.constant.EffectType;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.EffectDto;
import com.leucine.streem.dto.EffectQueryRequest;
import com.leucine.streem.dto.ColumnInfoDto;
import com.leucine.streem.dto.mapper.IEffectMapper;
import com.leucine.streem.dto.request.CreateEffectRequest;
import com.leucine.streem.dto.request.ReorderEffectRequest;
import com.leucine.streem.dto.request.UpdateEffectRequest;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.Action;
import com.leucine.streem.model.User;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.repository.IActionRepository;
import com.leucine.streem.repository.IEffectRepository;
import com.leucine.streem.repository.IUserRepository;
import com.leucine.streem.service.IEffectService;
import com.leucine.streem.util.DateTimeUtils;
import com.leucine.streem.util.JsonUtils;
import lombok.AllArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class EffectService implements IEffectService {

  private final IActionRepository actionRepository;
  private final IUserRepository userRepository;
  private final IEffectRepository effectRepository;
  private final IEffectMapper effectMapper;
  private final JdbcTemplate jdbcTemplate;
  private final MongoTemplate mongoTemplate;
  private final MongoConfig mongoConfig;

  @Override
  public BasicDto reorderEffects(ReorderEffectRequest reorderEffectRequest) {
    List<Effect> effectList = new ArrayList<>();
    reorderEffectRequest.getEffectOrder().forEach((key, value) -> {
      Effect effect = effectRepository.getReferenceById(key);
      effect.setOrderTree(value);
      effectList.add(effect);
    });
    effectRepository.saveAll(effectList);
    BasicDto basicDto = new BasicDto();
    basicDto.setMessage("Success");
    return basicDto;
  }

  @Override
  public EffectDto getEffect(Long effectId) {
    Effect effect = effectRepository.getReferenceById(effectId);
    return effectMapper.toDto(effect);

  }

  @Override
  @Transactional(rollbackOn = Exception.class)
  public BasicDto archiveEffect(Long effectId) throws StreemException {
    Effect effect = effectRepository.findById(effectId)
      .orElseThrow(() -> new StreemException("Effect not found"));

    effect.setArchived(true);
    effectRepository.save(effect);

    BasicDto basicDto = new BasicDto();
    basicDto.setMessage("Success");
    return basicDto;
  }

  @Override
  @Transactional(rollbackOn = Exception.class)
  public EffectDto updateEffect(Long effectId, UpdateEffectRequest updateEffectRequest) {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User user = userRepository.getReferenceById(principalUser.getId());
    Effect effect = effectRepository.getReferenceById(effectId);

    effect.setName(updateEffectRequest.getName());
    effect.setDescription(updateEffectRequest.getDescription());
    effect.setOrderTree(updateEffectRequest.getOrderTree());
    effect.setQuery(updateEffectRequest.getQuery());
    effect.setApiEndpoint(updateEffectRequest.getApiEndpoint());
    effect.setApiMethod(updateEffectRequest.getApiMethod());
    effect.setApiHeaders(updateEffectRequest.getApiHeaders());
    effect.setApiPayload(updateEffectRequest.getApiPayload());
    effect.setJavascriptEnabled(updateEffectRequest.isJavascriptEnabled());
    effect.setModifiedAt(DateTimeUtils.now());
    effect.setModifiedBy(user);
    Effect savedEffect = effectRepository.save(effect);
    return effectMapper.toDto(savedEffect);

  }

  @Override
  @Transactional(rollbackOn = Exception.class)
  public BasicDto createEffects(CreateEffectRequest createEffectRequest, Long actionId) throws StreemException {

    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User user = userRepository.getReferenceById(principalUser.getId());
    Action action = actionRepository.findById(actionId)
      .orElseThrow(() -> new StreemException("Action not found"));

    Effect effect = new Effect();
    effect.setEffectType(createEffectRequest.getEffectType());
    effect.setName(createEffectRequest.getName());
    effect.setDescription(createEffectRequest.getDescription());
    effect.setOrderTree(createEffectRequest.getOrderTree());
    effect.setQuery(createEffectRequest.getQuery());
    effect.setApiEndpoint(createEffectRequest.getApiEndpoint());
    effect.setApiMethod(createEffectRequest.getApiMethod());
    effect.setApiHeaders(createEffectRequest.getApiHeaders());
    effect.setApiPayload(createEffectRequest.getApiPayload());
    effect.setAction(action);
    effect.setJavascriptEnabled(createEffectRequest.isJavascriptEnabled());
    effect.setCreatedAt(DateTimeUtils.now());
    effect.setModifiedAt(DateTimeUtils.now());
    effect.setCreatedBy(action.getCreatedBy());
    effect.setModifiedBy(user);
    effectRepository.save(effect);

    BasicDto basicDto = new BasicDto();
    basicDto.setId(effect.getIdAsString());
    basicDto.setMessage("success");
    return basicDto;
  }

  @Override
  public List<EffectDto> getEffects(Long actionId) {
    List<Effect> effects = effectRepository.findByActionId(actionId);
    return effects.stream()
      .filter(effect -> !effect.isArchived())
      .map(effectMapper::toDto)
      .collect(Collectors.toList());
  }

//  @Override
//  public Object executeEffectQuery(EffectQueryRequest queryDetails) {
//    if (queryDetails.getEffectType() == EffectType.MONGO_QUERY) {
//      String uri = mongoConfig.buildUri();
//      String databaseName = mongoConfig.getDatabase();
//      String query = queryDetails.getQuery();
//      try (MongoClient mongoClient = MongoClients.create(uri)) {
//        MongoDatabase database = mongoClient.getDatabase(databaseName);
//
//        Document command = new Document("$eval", query);
//
//        Document result = database.runCommand(command);
//        System.out.println("Query Result: " + result.toJson());
//      } catch (Exception e) {
//        e.printStackTrace();
//      }
//    }
//    else if (queryDetails.getEffectType() == EffectType.SQL_QUERY) {
//      String query = queryDetails.getQuery();
//      if (query.trim().toLowerCase().startsWith("select")) {
//        return jdbcTemplate.queryForList(query);
//      } else {
//        return jdbcTemplate.update(query);
//      }
//
//    }
//  }


  public Object executeEffectQuery(EffectQueryRequest queryDetails) {
    if (queryDetails.getEffectType() == EffectType.MONGO_QUERY) {
      return executeMongoQuery(queryDetails.getQuery());
    }

    if (queryDetails.getEffectType() == EffectType.SQL_QUERY) {
      return executeSqlQuery(queryDetails.getQuery());
    }

    throw new IllegalArgumentException("Unsupported EffectType: " + queryDetails.getEffectType());
  }

  private Object executeMongoQuery(String query) {
    String collectionName = extractCollectionNameFromQuery(query);
    if (collectionName == null || collectionName.isEmpty()) {
      throw new IllegalArgumentException("Collection name is missing in the query");
    }

    MongoOperation operation = extractOperation(query);
    String operationParams = extractOperationParameters(query, operation);

    return mongoTemplate.execute(collectionName, collection -> {
      ObjectMapper objectMapper = new ObjectMapper();

      switch (operation) {
        case FIND:
          Document findFilter = operationParams.isEmpty() ? new Document() : Document.parse(operationParams);
          List<Document> findResults = collection.find(findFilter).into(new ArrayList<>());
          return convertToJsonResponse(findResults, objectMapper);

        case INSERT:
          Document insertDoc = Document.parse(operationParams);
          collection.insertOne(insertDoc);
          ObjectNode insertResult = objectMapper.createObjectNode();
          insertResult.put("message", "Document inserted successfully");
          return insertResult;

        case UPDATE:
          String[] updateParts = operationParams.split("\\},\\s*\\{");
          if (updateParts.length != 2) {
            throw new IllegalArgumentException("Update operation requires filter and update documents");
          }
          Document updateFilter = Document.parse(updateParts[0] + "}");
          Document updateDoc = Document.parse("{" + updateParts[1]);
          collection.updateMany(updateFilter, updateDoc);
          ObjectNode updateResult = objectMapper.createObjectNode();
          updateResult.put("message", "Documents updated successfully");
          return updateResult;

        case DELETE:
          Document deleteFilter = Document.parse(operationParams);
          collection.deleteMany(deleteFilter);
          ObjectNode deleteResult = objectMapper.createObjectNode();
          deleteResult.put("message", "Documents deleted successfully");
          return deleteResult;

        case AGGREGATE:
          List<Document> pipeline = new ArrayList<>();
          JsonNode pipelineArray = null;
          try {
            pipelineArray = JsonUtils.valueToNode(operationParams);
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
          for (JsonNode stage : pipelineArray) {
            pipeline.add(Document.parse(stage.toString()));
          }
          List<Document> aggregateResults = collection.aggregate(pipeline).into(new ArrayList<>());
          return convertToJsonResponse(aggregateResults, objectMapper);

        default:
          throw new IllegalArgumentException("Unsupported operation: " + operation);
      }
    });
  }

  private enum MongoOperation {
    FIND, INSERT, UPDATE, DELETE, AGGREGATE
  }

  private String extractCollectionNameFromQuery(String query) {
    Pattern pattern = Pattern.compile("getCollection\\(\"(.*?)\"\\)");
    Matcher matcher = pattern.matcher(query);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }

  private MongoOperation extractOperation(String query) {
    if (query.contains(".find(")) return MongoOperation.FIND;
    if (query.contains(".insertOne(")) return MongoOperation.INSERT;
    if (query.contains(".updateMany(")) return MongoOperation.UPDATE;
    if (query.contains(".deleteMany(")) return MongoOperation.DELETE;
    if (query.contains(".aggregate(")) return MongoOperation.AGGREGATE;
    throw new IllegalArgumentException("Unsupported MongoDB operation in query");
  }

  private String extractOperationParameters(String query, MongoOperation operation) {
    String operationString = switch (operation) {
      case FIND -> "find";
      case INSERT -> "insertOne";
      case UPDATE -> "updateMany";
      case DELETE -> "deleteMany";
      case AGGREGATE -> "aggregate";
    };

    Pattern pattern = Pattern.compile("\\." + operationString + "\\((.*?)\\)(?=\\s*;|$)");
    Matcher matcher = pattern.matcher(query);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return "{}";
  }

  private JsonNode convertToJsonResponse(List<Document> documents, ObjectMapper objectMapper) {
    if (!documents.isEmpty()) {
      ArrayNode arrayNode = objectMapper.createArrayNode();
      for (Document document : documents) {
        try {
          JsonNode jsonNode = JsonUtils.valueToNode(objectMapper.readValue(document.toJson(), Map.class));
          arrayNode.add(jsonNode);
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
      }
      return arrayNode;
    } else {
      ObjectNode emptyResult = objectMapper.createObjectNode();
      emptyResult.put("message", "No documents found");
      return emptyResult;
    }
  }

  private Object executeSqlQuery(String query) {
    try {
      if (query.trim().toLowerCase().startsWith("select")) {
        List<Map<String, Object>> result = jdbcTemplate.queryForList(query);
        for (Map<String, Object> row : result) {
          for (Map.Entry<String, Object> entry : row.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();
            try {
              JsonNode jsonNode = JsonUtils.valueToNode(fieldValue);
              if (jsonNode.getNodeType() == JsonNodeType.OBJECT) {
                var type = jsonNode.get("type").asText();
                var value = jsonNode.get("value").asText();
                jsonNode = JsonUtils.valueToNode(value);
                ColumnInfoDto columnInfoDto = new ColumnInfoDto(type, jsonNode);
                row.put(fieldName, columnInfoDto);
              }
            } catch (Exception e) {
              return "Error processing column " + e.getMessage();
            }
          }
        }
        return result;
      } else {
        return jdbcTemplate.update(query);
      }
    } catch (Exception e) {
      e.printStackTrace();
      return "Error executing SQL query: " + e.getMessage();
    }
  }
}
