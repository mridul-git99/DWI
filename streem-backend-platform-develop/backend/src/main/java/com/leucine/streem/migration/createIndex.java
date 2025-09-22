package com.leucine.streem.migration;

import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.service.impl.MongoService;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serial;
import java.io.Serializable;

@Component
@Slf4j
public class createIndex {

  private final MongoService mongoService;

  @Autowired
  public createIndex(MongoService mongoService) {
    this.mongoService = mongoService;
  }

  public BasicDto execute() throws Exception {
    log.info("Creating index for jobLogs collection");
    mongoService.createIndex("jobLogs", new Document("checklistId", -1));
    return new BasicDto(null, "Success", null);
  }
}
