#!/usr/bin/env bash

LINES="$(grep -E "Copyright \((c|C)\)" -rL --include=\*.kt --exclude-dir=generated-src --exclude-dir=projects .)"
STR_LEN=${#LINES}
if (( $STR_LEN > 0 )); then
  echo "These files seem to be missing a license header:"
  echo $LINES
  exit 1
fi
