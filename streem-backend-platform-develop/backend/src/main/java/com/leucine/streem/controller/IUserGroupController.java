package com.leucine.streem.controller;

import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.UserDto;
import com.leucine.streem.dto.UserGroupDto;
import com.leucine.streem.dto.request.UserGroupCreateRequest;
import com.leucine.streem.dto.request.UserGroupUpdateRequest;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.UserGroupAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/users/groups")
public interface IUserGroupController {
  @GetMapping
  @ResponseBody
  Response<Page<UserGroupDto>> getAll(@RequestParam(name = "filters", defaultValue = "") String filters, Pageable pageable);

  @GetMapping("/{id}")
  @ResponseBody
  Response<UserGroupDto> get(@PathVariable Long id) throws ResourceNotFoundException;


  @GetMapping("/{id}/users")
  Response<Page<UserDto>> getUsers(@PathVariable String id, @RequestParam(name = "filters", defaultValue = "") String filters, Pageable pageable);

  @PostMapping
  Response<BasicDto> create(@RequestBody UserGroupCreateRequest userGroupCreateRequest) throws StreemException;

  @PatchMapping("/{id}")
  Response<UserGroupDto> edit(@PathVariable String id, @RequestBody UserGroupUpdateRequest userGroupUpdateRequest) throws StreemException, ResourceNotFoundException;

  @PatchMapping("/{id}/archive")
  Response<BasicDto> archive(@PathVariable Long id, @RequestParam(name = "reason", defaultValue = "") String reason) throws StreemException, ResourceNotFoundException;

  @PatchMapping("/{id}/unarchive")
  Response<BasicDto> unarchive(@PathVariable Long id, @RequestParam(name = "reason", defaultValue = "") String reason) throws StreemException, ResourceNotFoundException;

  @GetMapping("/audits")
  @ResponseBody
  Response<List<UserGroupAudit>> getAudits(@RequestParam(name = "filters", defaultValue = "") String filters, Pageable pageable);

}
