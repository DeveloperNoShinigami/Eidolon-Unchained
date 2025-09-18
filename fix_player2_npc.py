#!/usr/bin/env python3
"""
Quick fix script to replace the broken Player2AIClient with the working Player2NPCClient
This addresses the core issue: wrong API endpoints causing AI responses to fail
"""

import os
import re

def fix_ai_provider_factory():
    """Update AIProviderFactory to use the corrected NPC client"""
    file_path = "src/main/java/com/bluelotuscoding/eidolonunchained/ai/AIProviderFactory.java"

    if not os.path.exists(file_path):
        print(f"❌ File not found: {file_path}")
        return False

    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

        # Add import for new NPC client
        if "import com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2NPCClient;" not in content:
            content = content.replace(
                "import com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2AIClient;",
                "import com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2AIClient;\nimport com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2NPCClient;"
            )

        # Update the createPlayer2AIProvider method to use NPC client
        old_method = """    private static AIProvider createPlayer2AIProvider() {
    // Unified auth: do not require a server API key. We support:
    // - Local Player2 app with game headers
    // - Web API with per-player device login (Bearer p2Key)
    // - Optional server key if configured
    // Provider can be constructed regardless; runtime will route/auth accordingly.
        int timeout = EidolonUnchainedConfig.COMMON.geminiTimeout.get();
        Player2AIClient client = new Player2AIClient(timeout);
    LOGGER.info("Creating Player2AI provider (unified auth: local app or per-player device login)");

        // Start health signal as required by Player2AI jam submission rules
        Player2HealthSignal.startHealthSignal();

        return new Player2AIProvider(client);
    }"""

        new_method = """    private static AIProvider createPlayer2AIProvider() {
        // Use the corrected Player2 NPC API workflow instead of broken OpenAI-compatible endpoints
        // This provides persistent character conversations with proper Player2.game integration
        Player2NPCClient client = new Player2NPCClient();
        LOGGER.info("Creating Player2 NPC provider (using proper NPC API endpoints)");

        // Start health signal as required by Player2AI jam submission rules
        Player2HealthSignal.startHealthSignal();

        return new Player2NPCAIProvider(client);
    }"""

        if old_method in content:
            content = content.replace(old_method, new_method)
            print("✅ Updated createPlayer2AIProvider method to use NPC client")
        else:
            print("⚠️  Could not find exact method match, method may have been modified")

        # Add import for the new provider class
        if "import com.bluelotuscoding.eidolonunchained.ai.Player2NPCAIProvider;" not in content:
            content = content.replace(
                "import java.util.concurrent.CompletableFuture;",
                "import java.util.concurrent.CompletableFuture;\n\n// Import the corrected Player2 NPC provider\nimport com.bluelotuscoding.eidolonunchained.ai.Player2NPCAIProvider;"
            )

        # Write the updated content
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)

        print(f"✅ Successfully updated {file_path}")
        return True

    except Exception as e:
        print(f"❌ Error updating {file_path}: {e}")
        return False

def main():
    print("🔧 Fixing Player2 AI Integration...")
    print("   Issue: Using wrong API endpoints (/chat/completions instead of /npcs/*)")
    print("   Fix: Replace with proper NPC API workflow")
    print()

    success = fix_ai_provider_factory()

    if success:
        print("\n✅ Player2 AI integration fixed!")
        print("   - Now uses proper NPC API endpoints")
        print("   - Supports persistent deity conversations")
        print("   - Both text and TTS responses should work")
        print("\n📝 Next steps:")
        print("   1. Test deity interactions")
        print("   2. Verify both TTS and non-TTS modes work")
        print("   3. Check that AI responses are actual conversations, not fallback text")
    else:
        print("\n❌ Fix failed. Manual intervention may be required.")

if __name__ == "__main__":
    main()