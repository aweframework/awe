#!/bin/bash

# Get version number
NEW_VERSION=$(cat ./pom.xml | grep -o '<version>[0-9\.]*[A-Z\-]*</version>' | sed -e 's/<[\/]*version>//g' | sed ':a;N;$!ba;s/\n/ /g' | sed 's/[A-Z\-]*//g' | awk '{printf $1}')
export NEW_VERSION

# Get merge requests and transforms into releaseNotes
echo "Generating release notes for version $NEW_VERSION"
currentDate=$(date +%d/%m/%Y)
releaseNotes=$(curl --header "Authorization: Bearer ${2}" -s "${1}/merge_requests?milestone=${NEW_VERSION}&state=merged&per_page=200" | jq --raw-output 'map("- " + (if (.labels | map(select(. == "has impacts")) | length == 1) then "**[HAS IMPACTS]** " else "" end) + (.title | gsub("Resolve ";"") | gsub("\"";"")) + ". [MR #" + (.iid | tostring) + "](" + .web_url + ") (" + .merged_by.name + ")") | join("\n")')
releaseNotesText=$(printf "\n# Release notes for ${3} ${NEW_VERSION}\n*${currentDate}*\n\n${releaseNotes}\n\n")
echo "$releaseNotesText"

# Generate release notes
echo "$releaseNotesText" > ./release_notes.md && echo "Generated release notes file at ./release_notes.md"