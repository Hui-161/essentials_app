#!/usr/bin/env bash
# Writes the release notes to the file given as $1: the top section of CHANGELOG.md (also shown
# by the in-app updater), installation hints, checksums and the commits since the previous
# release tag. Warnings go to stdout as GitHub workflow commands.
# Env: VERSION_CODE, CERT_SHA256, FILE (APK, next to FILE.sha256), GITHUB_REPOSITORY, GITHUB_SHA
set -euo pipefail

out=$1
repo_url="https://github.com/$GITHUB_REPOSITORY"
blob_url="$repo_url/blob/$GITHUB_SHA"
previous=$(git describe --tags --abbrev=0 --match 'v*-fork.*' 2>/dev/null || true)

# Top section without its heading; relative links point to this commit (GitHub resolves them
# against the release page, the app not at all)
changes=""
if [ -f CHANGELOG.md ]; then
  changes=$(awk '/^## /{n++; next} n==1' CHANGELOG.md |
    sed -E "s|\]\(([^):]+)\)|](${blob_url}/\1)|g" |
    sed -e '/./,$!d')
fi
if [ -z "$changes" ]; then
  echo "::warning::CHANGELOG.md has no section for this release - the notes only list commits."
  changes="Keine Änderungsbeschreibung, siehe Commits."
elif [ -n "$previous" ] && git diff --quiet "$previous" HEAD -- CHANGELOG.md; then
  echo "::warning::CHANGELOG.md is unchanged since $previous - the notes repeat that release."
fi

{
  echo "$changes"
  echo
  echo "### Installation"
  echo
  echo "- **Erstinstallation:** Die Original-App ist anders signiert und lässt sich nicht"
  echo "  überschreiben. Vorher in der Original-App *Einstellungen → Exportieren*, die Original-App"
  echo "  deinstallieren, dieses APK installieren, die Einstellungen importieren und die"
  echo "  Berechtigungen neu erteilen. Anleitung: [FORK_RELEASE.md](${blob_url}/docs/FORK_RELEASE.md#installation)"
  echo "- **Updates:** in der App über *Nach Updates suchen*. Installiert werden nur Releases"
  echo "  dieses Repositorys mit demselben Signaturzertifikat."
  echo
  echo "### Prüfsummen"
  echo
  echo "- Build: \`${GITHUB_SHA::12}\` (versionCode $VERSION_CODE)"
  echo "- Signaturzertifikat SHA-256: \`$CERT_SHA256\`"
  echo "- APK SHA-256: \`$(cut -d' ' -f1 "$FILE.sha256")\`"
  echo
  echo "### Commits"
  echo
  if [ -n "$previous" ]; then
    git log --no-merges --format='- %s' -n 60 "$previous..HEAD" # -n, not head: pipefail
  else
    git log --no-merges --format='- %s' -n 30
  fi
} > "$out"
