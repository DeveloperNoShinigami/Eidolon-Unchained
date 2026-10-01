// Phase 2 example: an extension of an Eidolon deity, the Dark (startup scripts): its calling.
EidolonUnchained.extendDeity('eidolon:dark')
    .calling({
        greeting: 'eu_examples.calling.dark.greeting', question: 'eu_examples.calling.dark.question',
        accept: 'eu_examples.calling.dark.accept', decline: 'eu_examples.calling.dark.decline',
        silence: 'eu_examples.calling.dark.silence',
        yes: ['yes', 'i will', 'i accept'], no: ['no', 'never'], wait: 60, callAgainAfter: 600,
        voice: 'eidolon:wraith_ambient'
    })
