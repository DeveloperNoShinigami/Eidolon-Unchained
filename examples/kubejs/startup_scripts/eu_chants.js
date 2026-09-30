// Phase 2 example: scripted chants (startup scripts). Their sign sequences are chant recipes in server_scripts/eu_chants.js.

// A plain chant: strikes lightning where the caster looks (4 blocks ahead, like Eidolon's own spells aim).
// .imbueCost(2): two soul shards (two reagent slots) imbue it into a weapon at the worktable (D35).
EidolonUnchained.chant('eu_examples:call_storm')
    .imbueCost(2)
    .cost(30)
    .delay(8)
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

// A deity-bound chant with a targeted path: this is what Deity's Protection casts at whatever hurt the wearer (D35),
// and what an imbued weapon or a mob casts at its target. .protectionCost(1): one soul shard binds it to the enchanted piece.
EidolonUnchained.chant('eu_examples:grave_curse')
    .deity('eu_examples:myrkul')
    .minReputation(0)
    .cost(12)
    .delay(5)
    .protectionCost(1)
    .cast((level, pos, player) => {                       // chanted by hand: curse what the player looks at
        const hit = player.rayTrace(12)
        if (hit.entity) { hit.entity.potionEffects.add('minecraft:wither', 100, 1); hit.entity.potionEffects.add('minecraft:slowness', 100, 1) }
    })
    .targetCast((level, caster, target) => {              // retaliation / imbued / mob path
        target.potionEffects.add('minecraft:wither', 100, 1)
        target.potionEffects.add('minecraft:slowness', 100, 1)
        level.spawnParticles('minecraft:soul', true, target.x, target.y + 1, target.z, 0.3, 0.5, 0.3, 12, 0.02)
    })

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
