// Eidolon Unchained smoke test (server scripts).
// Expected in the log: "[EU smoke] server ok" when the server starts, and the API validation report.
EidolonUnchainedEvents.serverReady(event => {
    console.info(`[EU smoke] server ok on ${event.server.motd}`)
    console.info(EidolonUnchained.apiReport())
})
