// Phase 5 example: followers with plain KubeJS (server scripts). EU adds only EidolonUnchained.patronOf(entity):
// the patron of a player or a mob, or null. Kills are KubeJS's own EntityEvents.death.

// Killing one of your own god's followers costs you favour with that god.
EntityEvents.death(e => {
    const killer = e.source.player                        // KubeJS: the player who killed it, or null
    if (!killer) return
    const god = EidolonUnchained.patronOf(e.entity)       // the slain one's patron, or null
    if (god && god == EidolonUnchained.patronOf(killer)) {
        EidolonUnchained.player(killer).subtractReputation(god, 5)   // EU's player helper (also player.data.eidolon)
        killer.tell('Your god grieves for its slain servant (-5).')
    }
})
