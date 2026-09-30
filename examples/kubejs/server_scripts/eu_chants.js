// Phase 2 example: chant recipes (server scripts). No datapack needed; /reload picks up changes.
ServerEvents.recipes(event => {
    // The first argument is the spell id; the recipe gets the same id, which is how Eidolon links them.
    event.recipes.eidolon.chant('eu_examples:call_storm', ['eidolon:soul', 'eu_examples:storm', 'eidolon:soul'])
    event.recipes.eidolon.chant('eu_examples:bone_shield', ['eidolon:wicked', 'eidolon:soul', 'eidolon:wicked'])

    // A command chant: signs, commands run as the server, mana cost.
    event.recipes.eidolon.command_chant('eu_examples:clear_skies', ['eidolon:sacred', 'eidolon:sacred', 'eidolon:flame'],
        ['weather clear'], 10)
})

EidolonUnchainedEvents.spellCast(e => console.info(`[EU example] ${e.player.username} chants ${e.spell} with ${e.signs}`))
