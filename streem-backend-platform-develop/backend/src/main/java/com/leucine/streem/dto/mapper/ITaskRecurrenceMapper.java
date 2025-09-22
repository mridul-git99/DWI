package com.leucine.streem.dto.mapper;

import com.leucine.streem.dto.TaskRecurrenceDto;
import com.leucine.streem.dto.mapper.helper.IBaseMapper;
import com.leucine.streem.model.TaskRecurrence;
import org.mapstruct.Mapper;

@Mapper
public interface ITaskRecurrenceMapper extends IBaseMapper<TaskRecurrenceDto, TaskRecurrence> {
}

