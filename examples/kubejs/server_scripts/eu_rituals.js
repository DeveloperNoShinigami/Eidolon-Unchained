// Phase 2 example: ritual recipes (server scripts). The brazier reaches a ritual through a ritual_brazier recipe.
ServerEvents.recipes(event => {
    // ritual id, reagent (burned in the brazier), pedestal items, focus items, health requirement (optional)
    event.recipes.eidolon.ritual_brazier('eu_examples:storm_rite', 'minecraft:lightning_rod',
        ['minecraft:copper_ingot', 'minecraft:copper_ingot', 'minecraft:copper_ingot', 'minecraft:copper_ingot'], [], 2)

    // Eidolon's crafting ritual: reagent -> output, with pedestal and focus items
    event.recipes.eidolon.ritual_brazier_crafting('4x minecraft:copper_ingot', 'minecraft:raw_copper_block',
        ['minecraft:coal', 'minecraft:coal'], [])
})
