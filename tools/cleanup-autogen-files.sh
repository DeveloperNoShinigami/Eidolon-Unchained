#!/bin/bash

# Cleanup script for auto-generated files that keep appearing
# Run this whenever you see unwanted markdown files in git status

echo "🧹 Cleaning up auto-generated files..."

# Remove auto-generated markdown files (usually created by AI tools/extensions)
echo "Removing auto-generated markdown files..."
rm -f AI_DEITY_*.md ALTAR_INTEGRATION_*.md API_CALL_*.md CHANT_*.md DEITY_*.md
rm -f DEVELOPMENT_*.md ENHANCED_*.md FEATURES.md FINAL_*.md FLEXIBLE_*.md
rm -f IMPLEMENTATION_*.md PROJECT_*.md RESEARCH_*.md SPELL_*.md TECHNICAL_*.md

# Remove auto-generated shell scripts
echo "Removing auto-generated shell scripts..."
rm -f fix_*.sh

# Remove other common auto-generated patterns
rm -f *_SUMMARY.md *_STATUS.md *_ANALYSIS.md *_GUIDE.md *_SYSTEM.md *_FIXES.md

echo "✅ Cleanup complete!"
echo ""
echo "If these files keep reappearing, they're likely being created by:"
echo "  - VS Code AI extensions (ChatGPT, Copilot, etc.)"
echo "  - Automated documentation tools"
echo "  - Build processes or workspace templates"
echo ""
echo "They are now ignored by .gitignore, so they won't show up in git status."
