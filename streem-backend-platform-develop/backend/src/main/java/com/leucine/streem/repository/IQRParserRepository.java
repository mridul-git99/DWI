package com.leucine.streem.repository;

import com.leucine.streem.collections.parser.QRParser;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IQRParserRepository extends MongoRepository<QRParser, String> {
  QRParser findByObjectTypeIdAndExternalId(String objectTypeId, String externalId);
}
