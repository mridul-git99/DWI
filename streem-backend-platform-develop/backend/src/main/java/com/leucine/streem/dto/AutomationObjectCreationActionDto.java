package com.leucine.streem.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.constant.Type;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Used in Automation Entity "action details"
 * e.g. Object
 * {
 *   "urlPath": "/objects/partial?collection=disinfectantLots",
 *   "sortOrder": 1,
 *   "collection": "disinfectantLots",
 *   "variables": "{}",
 *   "objectTypeId": "62c0464e27dc0abb3e9501c9",
 *   "objectTypeExternalId": "disinfectantLots",
 *   "objectTypeDisplayName": "Disinfectant Lot"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutomationObjectCreationActionDto implements Serializable {
  private static final long serialVersionUID = 888696212993275302L;
  private Integer sortOrder;
  private String collection;
  private JsonNode variables;
  private String urlPath;
  private String objectTypeId;
  private String objectTypeExternalId;
  private String objectTypeDisplayName;
  private String bulkCount;
  private String referencedParameterId; // number param to get count
  private Type.SelectorType selector;
  private List<PropertyParameterMappingDto> configuration;
}
