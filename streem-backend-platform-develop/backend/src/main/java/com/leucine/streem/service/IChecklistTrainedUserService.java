package com.leucine.streem.service;

import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.TrainedUserTaskMappingDto;
import com.leucine.streem.dto.TrainedUsersDto;
import com.leucine.streem.dto.projection.TrainedUsersView;
import com.leucine.streem.dto.request.TrainedUserMappingRequest;
import com.leucine.streem.exception.StreemException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IChecklistTrainedUserService {

  Page<TrainedUsersDto> getTrainedUsers(Long checklistId, Boolean isUser, Boolean isUserGroup, String query, Pageable pageable) throws StreemException;

  List<TrainedUsersView> getTrainedUsersOfFacility(Long checklistId, Long facilityId, Boolean isUser, Boolean isUserGroup, String query, int limit, int offset);

  BasicDto mapTrainedUsers(Long checklistId, TrainedUserMappingRequest trainedUserMappingRequest) throws StreemException;

  Page<TrainedUsersDto> getNonTrainedUsersOfFacility(Long checklistId, Long facilityId, Boolean isUser, Boolean isUserGroup, String query, Pageable pageable);

  Page<TrainedUsersDto> getUnTrainedUsers(Long checklistId, Boolean isUser, Boolean isUserGroup, String query, Pageable pageable);

  List<TrainedUserTaskMappingDto> getAllTrainedUserTaskMapping(Long checklistId, String query) throws StreemException;
}
