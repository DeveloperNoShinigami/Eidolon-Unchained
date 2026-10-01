// Phase 4/5 example: discoveries that reward Myrkul's path (server scripts). How discoveries work: common/discoveries.js.
var C = EidolonUnchained.conditions   // Phase 4 condition library (var: KubeJS files share one scope, so each file may repeat it)

EidolonUnchainedEvents.discoveries(event => {
    // Killing a zombie named "The Forgotten" teaches Necromantic Rites, if Myrkul regards you at all.
    event.discover('eu_examples:forgotten_lore')
        .on('kill')
        .when(C.entity('minecraft:zombie').named('The Forgotten'))
        .when(C.player().reputation('eu_examples:myrkul', 0))
        .grantResearch('eu_examples:necromantic_rites')
        .message('eu_examples.discovery.forgotten')

    // Enthralling any undead with Eidolon's enthrall chant.
    event.discover('eu_examples:thrall_master')
        .on('enthrall')
        .when(C.entity().undead())
        .grantFact('eu_examples:thrall_master')
        .message('eu_examples.discovery.thrall')

    // Reaching a fortress while Myrkul regards you. (Not the Dark: Eidolon caps Dark reputation at 3 until the player
    // knows its "Sacrifice Mob" research, so a Dark >= 10 condition can't pass early in a playthrough.)
    event.discover('eu_examples:fortress_omen')
        .on('structure', 'minecraft:fortress')
        .when(C.player().reputation('eu_examples:myrkul', 10))
        .reputation('eu_examples:myrkul', 3)
        .run(ctx => ctx.player.tell('§5The fortress walls hum; Myrkul has walked these halls.'))

    // Phase 5: callings. A god calls the player; they answer in chat (deity(...).calling sets the words).
    event.discover('eu_examples:myrkul_calls')
        .on('kill')
        .when(C.entity('minecraft:zombie').named('The Forgotten'))
        .offersPatronage('eu_examples:myrkul')   // once (the default): Myrkul asks a single time
})
