package com.leucine.streem.service;

import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.UserDto;
import com.leucine.streem.dto.UserGroupDto;
import com.leucine.streem.dto.request.UserGroupCreateRequest;
import com.leucine.streem.dto.request.UserGroupUpdateRequest;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface IUserGroupService {
  Page<UserGroupDto> getAll(String filters, Pageable pageable);

  Page<UserDto> getAllMembers(String id, String filters, Pageable pageable);

  BasicDto create(UserGroupCreateRequest userGroupCreateRequest) throws StreemException;

  UserGroupDto edit(String userGroupId, UserGroupUpdateRequest userGroupUpdateRequest) throws StreemException, ResourceNotFoundException;

  BasicDto archive(Long id, String reason) throws ResourceNotFoundException, StreemException;

  BasicDto unArchive(Long id, String reason) throws ResourceNotFoundException;

  UserGroupDto getById(Long id) throws ResourceNotFoundException;

}
