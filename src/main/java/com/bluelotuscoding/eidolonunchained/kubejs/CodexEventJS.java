package com.bluelotuscoding.eidolonunchained.kubejs;

import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.typings.Info;
import elucent.eidolon.codex.Category;
import elucent.eidolon.codex.CodexChapters;
import elucent.eidolon.codex.CodexEvents;

import java.util.List;

/**
 * {@code EidolonUnchainedEvents.codexPreInit / codexPostInit} (client): Eidolon's {@link CodexEvents}. Eidolon passes
 * its {@code CodexChapters.categories} list to the event (the event's own field is package-private), so the same
 * public list is exposed here: a script can read, reorder and append categories with Eidolon's codex classes.
 */
public class CodexEventJS extends EventJS {
    private final boolean post;

    public CodexEventJS(CodexEvents event) {
        this.post = event instanceof CodexEvents.PostInit;
    }

    @Info("Eidolon's codex categories (the live list; mutable)")
    public List<Category> getCategories() {
        return CodexChapters.categories;
    }

    @Info("Item -> index entry map used by the codex search (the live map; mutable)")
    public java.util.Map<net.minecraft.world.item.Item, elucent.eidolon.codex.IndexPage.IndexEntry> getItemToEntry() {
        return CodexChapters.itemToEntryMap;
    }

    @Info("true for codexPostInit, false for codexPreInit")
    public boolean isPost() {
        return post;
    }
}
