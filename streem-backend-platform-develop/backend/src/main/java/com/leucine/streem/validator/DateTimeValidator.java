package com.leucine.streem.validator;

import com.leucine.streem.constant.CollectionMisc;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;


public class DateTimeValidator implements ConstraintValidator {
  private boolean isValid;
  private final Long value;
  private final String errorMessage;
  private final CollectionMisc.DateUnit dateUnit;
  private final Long difference;
  private final CollectionMisc.PropertyValidationConstraint constraint;
  private final String timezone;

  public DateTimeValidator(Long value, Long difference, String errorMessage, CollectionMisc.DateUnit dateUnit, CollectionMisc.PropertyValidationConstraint constraint,  String timezone) {
    this.value = value;
    this.difference = difference;
    this.errorMessage = errorMessage;
    this.dateUnit = dateUnit;
    this.constraint = constraint;
    this.timezone = timezone;
  }

  @Override
  public void validate(Object value) {
    LocalDateTime compareToDate = new Timestamp(Long.parseLong((String) value) * 1000).toLocalDateTime(); //Here value will be like present time or referencedParameterInput
    LocalDateTime compareWithDate = new Timestamp(this.value * 1000).toLocalDateTime(); // Here value will be like the property input value

    // Get exact time difference in seconds
    Long actualTimeDifferenceInSeconds = ChronoUnit.SECONDS.between(compareWithDate, compareToDate);

    // Convert the required difference to seconds based on the unit
    Long requiredDifferenceInSeconds = convertDifferenceToSeconds(this.difference, dateUnit);

    switch (this.constraint) {
      case EQ -> this.isValid = Objects.equals(actualTimeDifferenceInSeconds, requiredDifferenceInSeconds);
      case LT -> this.isValid = actualTimeDifferenceInSeconds < requiredDifferenceInSeconds;
      case GT -> this.isValid = actualTimeDifferenceInSeconds > requiredDifferenceInSeconds;
      case LTE -> this.isValid = actualTimeDifferenceInSeconds <= requiredDifferenceInSeconds;
      case GTE -> this.isValid = actualTimeDifferenceInSeconds >= requiredDifferenceInSeconds;
      case NE -> this.isValid = !Objects.equals(actualTimeDifferenceInSeconds, requiredDifferenceInSeconds);
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
    LocalDateTime compareToDate = new Timestamp(Long.parseLong( "1726165800") * 1000).toLocalDateTime(); // Sep 13 20:20 PM
    LocalDateTime compareToDate1 = new Timestamp(Long.parseLong( "1726167180") * 1000).toLocalDateTime(); // Sep 13 12:23 AM
    System.out.println(compareToDate);
    System.out.println(compareToDate1);
  }

  private long convertDifferenceToSeconds(Long difference, CollectionMisc.DateUnit dateUnit) {
    //TODO: Need to Re-evaluate Years and Months Case.
    final int SECONDS_IN_MINUTE = 60;
    final int SECONDS_IN_HOUR = 3600;
    final int SECONDS_IN_DAY = 86400;
    final int SECONDS_IN_WEEK = 7 * SECONDS_IN_DAY;
    final int SECONDS_IN_YEAR = 365 * SECONDS_IN_DAY; // Approximate seconds in a year
    final long SECONDS_IN_MONTH = (long) (30.44 * SECONDS_IN_DAY); // Average seconds in a month (30.44 days)

    return switch (dateUnit) {
      case DAYS -> difference * SECONDS_IN_DAY;
      case HOURS -> difference * SECONDS_IN_HOUR;
      case WEEKS -> difference * SECONDS_IN_WEEK;
      case MINUTES -> difference * SECONDS_IN_MINUTE;
      case SECONDS -> difference;
      case YEARS -> difference * SECONDS_IN_YEAR; // Approximated Value
      case MONTHS -> difference * SECONDS_IN_MONTH; // Approximated Value
    };
  }

}
