#!/bin/bash

VERSION=$(sed -nE 's/^\s*"version": "(.*?)",$/\1/p' package.json)
LAST_COMMIT_SHA=$(git rev-parse HEAD)
BRANCH=$(git branch --show-current)

# write git details to the file
printf "{\"version\":\"%s\",\"branch\":\"%s\",\"commit\":\"%s\"}" "$VERSION" "$BRANCH" "$LAST_COMMIT_SHA" > src/version.json
