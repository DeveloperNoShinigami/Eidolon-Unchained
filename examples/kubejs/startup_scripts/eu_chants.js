// Phase 2 example: scripted chants (startup scripts). Their sign sequences are chant recipes in server_scripts/eu_chants.js.

// A plain chant: strikes lightning where the caster looks (4 blocks ahead, like Eidolon's own spells aim).
EidolonUnchained.chant('eu_examples:call_storm')
    .cost(30)
    .delay(15)
    .canCast((level, pos, player) => level.isRaining() || player.isCreative())
    .cast((level, pos, player) => {
        const target = player.rayTrace(8).hit ? player.rayTrace(8).block.pos : pos
        level.spawnLightning(target.x, target.y, target.z, false)
    })

// A deity-bound chant: only for those Myrkul acknowledges.
EidolonUnchained.chant('eu_examples:bone_shield')
    .deity('eu_examples:myrkul')
    .minReputation(10)
    .cost(20)
    .cast((level, pos, player) => player.potionEffects.add('minecraft:resistance', 200, 1))

// An effigy prayer for Myrkul (Eidolon's PrayerSpell: signs are fixed here, no recipe needed).
EidolonUnchained.prayer('eu_examples:myrkul_prayer')
    .deity('eu_examples:myrkul')
    .reputation(2)
    .power(1.0)
    .signs('eidolon:soul', 'eidolon:wicked', 'eidolon:soul')

// Phase 3: a chant with a mob path (rule C3) and a caster profile that uses it. The mob path shoots a chant projectile (D39).
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
