package com.leucine.streem.controller.impl;

import com.leucine.streem.controller.IEffectController;
import com.leucine.streem.dto.ActionDto;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.EffectDto;
import com.leucine.streem.dto.request.CreateEffectRequest;
import com.leucine.streem.dto.request.ReorderEffectRequest;
import com.leucine.streem.dto.request.UpdateEffectRequest;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.service.IEffectService;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class EffectController implements IEffectController {
  private final IEffectService effectService;



  @Override
  public Response<EffectDto> updateEffect(Long effectId, UpdateEffectRequest updateEffectRequest) {
    return Response.builder().data(effectService.updateEffect(effectId, updateEffectRequest)).build();
  }

  @Override
  public Response<BasicDto> reorderEffects(ReorderEffectRequest reorderEffectRequest) {
    return Response.builder().data(effectService.reorderEffects(reorderEffectRequest)).build();
  }

  @Override
  public Response<EffectDto> getEffect(Long id) {
    return Response.builder().data(effectService.getEffect(id)).build();
  }

  @Override
  public Response<BasicDto> archiveEffect(Long effectId) throws StreemException {
    return Response.builder().data(effectService.archiveEffect(effectId)).build();
  }
}
