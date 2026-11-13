#!/bin/bash

# Maintenance lines (support/*) publish their docs live via bin/website-maintenance-docs.sh
# on every develop/master website build; they never cut a versioned snapshot themselves.
CURRENT_BRANCH="${CI_COMMIT_REF_NAME:-$(git rev-parse --abbrev-ref HEAD 2>/dev/null || true)}"
if [[ "${CURRENT_BRANCH}" == support/* ]]; then
    echo "maintenance line: the live ${CURRENT_BRANCH#support/} version is published from the branch itself; skipping documentation snapshot"
    exit 0
fi

# Get version number
NEW_VERSION=$(cat ./pom.xml | grep -o '<version>[0-9\.]*[A-Z\-]*</version>' | sed -e 's/<[\/]*version>//g' | sed 's/\n/ /g' | sed 's/[A-Z\-]*//g' | awk '{printf $1}')
export NEW_VERSION

# Check if new version is minor or major
[[ ${NEW_VERSION} = *?.0 ]] && is_patch=$(echo ${NEW_VERSION} | sed -n 's/[^0]*//g') || is_patch=1

if [[ ${is_patch} -gt 0 ]]
then
    # Skip generation tag
    echo "This is not a minor or major release. Skipping documentation tag"
else
    # Generate documentation tag on the new version
    echo "Generating documentation tag for version ${NEW_VERSION}..."
    cd website || { echo "Error accessing website directory"; exit 1; }
    yarn install || { echo "Error updating yarn"; exit 1; }
    yarn docusaurus docs:version "${NEW_VERSION}" || { echo "Error generating documentation"; exit 1; }
    git add .
    git commit -m "Generated documentation tag for version ${NEW_VERSION}"
    cd ..
fi