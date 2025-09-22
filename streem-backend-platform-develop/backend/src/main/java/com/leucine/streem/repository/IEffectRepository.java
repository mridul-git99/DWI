package com.leucine.streem.repository;

import com.leucine.streem.constant.Effect;
import com.leucine.streem.dto.EffectDto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IEffectRepository extends JpaRepository<Effect, Long> {
  List<Effect> findAllByActionsIdInOrderByOrderTree(List<Long> actionIds);

  List<Effect> findByActionId(Long actionId);

  List<Effect> findByActionIdIn(List<Long> actionId);
}
