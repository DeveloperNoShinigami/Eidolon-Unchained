// Phase 5 example: discoveries for the Light (server scripts): its calling, and favour for slaying Myrkul's followers.
// How discoveries work: common/discoveries.js.
var C = EidolonUnchained.conditions   // Phase 4 condition library (var: KubeJS files share one scope, so each file may repeat it)

EidolonUnchainedEvents.discoveries(event => {
    // Phase 5: callings. A god calls the player; they answer in chat (deity(...).calling sets the words).
    // Repeatable: every n-th match tries again; the calling itself skips followers and waits out its cooldown.
    event.discover('eu_examples:light_calls')
        .on('kill')
        .when(C.entity().undead())
        .count(10)                                   // every tenth undead you put to rest
        .offersPatronage('eidolon:light')
        .repeatable()

    // Phase 5: followers. Each of Myrkul's followers you kill (skeletons, the wither, or anyone pledged to him) pleases
    // the Light, ten times at most.
    event.discover('eu_examples:bane_of_the_grave')
        .on('kill')
        .when(C.entity().patron('eu_examples:myrkul'))
        .reputation('eidolon:light', 1)
        .repeatable(10)
})
