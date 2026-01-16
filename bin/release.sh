#!/bin/bash

# -------------------------------
# Configuración
# -------------------------------
GITLAB_API="$1"   # URL base del proyecto, ej: https://gitlab.com/api/v4/projects/<project_id>
TOKEN="$2"
RELEASE_NAME="$3"

HEADERS="--header Authorization: Bearer ${TOKEN}"

# -------------------------------
# 1. Obtener versión desde pom.xml
# -------------------------------
NEW_VERSION=$(grep -o '<version>[0-9\.]*[A-Z\-]*</version>' ./pom.xml \
    | sed -e 's/<[\/]*version>//g' \
    | sed ':a;N;$!ba;s/\n/ /g' \
    | sed 's/[A-Z\-]*//g' \
    | awk '{printf $1}')
echo "Detected version: $NEW_VERSION"

# -------------------------------
# 2. Comprobar issues pendientes en el milestone actual
# -------------------------------
pending_issues=$(curl -s "${GITLAB_API}/issues_statistics?milestone=${NEW_VERSION}" $HEADERS \
    | jq --raw-output '.statistics.counts.opened')

if [[ ${pending_issues} -gt 0 ]]; then
    echo "Breaking release, there are still ${pending_issues} issues in milestone ${NEW_VERSION}."
    exit 1
fi

# -------------------------------
# 3. Obtener milestones y detectar anterior y siguiente
# -------------------------------
milestones_json=$(curl -s "${GITLAB_API}/milestones?order_by=due_date&sort=asc&per_page=100" $HEADERS)
previous_milestone=$(echo "$milestones_json" | jq -r --arg VER "$NEW_VERSION" '.[] | select(.title != $VER) | .title' | tail -n1)
next_version="${NEW_VERSION}.next"

# Crear siguiente milestone si no existe
next_milestone_id=$(echo "$milestones_json" | jq -r --arg NV "$next_version" '.[] | select(.title==$NV) | .id')
if [[ -z "$next_milestone_id" || "$next_milestone_id" == "null" ]]; then
    echo "Creating next milestone: $next_version"
    next_milestone_id=$(curl -s -X POST "${GITLAB_API}/milestones" \
        -H "Content-Type: application/json" \
        $HEADERS \
        -d "{\"title\": \"$next_version\"}" \
        | jq -r '.id')
fi

current_milestone_id=$(echo "$milestones_json" | jq -r --arg NV "$NEW_VERSION" '.[] | select(.title==$NV) | .id')

# -------------------------------
# Función para paginar requests GitLab
# -------------------------------
gitlab_paginate() {
    local url="$1"
    local page=1
    local per_page=100
    local results=()

    while true; do
        resp=$(curl -sI "$url&page=$page&per_page=$per_page" $HEADERS)
        next_page=$(echo "$resp" | grep -i "X-Next-Page" | awk '{print $2}' | tr -d '\r')
        data=$(curl -s "$url&page=$page&per_page=$per_page" $HEADERS | jq '.')
        results+=("$data")
        if [[ -z "$next_page" || "$next_page" == "null" ]]; then
            break
        fi
        page=$next_page
    done
    echo "${results[@]}" | jq -s 'add'
}

# -------------------------------
# 4. Mover MRs del milestone anterior al actual y sus issues
# -------------------------------
if [[ -n "$previous_milestone" ]]; then
    echo "Updating merge requests from previous milestone: $previous_milestone"
    mrs=$(gitlab_paginate "${GITLAB_API}/merge_requests?milestone=${previous_milestone}&state=merged")
    for mr_id in $(echo "$mrs" | jq -r '.[].iid'); do
        # Actualizar milestone del MR
        curl -s -X PUT "${GITLAB_API}/merge_requests/$mr_id" \
            -H "Content-Type: application/json" \
            $HEADERS \
            -d "{\"milestone_id\": $current_milestone_id}" > /dev/null

        # Actualizar issues relacionados al mismo milestone
        issues=$(curl -s "${GITLAB_API}/merge_requests/$mr_id/closes_issues" $HEADERS)
        for issue_id in $(echo "$issues" | jq -r '.[].iid'); do
            curl -s -X PUT "${GITLAB_API}/issues/$issue_id" \
                -H "Content-Type: application/json" \
                $HEADERS \
                -d "{\"milestone_id\": $current_milestone_id}" > /dev/null
        done
    done
fi

# -------------------------------
# 5. Mover issues abiertas del milestone actual al siguiente
# -------------------------------
open_issues=$(gitlab_paginate "${GITLAB_API}/issues?milestone=${NEW_VERSION}&state=opened")
for issue_id in $(echo "$open_issues" | jq -r '.[].iid'); do
    curl -s -X PUT "${GITLAB_API}/issues/$issue_id" \
        -H "Content-Type: application/json" \
        $HEADERS \
        -d "{\"milestone_id\": $next_milestone_id}" > /dev/null
done
echo "Moved $(echo "$open_issues" | jq length) open issues to milestone $next_version"

# -------------------------------
# 6. Generar changelog con categorías y emoticonos
# -------------------------------
echo "Generating changelog for version $NEW_VERSION"
currentDate=$(date +%d/%m/%Y)
mrs_current=$(gitlab_paginate "${GITLAB_API}/merge_requests?milestone=${NEW_VERSION}&state=merged")

declare -A categories
categories=( ["bug"]="🐛 Bug" ["feature"]="✨ Feature" ["documentation"]="📄 Docs" ["test"]="🧪 Test" )

changelog_text="# Changelog for ${RELEASE_NAME} ${NEW_VERSION}\n*${currentDate}*\n\n"

for cat in "${!categories[@]}"; do
    section=""
    for mr in $(echo "$mrs_current" | jq -c ".[]"); do
        labels=$(echo "$mr" | jq -r '.labels | join(",")')
        if [[ "$labels" == *"$cat"* ]]; then
            prefix=""
            if [[ "$labels" == *"has impacts"* ]]; then
                prefix="**[HAS IMPACTS]** "
            fi
            title=$(echo "$mr" | jq -r '.title' | sed 's/Resolve //g')
            iid=$(echo "$mr" | jq -r '.iid')
            url=$(echo "$mr" | jq -r '.web_url')
            author=$(echo "$mr" | jq -r '.merged_by.name')
            section+="- ${prefix}${title}. [MR #${iid}](${url}) (${author})"$'\n'
        fi
    done
    if [[ -n "$section" ]]; then
        changelog_text+="${categories[$cat]}:\n${section}\n"
    fi
done

echo "$changelog_text" > changelogUpdate
cat changelogUpdate ./CHANGELOG.md > allChangelog
rm changelogUpdate
mv allChangelog ./CHANGELOG.md
echo "Generated changelog file at ./CHANGELOG.md"

# -------------------------------
# 7. Cerrar el milestone actual
# -------------------------------
status=$(curl -s -X PUT "${GITLAB_API}/milestones/${current_milestone_id}" \
    -H "Content-Type: application/json" \
    $HEADERS \
    -d "{\"state_event\": \"close\"}" | jq -r '.state')
echo "Milestone ${NEW_VERSION} is now ${status}"
