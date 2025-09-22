package com.leucine.streem.controller.impl;

import com.leucine.streem.controller.IUserGroupController;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.UserDto;
import com.leucine.streem.dto.UserGroupDto;
import com.leucine.streem.dto.request.UserGroupCreateRequest;
import com.leucine.streem.dto.request.UserGroupUpdateRequest;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.UserGroupAudit;
import com.leucine.streem.service.IUserGroupAuditService;
import com.leucine.streem.service.IUserGroupService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class UserGroupController implements IUserGroupController {
  private final IUserGroupService userGroupService;
  private final IUserGroupAuditService userGroupAuditService;


  @Override
  public Response<List<UserGroupAudit>> getAudits(String filters, Pageable pageable) {
    return Response.builder().data(userGroupAuditService.getAudits(filters, pageable)).build();
  }

  @Override
  public Response<BasicDto> unarchive(Long id, String reason) throws StreemException, ResourceNotFoundException {
    return Response.builder().data(userGroupService.unArchive(id, reason)).build();
  }

  @Override
  public Response<BasicDto> archive(Long id, String reason) throws StreemException, ResourceNotFoundException {
    return Response.builder().data(userGroupService.archive(id, reason)).build();
  }

  @Override
  public Response<UserGroupDto> edit(String id, UserGroupUpdateRequest userGroupUpdateRequest) throws StreemException, ResourceNotFoundException {
    return Response.builder().data(userGroupService.edit(id, userGroupUpdateRequest)).build();
  }

  @Override
  public Response<BasicDto> create(UserGroupCreateRequest userGroupCreateRequest) throws StreemException {
    return Response.builder().data(userGroupService.create(userGroupCreateRequest)).build();
  }

  @Override
  public Response<Page<UserDto>> getUsers(String id, String filters, Pageable pageable) {
    return Response.builder().data(userGroupService.getAllMembers(id, filters, pageable)).build();
  }

  @Override
  public Response<UserGroupDto> get(Long id) throws ResourceNotFoundException {
    return Response.builder().data(userGroupService.getById(id)).build();
  }

  @Override
  public Response<Page<UserGroupDto>> getAll(String filters, Pageable pageable) {
    return Response.builder().data(userGroupService.getAll(filters, pageable)).build();
  }

}
