// Phase 2 example: player helpers and reputation events (server scripts).
// /kubejs hand is not needed; these run on join so the log shows the calls working.

EidolonUnchainedEvents.serverReady(event => {
    console.info(`[EU example] deities: ${EidolonUnchained.deities()} | signs: ${EidolonUnchained.signs().length} | researches: ${EidolonUnchained.researches().length}`)
    console.info(`[EU example] Myrkul stages: ${EidolonUnchained.stages('eu_examples:myrkul')} model: ${EidolonUnchained.deityModel('eu_examples:myrkul')}`)
    console.info(`[EU example] Light stages now: ${EidolonUnchained.stages('eidolon:light')}`)
})

PlayerEvents.loggedIn(event => {
    const p = event.player
    console.info(`[EU example] ${p.username}: knows storm sign? ${p.eidolon.knowsSign('eu_examples:storm')}; mana ${p.eidolon.soul().mana}/${p.eidolon.soul().maxMana}; Myrkul rep ${p.eidolon.reputation('eu_examples:myrkul')} stage ${p.eidolon.stage('eu_examples:myrkul')}`)
})

EidolonUnchainedEvents.reputationChanged(e => console.info(`[EU example] rep ${e.deity}: ${e.oldRep} -> ${e.newRep} (${e.player.username})`))
EidolonUnchainedEvents.stageUnlocked('eu_examples:myrkul', e => console.info(`[EU example] Myrkul stage unlocked: ${e.stage} major=${e.major}`))
