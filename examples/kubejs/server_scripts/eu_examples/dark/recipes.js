// The Dark's recipes (server scripts): the sign sequences of its chants and the Vow of Night brazier recipe.
// The chants and the rite are in startup_scripts/eu_examples/dark/.
ServerEvents.recipes(event => {
    // Phase 2: chant recipes. The first argument is the chant id; the recipe gets the same id, which is how Eidolon links them.
    event.recipes.eidolon.chant('eu_examples:dark_rebuke', ['eidolon:wicked', 'eidolon:blood', 'eidolon:blood'])   // wicked, blood, wicked is Eidolon's Dark Animal Sacrifice
    event.recipes.eidolon.chant('eu_examples:shadow_bolt', ['eidolon:wicked', 'eidolon:wicked', 'eidolon:soul'])

    // Phase 5: patron rites (startup_scripts/eu_examples/dark/rites.js)
    // ritual id, reagent (burned in the brazier), pedestal items, focus items, health requirement (optional)
    event.recipes.eidolon.ritual_brazier('eu_examples:vow_of_night', 'minecraft:crying_obsidian',
        ['minecraft:ink_sac', 'minecraft:ink_sac', 'minecraft:amethyst_shard', 'minecraft:amethyst_shard'], [])
})
