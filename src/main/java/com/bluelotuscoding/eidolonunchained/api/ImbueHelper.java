package com.bluelotuscoding.eidolonunchained.api;

import com.bluelotuscoding.eidolonunchained.imbue.ImbueCasting;
import com.bluelotuscoding.eidolonunchained.imbue.ImbueNbt;
import dev.latvian.mods.kubejs.typings.Info;
import elucent.eidolon.registries.Spells;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** {@code EidolonUnchained.imbued(stack)}: script side of imbued weapons and Deity's Protection pieces (D35). */
public final class ImbueHelper {
    private final ItemStack stack;

    ImbueHelper(ItemStack stack) {
        this.stack = stack == null ? ItemStack.EMPTY : stack;
    }

    @Info("Can this item take imbued chants (tag eidolonunchained:imbuable, or any tiered weapon / trident)")
    public boolean isImbuable() {
        return ImbueNbt.isImbuable(stack);
    }

    public List<String> chants() {
        return ImbueNbt.chants(stack).stream().map(Object::toString).toList();
    }

    public @Nullable String active() {
        var a = ImbueNbt.activeChant(stack);
        return a == null ? null : a.toString();
    }

    public ImbueHelper setActive(int index) {
        ImbueNbt.setActive(stack, index);
        return this;
    }

    @Info("Add a chant without the worktable (scripts, loot); false when unknown, blacklisted, present or the weapon is full")
    public boolean imbue(String chantId) {
        var id = Ids.of(chantId, "chant");
        var spell = Spells.find(id);
        if (spell == null) throw new IllegalArgumentException("Eidolon Unchained: unknown chant '" + chantId + "'");
        if (spell instanceof ScriptedSpell ss && !ss.isImbuable()) return false;
        return ImbueNbt.imbue(stack, id);
    }

    public boolean remove(String chantId) {
        return ImbueNbt.removeChant(stack, Ids.of(chantId, "chant"));
    }

    @Info("Cast the active chant from the player now (server), as a right-click would")
    public boolean cast(Player player) {
        if (!(player instanceof ServerPlayer sp)) return false;
        return ImbueCasting.castImbued(sp, stack);
    }

    // ---- Deity's Protection ----

    public int protectionLevel() {
        return ImbueNbt.protectionLevel(stack);
    }

    public @Nullable String protectionChant() {
        var c = ImbueNbt.protectionChant(stack);
        return c == null ? null : c.toString();
    }

    @Info("Bind a deity-bound chant to a piece that carries Deity's Protection; false when the chant is not deity-bound")
    public boolean protect(String chantId) {
        var id = Ids.of(chantId, "chant");
        if (!(Spells.find(id) instanceof ScriptedSpell ss)) throw new IllegalArgumentException("Eidolon Unchained: unknown scripted chant '" + chantId + "'");
        if (ss.deity() == null || ImbueNbt.protectionLevel(stack) <= 0) return false;
        ImbueNbt.setProtection(stack, id, ss.deity());
        return true;
    }

    public ImbueHelper unprotect() {
        ImbueNbt.setProtection(stack, null, null);
        return this;
    }
}
