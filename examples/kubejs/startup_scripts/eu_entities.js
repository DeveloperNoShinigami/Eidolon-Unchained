// Phase 3 showcase: EntityJS carries the GeckoLib models (user, 2026-09-30: "since we have EntityJS we can use it instead of
// our own entity model builder, at least for now").
//  - the three deity avatars from the studio's Blockbench files (geo/animation converted by knowledge/deity-models/tools/bb2gecko.py)
//  - a GeckoLib projectile (a bone spear) that a chant shoots with EidolonUnchained.projectile(caster).entity('eu_examples:bone_spear')
// Assets live in kubejs/assets/eu_examples/{geo,animations,textures/entity}.

StartupEvents.registry('entity_type', event => {
    // ---- deity avatars: idle loop, plus triggerable clips (manifest, speak, bless, depart, walk/glide) ----
    // Sizes (user, 2026-09-30): the gods stand twice a player (3.6 blocks), Myrkul ten times (18 blocks). `renderScale` is GeckoLib's
    // render scale (modelSize); the vanilla entity scale does not reach GeckoLib's renderer. `sized` is the hitbox.
    const avatar = (id, name, clips, height, width, renderScale) => {
        const b = event.create(id, 'entityjs:mob')
            .sized(width, height)
            .mobCategory('misc')
            .modelResource(e => `eu_examples:geo/${name}.geo.json`)
            .textureResource(e => `eu_examples:textures/entity/${name}.png`)
            .animationResource(e => `eu_examples:animations/${name}.animation.json`)
            .newGlowingGeoLayer(layer => layer.textureResource(e => `eu_examples:textures/entity/${name}_glowmask.png`))
            .modelSize(renderScale, renderScale)          // GeckoLib's own render scale (a poseStack.scale in scaleModelForRender doubled up on the glow layer's re-render)
            .isPushable(false)
            .setSummonable(true)
            .noEggItem()
            .addAnimationController('idle', 5, ev => { ev.thenLoop(`animation.${name}.idle`); return true })   // predicate: return true to keep playing
        // The other clips (manifest, speak, bless, depart, walk/glide) are in the animation file but not wired yet: EntityJS's
        // triggerable controllers played them at once, stacking onto the idle (2026-09-30). A trigger path comes with Phase 6.
        return b
    }
    // figure heights at scale 1 (gates excluded): Myrkul 10.6 blocks, Dark 6.2, Light 6.4
    avatar('eu_examples:myrkul_avatar', 'myrkul', ['manifest', 'speak', 'bless', 'depart'], 18.0, 6.0, 1.7)
    avatar('eu_examples:dark_avatar', 'dark_deity', ['manifest', 'speak', 'bless', 'depart', 'walk'], 3.6, 1.2, 0.57)
    avatar('eu_examples:light_avatar', 'light_deity', ['manifest', 'speak', 'bless', 'depart', 'glide'], 3.6, 1.2, 0.57)

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

// A Myrkul chant that throws the spear (players: where you look; mobs / imbued / Deity's Protection: at the target).
EidolonUnchained.chant('eu_examples:bone_volley')
    .deity('eu_examples:myrkul')
    .cost(15)
    .delay(6)
    .imbueCost(3)
    .cast((level, pos, player) => EidolonUnchained.projectile(player).entity('eu_examples:bone_spear').speed(1.8).shoot())
    .targetCast((level, caster, target) => EidolonUnchained.projectile(caster).at(target).entity('eu_examples:bone_spear').speed(1.8).shoot())
