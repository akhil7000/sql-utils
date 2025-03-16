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

# 1. Update version in pom.xml (only the main project version)
if [ -f "$POM_FILE" ]; then
  echo "Updating main project version in $POM_FILE"
  # Use a portable approach that works on both BSD and GNU sed
  awk '
    /<artifactId>sql-utils<\/artifactId>/ {
      print $0;
      getline;
      gsub(/<version>[0-9]\.[0-9]\.[0-9]<\/version>/, "<version>'"$NEW_VERSION"'<\/version>");
      print;
      next;
    }
    { print }
  ' "$POM_FILE" > "${POM_FILE}.tmp" && mv "${POM_FILE}.tmp" "$POM_FILE"
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
  
  # Clean up README bak file
  rm -f "${README_FILE}.bak"
else
  echo "Warning: $README_FILE not found"
fi

# 3. Update CHANGELOG.md - Insert new version before the latest version
if [ -f "$CHANGELOG_FILE" ]; then
  echo "Updating $CHANGELOG_FILE"
  
  # Check if new version already exists
  if grep -q "## \[$NEW_VERSION\]" "$CHANGELOG_FILE"; then
    echo "Version $NEW_VERSION already exists in CHANGELOG. No changes made."
  else
    # Find the latest version entry in the CHANGELOG
    LATEST_VERSION_LINE=$(grep -n "^## \[[0-9]\.[0-9]\.[0-9]\]" "$CHANGELOG_FILE" | head -1 | cut -d':' -f1)
    
    if [ -z "$LATEST_VERSION_LINE" ]; then
      echo "No existing version entries found in CHANGELOG."
      # Create a basic entry
      NEW_ENTRY="## [$NEW_VERSION] - $CURRENT_DATE

### Added
- Initial release of SQL Utils
- Support for parameter substitution in SQL queries and caching
- Ability to modify WHERE clauses programmatically

[$NEW_VERSION]: https://github.com/akhil7000/sql-utils/releases/tag/v$NEW_VERSION"
    else
      echo "Found latest version entry at line $LATEST_VERSION_LINE"
      
      # Get the latest version number
      LATEST_VERSION=$(grep "^## \[[0-9]\.[0-9]\.[0-9]\]" "$CHANGELOG_FILE" | head -1 | grep -o "\[[0-9]\.[0-9]\.[0-9]\]" | tr -d '[]')
      echo "Latest version is: $LATEST_VERSION"
      
      # Make a temporary file with the entry content
      grep -A 100 "^## \[$LATEST_VERSION\]" "$CHANGELOG_FILE" | grep -B 100 "\[$LATEST_VERSION\]:" | grep -v "\[$LATEST_VERSION\]:" > "${CHANGELOG_FILE}.entry"
      
      # Create new entry based on the latest one
      NEW_ENTRY=$(cat "${CHANGELOG_FILE}.entry" | sed "1s/## \[$LATEST_VERSION\] - [0-9-]*/## [$NEW_VERSION] - $CURRENT_DATE/")
      
      # Add the link for the new version
      PREV_VERSION=$LATEST_VERSION
      NEW_ENTRY="$NEW_ENTRY

[$NEW_VERSION]: https://github.com/akhil7000/sql-utils/compare/v$PREV_VERSION...v$NEW_VERSION"
    fi
    
    # Split the file at the insertion point (before the latest version)
    head -n $((LATEST_VERSION_LINE-1)) "$CHANGELOG_FILE" > "${CHANGELOG_FILE}.head"
    tail -n +$LATEST_VERSION_LINE "$CHANGELOG_FILE" > "${CHANGELOG_FILE}.tail"
    
    # Reassemble with the new version entry
    cat "${CHANGELOG_FILE}.head" > "${CHANGELOG_FILE}.new"
    echo -e "$NEW_ENTRY" >> "${CHANGELOG_FILE}.new"
    echo "" >> "${CHANGELOG_FILE}.new"  # Add blank line after new entry
    cat "${CHANGELOG_FILE}.tail" >> "${CHANGELOG_FILE}.new"
    
    # Move the new file to the original location
    mv "${CHANGELOG_FILE}.new" "$CHANGELOG_FILE"
    
    # Clean up
    rm -f "${CHANGELOG_FILE}.head" "${CHANGELOG_FILE}.tail" "${CHANGELOG_FILE}.entry"
    
    echo "Successfully added version $NEW_VERSION to CHANGELOG before version $LATEST_VERSION"
  fi
else
  echo "CHANGELOG file not found. Creating a new one."
  # Create a new CHANGELOG.md file with the basic structure
  cat > "$CHANGELOG_FILE" << EOF
# Changelog

## [$NEW_VERSION] - $CURRENT_DATE

### Added
- Initial release of SQL Utils
- Support for parameter substitution in SQL queries and caching
- Ability to modify WHERE clauses programmatically

[$NEW_VERSION]: https://github.com/akhil7000/sql-utils/releases/tag/v$NEW_VERSION
EOF
  echo "Created a new $CHANGELOG_FILE file"
fi

# Clean up any leftover .bak files
rm -f *.bak
rm -f ./*.bak
# Also clean up any other temporary files that might be left
rm -f "${CHANGELOG_FILE}.bak" "${POM_FILE}.bak" "${README_FILE}.bak"

echo "Version bump complete! Don't forget to:"
echo "1. Review the changes to ensure everything was updated correctly"
echo "2. Fill in the details in the CHANGELOG.md for the new version"
echo "3. Commit the changes with a message like 'Bump version to $NEW_VERSION'"
echo "4. Create a git tag: git tag -a v$NEW_VERSION -m 'Version $NEW_VERSION'"
