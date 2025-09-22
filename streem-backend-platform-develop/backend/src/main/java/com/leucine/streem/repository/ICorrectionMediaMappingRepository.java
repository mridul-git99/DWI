package com.leucine.streem.repository;

import com.leucine.streem.model.CorrectionMediaMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Repository
@Transactional(rollbackFor = Exception.class)
public interface ICorrectionMediaMappingRepository extends JpaRepository<CorrectionMediaMapping, Long> {
  List<CorrectionMediaMapping> findByCorrectionIdAndIsOldMedia(Long correctionId, boolean isOldMedia);

  List<CorrectionMediaMapping> findByCorrectionIdAndIsOldMediaAndArchived(Long correctionId, boolean isOldMedia, boolean isArchived);

  List<CorrectionMediaMapping> findAllByCorrectionIdInAndIsOldMedia(Set<Long> correctionIds, boolean isOldMedia);

  List<CorrectionMediaMapping> findAllByCorrectionIdInAndArchived(Set<Long> correctionIds, boolean isArchived);

  List<CorrectionMediaMapping> findAllByCorrectionIdInAndIsOldMediaAndArchived(Set<Long> correctionIds, boolean isOldMedia, boolean isArchived);

  @Transactional(rollbackFor = Exception.class)
  @Modifying
  @Query(value = "UPDATE corrections_media_mapping SET archived=:isArchived WHERE corrections_id = :correctionId AND medias_id = :mediaId", nativeQuery = true)
  void updateArchiveStatusByMediaIdAndCorrectionId(@Param("correctionId") Long correctionId, @Param("mediaId") Long mediaId, @Param("isArchived") boolean isArchived);

  List<CorrectionMediaMapping> findByCorrectionIdAndArchived(Long correctionId, boolean isArchived);

  List<CorrectionMediaMapping> findAllByCorrectionId(Long id);
}
