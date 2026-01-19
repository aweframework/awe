#!/bin/bash
set -Eeuo pipefail

# ============================================================
# Configuración
# ============================================================
GITLAB_API="$1"
TOKEN="$2"
RELEASE_NAME="$3"

# Optional dry-run mode (4th arg "--dry-run" or env DRY_RUN=1)
DRY_RUN_ARG="${4:-}"
if [[ "${DRY_RUN_ARG}" == "--dry-run" || "${DRY_RUN_ARG}" == "dry-run" ]]; then
  DRY_RUN=1
else
  DRY_RUN="${DRY_RUN:-0}"
fi

HEADERS=(
  -H "Authorization: Bearer ${TOKEN}"
  -H "Accept: application/json"
)

[[ "$DRY_RUN" == "1" ]] && echo "Running in DRY-RUN mode. The following actions will be logged but not executed."

# Helper to log and optionally execute PUT requests
api_put() {
  local url="$1"
  local body="$2"
  local description="${3:-}"
  if [[ -n "$description" ]]; then
    echo "PLAN: $description"
  else
    echo "PLAN: PUT $url with body: $body"
  fi
  if [[ "$DRY_RUN" == "1" ]]; then
    return 0
  fi
  curl -s -X PUT "$url" \
    -H "Content-Type: application/json" \
    "${HEADERS[@]}" \
    -d "$body" > /dev/null
}

# ============================================================
# Obtener versión desde pom.xml
# ============================================================
NEW_VERSION=$(grep -o '<version>[0-9\.]*[A-Z\-]*</version>' pom.xml \
  | sed -e 's/<[\/]*version>//g' \
  | sed 's/[A-Z\-]*//g' \
  | head -n1)

echo "Detected version: $NEW_VERSION"

# ============================================================
# Calcular siguiente versión (bugfix)
# ============================================================
IFS='.' read -r MAJOR MINOR PATCH <<< "$NEW_VERSION"
NEXT_VERSION="${MAJOR}.${MINOR}.$((PATCH + 1))"
echo "Next bugfix version: $NEXT_VERSION"

# ============================================================
# Función de paginación GitLab (segura)
# ============================================================
gitlab_paginate() {
    local url="$1"
    local page=1
    local per_page=100
    local all_pages='[]'

    while true; do
        response=$(curl -s -w "\n%{http_code}" "$url&page=$page&per_page=$per_page" "${HEADERS[@]}")
        body=$(echo "$response" | sed '$d')
        status=$(echo "$response" | tail -n1)

        if [[ "$status" -ge 400 ]]; then
            echo "❌ GitLab API error ($status) on: $url&page=$page" >&2
            echo "$body" >&2
            exit 1
        fi

        if [[ "$(echo "$body" | jq -r 'type')" != "array" ]]; then
            echo "⚠️ Skipping non-array response (probably empty or error)"
            break
        fi

        # Concatenar arrays correctamente
        all_pages=$(jq -s '.[0] + .[1]' <(echo "$all_pages") <(echo "$body"))

        # Revisar si hay siguiente página
        next_page=$(curl -sI "$url&page=$page&per_page=$per_page" "${HEADERS[@]}" \
            | grep -i "X-Next-Page" | awk '{print $2}' | tr -d '\r')
        [[ -z "$next_page" ]] && break
        page="$next_page"
    done

    echo "$all_pages"
}


# ============================================================
# Obtener milestones
# ============================================================
milestones=$(curl -s "${GITLAB_API}/milestones?state=active&per_page=100" "${HEADERS[@]}")

current_milestone_id=$(echo "$milestones" | jq -r --arg V "$NEW_VERSION" '.[] | select(.title==$V) | .id')

previous_milestone=$(echo "$milestones" \
  | jq -r '.[] | .title' \
  | grep -E '^[0-9]+\.[0-9]+\.[0-9]+$' \
  | sort -V \
  | awk -v cur="$NEW_VERSION" '$0==cur {print prev} {prev=$0}')

previous_milestone_id=$(echo "$milestones" | jq -r --arg V "$previous_milestone" '.[] | select(.title==$V) | .id')

[[ -z "$current_milestone_id" ]] && {
  echo "❌ Current milestone $NEW_VERSION not found"
  exit 1
}

# Crear siguiente milestone si no existe
next_milestone_id=$(echo "$milestones" | jq -r --arg V "$NEXT_VERSION" '.[] | select(.title==$V) | .id')

if [[ -z "$next_milestone_id" ]]; then
  echo "PLAN: Create next milestone: $NEXT_VERSION"
  if [[ "$DRY_RUN" != "1" ]]; then
    next_milestone_id=$(curl -s -X POST "${GITLAB_API}/milestones" \
      -H "Content-Type: application/json" \
      "${HEADERS[@]}" \
      -d "{\"title\":\"$NEXT_VERSION\"}" | jq -r '.id')
  fi
fi

# -------------------------------
# Mover issues abiertas del milestone actual al siguiente
# -------------------------------
open_issues=$(gitlab_paginate "${GITLAB_API}/issues?milestone=${NEW_VERSION}&state=opened")
for issue_id in $(echo "$open_issues" | jq -r '.[].iid'); do
    desc="Move Issue #$issue_id to milestone $NEXT_VERSION"
    if [[ "$DRY_RUN" == "1" && -z "$next_milestone_id" ]]; then
      echo "PLAN: $desc"
      continue
    fi
    api_put "${GITLAB_API}/issues/$issue_id" "{\"milestone_id\": $next_milestone_id}" "$desc"
done
echo "Moved $(echo "$open_issues" | jq length) open issues to milestone $NEXT_VERSION"

# ============================================================
# Corregir MRs y issues con milestone incorrecto
# ============================================================
echo "Checking for MRs and related issues with incorrect milestone..."

# Fecha de creación del milestone anterior
prev_milestone_created_at=$(echo "$milestones" | jq -r --arg V "$previous_milestone" '.[] | select(.title==$V) | .created_at')

# Fecha de creación del último tag del repositorio
# Nota: usamos commit.created_at del tag más reciente
repo_tags=$(curl -s "${GITLAB_API}/repository/tags?per_page=100" "${HEADERS[@]}")
last_tag_name=$(echo "$repo_tags" | jq -r 'sort_by(.commit.created_at) | last | .name // empty')
last_tag_created_at=$(echo "$repo_tags" | jq -r 'sort_by(.commit.created_at) | last | .commit.created_at // empty')

# Fecha de referencia para filtrar MRs
if [[ -n "$last_tag_created_at" && "$last_tag_created_at" != "null" ]]; then
  ref_created_at="$last_tag_created_at"
  echo "Using last tag ($last_tag_name) commit.created_at: $ref_created_at"
else
  ref_created_at="$prev_milestone_created_at"
  echo "Using previous milestone ($previous_milestone) created_at as fallback: $ref_created_at"
fi

# MRs mergeados desde la última release
gitlab_paginate "${GITLAB_API}/merge_requests?state=merged&per_page=100" | jq -c '.[]' | while read -r mr; do
    mr_iid=$(echo "$mr" | jq -r '.iid')
    mr_merged_at=$(echo "$mr" | jq -r '.merged_at')
    mr_milestone_id=$(echo "$mr" | jq -r '.milestone.id // empty')

    # Si se mergeó después de la última release (fecha de creación de la release anterior) y no tiene el milestone correcto
    if [[ "$mr_merged_at" > "$ref_created_at" && "$mr_milestone_id" != "$current_milestone_id" ]]; then
        desc_mr="Update MR #$mr_iid milestone to $NEW_VERSION"
        echo "⚡ $desc_mr"
        api_put "${GITLAB_API}/merge_requests/${mr_iid}" "{\"milestone_id\":$current_milestone_id}" "$desc_mr"

        # Actualizar issues relacionadas de este MR
        issues=$(curl -s "${GITLAB_API}/merge_requests/${mr_iid}/closes_issues" "${HEADERS[@]}")
        for issue_iid in $(echo "$issues" | jq -r '.[].iid'); do
            desc_issue="Update Issue #$issue_iid milestone to $NEW_VERSION (related to MR #$mr_iid)"
            api_put "${GITLAB_API}/issues/${issue_iid}" "{\"milestone_id\":$current_milestone_id}" "$desc_issue"
            echo "🔹 $desc_issue"
        done
    fi
done

echo "✅  Milestone corrections done"


# ============================================================
# Generar release notes
# ============================================================
echo "Generating release notes for version $NEW_VERSION"
currentDate=$(date +%d/%m/%Y)
mrs_current=$(gitlab_paginate "${GITLAB_API}/merge_requests?milestone=${NEW_VERSION}&state=merged")
echo "Found $(echo "$mrs_current" | jq length) merged MRs for milestone ${NEW_VERSION}"

FEATURES=""
BUGS=""
DOCS=""
TESTS=""

while IFS= read -r mr; do
  labels=$(echo "$mr" | jq -r '.labels | join(",")')

  # default a bug si no hay label de sección
  section="bug"
  for s in feature bug documentation test; do
    [[ "$labels" == *"$s"* ]] && section="$s"
  done

  prefix=""
  [[ "$labels" == *"has impacts"* ]] && prefix="**[HAS IMPACTS]** "

  # Extract title and strip optional surrounding quotes (ASCII and smart)
  title=$(echo "$mr" | jq -r '.title' | sed 's/^Resolve //' | sed -E 's/^[\"“”'"'"'‘’]+//; s/[\"“”'"'"'‘’]+$//')
  iid=$(echo "$mr" | jq -r '.iid')
  url=$(echo "$mr" | jq -r '.web_url')
  author=$(echo "$mr" | jq -r '.merged_by.name')

  line="- ${prefix}${title}. [MR #${iid}](${url}) (${author})"$'\n'

  case "$section" in
    feature) FEATURES+="$line" ;;
    bug) BUGS+="$line" ;;
    documentation) DOCS+="$line" ;;
    test) TESTS+="$line" ;;
  esac
done < <(echo "$mrs_current" | jq -c '.[]')

# Construir release notes con saltos de línea reales
releaseNotes=$(printf "# Release notes for %s %s\n*%s*\n \n" "$RELEASE_NAME" "$NEW_VERSION" "$currentDate")

[[ -n "$FEATURES" ]] && releaseNotes+=$'\n✨ Features:\n'"$FEATURES"
[[ -n "$BUGS" ]] && releaseNotes+=$'\n🐛 Bug fixes:\n'"$BUGS"
[[ -n "$DOCS" ]] && releaseNotes+=$'\n📄 Documentation:\n'"$DOCS"
[[ -n "$TESTS" ]] && releaseNotes+=$'\n🧪 Tests:\n'"$TESTS"

# Generate changelog
echo "$releaseNotes" > changelogUpdate && cat changelogUpdate ./CHANGELOG.md > allChangelog && rm changelogUpdate && mv allChangelog ./CHANGELOG.md && echo "Generated changelog file at ./CHANGELOG.md"

# ============================================================
# Cerrar milestone actual
# ============================================================
if [[ "$DRY_RUN" == "1" ]]; then
  echo "PLAN: Close milestone ${NEW_VERSION}"
  status="planned-close (dry-run)"
else
  echo "Closing milestone ${NEW_VERSION}"
  status=$(curl -s -X PUT "${GITLAB_API}/milestones/${current_milestone_id}" \
    -H "Content-Type: application/json" \
    "${HEADERS[@]}" \
    -d '{"state_event":"close"}' | jq -r '.state')
fi

echo "Milestone ${NEW_VERSION} is now ${status}"
