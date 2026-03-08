#!/usr/bin/env bash
set -e

if [ -z "$1" ]; then
  echo "Usage: $0 <relative-file-path>"
  exit 1
fi

FILE="$1"

if [ ! -f "$FILE" ]; then
  echo "Error: file '$FILE' not found"
  exit 1
fi

NAME="$(basename "$FILE")"
BASE="${NAME%.*}"

TYPE="file"
case "$FILE" in
  *"/model/"*)      TYPE="model" ;;
  *"/repository/"*) TYPE="repository" ;;
  *"/service/"*)    TYPE="service" ;;
  *"/controller"*|*"/controllers/"*) TYPE="controller" ;;
  *"/dtos/"*)       TYPE="DTO" ;;
  *"/mappers/"*)    TYPE="mapper" ;;
  *"/config"*|*"/config/"*) TYPE="config" ;;
  *"/security/"*)   TYPE="security component" ;;
  *"/exceptions/"*) TYPE="exception" ;;
  *"src/main/resources/"*) TYPE="configuration" ;;
  *"talabaty-frontend/src/pages/"*) TYPE="page" ;;
  *"talabaty-frontend/src/components/"*) TYPE="component" ;;
  *"talabaty-frontend/src/services/"*) TYPE="service" ;;
  *"talabaty-frontend/src/utils/"*) TYPE="utility" ;;
  *"talabaty-frontend/"*".json") TYPE="config" ;;
  *".md") TYPE="documentation" ;;
esac

MSG="feat: add ${BASE} ${TYPE}"

git add "$FILE"
git commit -m "$MSG"
