#!/usr/bin/env bash

LINES="$(grep -E "Copyright \((c|C)\)" -rL --include=\*.kt --include=\*.java --exclude-dir=generated-src --exclude-dir=projects --exclude-dir=gen --exclude-dir=build --exclude-dir=.gradle .)"
NUM_LINES=${#LINES}
if (( $NUM_LINES > 0 )); then
  echo "These files seem to be missing a license header:"
  echo $LINES | tr ' ' '\n'
  exit 1
fi
