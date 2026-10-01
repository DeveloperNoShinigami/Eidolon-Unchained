// Phase 2 example: ritual recipes (server scripts). The brazier reaches a ritual through a ritual_brazier recipe.
ServerEvents.recipes(event => {
    // ritual id, reagent (burned in the brazier), pedestal items, focus items, health requirement (optional)
    event.recipes.eidolon.ritual_brazier('eu_examples:storm_rite', 'minecraft:lightning_rod',
        ['minecraft:copper_ingot', 'minecraft:copper_ingot', 'minecraft:copper_ingot', 'minecraft:copper_ingot'], [], 2)

    // Eidolon's crafting ritual: reagent -> output, with pedestal and focus items
    event.recipes.eidolon.ritual_brazier_crafting('4x minecraft:copper_ingot', 'minecraft:raw_copper_block',
        ['minecraft:coal', 'minecraft:coal'], [])

    // Phase 5: patron rites (startup_scripts/eu_rituals.js)
    event.recipes.eidolon.ritual_brazier('eu_examples:pact_of_the_grave', 'minecraft:bone_block',
        ['minecraft:bone', 'minecraft:bone', 'eidolon:soul_shard', 'eidolon:soul_shard'], [], 2)
    event.recipes.eidolon.ritual_brazier('eu_examples:vow_of_light', 'minecraft:sunflower',
        ['minecraft:gold_ingot', 'minecraft:gold_ingot', 'minecraft:gold_ingot', 'minecraft:gold_ingot'], [])
    event.recipes.eidolon.ritual_brazier('eu_examples:vow_of_night', 'minecraft:crying_obsidian',
        ['minecraft:ink_sac', 'minecraft:ink_sac', 'minecraft:amethyst_shard', 'minecraft:amethyst_shard'], [])
    event.recipes.eidolon.ritual_brazier('eu_examples:renunciation', 'minecraft:feather',
        ['minecraft:paper', 'minecraft:paper'], [])
})
