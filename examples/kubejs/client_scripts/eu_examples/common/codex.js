// Phase 2 example: codex events (client scripts). Lists Eidolon's categories once the codex has been built.
EidolonUnchainedEvents.codexPostInit(event => {
    console.info(`[EU example] codex categories: ${event.categories.length}`)
})
