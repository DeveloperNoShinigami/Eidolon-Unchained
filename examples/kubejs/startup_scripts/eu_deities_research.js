// Phase 2 example: research, a custom deity (Myrkul) and an extension of an Eidolon deity (startup scripts).
var C = EidolonUnchained.conditions   // Phase 4 condition library (var: KubeJS files share one scope, so each file may repeat it)

EidolonUnchained.research('eu_examples:necromantic_rites')
    .stars(2)
    .foundOn('minecraft:soul_sand')
    .foundOn('minecraft:wither_skeleton')
    .task(2, EidolonUnchained.tasks.items('minecraft:bone', 8))

// Phase 5: Myrkul keeps the default .patronRequired(true): he gives no reputation until the player pledges to him
// (/eu patron set <player> eu_examples:myrkul), one major patron at a time. .maxMana(n) on a stage is the max mana
// his followers hold while they have that stage (a floor under Eidolon's prayer value; reaching it fills the gap once).
EidolonUnchained.deity('eu_examples:myrkul')
    .followers('#minecraft:skeletons', 'minecraft:wither')   // Phase 5: these mobs follow Myrkul and may use his power
    // Phase 5: how Myrkul calls a player (a discovery's .offersPatronage starts it); lines are lang keys or plain text
    .calling({
        greeting: 'eu_examples.calling.myrkul.greeting', question: 'eu_examples.calling.myrkul.question',
        accept: 'eu_examples.calling.myrkul.accept', decline: 'eu_examples.calling.myrkul.decline',
        silence: 'eu_examples.calling.myrkul.silence',
        yes: ['yes', 'i will', 'i accept', 'i serve'], no: ['no', 'never', 'begone'], wait: 60, callAgainAfter: 'never'
    })
    .color(111, 245, 216)
    .model('eu_examples:myrkul')
    .stage('eu_examples:acolyte', 10, true).maxMana(60)
        .requireResearch('eu_examples:necromantic_rites')
        .requireSign('eu_examples:storm')
    .stage('eu_examples:reaper', 40, true).maxMana(150)
        .require(C.player().knowsResearch('eu_examples:necromantic_rites'))   // Phase 4: a condition as a stage requirement
    .maxReputation(100)
    .onStageUnlocked((player, stage) => player.tell(`§bMyrkul acknowledges you: ${stage}`))
    .onReputationChanged((player, oldRep, newRep) => console.info(`[EU example] Myrkul rep ${oldRep} -> ${newRep} for ${player.username}`))

EidolonUnchained.extendDeity('eidolon:light')
    .followers('minecraft:iron_golem')                        // Phase 5: golems serve the Light
    .calling({
        greeting: 'eu_examples.calling.light.greeting', question: 'eu_examples.calling.light.question',
        accept: 'eu_examples.calling.light.accept', decline: 'eu_examples.calling.light.decline',
        silence: 'eu_examples.calling.light.silence',
        yes: ['yes', 'i will', 'i accept', 'i will walk in your light'], no: ['no', 'never'], wait: 60, callAgainAfter: 1200,
        voice: 'minecraft:block.note_block.chime'   // any sound id; default eidolon:chant_word
    })
    .model('eu_examples:light_avatar')
    .stage('eu_examples:sun_champion', 60, true)
    .onStageUnlocked((player, stage) => player.tell(`§eThe Light exalts you: ${stage}`))

EidolonUnchained.extendDeity('eidolon:dark')
    .calling({
        greeting: 'eu_examples.calling.dark.greeting', question: 'eu_examples.calling.dark.question',
        accept: 'eu_examples.calling.dark.accept', decline: 'eu_examples.calling.dark.decline',
        silence: 'eu_examples.calling.dark.silence',
        yes: ['yes', 'i will', 'i accept'], no: ['no', 'never'], wait: 60, callAgainAfter: 600,
        voice: 'eidolon:wraith_ambient'
    })
