
#!/bin/bash

# Repository configuration
REPO="/d/Swordie"
LOG_DIR="$REPO/logs"
LOG_FILE="$LOG_DIR/git_updates.log"

# Create log directory if necessary
mkdir -p "$LOG_DIR"

# Log helper
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Navigate to repository
cd "$REPO" || {
    log "ERROR: Could not access repository."
    exit 1
}

log "========================================"
log "Starting repository update."

# Verify Git repository
if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    log "ERROR: Directory is not a Git repository."
    exit 1
fi

# Check for changes
if [[ -z $(git status --porcelain) ]]; then
    log "No changes detected. Nothing to commit."
    exit 0
fi

# Log changed files
log "Changed files:"
git status --short | tee -a "$LOG_FILE"

# Stage all changes
git add -A

if [[ $? -ne 0 ]]; then
    log "ERROR: Failed to stage changes."
    exit 1
fi

# Commit changes
git commit -m "Automatic local update $(date '+%Y-%m-%d %H:%M:%S')"

if [[ $? -ne 0 ]]; then
    log "ERROR: Commit failed."
    exit 1
fi

# Push to remote
git push

if [[ $? -ne 0 ]]; then
    log "ERROR: Push failed. Local commit is preserved."
    exit 1
fi

log "SUCCESS: Repository successfully updated."
log "========================================"
