#!/usr/bin/env bash
#
# java_file_dependencies.sh
# Liste, à partir d'UN SEUL fichier Java "principal", tous les fichiers dont
# il dépend (transitivement), c'est-à-dire les fichiers qu'il importe,
# directement ou indirectement.
#
# Usage :
#   ./java_file_dependencies.sh <fichier_principal.java> <racine_des_sources> [options]
#
# Exemple :
#   ./java_file_dependencies.sh src/main/java/com/acme/App.java src/main/java
#
# Options :
#   -t, --tree            Affiche l'arborescence des dépendances (par défaut)
#   -f, --flat             Affiche une liste plate, triée, sans doublons
#   -d, --max-depth N      Limite la profondeur de récursion (0 = illimité, défaut)
#   -x, --no-external       N'affiche pas la liste des imports externes (JDK/libs)
#   -s, --no-same-package    Désactive l'heuristique "même package sans import"
#   -o, --output FICHIER     Écrit le résultat dans un fichier au lieu de stdout
#   -h, --help                Affiche cette aide
#
# Notes :
#   - "racine_des_sources" est le dossier racine du classpath source
#     (ex: src/main/java), c'est-à-dire le dossier tel que
#     racine/com/acme/App.java correspond à la classe com.acme.App.
#   - Les imports JDK/librairies externes (non résolus dans la racine
#     des sources) sont listés à part, non explorés récursivement.
#   - L'heuristique "même package" détecte les classes du même package
#     utilisées sans import explicite (cas courant en Java), en repérant
#     les noms de classes voisines référencés dans le code. Elle peut
#     produire de faux positifs (option --no-same-package pour désactiver).

set -uo pipefail

MAIN_FILE=""
SRC_ROOT=""
MODE="tree"
MAX_DEPTH=0
SHOW_EXTERNAL=true
SAME_PACKAGE=true
OUTPUT_FILE=""

usage() {
    grep '^#' "$0" | sed -e 's/^#//' -e '1d'
    exit 0
}

# --- Parsing des arguments ---
POSITIONAL=()
while [[ $# -gt 0 ]]; do
    case "$1" in
        -t|--tree) MODE="tree"; shift ;;
        -f|--flat) MODE="flat"; shift ;;
        -d|--max-depth) MAX_DEPTH="$2"; shift 2 ;;
        -x|--no-external) SHOW_EXTERNAL=false; shift ;;
        -s|--no-same-package) SAME_PACKAGE=false; shift ;;
        -o|--output) OUTPUT_FILE="$2"; shift 2 ;;
        -h|--help) usage ;;
        -*) echo "Option inconnue : $1" >&2; exit 1 ;;
        *) POSITIONAL+=("$1"); shift ;;
    esac
done
set -- "${POSITIONAL[@]:-}"
MAIN_FILE="${1:-}"
SRC_ROOT="${2:-}"

if [[ -z "$MAIN_FILE" || -z "$SRC_ROOT" ]]; then
    echo "Erreur : fichier principal et racine des sources requis." >&2
    usage
fi
if [[ ! -f "$MAIN_FILE" ]]; then
    echo "Erreur : le fichier '$MAIN_FILE' n'existe pas." >&2
    exit 1
fi
if [[ ! -d "$SRC_ROOT" ]]; then
    echo "Erreur : la racine des sources '$SRC_ROOT' n'existe pas." >&2
    exit 1
fi

MAIN_FILE="$(realpath "$MAIN_FILE")"
SRC_ROOT="$(realpath "$SRC_ROOT")"

if [[ -n "$OUTPUT_FILE" ]]; then
    exec > "$OUTPUT_FILE"
fi

declare -A VISITED          # fichiers internes déjà visités (déduplication / anti-cycle)
declare -A EXTERNAL_SEEN    # imports externes déjà rencontrés
FLAT_LIST=()
CYCLE_COUNT=0
FILE_COUNT=0

# --- Extrait les imports d'un fichier : kind(normal|static|wildcard) <TAB> fqn ---
get_imports() {
    local file="$1"
    grep -h -E '^[[:space:]]*import[[:space:]]+' "$file" 2>/dev/null | while IFS= read -r line; do
        local static_flag=0
        if [[ "$line" =~ ^[[:space:]]*import[[:space:]]+static[[:space:]]+ ]]; then
            static_flag=1
        fi
        local fqn
        fqn=$(echo "$line" | sed -E \
            -e 's/^[[:space:]]*import[[:space:]]+(static[[:space:]]+)?//' \
            -e 's/;.*$//' \
            -e 's/[[:space:]]+$//')
        [[ -z "$fqn" ]] && continue
        if [[ "$fqn" == *.\* ]]; then
            printf 'wildcard\t%s\n' "$fqn"
        elif [[ "$static_flag" -eq 1 ]]; then
            printf 'static\t%s\n' "$fqn"
        else
            printf 'normal\t%s\n' "$fqn"
        fi
    done
}

add_external() {
    local fqn="$1"
    if [[ -z "${EXTERNAL_SEEN[$fqn]:-}" ]]; then
        EXTERNAL_SEEN["$fqn"]=1
    fi
}

# --- Parcours récursif (DFS) ---
traverse() {
    local file="$1"
    local depth="$2"

    if [[ -n "${VISITED[$file]:-}" ]]; then
        if [[ "$MODE" == "tree" ]]; then
            printf '%*s↳ %s (déjà listé)\n' "$((depth * 2))" '' "${file#$SRC_ROOT/}"
        fi
        CYCLE_COUNT=$((CYCLE_COUNT + 1))
        return
    fi
    VISITED["$file"]=1
    FILE_COUNT=$((FILE_COUNT + 1))

    if [[ "$depth" -gt 0 ]]; then
        FLAT_LIST+=("$file")
        if [[ "$MODE" == "tree" ]]; then
            printf '%*s├─ %s\n' "$((depth * 2))" '' "${file#$SRC_ROOT/}"
        fi
    else
        if [[ "$MODE" == "tree" ]]; then
            printf '%s (fichier principal)\n' "${file#$SRC_ROOT/}"
        fi
    fi

    if [[ "$MAX_DEPTH" -gt 0 && "$depth" -ge "$MAX_DEPTH" ]]; then
        return
    fi

    local kind fqn
    while IFS=$'\t' read -r kind fqn; do
        case "$kind" in
            wildcard)
                local pkg_path="${fqn%.*}"
                local dir="$SRC_ROOT/${pkg_path//./\/}"
                if [[ -d "$dir" ]]; then
                    local f
                    for f in "$dir"/*.java; do
                        [[ -f "$f" ]] || continue
                        traverse "$f" "$((depth + 1))"
                    done
                else
                    add_external "$fqn"
                fi
                ;;
            static)
                local path="$SRC_ROOT/${fqn//./\/}.java"
                if [[ -f "$path" ]]; then
                    traverse "$path" "$((depth + 1))"
                else
                    local parent="${fqn%.*}"
                    local path2="$SRC_ROOT/${parent//./\/}.java"
                    if [[ -f "$path2" ]]; then
                        traverse "$path2" "$((depth + 1))"
                    else
                        add_external "$fqn"
                    fi
                fi
                ;;
            normal)
                local path="$SRC_ROOT/${fqn//./\/}.java"
                if [[ -f "$path" ]]; then
                    traverse "$path" "$((depth + 1))"
                else
                    add_external "$fqn"
                fi
                ;;
        esac
    done < <(get_imports "$file")

    if $SAME_PACKAGE; then
        local dir cls f
        dir="$(dirname "$file")"
        for f in "$dir"/*.java; do
            [[ -f "$f" ]] || continue
            [[ "$f" == "$file" ]] && continue
            cls="$(basename "$f" .java)"
            if grep -qwE "$cls" "$file" 2>/dev/null; then
                traverse "$f" "$((depth + 1))"
            fi
        done
    fi
}

# --- Exécution ---
if [[ "$MODE" == "tree" ]]; then
    echo "=== Arborescence des dépendances de $(basename "$MAIN_FILE") ==="
fi

traverse "$MAIN_FILE" 0

if [[ "$MODE" == "flat" ]]; then
    echo "=== Fichiers dont dépend $(basename "$MAIN_FILE") (${#FLAT_LIST[@]} au total) ==="
    printf '%s\n' "${FLAT_LIST[@]}" | sed "s|^$SRC_ROOT/||" | sort -u
fi

if $SHOW_EXTERNAL && [[ "${#EXTERNAL_SEEN[@]}" -gt 0 ]]; then
    echo ""
    echo "=== Dépendances externes (JDK / librairies, non explorées) ==="
    printf '%s\n' "${!EXTERNAL_SEEN[@]}" | sort -u
fi

# --- Résumé (toujours sur stderr pour rester visible même avec -o) ---
echo "" >&2
echo "Fichier principal        : ${MAIN_FILE#$SRC_ROOT/}" >&2
echo "Fichiers internes trouvés : $((FILE_COUNT - 1))" >&2
echo "Imports externes uniques  : ${#EXTERNAL_SEEN[@]}" >&2
[[ "$CYCLE_COUNT" -gt 0 ]] && echo "Cycles / doublons évités  : $CYCLE_COUNT" >&2
[[ -n "$OUTPUT_FILE" ]] && echo "Résultat écrit dans        : $OUTPUT_FILE" >&2
