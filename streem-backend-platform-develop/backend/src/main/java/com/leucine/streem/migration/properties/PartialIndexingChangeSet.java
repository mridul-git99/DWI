package com.leucine.streem.migration.properties;

import com.leucine.streem.config.MongoConfig;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import com.leucine.streem.migration.properties.config.PropertyLoader;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PartialIndexingChangeSet implements CustomTaskChange {

  private MongoTemplate mongoTemplate;

  @Override
  public String getConfirmationMessage() {
    return "Partial index is created successfully.";
  }

  @Override
  public void setUp() {

  }

  @Override
  public void setFileOpener(ResourceAccessor resourceAccessor) {

  }

  @Override
  public ValidationErrors validate(Database database) {
    return null;
  }

  @Override
  public void execute(Database database) throws CustomChangeException {
    try {
      initializeMongoTemplate();
      setPartialIndexForAllCollections();
    } catch (Exception e) {
      throw new CustomChangeException("Error creating partial index", e);
    }
  }

  private void initializeMongoTemplate() {
    PropertyLoader propertyLoader = new PropertyLoader();
    String host = propertyLoader.getProperty("mongodb.host");
    String port = propertyLoader.getProperty("mongodb.port");
    String database = propertyLoader.getProperty("mongodb.database");
    String authDatabase = propertyLoader.getProperty("mongodb.authentication-database");
    String username = propertyLoader.getProperty("mongodb.username");
    String password = propertyLoader.getProperty("mongodb.password");
    String replicaSet = propertyLoader.getProperty("mongodb.replica-set");

    // Example: "mongodb://root:root@localhost:27017/db_name?authSource=admin&replicaSet=rs0"
    String userPassSegment = "";
    if (username != null && !username.isBlank()) {
      if (password != null && !password.isBlank()) {
        userPassSegment = "%s:%s@".formatted(username, password);
      } else {
        userPassSegment = "%s@".formatted(username);
      }
    }
    String connectionString = "mongodb://%s%s:%s/%s?authSource=%s"
      .formatted(userPassSegment, host, port, database, authDatabase);

    if (replicaSet != null && !replicaSet.isBlank()) {
      connectionString += "&replicaSet=" + replicaSet;
    }
    MongoClient mongoClient = MongoClients.create(connectionString);
    mongoTemplate = new MongoTemplate(mongoClient, database);
  }

  private void setPartialIndexForAllCollections() {
    log.info("Creating partial index for all objectTypes collections");
    mongoTemplate.getCollection("objectTypes")
      .find().
      forEach(document -> {
        String collectionName = document.getString("externalId");

        if (!mongoTemplate.collectionExists(collectionName)) {
          log.error("Collection {} does not exist - skipping", collectionName);
          return;
        }
        mongoTemplate.getCollection(collectionName).listIndexes().forEach(index -> {
          String indexName = index.getString("name");
          // Skipping the _id index
          if (!"_id_".equals(indexName)) {
            mongoTemplate.getCollection(collectionName).dropIndex(indexName);
          }
        });
        // Creating new partial index
        mongoTemplate.indexOps(collectionName).ensureIndex(
          new Index()
            .on("usageStatus", Sort.Direction.ASC)
            .on("facilityId", Sort.Direction.ASC)
            .on("externalId", Sort.Direction.ASC)
            .unique()
            .partial(PartialIndexFilter.of(Criteria.where("usageStatus").is(1)))
        );
      });
  }
}
