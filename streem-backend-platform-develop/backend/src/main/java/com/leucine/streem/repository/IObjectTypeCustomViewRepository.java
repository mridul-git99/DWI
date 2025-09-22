package com.leucine.streem.repository;

import com.leucine.streem.ObjectTypeCustomView;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IObjectTypeCustomViewRepository extends MongoRepository<ObjectTypeCustomView, String> {

  ObjectTypeCustomView findByObjectTypeIdAndLabel(String objectTypeId, String label);
}
