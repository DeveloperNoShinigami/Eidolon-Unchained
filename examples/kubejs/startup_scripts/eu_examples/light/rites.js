// Phase 5 example: the Light's patron rite, the Vow of Light (startup scripts). Its brazier recipe is in
// server_scripts/eu_examples/light/recipes.js. Completing it pledges the performer (the nearest player) to the Light; a major
// god won't share its follower (see myrkul/rites.js).
EidolonUnchained.ritual('eu_examples:vow_of_light')
    .symbol('eidolon:particle/daylight_ritual')
    .color(255, 230, 150)
    .grantsPatron('eidolon:light')
