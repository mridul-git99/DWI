package com.leucine.streem.controller;

import com.leucine.streem.dto.EffectDto;
import com.leucine.streem.dto.EffectQueryRequest;
import com.leucine.streem.dto.response.Response;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/database-operations")
public interface IDatabaseOperationController {

  @PostMapping("/execute")
  Response<Object> executeEffectQuery(@RequestBody EffectQueryRequest queryDetails);
}
