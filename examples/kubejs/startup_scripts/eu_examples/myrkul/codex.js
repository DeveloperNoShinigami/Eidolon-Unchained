// Phase 2 example: Myrkul's codex content (startup scripts, decision D31): his own category and chapter.
// The Storm sign and pantheon chapters are in common/codex.js.
// A whole new category for Myrkul, with its own chapter.
EidolonUnchained.codex.category('eu_examples:necromancy')
    .icon('minecraft:wither_skeleton_skull')
    .color(111, 245, 216)
    .entry('eu_examples:myrkul_lore')

EidolonUnchained.codex.chapter('eu_examples:myrkul_lore')
    .icon('minecraft:wither_skeleton_skull')
    .titlePage('eu_examples.codex.myrkul.intro', 'minecraft:wither_skeleton_skull')
    .textPage('eu_examples.codex.myrkul.body')
    .entityPage('eu_examples:myrkul_avatar', { text: 'eu_examples.codex.myrkul.avatar', rotate: -20,
        animation: 'manifest', then: 'idle', arrivalScale: 0.9, hideBelowGround: true })   // rises out of his rift, then idles at full size
    .ritualPage('eu_examples:storm_rite')            // the ritual's id: its brazier recipe carries the same id
    .ritualPage('eu_examples:pact_of_the_grave')     // Phase 5: pledges the performer to Myrkul
