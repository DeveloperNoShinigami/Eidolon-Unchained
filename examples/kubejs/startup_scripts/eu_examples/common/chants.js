// Phase 2 example: scripted chants not bound to a god (startup scripts). Their sign sequences are chant recipes in
// server_scripts/eu_examples/common/chants.js.

// A plain chant: strikes lightning where the caster looks (4 blocks ahead, like Eidolon's own spells aim).
// .imbueCost(2): two soul shards (two reagent slots) imbue it into a weapon at the worktable (D35).
EidolonUnchained.chant('eu_examples:call_storm')
    .requires(ctx => String(ctx.level.dimension) != 'minecraft:the_nether')   // Phase 4: a plain function works wherever a condition does
    .imbueCost(2)
    .cost(30)
    .delay(8)
    .canCast((level, pos, player) => level.isRaining() || player.isCreative())
    .cast((level, pos, player) => {
        const target = player.rayTrace(8).hit ? player.rayTrace(8).block.pos : pos
        level.spawnLightning(target.x, target.y, target.z, false)
    })
