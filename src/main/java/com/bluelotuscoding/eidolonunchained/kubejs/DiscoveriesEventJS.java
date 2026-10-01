package com.bluelotuscoding.eidolonunchained.kubejs;

import com.bluelotuscoding.eidolonunchained.api.Ids;
import com.bluelotuscoding.eidolonunchained.api.condition.Discoveries;
import com.bluelotuscoding.eidolonunchained.api.condition.Discovery;
import dev.latvian.mods.kubejs.server.ServerEventJS;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.server.MinecraftServer;

/** {@code EidolonUnchainedEvents.discoveries(e => e.discover(id)…)}: fired at server start and after every /reload. */
public class DiscoveriesEventJS extends ServerEventJS {
    public DiscoveriesEventJS(MinecraftServer server) {
        super(server);
    }

    @Info("Declare a discovery: .on(trigger[, id]).when(condition).count(n).grantResearch(id).grantFact(id).grantSign(id).grantRune(id).reputation(deity, n).offersPatronage(deity).message(text).run(fn).once()/.repeatable([n])")
    public Discovery discover(String id) {
        return Discoveries.create(Ids.of(id, "discovery"));
    }
}
