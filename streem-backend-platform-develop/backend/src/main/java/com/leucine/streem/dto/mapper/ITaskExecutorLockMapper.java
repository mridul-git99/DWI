package com.leucine.streem.dto.mapper;

import com.leucine.streem.dto.TaskExecutorLockDto;
import com.leucine.streem.dto.mapper.helper.IBaseMapper;
import com.leucine.streem.model.TaskExecutorLock;
import org.mapstruct.Mapper;

@Mapper
public interface ITaskExecutorLockMapper extends IBaseMapper<TaskExecutorLockDto, TaskExecutorLock> {
}
