// Phase 5 example: the Dark's calling discovery (server scripts). How discoveries work: common/discoveries.js.
var C = EidolonUnchained.conditions   // Phase 4 condition library (var: KubeJS files share one scope, so each file may repeat it)

EidolonUnchainedEvents.discoveries(event => {
    // Phase 5: callings. A god calls the player; they answer in chat (deity(...).calling sets the words).
    // Repeatable: every n-th match tries again; the calling itself skips followers and waits out its cooldown.
    event.discover('eu_examples:dark_calls')
        .on('kill')
        .when(C.night())
        .count(5)                                    // every fifth kill under the night sky
        .offersPatronage('eidolon:dark')
        .repeatable()
})
