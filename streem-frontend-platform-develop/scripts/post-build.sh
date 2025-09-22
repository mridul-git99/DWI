#!/bin/bash

# extract version & customer_version from package.json
CUSTOMER_VERSION=$(sed -nE 's/^\s*"customer_version": "(.*?)",$/\1/p' package.json)
VERSION=$(sed -nE 's/^\s*"version": "(.*?)",$/\1/p' package.json)

# write version details to the file
printf "Product: Leucine\n\nVersion: %s" "$CUSTOMER_VERSION" > dist/version.txt

# fetch the git commit
LAST_COMMIT_SHA=$(git rev-parse HEAD)
BRANCH=$(git branch --show-current)

# write git details to the file
printf "{\"version\":\"%s\",\"branch\":\"%s\",\"commit\":\"%s\"}" "$VERSION" "$BRANCH" "$LAST_COMMIT_SHA" > dist/version.json
