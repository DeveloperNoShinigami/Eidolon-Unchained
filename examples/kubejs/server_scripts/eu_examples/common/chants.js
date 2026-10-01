// Phase 2 example: chant recipes not bound to a god, command chants, chant events and the shared imbue recipe (server scripts).
// No datapack needed; /reload picks up changes. Each god's chant, imbue and protect recipes are in its folder's recipes.js.
ServerEvents.recipes(event => {
    // The first argument is the chant id; the recipe gets the same id, which is how Eidolon links them.
    event.recipes.eidolon.chant('eu_examples:call_storm', ['eidolon:soul', 'eu_examples:storm', 'eidolon:soul'])

    // Command chants (Eidolon's command_chant: signs, commands, mana cost). The commands run at permission level 2 with the
    // chanter as @s; who may chant them is config commandChantPermissionLevel (D34, default 0 = anyone).
    event.recipes.eidolon.command_chant('eu_examples:clear_skies', ['eidolon:sacred', 'eidolon:sacred', 'eidolon:flame'],
        ['weather clear'], 10)
    event.recipes.eidolon.command_chant('eu_examples:nightfall', ['eidolon:soul', 'eidolon:soul', 'eidolon:wicked'],
        ['time set night', 'title @s actionbar {"text":"Night answers your chant","color":"dark_purple"}',
         'particle minecraft:soul ~ ~1 ~ 0.6 0.8 0.6 0.02 40'], 15)
})

EidolonUnchainedEvents.chantCast(e => console.info(`[EU example] ${e.player.username} chants ${e.chant} with ${e.signs}`))

EidolonUnchainedEvents.mobChantCast(e => console.info(`[EU example] ${e.entity.type} casts ${e.chant} at ${e.target ? e.target.name.string : '-'}`))

// Phase 3 (D35): imbued weapons and Deity's Protection, both cancelable.
EidolonUnchainedEvents.imbueCast(e => console.info(`[EU example] ${e.entity.username} casts imbued ${e.chant} from ${e.item.id}`))
EidolonUnchainedEvents.protectionTriggered(e => console.info(`[EU example] Deity's Protection ${e.enchantmentLevel} on ${e.item.id}: ${e.entity.name.string} answers ${e.attacker.name.string} with ${e.chant}`))

// Scripted imbue recipes (Eidolon's way: the written chant scroll is always the source; the table detects it).
// The generic scroll recipe is the global default; a weapon named here ONLY takes the chants scripted for it,
// so a scroll of any other chant does nothing on that weapon.
// - golden swords: only call_storm, 2 shard slots
// - the bone blade (Myrkul's): only Myrkul's chants, one line per allowed chant; 3 shard slots each (myrkul/recipes.js)
// - chest armour with Deity's Protection: grave_curse for 1 shard slot (myrkul/recipes.js)
ServerEvents.recipes(event => {
    event.recipes.eidolonunchained.imbue('eu_examples:call_storm', 'minecraft:golden_sword', 2)
})
