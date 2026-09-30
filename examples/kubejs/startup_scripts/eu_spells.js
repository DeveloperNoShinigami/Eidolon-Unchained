// Phase 2 example: scripted spells (startup scripts). Their sign sequences are chant recipes in server_scripts/eu_chants.js.

// A plain chant: strikes lightning where the caster looks (4 blocks ahead, like Eidolon's own spells aim).
EidolonUnchained.spell('eu_examples:call_storm')
    .cost(30)
    .delay(15)
    .canCast((level, pos, player) => level.isRaining() || player.isCreative())
    .cast((level, pos, player) => {
        const target = player.rayTrace(8).hit ? player.rayTrace(8).block.pos : pos
        level.spawnLightning(target.x, target.y, target.z, false)
    })

// A deity-bound chant: only for those Myrkul acknowledges.
EidolonUnchained.spell('eu_examples:bone_shield')
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
