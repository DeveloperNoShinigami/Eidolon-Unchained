package com.bluelotuscoding.eidolonunchained.kubejs;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ClassFilter;

/**
 * The KubeJS plugin. Registered through {@code kubejs.plugins.txt}. Phase 1 exposes only the diagnostics event group
 * and the {@code EidolonUnchained} binding; the Eidolon wrappers arrive in Phase 2.
 */
public class EUKubeJSPlugin extends KubeJSPlugin {
    @Override
    public void registerEvents() {
        EUEvents.GROUP.register();
        EidolonUnchained.LOGGER.info("KubeJS plugin registered: event group '{}'", EUEvents.GROUP);
    }

    @Override
    public void registerClasses(ScriptType type, ClassFilter filter) {
        // Scripts may touch Eidolon's public API surface and this mod's own API package, nothing deeper.
        filter.allow("elucent.eidolon.api");
        filter.allow("com.bluelotuscoding.eidolonunchained.api");
        filter.deny("com.bluelotuscoding.eidolonunchained.kubejs");
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("EidolonUnchained", EUBinding.INSTANCE);
    }

    @Override
    public void afterInit() {
        // Startup scripts have been loaded by now; tell them so. Server and client events are fired by EUEvents.
        EUEvents.postStartup();
    }
}
