// Phase 2 example: script-authored codex content (startup scripts, decision D31).
// A chapter about the Storm sign, listed in Eidolon's Signs category, with a chantable tile page for the sign.
EidolonUnchained.codex.chapter('eu_examples:storm_lore')
    .category('eidolon:signs')
    .icon('minecraft:lightning_rod')
    .titlePage('eu_examples.codex.storm.intro', 'minecraft:lightning_rod')
    .textPage('eu_examples.codex.storm.body')
    .signPage('eu_examples:storm')
    .signIndexPage('eu_examples:storm')            // this is what makes the sign chantable (without it, the fallback page would)
    .chantPage('eu_examples.codex.storm.chant', 'eu_examples:call_storm')

// A whole new category for Myrkul, with its own chapter.
EidolonUnchained.codex.category('eu_examples:necromancy')
    .icon('minecraft:wither_skeleton_skull')
    .color(111, 245, 216)
    .entry('eu_examples:myrkul_lore')

EidolonUnchained.codex.chapter('eu_examples:myrkul_lore')
    .icon('minecraft:wither_skeleton_skull')
    .titlePage('eu_examples.codex.myrkul.intro', 'minecraft:wither_skeleton_skull')
    .textPage('eu_examples.codex.myrkul.body')
    .entityPage('minecraft:wither_skeleton')
    .ritualPage('eu_examples:storm_rite')            // the ritual's id: its brazier recipe carries the same id
