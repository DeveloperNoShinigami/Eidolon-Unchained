// Phase 5 example: the Dark's patron rite, the Vow of Night (startup scripts). Its brazier recipe is in
// server_scripts/eu_examples/dark/recipes.js. Completing it pledges the performer (the nearest player) to the Dark; a major
// god won't share its follower (see myrkul/rites.js).
EidolonUnchained.ritual('eu_examples:vow_of_night')
    .symbol('eidolon:particle/moonlight_ritual')
    .color(120, 60, 170)
    .grantsPatron('eidolon:dark')
