package com.leucine.streem.dto.projection;

public interface VariationView {
  String getId();

  String getName();

  String getType();

  String getDescription();

  String getParameterName();


  String getOldVariation();

  String getNewVariation();
  String getJobId();

  Integer getStageOrderTree();
  Integer getTaskOrderTree();

  String getParameterId();
  String getVariationNumber();
  String getParameterType();
  String getTaskExecutionOrderTree();
}
