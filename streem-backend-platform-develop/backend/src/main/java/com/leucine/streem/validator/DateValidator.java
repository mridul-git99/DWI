package com.leucine.streem.validator;

import com.leucine.streem.constant.CollectionMisc;
import com.leucine.streem.util.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Slf4j
public class DateValidator implements ConstraintValidator {
  private boolean isValid;
  private final Long value;
  private final String errorMessage;
  private final CollectionMisc.DateUnit dateUnit;
  private final Long difference;
  private final CollectionMisc.PropertyValidationConstraint constraint;
  private final String timezone;

  public DateValidator(Long value, Long difference, String errorMessage, CollectionMisc.DateUnit dateUnit, CollectionMisc.PropertyValidationConstraint constraint, String timezone) {
    this.value = value;
    this.difference = difference;
    this.errorMessage = errorMessage;
    this.dateUnit = dateUnit;
    this.constraint = constraint;
    this.timezone = timezone;
  }

  @Override
  public void validate(Object value) {

    Long longValue = Long.parseLong(value.toString());
    LocalDateTime compareToDate = new Timestamp(DateTimeUtils.atStartOfDay(longValue, timezone) * 1000).toLocalDateTime(); //Here value will be like present time or referencedParameterInput
    LocalDateTime compareWithDate = new Timestamp(DateTimeUtils.atStartOfDay(this.value, timezone) * 1000).toLocalDateTime(); // Here value will be like the property input value

    Long actualTimeDifference = null;
    switch (dateUnit) {
      case DAYS -> actualTimeDifference = ChronoUnit.DAYS.between(compareWithDate, compareToDate);
      case HOURS -> actualTimeDifference = ChronoUnit.HOURS.between(compareWithDate, compareToDate);
      case WEEKS -> actualTimeDifference = ChronoUnit.WEEKS.between(compareWithDate, compareToDate);
      case YEARS -> actualTimeDifference = ChronoUnit.YEARS.between(compareWithDate, compareToDate);
      case MINUTES -> actualTimeDifference = ChronoUnit.MINUTES.between(compareWithDate, compareToDate);
      case SECONDS -> actualTimeDifference = ChronoUnit.SECONDS.between(compareWithDate, compareToDate);
    }

    switch (this.constraint) {
      case EQ -> this.isValid = Objects.equals(this.difference, actualTimeDifference);
      case LT -> this.isValid = actualTimeDifference < this.difference;
      case GT -> this.isValid = actualTimeDifference > this.difference;
      case LTE -> this.isValid = actualTimeDifference <= this.difference;
      case GTE -> this.isValid = actualTimeDifference >= this.difference;
      case NE -> this.isValid = !Objects.equals(this.difference, actualTimeDifference);
    }
  }

  @Override
  public boolean isValid() {
    return this.isValid;
  }

  @Override
  public String getErrorMessage() {
    return this.errorMessage;
  }

  public static void main(String[] args) {
    Long propertyValue = 1724877000L; //28th Aug 8:30 PM  [29th Aug 02:00:00] for IST
    String taskStart = "1724889660"; // 29 August 2024 00:01:00  [29th Aug 05:31:00] for IST

    DateValidator dateValidator = new DateValidator(propertyValue, 0L, "Task start date should be greater than property value", CollectionMisc.DateUnit.DAYS, CollectionMisc.PropertyValidationConstraint.LTE, null);
    long propertyInput = DateTimeUtils.atStartOfDay(1724877000L, "timezone");
    Long taskComplete = DateTimeUtils.atStartOfDay(Long.parseLong(taskStart.toString()), "timezone");
    System.out.println(propertyInput);
    System.out.println(taskComplete);
    dateValidator.validate(taskStart);
    System.out.println(dateValidator.isValid());

  }
}
