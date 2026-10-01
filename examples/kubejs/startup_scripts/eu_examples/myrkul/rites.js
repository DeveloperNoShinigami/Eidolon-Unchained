// Phase 5 example: Myrkul's patron rite (startup scripts). Its brazier recipe is in server_scripts/eu_examples/myrkul/recipes.js.
// Patron rites: completing one pledges the performer (the nearest player) to the god (the rite that ends a pledge is
// common/rites.js). A major god won't share its follower: if the performer already follows another major god, the brazier
// refuses at once, the pedestals keep their items and the reagent comes back.
EidolonUnchained.ritual('eu_examples:pact_of_the_grave')
    .symbol('eidolon:particle/summon_ritual')
    .color(111, 245, 216)
    .grantsPatron('eu_examples:myrkul')
