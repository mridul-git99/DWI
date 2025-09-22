package com.leucine.streem.util.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leucine.streem.exception.StreemException;
import lombok.SneakyThrows;

import java.lang.reflect.Field;

public class GenericNonNullFieldsDeserializer<T> extends JsonDeserializer<T> {
  private static final ObjectMapper sharedMapper = new ObjectMapper();
  private Class<T> targetClass;

  public GenericNonNullFieldsDeserializer(Class<T> targetClass) {
    this.targetClass = targetClass;
  }

  @SneakyThrows
  @Override
  public T deserialize(JsonParser jp, DeserializationContext ctxt) {
    JsonNode node = jp.getCodec().readTree(jp);

    T instance;
    try {
      instance = targetClass.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new RuntimeException("Failed to instantiate " + targetClass.getName(), e);
    }

    for (Field field : targetClass.getDeclaredFields()) {
      JsonNode fieldNode = node.get(field.getName());
      if (fieldNode == null || fieldNode.isNull() || (fieldNode.isTextual() && fieldNode.asText().isEmpty())) {
        throw new StreemException(field.getName() + " cannot be null");
      }
    }

    // Deserialization logic, potentially using ObjectMapper to set field values if not manually setting them

    return sharedMapper.treeToValue(node, targetClass);
  }
}

