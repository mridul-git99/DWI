package com.leucine.streem.service;

import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.EffectDto;
import com.leucine.streem.dto.EffectQueryRequest;
import com.leucine.streem.dto.request.CreateEffectRequest;
import com.leucine.streem.dto.request.ReorderEffectRequest;
import com.leucine.streem.dto.request.UpdateEffectRequest;
import com.leucine.streem.exception.StreemException;

import java.util.List;

public interface IEffectService {
  BasicDto createEffects(CreateEffectRequest createEffectRequest, Long actionId) throws StreemException;

  EffectDto updateEffect(Long effectId, UpdateEffectRequest updateEffectRequest);

  BasicDto archiveEffect(Long effectId) throws StreemException;

  EffectDto getEffect(Long effectId);

  BasicDto reorderEffects(ReorderEffectRequest reorderEffectRequest);

  Object executeEffectQuery(EffectQueryRequest queryDetails);

  List<EffectDto> getEffects(Long actionId);
}
