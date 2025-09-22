package com.leucine.streem.collections.parser;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.leucine.streem.constant.CollectionName;
import com.leucine.streem.dto.UserAuditDto;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.Id;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Document(CollectionName.QR_PARSER)
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@CompoundIndex(def = "{'externalId': 1, 'objectTypeId': 1}", unique = true)
//TODO: ADD state
public class QRParser implements Serializable {
  @Serial
  private static final long serialVersionUID = 1761401430449099896L;

  @Id
  @JsonSerialize(using = ToStringSerializer.class)
  private ObjectId id;
  private String displayName;
  private String externalId;
  private String objectTypeId;
  private String rawData;
  private String delimiter;
  private List<SplitDataRuleDto> rules;
  private Long createdAt;
  private Long modifiedAt;
  private UserAuditDto createdBy;
  private UserAuditDto modifiedBy;
  private int usageStatus;
}
