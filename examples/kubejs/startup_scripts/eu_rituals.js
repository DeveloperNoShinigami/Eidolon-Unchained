// Phase 2 example: a scripted ritual (startup scripts). Its brazier recipe is in server_scripts/eu_rituals.js.
EidolonUnchained.ritual('eu_examples:storm_rite')
    .symbol('eu_examples:particle/storm_rite')         // texture assets/eu_examples/textures/vfx/storm_rite.png; Eidolon stitches vfx/ as particle/ (optional: this is the default)
    .color(111, 199, 255)
    // Items and blood come from its brazier recipe (server_scripts/eu_rituals.js: 4 copper, 2 health). Requirements declared
    // here with .require / .requireHealth are ADDED to the recipe's, so declaring them in both places doubles them.
    .requires(EidolonUnchained.conditions.not(EidolonUnchained.conditions.dimension('minecraft:the_nether')))   // Phase 4: no storms in the Nether
    .onComplete((level, pos) => {
        level.spawnLightning(pos.x, pos.y + 1, pos.z, false)
        level.server.runCommandSilent(`weather thunder`)
    })
