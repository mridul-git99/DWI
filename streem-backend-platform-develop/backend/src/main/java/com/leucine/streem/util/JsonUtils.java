package com.leucine.streem.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.leucine.streem.dto.EffectDetailsDto;
import com.leucine.streem.dto.EffectDto;
import com.leucine.streem.dto.EffectRootNode;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public final class JsonUtils {

  private static final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private JsonUtils() {
  }

  public static <T> T jsonToCollectionType(String data, Class<? extends Collection> collection, Class<?> type)
    throws IOException {
    return (T) objectMapper.readValue(data, objectMapper.getTypeFactory().constructCollectionType(collection, type));
  }

  public static <T> T jsonToCollectionType(Object data, Class<? extends Collection> collection, Class<?> type)
    throws IOException {
    return (T) objectMapper.readValue(objectMapper.writeValueAsString(data), objectMapper.getTypeFactory().constructCollectionType(collection, type));
  }


  public static JsonNode valueToNode(Object value) {
    return objectMapper.valueToTree(value);
  }

  public static JsonNode valueToNode(String content) throws JsonProcessingException {
    return objectMapper.readTree(content);
  }

  public static JsonNode createObjectNode() {
    return objectMapper.createObjectNode();
  }

  public static ArrayNode createArrayNode() {
    return objectMapper.createArrayNode();
  }

  public static <T> T readValue(String content, Class<T> valueType) throws JsonProcessingException {
    return objectMapper.readValue(content, valueType);
  }

  public static <T> T readValue(String content, TypeReference<T> valueTypeRef) throws JsonProcessingException {
    return objectMapper.readValue(content, valueTypeRef);
  }

  public static <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef) throws IllegalArgumentException {
    return objectMapper.convertValue(fromValue, toValueTypeRef);
  }

  public static String writeValueAsString(Object value) throws JsonProcessingException {
    return objectMapper.writeValueAsString(value);
  }

  public static <T> T deepCopy(T object, Class<T> clazz) {
    try {
      return objectMapper.readValue(objectMapper.writeValueAsString(object), clazz);
    } catch (Exception e) {
      throw new RuntimeException("Deep copy failed", e);
    }
  }

  public static <T> void configureCustomDeserializer(Class<T> targetType, JsonDeserializer<? extends T> deserializer) {
    SimpleModule module = new SimpleModule();
    module.addDeserializer(targetType, deserializer);
    objectMapper.registerModule(module);
  }

  public static <T> T readValue(String content, Class<T> valueType, JsonDeserializer<T> deserializer) throws IOException {
    //TODO: use factory pattern to register future deserializers
    SimpleModule module = new SimpleModule();
    module.addDeserializer(valueType, deserializer);
    ObjectMapper localMapper = objectMapper.copy();
    localMapper.registerModule(module);
    return localMapper.registerModule(module).readValue(content, valueType);
  }

  public static <T> T convertValue(Object value) {
    return objectMapper.convertValue(value, new TypeReference<>() {
    });
  }
  public static <T> T convertJsonNodeToPojo(JsonNode jsonNode, Class<T> valueType) throws JsonProcessingException {
      return objectMapper.treeToValue(jsonNode, valueType);
  }

  public static void main(String[] args) {
    try {
      // Example JSON for apiEndpoint
      String apiEndpointJson = """
                    {
                        "root": {
                            "children": [
                                {
                                    "children": [
                                        {
                                            "detail": 0,
                                            "format": 0,
                                            "mode": "normal",
                                            "style": "",
                                            "text": "https://",
                                            "type": "text",
                                            "version": 1
                                        },
                                        {
                                            "trigger": "@s",
                                            "value": "S1",
                                            "data": {
                                                "id": "565898025750470656",
                                                "uuid": "9041ee50-2854-4b37-bb4f-7b3fbda614bf",
                                                "postfix": "res.data"
                                            },
                                            "type": "custom-beautifulMention",
                                            "version": 1
                                        },
                                        {
                                            "detail": 0,
                                            "format": 0,
                                            "mode": "normal",
                                            "style": "",
                                            "text": ".com",
                                            "type": "text",
                                            "version": 1
                                        }
                                    ],
                                    "direction": "ltr",
                                    "format": "",
                                    "indent": 0,
                                    "type": "paragraph",
                                    "version": 1,
                                    "textFormat": 0,
                                    "textStyle": ""
                                }
                            ],
                            "direction": "ltr",
                            "format": "",
                            "indent": 0,
                            "type": "root",
                            "version": 1
                        }
                    }
                    """;

      // Parse JSON string into JsonNode
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode apiEndpointNode = objectMapper.readTree(apiEndpointJson);

      // Convert JsonNode to EffectRootNode
      EffectRootNode rootNode = JsonUtils.convertJsonNodeToPojo(apiEndpointNode.get("root"), EffectRootNode.class);

      // Print the result
      System.out.println("Converted POJO: " + rootNode);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }  }
