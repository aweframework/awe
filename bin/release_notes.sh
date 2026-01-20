#!/bin/bash

# -------------------------------
# Configuración y versión
# -------------------------------
GITLAB_API="$1"   # URL base del proyecto, ej: https://gitlab.com/api/v4/projects/<project_id>
TOKEN="$2"        # Token con permisos API
RELEASE_NAME="$3" # Nombre de la release, ej: "Release"

HEADERS=(
  -H "Authorization: Bearer ${TOKEN}"
  -H "Accept: application/json"
)

# ============================================================
# Obtener versión desde pom.xml
# ============================================================
NEW_VERSION=$(grep -o '<version>[0-9\.]*[A-Z\-]*</version>' pom.xml \
  | sed -e 's/<[\/]*version>//g' \
  | sed 's/[A-Z\-]*//g' \
  | head -n1)

echo "Detected version: $NEW_VERSION"
export NEW_VERSION

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

# -------------------------------
# Guardar release notes
# -------------------------------
echo "$releaseNotes" > ./release_notes.md
echo "Generated release notes file at ./release_notes.md"
