package com.leucine.streem.migration.mongo;



/*
  * This is a MongoDB migration script that updates all documents in all collections in a MongoDB database.
  * The script iterates over all collections and all documents in each collection.
  * For each document, it iterates over all relations and all targets in each relation.
  * If a target has an "_id" field, it adds the target's "_id" to the searchable field of the document.
  * The searchable field is a nested object where each key is a relation ID and the value is an array of target IDs.
  * The script uses the MongoDB shell syntax and should be run in the MongoDB shell.
  * The script assumes that the relations field is an array of objects with an _id field and a targets field.
  * The targets field is an array of objects with an _id field.
  * The script updates the searchable field of each document with the target IDs from the relations.
  * The script is intended to be used as a one-time migration script to update existing documents in a MongoDB database.
  * The script can be customized to fit the specific structure of the documents in the database.
  */

/*

var allCollections = db.getCollectionNames();

allCollections.forEach(function(collectionName) {
    print("Processing collection:", collectionName);  // Debug output

    db.getCollection(collectionName).find({}).forEach(function(doc) {
        print("Processing document ID:", doc._id);  // Debug output

        var updateObject = {};

        if (Array.isArray(doc.relations)) {
            doc.relations.forEach(function(relation) {
                var targets = [];

                if (Array.isArray(relation.targets)) {
                    relation.targets.forEach(function(target) {
                        if (target._id) {
                            targets.push(target._id.toString());
                        }
                    });
                }

                if (targets.length > 0) {
                    updateObject['searchable.' + relation._id.toString()] = targets;
                }
            });
        }

        if (Object.keys(updateObject).length > 0) {
            db.getCollection(collectionName).update(
                { "_id": doc._id },
                { "$set": updateObject }
            );
        }
    });
});


 */
