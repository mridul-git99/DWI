package com.leucine.streem.service.impl;

import com.leucine.streem.service.IMongoService;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MongoService implements IMongoService {
  @Autowired
  private MongoClient mongoClient;
  @Value("${mongodb.database}")
  private String db;

  @Override
  public void createIndex(String collectionName, Document index) {
    MongoDatabase database = mongoClient.getDatabase(db);
    MongoCollection<Document> collection = database.getCollection(collectionName);

    List<Document> indexes = collection.listIndexes().into(new ArrayList<>());
    boolean indexExists = indexes.stream().anyMatch(doc -> doc.get("key").equals(index));

    if (!indexExists) {
      collection.createIndex(index);
      log.info("Index created for collection: {}", collectionName);
    }
  }
}
