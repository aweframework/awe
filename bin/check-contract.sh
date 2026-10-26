#!/bin/sh
set -eu

# ============================================================
# check-contract.sh
#
# Fails when the TypeScript screen contract checked in for the React client no longer matches the Java model (#808).
# It regenerates awe-framework/awe-client-react/src/contract/contract.ts with the Maven "contract" profile of awe-model and
# compares it with the committed copy. The "Lint · Frontend" type check then runs against the committed file, which this
# check proves is the generated one.
#
# Usage: bin/check-contract.sh <maven command and options>     e.g. bin/check-contract.sh mvn -B -o
# ============================================================

ROOT=${CONTRACT_ROOT:-$(cd "$(dirname "$0")/.." && pwd)}   # CONTRACT_ROOT: tests only
CONTRACT=awe-framework/awe-client-react/src/contract/contract.ts
[ "$#" -gt 0 ] || { echo "usage: $0 <maven command and options>" >&2; exit 2; }

cd "$ROOT"
KEPT=$(mktemp)
trap 'rm -f "$KEPT"' EXIT
trap 'exit 130' INT TERM
cp "$CONTRACT" "$KEPT"
# From here on, whatever happens (drift, a failed or interrupted regeneration), the committed contract is put back
trap 'cp "$KEPT" "$CONTRACT"; rm -f "$KEPT"' EXIT
"$@" -Pcontract -pl awe-framework/awe-model process-classes

if cmp -s "$KEPT" "$CONTRACT"; then
  echo "The TypeScript screen contract is up to date."
else
  diff -u "$KEPT" "$CONTRACT" | head -60 || true
  {
    echo "The TypeScript screen contract is out of date: $CONTRACT no longer matches the Java model."
    echo "Regenerate it and commit the result:"
    echo "  mvn -Pcontract -pl awe-framework/awe-model process-classes"
  } >&2
  exit 1
fi
