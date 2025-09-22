package com.leucine.streem.controller.impl;

import com.leucine.streem.controller.IDatabaseOperationController;
import com.leucine.streem.dto.EffectDto;
import com.leucine.streem.dto.EffectQueryRequest;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.service.IEffectService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class DatabaseOperationController implements IDatabaseOperationController {
  private final IEffectService effectService;

  @Override
  public Response<Object> executeEffectQuery(EffectQueryRequest queryDetails) {
    return Response.builder().data(effectService.executeEffectQuery(queryDetails)).build();
  }
}

