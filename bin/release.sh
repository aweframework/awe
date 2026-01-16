#!/bin/bash
set -e

# ============================================================
# Configuración
# ============================================================
GITLAB_API="$1"    # https://gitlab.com/api/v4/projects/<project_id>
TOKEN="$2"
RELEASE_NAME="$3"  # Ej: "AWE"

HEADERS="--header Authorization: Bearer ${TOKEN}"

# ============================================================
# Obtener versión desde pom.xml
# ============================================================
NEW_VERSION=$(grep -o '<version>[0-9\.]*[A-Z\-]*</version>' pom.xml \
  | sed -e 's/<[\/]*version>//g' \
  | sed 's/[A-Z\-]*//g' \
  | head -n1)

export NEW_VERSION
echo "Detected version: $NEW_VERSION"

# ============================================================
# Calcular siguiente versión (bugfix)
# ============================================================
IFS='.' read -r MAJOR MINOR PATCH <<< "$NEW_VERSION"
NEXT_PATCH=$((PATCH + 1))
NEXT_VERSION="${MAJOR}.${MINOR}.${NEXT_PATCH}"

echo "Next bugfix version: $NEXT_VERSION"

# ============================================================
# Función de paginación GitLab
# ============================================================
gitlab_paginate() {
  local url="$1"
  local page=1
  local per_page=100
  local all='[]'

  while true; do
    headers=$(curl -sI "$url&page=$page&per_page=$per_page" $HEADERS)
    next_page=$(echo "$headers" | grep -i "X-Next-Page" | awk '{print $2}' | tr -d '\r')

    data=$(curl -s "$url&page=$page&per_page=$per_page" $HEADERS)
    all=$(jq -s 'add' <(echo "$all") <(echo "$data"))

    [[ -z "$next_page" || "$next_page" == "null" ]] && break
    page="$next_page"
  done

  echo "$all"
}

# ============================================================
# Obtener milestones
# ============================================================
milestones=$(gitlab_paginate "${GITLAB_API}/milestones?state=active")

current_milestone_id=$(echo "$milestones" | jq -r --arg V "$NEW_VERSION" '.[] | select(.title==$V) | .id')
previous_milestone=$(echo "$milestones" | jq -r --arg V "$NEW_VERSION" '.[] | select(.title!=$V) | .title' | tail -n1)

# Crear siguiente milestone si no existe
next_milestone_id=$(echo "$milestones" | jq -r --arg V "$NEXT_VERSION" '.[] | select(.title==$V) | .id')
if [[ -z "$next_milestone_id" || "$next_milestone_id" == "null" ]]; then
  echo "Creating next milestone: $NEXT_VERSION"
  next_milestone_id=$(curl -s -X POST "${GITLAB_API}/milestones" \
    -H "Content-Type: application/json" \
    $HEADERS \
    -d "{\"title\":\"$NEXT_VERSION\"}" | jq -r '.id')
fi

# ============================================================
# Mover MRs del milestone anterior al actual + issues relacionadas
# ============================================================
if [[ -n "$previous_milestone" ]]; then
  echo "Migrating MRs from milestone: $previous_milestone"
  mrs_prev=$(gitlab_paginate "${GITLAB_API}/merge_requests?milestone=${previous_milestone}&state=merged")

  for mr in $(echo "$mrs_prev" | jq -c '.[]'); do
    mr_iid=$(echo "$mr" | jq -r '.iid')

    # Actualizar MR
    curl -s -X PUT "${GITLAB_API}/merge_requests/${mr_iid}" \
      -H "Content-Type: application/json" \
      $HEADERS \
      -d "{\"milestone_id\":$current_milestone_id}" > /dev/null

    # Actualizar issues relacionadas
    issues=$(curl -s "${GITLAB_API}/merge_requests/${mr_iid}/closes_issues" $HEADERS)
    for issue_iid in $(echo "$issues" | jq -r '.[].iid'); do
      curl -s -X PUT "${GITLAB_API}/issues/${issue_iid}" \
        -H "Content-Type: application/json" \
        $HEADERS \
        -d "{\"milestone_id\":$current_milestone_id}" > /dev/null
    done
  done
fi

# ============================================================
# Mover issues abiertas al siguiente milestone
# ============================================================
open_issues=$(gitlab_paginate "${GITLAB_API}/issues?milestone=${NEW_VERSION}&state=opened")

for issue_iid in $(echo "$open_issues" | jq -r '.[].iid'); do
  curl -s -X PUT "${GITLAB_API}/issues/${issue_iid}" \
    -H "Content-Type: application/json" \
    $HEADERS \
    -d "{\"milestone_id\":$next_milestone_id}" > /dev/null
done

echo "Moved $(echo "$open_issues" | jq length) open issues to milestone $NEXT_VERSION"

# ============================================================
# Generar release notes mejoradas
# ============================================================
echo "Generating release notes for version $NEW_VERSION"
currentDate=$(date +%d/%m/%Y)

mrs_current=$(gitlab_paginate "${GITLAB_API}/merge_requests?milestone=${NEW_VERSION}&state=merged")

declare -A sections=(
  ["feature"]="✨ Features"
  ["bug"]="🐛 Bug fixes"
  ["documentation"]="📄 Documentation"
  ["test"]="🧪 Tests"
)

releaseNotes="# Release notes for ${RELEASE_NAME} ${NEW_VERSION}\n*${currentDate}*\n\n"

for key in feature bug documentation test; do
  block=""
  for mr in $(echo "$mrs_current" | jq -c '.[]'); do
    labels=$(echo "$mr" | jq -r '.labels | join(",")')
    [[ "$labels" != *"$key"* ]] && continue

    prefix=""
    [[ "$labels" == *"has impacts"* ]] && prefix="**[HAS IMPACTS]** "

    title=$(echo "$mr" | jq -r '.title' | sed 's/^Resolve //')
    iid=$(echo "$mr" | jq -r '.iid')
    url=$(echo "$mr" | jq -r '.web_url')
    author=$(echo "$mr" | jq -r '.merged_by.name')

    block+="- ${prefix}${title}. [MR #${iid}](${url}) (${author})"$'\n'
  done

  [[ -n "$block" ]] && releaseNotes+="${sections[$key]}:\n${block}\n"
done

echo -e "$releaseNotes" > release_notes.md
echo "Generated release notes at ./release_notes.md"

# ============================================================
# Cerrar milestone actual
# ============================================================
status=$(curl -s -X PUT "${GITLAB_API}/milestones/${current_milestone_id}" \
  -H "Content-Type: application/json" \
  $HEADERS \
  -d '{"state_event":"close"}' | jq -r '.state')

echo "Milestone ${NEW_VERSION} is now ${status}"
