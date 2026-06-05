#!/bin/sh
# Strip Markdown from the design doc into a plain-text reading copy.
#
# Block level (awk): drop fenced ``` code blocks, image lines, table separator
#   rows (|---|), and horizontal rules.
# Inline level (sed): [text](url) -> text; remove heading markers, emphasis (*),
#   inline-code backticks, blockquote '>', and table pipes (kept as spacing).
# Underscores are left alone so filenames like PLEXUS_DESIGN survive.
#
# Usage: extract-design-text.sh [SRC.md] [OUT.txt]
set -e
SRC="${1:-PLEXUS_DESIGN.md}"
OUT="${2:-PLEXUS_DESIGN_TEXT_ONLY.txt}"

awk '
  /^[[:space:]]*```/                         { infence = !infence; next }   # toggle/drop code fences
  infence                                    { next }
  /^[[:space:]]*!\[[^]]*\]\([^)]*\)[[:space:]]*$/ { next }                   # image lines
  /^[[:space:]|:-]+$/ { if (index($0,"|")>0 && index($0,"-")>0) next }       # table separator rows
  /^[[:space:]]*---[[:space:]]*$/            { next }                        # horizontal rule
  { print }
' "$SRC" \
| sed -E \
    -e 's/\[([^]]*)\]\([^)]*\)/\1/g' \
    -e 's/^#{1,6}[[:space:]]*//' \
    -e 's/^>[[:space:]]?//' \
    -e 's/\*//g' \
    -e 's/`//g' \
    -e 's/^[[:space:]]*\|//' \
    -e 's/\|[[:space:]]*$//' \
    -e 's/[[:space:]]*\|[[:space:]]*/  /g' \
  > "$OUT.tmp"

# collapse runs of blank lines to a single blank line
awk 'NF==0 { b++; if (b<=1) print; next } { b=0; print }' "$OUT.tmp" > "$OUT"
rm -f "$OUT.tmp"
echo "wrote $OUT"
