package com.leucine.streem.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.util.Utility;
import lombok.Data;

@Data
public class DateDuration {

  private final Integer day;
  private final Integer hour;
  private final Integer minute;

  @JsonCreator
  public DateDuration(
    @JsonProperty(value = "day", required = true) Integer day,
    @JsonProperty(value = "hour", required = true) Integer hour,
    @JsonProperty(value = "minute", required = true) Integer minute) throws StreemException {
    if (Utility.isNegative(day) || Utility.isNegative(hour) || Utility.isNegative(minute)) {
      throw new StreemException("Invalid Date Duration");
    }

    this.day = day;
    this.hour = hour;
    this.minute = minute;
  }
}
