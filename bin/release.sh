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
  local http_code
  http_code=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$url" \
    -H "Content-Type: application/json" \
    "${HEADERS[@]}" \
    -d "$body") || http_code="000"
  if [[ "$http_code" -ge 400 || "$http_code" == "000" ]]; then
    echo "❌ PUT $url failed with HTTP $http_code" >&2
    return 1
  fi
}

# ============================================================
# Obtener versión: usa RELEASE_VERSION (env var) si está definida, en otro caso
# se parsea desde pom.xml. RELEASE_VERSION permite pasar la versión derivada de
# $CI_COMMIT_TAG en un pipeline de tag sin depender del pom.xml del checkout actual.
# ============================================================
if [[ -n "${RELEASE_VERSION:-}" ]]; then
  NEW_VERSION="${RELEASE_VERSION}"
  echo "Using version from RELEASE_VERSION: $NEW_VERSION"
else
  NEW_VERSION=$(grep -o '<version>[0-9\.]*[A-Z\-]*</version>' pom.xml \
    | sed -e 's/<[\/]*version>//g' \
    | sed 's/[A-Z\-]*//g' \
    | head -n1)
  echo "Detected version: $NEW_VERSION"
fi

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
#
# Una MR pertenece a la release cuando su merge_commit_sha o squash_commit_sha esta en
# el rango de git <tag de la release anterior>..HEAD. Asi solo se
# tocan las MRs de la linea que se libera (develop, support/4.x...), sin comparar fechas.
# Una MR o issue con un milestone ya CERRADO (liberado en otra linea) se respeta.
# ============================================================
echo "Checking for MRs and related issues with incorrect milestone..."

# El clone del job puede ser superficial: se necesitan historia y tags para calcular el rango.
# Sin historia completa el rango y la eleccion del tag anterior serian incorrectos: se aborta.
git fetch --quiet --tags origin || { echo "❌ Could not fetch tags from origin" >&2; exit 1; }
if [[ "$(git rev-parse --is-shallow-repository)" == "true" ]]; then
  git fetch --quiet --unshallow origin || { echo "❌ Could not fetch the full history from origin" >&2; exit 1; }
fi
[[ "$(git rev-parse --is-shallow-repository)" == "false" ]] || {
  echo "❌ The repository is still shallow; cannot compute the release range" >&2
  exit 1
}

# Tag de la release anterior. Los tags de release de este repositorio no siempre son
# ancestros de la rama que se libera (p.ej. v4.12.9 se creo en master y support/4.x se corto
# de develop), asi que `git describe` no sirve. Entre los ultimos tags de release anteriores
# a la version que se libera (excluye, por tanto, el tag de esta version si ya existe, como
# en un pipeline de tag), se elige el que deja menos commits fuera de su historia: <tag>..HEAD.
# En empate gana la version mas alta.
prev_tag=""
prev_count=""
while IFS= read -r candidate; do
  [[ -n "$candidate" ]] || continue
  candidate_count=$(git rev-list --count "${candidate}..HEAD")
  if [[ -z "$prev_count" || "$candidate_count" -le "$prev_count" ]]; then
    prev_tag="$candidate"
    prev_count="$candidate_count"
  fi
done < <({ git tag --list 'v[0-9]*.[0-9]*.[0-9]*' | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' || true; echo "v${NEW_VERSION}"; } \
  | sort -V | uniq | awk -v cur="v${NEW_VERSION}" '$0==cur {exit} {print}' | tail -n 10)
[[ -n "$prev_tag" ]] || {
  echo "❌ No release tag (vX.Y.Z) older than v${NEW_VERSION} found; cannot compute the release range" >&2
  exit 1
}
echo "Release range: ${prev_tag}..HEAD"

# Conjunto de shas del rango, como objeto JSON {sha: true}, para filtrar con jq
range_file=$(mktemp)
trap 'rm -f "$range_file"' EXIT
git rev-list "${prev_tag}..HEAD" | jq -R . | jq -s 'map({(.): true}) | add // {}' > "$range_file"
echo "Commits in range: $(jq 'length' "$range_file")"

# Todas las MRs mergeadas se comparan con el rango (sin prefiltro por fecha: una MR del rango
# puede ser anterior al tag anterior si llego por otra linea).
merged_mrs=$(gitlab_paginate "${GITLAB_API}/merge_requests?state=merged")
release_mrs=$(echo "$merged_mrs" | jq -c --slurpfile range "$range_file" '.[]
      | select(($range[0][.merge_commit_sha // ""] // false) or ($range[0][.squash_commit_sha // ""] // false))')
# Aviso (no decide pertenencia): MRs recientes sin commit de merge ni de squash no se pueden
# situar en el rango. Con el metodo de merge del proyecto (merge commit) no deberia ocurrir.
prev_tag_date=$(TZ=UTC git log -1 --format=%cd --date=format-local:%Y-%m-%dT%H:%M:%SZ "$prev_tag")
no_sha_mrs=$(echo "$merged_mrs" | jq -r --arg since "$prev_tag_date" '.[]
      | select((.merged_at // "") > $since)
      | select((.merge_commit_sha // "") == "" and (.squash_commit_sha // "") == "")
      | "!" + (.iid|tostring)' | paste -sd' ' -)
[[ -z "$no_sha_mrs" ]] || echo "⚠️ Merged MRs without merge or squash commit (cannot be placed in the release range): $no_sha_mrs" >&2

echo "MRs in the release range: $(echo "$release_mrs" | jq -s 'length') ($(echo "$release_mrs" | jq -r '"!" + (.iid|tostring)' | paste -sd' ' -))"

# Asigna el milestone de la release a una MR o issue salvo que ya lo tenga o tenga uno cerrado.
# $1 = objeto JSON de la MR o issue, $2 = descripcion del cambio, $3 = URL del recurso
# Devuelve 0 si no hay nada que hacer o si el cambio se aplica, y 1 solo si la API falla.
failed_updates=0
assign_release_milestone() {
  local item="$1" desc="$2" url="$3"
  local milestone_id milestone_state
  milestone_id=$(echo "$item" | jq -r '.milestone.id // empty')
  milestone_state=$(echo "$item" | jq -r '.milestone.state // empty')
  if [[ "$milestone_id" == "$current_milestone_id" || "$milestone_state" == "closed" ]]; then
    return 0
  fi
  echo "⚡ $desc"
  api_put "$url" "{\"milestone_id\":$current_milestone_id}" "$desc"
}

while IFS= read -r mr; do
    [[ -z "$mr" ]] && continue
    mr_iid=$(echo "$mr" | jq -r '.iid')

    assign_release_milestone "$mr" "Update MR #$mr_iid milestone to $NEW_VERSION" \
      "${GITLAB_API}/merge_requests/${mr_iid}" || failed_updates=$((failed_updates + 1))

    # Issues relacionadas de esta MR
    issues=$(gitlab_paginate "${GITLAB_API}/merge_requests/${mr_iid}/closes_issues?state=all")
    while IFS= read -r issue; do
        [[ -z "$issue" ]] && continue
        issue_iid=$(echo "$issue" | jq -r '.iid')
        assign_release_milestone "$issue" "Update Issue #$issue_iid milestone to $NEW_VERSION (related to MR #$mr_iid)" \
          "${GITLAB_API}/issues/${issue_iid}" || failed_updates=$((failed_updates + 1))
    done < <(echo "$issues" | jq -c 'if type == "array" then .[] else empty end')
done < <(echo "$release_mrs")

if [[ "$failed_updates" -gt 0 ]]; then
  echo "❌ $failed_updates milestone update(s) failed; fix them before publishing the release notes" >&2
  exit 1
fi
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
