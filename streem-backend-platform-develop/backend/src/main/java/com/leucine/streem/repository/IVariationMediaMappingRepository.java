package com.leucine.streem.repository;

import com.leucine.streem.model.VariationMediaMapping;
import com.leucine.streem.model.compositekey.VariationMediaCompositeKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface IVariationMediaMappingRepository extends JpaRepository<VariationMediaMapping, VariationMediaCompositeKey> {
  List<VariationMediaMapping> findAllByVariationIdIn(Set<Long> variationIds);
}
