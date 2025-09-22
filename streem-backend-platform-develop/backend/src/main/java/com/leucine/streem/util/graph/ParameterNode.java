package com.leucine.streem.util.graph;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ParameterNode {
  private Long id;
  private List<ParameterNode> neighbors;

  public ParameterNode(Long parameterId) {
    this.id = parameterId;
    neighbors = new ArrayList<>();
  }


}
