package com.leucine.streem.controller.impl;

import com.leucine.streem.controller.IActionController;
import com.leucine.streem.dto.ActionDto;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.EffectDto;
import com.leucine.streem.dto.request.CreateActionRequest;
import com.leucine.streem.dto.request.CreateEffectRequest;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.service.IActionService;
import com.leucine.streem.service.IEffectService;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@AllArgsConstructor
public class ActionController implements IActionController {
  private final IActionService actionService;
  private final IEffectService effectService;


  @Override
  public Response<BasicDto> createEffects(Long actionId, CreateEffectRequest createEffectRequest) throws StreemException {
    return Response.builder().data(effectService.createEffects(createEffectRequest, actionId)).build();
  }

  @Override
  public Response<BasicDto> archiveAction(Long id) {
    return Response.builder().data(actionService.archiveAction(id)).build();
  }

  @Override
  public Response<ActionDto> updateAction(Long id, ActionDto actionDto) {
    return Response.builder().data(actionService.updateAction(id, actionDto)).build();
  }


  @Override
  public Response<BasicDto> createAction(CreateActionRequest createActionRequest) {
    return Response.builder().data(actionService.createAction(createActionRequest)).build();
  }

  @Override
  public Response<List<EffectDto>> getEffects(Long actionId) {
    return Response.builder().data(effectService.getEffects(actionId)).build();
  }
}
