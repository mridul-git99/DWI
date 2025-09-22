package com.leucine.streem.util;

import com.leucine.streem.model.Facility;

import javax.validation.constraints.NotNull;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public final class DateTimeUtils {

  public static final String DEFAULT_DATE_TIME_FORMAT = "MMM dd, yyyy, HH:mm";
  public static final String DEFAULT_DATE_FORMAT = "MMM dd, yyyy";
  private static final String ZONE_OFFSET = "+00:00";


  private DateTimeUtils() {
    throw new IllegalStateException("DateTime class");
  }

  public static LocalDateTime getLocalDataTime() {
    return LocalDateTime.now();
  }

  public static LocalDateTime getLocalDateTime(@NotNull long epochSecond) {
    return LocalDateTime.ofEpochSecond(epochSecond, 0, ZoneOffset.UTC);
  }

  public static LocalDateTime getLocalDateTime(@NotNull long epochSecond, @NotNull ZoneOffset zoneOffset) {
    return LocalDateTime.ofEpochSecond(epochSecond, 0, zoneOffset);
  }

  public static LocalDateTime getLocalDateTimeStart(@NotNull long epochSecond) {
    Instant instant = Instant.ofEpochSecond(epochSecond);
    LocalDate date = instant.atZone(ZoneId.systemDefault()).toLocalDate();
    return date.atStartOfDay();
  }

  public static LocalDateTime getLocalDateTimeEnd(@NotNull long epochSecond) {
    Instant instant = Instant.ofEpochSecond(epochSecond);
    LocalDate date = instant.atZone(ZoneId.systemDefault()).toLocalDate();
    return date.atTime(23, 59, 59);
  }

  public static String getFormattedDateTime(Long date) {
    ZoneOffset zoneOffSet = ZoneOffset.of(ZONE_OFFSET);
    LocalDateTime localDateTime = DateTimeUtils.getLocalDateTime(date, zoneOffSet);
    return localDateTime.format(DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)).toUpperCase();
  }

  public static String getFormattedDateTimeOfPattern(Long date, String dateTimeFormat) {
    dateTimeFormat = (dateTimeFormat == null) ? DEFAULT_DATE_TIME_FORMAT : dateTimeFormat;
    LocalDateTime localDateTime = DateTimeUtils.getLocalDateTime(date);
    return localDateTime.format(DateTimeFormatter.ofPattern(dateTimeFormat));
  }

  public static String getFormattedDatePattern(Long date, String dateFormat) {
    dateFormat = (dateFormat == null) ? DEFAULT_DATE_FORMAT : dateFormat;
    LocalDateTime localDateTime = DateTimeUtils.getLocalDateTime(date);
    return localDateTime.format(DateTimeFormatter.ofPattern(dateFormat));
  }

  public static long now() {
    return LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
  }

  public static long nowInMillis() {
    return System.currentTimeMillis();
  }

  public static long getEpochTime(LocalDateTime localDateTime) {
    return localDateTime.toEpochSecond(ZoneOffset.UTC);
  }

  public static long getEpochTime(Date date) {
    return date.getTime() / 1000; // getTime returns value in milliseconds
  }

  public static String getNumericMonth() {
    return LocalDate.now().format(DateTimeFormatter.ofPattern("MM"));
  }

  public static String getYear() {
    return String.valueOf(LocalDate.now().getYear());
  }

  public static long getEpochSecondsDifference(long timestamp) {
    return now() - timestamp;
  }

  public static Date getDateFromEpoch(Long epochSecond) {
    LocalDateTime localDateTime = LocalDateTime.ofEpochSecond(epochSecond, 0, ZoneOffset.UTC);
    return Date.from(localDateTime.atZone(ZoneId.of("UTC")).toInstant());
  }

  public static boolean isDateInPast(long compareWithDate) {
    LocalDateTime currentDateTime = getLocalDataTime();
    LocalDateTime compareWithDateLocalDateTime = DateTimeUtils.getLocalDateTime(compareWithDate);

    return compareWithDateLocalDateTime.isBefore(currentDateTime);
  }

  // TODO refactor
  public static Date getTheRightDate(String recurrenceExpression, Date nextFireTime) {
    // Extract the time from the expression
    String time = recurrenceExpression.substring(recurrenceExpression.indexOf("DTSTART:") + 8, recurrenceExpression.indexOf("Z"));

    // Format the current date as yyyyMMdd
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    String formattedDate = dateFormat.format(nextFireTime);

    // Concatenate the formatted date and extracted time
    String dateTimeString = formattedDate + "T" + time.substring(9);

    // Parse the concatenated date and time
    SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
    Date modifiedDate = nextFireTime;
    try {
      modifiedDate = dateTimeFormat.parse(dateTimeString);
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return modifiedDate;
  }

  public static boolean isDateWithinNext24Hours(long compareWithDate) {
    // Get the current date and time
    LocalDateTime currentDateTime = LocalDateTime.now();

    // Convert the given date to LocalDateTime using the provided getLocalDateTime() function
    LocalDateTime compareWithDateTime = getLocalDateTime(compareWithDate);

    // Calculate the difference in minutes between the two dates
    long minutesDifference = ChronoUnit.MINUTES.between(currentDateTime, compareWithDateTime);

    // Check if the difference is within the next 24 hours
    return minutesDifference >= 0 && minutesDifference < (24 * 60);
  }

  public static boolean isDateAfter(long compareWithDate, long compareToDate) {
    LocalDateTime compareWithDateTime = getLocalDateTime(compareWithDate);
    LocalDateTime compareToDateTime = getLocalDateTime(compareToDate);

    return compareWithDateTime.isAfter(compareToDateTime);
  }
  public static long getEpochFromDate(String date) {
    return LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000;
  }

  public static long nowPlusMinutesToEpochMilli(Integer minutes) {
    return Instant.now().plus((null == minutes ? 0 : minutes), ChronoUnit.MINUTES).toEpochMilli();
  }

  public static long addOffSetToTime(long timestamp, String offSet){
    Instant instant = Instant.ofEpochSecond(timestamp);
    ZoneOffset zoneOffset = ZoneOffset.of(offSet);
    Duration duration = Duration.ofSeconds(zoneOffset.getTotalSeconds());
    Instant instantWithOffset = instant.plusSeconds(duration.toSeconds());
    return instantWithOffset.getEpochSecond();
  }

  public static long convertUTCEpochToZoneEpoch(long epochTime, String ianaTimeZone) {
    return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochTime), ZoneId.of(ianaTimeZone)).toEpochSecond(ZoneOffset.UTC);
  }

  public static long convertZoneEpochToUTCEpoch(long zonedEpoch, String ianaTimeZone) {
    ZoneId zoneId = ZoneId.of(ianaTimeZone);
    ZonedDateTime zonedDateTime = Instant.ofEpochSecond(zonedEpoch).atZone(zoneId);
    return zonedDateTime.withZoneSameLocal(ZoneOffset.UTC).toEpochSecond();
  }

  public static long getLocalDateEpoch(String ianaTimeZone) {
    LocalDate localDate = LocalDate.now();
    return localDate.atStartOfDay(ZoneId.of(ianaTimeZone)).toEpochSecond();
  }

  public static long getLocalDateEpoch(Long epochTime, String ianaTimeZone) {
    Instant compareWithInstant = Instant.ofEpochSecond(epochTime);
    return compareWithInstant.atZone(ZoneId.of(ianaTimeZone)).toLocalDate().atStartOfDay(ZoneId.of(ianaTimeZone)).toEpochSecond();
  }


  public static long atStartOfDay(long timestamp, String timezone) {
    Instant instant = Instant.ofEpochSecond(timestamp);
    LocalDate date = instant.atZone(ZoneId.of(timezone)).toLocalDate();
    return date.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
  }

  public static long adjustDateByDaysAtEndOfDay(long epochSeconds, String daysOffsetStr, String timezone) {
    int daysOffset = Integer.parseInt(daysOffsetStr);
    Instant instant = Instant.ofEpochSecond(epochSeconds);
    LocalDate date = instant.atZone(ZoneId.of(timezone)).toLocalDate();
    LocalDate adjustedDate = date.plusDays(daysOffset);
    return adjustedDate.atTime(23, 59, 59).atZone(ZoneId.of(timezone)).toEpochSecond();
  }

  public static long addHoursOffsetToTime(long timestamp, String hoursOffsetStr) {
    int secondsOffset = (int) (Double.parseDouble(hoursOffsetStr) * 3600);
    Instant instant = Instant.ofEpochSecond(timestamp);
    Instant instantWithOffset = instant.plus(secondsOffset, ChronoUnit.SECONDS);
    return instantWithOffset.getEpochSecond();
  }


  public static void main(String[] args) {
    System.out.println(atStartOfDay(1724877000, "Asia/Kolkata"));
    System.out.println(timeFormatDuration(1));
  }

  public static long atEndOfDay(long timestamp) {
    Instant instant = Instant.ofEpochSecond(timestamp);
    LocalDate date = instant.atZone(ZoneId.systemDefault()).toLocalDate();
    return date.atTime(23, 59, 59).toEpochSecond(ZoneOffset.UTC);
  }

  public static long getSecondsFromDays(long days) {
    return days * 86400;
  }

  public static ZonedDateTime convertEpochToZonedDateTime(long epochMillis, String timeZone) {
    Instant instant = Instant.ofEpochSecond(epochMillis);
    ZoneId zoneId = ZoneId.of(timeZone);
    return instant.atZone(zoneId);
  }

  public static long convertZonedDateTimeToEpoch(ZonedDateTime zonedDateTime) {
    Instant instant = zonedDateTime.toInstant();
    return instant.getEpochSecond();
  }

  /**
   * Converts a duration given in seconds to the string format HH:mm:ss.
   * Hours are NOT truncated at 24, so 90061 seconds ⇒ "25:01:01".
   *
   * @param seconds duration in seconds
   * @return formatted string, e.g. "02:34:56"
   */
  public static String timeFormatDuration(long seconds) {
    long hrs  = seconds / 3600;
    long mins = (seconds % 3600) / 60;
    long secs = seconds % 60;
    return String.format("%02d:%02d:%02d", hrs, mins, secs);
  }

  public static String getFormattedDateTimeForFacility(Long epochSecond, Facility facility) {
    if (Utility.isEmpty(epochSecond) || Utility.isEmpty(facility.getTimeZone()) || Utility.isEmpty(facility.getDateTimeFormat())) {
      return null;
    }
    ZonedDateTime zonedDateTime = Instant.ofEpochSecond(epochSecond).atZone(ZoneId.of(facility.getTimeZone()));
    return zonedDateTime.format(DateTimeFormatter.ofPattern(facility.getDateTimeFormat()));
  }

  public static String getFormattedDateForFacility(Long epoch, Facility facility) {
    if (Utility.isEmpty(epoch) || Utility.isEmpty(facility.getTimeZone()) || Utility.isEmpty(facility.getDateFormat())) {
      return null;
    }
    ZonedDateTime zonedDateTime = Instant.ofEpochSecond(epoch).atZone(ZoneId.of(facility.getTimeZone()));
    return zonedDateTime.format(DateTimeFormatter.ofPattern(facility.getDateFormat()));
  }

  public static String getFormattedTimeForFacility(Long epoch, Facility facility) {
    if (Utility.isEmpty(epoch) || Utility.isEmpty(facility.getTimeZone()) || Utility.isEmpty(facility.getTimeFormat())) {
      return null;
    }
    ZonedDateTime zonedDateTime = Instant.ofEpochSecond(epoch).atZone(ZoneId.of(facility.getTimeZone()));
    return zonedDateTime.format(DateTimeFormatter.ofPattern(facility.getTimeFormat()));
  }


  public static String calculateTimezoneOffset(Facility facility) {
    ZoneId zoneId = ZoneId.of(facility.getTimeZone());
    ZonedDateTime now = ZonedDateTime.now(zoneId);
    return now.getOffset().getId();
  }

}
