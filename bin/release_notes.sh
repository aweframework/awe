#!/bin/bash

# -------------------------------
# Configuración y versión
# -------------------------------
GITLAB_API="$1"   # URL base del proyecto, ej: https://gitlab.com/api/v4/projects/<project_id>
TOKEN="$2"        # Token con permisos API
RELEASE_NAME="$3" # Nombre de la release, ej: "Release"

HEADERS="--header Authorization: Bearer ${TOKEN}"

# Obtener versión desde pom.xml
NEW_VERSION=$(grep -o '<version>[0-9\.]*[A-Z\-]*</version>' ./pom.xml \
    | sed -e 's/<[\/]*version>//g' \
    | sed ':a;N;$!ba;s/\n/ /g' \
    | sed 's/[A-Z\-]*//g' \
    | awk '{printf $1}')
export NEW_VERSION
echo "Detected version: $NEW_VERSION"

# -------------------------------
# Función para paginar requests GitLab
# -------------------------------
gitlab_paginate() {
    local url="$1"
    local page=1
    local per_page=100
    local results=()

    while true; do
        resp_headers=$(curl -sI "$url&page=$page&per_page=$per_page" $HEADERS)
        next_page=$(echo "$resp_headers" | grep -i "X-Next-Page" | awk '{print $2}' | tr -d '\r')
        data=$(curl -s "$url&page=$page&per_page=$per_page" $HEADERS | jq '.')
        results+=("$data")
        if [[ -z "$next_page" || "$next_page" == "null" ]]; then
            break
        fi
        page=$next_page
    done

    # Combinar arrays JSON
    echo "${results[@]}" | jq -s 'add'
}

# -------------------------------
# Categorías y emoticonos
# -------------------------------
declare -A categories
categories=( ["feature"]="✨ Features" ["bug"]="🐛 Bug fixes" ["documentation"]="📄 Documentation" ["test"]="🧪 Tests" )

# -------------------------------
# Obtener MRs mergeados del milestone actual
# -------------------------------
echo "Generating release notes for version $NEW_VERSION"
currentDate=$(date +%d/%m/%Y)
mrs_current=$(gitlab_paginate "${GITLAB_API}/merge_requests?milestone=${NEW_VERSION}&state=merged")

# -------------------------------
# Construir release notes por categoría
# -------------------------------
releaseNotesText="# Release notes for ${RELEASE_NAME} ${NEW_VERSION}\n*${currentDate}*\n\n"

for cat in "feature" "bug" "documentation" "test"; do
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
        releaseNotesText+="${categories[$cat]}:\n${section}\n"
    fi
done

# -------------------------------
# Guardar release notes
# -------------------------------
echo "$releaseNotesText" > ./release_notes.md
echo "Generated release notes file at ./release_notes.md"
