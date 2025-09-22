package com.leucine.streem.controller;

import com.leucine.streem.dto.ActionDto;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.EffectDto;
import com.leucine.streem.dto.request.CreateActionRequest;
import com.leucine.streem.dto.request.CreateEffectRequest;
import com.leucine.streem.dto.request.ReorderEffectRequest;
import com.leucine.streem.dto.request.UpdateEffectRequest;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.StreemException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/effects")
public interface IEffectController {

  @PatchMapping("/{effectId}")
  Response<EffectDto> updateEffect(@PathVariable Long effectId, @RequestBody UpdateEffectRequest updateEffectRequest);

  @PatchMapping("/{effectId}/archive")
  Response<BasicDto> archiveEffect(@PathVariable Long effectId) throws StreemException;

  @GetMapping("/{id}")
  Response<EffectDto> getEffect(@PathVariable Long id);


  @PatchMapping("/reorder")
  Response<BasicDto> reorderEffects(@RequestBody ReorderEffectRequest reorderEffectRequest);


}
