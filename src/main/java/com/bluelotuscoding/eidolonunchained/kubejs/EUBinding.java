package com.bluelotuscoding.eidolonunchained.kubejs;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.api.EidolonApiValidation;
import net.minecraftforge.fml.ModList;

/**
 * The {@code EidolonUnchained} global available in every script type. Phase 1: version and diagnostics only.
 * <pre>
 * console.info(EidolonUnchained.version())          // "2.0.0-alpha.1"
 * console.info(EidolonUnchained.eidolonVersion())   // "0.3.13"
 * EidolonUnchained.apiReport()                      // the API validation report as text
 * </pre>
 */
public final class EUBinding {
    public static final EUBinding INSTANCE = new EUBinding();

    private EUBinding() {
    }

    public String version() {
        return EidolonUnchained.version();
    }

    public String eidolonVersion() {
        return EidolonUnchained.modVersion("eidolon");
    }

    public boolean isLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public String apiReport() {
        return EidolonApiValidation.report();
    }

    public void log(String message) {
        EidolonUnchained.LOGGER.info("[script] {}", message);
    }
}
