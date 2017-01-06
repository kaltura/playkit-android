#!/bin/bash


# Only allow tags
if [ -z "$TRAVIS_TAG" ]; then exit; fi


# Check that defined library version matches the tag.
mismatch() {
    >&2 echo "error: version name does not match tag name"
    exit 1
}

TAG_VERSION_NAME=${TRAVIS_TAG:1}    # Strip the "v" prefix
grep -q "def libVersionName = '$TAG_VERSION_NAME'" playkit/build.gradle || mismatch


# Assuming a successful playkit:build

# Create javadoc jar, sources jar, pom
./gradlew playkit:publishMavenPublicationToMavenLocal

# Upload
./gradlew bintrayUpload -PdryRun=false -PbintrayUser=$BINTRAY_USER -PbintrayKey=$BINTRAY_KEY
