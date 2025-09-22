package com.leucine.streem.collections;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.leucine.streem.constant.CollectionMisc;
import com.leucine.streem.constant.CollectionName;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
@Document(CollectionName.REPORT)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Report implements Serializable {

  @Serial
  private static final long serialVersionUID = -473861797752547603L;

  @Id
  @JsonSerialize(using = ToStringSerializer.class)
  private ObjectId id;

  // This is the name of the report
  private String name;

  @Enumerated(EnumType.STRING)
  private CollectionMisc.ReportType type;

  // This is the parameters that we pass to the report
  // for example facility id etc, so the dashboard can use facility id from the parameter
  // and use it its query
  private boolean useParameters;

  // In minutes
  private Integer tokenExpiration;

  // Example format
  // "resource": {
  //    "dashboard":#dashboardId,
  //  }
  private Map<String, Object> payload;

  private Long facilityId;

}
