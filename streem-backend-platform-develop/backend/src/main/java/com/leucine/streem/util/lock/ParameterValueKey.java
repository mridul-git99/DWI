package com.leucine.streem.util.lock;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@EqualsAndHashCode(of = {"key"})
@Getter
public class ParameterValueKey {
  private String key;
}
