package com.leucine.streem.migration;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import com.leucine.streem.migration.properties.config.PropertyLoader;

@Slf4j
@Component
public class JobLogIndexingChangeSet implements CustomTaskChange {

    private MongoTemplate mongoTemplate;

    @Override
    public void execute(Database database) throws CustomChangeException {
        try {
            log.info("[JobLogIndexingChangeSet] Starting MongoDB jobLogs index creation");
            
            initializeMongoTemplate();
            createJobLogsIndex();
            
            log.info("[JobLogIndexingChangeSet] Successfully created jobLogs index");

        } catch (Exception e) {
            log.error("[JobLogIndexingChangeSet] Failed to create index", e);
            throw new CustomChangeException("Failed to create jobLogs index", e);
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

        // Try connection without authentication first (for local development)
        String connectionString;
        try {
            // First try without authentication
            connectionString = "mongodb://%s:%s/%s".formatted(host, port, database);
            MongoClient testClient = MongoClients.create(connectionString);
            testClient.getDatabase(database).listCollectionNames().first(); // Test connection
            testClient.close();
            

        } catch (Exception e) {
            // If no-auth fails, try with authentication
            log.info("[JobLogIndexingChangeSet] No-auth connection failed, trying with authentication");
            
            String userPassSegment = "";
            if (username != null && !username.isBlank()) {
                if (password != null && !password.isBlank()) {
                    userPassSegment = "%s:%s@".formatted(username, password);
                } else {
                    userPassSegment = "%s@".formatted(username);
                }
            }
            connectionString = "mongodb://%s%s:%s/%s?authSource=%s"
                    .formatted(userPassSegment, host, port, database, authDatabase);
            
            // Only add replica set if it's explicitly configured and not empty
            if (replicaSet != null && !replicaSet.isBlank() && !replicaSet.equals("rs0")) {
                connectionString += "&replicaSet=" + replicaSet;
            }
            
            log.info("[JobLogIndexingChangeSet] Trying authenticated connection to: {}:{}/{}", host, port, database);
        }
        
        MongoClient mongoClient = MongoClients.create(connectionString);
        mongoTemplate = new MongoTemplate(mongoClient, database);
    }

    private void createJobLogsIndex() {
        // Create single compound index for checklistId + facilityId
        MongoCollection<Document> jobLogsCollection = mongoTemplate.getCollection("jobLogs");
        
        jobLogsCollection.createIndex(
            Indexes.compoundIndex(
                Indexes.ascending("checklistId"),
                Indexes.ascending("facilityId")
            ),
            new IndexOptions()
                .name("checklistId_1_facilityId_1")
                .background(false)
        );
        
        log.info("[JobLogIndexingChangeSet] Created compound index: checklistId_1_facilityId_1");
    }

    @Override
    public String getConfirmationMessage() {
        return "jobLogs MongoDB indexes created successfully";
    }

    @Override
    public void setUp() throws SetupException {
        // No setup required
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
        // Not needed
    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }
}
