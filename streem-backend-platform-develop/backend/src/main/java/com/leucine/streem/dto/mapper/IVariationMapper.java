package com.leucine.streem.dto.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.config.MediaConfig;
import com.leucine.streem.dto.MediaDto;
import com.leucine.streem.dto.VariationDto;
import com.leucine.streem.dto.projection.VariationView;
import com.leucine.streem.model.Media;
import com.leucine.streem.model.Variation;
import com.leucine.streem.model.VariationMediaMapping;
import com.leucine.streem.util.JsonUtils;
import com.leucine.streem.util.Utility;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mapper
public abstract class IVariationMapper {
  @Autowired
  protected MediaConfig mediaConfig;


  @Mapping(target = "oldVariation", ignore = true)
  @Mapping(target = "newVariation", ignore = true)
  abstract VariationDto toDto(VariationView variationView, @Context Map<Long, List<VariationMediaMapping>> variationIdMediaMap);


  public List<VariationDto> toDtoList(List<Variation> variations) {
    if (variations == null) {
      return null;
    }

    List<VariationDto> list = new ArrayList<>();
    for (Variation variation : variations) {
      list.add(toDto(variation));
    }

    return list;
  }

  VariationDto toDto(Variation variation) {
    VariationDto variationDto = new VariationDto();
    variationDto.setNewVariation(JsonUtils.createArrayNode().add(variation.getNewDetails()));
    variationDto.setOldVariation(JsonUtils.createArrayNode().add(variation.getOldDetails()));
    variationDto.setId(variation.getIdAsString());
    variationDto.setType(variation.getType());
    variationDto.setName(variation.getName());
    variationDto.setVariationNumber(variation.getVariationNumber());
    return variationDto;
  }

  public abstract List<VariationDto> toDtoList(List<VariationView> variationView, @Context Map<Long, List<VariationMediaMapping>> variationIdMediaMap);

  @AfterMapping
  void setVariation(VariationView variationView, @MappingTarget VariationDto variationDto, @Context Map<Long, List<VariationMediaMapping>> variationIdMediaMap) {
    try {
      JsonNode oldVariation = JsonUtils.readValue(variationView.getOldVariation(), JsonNode.class);
      JsonNode newVariation = JsonUtils.readValue(variationView.getNewVariation(), JsonNode.class);

      variationDto.setNewVariation(JsonUtils.createArrayNode().add(newVariation));
      variationDto.setOldVariation(JsonUtils.createArrayNode().add(oldVariation));
      List<VariationMediaMapping> variationMediaMappings = variationIdMediaMap.get(Long.valueOf(variationView.getId()));
      if (!Utility.isEmpty(variationMediaMappings)) {
        List<Media> medias = variationIdMediaMap.get(Long.valueOf(variationView.getId())).stream().map(VariationMediaMapping::getMedia).toList();
        variationDto.setMedias(toMediaDtoList(medias));
      }

    } catch (JsonProcessingException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  List<MediaDto> toMediaDtoList(List<Media> mediaList) {
    List<MediaDto> mediaDtoList = new ArrayList<>();
    for (Media media : mediaList) {
      mediaDtoList.add(toMediaDto(media));
    }
    return mediaDtoList;
  }

  private MediaDto toMediaDto(Media media) {
    var mediaDto = new MediaDto();
    mediaDto.setId(media.getIdAsString());
    mediaDto.setType(media.getType());
    mediaDto.setName(media.getName());
    mediaDto.setDescription(media.getDescription());
    mediaDto.setLink(mediaConfig.getCdn() + java.io.File.separator + media.getRelativePath() + java.io.File.separator + media.getFilename());
    mediaDto.setArchived(media.isArchived());
    mediaDto.setFilename(media.getFilename());
    return mediaDto;
  }
}
