package com.leucine.streem.dto.mapper;

import com.leucine.streem.dto.TaskSchedulesDto;
import com.leucine.streem.dto.mapper.helper.IBaseMapper;
import com.leucine.streem.model.TaskSchedules;
import org.mapstruct.Mapper;

@Mapper
public interface ITaskSchedulesMapper extends IBaseMapper<TaskSchedulesDto, TaskSchedules> {
}
