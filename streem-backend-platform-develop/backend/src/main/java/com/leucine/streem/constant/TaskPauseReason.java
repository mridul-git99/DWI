package com.leucine.streem.constant;

public enum TaskPauseReason {
  BIO_BREAK("Bio break"),
  SHIFT_CHANGE("Shift change"),
  LUNCH_DINNER_TEA_BREAK("Lunch/ Dinner/ Tea break"),
  NO_MANPOWER("No manpower"),
  DAY_END_WEEK_OFF("Day end/ Week off"),
  BREAKDOWN_AREA_EQUIPMENT_OTHER("Breakdown (Area/ Equipment/ Other)"),
  EMERGENCY("Emergency"),
  FIRE_ALARM("Fire alarm"),
  OTHER("Other"),
  SHIFT_BREAK("Shift break"),
  EQUIPMENT_BREAKDOWN("Equipment Breakdown"),
  LUNCH_BREAK("Lunch break"),
  AREA_BREAKDOWN("Area Breakdown"),
  EMERGENCY_DRILL("Emergency Drill"),
  TASK_COMPLETED("Completed"); // This is for system, not for user


  private final String text;


  TaskPauseReason(String text) {
    this.text = text;
  }

  public String getText() {
    return text;
  }

}
