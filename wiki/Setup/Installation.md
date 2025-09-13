# Setup & Installation

Requirements

- Java 17
- Forge MDK for MC 1.20.1

Build

- Windows: `gradlew.bat build`
- Unix: `./gradlew build`

Dev Run

- Client: `gradlew runClient`
- Server: `gradlew runServer`

Config File

- Generated at: `config/eidolonunchained-common.toml`
- Registered by: `src/main/java/com/bluelotuscoding/eidolonunchained/config/EidolonUnchainedConfig.java:register`

Key Options (high level)

- `enable_ai_deities` (boolean): Turn AI deity system on/off
- `ai_provider` (string): `gemini`, `player2ai`, `openrouter`, `openai`, `proxy`
- `gemini_model` / `openrouter_model`: Choose the model
- Display tuning: title/subtitle vs action bar, typing speed, wrap, etc.
- Chant system controls: enable, datapack chants, cooldowns, exact sign order

Set API Keys In‑Game

- ` /eidolon-unchained api set <provider> <key>`
- ` /eidolon-unchained api set-model <model>`
- Quick Player2AI setup (no key): ` /eidolon-unchained api set player2ai`

Validate

- Check config status: ` /eidolon-unchained config status`
- Validate config: ` /eidolon-unchained config validate`
