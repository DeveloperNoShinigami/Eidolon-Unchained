// Phase 2 example: a custom sign and a custom rune (startup scripts).
// The sprite PNGs live in kubejs/assets/eu_examples/textures/particle/storm_sign.png and .../rune/tempest.png;
// Eidolon Unchained adds them to the block atlas for you.
EidolonUnchained.sign('eu_examples:storm')
    .sprite('eu_examples:particle/storm_sign')
    .color(111, 199, 255)

EidolonUnchained.rune('eu_examples:tempest')
    .sprite('eu_examples:rune/tempest')
    .effect(seq => 'pass')

EidolonUnchainedEvents.init(event => {
    console.info(`[EU example] declared sign eu_examples:storm and rune eu_examples:tempest (registered at common setup)`)
})
