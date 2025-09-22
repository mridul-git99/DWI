package com.leucine.streem.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.constant.State;
import com.leucine.streem.model.helper.PrincipalUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public final class Utility {
  public static final String SPACE = " ";
  public static final String FILE_EXTENSION_SEPARATOR = ".";
  public static final double MAX_PRECISION_LIMIT = Math.pow(10, 12); // upto 12 places is the max precision limit
  public static final int MAX_PRECISION_LIMIT_UI = 9;
  public static final String HYPHEN = "-";


  private Utility() {
  }

  public static String generateUuid() {
    return UUID.randomUUID().toString();
  }

  public static String normalizeFilePath(String originalFileName) {
    return StringUtils.cleanPath(originalFileName);
  }

  public static boolean containsText(String text) {
    return StringUtils.hasText(text);
  }

  public static String generateUnique() {
    return generateUuid().replace("-", "");
  }

  public static boolean isEmpty(String field) {
    return ObjectUtils.isEmpty(field) || field.trim().isEmpty();
  }

  public static boolean isEmpty(Object object) {
    return object instanceof JsonNode jsonNode ? jsonNode.isEmpty() : ObjectUtils.isEmpty(object);
  }

  public static boolean isEmpty(Collection<?> collection) {
    return CollectionUtils.isEmpty(collection);
  }

  public static boolean isNull(Object o) {
    return Objects.isNull(o);
  }

  public static boolean isCollection(Object o) {
    return o instanceof Collection<?>;
  }

  public static boolean isString(Object o) {
    return o instanceof String;
  }

  public static boolean trimAndCheckIfEmpty(String s) {
    return isEmpty(StringUtils.trimWhitespace(s));
  }

  public static boolean isNullOrZero(Integer o) {
    if (Objects.isNull(o)) {
      return true;
    }
    return o == 0;
  }

  public static boolean isNullOrZero(Long o) {
    if (Objects.isNull(o)) {
      return true;
    }
    return o == 0;
  }

  public static boolean isNotNull(Object o) {
    return Objects.nonNull(o);
  }

  public static String toUriString(String uri, String filters, Pageable pageable) {
    return toUriString(uri, Collections.singletonMap("filters", filters), pageable);
  }

  public static String toUriString(String uri, Map<String, Object> parameters) {
    return toUriString(uri, parameters, null);
  }

  public static String toUriString(String uri, Map<String, Object> parameters, Pageable pageable) {
    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(uri);
    parameters.forEach((k, v) -> {
      if (v instanceof Collection) {
        ((Collection<?>) v).forEach(value -> builder.queryParam(k, value));
      } else {
        builder.queryParam(k, v);
      }
    });
    if (null != pageable) {
      String sort = pageable.getSort().stream().map(s -> s.getProperty() + "," + s.getDirection()).collect(Collectors.joining("&"));
      builder.queryParam("page", pageable.getPageNumber())
        .queryParam("size", pageable.getPageSize())
        .queryParam("sort", sort);
    }
    return builder.toUriString();
  }

  public static boolean isNumeric(String str) {
    try {
      Double.parseDouble(str);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  public static boolean isNumeric(Object obj) {
    return isNumeric(obj.toString());
  }

  public static boolean isNegative(String str) {
    try {
      double number = Double.parseDouble(str);
      return number < 0;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  public static boolean nullSafeEquals(String str1, String str2) {
    str1 = ObjectUtils.getDisplayString(str1);
    str2 = ObjectUtils.getDisplayString(str2);
    return ObjectUtils.nullSafeEquals(str1, str2);
  }

  public static String getFullNameAndEmployeeIdFromPrincipalUser(PrincipalUser principalUser) {
    StringBuilder stringBuilder = new StringBuilder(principalUser.getFirstName());
    if (principalUser.getLastName() != null) {
      stringBuilder.append(SPACE).append(principalUser.getLastName());
    }
    stringBuilder.append(SPACE)
      .append("(ID:")
      .append(SPACE)
      .append(principalUser.getEmployeeId())
      .append(")");
    return stringBuilder.toString();
  }

  public static String roundUpDecimalPlaces(double value, Integer precision) {
    if (precision == null) {
      precision = MAX_PRECISION_LIMIT_UI;
    }

    BigDecimal bd = new BigDecimal(String.valueOf(value));
    bd = bd.setScale(precision, RoundingMode.HALF_UP);
    return bd.toPlainString();
  }

  public static String getFullNameAndEmployeeId(String firstName, String lastName, String employeeId) {
    firstName = firstName == null ? "" : firstName;
    lastName = lastName == null ? "" : lastName;
    employeeId = employeeId == null ? "" : employeeId;
    return firstName + " " + lastName + " " + "(ID: " + employeeId + ")";
  }

  public static Pageable appendSortByIdDesc(Pageable pageable) {
    int pageNumber = pageable.getPageNumber();
    int pageSize = pageable.getPageSize();

    Sort sortById = Sort.by("id").descending();
    Sort combinedSort = pageable.getSort().and(sortById);

    return PageRequest.of(pageNumber, pageSize, combinedSort);
  }

  public static boolean isNegative(Integer value) {
    return value < 0;
  }

  public static boolean isInteger(String value) {
    try {
      Integer.parseInt(value);
      return true;
    }
    catch (NumberFormatException nfx) {
      return false;
    }
  }

  public static Long nullIfZero(Long value) {
    if (value != null && value == 0L) {
      return null;
    }
    return value;
  }

  public static String toDisplayName(Enum<?> e) {
    return Arrays.stream(e.name().split("_"))
      .map(word -> word.charAt(0) + word.substring(1).toLowerCase())
      .collect(Collectors.joining(" "));
  }


}

