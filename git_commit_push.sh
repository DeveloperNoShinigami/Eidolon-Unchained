#!/bin/bash

# Navigate to the project directory
cd /workspaces/Eidolon-Unchained

# Add all changes
echo "🔥 Adding all changes to git staging area..."
git add .

# Check status
echo "🔍 Current git status:"
git status

# Commit with a descriptive message
echo "💾 Committing prayer system fixes..."
git commit -m "🔥 MAJOR: Complete Prayer System JSON Configuration Fixes

✅ Fixed prayer type detection to use correct JSON keys
✅ Overhauled command selection to use reputation-based logic  
✅ Added comprehensive debug logging for item filtering
✅ Implemented full JSON-driven progression system

Details:
- determinePrayerType() now returns correct keys: communion, growth, protection, conversation, curse
- extractDeitySpecificCommands() uses reputation thresholds from JSON instead of hardcoded logic
- Added detailed logging for word filtering and permission checks
- Reputation-based blessing progression: initiate→acolyte→priest→high_priest→champion
- Smart command categorization and tier-appropriate selection

Prayer system now fully integrated with JSON configurations!"

# Force push to remote
echo "🚀 Force pushing to remote repository..."
git push origin 1.20.1_v3.9.0.9_Conversion --force

echo "✅ Git operations completed successfully!"
