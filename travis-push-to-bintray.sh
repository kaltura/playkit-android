#!/bin/bash


# Only allow tags
if [ -z "$TRAVIS_TAG" ]; then 
    echo "Not a Travis tag build; skipping deploy to bintray."
    exit
fi

# Strip the "v" prefix
TAG_VERSION_NAME=${TRAVIS_TAG:1}    

# Only allow proper "digits.digits.digits" versions.
if [[ ! $TAG_VERSION_NAME =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "Not a proper version string; skipping deploy to bintray." 
    exit
fi

# Check that defined library version matches the tag.
if ! grep -q "def libVersionName = '$TAG_VERSION_NAME'" playkit/build.gradle
then
    >&2 echo "error: Library version name in build.gradle does not match tag name."
    exit 1
fi


# Assuming a successful playkit:build, create javadoc jar, sources jar, pom
./gradlew playkit:publishMavenPublicationToMavenLocal

# Upload
./gradlew bintrayUpload -PdryRun=false -PbintrayUser=$BINTRAY_USER -PbintrayKey=$BINTRAY_KEY
