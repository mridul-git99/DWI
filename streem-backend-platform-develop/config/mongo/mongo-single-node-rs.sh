#!/bin/bash

if [ ! -f "./mongo.keyfile" ]; then
    openssl rand -base64 741 > config/mongo/mongo.keyfile
    chmod 400 config/mongo/mongo.keyfile
fi

sleep 5

DOCKER_COMPOSE_FILE="mongo-compose.yml"

# Set the name of your MongoDB container
CONTAINER_NAME="mongo"

#Start MongoDB using Docker Compose
docker compose --file ./config/mongo/"$DOCKER_COMPOSE_FILE" up --detach --build

#Wait for MongoDB to initialize
sleep 5

# MongoDB replica set initiation code
REPLICA_SET_CODE="rs.initiate({
  _id: 'rs0',
  members: [
    { _id: 0, host: 'localhost:27017' }
  ]
});"

# Use docker exec to execute the MongoDB shell with the configuration file
docker exec --tty mongo mongosh -u root -p root --authenticationDatabase admin --eval "$REPLICA_SET_CODE"

# Additional optional step to check the replica set status
docker exec --tty mongo mongosh -u root -p root --authenticationDatabase admin --eval "rs.status()"
