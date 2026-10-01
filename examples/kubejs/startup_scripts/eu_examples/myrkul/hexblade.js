// Phase 3 example: a scripted hexblade. Hexblades Renewed's weapon system (awaken on right-click, devotion-scaled
// power, durability as the energy pool, no invulnerability frames, on-hit effects, dialogue) bound to ANY deity:
// here Myrkul. It is a KubeJS item of type 'eidolonunchained:hexblade', so texture, name and tier come from KubeJS.
StartupEvents.registry('item', event => {
    event.create('eu_examples:bone_blade', 'eidolonunchained:hexblade')
        .displayName('Bone Blade of Myrkul')
        .texture('eu_examples:item/bone_blade')     // the flat icon, shown in inventory and other GUI slots
        // GeckoLib look everywhere else (hand, ground, item frames, other players and mobs): assets/eu_examples/
        // geo/item/bone_blade.geo.json, animations/item/bone_blade.animation.json, textures/item/bone_blade_geo.png
        // (+ _glowmask: the teal soul-fire glows) and models/item/bone_blade_geo.json (hand/world display transforms).
        // Awakening plays the 'awaken' transformation (the spine spreads over soul-fire, barbs and horns grow, the
        // skull's eyes light), loops 'awakened', and on sleep plays 'sleep' back to the 'dormant' blade.
        .geoModel('eu_examples:bone_blade')
        .tier('netherite')
        .attackDamageBaseline(4)                 // dormant damage = baseline + tier bonus
        .speedBaseline(-2.4)
        .maxDamage(1250)                         // the energy pool (drains while awakened, recharges while dormant)
        .deity('eu_examples:myrkul')             // devotion with Myrkul scales the awakened powers
        .rechargeTicks(5).drainPerTick(2).hitEnergy(10)
        .elementalRatio(10)                      // elemental power = devotion / 10
        .awakenedDamage(devotion => 2 + devotion / 10)
        .awakenedSpeed(devotion => 0.2)
        .onHit((stack, target, attacker, awakened) => {
            if (awakened) {
                target.attack(EidolonUnchained.hexblade(stack).elementalPower())
                target.potionEffects.add('minecraft:wither', 60, 0)
            }
        })
        .whileHeld((player, stack, awakened) => {
            if (awakened && player.age % 40 == 0) player.potionEffects.add('minecraft:speed', 45, 0)
        })
        .dialogue('The grave is patient. I am not.', 'Feed me, and I will remember you.', 'Bones know the way home.')
        .flavor('A blade that hums with Myrkul\'s hunger.')
        .awakenedTooltip('Awakened hits wither the target')
        .textColor(0x9AC7A0)
})
