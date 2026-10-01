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
    .entityPage('eu_examples:myrkul_avatar', { text: 'eu_examples.codex.myrkul.avatar', rotate: -20,
        animation: 'manifest', then: 'idle', arrivalScale: 0.9, hideBelowGround: true })   // rises out of his rift, then idles at full size
    .ritualPage('eu_examples:storm_rite')            // the ritual's id: its brazier recipe carries the same id
    .ritualPage('eu_examples:pact_of_the_grave')     // Phase 5: pledges the performer to Myrkul

// The pantheon: Eidolon's own deities as EntityJS avatars, each on a bestiary spread (name and text facing the model).
// Each steps out of its gate when the page is reached: arrivalScale shrinks it while the gate is up (the gate is larger
// than the deity), then it eases back to full size for the idle; hideBehind (blocks behind the avatar, just past the back
// of each gate) hides it until it comes through.
EidolonUnchained.codex.chapter('eu_examples:pantheon')
    .category('eidolon:theurgy')
    .icon('minecraft:nether_star')
    .entityPage('eu_examples:light_avatar', { text: 'eu_examples.codex.pantheon.light',
        animation: 'manifest', then: 'idle', arrivalScale: 0.6, hideBehind: 2.25 })
    .entityPage('eu_examples:dark_avatar', { text: 'eu_examples.codex.pantheon.dark',
        animation: 'manifest', then: 'idle', arrivalScale: 0.55, hideBehind: 2.15 })
    .ritualPage('eu_examples:vow_of_light')           // Phase 5: pledges to the Light
    .ritualPage('eu_examples:vow_of_night')           // Phase 5: pledges to the Dark
    .ritualPage('eu_examples:renunciation')           // Phase 5: ends every pledge
