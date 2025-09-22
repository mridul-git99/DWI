package com.leucine.streem.migration.mongo;



/*
 *
 */

/*
var allCollections = db.getCollectionNames();
allCollections.forEach(function(collectionName) {
    print("Processing collection:", collectionName);  // Debug output
    db.getCollection(collectionName).find({}).forEach(function(doc) {
        print("Processing document ID:", doc._id);  // Debug output
        var updateObject = {};
        if (Array.isArray(doc.properties)) {
            doc.properties.forEach(function(property) {
                var targets = [];
                if (Array.isArray(property.choices)) {
                    property.choices.forEach(function(choice) {
                        if (choice._id) {
                            targets.push(choice._id.toString());
                        }
                    });
                }
                if (targets.length > 0) {
                    updateObject['searchable.' + property._id.toString()] = targets;
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
