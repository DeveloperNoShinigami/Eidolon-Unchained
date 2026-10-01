// Phase 2 example: script-authored codex content shared by several gods (startup scripts, decision D31):
// the Storm sign chapter and the pantheon chapter (Light and Dark). Myrkul's category and chapter are in myrkul/codex.js.
// A chapter about the Storm sign, listed in Eidolon's Signs category, with a chantable tile page for the sign.
EidolonUnchained.codex.chapter('eu_examples:storm_lore')
    .category('eidolon:signs')
    .icon('minecraft:lightning_rod')
    .titlePage('eu_examples.codex.storm.intro', 'minecraft:lightning_rod')
    .textPage('eu_examples.codex.storm.body')
    .signPage('eu_examples:storm')
    .signIndexPage('eu_examples:storm')            // this is what makes the sign chantable (without it, the fallback page would)
    .chantPage('eu_examples.codex.storm.chant', 'eu_examples:call_storm')

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
