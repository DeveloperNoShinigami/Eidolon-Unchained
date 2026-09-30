// Phase 2 example: chant recipes (server scripts). No datapack needed; /reload picks up changes.
ServerEvents.recipes(event => {
    // The first argument is the chant id; the recipe gets the same id, which is how Eidolon links them.
    event.recipes.eidolon.chant('eu_examples:call_storm', ['eidolon:soul', 'eu_examples:storm', 'eidolon:soul'])
    event.recipes.eidolon.chant('eu_examples:bone_shield', ['eidolon:wicked', 'eidolon:soul', 'eidolon:wicked'])
    event.recipes.eidolon.chant('eu_examples:grave_curse', ['eidolon:wicked', 'eidolon:wicked', 'eidolon:wicked'])
    event.recipes.eidolon.chant('eu_examples:dark_rebuke', ['eidolon:wicked', 'eidolon:blood', 'eidolon:wicked'])
    event.recipes.eidolon.chant('eu_examples:shadow_bolt', ['eidolon:wicked', 'eidolon:wicked', 'eidolon:soul'])

    // A command chant: signs, commands run as the server, mana cost.
    event.recipes.eidolon.command_chant('eu_examples:clear_skies', ['eidolon:sacred', 'eidolon:sacred', 'eidolon:flame'],
        ['weather clear'], 10)
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
// - the bone blade (Myrkul's): only Myrkul's chants, one line per allowed chant; 3 shard slots each
// - chest armour with Deity's Protection: grave_curse for 1 shard slot
ServerEvents.recipes(event => {
    event.recipes.eidolonunchained.imbue('eu_examples:call_storm', 'minecraft:golden_sword', 2)
    event.recipes.eidolonunchained.imbue('eu_examples:grave_curse', 'eu_examples:bone_blade', 3)
    event.recipes.eidolonunchained.imbue('eu_examples:bone_shield', 'eu_examples:bone_blade', 3)
    event.recipes.eidolonunchained.protect('eu_examples:grave_curse', '#minecraft:chest_armor', 1)
})
