package com.leucine.streem.controller;

import com.leucine.streem.ObjectTypeCustomView;
import com.leucine.streem.collections.CustomView;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.request.CustomViewRequest;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/custom-views")
public interface ICustomViewController {
  @GetMapping()
  Response<Page<CustomView>> getAllCustomViews(@RequestParam(name = "filters", defaultValue = "") String filters);

  @GetMapping("/{customViewId}")
  Response<CustomView> getCustomViewById(@PathVariable String customViewId) throws ResourceNotFoundException;

  @PostMapping("/checklists/{checklistId}")
  Response<CustomView> createCustomView(@PathVariable Long checklistId, @RequestBody CustomViewRequest customViewRequest) throws ResourceNotFoundException, StreemException;

  @PostMapping
  Response<CustomView> createCustomView(@RequestBody CustomViewRequest customViewRequest) throws StreemException;

  @PatchMapping("/{customViewId}")
  Response<CustomView> editCustomView(@PathVariable String customViewId, @RequestBody CustomViewRequest customViewRequest) throws ResourceNotFoundException;

  @PatchMapping("/{customViewId}/archive")
  Response<BasicDto> archiveCustomView(@PathVariable String customViewId) throws ResourceNotFoundException;

  @GetMapping("/object-types")
  Response<Page<ObjectTypeCustomView>> getAllObjectTypeCustomViews(@RequestParam(name = "filters", defaultValue = "") String filters, Pageable pageable);

  @PostMapping("/object-types/{objectTypeId}")
  Response<ObjectTypeCustomView> createObjectTypeCustomView(@PathVariable String objectTypeId, @RequestBody CustomViewRequest customViewRequest) throws ResourceNotFoundException, StreemException;

  @PatchMapping("/object-types/{customViewId}")
  Response<ObjectTypeCustomView> editObjectTypeCustomView(@PathVariable String customViewId, @RequestBody CustomViewRequest customViewRequest) throws ResourceNotFoundException;

  @PatchMapping("/object-types/{customViewId}/archive")
  Response<BasicDto> archiveObjectTypeCustomView(@PathVariable String customViewId) throws ResourceNotFoundException;

  @PatchMapping("/object-types/{customViewId}/unarchive")
  Response<BasicDto> unarchiveObjectTypeCustomView(@PathVariable String customViewId) throws ResourceNotFoundException;

}
