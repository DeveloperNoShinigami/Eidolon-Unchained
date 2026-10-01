// Phase 2/3 example: the Dark's chants and its mob caster profile (startup scripts). Their sign sequences are chant recipes
// in server_scripts/eu_examples/dark/recipes.js.

// A Dark-bound targeted chant, so Deity's Protection has something to bind for Eidolon's own deity too
// (Eidolon's dark/light prayers are effigy prayers: they have no target, so the enchantment cannot use them).
EidolonUnchained.chant('eu_examples:dark_rebuke')
    .deity('eidolon:dark')
    .cost(10)
    .delay(5)
    .protectionCost(1)
    .cast((level, pos, player) => {
        const hit = player.rayTrace(12)
        if (hit.entity) { hit.entity.attack(4); hit.entity.potionEffects.add('minecraft:blindness', 60, 0) }
    })
    .targetCast((level, caster, target) => {
        target.attack(4)
        target.potionEffects.add('minecraft:blindness', 60, 0)
        level.spawnParticles('minecraft:smoke', true, target.x, target.y + 1, target.z, 0.3, 0.5, 0.3, 10, 0.02)
    })

// Phase 3: a chant with a mob path (rule C3) and a caster profile that uses it. The mob path shoots a chant projectile (D39).
// (shadow_bolt itself is not deity-bound; it lives here because its only caster profile, dark_caster, serves the Dark.)
EidolonUnchained.chant('eu_examples:shadow_bolt')
    .cost(10)
    .cast((level, pos, player) => player.tell('§5You feel the shadow bolt fizzle: this one is meant for mobs.'))
    .targetCast((level, caster, target) => EidolonUnchained.projectile(caster).at(target)
        .chant('eu_examples:shadow_bolt').color(90, 30, 140).size(0.35).speed(1.2).gravity(0).homing(0.12).lifetime(80)
        .onHit((level, projectile, hit, pos) => { if (hit) { hit.attack(6); hit.potionEffects.add('minecraft:blindness', 60, 0) } })
        .shoot())

EidolonUnchained.caster('eu_examples:dark_caster')
    .chants('eu_examples:shadow_bolt')
    .deity('eidolon:dark')
    .mana(40, 2)
    .castInterval(80)
    .signDelay(10)
    .range(3, 16)
    .requireLineOfSight(true)
    .targetPolicy('attack_target')
