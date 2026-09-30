// Phase 2 example: chant recipes (server scripts). No datapack needed; /reload picks up changes.
ServerEvents.recipes(event => {
    // The first argument is the chant id; the recipe gets the same id, which is how Eidolon links them.
    event.recipes.eidolon.chant('eu_examples:call_storm', ['eidolon:soul', 'eu_examples:storm', 'eidolon:soul'])
    event.recipes.eidolon.chant('eu_examples:bone_shield', ['eidolon:wicked', 'eidolon:soul', 'eidolon:wicked'])
    event.recipes.eidolon.chant('eu_examples:shadow_bolt', ['eidolon:wicked', 'eidolon:wicked', 'eidolon:soul'])

    // A command chant: signs, commands run as the server, mana cost.
    event.recipes.eidolon.command_chant('eu_examples:clear_skies', ['eidolon:sacred', 'eidolon:sacred', 'eidolon:flame'],
        ['weather clear'], 10)
})

EidolonUnchainedEvents.chantCast(e => console.info(`[EU example] ${e.player.username} chants ${e.chant} with ${e.signs}`))

EidolonUnchainedEvents.mobChantCast(e => console.info(`[EU example] ${e.entity.type} casts ${e.chant} at ${e.target ? e.target.name.string : '-'}`))
