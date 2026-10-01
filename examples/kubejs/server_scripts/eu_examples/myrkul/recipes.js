// Myrkul's recipes (server scripts): his chants' sign sequences, the bone blade's imbue recipes, Deity's Protection with
// grave_curse and the Pact of the Grave brazier recipe. The chants and the rite are in startup_scripts/eu_examples/myrkul/.
ServerEvents.recipes(event => {
    // Phase 2: chant recipes. The first argument is the chant id; the recipe gets the same id, which is how Eidolon links them.
    event.recipes.eidolon.chant('eu_examples:bone_shield', ['eidolon:soul', 'eidolon:wicked', 'eidolon:wicked'])   // wicked, soul, wicked is Hexblades' hex_pray
    event.recipes.eidolon.chant('eu_examples:grave_curse', ['eidolon:death', 'eidolon:wicked', 'eidolon:death'])   // wicked x3 is Eidolon's Dark Prayer
    event.recipes.eidolon.chant('eu_examples:bone_volley', ['eidolon:soul', 'eidolon:death', 'eidolon:soul'])   // soul, wicked, soul is Myrkul's prayer

    // Scripted imbue recipes (see common/chants.js for how they work):
    // - the bone blade (Myrkul's): only Myrkul's chants, one line per allowed chant; 3 shard slots each
    // - chest armour with Deity's Protection: grave_curse for 1 shard slot
    event.recipes.eidolonunchained.imbue('eu_examples:grave_curse', 'eu_examples:bone_blade', 3)
    event.recipes.eidolonunchained.imbue('eu_examples:bone_shield', 'eu_examples:bone_blade', 3)
    event.recipes.eidolonunchained.protect('eu_examples:grave_curse', '#minecraft:chest_armor', 1)

    // Phase 5: patron rites (startup_scripts/eu_examples/myrkul/rites.js)
    // ritual id, reagent (burned in the brazier), pedestal items, focus items, health requirement (optional)
    event.recipes.eidolon.ritual_brazier('eu_examples:pact_of_the_grave', 'minecraft:bone_block',
        ['minecraft:bone', 'minecraft:bone', 'eidolon:soul_shard', 'eidolon:soul_shard'], [], 2)
})
