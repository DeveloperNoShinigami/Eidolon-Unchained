// Phase 2 example: research, a custom deity (Myrkul) and an extension of an Eidolon deity (startup scripts).
var C = EidolonUnchained.conditions   // Phase 4 condition library (var: KubeJS files share one scope, so each file may repeat it)

EidolonUnchained.research('eu_examples:necromantic_rites')
    .stars(2)
    .foundOn('minecraft:soul_sand')
    .foundOn('minecraft:wither_skeleton')
    .task(2, EidolonUnchained.tasks.items('minecraft:bone', 8))

EidolonUnchained.deity('eu_examples:myrkul')
    .color(111, 245, 216)
    .model('eu_examples:myrkul')
    .stage('eu_examples:acolyte', 10, true)
        .requireResearch('eu_examples:necromantic_rites')
        .requireSign('eu_examples:storm')
    .stage('eu_examples:reaper', 40, true)
        .require(C.player().knowsResearch('eu_examples:necromantic_rites'))   // Phase 4: a condition as a stage requirement
    .maxReputation(100)
    .onStageUnlocked((player, stage) => player.tell(`§bMyrkul acknowledges you: ${stage}`))
    .onReputationChanged((player, oldRep, newRep) => console.info(`[EU example] Myrkul rep ${oldRep} -> ${newRep} for ${player.username}`))

EidolonUnchained.extendDeity('eidolon:light')
    .model('eu_examples:light_avatar')
    .stage('eu_examples:sun_champion', 60, true)
    .onStageUnlocked((player, stage) => player.tell(`§eThe Light exalts you: ${stage}`))
