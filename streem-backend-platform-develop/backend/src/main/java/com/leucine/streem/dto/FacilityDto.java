package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacilityDto implements Serializable {
  private static final long serialVersionUID = 1905344867689971397L;

  private String id;
  private String name;
  private String dateFormat;
  private String timeFormat;
  private String dateTimeFormat;
  private String timeZone;
}

