package com.leucine.streem.controller;

import com.leucine.streem.dto.ActionDto;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.EffectDto;
import com.leucine.streem.dto.request.CreateActionRequest;
import com.leucine.streem.dto.request.CreateEffectRequest;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.StreemException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/action")
public interface IActionController {
  @PostMapping
  Response<BasicDto> createAction(@RequestBody CreateActionRequest createActionRequest);

  @PatchMapping("/{id}")
  Response<ActionDto> updateAction(@PathVariable Long id, @RequestBody ActionDto actionDto);

  @PatchMapping("/{id}/archive")
  Response<BasicDto> archiveAction(@PathVariable Long id);

  @PostMapping("/{actionId}/effects")
  Response<BasicDto> createEffects(@PathVariable Long actionId, @RequestBody CreateEffectRequest createEffectRequest) throws StreemException;

  @GetMapping({"/{actionId}/effects"})
  Response<List<EffectDto>> getEffects(@PathVariable Long actionId);
}
