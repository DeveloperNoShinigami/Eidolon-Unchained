// Phase 3 showcase: Myrkul's avatar and his bone spear as EntityJS GeckoLib entities (user, 2026-09-30: "since we have
// EntityJS we can use it instead of our own entity model builder, at least for now").
//  - the deity avatar from the studio's Blockbench file (geo/animation converted by knowledge/deity-models/tools/bb2gecko.py)
//  - a GeckoLib projectile (a bone spear) that a chant shoots with EidolonUnchained.projectile(caster).entity('eu_examples:bone_spear')
//    (the chant is bone_volley, in myrkul/chants.js)
// Assets live in kubejs/assets/eu_examples/{geo,animations,textures/entity}. The other avatars: light/avatar.js, dark/avatar.js.

StartupEvents.registry('entity_type', event => {
    // ---- deity avatar: idle loop, plus triggerable clips (manifest, speak, bless, depart) ----
    // Sizes (user, 2026-09-30): the gods stand twice a player (3.6 blocks), Myrkul ten times (18 blocks). The `modelSize`
    // value is GeckoLib's render scale; the vanilla entity scale does not reach GeckoLib's renderer. `sized` is the hitbox
    // (width, height).
    // figure height at scale 1 (gate excluded): Myrkul 10.6 blocks
    event.create('eu_examples:myrkul_avatar', 'entityjs:mob')
        .sized(6.0, 18.0)
        .mobCategory('misc')
        .modelResource(e => `eu_examples:geo/myrkul.geo.json`)
        .textureResource(e => `eu_examples:textures/entity/myrkul.png`)
        .animationResource(e => `eu_examples:animations/myrkul.animation.json`)
        .newGlowingGeoLayer(layer => layer.textureResource(e => `eu_examples:textures/entity/myrkul_glowmask.png`))
        .modelSize(1.7, 1.7)          // GeckoLib's own render scale (a poseStack.scale in scaleModelForRender doubled up on the glow layer's re-render)
        .isPushable(false)
        .setSummonable(true)
        .noEggItem()
        .addAnimationController('idle', 5, ev => { ev.thenLoop(`animation.myrkul.idle`); return true })   // predicate: return true to keep playing
    // The other clips (manifest, speak, bless, depart) are in the animation file but not wired yet: EntityJS's
    // triggerable controllers played them at once, stacking onto the idle (2026-09-30). A trigger path comes with Phase 6.

    // ---- the bone spear: a GeckoLib projectile ----
    event.create('eu_examples:bone_spear', 'entityjs:geckolib_projectile')
        .sized(0.4, 0.4)
        .modelResource(e => 'eu_examples:geo/bone_spear.geo.json')
        .textureResource(e => 'eu_examples:textures/entity/bone_spear.png')
        .animationResource(e => 'eu_examples:animations/bone_spear.animation.json')
        .addAnimationController('spin', 0, ev => { ev.thenLoop('animation.bone_spear.spin'); return true })
        .setFacesTrajectory(true)
        .noItem()
        .onHitEntity(ctx => {
            const target = ctx.result.entity
            if (target.living) { target.attack(7); target.potionEffects.add('minecraft:wither', 60, 0) }
            ctx.entity.discard()
        })
        .onHitBlock(ctx => ctx.entity.discard())
})
