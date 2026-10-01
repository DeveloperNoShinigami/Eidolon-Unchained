// The Light's recipes (server scripts): the Vow of Light brazier recipe. The rite is in startup_scripts/eu_examples/light/rites.js.
ServerEvents.recipes(event => {
    // Phase 5: patron rites (startup_scripts/eu_examples/light/rites.js)
    // ritual id, reagent (burned in the brazier), pedestal items, focus items, health requirement (optional)
    event.recipes.eidolon.ritual_brazier('eu_examples:vow_of_light', 'minecraft:sunflower',
        ['minecraft:gold_ingot', 'minecraft:gold_ingot', 'minecraft:gold_ingot', 'minecraft:gold_ingot'], [])
})
