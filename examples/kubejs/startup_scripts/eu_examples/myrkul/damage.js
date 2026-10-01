// Myrkul's divine damage (Phase 5). Declaring it creates the damage type 'eu_examples:necrotic' and two attributes
// on every living thing: eu_examples:necrotic_damage and eu_examples:necrotic_resistance (flat parts with ADDITION
// modifiers, percent parts with MULTIPLY modifiers). Deal it with plain KubeJS:
//   target.hurt(EidolonUnchained.damageSource('eu_examples:necrotic', caster), 6)
// Only Myrkul's followers deal it at full power; their reputation with him scales it (his .devotion curve in deity.js).
EidolonUnchained.divineDamage('eu_examples:necrotic')
    .deity('eu_examples:myrkul')
