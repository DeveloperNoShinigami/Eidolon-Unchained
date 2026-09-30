// Phase 2 example: player helpers and reputation events (server scripts).
// KubeJS exposes attached helpers as player.data.<name>, so the helper is player.data.eidolon (or EidolonUnchained.player(p)).

EidolonUnchainedEvents.serverReady(event => {
    console.info(`[EU example] deities: ${EidolonUnchained.deities()} | signs: ${EidolonUnchained.signs().length} | researches: ${EidolonUnchained.researches().length}`)
    console.info(`[EU example] Myrkul stages: ${EidolonUnchained.stages('eu_examples:myrkul')} model: ${EidolonUnchained.deityModel('eu_examples:myrkul')}`)
    console.info(`[EU example] Light stages now: ${EidolonUnchained.stages('eidolon:light')}`)
})

PlayerEvents.loggedIn(event => {
    const p = event.player
    // Teach the example chant's signs so it can be tried straight away (Eidolon's KnowledgeUtil.grantSign)
    p.data.eidolon.grantSign('eidolon:soul'); p.data.eidolon.grantSign('eu_examples:storm')
    // Give the test player some mana to chant with (Eidolon's ISoul setMaxMagic/setMagic)
    if (p.data.eidolon.soul().maxMana < 100) { p.data.eidolon.soul().setMaxMana(100); p.data.eidolon.soul().setMana(100) }
    // Phase 3 active chanting: slot keys default to G/H/J/K: 1 = soul, 2 = storm, so G, H, G casts call_storm on its own
    p.data.eidolon.chant.assign(0, 'eidolon:soul'); p.data.eidolon.chant.assign(1, 'eu_examples:storm')
    console.info(`[EU example] ${p.username}: knows storm sign? ${p.data.eidolon.knowsSign('eu_examples:storm')}; mana ${p.data.eidolon.soul().mana}/${p.data.eidolon.soul().maxMana}; Myrkul rep ${p.data.eidolon.reputation('eu_examples:myrkul')} stage ${p.data.eidolon.stage('eu_examples:myrkul')}`)
})

EidolonUnchainedEvents.reputationChanged(e => console.info(`[EU example] rep ${e.deity}: ${e.oldRep} -> ${e.newRep} (${e.player.username})`))
EidolonUnchainedEvents.stageUnlocked('eu_examples:myrkul', e => console.info(`[EU example] Myrkul stage unlocked: ${e.stage} major=${e.major}`))

EidolonUnchainedEvents.chantSign(e => console.info(`[EU example] ${e.player.username} pressed ${e.sign} -> ${e.signs}`))
EidolonUnchainedEvents.chantMatched(e => console.info(`[EU example] ${e.player.username} matched ${e.signs} = ${e.chant}`))
