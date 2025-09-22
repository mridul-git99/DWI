package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.model.TempParameterValueMediaMapping;
import com.leucine.streem.model.compositekey.TempParameterValueMediaCompositeKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

public interface ITempParameterMediaMappingRepository extends JpaRepository<TempParameterValueMediaMapping, TempParameterValueMediaCompositeKey> {

  void deleteAllByTempParameterValueIdIn(List<Long> tempParameterValueIds);
  @Modifying(clearAutomatically = true)
  @Transactional(rollbackFor = Exception.class)
  @Query(value = Queries.ARCHIVE_TEMP_PARAMETER_MEDIA_MAPPING_BY_TEMP_PARAMETER_VALUE_ID_AND_MEDIA_IDS, nativeQuery = true)
  void archiveMediaByTempParameterValueIdAndMediaIdIn(@Param("tempParameterValueId") Long tempParameterValueId, @Param("archivedMediaIds") Set<Long> archivedMediaIds);
}
