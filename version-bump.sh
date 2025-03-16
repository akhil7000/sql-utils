#!/bin/bash
# version-bump.sh - Script to bump version numbers across the project
# ./version-bump.sh 1.0.2

set -e  # Exit immediately if a command exits with a non-zero status

# Check if a version number was provided
if [ $# -ne 1 ]; then
  echo "Usage: $0 <new-version>"
  echo "Example: $0 1.0.2"
  exit 1
fi

NEW_VERSION=$1
CURRENT_DATE=$(date +%Y-%m-%d)
CHANGELOG_FILE="CHANGELOG.md"
README_FILE="README.md"
POM_FILE="pom.xml"

echo "Bumping version to $NEW_VERSION"

# 1. Update version in pom.xml
if [ -f "$POM_FILE" ]; then
  echo "Updating version in $POM_FILE"
  # Use xmlstarlet or sed depending on your environment
  # This example uses sed for simplicity
  sed -i.bak "s/<version>[0-9]\.[0-9]\.[0-9]<\/version>/<version>$NEW_VERSION<\/version>/" "$POM_FILE"
  rm "${POM_FILE}.bak"
else
  echo "Warning: $POM_FILE not found"
fi

# 2. Update version in README.md (for Maven and Gradle examples)
if [ -f "$README_FILE" ]; then
  echo "Updating version in $README_FILE"
  # Update Maven dependency example
  sed -i.bak "s/<version>[0-9]\.[0-9]\.[0-9]<\/version>/<version>$NEW_VERSION<\/version>/" "$README_FILE"
  
  # Update Gradle dependency example
  sed -i.bak "s/implementation 'io.github.akhil7000:sql-utils:[0-9]\.[0-9]\.[0-9]'/implementation 'io.github.akhil7000:sql-utils:$NEW_VERSION'/" "$README_FILE"
  
  # Clean up
  rm "${README_FILE}.bak"
else
  echo "Warning: $README_FILE not found"
fi

# 3. Update CHANGELOG.md
if [ -f "$CHANGELOG_FILE" ]; then
  echo "Updating $CHANGELOG_FILE"
  
  # Get the previous version to update comparison links
  PREV_VERSION=$(grep -o '\[[0-9]\.[0-9]\.[0-9]\]' "$CHANGELOG_FILE" | head -1 | tr -d '[]')
  
  # Prepare new changelog entry
  NEW_ENTRY="## [$NEW_VERSION] - $CURRENT_DATE\n\n### Added\n- \n\n### Changed\n- \n\n### Fixed\n- \n\n"
  
  # Insert new version entry after the Unreleased section
  awk -v new_entry="$NEW_ENTRY" '
  /^## \[Unreleased\]/ {
    print $0;
    getline;
    print $0;
    print new_entry;
    next;
  }
  { print $0 }
  ' "$CHANGELOG_FILE" > "${CHANGELOG_FILE}.new"
  
  # Update comparison links at the bottom of the file
  sed -i.bak "s|\[Unreleased\]: https://github.com/akhil7000/sql-utils/compare/v[0-9]\.[0-9]\.[0-9]...HEAD|\[Unreleased\]: https://github.com/akhil7000/sql-utils/compare/v$NEW_VERSION...HEAD\n\[$NEW_VERSION\]: https://github.com/akhil7000/sql-utils/compare/v$PREV_VERSION...v$NEW_VERSION|" "${CHANGELOG_FILE}.new"
  
  mv "${CHANGELOG_FILE}.new" "$CHANGELOG_FILE"
  rm -f "${CHANGELOG_FILE}.bak"
else
  echo "Warning: $CHANGELOG_FILE not found"
fi

echo "Version bump complete! Don't forget to:"
echo "1. Review the changes to ensure everything was updated correctly"
echo "2. Fill in the details in the CHANGELOG.md for the new version"
echo "3. Commit the changes with a message like 'Bump version to $NEW_VERSION'"
echo "4. Create a git tag: git tag -a v$NEW_VERSION -m 'Version $NEW_VERSION'"
