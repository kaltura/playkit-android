#!/bin/bash

DRY_RUN=false

# Only allow tags
if [ -z "$TRAVIS_TAG" ]; then 
    echo "Not a Travis tag build; will perform a dry-run."
    DRY_RUN=true
fi

# Strip the "v" prefix
TAG_VERSION_NAME=${TRAVIS_TAG:1}    

# Only allow proper "digits.digits.digits" versions.
if [[ ! $TAG_VERSION_NAME =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "Not a proper version string; will perform a dry-run." 
    DRY_RUN=true
fi

# Check that defined library version matches the tag.
if ! grep -q "ext.playkitVersion = '$TAG_VERSION_NAME'" playkit/version.gradle
then
    echo "Library version name in build.gradle does not match tag name; will perform a dry-run."
    DRY_RUN=true
fi


# Assuming a successful playkit:build, create javadoc jar, sources jar, pom
./gradlew playkit:publishMavenPublicationToMavenLocal

# Upload
./gradlew bintrayUpload -PdryRun=$DRY_RUN -PbintrayUser=$BINTRAY_USER -PbintrayKey=$BINTRAY_KEY
