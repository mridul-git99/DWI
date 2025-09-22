package com.leucine.streem.collections.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.model.helper.search.SearchCriteria;
import com.leucine.streem.model.helper.search.SearchFilter;
import com.leucine.streem.model.helper.search.SearchOperator;
import com.leucine.streem.util.JsonUtils;
import com.leucine.streem.util.Utility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Pattern;


@Slf4j
public class MongoFilter {
  public static final String REGEX_LIKE = "(?i)(?s)^.*%s.*$";
  private static final String REGEX_STARTS_WITH = "^(?i)%s";
  private static final String INVALID_SEARCH_OPERATION = "Invalid filter search operation : {0}";

  public static Query buildQuery(String filters) {
    return buildQuery(filters, Collections.emptyList());
  }

  public static Query buildQueryWithFacilityId(String filters, String facilityId) {
    SearchCriteria searchCriteria = new SearchCriteria();
    searchCriteria.setOp(SearchOperator.EQ.name());
    searchCriteria.setField("facilityId");
    List<Object> value = new ArrayList<>();
    value.add(facilityId);
    searchCriteria.setValues(value);

    return buildQuery(filters, List.of(searchCriteria));
  }

  public static Query buildQuery(String filters, List<SearchCriteria> additionalSearchCriterias) {
    final Query query = new Query();

    List<Criteria> criteriaList = new ArrayList<>();
    SearchOperator op = SearchOperator.AND;
    try {
      if (!Utility.isEmpty(filters)) {
        // We are converting json formatted string to json node
        // In case of {} and Json NULL, we convert this to json node and check if its an empty json node
        JsonNode jsonNode = JsonUtils.valueToNode(filters);
        if (!Utility.isEmpty(jsonNode)) {
          SearchFilter searchFilter = JsonUtils.readValue(filters, SearchFilter.class);
          op = handleEnum(searchFilter.getOp());
          query.fields().include(searchFilter.getProjection().toArray(new String[0]));
          for (SearchCriteria searchCriteria : searchFilter.getFields()) {
            addCriteria(searchCriteria, criteriaList, op);
          }
        }
      }

      if (!Utility.isEmpty(additionalSearchCriterias)) {
        for (SearchCriteria searchCriteria : additionalSearchCriterias) {
          addCriteria(searchCriteria, criteriaList, op);
        }
      }

      prepareQueryFromCriteriaList(criteriaList, op, query);

    } catch (JsonProcessingException e) {
      log.error("Incorrect Filter or Encoding", e);
    }

    return query;
  }

  public static Query prepareQueryFromCriteriaList(List<Criteria> criteriaList, SearchOperator op, Query query) {
    if (!criteriaList.isEmpty()) {
      if (op == SearchOperator.AND) {
        query.addCriteria(new Criteria().andOperator(criteriaList));
      } else if (op == SearchOperator.OR) {
        query.addCriteria(new Criteria().orOperator(criteriaList));
      } else {
        throw new IllegalArgumentException(MessageFormat.format(INVALID_SEARCH_OPERATION, op));
      }
    }
    return query;
  }

  private static void addCriteria(SearchCriteria searchCriteria, List<Criteria> criteria, SearchOperator operator) {
    if (!searchCriteria.getField().equals("usageStatus") && operator == SearchOperator.OR) {
      return;
    }
    
    // Check if values list is null or empty
    if (searchCriteria.getValues() == null || searchCriteria.getValues().isEmpty()) {
      log.warn("Empty values list for field: {}, operation: {}", searchCriteria.getField(), searchCriteria.getOp());
      return; // Skip this criteria if values list is empty
    }
    
    // Process the first value if it's a complex object
    Object firstValue = searchCriteria.getValues().get(0);
    if (firstValue instanceof Map) {
      Map<?, ?> map = (java.util.Map<?, ?>) firstValue;
      if (map.containsKey("value")) {
        // Replace the first value with the actual value
        searchCriteria.getValues().set(0, map.get("value"));
      }
    }
    
    var op = handleEnum(searchCriteria.getOp());
    switch (op) {
      case EQ -> criteria.add(Criteria.where(searchCriteria.getField()).is(searchCriteria.getValues().get(0)));
      case NE -> {
        criteria.add(Criteria.where(searchCriteria.getField()).ne(searchCriteria.getValues().get(0)));
        criteria.add(Criteria.where(searchCriteria.getField()).ne(null));
      }
      case ANY -> criteria.add(Criteria.where(searchCriteria.getField()).in(searchCriteria.getValues()));
      case ALL -> {
        criteria.add(Criteria.where(searchCriteria.getField()).all(searchCriteria.getValues()));
        criteria.add(Criteria.where(searchCriteria.getField()).size(searchCriteria.getValues().size()));
      }
      case NOT_ALL -> criteria.add(
        new Criteria().andOperator(
          Criteria.where(searchCriteria.getField()).ne(null),
          new Criteria().orOperator(
            Criteria.where(searchCriteria.getField()).not().all(searchCriteria.getValues()),
            Criteria.where(searchCriteria.getField()).not().size(searchCriteria.getValues().size())
          )
        )
      );
      case NIN -> {
        searchCriteria.getValues().add(null);
        criteria.add(Criteria.where(searchCriteria.getField()).nin(searchCriteria.getValues()));
      }
      case GT -> {
        double value = Double.parseDouble(searchCriteria.getValues().get(0).toString());
        criteria.add(Criteria.where(searchCriteria.getField()).gt(value));
      }
      case GTE, GOE -> {
        double value = Double.parseDouble(searchCriteria.getValues().get(0).toString());
        criteria.add(Criteria.where(searchCriteria.getField()).gte(value));
      }
      case LT -> {
        double value = Double.parseDouble(searchCriteria.getValues().get(0).toString());
        criteria.add(Criteria.where(searchCriteria.getField()).lt(value));
      }
      case LTE, LOE -> {
        double value = Double.parseDouble(searchCriteria.getValues().get(0).toString());
        criteria.add(Criteria.where(searchCriteria.getField()).lte(value));
      }
      case LIKE -> {
        String searchValue = searchCriteria.getValues().get(0).toString().trim();
        String[] words = searchValue.split("\\s+");
        String regex;
        if (words.length > 1) {
            regex = String.format(REGEX_LIKE, String.join("\\s+", Arrays.stream(words).map(Pattern::quote).toArray(String[]::new)));
        } else {
            regex = String.format(REGEX_LIKE, Pattern.quote(searchValue));
        }
        criteria.add(Criteria.where(searchCriteria.getField()).regex(regex));
      }
      case STARTS_WITH -> {
        String regex = String.format(REGEX_STARTS_WITH, Pattern.quote(searchCriteria.getValues().get(0).toString()));
        criteria.add(Criteria.where(searchCriteria.getField()).regex(regex));
      }
      default -> throw new IllegalArgumentException(MessageFormat.format(INVALID_SEARCH_OPERATION, op));
    }
  }

  private static SearchOperator handleEnum(String op) {
    return SearchOperator.valueOf(op);
  }

  public static List<Criteria> getCriteriaList(String filters, List<SearchCriteria> additionalSearchCriterias, String query) {
    List<Criteria> criteriaList = new ArrayList<>();
    SearchOperator op = SearchOperator.AND;
    try {
      if (!Utility.isEmpty(filters)) {
        // We are converting json formatted string to json node
        // In case of {} and Json NULL, we convert this to json node and check if its an empty json node
        JsonNode jsonNode = JsonUtils.valueToNode(filters);
        if (!Utility.isEmpty(jsonNode)) {
          SearchFilter searchFilter = JsonUtils.readValue(filters, SearchFilter.class);

          op = handleEnum(searchFilter.getOp());
          for (SearchCriteria searchCriteria : searchFilter.getFields()) {
            addCriteria(searchCriteria, criteriaList, op);
          }
        }
      }

      if (!Utility.isEmpty(additionalSearchCriterias)) {
        for (SearchCriteria searchCriteria : additionalSearchCriterias) {
          addCriteria(searchCriteria, criteriaList, op);
        }
      }

      if (!Utility.isEmpty(query)) {
        Criteria orCriteria = new Criteria().orOperator(
          Criteria.where("displayName").regex(String.format(REGEX_LIKE, query)),
          Criteria.where("externalId").regex(String.format(REGEX_LIKE, query))
        );
        criteriaList.add(orCriteria);
      }
    } catch (JsonProcessingException e) {
      log.error("Incorrect Filter or Encoding", e);
    }

    return criteriaList;
  }
}
