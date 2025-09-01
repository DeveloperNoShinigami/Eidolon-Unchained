#!/bin/bash

# Eidolon Unchained Documentation Cleanup Script
# Removes outdated .md files and consolidates into wiki structure

echo "🧹 Cleaning up scattered documentation files..."

# Root directory MD files to remove (keeping only essential ones)
CLEANUP_FILES=(
    "ACTUAL_FIXES_COMPLETED.md"
    "AI_API_SETUP.md" 
    "AI_COMMAND_DEBUGGING_GUIDE.md"
    "AI_CONFIGURATION_HARDCODED_ISSUE_FIX.md"
    "AI_DEITY_CHANT_SYSTEM.md"
    "AI_DEITY_CONFIG_CLEANUP_COMPLETE.md"
    "AI_DEITY_FIXES.md"
    "AI_DEITY_FIX_IMMEDIATE.md"
    "AI_DEITY_IMPLEMENTATION.md"
    "AI_DEITY_ISSUE_RESOLUTION.md"
    "AI_DEITY_PHASE1_SUMMARY.md"
    "AI_ENGAGEMENT_ANALYSIS.md"
    "ALTAR_INTEGRATION_SYSTEM.md"
    "API_CALL_ANALYSIS.md"
    "BUILD_TEST_RESULTS.md"
    "CHANT_API_FIX_SUMMARY.md"
    "CHANT_CASTING_SOLUTION.md"
    "CHANT_KEYBIND_IMPROVEMENTS.md"
    "CHAT_CLEANUP_SUMMARY.md"
    "CODEX_EXTENSION_GUIDE.md"
    "CODEX_SYSTEM_DOCUMENTATION.md"
    "COMPLETE_CATEGORY_DIAGRAM.md"
    "COMPLETE_CHAPTER_STRUCTURE.md"
    "COMPLETE_DEITY_EXAMPLES_SUMMARY.md"
    "COMPLETE_SYSTEM_SUMMARY.md"
    "COMPREHENSIVE_AI_ENHANCEMENT_COMPLETE.md"
    "COMPREHENSIVE_CODEX_IMPLEMENTATION.md"
    "COMPREHENSIVE_CODEX_IMPLEMENTATION_GUIDE.md"
    "CONFIGURABLE_DISPLAY_SYSTEM.md"
    "CONSOLIDATION_COMPLETE_SUMMARY.md"
    "CUSTOM_CATEGORIES_GUIDE.md"
    "CUSTOM_CATEGORY_VISUAL_EXAMPLE.md"
    "DEITY_CONSOLIDATION_PLAN.md"
    "DEITY_EXAMPLES.md"
    "DEVELOPMENT_LOG.md"
    "EIDOLON_STRUCTURE_ANALYSIS.md"
    "EIDOLON_UPDATE_CHECKLIST.md"
    "EIDOLON_VERSION_MIGRATION_GUIDE.md"
    "ENHANCED_AI_SYSTEM_GUIDE.md"
    "ENHANCED_DISPLAY_SYSTEM.md"
    "ENHANCED_PROACTIVE_AI_SYSTEM.md"
    "FEATURES.md"
    "FIELD_VALIDATION_STATUS.md"
    "FINAL_CHANT_FIX_STATUS.md"
    "FLEXIBLE_CHANT_SYSTEM.md"
    "FUTURE_EXPANSION_ROADMAP.md"
    "IMPLEMENTATION_GUIDE.md"
    "IMPLEMENTATION_STATUS.md"
    "IMPORT_VS_REFLECTION_STRATEGY.md"
    "JSON_MODEL_FIELD_FIX.md"
    "LANGUAGE_STANDARDIZATION_PLAN.md"
    "OPENROUTER_INTEGRATION_COMPLETE.md"
    "OPENROUTER_MODEL_VERIFICATION.md"
    "OPENROUTER_PRIVACY_POLICY_FIX.md"
    "PATRON_SYSTEM_IMPLEMENTATION_COMPLETE.md"
    "PER_DEITY_AI_PROVIDER_SYSTEM_COMPLETE.md"
    "PLAYER2AI_CONFIGURATION_FIX.md"
    "PLAYER2AI_DEBUGGING_ANALYSIS.md"
    "PLAYER2AI_TESTING_DEBUG_FIXES_COMPLETE.md"
    "POLISH_PLAN.md"
    "PROGRESSION_CONSISTENCY_FIX.md"
    "PROJECT_SUCCESS_SUMMARY.md"
    "REAL_TIME_WORLD_DATA_INTEGRATION_COMPLETE.md"
    "RESEARCH_SYSTEM_FIX_EXPLANATION.md"
    "RESEARCH_SYSTEM_GUIDE.md"
    "RESEARCH_SYSTEM_REALITY_CHECK.md"
    "RESEARCH_TRIGGERS_README.md"
    "RESEARCH_TRIGGER_FIXES_COMPLETE.md"
    "RESEARCH_TRIGGER_TEST_GUIDE.md"
    "RESEARCH_TRIGGER_VERIFICATION.md"
    "RETRY_SYSTEM_IMPLEMENTATION.md"
    "RITUAL_PATRON_SELECTION_GUIDE.md"
    "RITUAL_UPDATES_SUMMARY.md"
    "SPELL_COOLDOWN_SYSTEM_CONVERSATION_SUMMARY.md"
    "SYSTEM_STATUS_ANALYSIS.md"
    "TARGET_CHAPTER_REFERENCE.md"
    "TECHNICAL_FAILURE_ANALYSIS.md"
)

# Essential files to keep
KEEP_FILES=(
    "README.md"
    "CHANGELOG.md"
    "LICENSE"
)

echo "📊 Found ${#CLEANUP_FILES[@]} files to clean up"

# Create backup directory for reference
mkdir -p archive/old-docs
echo "📦 Created backup directory: archive/old-docs"

# Move files to archive
for file in "${CLEANUP_FILES[@]}"; do
    if [ -f "$file" ]; then
        mv "$file" "archive/old-docs/"
        echo "  ✅ Archived: $file"
    fi
done

# Update main README to point to wiki
echo "📝 Updating main README.md..."

# The updated README content will be created by the next file creation

echo ""
echo "🎉 Documentation cleanup complete!"
echo ""
echo "📁 New structure:"
echo "  ├── README.md (updated to point to wiki)"
echo "  ├── CHANGELOG.md (preserved)" 
echo "  ├── LICENSE (preserved)"
echo "  ├── wiki/ (comprehensive documentation)"
echo "  └── archive/old-docs/ (backup of old files)"
echo ""
echo "🚀 Wiki now contains all documentation in organized format!"
