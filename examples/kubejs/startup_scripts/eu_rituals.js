// Phase 2 example: a scripted ritual (startup scripts). Its brazier recipe is in server_scripts/eu_rituals.js.
EidolonUnchained.ritual('eu_examples:storm_rite')
    .symbol('eu_examples:textures/rituals/storm.png')
    .color(111, 199, 255)
    .require('minecraft:copper_ingot', 4)          // four pedestals with copper
    .requireHealth(2)                              // and a little blood
    .onComplete((level, pos) => {
        level.spawnLightning(pos.x, pos.y + 1, pos.z, false)
        level.server.runCommandSilent(`weather thunder`)
    })
