// Phase 2 example: a scripted ritual (startup scripts), plus the Phase 5 rite that ends every pledge.
// Their brazier recipes are in server_scripts/eu_examples/common/rituals.js; each god's own patron rite is in its folder
// (myrkul/rites.js, light/rites.js, dark/rites.js).
var C = EidolonUnchained.conditions   // Phase 4 condition library (var: KubeJS files share one scope, so each file may repeat it)
EidolonUnchained.ritual('eu_examples:storm_rite')
    .symbol('eu_examples:particle/storm_rite')         // texture assets/eu_examples/textures/vfx/storm_rite.png; Eidolon stitches vfx/ as particle/ (optional: this is the default)
    .color(111, 199, 255)
    // Items and blood come from its brazier recipe (server_scripts/eu_examples/common/rituals.js: 4 copper, 2 health). Requirements declared
    // here with .require / .requireHealth are ADDED to the recipe's, so declaring them in both places doubles them.
    .requires(C.dimension('minecraft:the_nether').not())   // Phase 4: no storms in the Nether
    .onComplete((level, pos) => {
        level.spawnLightning(pos.x, pos.y + 1, pos.z, false)
        level.server.runCommandSilent(`weather thunder`)
    })

// Phase 5: patron rites. Completing one pledges the performer (the nearest player) to the god, or ends a pledge.
// A major god won't share its follower: if the performer already follows another major god, the brazier refuses
// at once, the pedestals keep their items and the reagent comes back.
// Ends every pledge (reputation with each left at 0); .revokePatron('eu_examples:myrkul', -10) would end one and leave a grudge.
EidolonUnchained.ritual('eu_examples:renunciation')
    .symbol('eidolon:particle/purify_ritual')
    .color(220, 220, 220)
    .revokePatron()
