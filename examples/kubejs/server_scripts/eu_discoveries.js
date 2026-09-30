// Phase 4 example: discoveries (server scripts, rebuilt on /reload). "When this happens, and these conditions hold, give this."
// Each is once per player unless .repeatable(); /eu discoveries reset forgets them for testing, /eu discoveries list shows them.
var C = EidolonUnchained.conditions   // Phase 4 condition library (var: KubeJS files share one scope, so each file may repeat it)

EidolonUnchainedEvents.discoveries(event => {
    // Easy to test: the first time you walk into any ocean biome.
    event.create('eu_examples:sea_memory')
        .on('biome')
        .when(C.biome('#minecraft:is_ocean'))
        .grantFact('eu_examples:sea_memory')
        .message('eu_examples.discovery.sea_memory')

    // Entering the Deep Dark teaches the Storm sign.
    event.create('eu_examples:deep_dark_whispers')
        .on('biome', 'minecraft:deep_dark')
        .grantSign('eu_examples:storm')
        .message('eu_examples.discovery.deep_dark')

    // Killing a zombie named "The Forgotten" teaches Necromantic Rites, if Myrkul regards you at all.
    event.create('eu_examples:forgotten_lore')
        .on('kill')
        .when(C.entity('minecraft:zombie').named('The Forgotten'))
        .when(C.player().reputation('eu_examples:myrkul', 0))
        .grantResearch('eu_examples:necromantic_rites')
        .message('eu_examples.discovery.forgotten')

    // Every Storm Rite you finish pleases Myrkul a little.
    event.create('eu_examples:storm_rite_favor')
        .on('ritual', 'eu_examples:storm_rite')
        .reputation('eu_examples:myrkul', 5)
        .message('eu_examples.discovery.storm_rite')
        .repeatable()

    // Enthralling any undead with Eidolon's enthrall chant.
    event.create('eu_examples:thrall_master')
        .on('enthrall')
        .when(C.entity().undead())
        .grantFact('eu_examples:thrall_master')
        .message('eu_examples.discovery.thrall')

    // Reaching a fortress while Myrkul regards you. (Not the Dark: Eidolon caps Dark reputation at 3 until the player
    // knows its "Sacrifice Mob" research, so a Dark >= 10 condition can't pass early in a playthrough.)
    event.create('eu_examples:fortress_omen')
        .on('structure', 'minecraft:fortress')
        .when(C.player().reputation('eu_examples:myrkul', 10))
        .reputation('eu_examples:myrkul', 3)
        .run(ctx => ctx.player.tell('§5The fortress walls hum; Myrkul has walked these halls.'))
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
