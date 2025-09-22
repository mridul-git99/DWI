package com.leucine.streem.dto.mapper;

import com.leucine.streem.dto.TrainedUsersDto;
import com.leucine.streem.dto.mapper.helper.IBaseMapper;
import com.leucine.streem.dto.projection.TrainedUsersView;
import com.leucine.streem.model.TrainedUser;
import com.leucine.streem.service.impl.UserGroupService;
import com.leucine.streem.util.Utility;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mapper
public abstract class ITrainedUserMapper implements IBaseMapper<TrainedUsersDto, TrainedUser> {
  @Autowired
  @Lazy
  private UserGroupService userGroupService;

  public List<TrainedUsersDto> toDtoList(List<TrainedUsersView> trainedUsers, Map<String, Set<String>> userIdTaskIdMapping, Map<String, Set<String>> userGroupIdTaskIdMapping) {
    List<TrainedUsersDto> trainedUsersDtoList = new ArrayList<>();
    for (TrainedUsersView trainedUsersView : trainedUsers) {
      TrainedUsersDto trainedUsersDto = new TrainedUsersDto();
      trainedUsersDto.setUserId(trainedUsersView.getUserId());
      trainedUsersDto.setEmployeeId(trainedUsersView.getEmployeeId());
      trainedUsersDto.setFirstName(trainedUsersView.getFirstName());
      trainedUsersDto.setLastName(trainedUsersView.getLastName());
      trainedUsersDto.setEmailId(trainedUsersView.getEmailId());
      trainedUsersDto.setUserGroupId(trainedUsersView.getUserGroupId());
      trainedUsersDto.setUserGroupName(trainedUsersView.getUserGroupName());
      trainedUsersDto.setUserGroupDescription(trainedUsersView.getUserGroupDescription());
      trainedUsersDto.setStatus(trainedUsersView.getStatus());
      if (trainedUsersView.getUserId() != null) {
        Set<String> taskIds = userIdTaskIdMapping.get(trainedUsersView.getUserId());
        if (!Utility.isEmpty(taskIds)) {
          trainedUsersDto.setTaskIds(taskIds);
        }
      }
      if (trainedUsersView.getUserGroupId() != null) {
        Set<String> taskIds = userGroupIdTaskIdMapping.get(trainedUsersView.getUserGroupId());
        if (!Utility.isEmpty(taskIds)) {
          trainedUsersDto.setTaskIds(taskIds);
        }
        trainedUsersDto.setUsers(userGroupService.getAllMembers(trainedUsersView.getUserGroupId(), null, Pageable.ofSize(10)).getContent());
      }
      trainedUsersDtoList.add(trainedUsersDto);
    }
    return trainedUsersDtoList;
  }

  public List<TrainedUsersDto> toDtoList(List<TrainedUsersView> trainedUsers) {
    List<TrainedUsersDto> trainedUsersDtoList = new ArrayList<>();
    for (TrainedUsersView trainedUsersView : trainedUsers) {
      TrainedUsersDto trainedUsersDto = new TrainedUsersDto();
      trainedUsersDto.setUserId(trainedUsersView.getUserId());
      trainedUsersDto.setEmployeeId(trainedUsersView.getEmployeeId());
      trainedUsersDto.setFirstName(trainedUsersView.getFirstName());
      trainedUsersDto.setLastName(trainedUsersView.getLastName());
      trainedUsersDto.setUserGroupId(trainedUsersView.getUserGroupId());
      trainedUsersDto.setUserGroupName(trainedUsersView.getUserGroupName());
      trainedUsersDto.setUserGroupDescription(trainedUsersView.getUserGroupDescription());
      trainedUsersDtoList.add(trainedUsersDto);
    }
    return trainedUsersDtoList;
  }
}
