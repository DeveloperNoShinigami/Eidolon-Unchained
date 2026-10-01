// Phase 2 example: an extension of an Eidolon deity, the Light (startup scripts): followers, calling, model and a stage.
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
