package com.leucine.streem.collections.parser;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bson.types.ObjectId;

import javax.persistence.Id;
import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@ToString
public class SplitDataRuleDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -2308259137993543020L;

  @Id
  @JsonSerialize(using = ToStringSerializer.class)
  private ObjectId id;
  private String propertyId;
  private int startPos;
  private int endPos;
  private Type dataType;
  private String dateFormat;
  private String extractedData;
  private String result;
}
