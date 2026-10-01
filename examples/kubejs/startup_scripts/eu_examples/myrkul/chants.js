// Phase 2/3 example: Myrkul's chants and his effigy prayer (startup scripts). Their sign sequences are chant recipes in
// server_scripts/eu_examples/myrkul/recipes.js.

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
        if (hit.entity) {
            hit.entity.hurt(EidolonUnchained.damageSource('eu_examples:necrotic', player), 4)   // Phase 5: Myrkul's necrotic damage
            hit.entity.potionEffects.add('minecraft:wither', 100, 1); hit.entity.potionEffects.add('minecraft:slowness', 100, 1)
        }
    })
    .targetCast((level, caster, target) => {              // retaliation / imbued / mob path
        target.hurt(EidolonUnchained.damageSource('eu_examples:necrotic', caster), 4)
        target.potionEffects.add('minecraft:wither', 100, 1)
        target.potionEffects.add('minecraft:slowness', 100, 1)
        level.spawnParticles('minecraft:soul', true, target.x, target.y + 1, target.z, 0.3, 0.5, 0.3, 12, 0.02)
    })

// An effigy prayer for Myrkul (Eidolon's PrayerSpell: signs are fixed here, no recipe needed).
EidolonUnchained.prayer('eu_examples:myrkul_prayer')
    .deity('eu_examples:myrkul')
    .reputation(2)
    .power(1.0)
    .signs('eidolon:soul', 'eidolon:wicked', 'eidolon:soul')

// A Myrkul chant that throws the spear (players: where you look; mobs / imbued / Deity's Protection: at the target).
// The spear (eu_examples:bone_spear) is registered in myrkul/avatar.js.
EidolonUnchained.chant('eu_examples:bone_volley')
    .deity('eu_examples:myrkul')
    .cost(15)
    .delay(6)
    .imbueCost(3)
    .cast((level, pos, player) => EidolonUnchained.projectile(player).entity('eu_examples:bone_spear').speed(1.8).shoot())
    .targetCast((level, caster, target) => EidolonUnchained.projectile(caster).at(target).entity('eu_examples:bone_spear').speed(1.8).shoot())
