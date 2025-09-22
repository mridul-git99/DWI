package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEventDto implements Serializable {
  private static final long serialVersionUID = 7228319057126145700L;
  private String title;
  private String checklistName;
  private long start;
  private Long end; // Use Long to allow null values
}
