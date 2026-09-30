package com.bluelotuscoding.eidolonunchained.api;

import dev.latvian.mods.kubejs.typings.Info;
import elucent.eidolon.api.research.Research;
import elucent.eidolon.api.research.ResearchTask;
import elucent.eidolon.registries.Researches;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code EidolonUnchained.research(id).stars(n).foundOn('block or entity id').task(step, task)} — ends in
 * {@code Researches.register(new Research(id, stars), sources…)} with {@code Research.addSpecialTasks(step, …)}.
 */
public final class ResearchBuilder {
    final ResourceLocation id;
    Integer stars;
    final List<ResourceLocation> sources = new ArrayList<>();
    final List<Object[]> tasks = new ArrayList<>();   // {step, ResearchTask}

    ResearchBuilder(ResourceLocation id) {
        this.id = id;
        EURegistry.declare(EURegistry.Stage.RESEARCH, id, "research", this::register);
    }

    @Info("Difficulty: how many task steps the research has")
    public ResearchBuilder stars(int stars) {
        if (stars < 1) throw new IllegalArgumentException("Eidolon Unchained: research '" + id + "' stars must be >= 1");
        this.stars = stars;
        return this;
    }

    @Info("A block or entity id the research can be discovered on (repeatable)")
    public ResearchBuilder foundOn(String blockOrEntityId) {
        sources.add(Ids.of(blockOrEntityId, "research source"));
        return this;
    }

    @Info("A fixed task at the given step (1-based); see EidolonUnchained.tasks")
    public ResearchBuilder task(int step, ResearchTask task) {
        if (task == null) throw new IllegalArgumentException("Eidolon Unchained: research '" + id + "' task is null");
        tasks.add(new Object[]{step, task});
        return this;
    }

    private void register(ResourceLocation id) {
        if (stars == null) throw new IllegalStateException("research '" + id + "' has no .stars(...)");
        if (Researches.find(id) != null) throw new IllegalStateException("a research with id '" + id + "' already exists");
        var research = new Research(id, stars);
        for (var t : tasks) research.addSpecialTasks((Integer) t[0], (ResearchTask) t[1]);
        var resolved = new ArrayList<>();
        for (var rl : sources) {
            var block = ForgeRegistries.BLOCKS.containsKey(rl) ? ForgeRegistries.BLOCKS.getValue(rl) : null;
            var entity = ForgeRegistries.ENTITY_TYPES.containsKey(rl) ? ForgeRegistries.ENTITY_TYPES.getValue(rl) : null;
            if (block != null) resolved.add(block);
            else if (entity != null) resolved.add(entity);
            else throw new IllegalStateException("research '" + id + "': '" + rl + "' is neither a block nor an entity type");
        }
        Researches.register(research, resolved.toArray());
    }

    /** {@code EidolonUnchained.tasks}: the fixed research tasks Eidolon offers. */
    public static final class Tasks {
        public static final Tasks INSTANCE = new Tasks();

        private Tasks() {
        }

        @Info("Hand in items: item id and count")
        public ResearchTask items(String itemId, int count) {
            var rl = Ids.of(itemId, "item");
            var item = ForgeRegistries.ITEMS.getValue(rl);
            if (item == null || !ForgeRegistries.ITEMS.containsKey(rl)) throw new IllegalArgumentException("Eidolon Unchained: unknown item '" + itemId + "'");
            return new ResearchTask.TaskItems(new ItemStack(item, count));
        }

        @Info("Pay experience levels")
        public ResearchTask xp(int levels) {
            return new ResearchTask.XP(levels);
        }
    }
}
