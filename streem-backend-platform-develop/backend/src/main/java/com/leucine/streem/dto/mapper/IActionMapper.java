package com.leucine.streem.dto.mapper;

import com.leucine.streem.dto.ActionDto;
import com.leucine.streem.dto.mapper.helper.IBaseMapper;
import com.leucine.streem.dto.request.ImportActionRequest;
import com.leucine.streem.model.Action;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper
public interface IActionMapper extends IBaseMapper<ActionDto, Action> {
  ImportActionRequest toImport(Action entity);
  List<ImportActionRequest> toImport(List<Action> list);

  List<ActionDto> toExport(List<Action> actions);
}
