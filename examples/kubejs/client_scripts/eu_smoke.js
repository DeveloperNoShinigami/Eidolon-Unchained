// Eidolon Unchained smoke test (client scripts).
// Expected in the log: "[EU smoke] client ok" when you join a world.
EidolonUnchainedEvents.clientReady(event => {
    console.info(`[EU smoke] client ok for ${event.player.username}`)
})
