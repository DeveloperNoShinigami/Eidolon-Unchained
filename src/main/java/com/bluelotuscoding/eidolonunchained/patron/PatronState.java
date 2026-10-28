package com.bluelotuscoding.eidolonunchained.patron;

import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Player-scoped patron/title state backed by player persistent NBT.
 */
public final class PatronState {

    private static final String ROOT_TAG = "eidolonunchained_patron";
    private static final String PATRON_KEY = "patron";
    private static final String TITLE_KEY = "title";

    private PatronState() {
    }

    private static CompoundTag getRootTag(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_TAG, CompoundTag.TAG_COMPOUND)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        return persistent.getCompound(ROOT_TAG);
    }

    public static ResourceLocation getPatron(ServerPlayer player) {
        CompoundTag root = getRootTag(player);
        if (!root.contains(PATRON_KEY, CompoundTag.TAG_STRING)) {
            return null;
        }
        String raw = root.getString(PATRON_KEY);
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        return ResourceLocation.tryParse(raw);
    }

    public static void setPatron(ServerPlayer player, ResourceLocation deityId) {
        CompoundTag root = getRootTag(player);
        if (deityId == null) {
            root.remove(PATRON_KEY);
            root.remove(TITLE_KEY);
            return;
        }
        root.putString(PATRON_KEY, deityId.toString());
        updateTitle(player);
    }

    public static String getTitle(ServerPlayer player) {
        CompoundTag root = getRootTag(player);
        if (!root.contains(TITLE_KEY, CompoundTag.TAG_STRING)) {
            return null;
        }
        String title = root.getString(TITLE_KEY);
        return title == null || title.isEmpty() ? null : title;
    }

    public static void setTitle(ServerPlayer player, String title) {
        CompoundTag root = getRootTag(player);
        if (title == null || title.isEmpty()) {
            root.remove(TITLE_KEY);
        } else {
            root.putString(TITLE_KEY, title);
        }
    }

    public static void updateTitle(ServerPlayer player) {
        ResourceLocation patron = getPatron(player);
        if (patron == null) {
            setTitle(player, null);
            return;
        }

        DatapackDeity deity = DatapackDeityManager.getDeity(patron);
        if (deity != null) {
            deity.updatePatronTitle(player);
        }
    }

    public static double getReputationModifier(ServerPlayer player, ResourceLocation deityId) {
        ResourceLocation patron = getPatron(player);
        if (patron == null || deityId == null) {
            return 1.0;
        }
        if (patron.equals(deityId)) {
            return 1.2;
        }
        if (areOpposingDeities(patron, deityId)) {
            return 0.7;
        }
        return 1.0;
    }

    public static boolean areOpposingDeities(ResourceLocation deity1, ResourceLocation deity2) {
        return deity1 != null && deity2 != null && !deity1.equals(deity2);
    }
}
