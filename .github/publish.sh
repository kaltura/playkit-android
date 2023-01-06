#!/bin/bash
# This script takes care of the following tasks,
## 1. Create a new branch from the default 'develop' or 'dev' branch.
## 2. Change the version in version.gradle file.
## 3. Commit and push the version.gradle changes to remote.
## 4. Call `release_notes.sh` to generate release notes.
## 5. Publish and Close it for SONATYPE Maven publishing.
## 6. Create a TAG with release notes and push to remote.
## 7. Notify MS-Teams when the whole process it done.
## IMP: For Patch, there should always be a branch pushed to the remote
## named as `patch/v*.*.*`. Because for patch, script is checking out that branch
## and tagging it.

exit_on_failure() {
    echo "$@" 1>&2
    exit 1
}

checkout() {
    echo Checking out newtag = "$NEW_TAG", release type = "$RELEASE_TYPE"

    case $RELEASE_TYPE in
      Full)
          git checkout -b "$BRANCH_NAME" || exit_on_failure "Unable to checkout $BRANCH_NAME";;
      Patch)
          git checkout "$BRANCH_NAME" || exit_on_failure "Unable to checkout $BRANCH_NAME";;
      Update)
          git checkout -b "$BRANCH_NAME" "$PREV_TAG" || exit_on_failure "Unable to checkout $BRANCH_NAME";;
    esac
}

set_version() {
    echo Setting version of "$REPO_NAME" to "$NEW_VERSION"

    # Changing the version in version.gradle file
    perl -pi -e "s/^ext.playkitVersion.*$/ext.playkitVersion = '$NEW_VERSION'/" $VERSION_FILE
}

build() {
    chmod +x gradlew
    ./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository
}

release_and_tag() {
    git config user.name "$GH_USER_NAME"
    git config user.email "<>"

    echo Releasing version $NEW_VERSION of $REPO_NAME to GitHub
    set +e
    git add $VERSION_FILE
    git commit -m "Update version to $NEW_TAG"
    set -e
    git push origin HEAD:$BRANCH_NAME

    # Generate Release notes
    bash $RELEASE_NOTES_SCRIPT

    if [[ "$RELEASE_TYPE" = "Patch" || "$RELEASE_TYPE" = "Full" ]]; then

    releaseNotes=$(awk -v d="\\\n" '{s=(NR==1?s:s d)$0}END{print s}' $RELEASE_NOTES)


cat << EOF > ./post.json
{
      "name": "$NEW_TAG",
      "body": "$releaseNotes",
      "tag_name": "$NEW_TAG",
      "target_commitish": "$BRANCH_NAME"
}
EOF
    fi

    if [ "$RELEASE_TYPE" = "Update" ]; then
                      JSON_BODY="### Playkit Plugin Support\n\n"
                      JSON_BODY="$JSON_BODY$NEW_TAG\n\n"
          JSON_BODY="$JSON_BODY * upgrade to $NEW_TAG\n\n"
          JSON_BODY="$JSON_BODY #### Gradle\n\n"
                      JSON_BODY="$JSON_BODY * implementation 'com.kaltura.playkit:playkit"
          JSON_BODY="$NEW_VERSION"
          JSON_BODY="$JSON_BODY'"


cat << EOF > ./post.json
{
      "name": "$NEW_TAG",
      "body": "## Changes from [$PREV_TAG](https://github.com/kaltura/$REPO_NAME/releases/tag/$PREV_TAG)\n\n$JSON_BODY",
      "tag_name": "$NEW_TAG",
      "target_commitish": "$BRANCH_NAME"
}
EOF
    fi

    cat post.json

    curl --request POST \
         --url https://api.github.com/repos/kaltura/$REPO_NAME/releases \
         --header "authorization: Bearer $TOKEN" \
         --header 'content-type: application/json' \
         -d@post.json

    rm ./post.json
    rm $RELEASE_NOTES

    # delete temp branch
    #git push origin --delete $BRANCH_NAME
}

notify_teams() {
COMMIT_SHA=$(git log --pretty=format:'%h' -n 1)
COMMIT_MESSAGE=$(git log --format=%B -n 1 "$COMMIT_SHA")

color=0072C6
  curl "$TEAMS_WEBHOOK" -d @- << EOF
  {
      "@context": "https://schema.org/extensions",
      "@type": "MessageCard",
      "themeColor": "$color",
      "title": "$REPO_NAME | $BRANCH_NAME",
      "text": "🎉 Release Ready",
      "sections": [
          {
              "facts": [
                  {
                      "name": "Branch/tag",
                      "value": "$BRANCH_NAME"
                  },
                  {
                      "name": "Commit",
                      "value": "$COMMIT_SHA ($COMMIT_MESSAGE)"
                  },
                  {
                      "name": "Pusher",
                      "value": "$GH_USER_NAME"
                  },
                  {
                      "name": "Gradle line",
                      "value": "implementation 'com.kaltura.playkit:playkit:$COMMIT_SHA'"
                  }
              ]
          }
      ],
      "potentialAction": [
          {
              "@type": "OpenUri",
              "name": "GitHub Release Page",
              "targets": [
                  {
                      "os": "default",
                      "uri": "$RELEASE_URL"
                  }
              ]
          }
      ]
  }
EOF

}

  RELEASE_TYPE=$RELEASE_TYPE

  export REPO_NAME=$REPO_NAME
  MODULE_NAME=$MODULE_NAME
  VERSION_FILE=$MODULE_NAME/version.gradle

  REPO_URL=https://github.com/kaltura/$REPO_NAME
  export NEW_VERSION=$NEW_VERSION
  PREV_VERSION=$PREV_VERSION
  TOKEN=$TOKEN
  TEAMS_WEBHOOK=$TEAMS_WEBHOOK

  NEW_TAG=v$NEW_VERSION #New Version with 'v'
  export PREV_TAG=v$PREV_VERSION #Previous Version with 'v'
  RELEASE_URL=$REPO_URL/releases/tag/$NEW_TAG

  if [[ "$RELEASE_TYPE" = "Full" || "$RELEASE_TYPE" = "Update" ]]; then
  BRANCH_NAME="release/$NEW_TAG"
  fi

  if [ "$RELEASE_TYPE" = "Patch" ]; then
  BRANCH_NAME="patch/$NEW_TAG"
  fi

  export RELEASE_NOTES="release_notes.md"
  RELEASE_NOTES_SCRIPT=".github/release_notes.sh"
  GH_USER_NAME="Github Actions Bot KLTR"

  checkout
  set_version
  build
  release_and_tag
  notify_teams
