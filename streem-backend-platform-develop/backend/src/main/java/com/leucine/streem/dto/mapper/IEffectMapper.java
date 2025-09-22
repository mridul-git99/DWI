package com.leucine.streem.dto.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.constant.Effect;
import com.leucine.streem.dto.EffectDto;
import com.leucine.streem.dto.mapper.helper.IBaseMapper;
import com.leucine.streem.dto.request.ImportEffectRequest;
import com.leucine.streem.util.JsonUtils;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper
public interface IEffectMapper extends IBaseMapper<EffectDto, Effect> {

  default JsonNode map(String value) {
    try {
      return JsonUtils.valueToNode(value);
    } catch (Exception e) {
      throw new RuntimeException("Error converting String to JsonNode", e);
    }
  }

  default String map(JsonNode value) {
    try {
      return JsonUtils.writeValueAsString(value);
    } catch (Exception e) {
      throw new RuntimeException("Error converting JsonNode to String", e);
    }
  }
  ImportEffectRequest toImport(Effect entity);
  List<ImportEffectRequest> toImport(List<Effect> entities);

  List<EffectDto> toExport(List<Effect> effects);
}
