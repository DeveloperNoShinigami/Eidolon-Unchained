# AI Deity JSON

[Overview](Overview.md) | [AI Deity JSON](AI-Deity-JSON.md) | [Chant JSON](Chant-JSON.md) | [Deity JSON](Deity-JSON.md) | [Codex JSON](Codex-JSON.md) | [Research JSON](Research-JSON.md) | [Fate JSON](Fate-JSON.md) | [Facts JSON](Facts-JSON.md) | [Keybind Settings JSON](Keybind-Settings-JSON.md) | [Ritual And Recipe Notes](Ritual-Recipes.md) | [Localization](Localization.md)

Source of truth: `AIDeityManager` and `AIDeityConfig`.

## Path

`data/<namespace>/ai_deities/<file>.json`

## Top-Level Schema

```json
{
  "deity": "<namespace>:<deity_name>",
  "ai_provider": "<provider_name>",
  "model": "<model_name>",
  "personality": "<system_prompt_text>",
  "mod_context_ids": ["<namespace>"],
  "behavior_rules": {},
  "prayer_configs": {},
  "api_settings": {},
  "patron_config": {},
  "natural_language_triggers": [],
  "tts_config": {}
  // "task_config": {}  -- deprecated, use Fate JSON files instead
}
```

Accepted alias:

- `deity_id` instead of `deity`

Fallback behavior:

- If both are missing, `AIDeityManager` falls back to the resource entry name.

## `behavior_rules`

Canonical container:

```json
"behavior_rules": {
  "reputation_thresholds": {
    "0": "<behavior_text>",
    "50": "<behavior_text>"
  },
  "research_requirements": {
    "1": "<behavior_text>"
  },
  "personality_shifts": {
    "<condition_key>": "<behavior_text>"
  },
  "blessings": {
    "<condition_key>": "<behavior_text>"
  },
  "curses": {
    "<condition_key>": "<behavior_text>"
  },
  "gifts": {
    "<condition_key>": "<behavior_text>"
  },
  "dynamic_responses": {
    "time_of_day": {
      "day": "<behavior_text>",
      "night": "<behavior_text>"
    },
    "biome": {
      "<namespace>:<biome_name>": "<behavior_text>"
    },
    "environment": {
      "underground": "<behavior_text>",
      "high_altitude": "<behavior_text>"
    }
  }
}
```

Legacy compatibility:

- `time_behaviors`
- `biome_behaviors`

Runtime note:

- `dynamic_responses.environment` is currently folded into personality shifts using keys prefixed as `env_<name>`.

## `prayer_configs`

`AIDeityManager.loadPrayerConfigs` treats each key in `prayer_configs` as the prayer type name.

```json
"prayer_configs": {
  "<prayer_type_name>": {
    "base_prompt": "<prompt_text>",
    "max_commands": 1,
    "cooldown_seconds": 300,
    "reputation_required": 0,
    "allowed_commands": ["give", "effect"],
    "additional_prompts": ["<prompt_text>"],
    "reference_commands": ["<command_string>"],
    "auto_judge_commands": false,
    "judgment_config": {
      "blessing_threshold": 50,
      "curse_threshold": -20,
      "blessing_commands": ["<command_string>"],
      "curse_commands": ["<command_string>"],
      "neutral_commands": ["<command_string>"]
    }
  }
}
```

Required prayer fields:

- `base_prompt`
- `max_commands`
- `cooldown_seconds` or legacy `cooldown_minutes`
- `reputation_required`
- `allowed_commands`

## `api_settings`

```json
"api_settings": {
  "api_key_env": "<environment_variable_name>",
  "model": "<model_name>",
  "timeout_seconds": 30,
  "temperature": 0.8,
  "max_tokens": 300,
  "generation_config": {
    "temperature": 0.8,
    "max_output_tokens": 300,
    "maxOutputTokens": 300
  },
  "safety_settings": {
    "harassment": "block_none",
    "hate_speech": "block_medium_and_above",
    "sexually_explicit": "block_medium_and_above",
    "dangerous_content": "block_medium_and_above"
  }
}
```

Compatibility notes:

- top-level `temperature` and `max_tokens` inside `api_settings` are still read
- `generation_config.maxOutputTokens` is accepted as a legacy alias for `max_output_tokens`
- although `GenerationConfig` also has `top_k` and `top_p` fields (defaults: `top_k: 40`, `top_p: 0.95`), `AIDeityManager.loadAPISettings` does not currently parse them from JSON. They are initialized to default values in `GenerationConfig` and are not overridable through this file.

## Server-Side AI Overrides (`EffectiveAIConfig`)

`EffectiveAIConfig` implements a three-level priority system for prayer config values:

```
Server Override (ConversationHistoryManager world data)
    > JSON Default (this file)
        > System Default
```

Overridable values per deity + prayer type:
- `cooldown_seconds` — server admin can tighten or relax cooldowns without reloading datapacks.
- `max_commands` — server limit acts as a ceiling even when JSON requests more commands.
- `reputation_required` — server can raise reputation gates.
- `auto_judge_commands` — server can force judgment on/off per deity.

Server operators set these overrides through the `/eu` admin command subtree or by editing `ConversationHistoryManager` world data. JSON values remain the fallback when no server override is set.

## `ritual_integration`

An open-ended map available on `AIDeityConfig` for modpack-specific ritual binding data.

```json
"ritual_integration": {
  "<key>": "<value>"
}
```

Values are not consumed by any built-in system; they are available to modpack scripts and custom data loaders that reference `AIDeityConfig.ritual_integration`.

## `patron_config`

```json
"patron_config": {
  "accepts_followers": true,
  "requires_patron_status": "any",
  "opposing_deities": ["<namespace>:<deity_name>"],
  "allied_deities": ["<namespace>:<deity_name>"],
  "follower_personality_modifiers": {
    "default": "<behavior_text>",
    "<progression_title>": "<behavior_text>"
  },
  "enemy_personality_modifier": "<behavior_text>",
  "neutral_personality_modifier": "<behavior_text>",
  "no_patron_personality_modifier": "<behavior_text>",
  "allied_personality_modifier": "<behavior_text>",
  "conversation_rules": {
    "<rule_key>": "<rule_value>"
  },
  "assignsPlayersToTeam": true,
  "teamName": "<scoreboard_team_display_name>",
  "teamColor": "<minecraft_chat_color_name>",
  "friendlyFire": false,
  "followerMobIds": ["<namespace>:<mob_entity_name>"],
  "stageRequiredForEntrall": "<progression_title_string>",
  "defaultFollowerMobMana": 100,
  "defaultFollowerMobMagicPower": 1.0,
  "followerMobManaByStage": {
    "<progression_title>": 150
  },
  "followerMobMagicPowerByStage": {
    "<progression_title>": 1.5
  }
}
```

Compatibility aliases currently supported:

- `supportedMobIds` instead of `followerMobIds`
- `tamingRequiredStage` instead of `stageRequiredForEntrall`
- `stageRequiredForEnthrall` is the corrected-spelling alias (both spellings accepted)
- snake_case aliases for mob economy keys: `default_follower_mob_mana`, `default_follower_mob_magic_power`, `follower_mob_mana_by_stage`, `follower_mob_magic_power_by_stage`

Important notes:

- `stageRequiredForEntrall` is compared against the deity's progression title string, not a stable stage ID.
- `defaultFollowerMobMana` sets the baseline mana pool for all enthralled mobs under this deity.
- `followerMobManaByStage` keys must match the `title` field of the patron's progression stage, not the stage `id`.
- `defaultFollowerMobMagicPower` and `followerMobMagicPowerByStage` scale the magic power bonus for enthralled mob chant casting.

Current parser-backed keys are the ones shown above. Common legacy-looking keys such as `requires_patron`, `neutral_deities`, and `allow_neutral_conversations` are not consumed by `loadPatronConfig`.

## `natural_language_triggers`

```json
"natural_language_triggers": [
  {
    "id": "<trigger_id>",
    "contains": ["<keyword_fragment>"],
    "regex": ["<regex_pattern>"],
    "min_reputation": 0,
    "cooldown_seconds": 0,
    "action": "<action_name>",
    "params": {
      "<param_name>": "<param_value>"
    }
  }
]
```

Important notes:

- entries must be objects; plain string entries are ignored by the current parser
- supported `action` values in `DeityChat.evaluateNaturalLanguageTriggers` are `offer_fate`, `run_commands`, `send_message`, and `curse_target`

Action param shapes currently supported:

```json
{
  "action": "run_commands",
  "params": {
    "commands": ["<command_string>"],
    "prayer_type": "blessing"
  }
}
```

```json
{
  "action": "send_message",
  "params": {
    "text": "<message_text>"
  }
}
```

```json
{
  "action": "curse_target",
  "params": {
    "radius": 10.0,
    "max_targets": 3,
    "team": "<team_name>",
    "not_team": "<team_name>",
    "tag": "<entity_tag>",
    "not_tag": "<entity_tag>",
    "opposing_to_player": true
  }
}
```

<!-- MARKED FOR REMOVAL: inline task_config is superseded by the Fate datapack system (data/<ns>/fates/). Use Fate JSON files instead. See Fate-JSON.md.

## `task_config`

```json
"task_config": {
  "enabled": true,
  "max_active_tasks": 1,
  "task_assignment_behavior": {
    "auto_assign_probability": 0.0,
    "min_reputation_for_auto_assign": 0,
    "cooldown_between_assignments_hours": 0
  },
  "available_tasks": [
    {
      "task_id": "<task_id>",
      "display_name": "<display_name>",
      "description": "<description_text>",
      "progression_tier": "<progression_title_or_id>",
      "requirements": [
        {
          "type": "<requirement_type>",
          "value": "<requirement_payload>"
        }
      ],
      "rewards": {
        "reputation": 0,
        "commands": ["<command_string>"],
        "progression_unlock": "<progression_stage>"
      },
      "cooldown_hours": 0,
      "repeatable": false,
      "ai_assignment_context": {
        "<context_key>": "<context_value>"
      }
    }
  ]
}
```

Task loader notes:

- each `requirements` entry is currently expected to be an object with at least `type`; the loader serializes the whole object into an internal string form
- `ai_assignment_context` is stored as raw JSON text, not a structured runtime object
- `TaskTemplate.reputationRequired` exists in code but is not populated by `loadTaskConfig`

-->

## `tts_config`

```json
"tts_config": {
  "enabled": true,
  "tts_provider": "<tts_provider_name>",
  "model": "<tts_model_name>",
  "voice_id": "<registered_voice_name_or_auto>",
  "backup_voice": "<registered_voice_name>",
  "reputation_voices": {
    "0": "<registered_voice_name>"
  },
  "time_voices": {
    "day": "<registered_voice_name>"
  },
  "biome_voices": {
    "<namespace>:<biome_name>": "<registered_voice_name>"
  },
  "voice_aliases": {
    "<local_alias>": "<registered_voice_name>"
  },
  "pitch": 1.0,
  "speed": 1.0,
  "volume": 1.0,
  "emotion": "<emotion_hint>",
  "accent": "<accent_hint>",
  "emphasis_level": 0,
  "allow_player_override": true,
  "funding_preference": "player_first",
  "audio_format": "wav",
  "voice_gender": "female",
  "voice_language": "en-US",
  "advanced_params": {
    "<provider_specific_key>": "<provider_specific_value>"
  }
}
```

TTS notes:

- the parser-backed key is `model`, not `tts_model`
- `emphasis_level` is read as an integer
- `custom_voice_file` exists on `AIDeityConfig.TTSConfig` but is not currently loaded by `loadTTSConfig`

[Overview](Overview.md) | [AI Deity JSON](AI-Deity-JSON.md) | [Chant JSON](Chant-JSON.md) | [Deity JSON](Deity-JSON.md) | [Codex JSON](Codex-JSON.md) | [Research JSON](Research-JSON.md) | [Fate JSON](Fate-JSON.md) | [Facts JSON](Facts-JSON.md) | [Keybind Settings JSON](Keybind-Settings-JSON.md) | [Ritual And Recipe Notes](Ritual-Recipes.md) | [Localization](Localization.md)
