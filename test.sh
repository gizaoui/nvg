#!/usr/bin/env bash
#
# list_java_imports.sh
# Liste les dépendances (imports) d'une application Java à partir de ses fichiers .java
#
# Usage :
#   ./list_java_imports.sh <répertoire_source> [options]
#
# Options :
#   -e, --external-only   N'affiche que les imports externes (hors java.*, javax.*)
#   -p, --packages        Regroupe par package racine (ex: org.apache, com.google)
#   -c, --count           Affiche le nombre d'occurrences de chaque import
#   -o, --output FICHIER  Écrit le résultat dans un fichier au lieu de stdout
#   -h, --help            Affiche cette aide
#
# Exemples :
#   ./list_java_imports.sh ./src
#   ./list_java_imports.sh ./src --external-only --count
#   ./list_java_imports.sh ./src --packages -o deps.txt

set -euo pipefail

SRC_DIR=""
EXTERNAL_ONLY=false
GROUP_PACKAGES=false
SHOW_COUNT=false
OUTPUT_FILE=""

usage() {
    grep '^#' "$0" | sed -e 's/^#//' -e '1d'
    exit 0
}

# --- Parsing des arguments ---
while [[ $# -gt 0 ]]; do
    case "$1" in
        -e|--external-only)
            EXTERNAL_ONLY=true
            shift
            ;;
        -p|--packages)
            GROUP_PACKAGES=true
            shift
            ;;
        -c|--count)
            SHOW_COUNT=true
            shift
            ;;
        -o|--output)
            OUTPUT_FILE="$2"
            shift 2
            ;;
        -h|--help)
            usage
            ;;
        -*)
            echo "Option inconnue : $1" >&2
            exit 1
            ;;
        *)
            SRC_DIR="$1"
            shift
            ;;
    esac
done

if [[ -z "$SRC_DIR" ]]; then
    echo "Erreur : vous devez spécifier un répertoire source." >&2
    usage
fi

if [[ ! -d "$SRC_DIR" ]]; then
    echo "Erreur : le répertoire '$SRC_DIR' n'existe pas." >&2
    exit 1
fi

if ! command -v find >/dev/null 2>&1; then
    echo "Erreur : 'find' est requis." >&2
    exit 1
fi

# --- Extraction des imports ---
# On cherche toutes les lignes "import ...;" (en ignorant les commentaires simples //)
# dans tous les fichiers .java du répertoire (récursif)
extract_imports() {
    find "$SRC_DIR" -type f -name "*.java" -print0 \
        | xargs -0 grep -h -E '^[[:space:]]*import[[:space:]]+' \
        | sed -E \
            -e 's/^[[:space:]]*import[[:space:]]+(static[[:space:]]+)?//' \
            -e 's/;.*$//' \
            -e 's/[[:space:]]+$//'
}

IMPORTS=$(extract_imports || true)

if [[ -z "$IMPORTS" ]]; then
    echo "Aucun import trouvé dans '$SRC_DIR'." >&2
    exit 0
fi

# --- Filtrage externe uniquement (hors JDK) ---
if $EXTERNAL_ONLY; then
    IMPORTS=$(echo "$IMPORTS" | grep -vE '^(java|javax)\.')
fi

# --- Regroupement par package racine (2 premiers segments, ex: org.apache) ---
if $GROUP_PACKAGES; then
    IMPORTS=$(echo "$IMPORTS" | awk -F. '{print $1"."$2}')
fi

# --- Tri, comptage ---
if $SHOW_COUNT; then
    RESULT=$(echo "$IMPORTS" | sort | uniq -c | sort -rn)
else
    RESULT=$(echo "$IMPORTS" | sort -u)
fi

# --- Sortie ---
if [[ -n "$OUTPUT_FILE" ]]; then
    echo "$RESULT" > "$OUTPUT_FILE"
    echo "Résultat écrit dans : $OUTPUT_FILE"
else
    echo "$RESULT"
fi

# --- Résumé ---
TOTAL_FILES=$(find "$SRC_DIR" -type f -name "*.java" | wc -l | tr -d ' ')
TOTAL_UNIQUE=$(echo "$IMPORTS" | sort -u | wc -l | tr -d ' ')
echo "" >&2
echo "Fichiers .java analysés : $TOTAL_FILES" >&2
echo "Dépendances uniques trouvées : $TOTAL_UNIQUE" >&2
