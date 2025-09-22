package com.leucine.streem.service;
import org.bson.Document;

public interface IMongoService {
  void createIndex(String collectionName, Document index);
}
