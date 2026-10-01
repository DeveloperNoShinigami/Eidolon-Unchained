// Eidolon Unchained smoke test (startup scripts). Copy the `examples/kubejs` folder over your instance's `kubejs` folder.
// Expected in the log: "[EU smoke] startup ok ..." once, after startup scripts load.
EidolonUnchainedEvents.init(event => {
    console.info(`[EU smoke] startup ok: EU ${EidolonUnchained.version()}, Eidolon ${EidolonUnchained.eidolonVersion()}`)
})
