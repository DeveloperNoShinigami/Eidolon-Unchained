# Installation

## Required Game Versions And Dependencies

The current project configuration targets:

- Minecraft `1.20.1`
- Forge `47.4.0`
- Eidolon Repraised `0.3.8+`
- Curios `5.14.1+1.20.1`

Optional integration:

- Simple Voice Chat API `2.6.0`

## Mod Installation

Install Forge for Minecraft `1.20.1`, then place the required mod jars into your `mods` folder:

- Eidolon Repraised
- Curios
- Eidolon Unchained

If you want spatial deity voice playback, also install Simple Voice Chat on the same client and server.

## AI Provider Setup

At least one supported AI provider must be configured for deity conversation to work.

Current providers in the codebase:

- Gemini
- Player2AI
- OpenRouter

## API Key Sources

The current build supports two main configuration paths for API keys:

- Environment variables
- `config/eidolonunchained/server-api-keys.properties`

The server-side properties file uses provider-prefixed keys such as:

```properties
gemini.api_key=YOUR_KEY
openrouter.api_key=YOUR_KEY
player2ai.api_key=YOUR_KEY
```

## In-Game Command Surface

The main command root is:

- `/eidolon-unchained`
- `/eu` as the short alias

Useful setup commands include:

- `/eidolon-unchained api set <provider> <key>`
- `/eidolon-unchained api test <provider>`
- `/eidolon-unchained api list`

Player2AI also exposes its own authentication subtree:

- `/eidolon-unchained player2ai login device`
- `/eidolon-unchained player2ai login status`
- `/eidolon-unchained player2ai logout`

## Developer Validation

If you are running the mod from this repository instead of using built jars, the standard Gradle entry points are:

- `./gradlew runClient`
- `./gradlew runServer`
- `./gradlew runData`

The project uses Java `17` for Minecraft `1.20.1`.