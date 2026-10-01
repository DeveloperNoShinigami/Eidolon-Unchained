// Phase 4 example: discoveries (server scripts, rebuilt on /reload). "When this happens, and these conditions hold, give this."
// Each is once per player unless .repeatable([n]); /eu discoveries reset forgets them for testing, /eu discoveries list shows them.
// This file holds the discoveries not tied to one god, the discovery-related events and a plain condition; each god's own
// discoveries are in its folder's discoveries.js.
var C = EidolonUnchained.conditions   // Phase 4 condition library (var: KubeJS files share one scope, so each file may repeat it)

EidolonUnchainedEvents.discoveries(event => {
    // Easy to test: the first time you walk into any ocean biome.
    event.discover('eu_examples:sea_memory')
        .on('biome')
        .when(C.biome('#minecraft:is_ocean'))
        .grantFact('eu_examples:sea_memory')
        .message('eu_examples.discovery.sea_memory')

    // Entering the Deep Dark teaches the Storm sign.
    event.discover('eu_examples:deep_dark_whispers')
        .on('biome', 'minecraft:deep_dark')
        .grantSign('eu_examples:storm')
        .message('eu_examples.discovery.deep_dark')

    // Every Storm Rite you finish pleases Myrkul a little.
    event.discover('eu_examples:storm_rite_favor')
        .on('ritual', 'eu_examples:storm_rite')
        .reputation('eu_examples:myrkul', 5)
        .message('eu_examples.discovery.storm_rite')
        .repeatable()
})

// The new events, for anything a discovery cannot express (extra id filters work like the other events).
EidolonUnchainedEvents.enteredStructure(e => console.info(`[EU example] ${e.player.username} entered structure ${e.id}`))
EidolonUnchainedEvents.learnedSign(e => console.info(`[EU example] ${e.player.username} learned sign ${e.id}`))
EidolonUnchainedEvents.learnedFact(e => console.info(`[EU example] ${e.player.username} learned fact ${e.id}`))
EidolonUnchainedEvents.ritualCompleted(e => console.info(`[EU example] ritual ${e.id} completed near ${e.player ? e.player.username : 'nobody'}`))
EidolonUnchainedEvents.enteredBiome('minecraft:deep_dark', e => e.player.tell('§8The dark swallows sound.'))

// Conditions work anywhere in scripts too: a condition is a plain value with .test(...)
const inTheNether = C.dimension('minecraft:the_nether')
PlayerEvents.loggedIn(e => { if (inTheNether.test(e.player)) e.player.tell('§cYou wake in the Nether.') })
