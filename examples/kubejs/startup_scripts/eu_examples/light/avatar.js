// Phase 3 showcase: the Light's avatar as an EntityJS GeckoLib entity (user, 2026-09-30: "since we have EntityJS we can use
// it instead of our own entity model builder, at least for now"). The deity avatar comes from the studio's Blockbench file
// (geo/animation converted by knowledge/deity-models/tools/bb2gecko.py).
// Assets live in kubejs/assets/eu_examples/{geo,animations,textures/entity}. The other avatars: myrkul/avatar.js, dark/avatar.js.

StartupEvents.registry('entity_type', event => {
    // ---- deity avatar: idle loop, plus triggerable clips (manifest, speak, bless, depart, glide) ----
    // Sizes (user, 2026-09-30): the gods stand twice a player (3.6 blocks), Myrkul ten times (18 blocks). The `modelSize`
    // value is GeckoLib's render scale; the vanilla entity scale does not reach GeckoLib's renderer. `sized` is the hitbox
    // (width, height).
    // figure height at scale 1 (gate excluded): Light 6.4 blocks
    event.create('eu_examples:light_avatar', 'entityjs:mob')
        .sized(1.2, 3.6)
        .mobCategory('misc')
        .modelResource(e => `eu_examples:geo/light_deity.geo.json`)
        .textureResource(e => `eu_examples:textures/entity/light_deity.png`)
        .animationResource(e => `eu_examples:animations/light_deity.animation.json`)
        .newGlowingGeoLayer(layer => layer.textureResource(e => `eu_examples:textures/entity/light_deity_glowmask.png`))
        .modelSize(0.57, 0.57)          // GeckoLib's own render scale (a poseStack.scale in scaleModelForRender doubled up on the glow layer's re-render)
        .isPushable(false)
        .setSummonable(true)
        .noEggItem()
        .addAnimationController('idle', 5, ev => { ev.thenLoop(`animation.light_deity.idle`); return true })   // predicate: return true to keep playing
    // The other clips (manifest, speak, bless, depart, glide) are in the animation file but not wired yet: EntityJS's
    // triggerable controllers played them at once, stacking onto the idle (2026-09-30). A trigger path comes with Phase 6.
})
