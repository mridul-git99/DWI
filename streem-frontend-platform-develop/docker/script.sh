#!/bin/sh

BACKEND_URL='http://localhost:8080/v1'
PUSH_TO_ACR=false
while getopts a:p: flag
do
  case "${flag}" in
    a) BACKEND_URL=${OPTARG};;
    p) PUSH_TO_ACR=${OPTARG};;
  esac
done
echo "Configured Backend Url: $BACKEND_URL, for configuring a custom Backend Url, use -a flag";

# extract version from package.json
VERSION=$(sed -nE 's/^\s*"version": "(.*?)",$/\1/p' package.json)

# run docker to build image with VERSION and latest tag
docker build --no-cache --build-arg BACKEND_URL=$BACKEND_URL --build-arg NODE_VERSION=$(cat .nvmrc | tr -cd [:digit:].) --build-arg VERSION=$VERSION --build-arg=COMMIT=$(git rev-parse HEAD) -f docker/Dockerfile --tag leucine.azurecr.io/cleen-dwi/frontend:latest --tag leucine.azurecr.io/cleen-dwi/frontend:$VERSION .

if $PUSH_TO_ACR ; then
  echo "Pushing the image to ACR..."
  # push the images to leucine.azurecr.io
  docker push leucine.azurecr.io/cleen-dwi/frontend:latest
  docker push leucine.azurecr.io/cleen-dwi/frontend:$VERSION
fi

echo "Process Completed."
