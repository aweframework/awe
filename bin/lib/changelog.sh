#!/bin/bash
# ============================================================
# Changelog functions of bin/release.sh, sourced by it and by bin/test-release-changelog.sh.
# They only read and write files and the local git repository: no GitLab access.
# ============================================================

# Removes the leading "# Release notes for <anything> (unreleased)" section of a changelog read from stdin:
# its header and body, up to the next "# Release notes for" header, the closing pointer to the changelog of the older
# lines (a "---" line whose next non-blank line links a CHANGELOG-<M>.x.md) or the end of the input. A "---" inside the
# draft is part of it. Any other changelog is written unchanged.
changelog_strip_unreleased() {
  awk '
    { line[NR] = $0 }
    END {
      state = 0
      for (n = 1; n <= NR; n++) {
        if (state == 0) {
          if (line[n] ~ /^[[:space:]]*$/) { blank[++blanks] = line[n]; continue }
          if (line[n] ~ /^# Release notes for .*\(unreleased\)[[:space:]]*$/) { state = 1; continue }
          for (i = 1; i <= blanks; i++) print blank[i]
          state = 2
        }
        if (state == 1) {
          if (line[n] ~ /^# Release notes for /) state = 2
          else if (line[n] ~ /^---[[:space:]]*$/) {
            next_line = n + 1
            while (next_line <= NR && line[next_line] ~ /^[[:space:]]*$/) next_line++
            if (next_line <= NR && line[next_line] ~ /CHANGELOG-[0-9]+\.x\.md/) state = 2
            else continue
          }
          else continue
        }
        print line[n]
      }
    }
  '
}

# Prints "<section><TAB><line>" for a merged MR (JSON object): the changelog section (feature, bug, documentation or
# test) and the changelog line. The section comes from the labels and, without a section label, from the Conventional
# Commit prefix of the title (feat, fix, docs, test; any other prefix is a bug fix).
changelog_mr_entry() {
  local mr="$1" labels section="" prefix="" title iid url author type
  labels=$(jq -r '.labels | join(",")' <<< "$mr")

  for s in feature bug documentation test; do
    [[ "$labels" == *"$s"* ]] && section="$s"
  done

  # Extract title and strip optional surrounding quotes (ASCII and smart)
  title=$(jq -r '.title' <<< "$mr" | sed 's/^Resolve //' | sed -E 's/^[\"“”'"'"'‘’]+//; s/[\"“”'"'"'‘’]+$//')

  if [[ -z "$section" ]]; then
    section="bug"
    type=$(sed -E 's/^(\[[^]]*\][[:space:]]*)+//' <<< "$title" \
      | sed -nE 's/^([A-Za-z]+)(\([^)]*\))?!?:.*/\1/p' | tr '[:upper:]' '[:lower:]')
    case "$type" in
      feat) section="feature" ;;
      docs) section="documentation" ;;
      test) section="test" ;;
    esac
  fi

  if [[ "$labels" == *"has impacts"* ]] && ! grep -qiF '[has impacts]' <<< "$title"; then
    prefix="**[HAS IMPACTS]** "
  fi

  iid=$(jq -r '.iid' <<< "$mr")
  url=$(jq -r '.web_url' <<< "$mr")
  author=$(jq -r '.merged_by.name' <<< "$mr")
  printf '%s\t- %s%s. [MR #%s](%s) (%s)\n' "$section" "$prefix" "$title" "$iid" "$url" "$author"
}

# Prints the release section for a version: header, date and the merged MRs (JSON array) grouped by section.
changelog_release_notes() {
  local release_name="$1" version="$2" date="$3" mrs="$4"
  local features="" bugs="" docs="" tests="" mr entry notes
  while IFS= read -r mr; do
    [[ -n "$mr" ]] || continue
    entry=$(changelog_mr_entry "$mr")
    case "${entry%%$'\t'*}" in
      feature) features+="${entry#*$'\t'}"$'\n' ;;
      bug) bugs+="${entry#*$'\t'}"$'\n' ;;
      documentation) docs+="${entry#*$'\t'}"$'\n' ;;
      test) tests+="${entry#*$'\t'}"$'\n' ;;
    esac
  done < <(jq -c '.[]' <<< "$mrs")

  notes=$(printf "# Release notes for %s %s\n*%s*\n \n" "$release_name" "$version" "$date")
  [[ -n "$features" ]] && notes+=$'\n✨ Features:\n'"$features"
  [[ -n "$bugs" ]] && notes+=$'\n🐛 Bug fixes:\n'"$bugs"
  [[ -n "$docs" ]] && notes+=$'\n📄 Documentation:\n'"$docs"
  [[ -n "$tests" ]] && notes+=$'\n🧪 Tests:\n'"$tests"
  echo "$notes"
}

# Writes the release section of a version on top of a changelog file, replacing its leading "(unreleased)" section.
# Fails, leaving the file unchanged, when the file already has a release section for that version.
changelog_update() {
  local file="$1" release_name="$2" version="$3" date="$4" mrs="$5" rest
  rest=$(mktemp)
  changelog_strip_unreleased < "$file" > "$rest"
  if awk -v v="$version" '
      index($0, "# Release notes for ") == 1 { n = split($0, words, " "); if (words[n] == v) found = 1 }
      END { exit !found }' "$rest"; then
    rm -f "$rest"
    echo "❌ $file already has a release section for $version; not adding another one" >&2
    return 1
  fi
  { changelog_release_notes "$release_name" "$version" "$date" "$mrs"; cat "$rest"; } > "$file"
  rm -f "$rest"
}

# Regenerates each existing CHANGELOG-<M>.x.md of a repository whose major M is lower than the major of the released
# version, from the CHANGELOG.md of the latest vM.y.z tag without its leading "(unreleased)" section.
# A line without tags, or whose tag has no CHANGELOG.md, keeps its file and is reported.
changelog_sync_previous_lines() {
  local version="$1" dir="${2:-.}" major file name line_major tag synced
  major="${version%%.*}"
  for file in "$dir"/CHANGELOG-*.x.md; do
    [[ -f "$file" ]] || continue
    name=$(basename "$file")
    line_major="${name#CHANGELOG-}"
    line_major="${line_major%.x.md}"
    [[ "$line_major" =~ ^[0-9]+$ ]] || continue
    (( 10#$line_major < 10#$major )) || continue

    tag=$(git -C "$dir" tag --list "v${line_major}.*" | grep -E "^v${line_major}\.[0-9]+\.[0-9]+$" | sort -V | tail -n 1 || true)
    if [[ -z "$tag" ]]; then
      echo "⚠️ No v${line_major}.y.z tag found; $name is kept as it is" >&2
      continue
    fi
    synced=$(mktemp)
    if ! git -C "$dir" show "${tag}:CHANGELOG.md" > "$synced" 2>/dev/null; then
      rm -f "$synced"
      echo "⚠️ $tag has no CHANGELOG.md; $name is kept as it is" >&2
      continue
    fi
    changelog_strip_unreleased < "$synced" > "$file"
    rm -f "$synced"
    echo "Synced $name from ${tag}:CHANGELOG.md"
  done
}
